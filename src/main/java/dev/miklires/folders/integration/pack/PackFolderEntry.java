package dev.miklires.folders.integration.pack;

import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.integration.FolderEntryDelegate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.text.Text;

/**
 * A folder inside one of the resource pack lists.
 *
 * <p>Unlike the world and server entries this one has to satisfy a constructor
 * that wants a pack, so it carries a {@link FolderPackStub}. Every method that
 * would touch the pack is overridden, and the stub does nothing regardless, so a
 * folder row can never enable, disable or reorder anything (§48).
 */
public final class PackFolderEntry extends PackListWidget.ResourcePackEntry {

    private final FolderEntryDelegate delegate;
    private final PackListWidget widget;

    private int lastX;
    private int lastY;

    public PackFolderEntry(PackListIntegration integration, PackListWidget widget, Folder folder) {
        super(MinecraftClient.getInstance(), widget,
                new FolderPackStub("folders/" + folder.id(), Text.literal(folder.name())));
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
    public Text getNarration() {
        return delegate.narration();
    }
}
