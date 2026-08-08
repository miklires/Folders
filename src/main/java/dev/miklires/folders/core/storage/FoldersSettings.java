package dev.miklires.folders.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The handful of options from §70. Deliberately small; anything not listed here
 * is not configurable in the first version.
 */
public final class FoldersSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger("folders/settings");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final float MIN_ANIMATION_SPEED = 0.25f;
    public static final float MAX_ANIMATION_SPEED = 4.0f;

    private boolean animations = true;
    private float animationSpeed = 1.0f;
    private boolean showStatistics = true;
    private boolean showOnlineIndicator = true;
    private boolean showTooltips = true;

    public boolean animations() {
        return animations;
    }

    public void setAnimations(boolean animations) {
        this.animations = animations;
    }

    /** Multiplier on every animation duration; clamped to a sane band. */
    public float animationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float speed) {
        this.animationSpeed = Math.clamp(speed, MIN_ANIMATION_SPEED, MAX_ANIMATION_SPEED);
    }

    public boolean showStatistics() {
        return showStatistics;
    }

    public void setShowStatistics(boolean showStatistics) {
        this.showStatistics = showStatistics;
    }

    public boolean showOnlineIndicator() {
        return showOnlineIndicator;
    }

    public void setShowOnlineIndicator(boolean showOnlineIndicator) {
        this.showOnlineIndicator = showOnlineIndicator;
    }

    public boolean showTooltips() {
        return showTooltips;
    }

    public void setShowTooltips(boolean showTooltips) {
        this.showTooltips = showTooltips;
    }

    /** Effective duration for an animation nominally lasting {@code baseMs}. */
    public long scaledDuration(long baseMs) {
        if (!animations) {
            return 0L;
        }
        return Math.max(0L, (long) (baseMs / animationSpeed));
    }

    // ------------------------------------------------------------------

    /** Never throws; a broken settings file just means defaults. */
    public static FoldersSettings load(Path file) {
        FoldersSettings settings = new FoldersSettings();
        if (!Files.isRegularFile(file)) {
            return settings;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return settings;
            }
            JsonObject json = parsed.getAsJsonObject();
            settings.animations = bool(json, "animations", settings.animations);
            settings.setAnimationSpeed(number(json, "animation_speed", settings.animationSpeed));
            settings.showStatistics = bool(json, "show_statistics", settings.showStatistics);
            settings.showOnlineIndicator = bool(json, "show_online_indicator", settings.showOnlineIndicator);
            settings.showTooltips = bool(json, "show_tooltips", settings.showTooltips);
        } catch (Exception e) {
            LOGGER.warn("Could not read {}, using default settings", file, e);
        }
        return settings;
    }

    public void save(Path file) {
        JsonObject json = new JsonObject();
        json.addProperty("animations", animations);
        json.addProperty("animation_speed", animationSpeed);
        json.addProperty("show_statistics", showStatistics);
        json.addProperty("show_online_indicator", showOnlineIndicator);
        json.addProperty("show_tooltips", showTooltips);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not save {}", file, e);
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        return fallback;
    }

    private static float number(JsonObject json, String key, float fallback) {
        JsonElement element = json.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsFloat();
        }
        return fallback;
    }
}
