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
}
