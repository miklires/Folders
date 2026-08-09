package dev.miklires.folders.client.integration.pack;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.identity.PackIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersListHooks;
import dev.miklires.folders.client.ui.FolderStats;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;

import java.util.List;

/**
 * Folders in one of the two resource pack lists.
 *
 * <p>One controller per list, both sharing the single {@code resource_packs.json}: a folder is
 * remembered per pack id, so it shows up on whichever side its packs are currently on, and the same
 * folder can appear on both with different contents. That is the only arrangement that survives a
 * pack being enabled, which moves it between the lists without changing anything about the folder.
 *
 * <p>Because each side sees only half the packs, this controller never prunes. Syncing the
 * available list against the model would otherwise conclude that every enabled pack had been
 * deleted, and empty the folders on the other side.
 *
 * <p>The entry type is {@code Object} rather than the list's own entry class. That class is nested
 * inside {@code TransferableSelectionList} and this mod does not need to name it: nothing here is
 * ever constructed as one, only vanilla rows are handed back and forth, and the one call that needs
 * the real type goes through {@code replaceEntries} raw. Not naming it is one fewer thing that can
 * be renamed out from under the mod.
 */
public final class PackListIntegration extends FolderListController<Object> {

    private final TransferableSelectionList widget;

    /** The rows this controller last installed, to notice when vanilla has replaced them. */
    private List<Object> lastApplied = List.of();

    private PackListIntegration(TransferableSelectionList widget) {
        super(FolderType.RESOURCE_PACKS);
        this.widget = widget;
    }

    /** Builds a controller and registers it, so the other list can be rebuilt alongside it. */
    public static PackListIntegration create(TransferableSelectionList widget) {
        PackListIntegration integration = new PackListIntegration(widget);
        PackScreens.register(integration);
        return integration;
    }

    public TransferableSelectionList widget() {
        return widget;
    }

    @Override
    protected boolean isFolderRow(Object entry) {
        return entry instanceof TransferableSelectionList.PackEntry pack
                && FolderPack.isFolderId(pack.getPackId());
    }

    @Override
    protected String idOf(Object entry) {
        if (!(entry instanceof TransferableSelectionList.PackEntry pack)) {
            return null;
        }
        String packId = pack.getPackId();
        if (FolderPack.isFolderId(packId)) {
            // One of ours. Not a pack, so it has no item id and claims no row.
            return null;
        }
        return PackIdentityResolver.idOfProfileId(packId);
    }

    @Override
    protected String displayNameOf(Object entry) {
        return entry instanceof TransferableSelectionList.PackEntry pack
                ? pack.getNarration().getString()
                : "";
    }

    @Override
    protected FolderStats statsFor(Folder folder) {
        return FolderStats.of(countPresent(folder));
    }

    @Override
    protected String countKey() {
        return "folders.count.packs";
    }

    /**
     * Builds the row as a vanilla {@code PackEntry} over a {@link FolderPack}, so the pack screen
     * draws it with its own code and Folders draws nothing at all.
     */
    @Override
    protected Object createFolderEntry(Folder folder) {
        FolderPack pack = new FolderPack(folder, FolderPack.describe(statsFor(folder), countKey()));
        return widget.new PackEntry(Minecraft.getInstance(), widget, pack);
    }

    /** Renaming happens in a text field above the list; see {@link PackRenameBar}. */
    @Override
    protected void requestRename(Folder folder) {
        PackRenameBar.begin(folder.name(), name -> {
            if (repository().renameFolder(folder.id(), name)) {
                afterModelChange();
            }
        });
    }

    /**
     * A pack folder row snapshots its title and description when it is built, so any change to the
     * model has to throw the rows away rather than expect them to notice.
     */
    @Override
    protected void afterModelChange() {
        PackScreens.invalidateAll();
        Folders.data().saveIfDirty(FolderType.RESOURCE_PACKS);
        PackScreens.rebuildAll();
    }

    @Override
    protected void requestRebuild() {
        PackScreens.rebuildAll();
    }

    void invalidate() {
        clearFolderEntries();
        lastApplied = List.of();
    }

    /**
     * Re-applies the folder rows when vanilla has rebuilt the list underneath them.
     *
     * <p>The other two screens hook the method that rebuilds their list. The pack screen's takes a
     * parameter type nested inside {@code PackSelectionModel} that this mod would have to name to
     * inject there, and naming it buys nothing: comparing the rows against the ones last installed
     * answers the same question — has vanilla replaced them? — using no vanilla API at all.
     *
     * <p>Runs at the head of the list's render, before it walks its children, so swapping them here
     * cannot disturb a draw in progress.
     */
    @Override
    protected void beforeRenderHook() {
        List<Object> children = currentChildren();
        if (children.size() != lastApplied.size()) {
            apply();
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) != lastApplied.get(i)) {
                apply();
                return;
            }
        }
    }

    /** Recomputes this list's rows and hands them to the widget. */
    void apply() {
        Folders.guarded("rebuilding the resource pack list", () -> {
            // complete = false: this list holds half the packs, and pruning against half the model
            // would delete the other half from every folder.
            List<Object> ordered = FoldersListHooks.orderedEntries(widget, this, false);
            replaceEntries(ordered);
            lastApplied = currentChildren();
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void replaceEntries(List<Object> entries) {
        // Raw on purpose: the element type is the list's own nested entry class, which this mod
        // deliberately never names. The contents are vanilla rows plus rows built by
        // createFolderEntry, so they are the right type however it is spelled.
        ((AbstractSelectionList) widget).replaceEntries(entries);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Object> currentChildren() {
        return List.copyOf((List) ((AbstractSelectionList) widget).children());
    }
}
