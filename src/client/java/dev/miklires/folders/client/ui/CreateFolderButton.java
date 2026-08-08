package dev.miklires.folders.client.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * The "Create folder" button each supported screen gains.
 *
 * <p>An ordinary vanilla {@code Button}, deliberately unstyled: a folder is meant to look like a
 * feature the game shipped with, and so is the button that makes one.
 */
public final class CreateFolderButton {

    public static final int DEFAULT_WIDTH = 100;
    public static final int DEFAULT_HEIGHT = 20;

    private CreateFolderButton() {
    }

    /**
     * @param list the widget whose mixin carries the controller; the button is
     *             disabled if Folders somehow did not attach to it
     */
    public static Button create(AbstractSelectionList<?> list, int x, int y, int width, int height) {
        Button button = Button.builder(
                        Component.translatable("folders.create"),
                        ignored -> onPressed(list))
                .bounds(x, y, width, height)
                .tooltip(Tooltip.create(Component.translatable("folders.create.tooltip")))
                .build();
        button.active = list instanceof FoldersControllerHost;
        return button;
    }

    private static void onPressed(AbstractSelectionList<?> list) {
        if (!(list instanceof FoldersControllerHost host)) {
            return;
        }
        Folders.guarded("creating a folder", () -> {
            FolderListController<?> controller = host.folders$controller();
            // Created, saved and put straight into rename mode.
            controller.createFolder();
        });
    }
}
