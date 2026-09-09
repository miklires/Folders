package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.FoldersListHooks;
import dev.miklires.folders.client.integration.world.WorldListIntegration;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Connects the world list to Folders. All it does is forward one event.
 *
 * <p>{@code fillLevels} rebuilds the rows once the saves have been read off disk, and re-runs on
 * every keystroke in the search box, so folders survive filtering without a second hook.
 */
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin implements FoldersControllerHost {

    @Unique
    private WorldListIntegration folders$integration;

    @Unique
    private WorldListIntegration folders$integration() {
        if (folders$integration == null) {
            WorldSelectionList self = (WorldSelectionList) (Object) this;
            folders$integration = new WorldListIntegration(self,
                    () -> folders$applyCached(self));
        }
        return folders$integration;
    }

    /**
     * A non-empty search deliberately omits worlds, so it must never prune stored membership.
     * Only the unfiltered list is a complete snapshot of the saves on disk.
     */
    @Inject(method = "fillLevels", at = @At("TAIL"))
    private void folders$afterFillLevels(String search, List<LevelSummary> levels, CallbackInfo info) {
        folders$apply((WorldSelectionList) (Object) this, search == null || search.isBlank());
    }

    @Unique
    private void folders$apply(WorldSelectionList self, boolean complete) {
        Folders.guarded("rebuilding the list", () ->
                self.replaceEntries(FoldersListHooks.orderedEntries(self, folders$integration(), complete)));
    }

    @Unique
    private void folders$applyCached(WorldSelectionList self) {
        Folders.guarded("rebuilding the cached world list", () ->
                self.replaceEntries(FoldersListHooks.cachedEntries(folders$integration())));
    }

    @Override
    public FolderListController<?> folders$controller() {
        return folders$integration();
    }
}
