package dev.miklires.folders.core.storage;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderConfig;
import dev.miklires.folders.core.data.FolderIcon;
import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStorageTest {

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("a missing file loads as an empty configuration (§8)")
    void missingFile(@TempDir Path dir) {
        try (JsonStorage storage = new JsonStorage(dir)) {
            FolderConfig config = storage.load(FolderType.WORLDS);
            assertTrue(config.folders().isEmpty());
            assertTrue(config.root().isEmpty());
        }
    }

    @Test
    @DisplayName("an empty file loads as empty without being quarantined")
    void emptyFile(@TempDir Path dir) throws IOException {
        write(dir.resolve("worlds.json"), "   \n ");
        try (JsonStorage storage = new JsonStorage(dir)) {
            assertTrue(storage.load(FolderType.WORLDS).folders().isEmpty());
            assertFalse(Files.exists(dir.resolve("worlds.json.bak")));
        }
    }

    @Test
    @DisplayName("a corrupt file is backed up and replaced, the game carries on (§8)")
    void corruptFileIsQuarantined(@TempDir Path dir) throws IOException {
        write(dir.resolve("worlds.json"), "{ this is not json ");
        try (JsonStorage storage = new JsonStorage(dir)) {
            FolderConfig config = storage.load(FolderType.WORLDS);

            assertTrue(config.folders().isEmpty());
            assertTrue(Files.exists(dir.resolve("worlds.json.bak")), "the broken file should be kept as .bak");
            assertEquals("{ this is not json ", Files.readString(dir.resolve("worlds.json.bak")));
            assertFalse(Files.exists(dir.resolve("worlds.json")));
        }
    }

    @Test
    @DisplayName("a second corruption does not overwrite the first backup")
    void secondBackupIsTimestamped(@TempDir Path dir) throws IOException {
        try (JsonStorage storage = new JsonStorage(dir)) {
            write(dir.resolve("worlds.json"), "broken one");
            storage.load(FolderType.WORLDS);
            write(dir.resolve("worlds.json"), "broken two");
            storage.load(FolderType.WORLDS);

            assertEquals("broken one", Files.readString(dir.resolve("worlds.json.bak")));
            long extras;
            try (var stream = Files.list(dir)) {
                extras = stream.filter(p -> p.getFileName().toString().endsWith(".bak")).count();
            }
            assertEquals(2, extras);
        }
    }

    @Test
    @DisplayName("a file from a newer format version is preserved, not mangled (§8)")
    void futureVersionIsQuarantined(@TempDir Path dir) throws IOException {
        write(dir.resolve("worlds.json"), "{\"version\": 99, \"folders\": []}");
        try (JsonStorage storage = new JsonStorage(dir)) {
            assertTrue(storage.load(FolderType.WORLDS).folders().isEmpty());
            assertTrue(Files.exists(dir.resolve("worlds.json.bak")));
        }
    }

    @Test
    @DisplayName("one bad entry costs that entry, not the whole file")
    void partiallyBrokenFile(@TempDir Path dir) throws IOException {
        write(dir.resolve("worlds.json"), """
                {
                  "version": 1,
                  "folders": [
                    {"id": "not-a-uuid", "name": "Broken", "items": []},
                    {"id": "550e8400-e29b-41d4-a716-446655440000", "name": "Good",
                     "icon": {"type": "default"}, "expanded": true,
                     "items": ["world:a", "server:x:1", "world:a", "world:b"]},
                    "a string where an object should be"
                  ],
                  "root": ["folder:550e8400-e29b-41d4-a716-446655440000", "world:c", 42]
                }
                """);
        try (JsonStorage storage = new JsonStorage(dir)) {
            FolderConfig config = storage.load(FolderType.WORLDS);

            assertEquals(1, config.folders().size());
            Folder folder = config.folders().get(0);
            assertEquals("Good", folder.name());
            assertTrue(folder.expanded());
            // The wrong-type id and the duplicate are both dropped.
            assertEquals(List.of("world:a", "world:b"), folder.items());
            assertEquals(List.of("folder:550e8400-e29b-41d4-a716-446655440000", "world:c"), config.root());
        }
    }

    @Test
    @DisplayName("an icon file name that looks like a path falls back to the default (§52)")
    void rejectsPathShapedIconNames(@TempDir Path dir) throws IOException {
        write(dir.resolve("worlds.json"), """
                {"version":1,"folders":[
                  {"id":"550e8400-e29b-41d4-a716-446655440000","name":"A",
                   "icon":{"type":"custom","file":"../../../etc/passwd.png"},"items":[]}
                ],"root":[]}
                """);
        try (JsonStorage storage = new JsonStorage(dir)) {
            FolderConfig config = storage.load(FolderType.WORLDS);
            assertEquals(FolderIcon.DEFAULT, config.folders().get(0).icon());
        }
    }

    @Test
    @DisplayName("a saved configuration reloads identically (§57, §59)")
    void roundTrip(@TempDir Path dir) throws ExecutionException, InterruptedException {
        try (JsonStorage storage = new JsonStorage(dir)) {
            FolderRepository repository = FolderRepository.empty(FolderType.WORLDS);
            repository.sync(List.of("world:a", "world:b", "world:c"), true);
            Folder folder = repository.createFolder("Survival");
            repository.moveFolder(folder.id(), 1);
            repository.moveToFolder("world:b", folder.id(), -1);
            repository.moveToFolder("world:c", folder.id(), 0);
            repository.setExpanded(folder.id(), true);
            repository.setIcon(folder.id(), FolderIcon.custom("icon.png"));

            storage.saveAsync(FolderType.WORLDS, repository.config()).get();

            FolderConfig reloaded = storage.load(FolderType.WORLDS);
            FolderRepository restored = new FolderRepository(FolderType.WORLDS, reloaded);

            Folder restoredFolder = restored.folder(folder.id()).orElseThrow();
            assertEquals("Survival", restoredFolder.name());
            assertTrue(restoredFolder.expanded());
            assertEquals(List.of("world:c", "world:b"), restoredFolder.items());
            assertEquals("icon.png", restoredFolder.icon().file().orElseThrow());
            assertEquals(List.of("world:a", "folder:" + folder.id()), reloaded.root());
        }
    }

    @Test
    @DisplayName("queued saves land in order and leave no temp file behind (§51)")
    void savesAreSerialised(@TempDir Path dir) throws Exception {
        try (JsonStorage storage = new JsonStorage(dir)) {
            FolderRepository repository = FolderRepository.empty(FolderType.WORLDS);
            for (int i = 0; i < 25; i++) {
                repository.createFolder("F" + i);
                storage.saveAsync(FolderType.WORLDS, repository.config());
            }
            storage.flush();

            FolderConfig reloaded = storage.load(FolderType.WORLDS);
            assertEquals(25, reloaded.folders().size());
            try (var stream = Files.list(dir)) {
                assertFalse(stream.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
            }
        }
    }

    @Test
    @DisplayName("settings survive a round trip and clamp the animation speed")
    void settingsRoundTrip(@TempDir Path dir) {
        Path file = dir.resolve("settings.json");
        FoldersSettings settings = new FoldersSettings();
        settings.setAnimations(false);
        settings.setAnimationSpeed(1000.0f);
        settings.setShowStatistics(false);
        settings.save(file);

        FoldersSettings reloaded = FoldersSettings.load(file);
        assertFalse(reloaded.animations());
        assertEquals(FoldersSettings.MAX_ANIMATION_SPEED, reloaded.animationSpeed());
        assertFalse(reloaded.showStatistics());
        assertTrue(reloaded.showOnlineIndicator());
        assertEquals(0L, reloaded.scaledDuration(200L), "animations off means no duration");
    }

    @Test
    @DisplayName("broken settings fall back to defaults instead of throwing")
    void brokenSettings(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("settings.json");
        write(file, "not json at all");
        FoldersSettings settings = FoldersSettings.load(file);
        assertNotNull(settings);
        assertTrue(settings.animations());
    }
}
