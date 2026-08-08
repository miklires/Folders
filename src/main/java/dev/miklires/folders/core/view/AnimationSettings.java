package dev.miklires.folders.core.view;

/**
 * The two things the layout needs to know about the player's preferences.
 *
 * <p>An interface rather than the config class itself so the model, the layout and their tests stay
 * free of YACL and of Minecraft. The client implements it on the real config.
 */
public interface AnimationSettings {

    /** Everything on, at normal speed. Used by tests and as a fallback. */
    AnimationSettings DEFAULT = baseMs -> baseMs;

    /**
     * Effective duration for an animation nominally lasting {@code baseMs}.
     *
     * @return {@code 0} when animations are off, which makes every transition apply immediately
     */
    long scaledDuration(long baseMs);
}
