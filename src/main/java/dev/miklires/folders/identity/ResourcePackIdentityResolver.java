package dev.miklires.folders.identity;

import dev.miklires.folders.core.identity.ItemIdentity;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;

/**
 * Identity for a resource pack: the profile id, which encodes the pack's source
 * (for a file pack, {@code file/<name>.zip}). Two packs whose metadata gives them
 * the same display title stay separate (§6).
 *
 * <p>MAPPING NOTE: {@code ResourcePackOrganizer.Pack#getName()} returns the
 * profile id in Yarn while {@code getDisplayName()} returns the shown title.
 * These two are easy to confuse and getting it wrong means folders key on the
 * display name.
 */
public final class ResourcePackIdentityResolver {

    private ResourcePackIdentityResolver() {
    }

    public static String idOf(ResourcePackOrganizer.Pack pack) {
        if (pack == null) {
            return null;
        }
        return ItemIdentity.resourcePack(pack.getName());
    }

    public static String idOfProfile(String profileId) {
        return ItemIdentity.resourcePack(profileId);
    }
}
