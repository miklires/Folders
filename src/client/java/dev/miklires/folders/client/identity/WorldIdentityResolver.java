package dev.miklires.folders.client.identity;

import dev.miklires.folders.core.identity.ItemIdentity;
import net.minecraft.world.level.storage.LevelSummary;

/**
 * Identity for a single-player world.
 *
 * <p>Minecraft has no world UUID exposed to the client, and writing a marker file
 * into {@code saves/} would be exactly the kind of interference this mod refuses to do. The
 * save directory name is the next best thing: it is unique inside {@code saves/},
 * Minecraft itself treats it as the key, and it does <em>not</em> change when the
 * player renames the world in-game — which is the case that matters here.
 *
 * <p>MAPPING NOTE: {@code getLevelId()} is the save directory and {@code getLevelName()} is the
 * shown title. Confirm that before shipping: swapping them silently keys every folder on the
 * display name, which is the one thing an identifier here must never be.
 */
public final class WorldIdentityResolver {

    private WorldIdentityResolver() {
    }

    /** @return the stable id, or {@code null} if this summary cannot be keyed */
    public static String idOf(LevelSummary summary) {
        if (summary == null) {
            return null;
        }
        return ItemIdentity.world(summary.getLevelId());
    }

    /** Fallback for a world referenced by path rather than by summary. */
    public static String idOfDirectory(String directoryName) {
        return ItemIdentity.world(directoryName);
    }
}
