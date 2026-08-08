package dev.miklires.folders.ui;

import dev.miklires.folders.core.data.Folder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Draws a folder row so it reads as part of the list it lives in (§20, §69):
 * a 32px icon on the left, the name where a world or server name would be, and a
 * dimmer statistics line beneath. No cards, no gradients, no rounded corners.
 */
public final class FolderRowRenderer {

    public static final int ICON_SIZE = 32;
    public static final int TEXT_OFFSET_X = ICON_SIZE + 3;

    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int STATS_COLOR = 0xFF808080;
    private static final int DROP_BORDER = 0xFF7FE07F;
    private static final int DROP_FILL = 0x2255DD55;
    private static final int ARROW_SCRIM = 0xA0000000;

    private static final int ONLINE_COLOR = 0xFF4CAF50;
    private static final int OFFLINE_COLOR = 0xFFB03030;
    private static final int UNKNOWN_COLOR = 0xFF6E6E6E;

    /** Online state of a closed server folder (§32). */
    public enum OnlineState {
        NONE,
        ONLINE,
        UNKNOWN,
        FAILED
    }

    /** Everything a row needs to draw itself, gathered by the integration layer. */
    public record RowContext(Folder folder,
                             FolderStats stats,
                             String countKey,
                             String highlightKey,
                             OnlineState online,
                             boolean hovered,
                             boolean iconHovered,
                             boolean dropTarget,
                             float expansion,
                             float alpha) {
    }

    private FolderRowRenderer() {
    }

    public static void render(DrawContext context, RowContext row, int x, int y, int width, int height) {
        int tint = GuiCompat.alphaTint(row.alpha());

        if (row.dropTarget()) {
            // §17: only ever highlighted when the drop would actually be accepted.
            GuiCompat.fill(context, x - 2, y - 1, x + width + 2, y + height - 1, DROP_FILL);
            GuiCompat.outline(context, x - 2, y - 1, width + 4, height, DROP_BORDER);
        }

        Identifier icon = IconManager.textureFor(row.folder());
        GuiCompat.drawTexture(context, icon, x, y, ICON_SIZE, ICON_SIZE, tint);

        if (row.iconHovered()) {
            renderArrow(context, row, x, y);
        }
        if (row.online() != OnlineState.NONE) {
            renderOnlineDot(context, row.online(), x, y, row.alpha());
        }

        int textX = x + TEXT_OFFSET_X;
        int textWidth = Math.max(0, width - TEXT_OFFSET_X - 4);

        GuiCompat.drawTrimmedText(context, Text.literal(row.folder().name()), textX, y + 1, textWidth,
                GuiCompat.fadeColor(NAME_COLOR, row.alpha()), false);

        Text subtitle = row.stats().describe(row.countKey(), row.highlightKey(), row.dropTarget());
        GuiCompat.drawTrimmedText(context, subtitle, textX, y + 12, textWidth,
                GuiCompat.fadeColor(STATS_COLOR, row.alpha()), false);
    }

    /**
     * The interactive arrow from §24: drawn as a texture rather than a Unicode
     * glyph so it matches the rest of the GUI, and only over the icon so it cannot
     * be confused with clicking the name (§25).
     */
    private static void renderArrow(DrawContext context, RowContext row, int x, int y) {
        GuiCompat.fill(context, x, y, x + ICON_SIZE, y + ICON_SIZE, ARROW_SCRIM);
        Identifier arrow = row.expansion() >= 0.5f ? FolderTextures.ARROW_UP : FolderTextures.ARROW_DOWN;
        int size = 16;
        int offset = (ICON_SIZE - size) / 2;
        GuiCompat.drawTexture(context, arrow, x + offset, y + offset, size, size,
                GuiCompat.alphaTint(row.alpha()));
    }

    /** A 3px dot in the icon corner — present, but not noisy (§32). */
    private static void renderOnlineDot(DrawContext context, OnlineState state, int x, int y, float alpha) {
        int color = switch (state) {
            case ONLINE -> ONLINE_COLOR;
            case FAILED -> OFFLINE_COLOR;
            default -> UNKNOWN_COLOR;
        };
        int right = x + ICON_SIZE - 2;
        int bottom = y + ICON_SIZE - 2;
        GuiCompat.fill(context, right - 4, bottom - 4, right, bottom, GuiCompat.fadeColor(0xFF000000, alpha));
        GuiCompat.fill(context, right - 3, bottom - 3, right - 1, bottom - 1, GuiCompat.fadeColor(color, alpha));
    }

    /** True when the pointer is over the icon, which is the arrow's hit area (§24). */
    public static boolean isOverIcon(int rowX, int rowY, double mouseX, double mouseY) {
        return mouseX >= rowX && mouseX < rowX + ICON_SIZE
                && mouseY >= rowY && mouseY < rowY + ICON_SIZE;
    }
}
