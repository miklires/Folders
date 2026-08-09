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
 * Routes clicks to the folder context menu.
 *
 * <p>Neither selection list declares a mouse method of its own, but their common parent does — this
 * is the level the events actually arrive at. Without this the menu could be opened and then never
 * clicked: every press fell straight through to the row underneath it.
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

    @Unique
    private FolderListController<?> folders$controller() {
        return this instanceof FoldersControllerHost host ? host.folders$controller() : null;
    }
}
