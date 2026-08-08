package dev.miklires.folders.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.config.FoldersConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/**
 * Client entrypoint. There is no server counterpart and never will be: folders are a local view
 * over local data, they are never sent anywhere, and no server needs to know the mod exists.
 */
public class FoldersClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FoldersConfig.load();
        Folders.initialise();

        // Last chance to flush. Saves are otherwise triggered by whichever operation dirtied the
        // model, so this normally has nothing to do.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Folders.data().close());
    }
}
