package dev.miklires.folders.client.integration.server;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.integration.FolderEntryDelegate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;

/**
 * A folder inside the multiplayer list. A thin shell over
 * {@link FolderEntryDelegate}, like its world and pack siblings.
 */
public final class ServerFolderEntry extends ServerSelectionList.Entry {

    private final FolderEntryDelegate delegate;
    private final ServerSelectionList widget;

    private int lastX;
    private int lastY;

    public ServerFolderEntry(ServerListIntegration integration, ServerSelectionList widget, Folder folder) {
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
