package dev.miklires.folders.client.integration.world;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.client.identity.WorldIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.ui.FolderStats;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;

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
        // Entry.getLevelSummary() is null for the loading header and the "no worlds" placeholder,
        // which is exactly the rows that have no identity and should pass through untouched.
        return WorldIdentityResolver.idOf(entry.getLevelSummary());
    }

    @Override
    protected String displayNameOf(WorldSelectionList.Entry entry) {
        LevelSummary summary = entry.getLevelSummary();
        return summary == null ? "" : summary.getLevelName();
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
