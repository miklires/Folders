package dev.miklires.folders.mixin.server;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the {@link ServerData} behind a server row, for host+port identity.
 *
 * <p>MAPPING NOTE: the field is {@code server} in Yarn.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public interface OnlineServerEntryAccessor {

    @Accessor("server")
    ServerData folders$server();
}
