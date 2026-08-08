package dev.miklires.folders.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The compact right-click menu
 *
 * <p>Not a {@code Screen}: it is drawn over the current one and fed input by the
 * integration, so opening it never disturbs the list underneath.
 */
public final class FolderContextMenu {

    public record Item(Component label, Runnable action, boolean enabled) {
        public static Item of(String key, Runnable action) {
            return new Item(Component.translatable(key), action, true);
        }

        public static Item of(String key, Runnable action, boolean enabled) {
            return new Item(Component.translatable(key), action, enabled);
        }
    }

    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 3;
    private static final int MIN_WIDTH = 70;
    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER = 0xFF3F3F5F;
    private static final int HOVER = 0x40FFFFFF;
    private static final int LABEL = 0xFFE0E0E0;
    private static final int LABEL_DISABLED = 0xFF6A6A6A;

    private final List<Item> items;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private FolderContextMenu(List<Item> items, int x, int y, int width, int height) {
        this.items = items;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * @param screenWidth  used to flip the menu left when it would run off the edge
     * @param screenHeight used to lift it up when it would run off the bottom
     */
    public static FolderContextMenu open(List<Item> items, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        List<Item> copy = List.copyOf(items);
        int width = MIN_WIDTH;
        for (Item item : copy) {
            width = Math.max(width, GuiCompat.width(item.label()) + PADDING * 2 + 4);
        }
        int height = copy.size() * ROW_HEIGHT + PADDING * 2;

        int x = mouseX + width > screenWidth ? Math.max(0, mouseX - width) : mouseX;
        int y = mouseY + height > screenHeight ? Math.max(0, screenHeight - height) : mouseY;
        return new FolderContextMenu(copy, x, y, width, height);
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiCompat.fill(graphics, x, y, x + width, y + height, BACKGROUND);
        GuiCompat.outline(graphics, x, y, width, height, BORDER);

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            int rowY = y + PADDING + i * ROW_HEIGHT;
            if (item.enabled() && isOverRow(mouseX, mouseY, rowY)) {
                GuiCompat.fill(graphics, x + 1, rowY - 1, x + width - 1, rowY + ROW_HEIGHT - 2, HOVER);
            }
            GuiCompat.trimmedText(graphics, item.label(), x + PADDING, rowY, width - PADDING * 2,
                    item.enabled() ? LABEL : LABEL_DISABLED);
        }
    }

    private boolean isOverRow(double mouseX, double mouseY, int rowY) {
        return mouseX >= x && mouseX < x + width && mouseY >= rowY - 1 && mouseY < rowY + ROW_HEIGHT - 2;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * @return true if the click was consumed. A click outside simply closes the
     *         menu without falling through to the list.
     */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item.enabled() && isOverRow(mouseX, mouseY, y + PADDING + i * ROW_HEIGHT)) {
                item.action().run();
                return true;
            }
        }
        return true;
    }

    /** Convenience for building a menu whose contents depend on the folder type. */
    public static final class Builder {
        private final List<Item> items = new ArrayList<>();

        public Builder add(String key, Runnable action) {
            items.add(Item.of(key, action));
            return this;
        }

        public Builder add(String key, Runnable action, boolean enabled) {
            items.add(Item.of(key, action, enabled));
            return this;
        }

        public List<Item> build() {
            return List.copyOf(items);
        }
    }
}
