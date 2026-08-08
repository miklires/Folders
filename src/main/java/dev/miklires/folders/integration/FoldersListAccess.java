package dev.miklires.folders.integration;

/**
 * Implemented on vanilla list widgets by {@code EntryListWidgetMixin}.
 *
 * <p>A list with no layout installed behaves exactly as it always did — this is
 * what keeps every other list in the game (options, language, statistics…)
 * completely unaffected (§56).
 */
public interface FoldersListAccess {

    void folders$setLayout(FolderRowLayout layout);

    FolderRowLayout folders$layout();

    default boolean folders$hasLayout() {
        return folders$layout() != null;
    }
}
