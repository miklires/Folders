package dev.miklires.folders.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

/**
 * Every drawing primitive the mod uses, in one place.
 *
 * <p>26.2 draws through {@link GuiGraphicsExtractor}: widgets extract a render state rather than
 * issuing draw calls, and the signatures moved with it. Routing the whole mod through the handful
 * of methods below means the next time that happens it is one file to fix rather than a hunt
 * through the renderer, the entries and the context menu.
 *
 * <p>No raw OpenGL here or anywhere else in the mod — everything goes through the extractor and
 * {@link RenderPipelines}.
 *
 * <p>There is no depth helper: 26.2's pose stack is a {@code Matrix3x2fStack}, purely 2D, so
 * "on top" means "submitted later". The ghost preview and the context menu are drawn at the tail of
 * the list's render for that reason.
 */
public final class GuiCompat {

    public static final int OPAQUE = 0xFFFFFFFF;

    private GuiCompat() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int lineHeight() {
        return font().lineHeight;
    }

    public static int width(Component text) {
        return font().width(text);
    }

    /** Solid rectangle. {@code color} is ARGB. */
    public static void fill(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    /** One-pixel outline, used for the drop-zone highlight. */
    public static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * Draws a whole texture scaled into a box.
     *
     * @param tint ARGB; {@link #OPAQUE} for none. The alpha channel is what fades rows in while a
     *             folder is opening.
     */
    public static void blit(GuiGraphicsExtractor graphics, Identifier texture,
                            int x, int y, int width, int height, int tint) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f,
                width, height, width, height, width, height, tint);
    }

    /** White at the given opacity, for tinting a texture. */
    public static int alpha(float opacity) {
        return ARGB.white(Math.clamp(opacity, 0.0f, 1.0f));
    }

    /** Applies an opacity to a colour that already carries an alpha. */
    public static int fade(int argb, float opacity) {
        int a = (int) (ARGB.alpha(argb) * Math.clamp(opacity, 0.0f, 1.0f));
        return ARGB.color(a, ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb));
    }

    public static void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(font(), text.getVisualOrderText(), x, y, color);
    }

    /** Draws text truncated with an ellipsis, so a long folder name never bleeds past its row. */
    public static void trimmedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth,
                                   int color) {
        Font font = font();
        FormattedCharSequence visual = text.getVisualOrderText();
        if (font.width(visual) <= maxWidth) {
            graphics.text(font, visual, x, y, color);
            return;
        }
        String trimmed = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("...")));
        graphics.text(font, Component.literal(trimmed + "...").getVisualOrderText(), x, y, color);
    }

    /**
     * Clips subsequent drawing to a box; always pair with {@link #popScissor}.
     *
     * <p>This is what keeps a half-revealed row inside its folder's band instead of spilling over
     * the row below it.
     */
    public static void pushScissor(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    public static void popScissor(GuiGraphicsExtractor graphics) {
        graphics.disableScissor();
    }

}
