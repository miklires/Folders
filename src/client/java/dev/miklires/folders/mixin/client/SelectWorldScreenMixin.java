package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.CreateFolderButton;
import dev.miklires.folders.client.ui.Gutter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Create folder" to the singleplayer screen. Nothing else about the screen changes — it is
 * not replaced, subclassed or reimplemented.
 *
 * <p>No {@code @Shadow} of the list field: the button finds the list by walking the screen's
 * children. A missing shadow is a crash at mixin-apply time, and this screen has already renamed
 * that field once.
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {

    private SelectWorldScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addRenderableWidget(CreateFolderButton.place(this)));
    }

    /** Escape abandons a drag before it reaches the vanilla "close screen". */
    /** Folders' widgets are not part of the vanilla layout, so a resize has to move them itself. */
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void folders$reposition(CallbackInfo info) {
        Gutter.reposition(this);
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
