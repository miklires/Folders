package dev.miklires.folders.client.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The "Create folder" button each supported screen gains.
 *
 * <p>An ordinary vanilla {@code Button}, deliberately unstyled: a folder is meant to look like a
 * feature the game shipped with, and so is the button that makes one.
 *
 * <p>The button finds its list by walking the screen's children for the one Folders attached a
 * controller to, rather than by having each screen mixin shadow a private field. Those field names
 * ({@code levelList}, {@code serverSelectionList}) are exactly the kind of thing that gets renamed
 * between versions, and a wrong guess is a hard crash at mixin-apply time rather than a quiet
 * failure. Walking the children asks the question that actually matters — "which widget here is a
 * folder list?" — and cannot be wrong.
 */
public final class CreateFolderButton {

    public static final int DEFAULT_WIDTH = 100;
    public static final int DEFAULT_HEIGHT = 20;

    /**
     * Inset from the top-left corner.
     *
     * <p>Both screens lay their footer out with {@code HeaderAndFooterLayout}, which owns those
     * rows and repositions them on every resize. A free-floating widget dropped into that area
     * lands on top of "Play Selected World" and "Join Server" — which is exactly what happened.
     * The corner is the one place on all three screens that vanilla leaves empty, so the button
     * goes there until it is added to the layout properly rather than beside it.
     */
    public static final int MARGIN = 6;

    private CreateFolderButton() {
    }

    public static Button create(Screen screen, int x, int y, int width, int height) {
        return Button.builder(Component.translatable("folders.create"), ignored -> onPressed(screen))
                .bounds(x, y, width, height)
                .tooltip(Tooltip.create(Component.translatable("folders.create.tooltip")))
                .build();
    }

    private static void onPressed(Screen screen) {
        Folders.guarded("creating a folder", () -> {
            Optional<FoldersControllerHost> host = host(screen);
            if (host.isEmpty()) {
                // Says so rather than doing nothing: a button that silently no-ops is the hardest
                // kind of failure to report.
                Folders.LOGGER.warn("No folder list found on {}; Create folder did nothing",
                        screen.getClass().getName());
                return;
            }
            FolderListController<?> controller = host.get().folders$controller();
            // Created, saved, and put straight into rename mode.
            controller.createFolder();
        });
    }

    /** The first child widget Folders has attached a controller to, if the screen has one. */
    private static Optional<FoldersControllerHost> host(Screen screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof FoldersControllerHost host) {
                return Optional.of(host);
            }
        }
        return Optional.empty();
    }
}
