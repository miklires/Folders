package dev.miklires.folders.mixin.client;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the {@link LevelSummary} behind a world row so its save directory can
 * be read for identity.
 *
 * The field is {@code summary}.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {

    @Accessor("summary")
    LevelSummary folders$summary();
}
