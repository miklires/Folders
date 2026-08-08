package dev.miklires.folders.identity;

import dev.miklires.folders.core.identity.ItemIdentity;
import net.minecraft.world.level.storage.LevelSummary;

/**
 * Identity for a single-player world.
 *
 * <p>Minecraft has no world UUID exposed to the client, and writing a marker file
 * into {@code saves/} would be exactly the kind of interference §77 rules out. The
 * save directory name is the next best thing: it is unique inside {@code saves/},
 * Minecraft itself treats it as the key, and it does <em>not</em> change when the
 * player renames the world in-game — which is the case §6 actually cares about.
 *
 * <p>MAPPING NOTE: {@code LevelSummary#getName()} is the directory name in Yarn
 * and {@code getDisplayName()} is the shown title. Verify both against the
 * mappings in use before shipping; swapping them silently would key folders on
 * the display name, which §77.9 forbids.
 */
public final class WorldIdentityResolver {

    private WorldIdentityResolver() {
    }

    /** @return the stable id, or {@code null} if this summary cannot be keyed */
    public static String idOf(LevelSummary summary) {
        if (summary == null) {
            return null;
        }
        return ItemIdentity.world(summary.getName());
    }

    /** Fallback for a world referenced by path rather than by summary (§6). */
    public static String idOfDirectory(String directoryName) {
        return ItemIdentity.world(directoryName);
    }
}
