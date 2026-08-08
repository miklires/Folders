package dev.miklires.folders.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.integration.FolderListController;
import dev.miklires.folders.integration.FoldersControllerHost;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;

/**
 * The "Create folder" button each supported screen gains (§11).
 *
 * <p>An ordinary vanilla {@code ButtonWidget} — no custom styling, because a
 * folder is meant to look like a feature Minecraft shipped (§69).
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
    public static ButtonWidget create(EntryListWidget<?> list, int x, int y, int width, int height) {
        ButtonWidget button = ButtonWidget.builder(
                        Text.translatable("folders.create"),
                        ignored -> onPressed(list))
                .dimensions(x, y, width, height)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("folders.create.tooltip")))
                .build();
        button.active = list instanceof FoldersControllerHost;
        return button;
    }

    private static void onPressed(EntryListWidget<?> list) {
        if (!(list instanceof FoldersControllerHost host)) {
            return;
        }
        Folders.guarded("creating a folder", () -> {
            FolderListController<?> controller = host.folders$controller();
            // Created, saved and put straight into rename mode (§11).
            controller.createFolder();
        });
    }
}
