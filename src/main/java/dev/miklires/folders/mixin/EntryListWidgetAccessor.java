package dev.miklires.folders.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the uniform row height vanilla would otherwise use.
 *
 * <p>MAPPING NOTE: the field is {@code itemHeight} in Yarn.
 */
@Mixin(EntryListWidget.class)
public interface EntryListWidgetAccessor {

    @Accessor("itemHeight")
    int folders$itemHeight();
}
