package dev.miklires.folders.mixin.pack;

import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the pack behind a resource pack row, for profile-id identity.
 *
 * <p>MAPPING NOTE: the field is {@code pack} in Yarn.
 */
@Mixin(TransferableSelectionList.PackEntry.class)
public interface PackEntryAccessor {

    @Accessor("pack")
    PackSelectionModel.Entry folders$pack();
}
