package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderRowLayout;
import dev.miklires.folders.client.integration.FoldersListAccess;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Gives a list widget variable row heights — but only when Folders has installed a layout on it.
 *
 * <p>Vanilla positions row {@code i} at {@code i * itemHeight}, which cannot express a folder that
 * is 60% open. These two injections replace that arithmetic with a lookup. Every list in the game
 * without a layout installed (options, language, statistics, controls…) takes the early return and
 * behaves exactly as before.
 *
 * <p>The offsets are expressed as a correction to whatever vanilla computed, rather than as an
 * absolute position, so a changed content origin does not silently shift every row.
 *
 * <p>{@code AbstractSelectionList.Entry} is protected, so it cannot be named from here at all —
 * hence {@code List<?>} and no hit-testing injection. Hit testing therefore still uses vanilla's
 * uniform arithmetic, which is only wrong for the ~200 ms a folder spends animating; a click during
 * that window can land on the neighbouring row.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin implements FoldersListAccess {

    @Shadow
    public abstract List<?> children();

    @Unique
    private FolderRowLayout folders$layout;

    @Override
    public void folders$setLayout(FolderRowLayout layout) {
        this.folders$layout = layout;
    }

    @Override
    public FolderRowLayout folders$layout() {
        return folders$layout;
    }

    /** Vanilla returns {@code contentTop + index * itemHeight}; swap the second term for a lookup. */
    @Inject(method = "getRowTop", at = @At("RETURN"), cancellable = true)
    private void folders$rowTop(int index, CallbackInfoReturnable<Integer> info) {
        if (folders$layout == null) {
            return;
        }
        int uniform = index * folders$itemHeight();
        info.setReturnValue(info.getReturnValue() - uniform + folders$layout.rowTop(index));
    }

    /** Drives the scrollbar, so it has to grow and shrink with the animation. */
    @Inject(method = "getMaxPosition", at = @At("RETURN"), cancellable = true)
    private void folders$maxPosition(CallbackInfoReturnable<Integer> info) {
        if (folders$layout == null) {
            return;
        }
        int uniform = children().size() * folders$itemHeight();
        info.setReturnValue(info.getReturnValue() - uniform + folders$layout.contentHeight());
    }

    @Unique
    private int folders$itemHeight() {
        return ((AbstractSelectionListAccessor) this).folders$itemHeight();
    }
}
