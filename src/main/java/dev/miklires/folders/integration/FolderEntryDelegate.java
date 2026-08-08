package dev.miklires.folders.integration;

import dev.miklires.folders.Folders;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.ui.FolderRowRenderer;
import dev.miklires.folders.ui.GuiCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Everything a folder row does, independent of which list it is in.
 *
 * <p>Each screen's entry class is a five-line subclass of its own vanilla
 * {@code Entry} that forwards here, which is what stops the same interaction code
 * being written three times and keeps the mod off any one entry API (§45, §79).
 */
public final class FolderEntryDelegate {

    /** Two clicks within this window on the name start a rename (§12). */
    private static final long DOUBLE_CLICK_MS = 250L;

    private final FolderListController<?> controller;
    private final Folder folder;

    private TextFieldWidget renameField;
    private long lastNameClickMs;

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

    public void render(DrawContext context, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean hovered) {
        Folders.guarded("rendering a folder row", () -> {
            if (controller.isRenaming(folder.id())) {
                renderRenaming(context, x, y, width, height, mouseX, mouseY);
                return;
            }
            discardRenameField();
            FolderRowRenderer.render(context,
                    controller.rowContextFor(folder, x, y, mouseX, mouseY, hovered, 1.0f),
                    x, y, width, height);
        });
    }

    private void renderRenaming(DrawContext context, int x, int y, int width, int height,
                                int mouseX, int mouseY) {
        // The icon still draws, so the row does not visibly change shape while
        // being renamed; only the name becomes a field.
        FolderRowRenderer.render(context,
                controller.rowContextFor(folder, x, y, mouseX, mouseY, false, 1.0f),
                x, y, width, height);

        TextFieldWidget field = renameField();
        int fieldX = x + FolderRowRenderer.TEXT_OFFSET_X;
        field.setX(fieldX);
        field.setY(y + 1);
        field.setWidth(Math.max(40, width - FolderRowRenderer.TEXT_OFFSET_X - 6));
        // Cover the name the renderer just drew.
        GuiCompat.fill(context, fieldX - 1, y, x + width, y + 12, 0xFF000000);
        field.render(context, mouseX, mouseY, 0.0f);
    }

    private TextFieldWidget renameField() {
        if (renameField == null) {
            renameField = new TextFieldWidget(GuiCompat.textRenderer(), 0, 0, 100, 11,
                    Text.translatable("folders.menu.rename"));
            renameField.setMaxLength(Folder.MAX_NAME_LENGTH);
            renameField.setText(folder.name());
            renameField.setCursorToEnd(false);
            renameField.setSelectionEnd(0);
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
     * @param rowX,rowY      screen position of this row, needed to tell the icon
     *                       area from the name area (§24, §25)
     * @param viewportBottom bottom of the list, so opening near the edge can scroll
     * @return true if the click was consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int rowX, int rowY, int viewportBottom) {
        if (controller.isRenaming(folder.id())) {
            return renameField().mouseClicked(mouseX, mouseY, button);
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            controller.openContextMenu(folder, (int) mouseX, (int) mouseY);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (FolderRowRenderer.isOverIcon(rowX, rowY, mouseX, mouseY)) {
            // The arrow: the one place a click is unambiguously "open/close" (§25).
            controller.toggle(folder, viewportBottom);
            return true;
        }

        long now = System.currentTimeMillis();
        boolean doubleClick = now - lastNameClickMs <= DOUBLE_CLICK_MS;
        lastNameClickMs = now;
        if (doubleClick) {
            controller.beginRename(folder.id());
            return true;
        }

        // A press on the body might still become a drag; the controller decides
        // once the pointer has moved far enough (§16).
        controller.pressFolder(folder, mouseX, mouseY);
        controller.toggle(folder, viewportBottom);
        return true;
    }

    // ------------------------------------------------------------------
    // Keyboard (§40)
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
                // An empty name is refused and the field stays open (§12).
                if (controller.commitRename(folder.id(), renameField().getText())) {
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

    public boolean charTyped(char chr, int modifiers) {
        return controller.isRenaming(folder.id()) && renameField().charTyped(chr, modifiers);
    }

    // ------------------------------------------------------------------

    public Text narration() {
        return controller.narrationFor(folder);
    }
}
