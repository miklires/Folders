package dev.miklires.folders.client.integration.server;

import dev.miklires.folders.client.config.FoldersConfig;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.client.identity.ServerIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
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
            return serverEntry.getServerData();
        }
        return null;
    }

    @Override
    protected FolderStats statsFor(Folder folder) {
        return FolderStats.of(countPresent(folder), countOnline(folder));
    }

    @Override
    protected String countKey() {
        return "folders.count.servers";
    }

    @Override
    protected String highlightKey() {
        return "folders.count.online";
    }

    /**
     * Whether a server answered its last ping.
     *
     * <p>26.2 dropped the {@code online} flag but kept {@code ping}, which is what the dot actually
     * means: a round trip completed. It stays 0 until a status response arrives and goes negative
     * when one fails, so a positive value is the honest test.
     */
    private static boolean isOnline(ServerData info) {
        return info != null && info.ping > 0L;
    }

    private int countOnline(Folder folder) {
        int online = 0;
        for (ServerSelectionList.Entry entry : presentEntries(folder)) {
            if (isOnline(infoOf(entry))) {
                online++;
            }
        }
        return online;
    }

    @Override
    protected FolderRowRenderer.OnlineState onlineStateFor(Folder folder) {
        if (!FoldersConfig.get().showOnlineIndicator || folder.isEmpty()) {
            return FolderRowRenderer.OnlineState.NONE;
        }
        return countOnline(folder) > 0
                ? FolderRowRenderer.OnlineState.ONLINE
                : FolderRowRenderer.OnlineState.UNKNOWN;
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
