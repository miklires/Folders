package dev.miklires.folders.core.animation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTest {

    @Test
    @DisplayName("progress follows elapsed time, not the number of updates (§67)")
    void frameRateIndependent() {
        Animation slow = new Animation(0.0f, Easing.LINEAR);
        Animation fast = new Animation(0.0f, Easing.LINEAR);

        slow.animateTo(1.0f, 200L);
        fast.animateTo(1.0f, 200L);

        // 2 updates of 50 ms vs 10 updates of 10 ms — same 100 ms of wall time.
        slow.update(50L);
        slow.update(50L);
        for (int i = 0; i < 10; i++) {
            fast.update(10L);
        }

        assertEquals(0.5f, slow.value(), 1e-4);
        assertEquals(0.5f, fast.value(), 1e-4);
    }

    @Test
    @DisplayName("a zero duration applies immediately, which is how 'animations off' works")
    void zeroDurationSnaps() {
        Animation animation = new Animation(0.0f);
        animation.animateTo(1.0f, 0L);
        assertEquals(1.0f, animation.value());
        assertFalse(animation.isRunning());
    }

    @Test
    @DisplayName("retargeting mid-flight eases from the current value, not from the start")
    void retargetsFromCurrentValue() {
        Animation animation = new Animation(0.0f, Easing.LINEAR);
        animation.animateTo(1.0f, 200L);
        animation.update(100L);
        assertEquals(0.5f, animation.value(), 1e-4);

        animation.animateTo(0.0f, 200L);
        animation.update(100L);

        assertEquals(0.25f, animation.value(), 1e-4);
    }

    @Test
    void settlesExactlyOnTarget() {
        Animation animation = new Animation(0.0f);
        animation.animateTo(1.0f, 200L);
        animation.update(10_000L);
        assertEquals(1.0f, animation.value());
        assertFalse(animation.isRunning());
    }

    @Test
    void clockGoingBackwardsDoesNotRewind() {
        Animation animation = new Animation(0.0f, Easing.LINEAR);
        animation.animateTo(1.0f, 200L);
        animation.update(100L);
        animation.update(-500L);
        assertEquals(0.5f, animation.value(), 1e-4);
        assertTrue(animation.isRunning());
    }

    @Test
    @DisplayName("every easing stays inside [0,1] and hits both ends")
    void easingsAreWellBehaved() {
        for (Easing easing : new Easing[]{Easing.LINEAR, Easing.EASE_OUT_QUAD, Easing.EASE_OUT_CUBIC, Easing.EASE_IN_OUT_QUAD}) {
            assertEquals(0.0f, easing.apply(0.0f), 1e-5);
            assertEquals(1.0f, easing.apply(1.0f), 1e-5);
            for (int i = 0; i <= 100; i++) {
                float value = easing.apply(i / 100.0f);
                assertTrue(value >= -1e-5 && value <= 1.0f + 1e-5, "out of range: " + value);
            }
        }
    }
}
