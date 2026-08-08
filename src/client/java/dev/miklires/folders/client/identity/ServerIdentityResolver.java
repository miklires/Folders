package dev.miklires.folders.client.identity;

import dev.miklires.folders.core.identity.ItemIdentity;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Identity for a multiplayer entry: host and port, never the display name.
 * Renaming "Hypixel" to "hypixel (main)" must not empty a folder.
 *
 * <p>MAPPING NOTE: {@code ServerData#address} is a public field in Yarn. If a
 * future version makes it a method, only this class changes.
 */
public final class ServerIdentityResolver {

    private ServerIdentityResolver() {
    }

    public static String idOf(ServerData info) {
        if (info == null) {
            return null;
        }
        return ItemIdentity.server(info.ip);
    }

    public static String idOfAddress(String address) {
        return ItemIdentity.server(address);
    }
}
