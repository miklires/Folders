package dev.miklires.folders.core.data;

/**
 * One row of the top level: either a folder or a loose item. This is the shape
 * the UI consumes, so the UI never has to know about the token encoding used in
 * {@link FolderConfig#root()}.
 */
public sealed interface RootEntry {

    record FolderRef(Folder folder) implements RootEntry {
    }

    record ItemRef(String itemId) implements RootEntry {
    }
}
