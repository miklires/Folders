package dev.miklires.folders.client.integration.pack;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;

/**
 * A do-nothing {@link PackSelectionModel.Entry} that lets a folder row exist in a list typed to
 * hold packs.
 *
 * <p>{@code TransferableSelectionList} is an {@code ObjectSelectionList<PackEntry>}, so every row
 * must be a pack entry and every pack entry must hold a pack. Rather than pass a null and hope
 * nothing dereferences it, {@link PackFolderEntry} carries this: every accessor answers something
 * harmless and every mutator does nothing. If a vanilla code path ever does reach a folder row, it
 * selects nothing, moves nothing and changes no pack state.
 *
 * <p>MAPPING NOTE: {@code PackSelectionModel.Entry} is an interface. Should it become a class, this
 * becomes a subclass with the same overrides.
 */
public final class FolderPackStub implements PackSelectionModel.Entry {

    private final String id;
    private final Component title;

    public FolderPackStub(String id, Component title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getDescription() {
        return Component.empty();
    }

    @Override
    public PackCompatibility getCompatibility() {
        return PackCompatibility.COMPATIBLE;
    }

    @Override
    public PackSource getPackSource() {
        return PackSource.DEFAULT;
    }

    @Override
    public boolean isFixedPosition() {
        return false;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean canSelect() {
        return false;
    }

    @Override
    public boolean canUnselect() {
        return false;
    }

    @Override
    public boolean canMoveUp() {
        return false;
    }

    @Override
    public boolean canMoveDown() {
        return false;
    }

    @Override
    public void select() {
        // A folder is not a pack; selecting it must mean nothing.
    }

    @Override
    public void unselect() {
    }

    @Override
    public void moveUp() {
    }

    @Override
    public void moveDown() {
    }
}
