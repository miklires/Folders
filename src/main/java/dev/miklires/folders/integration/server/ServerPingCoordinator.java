package dev.miklires.folders.integration.server;

import dev.miklires.folders.Folders;
import net.minecraft.client.network.ServerInfo;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.function.BiConsumer;

/**
 * "Refresh all" for a server folder (§31).
 *
 * <p>Pinging is handed to the vanilla pinger, which is already asynchronous, so
 * nothing here blocks the client thread (§77.10). What this adds is a cap on how
 * many pings are in flight: a folder with forty servers must not open forty
 * sockets at once. A server that never answers holds one slot until the vanilla
 * pinger times it out and cannot stall the others.
 */
public final class ServerPingCoordinator {

    /** §31 asks for a limit; four is roughly what the vanilla screen does on open. */
    public static final int MAX_IN_FLIGHT = 4;

    private final BiConsumer<ServerInfo, Runnable> submit;
    private final Deque<ServerInfo> queue = new ArrayDeque<>();
    private final Runnable onProgress;

    private int inFlight;
    private int remaining;

    /**
     * @param submit     hands one server to the vanilla pinger, invoking the
     *                   supplied {@code Runnable} once that ping settles. Keeping
     *                   it a lambda is what isolates the pinger's signature to a
     *                   single line in the integration.
     * @param onProgress called on the client thread whenever a ping settles
     */
    public ServerPingCoordinator(BiConsumer<ServerInfo, Runnable> submit, Runnable onProgress) {
        this.submit = submit;
        this.onProgress = onProgress;
    }

    /** @return false if a sweep is already running for this folder */
    public boolean start(Collection<ServerInfo> servers) {
        if (isRunning()) {
            return false;
        }
        queue.clear();
        queue.addAll(servers);
        remaining = queue.size();
        pump();
        return remaining > 0;
    }

    public boolean isRunning() {
        return inFlight > 0 || !queue.isEmpty();
    }

    public int remaining() {
        return remaining;
    }

    private void pump() {
        while (inFlight < MAX_IN_FLIGHT && !queue.isEmpty()) {
            ServerInfo next = queue.poll();
            inFlight++;
            try {
                submit.accept(next, () -> settled(next));
            } catch (Throwable error) {
                Folders.LOGGER.warn("Could not ping {}", next.address, error);
                settled(next);
            }
        }
    }

    private void settled(ServerInfo info) {
        inFlight = Math.max(0, inFlight - 1);
        remaining = Math.max(0, remaining - 1);
        onProgress.run();
        pump();
    }

    public void cancel() {
        queue.clear();
        remaining = inFlight;
    }
}
