package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.CreateFolderButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 60))
    private int folders$makeRoomForFolderRow(int original) {
        return 84;
    }

    /** Adds a full-width third row to the vanilla footer grid. */
    @Redirect(method = "createFooterButtons", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 3))
    private LayoutElement folders$addFolderRow(GridLayout.RowHelper rows, LayoutElement backButton) {
        LayoutElement result = rows.addChild(backButton);
        rows.addChild(CreateFolderButton.create(this, 0, 0, 308, 20), 4);
        return result;
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
