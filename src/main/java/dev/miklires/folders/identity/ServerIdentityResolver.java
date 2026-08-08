package dev.miklires.folders.identity;

import dev.miklires.folders.core.identity.ItemIdentity;
import net.minecraft.client.network.ServerInfo;

/**
 * Identity for a multiplayer entry: host and port, never the display name (§6).
 * Renaming "Hypixel" to "hypixel (main)" must not empty a folder.
 *
 * <p>MAPPING NOTE: {@code ServerInfo#address} is a public field in Yarn. If a
 * future version makes it a method, only this class changes.
 */
public final class ServerIdentityResolver {

    private ServerIdentityResolver() {
    }

    public static String idOf(ServerInfo info) {
        if (info == null) {
            return null;
        }
        return ItemIdentity.server(info.address);
    }

    public static String idOfAddress(String address) {
        return ItemIdentity.server(address);
    }
}
