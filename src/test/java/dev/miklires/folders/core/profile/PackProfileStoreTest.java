package dev.miklires.folders.core.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackProfileStoreTest {

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("profiles survive a save and reload")
    void roundTrip(@TempDir Path dir) {
        PackProfileStore store = PackProfileStore.load(dir);
        store.add("Shaders", List.of("file/sildurs.zip", "file/detail.zip"));
        store.saveIfDirty();

        PackProfileStore reloaded = PackProfileStore.load(dir);
        assertEquals(1, reloaded.profiles().size());
        PackProfile profile = reloaded.profiles().getFirst();
        assertEquals("Shaders", profile.name());
        assertEquals(List.of("file/sildurs.zip", "file/detail.zip"), profile.packIds());
    }

    @Test
    @DisplayName("pack order is kept and duplicates are dropped")
    void orderKeptDuplicatesDropped() {
        PackProfile profile = PackProfile.of("Build",
                List.of("file/b.zip", "file/a.zip", "file/b.zip"));
        assertEquals(List.of("file/b.zip", "file/a.zip"), profile.packIds());
    }

    @Test
    @DisplayName("a second profile cannot take a name already in use")
    void namesAreUnique(@TempDir Path dir) {
        PackProfileStore store = PackProfileStore.load(dir);
        store.add("Build", List.of("file/a.zip"));
        PackProfile second = store.add("Build", List.of("file/b.zip"));
        assertEquals("Build 2", second.name());
    }

    @Test
    @DisplayName("renaming to a taken name is disambiguated, renaming to blank is refused")
    void renaming(@TempDir Path dir) {
        PackProfileStore store = PackProfileStore.load(dir);
        PackProfile first = store.add("Build", List.of("file/a.zip"));
        PackProfile second = store.add("Survival", List.of("file/b.zip"));

        assertTrue(store.rename(second.id(), "Build"));
        assertEquals("Build 2", store.profile(second.id()).orElseThrow().name());

        assertFalse(store.rename(first.id(), "   "));
        assertEquals("Build", store.profile(first.id()).orElseThrow().name());
    }

    @Test
    @DisplayName("renaming a profile to its own name leaves it alone")
    void renameToSelf(@TempDir Path dir) {
        PackProfileStore store = PackProfileStore.load(dir);
        PackProfile profile = store.add("Build", List.of("file/a.zip"));
        assertTrue(store.rename(profile.id(), "Build"));
        assertEquals("Build", store.profile(profile.id()).orElseThrow().name());
    }

    @Test
    @DisplayName("overwriting a profile keeps its id and name")
    void update(@TempDir Path dir) {
        PackProfileStore store = PackProfileStore.load(dir);
        PackProfile profile = store.add("Build", List.of("file/a.zip"));
        assertTrue(store.update(profile.id(), List.of("file/c.zip")));

        PackProfile updated = store.profile(profile.id()).orElseThrow();
        assertEquals("Build", updated.name());
        assertEquals(List.of("file/c.zip"), updated.packIds());
    }

    @Test
    @DisplayName("a malformed file is moved aside and the game starts with no profiles")
    void malformedFileQuarantined(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(PackProfileStore.FILE_NAME);
        write(file, "{ not json");

        PackProfileStore store = PackProfileStore.load(dir);
        assertTrue(store.isEmpty());
        assertFalse(Files.exists(file));
        assertTrue(Files.exists(dir.resolve(PackProfileStore.FILE_NAME + ".bak")));
    }

    @Test
    @DisplayName("a file from a newer build is quarantined rather than read")
    void newerVersionQuarantined(@TempDir Path dir) throws IOException {
        write(dir.resolve(PackProfileStore.FILE_NAME),
                "{\"version\": 99, \"profiles\": [{\"id\": \"" + UUID.randomUUID() + "\", \"name\": \"x\"}]}");

        PackProfileStore store = PackProfileStore.load(dir);
        assertTrue(store.isEmpty());
        assertTrue(Files.exists(dir.resolve(PackProfileStore.FILE_NAME + ".bak")));
    }

    @Test
    @DisplayName("one unreadable profile costs that profile, not the file")
    void oneBadEntryDropped(@TempDir Path dir) throws IOException {
        String good = UUID.randomUUID().toString();
        write(dir.resolve(PackProfileStore.FILE_NAME), """
                {
                  "version": 1,
                  "profiles": [
                    {"id": "not-a-uuid", "name": "broken", "packs": ["file/a.zip"]},
                    {"id": "%s", "name": "fine", "packs": ["file/b.zip"]}
                  ]
                }
                """.formatted(good));

        PackProfileStore store = PackProfileStore.load(dir);
        assertEquals(1, store.profiles().size());
        assertEquals("fine", store.profiles().getFirst().name());
    }

    @Test
    @DisplayName("a name with control characters is cleaned and capped")
    void namesAreSanitised() {
        PackProfile profile = PackProfile.of("a\nb\tc" + "x".repeat(80), List.of());
        assertFalse(profile.name().contains("\n"));
        assertEquals(PackProfile.MAX_NAME_LENGTH, profile.name().length());
    }

    @Test
    @DisplayName("deleting a profile leaves the others in place")
    void remove(@TempDir Path dir) {
        PackProfileStore store = PackProfileStore.load(dir);
        PackProfile first = store.add("Build", List.of("file/a.zip"));
        store.add("Survival", List.of("file/b.zip"));

        assertTrue(store.remove(first.id()));
        assertFalse(store.remove(first.id()));
        assertEquals(List.of("Survival"), store.profiles().stream().map(PackProfile::name).toList());
    }
}
