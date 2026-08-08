package dev.miklires.folders.integration.server;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.integration.FolderEntryDelegate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.text.Text;

/**
 * A folder inside the multiplayer list. A thin shell over
 * {@link FolderEntryDelegate}, like its world and pack siblings (§45).
 */
public final class ServerFolderEntry extends MultiplayerServerListWidget.Entry {

    private final FolderEntryDelegate delegate;
    private final MultiplayerServerListWidget widget;

    private int lastX;
    private int lastY;

    public ServerFolderEntry(ServerListIntegration integration, MultiplayerServerListWidget widget, Folder folder) {
        this.delegate = new FolderEntryDelegate(integration, folder);
        this.widget = widget;
    }

    public Folder folder() {
        return delegate.folder();
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.lastX = x;
        this.lastY = y;
        delegate.render(context, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return delegate.mouseClicked(mouseX, mouseY, button, lastX, lastY, widget.getBottom());
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
    public Text getNarration() {
        return delegate.narration();
    }
}
