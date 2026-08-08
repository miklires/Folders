package dev.miklires.folders.client.integration;

/**
 * Implemented by the list mixins so a screen can reach its list's controller
 * without knowing which of the three it is — that is all the "Create folder"
 * button needs.
 */
public interface FoldersControllerHost {

    FolderListController<?> folders$controller();
}
