package dev.miklires.folders.mixin.client;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the {@link ServerData} behind a server row, for host+port identity.
 *
 * The field is {@code serverData}.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public interface OnlineServerEntryAccessor {

    @Accessor("serverData")
    ServerData folders$serverData();
}
