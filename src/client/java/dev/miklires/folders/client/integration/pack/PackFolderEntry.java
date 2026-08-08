package dev.miklires.folders.client.integration.pack;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.integration.FolderEntryDelegate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.network.chat.Component;

/**
 * A folder inside one of the resource pack lists.
 *
 * <p>Unlike the world and server entries this one has to satisfy a constructor
 * that wants a pack, so it carries a {@link FolderPackStub}. Every method that
 * would touch the pack is overridden, and the stub does nothing regardless, so a
 * folder row can never enable, disable or reorder anything.
 */
public final class PackFolderEntry extends TransferableSelectionList.PackEntry {

    private final FolderEntryDelegate delegate;
    private final TransferableSelectionList widget;

    private int lastX;
    private int lastY;

    public PackFolderEntry(PackListIntegration integration, TransferableSelectionList widget, Folder folder) {
        super(Minecraft.getInstance(), widget,
                new FolderPackStub("folders/" + folder.id(), Component.literal(folder.name())));
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
        // Deliberately does not fall through to super: the vanilla pack entry binds
        // the arrow keys to moving a pack between lists, which must not happen to a
        // folder row.
        return delegate.keyPressed(keyCode, scanCode, modifiers, widget.getBottom());
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return delegate.charTyped(chr, modifiers);
    }

    @Override
    public Component getNarration() {
        return delegate.narration();
    }
}
