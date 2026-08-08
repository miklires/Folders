package dev.miklires.folders.client.integration.server;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.integration.FolderEntryDelegate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A folder inside the multiplayer list — a thin shell over {@link FolderEntryDelegate}, like its
 * world and pack siblings.
 */
public final class ServerFolderEntry extends ServerSelectionList.Entry {

    private final FolderEntryDelegate delegate;
    private final ServerSelectionList list;

    public ServerFolderEntry(ServerListIntegration integration, ServerSelectionList list, Folder folder) {
        this.delegate = new FolderEntryDelegate(integration, folder);
        this.list = list;
    }

    public Folder folder() {
        return delegate.folder();
    }

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                               boolean hovered, float delta) {
        delegate.render(graphics, getX(), getContentY(), getContentWidth(), getContentHeight(),
                mouseX, mouseY, hovered);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        return delegate.mouseClicked(click, doubleClick, getX(), getContentY(), list.getBottom());
    }

    /**
     * What the list does when a row is activated by keyboard or double-clicked — joining, for a
     * server. Opening is a folder's equivalent, so both ways in behave the same.
     */
    @Override
    public void join() {
        delegate.activate(list.getBottom());
    }

    /**
     * Whether {@code other} stands for the same row, so the list can keep its selection across a
     * rebuild. Rows are rebuilt from scratch whenever the model changes, so object identity is no
     * use; the folder id is the thing that persists.
     */
    @Override
    public boolean matches(ServerSelectionList.Entry other) {
        return other instanceof ServerFolderEntry entry && entry.folder().id().equals(folder().id());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return delegate.keyPressed(event, list.getBottom()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return delegate.charTyped(event) || super.charTyped(event);
    }

    @Override
    public Component getNarration() {
        return delegate.narration();
    }
}
