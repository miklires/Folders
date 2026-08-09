package dev.miklires.folders.client.integration;

import java.util.Optional;

/**
 * The folder controller for the list currently on screen.
 *
 * <p>A vanilla row holds its list in a private field, and reaching it would mean shadowing a name
 * that has already moved once in this version. Nothing needs to be looked up, though: the list sets
 * this as it draws, so by the time a click arrives the value is the list that click landed in.
 *
 * <p>Single-valued because the two screens Folders extends have one folder list each. The pack
 * screen has two, and will need this keyed by list when pack folders land.
 */
public final class ScreenFolders {

    private static FolderListController<?> active;

    private ScreenFolders() {
    }

    /** Called from the list's draw, every frame. */
    static void setActive(FolderListController<?> controller) {
        active = controller;
    }

    public static Optional<FolderListController<?>> current() {
        return Optional.ofNullable(active);
    }
}
