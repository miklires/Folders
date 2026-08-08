package dev.miklires.folders.integration.world;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.identity.WorldIdentityResolver;
import dev.miklires.folders.integration.FolderListController;
import dev.miklires.folders.mixin.world.WorldEntryAccessor;
import dev.miklires.folders.ui.FolderStats;
import net.minecraft.client.gui.screen.world.WorldListWidget;

/**
 * Folders in the singleplayer world list (§35, §46).
 *
 * <p>World rows keep their vanilla entry object, so the screenshot, the title, the
 * version warnings, the edit/delete buttons and double-click-to-play all keep
 * working untouched. Folders only decides where a row sits and how far it is
 * indented — it never redraws a world entry itself (§46).
 */
public final class WorldListIntegration extends FolderListController<WorldListWidget.Entry> {

    private final WorldListWidget widget;
    private final Runnable rebuild;

    public WorldListIntegration(WorldListWidget widget, Runnable rebuild) {
        super(FolderType.WORLDS);
        this.widget = widget;
        this.rebuild = rebuild;
    }

    @Override
    protected String idOf(WorldListWidget.Entry entry) {
        if (!(entry instanceof WorldListWidget.WorldEntry worldEntry)) {
            // LoadingEntry and the "no worlds" placeholder have no identity; they
            // are passed through untouched.
            return null;
        }
        return WorldIdentityResolver.idOf(((WorldEntryAccessor) worldEntry).folders$level());
    }

    @Override
    protected String displayNameOf(WorldListWidget.Entry entry) {
        if (entry instanceof WorldListWidget.WorldEntry worldEntry) {
            return ((WorldEntryAccessor) worldEntry).folders$level().getDisplayName();
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
    protected WorldListWidget.Entry createFolderEntry(Folder folder) {
        return new WorldFolderEntry(this, widget, folder);
    }

    @Override
    protected void requestRebuild() {
        rebuild.run();
    }

    public WorldListWidget widget() {
        return widget;
    }
}
