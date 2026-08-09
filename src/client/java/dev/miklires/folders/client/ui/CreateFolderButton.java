package dev.miklires.folders.client.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import net.minecraft.client.gui.components.AbstractWidget;
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

    /** Inset from the screen edge. */
    public static final int MARGIN = 6;

    /** Below this the button is too small to read, and it goes back to the top-left corner. */
    private static final int MIN_WIDTH = 56;

    /** How far up from the bottom counts as "the footer" when measuring the free gutter. */
    private static final int FOOTER_BAND = 64;

    private CreateFolderButton() {
    }

    /**
     * Puts the button in the bottom-left gutter, beside the footer rather than on top of them.
     *
     * <p>The gutter is measured, not assumed: the footer is laid out by
     * {@code HeaderAndFooterLayout}, which centres its rows and re-centres them on every resize, so
     * how much room is left at the left edge depends on the GUI scale and on which screen this is.
     * Asking the footer widgets where they actually start is the only number that is right on all
     * of them — an earlier version guessed, and landed the button squarely on "Join Server".
     *
     * <p>If the gutter is too narrow to hold a legible button, it goes back to the top-left corner,
     * which is empty on every screen at every scale.
     */
    public static Button place(Screen screen) {
        int gutter = footerLeft(screen) - 2 * MARGIN;
        if (gutter < MIN_WIDTH) {
            return create(screen, MARGIN, MARGIN, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        int width = Math.min(DEFAULT_WIDTH, gutter);
        return create(screen, MARGIN, screen.height - MARGIN - DEFAULT_HEIGHT, width, DEFAULT_HEIGHT);
    }

    /** Left edge of the leftmost footer widget, or the screen width if the footer is empty. */
    private static int footerLeft(Screen screen) {
        int footerTop = screen.height - FOOTER_BAND;
        int leftmost = screen.width;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getY() >= footerTop) {
                leftmost = Math.min(leftmost, widget.getX());
            }
        }
        return leftmost;
    }

    public static Button create(Screen screen, int x, int y, int width, int height) {
        // A squeezed button gets the short label rather than text overflowing its own border.
        Component label = Component.translatable(
                width < DEFAULT_WIDTH ? "folders.create.short" : "folders.create");
        return Button.builder(label, ignored -> onPressed(screen))
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
