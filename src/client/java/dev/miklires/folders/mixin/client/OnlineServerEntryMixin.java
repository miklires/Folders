package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.ScreenFolders;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-clicking a server offers to move it into a folder, and a left press arms a drag. Left
 * click and join are otherwise untouched.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$moveMenu(MouseButtonEvent click, boolean doubleClick,
                                  CallbackInfoReturnable<Boolean> info) {
        ServerSelectionList.Entry self = (ServerSelectionList.Entry) (Object) this;

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Arms the drag and returns without cancelling, so the click carries on to vanilla.
            Folders.guarded("beginning a drag", () -> ScreenFolders.current()
                    .ifPresent(controller -> controller.pressVanilla(self, click.x(), click.y())));
            return;
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        Folders.guarded("opening the move-to-folder menu", () ->
                ScreenFolders.current().ifPresent(controller -> {
                    String id = controller.identify(self);
                    if (id != null) {
                        controller.openMoveMenu(id, (int) click.x(), (int) click.y());
                        info.setReturnValue(true);
                    }
                }));
    }
}
