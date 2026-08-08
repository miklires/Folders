package dev.miklires.folders.integration.pack;

import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.text.Text;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackSource;

/**
 * A do-nothing {@link ResourcePackOrganizer.Pack} that lets a folder row exist in
 * a list typed to hold packs.
 *
 * <p>{@code PackListWidget} is an {@code EntryListWidget<ResourcePackEntry>}, so
 * every row must be a pack entry, and every pack entry must hold a pack. Rather
 * than reach for a null and hope nothing dereferences it,
 * {@link PackFolderEntry} carries this: every accessor answers something harmless
 * and every mutator does nothing, so if a vanilla code path ever does reach a
 * folder row it changes no pack state (§48 — group on top of the pack manager,
 * do not reach into it).
 *
 * <p>MAPPING NOTE: {@code ResourcePackOrganizer.Pack} is an interface in Yarn.
 * Should it become a class, this becomes a subclass with the same overrides.
 */
public final class FolderPackStub implements ResourcePackOrganizer.Pack {

    private final String id;
    private final Text displayName;

    public FolderPackStub(String id, Text displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public Text getDisplayName() {
        return displayName;
    }

    @Override
    public Text getDescription() {
        return Text.empty();
    }

    @Override
    public ResourcePackCompatibility getCompatibility() {
        return ResourcePackCompatibility.COMPATIBLE;
    }

    @Override
    public ResourcePackSource getSource() {
        return ResourcePackSource.NONE;
    }

    @Override
    public boolean isPinned() {
        return false;
    }

    @Override
    public boolean isAlwaysEnabled() {
        return false;
    }

    @Override
    public boolean canBeEnabled() {
        return false;
    }

    @Override
    public boolean canBeDisabled() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void enable() {
        // A folder is not a pack; enabling it must mean nothing.
    }

    @Override
    public void disable() {
    }

    @Override
    public void toggle() {
    }
}
