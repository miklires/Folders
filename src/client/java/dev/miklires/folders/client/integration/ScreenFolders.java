package dev.miklires.folders.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

/**
 * Finds the folder controller for whatever screen is currently open.
 *
 * <p>A vanilla row has no reference to the list holding it that Folders can reach without shadowing
 * a private field, and those fields are precisely what moves between versions. The open screen does
 * know its widgets, so the question is asked there instead.
 */
public final class ScreenFolders {

    private ScreenFolders() {
    }

    public static Optional<FolderListController<?>> current() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return Optional.empty();
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof FoldersControllerHost host) {
                return Optional.of(host.folders$controller());
            }
        }
        return Optional.empty();
    }
}
