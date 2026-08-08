package dev.miklires.folders.core.drag;

/**
 * Somewhere a payload can land: a folder row, a gap between two rows, or the
 * root list itself.
 *
 * <p>Implementations live in the integration layer because only it knows screen
 * coordinates; the manager just asks whether the pointer is inside and whether
 * the payload is welcome.
 */
public interface DropTarget {

    /**
     * @return false when this payload cannot land here — a folder must not accept
     *         another folder (no nesting in v1, §10), and no target accepts a
     *         payload of a different {@code FolderType}.
     */
    boolean accepts(DragPayload payload);

    boolean containsPoint(double mouseX, double mouseY);

    /** Performs the move. Only called when {@link #accepts} returned true. */
    void drop(DragPayload payload);

    /**
     * Higher wins when targets overlap — an insertion gap sitting on top of a
     * folder row should take the drop before the folder does.
     */
    default int priority() {
        return 0;
    }

    /** Whether this target should be highlighted while hovered (§17). */
    default boolean highlightWhenHovered() {
        return true;
    }
}
