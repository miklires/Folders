package dev.miklires.folders.client.integration.world;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.client.identity.WorldIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.mixin.client.WorldListEntryAccessor;
import dev.miklires.folders.client.ui.FolderStats;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

/**
 * Folders in the singleplayer world list.
 *
 * <p>World rows keep their vanilla entry object, so the screenshot, the title, the
 * version warnings, the edit/delete buttons and double-click-to-play all keep
 * working untouched. Folders only decides where a row sits and how far it is
 * indented — it never redraws a world entry itself.
 */
public final class WorldListIntegration extends FolderListController<WorldSelectionList.Entry> {

    private final WorldSelectionList widget;
    private final Runnable rebuild;

    public WorldListIntegration(WorldSelectionList widget, Runnable rebuild) {
        super(FolderType.WORLDS);
        this.widget = widget;
        this.rebuild = rebuild;
    }

    @Override
    protected String idOf(WorldSelectionList.Entry entry) {
        if (!(entry instanceof WorldSelectionList.WorldListEntry worldEntry)) {
            // LoadingEntry and the "no worlds" placeholder have no identity; they
            // are passed through untouched.
            return null;
        }
        return WorldIdentityResolver.idOf(((WorldListEntryAccessor) worldEntry).folders$level());
    }

    @Override
    protected String displayNameOf(WorldSelectionList.Entry entry) {
        if (entry instanceof WorldSelectionList.WorldListEntry worldEntry) {
            return ((WorldListEntryAccessor) worldEntry).folders$level().getLevelName();
        }
        return "";
    }

    @Override
    protected FolderStats statsFor(Folder folder) {
        return FolderStats.of(countPresent(folder));
    }

    @Override
    protected String countKey() {
        return "folders.count.worlds";
    }

    @Override
    protected WorldSelectionList.Entry createFolderEntry(Folder folder) {
        return new WorldFolderEntry(this, widget, folder);
    }

    @Override
    protected void requestRebuild() {
        rebuild.run();
    }

    public WorldSelectionList widget() {
        return widget;
    }
}
