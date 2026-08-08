package dev.miklires.folders.core.drag;

/**
 * States
 *
 * <p>{@link #PRESSED} is the one that matters: between the button going down and
 * the pointer travelling {@link DragManager#DRAG_THRESHOLD} pixels, nothing has
 * happened yet. That gap is what keeps an ordinary click an ordinary click.
 */
public enum DragState {
    IDLE,
    PRESSED,
    DRAGGING,
    DROPPING,
    CANCELLED
}
