package dev.miklires.folders.mixin.client;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the pack screen's selection model.
 *
 * <p>Applying a profile means selecting and unselecting packs, and the model is the only object
 * that can do that — going around it and writing the repository directly is exactly the kind of
 * interference this mod exists to avoid. The screen keeps it private, so this asks for it by name.
 */
@Mixin(PackSelectionScreen.class)
public interface PackSelectionScreenAccessor {

    @Accessor("model")
    PackSelectionModel folders$model();
}
