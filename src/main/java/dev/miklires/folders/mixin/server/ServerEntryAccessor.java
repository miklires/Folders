package dev.miklires.folders.mixin.server;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the {@link ServerInfo} behind a server row, for host+port identity (§6).
 *
 * <p>MAPPING NOTE: the field is {@code server} in Yarn.
 */
@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public interface ServerEntryAccessor {

    @Accessor("server")
    ServerInfo folders$server();
}
