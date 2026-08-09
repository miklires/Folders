package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.FoldersListHooks;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes mouse events to the context menu and to drag and drop.
 *
 * <p>Neither selection list declares a mouse method of its own, but their common parent declares
 * all three — this is the level the events actually arrive at. Without this the menu could be
 * opened and then never clicked, and a drag could be started and never finished.
 *
 * <p>Guarded on the widget being one Folders attached to, so every other container in the game is
 * untouched.
 */
@Mixin(AbstractContainerWidget.class)
public abstract class AbstractContainerWidgetMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$mouseClicked(MouseButtonEvent click, boolean doubled,
                                      CallbackInfoReturnable<Boolean> info) {
        FolderListController<?> controller = folders$controller();
        if (controller != null && FoldersListHooks.mouseClicked(controller, click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    /**
     * A press becomes a drag once it has travelled far enough; from then on the pointer belongs to
     * Folders and vanilla's own drag — the scrollbar — is left alone.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void folders$mouseDragged(MouseButtonEvent click, double dragX, double dragY,
                                      CallbackInfoReturnable<Boolean> info) {
        FolderListController<?> controller = folders$controller();
        if (controller != null && FoldersListHooks.mouseDragged(controller, click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void folders$mouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> info) {
        FolderListController<?> controller = folders$controller();
        if (controller != null && FoldersListHooks.mouseReleased(controller, click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    @Unique
    private FolderListController<?> folders$controller() {
        return this instanceof FoldersControllerHost host ? host.folders$controller() : null;
    }
}
