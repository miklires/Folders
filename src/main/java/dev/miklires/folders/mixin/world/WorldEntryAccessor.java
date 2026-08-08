package dev.miklires.folders.mixin.world;

import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the {@link LevelSummary} behind a world row so its save directory can
 * be read for identity (§6).
 *
 * <p>MAPPING NOTE: the field is {@code level} in Yarn.
 */
@Mixin(WorldListWidget.WorldEntry.class)
public interface WorldEntryAccessor {

    @Accessor("level")
    LevelSummary folders$level();
}
