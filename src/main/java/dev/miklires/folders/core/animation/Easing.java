package dev.miklires.folders.core.animation;

/**
 * Easing curves for §66. {@link #EASE_OUT_CUBIC} is the default: it starts fast
 * and settles, which is what makes a list expansion feel like it snaps open
 * rather than drifts.
 */
@FunctionalInterface
public interface Easing {

    /** @param t progress in {@code [0, 1]} */
    float apply(float t);

    Easing LINEAR = t -> t;

    Easing EASE_OUT_QUAD = t -> 1.0f - (1.0f - t) * (1.0f - t);

    Easing EASE_OUT_CUBIC = t -> {
        float inverted = 1.0f - t;
        return 1.0f - inverted * inverted * inverted;
    };

    Easing EASE_IN_OUT_QUAD = t -> t < 0.5f
            ? 2.0f * t * t
            : 1.0f - (-2.0f * t + 2.0f) * (-2.0f * t + 2.0f) / 2.0f;
}
