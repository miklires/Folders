package dev.miklires.folders.client.integration.world;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.integration.FolderEntryDelegate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;

/**
 * A folder inside the world list. Everything it does lives in
 * {@link FolderEntryDelegate}; this class exists only to be the entry type the
 * vanilla widget expects.
 *
 * <p>MAPPING NOTE: the {@code render} signature of {@code AbstractSelectionList.Entry}
 * changes between versions. If it moves, it moves here and in the two sibling
 * entry classes, and nowhere else.
 */
public final class WorldFolderEntry extends WorldSelectionList.Entry {

    private final FolderEntryDelegate delegate;
    private final WorldSelectionList widget;

    /** Captured during render so mouse handlers know where the row was drawn. */
    private int lastX;
    private int lastY;

    public WorldFolderEntry(WorldListIntegration integration, WorldSelectionList widget, Folder folder) {
        this.delegate = new FolderEntryDelegate(integration, folder);
        this.widget = widget;
    }

    public Folder folder() {
        return delegate.folder();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.lastX = x;
        this.lastY = y;
        delegate.render(graphics, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return delegate.mouseClicked(click, doubled, lastX, lastY, widget.getBottom());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return delegate.keyPressed(keyCode, scanCode, modifiers, widget.getBottom())
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return delegate.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public Component getNarration() {
        return delegate.narration();
    }
}
