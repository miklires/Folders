package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.CreateFolderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Create folder" to the resource pack screen, creating the folder in the
 * available list where packs start out.
 *
 * <p>Importing packs, dragging between the two lists and applying a selection all
 * keep working; Folders adds grouping and nothing more.
 *
 * <p>MAPPING NOTE: {@code availablePackList} and {@code selectedPackList} are the
 * Yarn field names.
 */
@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {

    @Shadow
    private TransferableSelectionList availablePackList;

    private PackSelectionScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addRenderableWidget(
                CreateFolderButton.create(availablePackList,
                        this.width / 2 - 154,
                        this.height - 48,
                        CreateFolderButton.DEFAULT_WIDTH,
                        CreateFolderButton.DEFAULT_HEIGHT)));
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
