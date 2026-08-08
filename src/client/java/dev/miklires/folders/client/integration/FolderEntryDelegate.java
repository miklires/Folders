package dev.miklires.folders.client.integration;

import dev.miklires.folders.Folders;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.ui.FolderRowRenderer;
import dev.miklires.folders.client.ui.GuiCompat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Everything a folder row does, independent of which list it is in.
 *
 * <p>Each screen's entry class is a five-line subclass of its own vanilla
 * {@code Entry} that forwards here, which is what stops the same interaction code
 * being written three times and keeps the mod off any one entry API.
 */
public final class FolderEntryDelegate {

    private final FolderListController<?> controller;
    private final Folder folder;

    private EditBox renameField;

    public FolderEntryDelegate(FolderListController<?> controller, Folder folder) {
        this.controller = controller;
        this.folder = folder;
    }

    public Folder folder() {
        return folder;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean hovered) {
        Folders.guarded("rendering a folder row", () -> {
            if (controller.isRenaming(folder.id())) {
                renderRenaming(graphics, x, y, width, height, mouseX, mouseY);
                return;
            }
            discardRenameField();
            FolderRowRenderer.render(graphics,
                    controller.rowContextFor(folder, x, y, mouseX, mouseY, hovered, 1.0f),
                    x, y, width, height);
        });
    }

    private void renderRenaming(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                int mouseX, int mouseY) {
        // The icon still draws, so the row does not visibly change shape while
        // being renamed; only the name becomes a field.
        FolderRowRenderer.render(graphics,
                controller.rowContextFor(folder, x, y, mouseX, mouseY, false, 1.0f),
                x, y, width, height);

        EditBox field = renameField();
        int fieldX = x + FolderRowRenderer.TEXT_OFFSET_X;
        field.setX(fieldX);
        field.setY(y + 1);
        field.setWidth(Math.max(40, width - FolderRowRenderer.TEXT_OFFSET_X - 6));
        // Cover the name the renderer just drew.
        GuiCompat.fill(graphics, fieldX - 1, y, x + width, y + 12, 0xFF000000);
        field.render(graphics, mouseX, mouseY, 0.0f);
    }

    private EditBox renameField() {
        if (renameField == null) {
            renameField = new EditBox(GuiCompat.font(), 0, 0, 100, 11,
                    Component.translatable("folders.menu.rename"));
            renameField.setMaxLength(Folder.MAX_NAME_LENGTH);
            renameField.setValue(folder.name());
            renameField.moveCursorToEnd(false);
            renameField.setHighlightPos(0);
            renameField.setFocused(true);
        }
        return renameField;
    }

    private void discardRenameField() {
        renameField = null;
    }

    // ------------------------------------------------------------------
    // Mouse
    // ------------------------------------------------------------------

    /**
     * @param click          26.2 hands the whole event across, including whether this was the
     *                       second click of a pair — so the rename gesture needs no timer of its own
     * @param rowX,rowY      screen position of this row, needed to tell the icon area from the name
     * @param viewportBottom bottom of the list, so opening near the edge can scroll
     * @return true if the click was consumed
     */
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled,
                                int rowX, int rowY, int viewportBottom) {
        if (controller.isRenaming(folder.id())) {
            return renameField().mouseClicked(click, doubled);
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            controller.openContextMenu(folder, (int) click.x(), (int) click.y());
            return true;
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (doubled) {
            // Double click on the row renames it. Checked before the icon test so a quick
            // double click anywhere on the folder does the same thing.
            controller.beginRename(folder.id());
            return true;
        }

        if (FolderRowRenderer.isOverIcon(rowX, rowY, click.x(), click.y())) {
            // The arrow is the one place a click is unambiguously "open or close".
            controller.toggle(folder, viewportBottom);
            return true;
        }

        // A press on the body might still become a drag; the controller decides once the pointer
        // has moved far enough. Until then this is an ordinary click and opens the folder.
        controller.pressFolder(folder, click.x(), click.y());
        controller.toggle(folder, viewportBottom);
        return true;
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, int viewportBottom) {
        if (controller.isRenaming(folder.id())) {
            return renameKeyPressed(keyCode, scanCode, modifiers);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                controller.toggle(folder, viewportBottom);
                yield true;
            }
            case GLFW.GLFW_KEY_F2 -> {
                controller.beginRename(folder.id());
                yield true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                controller.deleteFolder(folder);
                yield true;
            }
            default -> false;
        };
    }

    private boolean renameKeyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                // An empty name is refused and the field stays open.
                if (controller.commitRename(folder.id(), renameField().getValue())) {
                    discardRenameField();
                }
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                controller.cancelRename();
                discardRenameField();
                return true;
            }
            default -> {
                return renameField().keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    /** Open or close, as the keyboard and a double click both mean. */
    public void activate(int viewportBottom) {
        controller.toggle(folder, viewportBottom);
    }

    public boolean charTyped(char chr, int modifiers) {
        return controller.isRenaming(folder.id()) && renameField().charTyped(chr, modifiers);
    }

    // ------------------------------------------------------------------

    public Component narration() {
        return controller.narrationFor(folder);
    }
}
