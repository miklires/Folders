package dev.miklires.folders.mixin.client;

import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the pack behind a resource pack row, for profile-id identity.
 *
 * The field is {@code pack}.
 */
@Mixin(TransferableSelectionList.PackEntry.class)
public interface PackEntryAccessor {

    @Accessor("pack")
    PackSelectionModel.Entry folders$pack();
}
