package dev.miklires.folders.core.drag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The one drag-and-drop implementation, shared by all three screens.
 *
 * <p>It owns nothing Minecraft-shaped: screens feed it pointer events and a list
 * of {@link DropTarget}s, and it answers "is this a drag yet?", "what is under
 * the cursor?" and "did the drop happen?". That is what makes it testable and
 * what stops each screen from growing its own subtly different version.
 */
public final class DragManager {

    /**: below this the gesture is still a click. */
    public static final double DRAG_THRESHOLD = 5.0;

    private DragState state = DragState.IDLE;
    private DragPayload payload;

    private double startX;
    private double startY;
    private double mouseX;
    private double mouseY;
    private long startedAtMs;

    private final List<DropTarget> targets = new ArrayList<>();
    private DropTarget hovered;

    // ------------------------------------------------------------------
    // Pointer events
    // ------------------------------------------------------------------

    /**
     * Records a press over a draggable row. Nothing is dragging yet — the click
     * still belongs to the vanilla handler until the pointer moves far enough.
     */
    public void press(DragPayload payload, double mouseX, double mouseY, long nowMs) {
        this.payload = payload;
        this.state = DragState.PRESSED;
        this.startX = mouseX;
        this.startY = mouseY;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.startedAtMs = nowMs;
        this.hovered = null;
    }

    /**
     * @return true once the gesture has become a real drag, meaning the caller
     *         should suppress the vanilla click.
     */
    public boolean move(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        if (state == DragState.PRESSED) {
            double dx = mouseX - startX;
            double dy = mouseY - startY;
            if (dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD) {
                state = DragState.DRAGGING;
            }
        }
        if (state == DragState.DRAGGING) {
            hovered = findTarget(mouseX, mouseY).orElse(null);
            return true;
        }
        return false;
    }

    /**
     * Ends the gesture.
     *
     * @return true if a drop was performed. False means this was a plain click (or
     *         a drag that ended over nothing), and the caller should let the
     *         vanilla action run.
     */
    public boolean release(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        if (state != DragState.DRAGGING) {
            reset();
            return false;
        }

        DropTarget target = findTarget(mouseX, mouseY).orElse(null);
        if (target == null || payload == null) {
            state = DragState.CANCELLED;
            reset();
            return false;
        }

        state = DragState.DROPPING;
        try {
            target.drop(payload);
        } finally {
            reset();
        }
        return true;
    }

    /** Escape, or the screen closing mid-drag. */
    public void cancel() {
        if (state != DragState.IDLE) {
            state = DragState.CANCELLED;
        }
        reset();
    }

    private void reset() {
        state = DragState.IDLE;
        payload = null;
        hovered = null;
        targets.clear();
    }

    // ------------------------------------------------------------------
    // Targets
    // ------------------------------------------------------------------

    /**
     * Replaces the candidate targets. Screens rebuild this every frame from the
     * rows currently on screen, which keeps it correct while the list scrolls or
     * a folder animates open.
     */
    public void setTargets(List<DropTarget> newTargets) {
        targets.clear();
        if (newTargets != null) {
            targets.addAll(newTargets);
        }
        if (state == DragState.DRAGGING) {
            hovered = findTarget(mouseX, mouseY).orElse(null);
        }
    }

    private Optional<DropTarget> findTarget(double x, double y) {
        if (payload == null) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(target -> target.accepts(payload))
                .filter(target -> target.containsPoint(x, y))
                .max(Comparator.comparingInt(DropTarget::priority));
    }

    // ------------------------------------------------------------------
    // Queries for the renderer
    // ------------------------------------------------------------------

    public DragState state() {
        return state;
    }

    public boolean isDragging() {
        return state == DragState.DRAGGING;
    }

    /** True while a press is pending — used to hold off hover effects. */
    public boolean isPressed() {
        return state == DragState.PRESSED;
    }

    public Optional<DragPayload> payload() {
        return Optional.ofNullable(payload);
    }

    /** The target that would receive a drop right now, for the highlight in */
    public Optional<DropTarget> hoveredTarget() {
        return Optional.ofNullable(hovered);
    }

    public boolean isHovered(DropTarget target) {
        return hovered != null && hovered == target;
    }

    public double mouseX() {
        return mouseX;
    }

    public double mouseY() {
        return mouseY;
    }

    public long startedAtMs() {
        return startedAtMs;
    }
}
