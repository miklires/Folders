package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.ScreenFolders;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-clicking a world offers to move it into a folder; a left press arms a drag.
 *
 * <p>Vanilla does nothing with the right button on these rows, so this adds a gesture rather than
 * taking one away: left click still selects, double click still plays.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$moveMenu(MouseButtonEvent click, boolean doubleClick,
                                  CallbackInfoReturnable<Boolean> info) {
        WorldSelectionList.Entry self = (WorldSelectionList.Entry) (Object) this;

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
