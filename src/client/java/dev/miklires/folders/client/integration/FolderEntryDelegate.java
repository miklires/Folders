package dev.miklires.folders.client.integration;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.FolderRowRenderer;
import dev.miklires.folders.client.ui.GuiCompat;
import dev.miklires.folders.core.data.Folder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Everything a folder row does, independent of which list it is in.
 *
 * <p>Each screen's entry class is a short subclass of its own vanilla {@code Entry} that forwards
 * here, which is what stops the same interaction code being written three times and keeps the mod
 * off any one entry API.
 */
public final class FolderEntryDelegate {

    private static final int EDIT_BACKGROUND = 0xFF000000;
    private static final int EDIT_BORDER = 0xFFA0A0A0;
    private static final int EDIT_TEXT = 0xFFFFFFFF;
    private static final int CARET = 0xFFD0D0D0;

    /** Caret blink period; matches the feel of a vanilla text field. */
    private static final long BLINK_MS = 600L;

    private final FolderListController<?> controller;
    private final Folder folder;

    /**
     * The rename buffer.
     *
     * <p>Written by hand rather than with an {@code EditBox}: a widget has to be positioned,
     * focused and drawn by a screen, and a row inside a list is none of those things. A folder name
     * is a single short line with no selection or scrolling, so the whole editor is the few methods
     * below.
     */
    private StringBuilder buffer;
    private long editingSince;

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
            boolean renaming = controller.isRenaming(folder.id());
            if (!renaming) {
                discardBuffer();
            }
            FolderRowRenderer.render(graphics,
                    controller.rowContextFor(folder, x, y, mouseX, mouseY, hovered && !renaming, 1.0f),
                    x, y, width, height);
            if (renaming) {
                renderEditor(graphics, x, y, width);
            }
        });
    }

    /** Draws the name field over the name the row just drew. */
    private void renderEditor(GuiGraphicsExtractor graphics, int x, int y, int width) {
        int left = x + FolderRowRenderer.TEXT_OFFSET_X - 1;
        int right = x + width - 4;
        int top = y;
        int bottom = y + GuiCompat.lineHeight() + 2;

        GuiCompat.fill(graphics, left, top, right, bottom, EDIT_BACKGROUND);
        GuiCompat.outline(graphics, left, top, right - left, bottom - top, EDIT_BORDER);

        String text = buffer().toString();
        int textX = left + 3;
        GuiCompat.trimmedText(graphics, Component.literal(text), textX, top + 2, right - textX - 4, EDIT_TEXT);

        boolean caretVisible = ((System.currentTimeMillis() - editingSince) / BLINK_MS) % 2 == 0;
        if (caretVisible) {
            int caretX = Math.min(textX + GuiCompat.width(Component.literal(text)), right - 2);
            GuiCompat.fill(graphics, caretX, top + 2, caretX + 1, bottom - 2, CARET);
        }
    }

    private StringBuilder buffer() {
        if (buffer == null) {
            buffer = new StringBuilder(folder.name());
            editingSince = System.currentTimeMillis();
        }
        return buffer;
    }

    private void discardBuffer() {
        buffer = null;
    }

    // ------------------------------------------------------------------
    // Mouse
    // ------------------------------------------------------------------

    /**
     * @param click          26.2 hands the whole event across, including whether this was the
     *                       second click of a pair, so the rename gesture needs no timer of its own
     * @param rowX,rowY      where the row was placed, to tell the icon area from the name
     * @param viewportBottom bottom of the list, so opening near the edge can scroll
     * @return true if the click was consumed
     */
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled,
                                int rowX, int rowY, int viewportBottom) {
        if (controller.isRenaming(folder.id())) {
            // Clicks land in the field while renaming rather than falling through to the row.
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            controller.openContextMenu(folder, (int) click.x(), (int) click.y());
            return true;
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (doubled) {
            // A double click renames, checked before the icon test so a quick double click
            // anywhere on the row does the same thing.
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

    /**
     * MAPPING NOTE: {@code KeyEvent} and {@code CharacterEvent} are 26.2's replacement for the old
     * loose {@code (keyCode, scanCode, modifiers)} triples. Their accessors are read in exactly one
     * place each, here, so a wrong guess is a two-line fix.
     */
    private static int keyOf(KeyEvent event) {
        return event.key();
    }

    private static char charOf(CharacterEvent event) {
        return (char) event.codepoint();
    }

    public boolean keyPressed(KeyEvent event, int viewportBottom) {
        int key = keyOf(event);
        if (controller.isRenaming(folder.id())) {
            return renameKeyPressed(key);
        }
        return switch (key) {
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

    private boolean renameKeyPressed(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                // An empty name is refused and the field stays open.
                if (controller.commitRename(folder.id(), buffer().toString())) {
                    discardBuffer();
                }
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                controller.cancelRename();
                discardBuffer();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                StringBuilder text = buffer();
                if (!text.isEmpty()) {
                    text.deleteCharAt(text.length() - 1);
                }
                return true;
            }
            default -> {
                // Everything else is swallowed so the list does not scroll or change selection
                // under a field that is being typed into.
                return true;
            }
        }
    }

    public boolean charTyped(CharacterEvent event) {
        if (!controller.isRenaming(folder.id())) {
            return false;
        }
        char typed = charOf(event);
        StringBuilder text = buffer();
        if (typed >= ' ' && typed != 127 && text.length() < Folder.MAX_NAME_LENGTH) {
            text.append(typed);
        }
        return true;
    }

    /** Open or close, as the keyboard and a double click both mean. */
    public void activate(int viewportBottom) {
        controller.toggle(folder, viewportBottom);
    }

    public Component narration() {
        return controller.narrationFor(folder);
    }
}
