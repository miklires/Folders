package dev.miklires.folders.client.integration.pack;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.client.integration.FolderEntryDelegate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A folder inside one of the resource pack lists.
 *
 * <p>Unlike the world and server entries this one has to satisfy a constructor that wants a pack,
 * so it carries a {@link FolderPackStub}. Every method that would touch the pack is overridden, and
 * the stub does nothing regardless, so a folder row can never select, deselect or reorder anything.
 *
 * <p>MAPPING NOTE: the least certain class in the mod. The world and server entries were checked
 * against working 26.2 code; this constructor was not, and {@code TransferableSelectionList.PackEntry}
 * may want a different argument list.
 */
public final class PackFolderEntry extends TransferableSelectionList.PackEntry {

    private final FolderEntryDelegate delegate;
    private final TransferableSelectionList list;

    public PackFolderEntry(PackListIntegration integration, TransferableSelectionList list, Folder folder) {
        super(Minecraft.getInstance(), list,
                new FolderPackStub("folders/" + folder.id(), Component.literal(folder.name())));
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
        // Deliberately does not fall through to super: the vanilla pack entry binds the arrow keys
        // to moving a pack between the two lists, which must not happen to a folder row.
        return delegate.keyPressed(keyCode, scanCode, modifiers, list.getBottom());
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
