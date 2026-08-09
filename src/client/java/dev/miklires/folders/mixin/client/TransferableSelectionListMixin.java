package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderListController;
import dev.miklires.folders.client.integration.FoldersControllerHost;
import dev.miklires.folders.client.integration.pack.PackListIntegration;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Marks both resource pack lists as ones Folders has a controller for.
 *
 * <p>No injection: everything the pack lists need already arrives through the shared hooks on
 * {@code AbstractSelectionList} and {@code AbstractContainerWidget}, which look for exactly this
 * interface. The list is rebuilt from the render hook rather than from the method that repopulates
 * it, because that method takes a type nested inside {@code PackSelectionModel} which would have to
 * be named to inject there — see {@code PackListIntegration#beforeRenderHook}.
 */
@Mixin(TransferableSelectionList.class)
public abstract class TransferableSelectionListMixin implements FoldersControllerHost {

    @Unique
    private PackListIntegration folders$integration;

    @Override
    public FolderListController<?> folders$controller() {
        if (folders$integration == null) {
            folders$integration = PackListIntegration.create((TransferableSelectionList) (Object) this);
        }
        return folders$integration;
    }
}
