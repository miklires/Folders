package dev.miklires.folders.client.ui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The empty strip at the bottom-left of a vanilla screen, and the widgets Folders puts in it.
 *
 * <p>How much room is there depends on the screen and the GUI scale: the footer is laid out by
 * {@code HeaderAndFooterLayout}, which centres its rows, so the gutter is whatever is left beside
 * them. It is measured rather than assumed — an earlier version assumed, and landed the button
 * squarely on "Join Server".
 *
 * <p>Widgets registered here are re-placed when the screen is resized. They are not part of the
 * vanilla layout, so nothing else would move them, and a button anchored to the bottom edge of a
 * window that has just changed height is otherwise left floating in the middle of the list.
 */
public final class Gutter {

    /** Inset from the screen edge. */
    public static final int MARGIN = 6;

    public static final int DEFAULT_WIDTH = 100;
    public static final int DEFAULT_HEIGHT = 20;

    /** Below this the gutter is too narrow for a legible button, and everything goes top-left. */
    private static final int MIN_WIDTH = 56;

    /** How far up from the bottom counts as "the footer" when measuring. */
    private static final int FOOTER_BAND = 64;

    private static final int ROW_STEP = DEFAULT_HEIGHT + 2;

    private record Anchor(int row, int minWidth) {
    }

    /**
     * Weak so a closed screen's widgets are collected. Keyed by widget rather than by screen
     * because a screen can rebuild its widgets without Folders being told.
     */
    private static final Map<AbstractWidget, Anchor> ANCHORED = new WeakHashMap<>();

    private Gutter() {
    }

    public record Slot(int x, int y, int width, int height) {
    }

    /** Where the {@code row}-th widget goes, counting up from the bottom of the screen. */
    public static Slot slot(Screen screen, int row, int minWidth) {
        int gutter = footerLeft(screen) - 2 * MARGIN;
        if (gutter < MIN_WIDTH) {
            // No room beside the footer. The top-left corner is empty on every screen at every
            // scale, so that is where these go instead.
            return new Slot(MARGIN, MARGIN + row * ROW_STEP, Math.max(DEFAULT_WIDTH, minWidth), DEFAULT_HEIGHT);
        }
        int width = Math.max(Math.min(DEFAULT_WIDTH, gutter), minWidth);
        return new Slot(MARGIN, screen.height - MARGIN - DEFAULT_HEIGHT - row * ROW_STEP, width, DEFAULT_HEIGHT);
    }

    public static Slot slot(Screen screen, int row) {
        return slot(screen, row, 0);
    }

    /** Remembers where a widget belongs so a resize can put it back. */
    public static <T extends AbstractWidget> T anchor(T widget, int row, int minWidth) {
        ANCHORED.put(widget, new Anchor(row, minWidth));
        return widget;
    }

    /** Re-places every anchored widget on this screen. Call from {@code repositionElements}. */
    public static void reposition(Screen screen) {
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget)) {
                continue;
            }
            Anchor anchor = ANCHORED.get(widget);
            if (anchor == null) {
                continue;
            }
            Slot slot = slot(screen, anchor.row(), anchor.minWidth());
            widget.setX(slot.x());
            widget.setY(slot.y());
            widget.setWidth(slot.width());
        }
    }

    /**
     * Left edge of the leftmost footer widget, or the screen width if there is none.
     *
     * <p>Folders' own widgets are skipped. They sit in the gutter themselves, so counting them
     * would measure the gutter as being exactly as wide as the margin and send everything back to
     * the corner on the second frame.
     */
    private static int footerLeft(Screen screen) {
        int footerTop = screen.height - FOOTER_BAND;
        int leftmost = screen.width;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget
                    && !ANCHORED.containsKey(widget)
                    && widget.getY() >= footerTop) {
                leftmost = Math.min(leftmost, widget.getX());
            }
        }
        return leftmost;
    }
}
