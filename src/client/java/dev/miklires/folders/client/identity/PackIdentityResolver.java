package dev.miklires.folders.client.identity;

import dev.miklires.folders.core.identity.ItemIdentity;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;

/**
 * Identity for a resource pack: its profile id, never its title.
 *
 * <p>Two packs are allowed to call themselves the same thing, and a pack is allowed to rename
 * itself in an update. The profile id — {@code file/shaders.zip}, {@code vanilla} — is what
 * Minecraft itself uses to remember which packs are on, so it is what a folder remembers too.
 */
public final class PackIdentityResolver {

    private PackIdentityResolver() {
    }

    public static String idOf(PackSelectionModel.Entry pack) {
        return pack == null ? null : ItemIdentity.resourcePack(pack.getId());
    }

    public static String idOfProfileId(String profileId) {
        return ItemIdentity.resourcePack(profileId);
    }
}
