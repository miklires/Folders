package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.integration.pack.PackListIntegration;
import dev.miklires.folders.client.integration.pack.PackProfiles;
import dev.miklires.folders.client.integration.pack.PackRenameBar;
import dev.miklires.folders.client.integration.pack.PackScreens;
import dev.miklires.folders.client.ui.CreateFolderButton;
import dev.miklires.folders.client.ui.Gutter;
import dev.miklires.folders.core.data.Folder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds folders and pack profiles to the resource pack screen.
 *
 * <p>Two buttons in the left gutter and one text field above them. Everything else on the screen —
 * the two lists, drag and drop between them, the search box, "Open Pack Folder" — is untouched, and
 * {@code resourcepacks/} is never written to by this mod.
 *
 * <p>The text field is the pack screen's answer to renaming. Its rows are vanilla widgets built
 * from a title, so there is nowhere in a row to put a caret; a real {@code EditBox} is both simpler
 * and better behaved, and it is the same field whether a folder or a profile is being named.
 */
@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {

    private PackSelectionScreenMixin() {
        super(null);
    }

    @Unique
    private EditBox folders$nameField;

    /** Both lists are about to be rebuilt, so nothing from the previous screen should survive. */
    @Inject(method = "init", at = @At("HEAD"))
    private void folders$resetScreen(CallbackInfo info) {
        PackScreens.reset();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void folders$addControls(CallbackInfo info) {
        Folders.guarded("adding the Folders controls to the resource pack screen", () -> {
            addRenderableWidget(CreateFolderButton.place(this));
            addRenderableWidget(folders$profilesButton());
            folders$nameField = folders$buildNameField();
            addRenderableWidget(folders$nameField);
        });
    }

    @Unique
    private Button folders$profilesButton() {
        Gutter.Slot slot = Gutter.slot(this, 1);
        return Gutter.anchor(Button.builder(Component.translatable("folders.profile.button"), ignored -> Folders
                .guarded("opening the pack profiles menu", () -> PackScreens.any()
                        .ifPresent(this::folders$openProfiles)))
                .bounds(slot.x(), slot.y(), slot.width(), slot.height())
                .tooltip(Tooltip.create(Component.translatable("folders.profile.button.tooltip")))
                .build(), 1, 0);
    }

    @Unique
    private void folders$openProfiles(PackListIntegration controller) {
        // Anchored inside the list, not next to the button that opened it. A menu is drawn and
        // clicked by the list it belongs to, so one floating over the gutter would appear and then
        // swallow nothing: every click on it would miss the widget that routes them.
        PackProfiles.openMenu(((PackSelectionScreenAccessor) this).folders$model(), controller,
                controller.widget().getX() + 4, controller.widget().getY() + 4);
    }

    @Unique
    private EditBox folders$buildNameField() {
        Gutter.Slot slot = Gutter.slot(this, 2, 120);
        // Wider than the gutter on purpose. It is a transient editor that only appears while a name
        // is being typed, and a field too narrow to read the name back is worse than one that
        // overlaps the list for a moment.
        EditBox field = new EditBox(font, slot.x(), slot.y(), slot.width(), slot.height(),
                Component.translatable("folders.rename.folder"));
        field.setMaxLength(Folder.MAX_NAME_LENGTH);
        field.setResponder(PackRenameBar::changed);
        field.visible = false;
        field.active = false;
        return Gutter.anchor(field, 2, 120);
    }

    /** Folders' widgets are not part of the vanilla layout, so a resize has to move them itself. */
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void folders$reposition(CallbackInfo info) {
        Gutter.reposition(this);
    }

    /**
     * Shows and hides the name field to match whatever asked for a name.
     *
     * <p>Driven from the tick rather than pushed by the caller: the thing that wants a name is a
     * controller, which has no business holding a reference to a screen that may already have been
     * closed. Twenty times a second is well under the threshold for feeling immediate.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void folders$syncNameField(CallbackInfo info) {
        EditBox field = folders$nameField;
        if (field == null) {
            return;
        }
        boolean wanted = PackRenameBar.isActive();
        if (wanted == field.visible) {
            // Clicking anywhere else is how you finish typing.
            if (wanted && !field.isFocused()) {
                PackRenameBar.end();
            }
            return;
        }
        if (wanted) {
            field.setValue(PackRenameBar.initial());
            field.setHint(Component.translatable(PackRenameBar.labelKey()));
            field.visible = true;
            field.active = true;
            field.setFocused(true);
            setFocused(field);
        } else {
            field.visible = false;
            field.active = false;
            field.setFocused(false);
            if (getFocused() == field) {
                setFocused(null);
            }
        }
    }

    /**
     * Escape while naming something closes the field, not the screen.
     *
     * <p>Only while the field actually has focus: pressing Done moves focus to the button first, so
     * that still closes the screen on the first press.
     */
    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void folders$onClose(CallbackInfo info) {
        if (PackRenameBar.isActive() && folders$nameField != null && folders$nameField.isFocused()) {
            PackRenameBar.end();
            info.cancel();
            return;
        }
        Folders.dragManager().cancel();
        Folders.data().saveIfDirty();
        Folders.profiles().saveIfDirty();
    }
}
