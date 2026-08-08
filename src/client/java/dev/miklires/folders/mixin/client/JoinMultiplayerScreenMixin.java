package dev.miklires.folders.mixin.server;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.CreateFolderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Create folder" to the multiplayer screen. Add, edit, delete, join and the
 * direct-connect flow are all untouched.
 *
 * <p>MAPPING NOTE: {@code serverListWidget} is the Yarn field name.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {

    @Shadow
    protected ServerSelectionList serverListWidget;

    private JoinMultiplayerScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addRenderableWidget(
                CreateFolderButton.create(serverListWidget,
                        this.width / 2 - 154,
                        this.height - 28 - 24,
                        CreateFolderButton.DEFAULT_WIDTH,
                        CreateFolderButton.DEFAULT_HEIGHT)));
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
