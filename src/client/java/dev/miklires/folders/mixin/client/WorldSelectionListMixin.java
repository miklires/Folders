package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.FoldersListHooks;
import dev.miklires.folders.client.integration.world.WorldListIntegration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connects the world list to Folders. All it does is forward events.
 *
 * <p>{@code fillLevels} is the method that rebuilds the rows once the saves have been read off
 * disk, and it re-runs on every search keystroke, so it is the one hook folders need here.
 */
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin extends ObjectSelectionList<WorldSelectionList.Entry>
        implements FoldersControllerHost {

    @Unique
    private WorldListIntegration folders$integration;

    private WorldSelectionListMixin() {
        super(null, 0, 0, 0, 0);
    }

    @Unique
    private WorldListIntegration folders$integration() {
        if (folders$integration == null) {
            WorldSelectionList self = (WorldSelectionList) (Object) this;
            folders$integration = new WorldListIntegration(self, () -> folders$apply(true));
        }
        return folders$integration;
    }

    @Unique
    private void folders$apply(boolean complete) {
        FoldersListHooks.applyEntries(this, folders$integration(), complete);
    }

    /** The saves are on screen, so the snapshot is complete and orphaned references can go. */
    @Inject(method = "fillLevels", at = @At("TAIL"))
    private void folders$afterFillLevels(String search, List<LevelSummary> levels, CallbackInfo info) {
        folders$apply(true);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void folders$beforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.beforeRender(this, folders$integration());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void folders$afterRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.afterRender(graphics, folders$integration(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$mouseClicked(MouseButtonEvent click, boolean doubled,
                                      CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseClicked(folders$integration(), click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void folders$mouseDragged(MouseButtonEvent click, double dragX, double dragY,
                                      CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseDragged(folders$integration(), click.x() + dragX, click.y() + dragY)) {
            // Suppress vanilla drag-scrolling while a row is being carried.
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void folders$mouseReleased(MouseButtonEvent click,
                                       CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseReleased(folders$integration(), click.x(), click.y())) {
            info.setReturnValue(true);
        }
    }

    @Override
    public FolderListController<?> folders$controller() {
        return folders$integration();
    }
}
