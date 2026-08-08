package dev.miklires.folders.core.view;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.core.view.AnimationSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderViewModelTest {

    private static final int ROW = 36;

    /** Stands in for the client config, which lives on the other side of the Minecraft boundary. */
    private static final class TestSettings implements AnimationSettings {
        boolean animations = true;

        @Override
        public long scaledDuration(long baseMs) {
            return animations ? baseMs : 0L;
        }
    }

    private FolderRepository repository;
    private TestSettings settings;
    private FolderViewModel model;

    @BeforeEach
    void setUp() {
        repository = FolderRepository.empty(FolderType.WORLDS);
        settings = new TestSettings();
        model = new FolderViewModel(repository, settings);
        model.setRowHeight(ROW);
        model.setChildIndent(12);
    }

    private static Set<String> available(String... ids) {
        return new LinkedHashSet<>(List.of(ids));
    }

    @Test
    @DisplayName("a closed folder contributes one row and hides its contents")
    void closedFolderIsOneRow() {
        repository.sync(List.of("world:a", "world:b"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);

        List<DisplayRow> rows = model.layout(available("world:a", "world:b"));

        assertEquals(2, rows.size());
        assertEquals(2 * ROW, model.totalHeight());
    }

    @Test
    @DisplayName("an open folder shows its children indented, below it")
    void openFolderShowsIndentedChildren() {
        repository.sync(List.of("world:a", "world:b"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);
        settings.animations = false;
        model.setExpanded(folder.id(), true);

        List<DisplayRow> rows = model.layout(available("world:a", "world:b"));

        assertEquals(3, rows.size());
        DisplayRow.FolderRow folderRow = (DisplayRow.FolderRow) rows.get(0);
        DisplayRow.ItemRow child = (DisplayRow.ItemRow) rows.get(1);
        DisplayRow.ItemRow sibling = (DisplayRow.ItemRow) rows.get(2);

        assertEquals(0, folderRow.y());
        assertEquals(ROW, child.y());
        assertEquals(12, child.indent());
        assertEquals(folder.id(), child.owner());
        assertEquals(2 * ROW, sibling.y(), "the row below must be pushed down by the open folder");
        assertEquals(0, sibling.indent());
        assertEquals(3 * ROW, model.totalHeight());
    }

    @Test
    @DisplayName("mid-animation the list is part-way open, not snapped")
    void animationProducesIntermediateHeights() {
        repository.sync(List.of("world:a", "world:b", "world:c"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);
        repository.moveToFolder("world:b", folder.id(), -1);

        model.tick(0L);
        model.setExpanded(folder.id(), true);
        model.tick(FolderViewModel.EXPAND_DURATION_MS / 2);
        model.layout(available("world:a", "world:b", "world:c"));

        int closed = 2 * ROW;
        int open = 4 * ROW;
        assertTrue(model.totalHeight() > closed && model.totalHeight() < open,
                "expected a height between " + closed + " and " + open + ", got " + model.totalHeight());

        model.tick(FolderViewModel.EXPAND_DURATION_MS * 2);
        model.layout(available("world:a", "world:b", "world:c"));
        assertEquals(open, model.totalHeight());
        assertFalse(model.isAnimating());
    }

    @Test
    @DisplayName("children entering the reveal band fade in")
    void childrenFadeIn() {
        repository.sync(List.of("world:a", "world:b"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);
        repository.moveToFolder("world:b", folder.id(), -1);

        model.tick(0L);
        model.setExpanded(folder.id(), true);
        model.tick(20L);
        List<DisplayRow> rows = model.layout(available("world:a", "world:b"));

        DisplayRow.ItemRow first = (DisplayRow.ItemRow) rows.get(1);
        assertTrue(first.alpha() > 0.0f && first.alpha() < 1.0f,
                "a partially revealed row should be partially transparent, was " + first.alpha());
        assertTrue(first.needsClip());
    }

    @Test
    @DisplayName("no child is ever drawn below the band it belongs to")
    void childrenStayInsideTheBand() {
        repository.sync(List.of("world:a", "world:b", "world:c"), true);
        Folder folder = repository.createFolder("F");
        for (String id : List.of("world:a", "world:b", "world:c")) {
            repository.moveToFolder(id, folder.id(), -1);
        }
        model.tick(0L);
        model.setExpanded(folder.id(), true);

        for (long t = 0; t <= FolderViewModel.EXPAND_DURATION_MS; t += 7) {
            model.tick(t);
            for (DisplayRow row : model.layout(available("world:a", "world:b", "world:c"))) {
                if (row instanceof DisplayRow.ItemRow item && item.isInFolder()) {
                    assertTrue(item.y() < item.clipBottom(),
                            "row at y=" + item.y() + " starts below its band bottom " + item.clipBottom());
                    assertTrue(item.clipBottom() <= model.totalHeight());
                }
            }
        }
    }

    @Test
    @DisplayName("a deleted world is left out of the layout rather than drawn broken")
    void missingItemsAreSkipped() {
        repository.sync(List.of("world:a", "world:b"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);
        repository.moveToFolder("world:b", folder.id(), -1);
        settings.animations = false;
        model.setExpanded(folder.id(), true);

        List<DisplayRow> rows = model.layout(available("world:a"));

        assertEquals(2, rows.size());
        assertEquals("world:a", ((DisplayRow.ItemRow) rows.get(1)).itemId());
    }

    @Test
    @DisplayName("an empty open folder adds no rows, it just says so on its own line")
    void emptyFolderAddsNoRows() {
        Folder folder = repository.createFolder("F");
        settings.animations = false;
        model.setExpanded(folder.id(), true);

        assertEquals(1, model.layout(Set.of()).size());
        assertEquals(ROW, model.totalHeight());
    }

    @Test
    @DisplayName("settledHeight predicts where the list will end up, for scroll correction")
    void settledHeightIgnoresAnimation() {
        repository.sync(List.of("world:a", "world:b"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);
        model.tick(0L);
        model.setExpanded(folder.id(), true);
        model.tick(10L);

        Set<String> items = available("world:a", "world:b");
        model.layout(items);

        assertEquals(3 * ROW, model.settledHeight(items));
        assertTrue(model.totalHeight() < model.settledHeight(items));
    }

    @Test
    @DisplayName("with animations off the state applies instantly")
    void animationsCanBeDisabled() {
        repository.sync(List.of("world:a"), true);
        Folder folder = repository.createFolder("F");
        repository.moveToFolder("world:a", folder.id(), -1);
        settings.animations = false;

        model.tick(0L);
        model.setExpanded(folder.id(), true);
        model.layout(available("world:a"));

        assertFalse(model.isAnimating());
        assertEquals(2 * ROW, model.totalHeight());
    }

    @Test
    @DisplayName("expanded state is persisted through the repository")
    void toggleWritesThroughToTheModel() {
        Folder folder = repository.createFolder("F");
        repository.markClean();

        model.toggle(folder.id());

        assertTrue(folder.expanded());
        assertTrue(repository.isDirty());

        model.toggle(folder.id());
        assertFalse(folder.expanded());
    }
}
