package dev.miklires.folders.mixin.pack;

import dev.miklires.folders.integration.FolderListController;
import dev.miklires.folders.integration.FoldersControllerHost;
import dev.miklires.folders.integration.FoldersListHooks;
import dev.miklires.folders.integration.pack.PackListIntegration;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connects both resource pack lists to Folders (§48).
 *
 * <p>The mixin applies to the widget class, which the pack screen instantiates
 * twice, so each list gets its own controller over the shared repository. Which
 * side a list is on is read from its title.
 *
 * <p>§49 — the drag conflict. The vanilla pack screen already uses drag to move a
 * pack between the two lists, and that has to keep working, so the two modes are
 * split by area rather than by guesswork: a drag that starts on the pack's 32px
 * icon is a Folders drag, a drag that starts anywhere else on the row is the
 * vanilla one. Folders only claims the event once the pointer has passed the
 * threshold, so a plain click on the icon still toggles the pack as before.
 */
@Mixin(PackListWidget.class)
public abstract class PackListWidgetMixin
        extends AlwaysSelectedEntryListWidget<PackListWidget.ResourcePackEntry>
        implements FoldersControllerHost {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private Text title;

    @Unique
    private PackListIntegration folders$integration;

    private PackListWidgetMixin() {
        super(null, 0, 0, 0, 0);
    }

    @Unique
    private PackListIntegration folders$integration() {
        if (folders$integration == null) {
            PackListWidget self = (PackListWidget) (Object) this;
            folders$integration = new PackListIntegration(self, () -> folders$apply(true), folders$isSelectedList());
        }
        return folders$integration;
    }

    /**
     * MAPPING NOTE: the selected list's title is {@code pack.selected.title}. If
     * that key changes this only affects which side shows an "enabled" count, not
     * grouping or drag.
     */
    @Unique
    private boolean folders$isSelectedList() {
        return title != null && title.getString().equals(Text.translatable("pack.selected.title").getString());
    }

    @Unique
    private void folders$apply(boolean complete) {
        FoldersListHooks.applyEntries(this, folders$integration(), complete);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void folders$beforeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.beforeRender(this, folders$integration());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void folders$afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        FoldersListHooks.afterRender(context, folders$integration(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseClicked(folders$integration(), mouseX, mouseY)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void folders$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
                                      CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseDragged(folders$integration(), mouseX, mouseY)) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void folders$mouseReleased(double mouseX, double mouseY, int button,
                                       CallbackInfoReturnable<Boolean> info) {
        if (FoldersListHooks.mouseReleased(folders$integration(), mouseX, mouseY)) {
            info.setReturnValue(true);
        }
    }

    @Override
    public FolderListController<?> folders$controller() {
        return folders$integration();
    }
}
