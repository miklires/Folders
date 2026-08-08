package dev.miklires.folders.client.integration.world;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.integration.FolderEntryDelegate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A folder inside the world list. Everything it does lives in
 * {@link FolderEntryDelegate}; this class only exists to be the entry type the vanilla widget
 * expects, which is what keeps the mod off any one entry API.
 *
 * <p>26.2 hands rows no bounds: the list has already placed the entry by the time it draws, so the
 * geometry is read back off {@code this}. That is why nothing here caches a position between the
 * draw and the click.
 */
public final class WorldFolderEntry extends WorldSelectionList.Entry {

    private final FolderEntryDelegate delegate;
    private final WorldSelectionList list;

    public WorldFolderEntry(WorldListIntegration integration, WorldSelectionList list, Folder folder) {
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return delegate.keyPressed(keyCode, scanCode, modifiers, list.getBottom())
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

    /**
     * World rows are closeable because a real one holds an open level icon texture. A folder row
     * owns nothing the list allocated, so there is nothing to release.
     */
    @Override
    public void close() {
    }
}
