package dev.miklires.folders.mixin.server;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Borrows the screen's own pinger for "Refresh all".
 *
 * <p>Reusing it rather than opening a second one means folder pings share the
 * screen's lifecycle: they are cancelled when it closes, exactly like the pings
 * vanilla starts itself.
 *
 * <p>MAPPING NOTE: the field is {@code serverListPinger} in Yarn.
 */
@Mixin(JoinMultiplayerScreen.class)
public interface JoinMultiplayerScreenAccessor {

    @Accessor("serverListPinger")
    ServerStatusPinger folders$serverListPinger();
}
