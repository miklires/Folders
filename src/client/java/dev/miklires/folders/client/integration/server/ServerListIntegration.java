package dev.miklires.folders.client.integration.server;

import dev.miklires.folders.client.config.FoldersConfig;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.client.identity.ServerIdentityResolver;
import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.mixin.client.OnlineServerEntryAccessor;
import dev.miklires.folders.client.ui.FolderContextMenu;
import dev.miklires.folders.client.ui.FolderRowRenderer;
import dev.miklires.folders.client.ui.FolderStats;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

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
    private final BiConsumer<ServerData, Runnable> pingSubmitter;

    private final Map<UUID, ServerPingCoordinator> sweeps = new HashMap<>();

    /**
     * @param pingSubmitter passes one server to the screen's vanilla pinger; see
     *                      {@link ServerPingCoordinator}
     */
    public ServerListIntegration(ServerSelectionList widget, Runnable rebuild,
                                 BiConsumer<ServerData, Runnable> pingSubmitter) {
        super(FolderType.SERVERS);
        this.widget = widget;
        this.rebuild = rebuild;
        this.pingSubmitter = pingSubmitter;
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
            return ((OnlineServerEntryAccessor) serverEntry).folders$server();
        }
        return null;
    }

    @Override
    protected FolderStats statsFor(Folder folder) {
        FolderStats stats = FolderStats.of(countPresent(folder), countOnline(folder));
        ServerPingCoordinator sweep = sweeps.get(folder.id());
        return sweep != null && sweep.isRunning() ? stats.refreshing() : stats;
    }

    @Override
    protected String countKey() {
        return "folders.count.servers";
    }

    @Override
    protected String highlightKey() {
        return "folders.count.online";
    }

    @Override
    protected FolderRowRenderer.OnlineState onlineStateFor(Folder folder) {
        if (!FoldersConfig.get().showOnlineIndicator) {
            return FolderRowRenderer.OnlineState.NONE;
        }
        boolean anyKnown = false;
        for (ServerData info : serversIn(folder)) {
            if (isOnline(info)) {
                return FolderRowRenderer.OnlineState.ONLINE;
            }
            anyKnown |= info.ping != 0L;
        }
        if (folder.isEmpty()) {
            return FolderRowRenderer.OnlineState.NONE;
        }
        return anyKnown ? FolderRowRenderer.OnlineState.FAILED : FolderRowRenderer.OnlineState.UNKNOWN;
    }

    /**
     * MAPPING NOTE: {@code ServerData#online} is set by the vanilla pinger once a
     * status response arrives. If it disappears, {@code ping > 0} together with a
     * non-null {@code playerCountLabel} is the fallback.
     */
    private static boolean isOnline(ServerData info) {
        return info != null && info.online;
    }

    private int countOnline(Folder folder) {
        int online = 0;
        for (ServerData info : serversIn(folder)) {
            if (isOnline(info)) {
                online++;
            }
        }
        return online;
    }

    private List<ServerData> serversIn(Folder folder) {
        return presentEntries(folder).stream()
                .map(ServerListIntegration::infoOf)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    protected List<FolderContextMenu.Item> typeMenuItems(Folder folder) {
        return List.of(FolderContextMenu.Item.of("folders.menu.refresh_ping",
                () -> refreshAll(folder), !folder.isEmpty()));
    }

    /**: asynchronous, bounded, and one dead server does not hold up the rest. */
    public void refreshAll(Folder folder) {
        ServerPingCoordinator sweep = sweeps.computeIfAbsent(folder.id(),
                id -> new ServerPingCoordinator(pingSubmitter, this::requestRebuild));
        sweep.start(serversIn(folder));
        requestRebuild();
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
