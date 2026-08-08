package dev.miklires.folders.core.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderRepositoryTest {

    private FolderRepository repository;

    @BeforeEach
    void setUp() {
        repository = FolderRepository.empty(FolderType.WORLDS);
    }

    private static String world(String name) {
        return "world:" + name;
    }

    private List<String> rootIds() {
        return repository.rootEntries().stream()
                .map(entry -> switch (entry) {
                    case RootEntry.FolderRef ref -> "F:" + ref.folder().name();
                    case RootEntry.ItemRef ref -> ref.itemId();
                })
                .toList();
    }

    @Nested
    @DisplayName("folder lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("repeated creation numbers the default name")
        void numbersDuplicateNames() {
            assertEquals("New folder", repository.createFolder("New folder").name());
            assertEquals("New folder 2", repository.createFolder("New folder").name());
            assertEquals("New folder 3", repository.createFolder("New folder").name());
        }

        @Test
        @DisplayName("renaming keeps the id and the contents")
        void renameKeepsContents() {
            Folder folder = repository.createFolder("Survival");
            repository.moveToFolder(world("a"), folder.id(), -1);

            assertTrue(repository.renameFolder(folder.id(), "Hardcore"));

            assertEquals("Hardcore", folder.name());
            assertEquals(List.of(world("a")), folder.items());
            assertSame(folder, repository.folder(folder.id()).orElseThrow());
        }

        @Test
        @DisplayName("an empty or whitespace-only name is rejected")
        void rejectsEmptyName() {
            Folder folder = repository.createFolder("Survival");
            assertFalse(repository.renameFolder(folder.id(), "   "));
            assertFalse(repository.renameFolder(folder.id(), ""));
            assertEquals("Survival", folder.name());
        }

        @Test
        @DisplayName("a long name is clamped rather than refused")
        void clampsLongName() {
            Folder folder = repository.createFolder("x");
            repository.renameFolder(folder.id(), "y".repeat(500));
            assertEquals(Folder.MAX_NAME_LENGTH, folder.name().length());
        }

        @Test
        @DisplayName("deleting returns the contents to the root, in place")
        void deleteReleasesContentsInPlace() {
            repository.sync(List.of(world("before"), world("a"), world("b"), world("after")), true);
            Folder folder = repository.createFolder("Survival");
            repository.moveFolder(folder.id(), 1);
            repository.moveToFolder(world("a"), folder.id(), -1);
            repository.moveToFolder(world("b"), folder.id(), -1);

            assertEquals(List.of(world("before"), "F:Survival", world("after")), rootIds());

            assertTrue(repository.deleteFolder(folder.id()));

            assertEquals(List.of(world("before"), world("a"), world("b"), world("after")), rootIds());
            assertTrue(repository.folder(folder.id()).isEmpty());
        }
    }

    @Nested
    @DisplayName("membership")
    class Membership {

        @Test
        @DisplayName("an item lives in exactly one folder")
        void movingBetweenFoldersDetaches() {
            Folder a = repository.createFolder("A");
            Folder b = repository.createFolder("B");

            repository.moveToFolder(world("w"), a.id(), -1);
            repository.moveToFolder(world("w"), b.id(), -1);

            assertTrue(a.items().isEmpty());
            assertEquals(List.of(world("w")), b.items());
            assertEquals(b, repository.folderContaining(world("w")).orElseThrow());
        }

        @Test
        @DisplayName("an item in a folder is not also at the root")
        void folderedItemLeavesRoot() {
            repository.sync(List.of(world("w")), true);
            assertEquals(List.of(world("w")), rootIds());

            Folder folder = repository.createFolder("A");
            repository.moveToFolder(world("w"), folder.id(), -1);

            assertEquals(List.of("F:A"), rootIds());
        }

        @Test
        @DisplayName("pulling an item back out puts it at the requested root position")
        void moveBackToRoot() {
            repository.sync(List.of(world("a"), world("b")), true);
            Folder folder = repository.createFolder("A");
            repository.moveToFolder(world("b"), folder.id(), -1);

            repository.moveToRoot(world("b"), 0);

            assertEquals(List.of(world("b"), "F:A", world("a")), rootIds());
            assertTrue(repository.folderContaining(world("b")).isEmpty());
        }

        @Test
        @DisplayName("ids of another type are refused")
        void rejectsForeignIds() {
            Folder folder = repository.createFolder("A");
            assertFalse(repository.moveToFolder("server:example.com:25565", folder.id(), -1));
            assertFalse(repository.moveToRoot("pack:file/x.zip", 0));
            assertTrue(folder.items().isEmpty());
        }

        @Test
        @DisplayName("items can be reordered inside a folder")
        void reorderInsideFolder() {
            Folder folder = repository.createFolder("A");
            repository.moveToFolder(world("a"), folder.id(), -1);
            repository.moveToFolder(world("b"), folder.id(), -1);
            repository.moveToFolder(world("c"), folder.id(), -1);

            repository.reorderInFolder(folder.id(), world("c"), 0);

            assertEquals(List.of(world("c"), world("a"), world("b")), folder.items());
        }
    }

    @Nested
    @DisplayName("sync with the vanilla list")
    class Sync {

        @Test
        @DisplayName("folders keep their position while new items appear beside their neighbour")
        void keepsOrderAndPlacesNewItems() {
            repository.sync(List.of(world("a"), world("b")), true);
            Folder folder = repository.createFolder("F");
            repository.moveFolder(folder.id(), 1);
            assertEquals(List.of(world("a"), "F:F", world("b")), rootIds());

            // "c" is new and sits after "b" in the vanilla list.
            repository.sync(List.of(world("a"), world("b"), world("c")), true);

            assertEquals(List.of(world("a"), "F:F", world("b"), world("c")), rootIds());
        }

        @Test
        @DisplayName("an item that is new and first in vanilla order goes to the top")
        void newestFirstGoesToTop() {
            repository.sync(List.of(world("a"), world("b")), true);
            // Worlds sort most-recently-played first, so a fresh world arrives at index 0.
            repository.sync(List.of(world("fresh"), world("a"), world("b")), true);

            assertEquals(List.of(world("fresh"), world("a"), world("b")), rootIds());
        }

        @Test
        @DisplayName("deleted worlds are pruned from folders")
        void prunesOrphans() {
            Folder folder = repository.createFolder("F");
            repository.moveToFolder(world("gone"), folder.id(), -1);
            repository.moveToFolder(world("here"), folder.id(), -1);

            repository.sync(List.of(world("here")), true);

            assertEquals(List.of(world("here")), folder.items());
        }

        @Test
        @DisplayName("a partial snapshot never prunes")
        void partialSnapshotKeepsReferences() {
            Folder folder = repository.createFolder("F");
            repository.moveToFolder(world("a"), folder.id(), -1);

            repository.sync(List.of(), false);

            assertEquals(List.of(world("a")), folder.items());
        }

        @Test
        @DisplayName("an item listed in two folders is kept only by the first")
        void repairsCrossFolderDuplicate() {
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            FolderConfig config = new FolderConfig(1, List.of(
                    new Folder(first, "A", FolderIcon.DEFAULT, false, List.of(world("w"))),
                    new Folder(second, "B", FolderIcon.DEFAULT, false, List.of(world("w")))
            ), List.of());
            FolderRepository broken = new FolderRepository(FolderType.WORLDS, config);

            broken.sync(List.of(world("w")), true);

            assertEquals(List.of(world("w")), broken.folder(first).orElseThrow().items());
            assertTrue(broken.folder(second).orElseThrow().items().isEmpty());
        }

        @Test
        @DisplayName("duplicate folder ids are collapsed")
        void repairsDuplicateFolderIds() {
            UUID id = UUID.randomUUID();
            FolderConfig config = new FolderConfig(1, List.of(
                    new Folder(id, "A", FolderIcon.DEFAULT, false, List.of()),
                    new Folder(id, "B", FolderIcon.DEFAULT, false, List.of())
            ), List.of());
            FolderRepository broken = new FolderRepository(FolderType.WORLDS, config);

            broken.sync(List.of(), true);

            assertEquals(1, broken.folders().size());
            assertEquals("A", broken.folders().get(0).name());
        }

        @Test
        @DisplayName("a folder missing from the root order is re-added, not lost")
        void reattachesFolderWithoutRootToken() {
            UUID id = UUID.randomUUID();
            FolderConfig config = new FolderConfig(1, List.of(
                    new Folder(id, "A", FolderIcon.DEFAULT, false, List.of())
            ), List.of());
            FolderRepository broken = new FolderRepository(FolderType.WORLDS, config);

            broken.sync(List.of(), true);

            assertEquals(List.of("F:A"), broken.rootEntries().stream()
                    .map(e -> "F:" + ((RootEntry.FolderRef) e).folder().name()).toList());
        }

        @Test
        @DisplayName("a settled model reports no further changes")
        void isIdempotent() {
            repository.sync(List.of(world("a"), world("b")), true);
            Folder folder = repository.createFolder("F");
            repository.moveToFolder(world("a"), folder.id(), -1);
            repository.sync(List.of(world("a"), world("b")), true);

            assertFalse(repository.sync(List.of(world("a"), world("b")), true),
                    "a second sync over the same list should be a no-op");
        }
    }

    @Nested
    @DisplayName("dirty tracking")
    class Dirty {

        @Test
        void mutationsMarkDirty() {
            assertFalse(repository.isDirty());
            Folder folder = repository.createFolder("A");
            assertTrue(repository.isDirty());

            repository.markClean();
            repository.setExpanded(folder.id(), true);
            assertTrue(repository.isDirty());
        }

        @Test
        @DisplayName("a no-op does not mark the model dirty")
        void noOpStaysClean() {
            Folder folder = repository.createFolder("A");
            repository.setExpanded(folder.id(), true);
            repository.markClean();

            repository.setExpanded(folder.id(), true);
            repository.renameFolder(folder.id(), "A");

            assertFalse(repository.isDirty());
        }
    }
}
