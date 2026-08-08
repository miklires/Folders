package dev.miklires.folders.integration.pack;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.identity.ResourcePackIdentityResolver;
import dev.miklires.folders.integration.FolderListController;
import dev.miklires.folders.mixin.pack.ResourcePackEntryAccessor;
import dev.miklires.folders.ui.FolderContextMenu;
import dev.miklires.folders.ui.FolderStats;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;

import java.util.List;
import java.util.Objects;

/**
 * Folders in the resource pack screen (§48).
 *
 * <p>The pack screen is two lists, available and selected, and a pack is
 * physically in one of them at a time. Folders sits on top of that rather than
 * replacing it:
 *
 * <ul>
 *   <li>a folder is remembered per pack id, so it shows up in whichever list its
 *       packs are currently in — the same folder can appear in both, each showing
 *       only the packs on that side;</li>
 *   <li>enabling and disabling always goes through {@code ResourcePackOrganizer},
 *       never by editing options.txt or the pack list by hand (§33);</li>
 *   <li>Minecraft stays the source of truth for what is enabled; the folder stores
 *       no copy of that state (§34).</li>
 * </ul>
 *
 * <p>Both lists share one repository, so dragging a pack from available to
 * selected keeps it in its folder.
 */
public final class PackListIntegration extends FolderListController<PackListWidget.ResourcePackEntry> {

    private final PackListWidget widget;
    private final Runnable rebuild;
    private final boolean selectedList;

    /**
     * @param selectedList true for the right-hand "selected" list, which is what
     *                     makes the enabled/disabled counts meaningful
     */
    public PackListIntegration(PackListWidget widget, Runnable rebuild, boolean selectedList) {
        super(FolderType.RESOURCE_PACKS);
        this.widget = widget;
        this.rebuild = rebuild;
        this.selectedList = selectedList;
    }

    public boolean isSelectedList() {
        return selectedList;
    }

    @Override
    protected String idOf(PackListWidget.ResourcePackEntry entry) {
        ResourcePackOrganizer.Pack pack = packOf(entry);
        return pack == null ? null : ResourcePackIdentityResolver.idOf(pack);
    }

    @Override
    protected String displayNameOf(PackListWidget.ResourcePackEntry entry) {
        ResourcePackOrganizer.Pack pack = packOf(entry);
        return pack == null ? "" : pack.getDisplayName().getString();
    }

    private static ResourcePackOrganizer.Pack packOf(PackListWidget.ResourcePackEntry entry) {
        if (entry instanceof PackFolderEntry) {
            return null;
        }
        return ((ResourcePackEntryAccessor) entry).folders$pack();
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
        List<ResourcePackOrganizer.Pack> packs = packsIn(folder);
        boolean anyEnableable = packs.stream().anyMatch(ResourcePackOrganizer.Pack::canBeEnabled);
        boolean anyDisableable = packs.stream().anyMatch(ResourcePackOrganizer.Pack::canBeDisabled);
        // §33: the action greys out once there is nothing left for it to do.
        return List.of(
                FolderContextMenu.Item.of("folders.menu.enable_all", () -> enableAll(folder), anyEnableable),
                FolderContextMenu.Item.of("folders.menu.disable_all", () -> disableAll(folder), anyDisableable));
    }

    private List<ResourcePackOrganizer.Pack> packsIn(Folder folder) {
        return presentEntries(folder).stream()
                .map(PackListIntegration::packOf)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Uses the vanilla organiser for every pack, one at a time (§33). */
    public void enableAll(Folder folder) {
        for (ResourcePackOrganizer.Pack pack : packsIn(folder)) {
            if (pack.canBeEnabled()) {
                pack.enable();
            }
        }
        requestRebuild();
    }

    public void disableAll(Folder folder) {
        for (ResourcePackOrganizer.Pack pack : packsIn(folder)) {
            if (pack.canBeDisabled()) {
                pack.disable();
            }
        }
        requestRebuild();
    }

    @Override
    protected PackListWidget.ResourcePackEntry createFolderEntry(Folder folder) {
        return new PackFolderEntry(this, widget, folder);
    }

    @Override
    protected void requestRebuild() {
        rebuild.run();
    }
}
