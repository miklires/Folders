package dev.miklires.folders.mixin.world;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the {@link LevelSummary} behind a world row so its save directory can
 * be read for identity.
 *
 * <p>MAPPING NOTE: the field is {@code level} in Yarn.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {

    @Accessor("level")
    LevelSummary folders$level();
}
