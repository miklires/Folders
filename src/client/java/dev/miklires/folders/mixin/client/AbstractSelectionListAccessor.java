package dev.miklires.folders.mixin.client;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the uniform row height vanilla would otherwise use.
 *
 * <p>MAPPING NOTE: the field is {@code itemHeight} in Yarn.
 */
@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor {

    @Accessor("itemHeight")
    int folders$itemHeight();
}
