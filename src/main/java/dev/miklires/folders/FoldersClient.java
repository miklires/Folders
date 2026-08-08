package dev.miklires.folders;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/**
 * Client entrypoint. There is no server counterpart and never will be — folders
 * are a local view over local data (§54, §55).
 */
public final class FoldersClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Folders.initialise();
        Folders.LOGGER.info("Folders loaded");

        // Last chance to flush pending writes; saves are otherwise triggered by
        // the operation that dirtied the model (§51).
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Folders.data().close());
    }
}
