package dev.miklires.folders.client.integration;

import dev.miklires.folders.core.data.Folder;

/**
 * Marks an entry as one of Folders' own rows rather than a vanilla one.
 *
 * <p>Rebuilds feed the widget's current children back in as the "vanilla" list, and by then that
 * list already contains folder rows from the previous pass. Without a way to tell them apart they
 * would be treated as unidentifiable vanilla entries and appended a second time, so a folder would
 * multiply on every refresh.
 */
public interface FolderRow {

    Folder folder();

    /**
     * Top of the row in screen space, as the list actually placed it.
     *
     * <p>Read off the widget rather than computed, because those are two different numbers. The
     * view model lays folders out in its own coordinate space; the list then positions rows by
     * walking its children and accumulating heights, and applies its own scroll. Asking the row
     * where it ended up is the only answer that stays right while the list is scrolled — which is
     * exactly when a drop landing on the wrong folder would be hardest to notice.
     */
    int rowTop();

    int rowBottom();
}
