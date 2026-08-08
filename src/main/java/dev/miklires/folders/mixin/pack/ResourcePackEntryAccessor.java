package dev.miklires.folders.mixin.pack;

import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the pack behind a resource pack row, for profile-id identity (§6).
 *
 * <p>MAPPING NOTE: the field is {@code pack} in Yarn.
 */
@Mixin(PackListWidget.ResourcePackEntry.class)
public interface ResourcePackEntryAccessor {

    @Accessor("pack")
    ResourcePackOrganizer.Pack folders$pack();
}
