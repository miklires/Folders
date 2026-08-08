package dev.miklires.folders.client.integration.server;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.client.identity.ServerIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.mixin.client.OnlineServerEntryAccessor;
import dev.miklires.folders.client.ui.FolderRowRenderer;
import dev.miklires.folders.client.ui.FolderStats;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Folders in the multiplayer list.
 *
 * <p>Server rows stay vanilla entries, so adding, editing, deleting and joining
 * all behave exactly as before and {@code servers.dat} is never touched — Folders
 * writes only its own JSON.
 */
public final class ServerListIntegration extends FolderListController<ServerSelectionList.Entry> {

    private final ServerSelectionList widget;
    private final Runnable rebuild;

    public ServerListIntegration(ServerSelectionList widget, Runnable rebuild) {
        super(FolderType.SERVERS);
        this.widget = widget;
        this.rebuild = rebuild;
    }

    @Override
    protected String idOf(ServerSelectionList.Entry entry) {
        ServerData info = infoOf(entry);
        return info == null ? null : ServerIdentityResolver.idOf(info);
    }

    @Override
    protected String displayNameOf(ServerSelectionList.Entry entry) {
        ServerData info = infoOf(entry);
        return info == null ? "" : info.name;
    }

    private static ServerData infoOf(ServerSelectionList.Entry entry) {
        // ScanEntry (LAN discovery) and LanScanEntry have no ServerData and are
        // passed through as ordinary rows.
        if (entry instanceof ServerSelectionList.OnlineServerEntry serverEntry) {
            return ((OnlineServerEntryAccessor) serverEntry).folders$serverData();
        }
        return null;
    }

    @Override
    protected FolderStats statsFor(Folder folder) {
        return FolderStats.of(countPresent(folder));
    }

    @Override
    protected String countKey() {
        return "folders.count.servers";
    }

    /**
     * No second figure while the online state is unknown; "Online: 0" on a folder full of live
     * servers would be a lie, and an empty column is honest.
     */
    @Override
    protected String highlightKey() {
        return null;
    }

    /**
     * The online dot, currently never shown.
     *
     * <p>{@code ServerData} in 26.2 exposes neither the {@code online} flag nor the {@code ping}
     * field this used to read, and the replacements were not identifiable without the mappings to
     * hand. A dot that reports an invented state is worse than no dot, so it stays dark until the
     * real field is known; the plumbing above it is unchanged and one method restores it.
     */
    @Override
    protected FolderRowRenderer.OnlineState onlineStateFor(Folder folder) {
        return FolderRowRenderer.OnlineState.NONE;
    }

    @Override
    protected ServerSelectionList.Entry createFolderEntry(Folder folder) {
        return new ServerFolderEntry(this, widget, folder);
    }

    @Override
    protected void requestRebuild() {
        rebuild.run();
    }
}
