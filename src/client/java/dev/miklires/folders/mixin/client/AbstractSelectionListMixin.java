package dev.miklires.folders.mixin.client;

import dev.miklires.folders.client.integration.FolderRowLayout;
import dev.miklires.folders.client.integration.FoldersListAccess;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives a list widget variable row heights — but only when Folders has installed
 * a layout on it.
 *
 * <p>Vanilla positions row {@code i} at {@code i * itemHeight}, which cannot
 * express a folder that is 60% open. These three redirects replace that
 * arithmetic with a lookup. Every list in the game without a layout installed
 * (options, language, statistics, controls…) takes the early return and behaves
 * exactly as before.
 *
 * <p>MAPPING NOTE: the three method names below and the exact formula vanilla
 * uses in {@code getRowTop} are the parts to verify. The offsets are expressed
 * relative to the value vanilla computed, so a changed constant does not silently
 * break the layout.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin implements FoldersListAccess {

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

    /**
     * Vanilla returns {@code contentTop + index * itemHeight}. Substituting the
     * laid-out top keeps whatever vanilla uses as the content origin.
     */
    @Inject(method = "getRowTop", at = @At("RETURN"), cancellable = true)
    private void folders$rowTop(int index, CallbackInfoReturnable<Integer> info) {
        if (folders$layout == null) {
            return;
        }
        int vanilla = info.getReturnValue();
        int uniform = index * folders$itemHeight();
        info.setReturnValue(vanilla - uniform + folders$layout.rowTop(index));
    }

    /** Drives the scrollbar; must grow and shrink with the animation. */
    @Inject(method = "getMaxPosition", at = @At("RETURN"), cancellable = true)
    private void folders$maxPosition(CallbackInfoReturnable<Integer> info) {
        if (folders$layout == null) {
            return;
        }
        int vanilla = info.getReturnValue();
        int uniform = folders$entryCount() * folders$itemHeight();
        info.setReturnValue(vanilla - uniform + folders$layout.contentHeight());
    }

    /**
     * Hit testing. Without this a click near a half-open folder would land on the
     * row vanilla thinks is there rather than the one that was drawn.
     */
    @Inject(method = "getEntryAtPosition", at = @At("HEAD"), cancellable = true)
    private void folders$entryAtPosition(double x, double y, CallbackInfoReturnable<AbstractSelectionList.Entry<?>> info) {
        if (folders$layout == null) {
            return;
        }
        int contentY = (int) (y - folders$contentTop());
        int index = folders$layout.rowAt(contentY);
        info.setReturnValue(index < 0 || index >= folders$entryCount() ? null : folders$entryAt(index));
    }

    // ------------------------------------------------------------------
    // Bridges to vanilla internals, isolated so a rename touches one line each.
    // ------------------------------------------------------------------

    @Unique
    @SuppressWarnings("unchecked")
    private AbstractSelectionList<AbstractSelectionList.Entry<?>> folders$self() {
        return (AbstractSelectionList<AbstractSelectionList.Entry<?>>) (Object) this;
    }

    @Unique
    private int folders$itemHeight() {
        return ((AbstractSelectionListAccessor) this).folders$itemHeight();
    }

    @Unique
    private int folders$entryCount() {
        return folders$self().children().size();
    }

    @Unique
    private AbstractSelectionList.Entry<?> folders$entryAt(int index) {
        return folders$self().children().get(index);
    }

    /** Screen-space y of content row 0, i.e. row 0's top with the layout removed. */
    @Unique
    private int folders$contentTop() {
        FolderRowLayout layout = folders$layout;
        folders$layout = null;
        try {
            return folders$self().getRowTop(0);
        } finally {
            folders$layout = layout;
        }
    }
}
