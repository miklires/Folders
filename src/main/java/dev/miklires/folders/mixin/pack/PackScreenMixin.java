package dev.miklires.folders.mixin.pack;

import dev.miklires.folders.Folders;
import dev.miklires.folders.ui.CreateFolderButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
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
 * keep working; Folders adds grouping and nothing more (§48).
 *
 * <p>MAPPING NOTE: {@code availablePackList} and {@code selectedPackList} are the
 * Yarn field names.
 */
@Mixin(PackScreen.class)
public abstract class PackScreenMixin extends Screen {

    @Shadow
    private PackListWidget availablePackList;

    private PackScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addDrawableChild(
                CreateFolderButton.create(availablePackList,
                        this.width / 2 - 154,
                        this.height - 48,
                        CreateFolderButton.DEFAULT_WIDTH,
                        CreateFolderButton.DEFAULT_HEIGHT)));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
