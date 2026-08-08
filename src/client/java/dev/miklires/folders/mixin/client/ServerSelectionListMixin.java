package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.FoldersListHooks;
import dev.miklires.folders.client.integration.server.ServerListIntegration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connects the multiplayer list to Folders.
 *
 * <p>{@code refreshEntries} rebuilds the rows after a server is added, edited, deleted or moved.
 * Injecting at its tail keeps folders in step with {@code servers.dat} without Folders ever writing
 * to it, and leaves LAN discovery and the network rows exactly as vanilla built them.
 */
@Mixin(ServerSelectionList.class)
public abstract class ServerSelectionListMixin
        extends ObjectSelectionList<ServerSelectionList.Entry>
        implements FoldersControllerHost {

    @Unique
    private ServerListIntegration folders$integration;

    private ServerSelectionListMixin() {
        super(null, 0, 0, 0, 0);
    }

    @Unique
    private ServerListIntegration folders$integration() {
        if (folders$integration == null) {
            ServerSelectionList self = (ServerSelectionList) (Object) this;
            folders$integration = new ServerListIntegration(self, () -> folders$apply(true));
        }
        return folders$integration;
    }

    @Unique
    private void folders$apply(boolean complete) {
        FoldersListHooks.applyEntries(this, folders$integration(), complete);
    }

    @Inject(method = "refreshEntries", at = @At("TAIL"))
    private void folders$afterRefreshEntries(CallbackInfo info) {
        folders$apply(true);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void folders$beforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.beforeRender(this, folders$integration());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void folders$afterRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.afterRender(graphics, folders$integration(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$mouseClicked(MouseButtonEvent click, boolean doubled,
                                      CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseClicked(folders$integration(), click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void folders$mouseDragged(MouseButtonEvent click, double dragX, double dragY,
                                      CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseDragged(folders$integration(), click.x() + dragX, click.y() + dragY)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void folders$mouseReleased(MouseButtonEvent click,
                                       CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseReleased(folders$integration(), click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    @Override
    public FolderListController<?> folders$controller() {
        return folders$integration();
    }
}
