package dev.miklires.folders.mixin.server;

import dev.miklires.folders.integration.FolderListController;
import dev.miklires.folders.integration.FoldersControllerHost;
import dev.miklires.folders.integration.FoldersListHooks;
import dev.miklires.folders.integration.server.ServerListIntegration;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connects the multiplayer list to Folders.
 *
 * <p>MAPPING NOTE: {@code setServers(ServerList)} is the Yarn name of the method
 * that rebuilds the rows after a server is added, edited, deleted or moved. It is
 * the single hook that keeps folders in step with {@code servers.dat} without
 * Folders ever writing to it (§47, §77.1).
 */
@Mixin(MultiplayerServerListWidget.class)
public abstract class MultiplayerServerListWidgetMixin
        extends AlwaysSelectedEntryListWidget<MultiplayerServerListWidget.Entry>
        implements FoldersControllerHost {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private MultiplayerScreen screen;

    @Unique
    private ServerListIntegration folders$integration;

    private MultiplayerServerListWidgetMixin() {
        super(null, 0, 0, 0, 0);
    }

    @Unique
    private ServerListIntegration folders$integration() {
        if (folders$integration == null) {
            MultiplayerServerListWidget self = (MultiplayerServerListWidget) (Object) this;
            folders$integration = new ServerListIntegration(self, () -> folders$apply(true),
                    // The only line that knows the vanilla pinger's signature (§31).
                    (info, done) -> ((MultiplayerScreenAccessor) screen).folders$serverListPinger()
                            .add(info, done, done));
        }
        return folders$integration;
    }

    @Unique
    private void folders$apply(boolean complete) {
        FoldersListHooks.applyEntries(this, folders$integration(), complete);
    }

    @Inject(method = "setServers", at = @At("TAIL"))
    private void folders$afterSetServers(CallbackInfo info) {
        folders$apply(true);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void folders$beforeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.beforeRender(this, folders$integration());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void folders$afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.afterRender(context, folders$integration(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseClicked(folders$integration(), mouseX, mouseY)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void folders$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
                                      CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseDragged(folders$integration(), mouseX, mouseY)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void folders$mouseReleased(double mouseX, double mouseY, int button,
                                       CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseReleased(folders$integration(), mouseX, mouseY)) {
            info.setReturnValue(true);
        }
    }

    @Override
    public FolderListController<?> folders$controller() {
        return folders$integration();
    }
}
