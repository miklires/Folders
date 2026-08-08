package dev.miklires.folders.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.miklires.folders.Folders;
import dev.miklires.folders.core.view.AnimationSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * Every user-facing setting of the mod. Serialised to {@code config/folders.json} by YACL.
 *
 * <p>Deliberately short. Folders are meant to look like a feature the game shipped with, and a mod
 * that needs a page of options to do that has usually got the defaults wrong.
 *
 * <p>Implements {@link AnimationSettings} so the layout can read the animation preference without
 * the model package having to know YACL exists.
 */
public class FoldersConfig implements AnimationSettings {

    public static final ConfigClassHandler<FoldersConfig> HANDLER = ConfigClassHandler.createBuilder(FoldersConfig.class)
            .id(Identifier.fromNamespaceAndPath(Folders.MOD_ID, "config"))
            .serializer(handler -> GsonConfigSerializerBuilder.create(handler)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("folders.json"))
                    .build())
            .build();

    public static FoldersConfig get() {
        return HANDLER.instance();
    }

    // ------------------------------------------------------------------ animation

    /** Off applies every expand and collapse instantly, which is also the accessibility answer. */
    @SerialEntry
    public boolean animations = true;

    /** Multiplier on the 200 ms expand; higher is faster. */
    @SerialEntry
    public float animationSpeed = 1.0f;

    // ------------------------------------------------------------------ row contents

    /** The dim second line under a folder name: "Worlds: 5", "Servers: 8 · Online: 3". */
    @SerialEntry
    public boolean showStatistics = true;

    /** The small dot on a closed server folder when something inside it answered a ping. */
    @SerialEntry
    public boolean showOnlineIndicator = true;

    @SerialEntry
    public boolean showTooltips = true;

    // ------------------------------------------------------------------

    @Override
    public long scaledDuration(long baseMs) {
        if (!animations) {
            return 0L;
        }
        float speed = Math.clamp(animationSpeed, 0.25f, 4.0f);
        return Math.max(0L, (long) (baseMs / speed));
    }

    public static void save() {
        HANDLER.save();
    }

    public static void load() {
        HANDLER.load();
    }
}
