package dev.miklires.folders.mixin.world;

import dev.miklires.folders.integration.FolderListController;
import dev.miklires.folders.integration.FoldersControllerHost;
import dev.miklires.folders.integration.FoldersListHooks;
import dev.miklires.folders.integration.world.WorldListIntegration;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connects the world list to Folders. All it does is forward events (§44).
 *
 * <p>MAPPING NOTE: {@code show(List)} is the Yarn name of the method that fills
 * the list once the saves have been read off disk. If it is renamed, this is the
 * only injection that needs updating for worlds.
 */
@Mixin(WorldListWidget.class)
public abstract class WorldListWidgetMixin extends AlwaysSelectedEntryListWidget<WorldListWidget.Entry>
        implements FoldersControllerHost {

    @Unique
    private WorldListIntegration folders$integration;

    private WorldListWidgetMixin() {
        super(null, 0, 0, 0, 0);
    }

    @Unique
    private WorldListIntegration folders$integration() {
        if (folders$integration == null) {
            WorldListWidget self = (WorldListWidget) (Object) this;
            folders$integration = new WorldListIntegration(self, () -> folders$apply(true));
        }
        return folders$integration;
    }

    @Unique
    private void folders$apply(boolean complete) {
        FoldersListHooks.applyEntries(this, folders$integration(), complete);
    }

    /** The saves are on screen, so the snapshot is complete and orphans can go (§58). */
    @Inject(method = "show", at = @At("TAIL"))
    private void folders$afterShow(CallbackInfo info) {
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
            // Suppress vanilla drag-scrolling while a row is being carried.
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
