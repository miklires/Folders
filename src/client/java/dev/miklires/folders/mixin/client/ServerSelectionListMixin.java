package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.FoldersListHooks;
import dev.miklires.folders.client.integration.server.ServerListIntegration;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Connects the multiplayer list to Folders.
 *
 * <p>{@code refreshEntries} rebuilds the rows after a server is added, edited, deleted or moved.
 * Injecting at its tail keeps folders in step with {@code servers.dat} without Folders ever writing
 * to it, and leaves LAN discovery and the network rows exactly as vanilla built them.
 */
@Mixin(ServerSelectionList.class)
public abstract class ServerSelectionListMixin implements FoldersControllerHost {

    @Unique
    private ServerListIntegration folders$integration;

    @Unique
    private ServerListIntegration folders$integration() {
        if (folders$integration == null) {
            ServerSelectionList self = (ServerSelectionList) (Object) this;
            folders$integration = new ServerListIntegration(self,
                    () -> FoldersListHooks.applyEntries(self, folders$integration(), true));
        }
        return folders$integration;
    }

    @Inject(method = "refreshEntries", at = @At("TAIL"))
    private void folders$afterRefreshEntries(CallbackInfo info) {
        ServerSelectionList self = (ServerSelectionList) (Object) this;
        FoldersListHooks.applyEntries(self, folders$integration(), true);
    }

    @Override
    public FolderListController<?> folders$controller() {
        return folders$integration();
    }
}
