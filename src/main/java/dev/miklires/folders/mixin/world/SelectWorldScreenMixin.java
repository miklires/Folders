package dev.miklires.folders.mixin.world;

import dev.miklires.folders.Folders;
import dev.miklires.folders.ui.CreateFolderButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Create folder" to the singleplayer screen. Nothing else about the screen
 * changes — it is not replaced, subclassed or reimplemented (§77.13).
 *
 * <p>MAPPING NOTE: {@code levelList} is the Yarn field name for the world list,
 * and the button position assumes the vanilla bottom button row.
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {

    @Shadow
    private WorldListWidget levelList;

    private SelectWorldScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addCreateButton(CallbackInfo info) {
        Folders.guarded("adding the Create folder button", () -> addDrawableChild(
                CreateFolderButton.create(levelList,
                        this.width / 2 - 154,
                        this.height - 28 - 24,
                        CreateFolderButton.DEFAULT_WIDTH,
                        CreateFolderButton.DEFAULT_HEIGHT)));
    }

    /** Escape abandons a drag before it reaches the vanilla "close screen" (§40). */
    @Inject(method = "close", at = @At("HEAD"))
    private void folders$cancelDrag(CallbackInfo info) {
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
    }
}
