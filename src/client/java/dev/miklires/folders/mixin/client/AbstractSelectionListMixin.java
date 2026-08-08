package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.FoldersListHooks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the per-frame work for whichever lists Folders has attached to.
 *
 * <p>The hook lives on the base class rather than on each list because the two lists Folders
 * extends do not both override the draw method: {@code WorldSelectionList} does,
 * {@code ServerSelectionList} does not, and a mixin cannot inject into a method a class merely
 * inherits. One injection here covers both, and lists Folders never touched fall straight through
 * the null check.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void folders$beforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                      float partialTick, CallbackInfo info) {
        FolderListController<?> controller = folders$controller();
        if (controller != null) {
            FoldersListHooks.beforeRender((AbstractSelectionList<?>) (Object) this, controller);
        }
    }

    /** Ghost preview and context menu, drawn last so they land on top. */
    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void folders$afterRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                     float partialTick, CallbackInfo info) {
        FolderListController<?> controller = folders$controller();
        if (controller != null) {
            FoldersListHooks.afterRender(graphics, controller, mouseX, mouseY);
        }
    }

    private FolderListController<?> folders$controller() {
        return this instanceof FoldersControllerHost host ? host.folders$controller() : null;
    }
}
