package dev.miklires.folders.core.drag;

import dev.miklires.folders.core.data.FolderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragManagerTest {

    /** A rectangle that records what landed on it. */
    private static final class TestTarget implements DropTarget {
        final int x1, y1, x2, y2;
        final boolean acceptEverything;
        final int priority;
        DragPayload received;

        TestTarget(int x1, int y1, int x2, int y2, boolean acceptEverything, int priority) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.acceptEverything = acceptEverything;
            this.priority = priority;
        }

        static TestTarget at(int x1, int y1, int x2, int y2) {
            return new TestTarget(x1, y1, x2, y2, true, 0);
        }

        @Override
        public boolean accepts(DragPayload payload) {
            return acceptEverything;
        }

        @Override
        public boolean containsPoint(double mouseX, double mouseY) {
            return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
        }

        @Override
        public void drop(DragPayload payload) {
            received = payload;
        }

        @Override
        public int priority() {
            return priority;
        }
    }

    private DragManager manager;
    private DragPayload.Item payload;

    @BeforeEach
    void setUp() {
        manager = new DragManager();
        payload = new DragPayload.Item(FolderType.WORLDS, "world:a", "World A", null);
    }

    @Test
    @DisplayName("a short movement stays a click (§16)")
    void shortMovementIsAClick() {
        manager.press(payload, 100, 100, 0L);
        assertFalse(manager.move(102, 101));
        assertEquals(DragState.PRESSED, manager.state());

        assertFalse(manager.release(102, 101), "release should let the vanilla click through");
        assertEquals(DragState.IDLE, manager.state());
    }

    @Test
    @DisplayName("passing the threshold starts a real drag")
    void thresholdStartsDrag() {
        manager.press(payload, 100, 100, 0L);
        assertTrue(manager.move(100, 100 + (int) DragManager.DRAG_THRESHOLD));
        assertEquals(DragState.DRAGGING, manager.state());
        assertTrue(manager.isDragging());
    }

    @Test
    @DisplayName("a drop hands the payload to the target under the cursor")
    void dropsOnTarget() {
        TestTarget target = TestTarget.at(0, 200, 300, 240);
        manager.press(payload, 100, 100, 0L);
        manager.setTargets(List.of(target));
        manager.move(100, 220);

        assertTrue(manager.release(100, 220));

        assertSame(payload, target.received);
        assertEquals(DragState.IDLE, manager.state());
    }

    @Test
    @DisplayName("releasing over nothing drops nothing and clears state")
    void releaseOverNothing() {
        TestTarget target = TestTarget.at(0, 200, 300, 240);
        manager.press(payload, 100, 100, 0L);
        manager.setTargets(List.of(target));
        manager.move(100, 500);

        assertFalse(manager.release(100, 500));

        assertNull(target.received);
        assertEquals(DragState.IDLE, manager.state());
    }

    @Test
    @DisplayName("a target that refuses the payload is never highlighted and never receives it (§17)")
    void refusingTargetIsSkipped() {
        TestTarget refuses = new TestTarget(0, 200, 300, 240, false, 0);
        manager.press(payload, 100, 100, 0L);
        manager.setTargets(List.of(refuses));
        manager.move(100, 220);

        assertTrue(manager.hoveredTarget().isEmpty());
        assertFalse(manager.release(100, 220));
        assertNull(refuses.received);
    }

    @Test
    @DisplayName("overlapping targets resolve by priority, so a gap beats the folder under it")
    void priorityWins() {
        TestTarget folder = new TestTarget(0, 200, 300, 240, true, 0);
        TestTarget gap = new TestTarget(0, 235, 300, 245, true, 10);
        manager.press(payload, 100, 100, 0L);
        manager.setTargets(List.of(folder, gap));
        manager.move(100, 238);

        manager.release(100, 238);

        assertSame(payload, gap.received);
        assertNull(folder.received);
    }

    @Test
    @DisplayName("cancel abandons the gesture (§40)")
    void cancelClearsEverything() {
        TestTarget target = TestTarget.at(0, 200, 300, 240);
        manager.press(payload, 100, 100, 0L);
        manager.setTargets(List.of(target));
        manager.move(100, 220);

        manager.cancel();

        assertEquals(DragState.IDLE, manager.state());
        assertTrue(manager.payload().isEmpty());
        assertFalse(manager.release(100, 220));
        assertNull(target.received);
    }

    @Test
    @DisplayName("the hovered target tracks the cursor while dragging")
    void hoverFollowsCursor() {
        TestTarget upper = TestTarget.at(0, 200, 300, 240);
        TestTarget lower = TestTarget.at(0, 240, 300, 280);
        manager.press(payload, 100, 100, 0L);
        manager.setTargets(List.of(upper, lower));

        manager.move(100, 210);
        assertTrue(manager.isHovered(upper));

        manager.move(100, 270);
        assertTrue(manager.isHovered(lower));
        assertFalse(manager.isHovered(upper));
    }

    @Test
    @DisplayName("a folder payload carries its id so a folder can be reordered too (§61)")
    void folderPayload() {
        UUID id = UUID.randomUUID();
        DragPayload.FolderHandle handle = new DragPayload.FolderHandle(FolderType.SERVERS, id, "Servers");
        TestTarget target = TestTarget.at(0, 0, 100, 100);

        manager.press(handle, 10, 10, 0L);
        manager.setTargets(List.of(target));
        manager.move(10, 40);
        manager.release(10, 40);

        assertSame(handle, target.received);
    }
}
