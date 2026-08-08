package dev.miklires.folders.mixin.server;

import dev.miklires.folders.Folders;
import dev.miklires.folders.ui.CreateFolderButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Create folder" to the multiplayer screen. Add, edit, delete, join and the
 * direct-connect flow are all untouched (§56).
 *
 * <p>MAPPING NOTE: {@code serverListWidget} is the Yarn field name.
 */
@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    private MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addDrawableChild(
                CreateFolderButton.create(serverListWidget,
                        this.width / 2 - 154,
                        this.height - 28 - 24,
                        CreateFolderButton.DEFAULT_WIDTH,
                        CreateFolderButton.DEFAULT_HEIGHT)));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
