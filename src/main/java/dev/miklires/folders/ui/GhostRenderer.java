package dev.miklires.folders.ui;

import dev.miklires.folders.core.drag.DragManager;
import dev.miklires.folders.core.drag.DragPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The translucent label that follows the cursor while dragging (§18).
 *
 * <p>Purely decorative: it is drawn after the list and never takes input, so it
 * cannot interfere with vanilla clicks.
 */
public final class GhostRenderer {

    private static final int BACKGROUND = 0xC0101010;
    private static final int BORDER = 0x80FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int PADDING = 4;
    private static final float LAYER = 400.0f;

    private GhostRenderer() {
    }

    public static void render(DrawContext context, DragManager drag) {
        if (!drag.isDragging()) {
            return;
        }
        DragPayload payload = drag.payload().orElse(null);
        if (payload == null) {
            return;
        }

        Text label = Text.literal(payload.displayName());
        int width = GuiCompat.textWidth(label) + PADDING * 2;
        int height = GuiCompat.fontHeight() + PADDING * 2;

        // Offset so the ghost sits below-right of the cursor and never hides the
        // drop target the player is aiming at.
        int x = (int) drag.mouseX() + 8;
        int y = (int) drag.mouseY() + 8;

        GuiCompat.pushLayer(context, LAYER);
        GuiCompat.fill(context, x, y, x + width, y + height, BACKGROUND);
        GuiCompat.outline(context, x, y, width, height, BORDER);
        GuiCompat.drawText(context, label, x + PADDING, y + PADDING, TEXT, false);
        GuiCompat.popLayer(context);
    }
}
