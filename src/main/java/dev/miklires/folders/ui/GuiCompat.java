package dev.miklires.folders.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Every drawing primitive Folders uses, in one place.
 *
 * <p>This exists because {@link DrawContext} is the fastest-moving API in the
 * client: texture drawing, tinting and scissoring have all been reshaped more
 * than once, and 26.2's Blaze3D work moves them again. Routing the whole mod
 * through eight methods means a mappings bump is a single-file fix instead of a
 * hunt through the renderer, the entries and the context menu.
 *
 * <p>No raw OpenGL here or anywhere else in the mod (§68, §77.14) — everything
 * goes through {@code DrawContext}.
 *
 * <p>MAPPING NOTE: this is the file to check first against real 26.2 mappings.
 * The signatures below are the 1.21.x shapes.
 */
public final class GuiCompat {

    private GuiCompat() {
    }

    public static TextRenderer textRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static int fontHeight() {
        return textRenderer().fontHeight;
    }

    public static int textWidth(Text text) {
        return textRenderer().getWidth(text);
    }

    /** Solid rectangle. {@code color} is ARGB. */
    public static void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    /** One-pixel outline, used for the drop-zone highlight (§17). */
    public static void outline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * Draws a full texture scaled into a box, optionally tinted.
     *
     * @param tint ARGB; use {@link #OPAQUE} for no tint. The alpha channel is what
     *             fades rows in during the expand animation (§27).
     */
    public static void drawTexture(DrawContext context, Identifier texture,
                                   int x, int y, int width, int height, int tint) {
        context.drawTexture(RenderLayer::getGuiTextured, texture,
                x, y, 0.0f, 0.0f, width, height, width, height, width, height, tint);
    }

    public static final int OPAQUE = 0xFFFFFFFF;

    /** Packs an ARGB white tint at the given alpha, for fading. */
    public static int alphaTint(float alpha) {
        int a = (int) (Math.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        return (a << 24) | 0x00FFFFFF;
    }

    /** Applies an alpha to a text colour that already has one. */
    public static int fadeColor(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.clamp(alpha, 0.0f, 1.0f));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static void drawText(DrawContext context, Text text, int x, int y, int color, boolean shadow) {
        context.drawText(textRenderer(), text, x, y, color, shadow);
    }

    /** Draws text truncated with an ellipsis so a long folder name never bleeds. */
    public static void drawTrimmedText(DrawContext context, Text text, int x, int y, int maxWidth,
                                       int color, boolean shadow) {
        TextRenderer renderer = textRenderer();
        if (renderer.getWidth(text) <= maxWidth) {
            context.drawText(renderer, text, x, y, color, shadow);
            return;
        }
        String trimmed = renderer.trimToWidth(text.getString(), Math.max(0, maxWidth - renderer.getWidth("...")));
        context.drawText(renderer, trimmed + "...", x, y, color, shadow);
    }

    /** Clips subsequent drawing to a box; always pair with {@link #popScissor}. */
    public static void pushScissor(DrawContext context, int x1, int y1, int x2, int y2) {
        context.enableScissor(x1, y1, x2, y2);
    }

    public static void popScissor(DrawContext context) {
        context.disableScissor();
    }

    /**
     * Raises subsequent drawing above the list, for the ghost preview and the
     * context menu.
     *
     * <p>MAPPING NOTE: the matrix-stack translate is the 1.21.x way to do this.
     */
    public static void pushLayer(DrawContext context, float z) {
        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, z);
    }

    public static void popLayer(DrawContext context) {
        context.getMatrices().pop();
    }
}
