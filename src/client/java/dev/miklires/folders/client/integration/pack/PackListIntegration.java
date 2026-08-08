package dev.miklires.folders.client.integration.pack;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.client.identity.ResourcePackIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.mixin.client.PackEntryAccessor;
import dev.miklires.folders.client.ui.FolderContextMenu;
import dev.miklires.folders.client.ui.FolderStats;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;

import java.util.List;
import java.util.Objects;

/**
 * Folders in the resource pack screen.
 *
 * <p>The pack screen is two lists, available and selected, and a pack is
 * physically in one of them at a time. Folders sits on top of that rather than
 * replacing it:
 *
 * <ul>
 *   <li>a folder is remembered per pack id, so it shows up in whichever list its
 *       packs are currently in — the same folder can appear in both, each showing
 *       only the packs on that side;</li>
 *   <li>enabling and disabling always goes through {@code PackSelectionModel},
 *       never by editing options.txt or the pack list by hand;</li>
 *   <li>Minecraft stays the source of truth for what is enabled; the folder stores
 *       no copy of that state.</li>
 * </ul>
 *
 * <p>Both lists share one repository, so dragging a pack from available to
 * selected keeps it in its folder.
 */
public final class PackListIntegration extends FolderListController<TransferableSelectionList.PackEntry> {

    private final TransferableSelectionList widget;
    private final Runnable rebuild;
    private final boolean selectedList;

    /**
     * @param selectedList true for the right-hand "selected" list, which is what
     *                     makes the enabled/disabled counts meaningful
     */
    public PackListIntegration(TransferableSelectionList widget, Runnable rebuild, boolean selectedList) {
        super(FolderType.RESOURCE_PACKS);
        this.widget = widget;
        this.rebuild = rebuild;
        this.selectedList = selectedList;
    }

    public boolean isSelectedList() {
        return selectedList;
    }

    @Override
    protected String idOf(TransferableSelectionList.PackEntry entry) {
        PackSelectionModel.Entry pack = packOf(entry);
        return pack == null ? null : ResourcePackIdentityResolver.idOf(pack);
    }

    @Override
    protected String displayNameOf(TransferableSelectionList.PackEntry entry) {
        PackSelectionModel.Entry pack = packOf(entry);
        return pack == null ? "" : pack.getTitle().getString();
    }

    private static PackSelectionModel.Entry packOf(TransferableSelectionList.PackEntry entry) {
        if (entry instanceof PackFolderEntry) {
            return null;
        }
        return ((PackEntryAccessor) entry).folders$pack();
    }

    @Override
    protected FolderStats statsFor(Folder folder) {
        int present = countPresent(folder);
        // Only the selected list has a meaningful "enabled" count; on the available
        // side every pack is by definition disabled, so the second number is
        // dropped rather than shown as a constant zero.
        return selectedList ? FolderStats.of(present, present) : FolderStats.of(present);
    }

    @Override
    protected String countKey() {
        return "folders.count.packs";
    }

    @Override
    protected String highlightKey() {
        return selectedList ? "folders.count.enabled" : null;
    }

    @Override
    protected List<FolderContextMenu.Item> typeMenuItems(Folder folder) {
        List<PackSelectionModel.Entry> packs = packsIn(folder);
        boolean anyEnableable = packs.stream().anyMatch(PackSelectionModel.Entry::canSelect);
        boolean anyDisableable = packs.stream().anyMatch(PackSelectionModel.Entry::canUnselect);
        //: the action greys out once there is nothing left for it to do.
        return List.of(
                FolderContextMenu.Item.of("folders.menu.enable_all", () -> enableAll(folder), anyEnableable),
                FolderContextMenu.Item.of("folders.menu.disable_all", () -> disableAll(folder), anyDisableable));
    }

    private List<PackSelectionModel.Entry> packsIn(Folder folder) {
        return presentEntries(folder).stream()
                .map(PackListIntegration::packOf)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Uses the vanilla organiser for every pack, one at a time. */
    public void enableAll(Folder folder) {
        for (PackSelectionModel.Entry pack : packsIn(folder)) {
            if (pack.canSelect()) {
                pack.select();
            }
        }
        requestRebuild();
    }

    public void disableAll(Folder folder) {
        for (PackSelectionModel.Entry pack : packsIn(folder)) {
            if (pack.canUnselect()) {
                pack.unselect();
            }
        }
        requestRebuild();
    }

    @Override
    protected TransferableSelectionList.PackEntry createFolderEntry(Folder folder) {
        return new PackFolderEntry(this, widget, folder);
    }

    @Override
    protected void requestRebuild() {
        rebuild.run();
    }
}
