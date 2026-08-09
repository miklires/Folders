package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.CreateFolderButton;
import dev.miklires.folders.client.ui.Gutter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Create folder" to the multiplayer screen. Add, edit, delete, join and the direct-connect
 * flow are all untouched.
 *
 * <p>No {@code @Shadow} of the list field, for the same reason as the singleplayer screen: the
 * button locates the list through the screen's children instead.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {

    private JoinMultiplayerScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addRenderableWidget(CreateFolderButton.place(this)));
    }

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
