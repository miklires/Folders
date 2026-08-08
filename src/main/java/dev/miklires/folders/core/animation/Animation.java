package dev.miklires.folders.core.animation;

/**
 * A single scalar driven by elapsed wall time, not by frames (§67).
 *
 * <p>Retargeting mid-flight restarts from wherever the value currently is, so
 * opening a folder and immediately closing it again eases back from the partial
 * position instead of snapping to the end first.
 */
public final class Animation {
    private final Easing easing;

    private float origin;
    private float target;
    private float value;

    private long durationMs;
    private long elapsedMs;

    public Animation(float initial) {
        this(initial, Easing.EASE_OUT_CUBIC);
    }

    public Animation(float initial, Easing easing) {
        this.easing = easing == null ? Easing.EASE_OUT_CUBIC : easing;
        this.origin = initial;
        this.target = initial;
        this.value = initial;
        this.durationMs = 0L;
        this.elapsedMs = 0L;
    }

    /**
     * Animates towards {@code target}. A duration of {@code 0} (animations off,
     * §70) applies the change immediately.
     */
    public void animateTo(float target, long durationMs) {
        if (Float.compare(target, this.target) == 0 && elapsedMs >= this.durationMs) {
            this.value = target;
            return;
        }
        if (durationMs <= 0L) {
            snapTo(target);
            return;
        }
        this.origin = this.value;
        this.target = target;
        this.durationMs = durationMs;
        this.elapsedMs = 0L;
    }

    /** Jumps straight to a value, cancelling any motion. */
    public void snapTo(float value) {
        this.origin = value;
        this.target = value;
        this.value = value;
        this.durationMs = 0L;
        this.elapsedMs = 0L;
    }

    /** @param deltaMs milliseconds since the previous update; negatives are ignored */
    public void update(long deltaMs) {
        if (!isRunning()) {
            value = target;
            return;
        }
        if (deltaMs > 0L) {
            elapsedMs += deltaMs;
        }
        if (elapsedMs >= durationMs) {
            elapsedMs = durationMs;
            value = target;
            return;
        }
        float progress = (float) elapsedMs / (float) durationMs;
        value = origin + (target - origin) * easing.apply(progress);
    }

    public float value() {
        return value;
    }

    public float target() {
        return target;
    }

    public boolean isRunning() {
        return durationMs > 0L && elapsedMs < durationMs;
    }
}
