package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.CreateFolderButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 60))
    private int folders$makeRoomForFolderRow(int original) {
        return 84;
    }

    /** Adds a centered third row to the vanilla footer, before visitWidgets registers it. */
    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 1))
    private LayoutElement folders$addFolderRow(LinearLayout footer, LayoutElement lowerRow) {
        LayoutElement result = footer.addChild(lowerRow);
        LinearLayout folderRow = LinearLayout.horizontal();
        folderRow.addChild(CreateFolderButton.create(this, 0, 0, 150, 20));
        footer.addChild(folderRow);
        return result;
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
