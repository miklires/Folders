package dev.miklires.folders.client.integration.pack;

import dev.miklires.folders.client.ui.FolderStats;
import dev.miklires.folders.client.ui.IconManager;
import dev.miklires.folders.core.data.Folder;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;

import java.util.UUID;

/**
 * A folder disguised as a resource pack.
 *
 * <p>The other two screens get folder rows by subclassing the list's entry type. The pack screen
 * takes the opposite route: its rows are built from a {@link PackSelectionModel.Entry}, a small and
 * fully public interface, so a folder can simply <em>be</em> one. Vanilla then draws the row —
 * icon, title, description — with no custom rendering, no geometry read back off the widget, and
 * nothing that breaks the next time the pack screen is reorganised.
 *
 * <p>Every mutating method is a no-op and every "can you" answers no, so vanilla offers none of the
 * select/move arrows on a folder row and nothing it does can reach the pack repository. What a
 * click means is decided in {@code PackEntryMixin}, which recognises the row by its id.
 */
public final class FolderPack implements PackSelectionModel.Entry {

    /**
     * Prefix marking a row as a folder rather than a pack.
     *
     * <p>Namespaced with the mod id so it cannot collide with a real pack: vanilla's own ids are
     * {@code vanilla}, {@code file/…} and {@code server/…}, and a pack that managed to call itself
     * {@code folders:folder/…} would need a colon in a filename.
     */
    private static final String PREFIX = "folders:folder/";

    private final Folder folder;
    private final Component description;

    public FolderPack(Folder folder, Component description) {
        this.folder = folder;
        this.description = description;
    }

    public Folder folder() {
        return folder;
    }

    public static String idFor(Folder folder) {
        return PREFIX + folder.id();
    }

    public static boolean isFolderId(String packId) {
        return packId != null && packId.startsWith(PREFIX);
    }

    /** The folder a row's pack id refers to, or {@code null} if it is a real pack. */
    public static UUID folderIdOf(String packId) {
        if (!isFolderId(packId)) {
            return null;
        }
        try {
            return UUID.fromString(packId.substring(PREFIX.length()));
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // What the row shows
    // ------------------------------------------------------------------

    @Override
    public String getId() {
        return idFor(folder);
    }

    @Override
    public Identifier getIconTexture() {
        return IconManager.textureFor(folder);
    }

    @Override
    public Component getTitle() {
        return Component.literal(folder.name());
    }

    @Override
    public Component getDescription() {
        return description;
    }

    /**
     * MAPPING NOTE: {@code PackSource.DEFAULT} is the pass-through source — the one that adds no
     * "(built-in)" style suffix to the title. {@code PackCompatibility.COMPATIBLE} keeps the row
     * out of the incompatible-pack confirmation flow. Both are read in exactly one place, here.
     */
    @Override
    public PackSource getPackSource() {
        return PackSource.DEFAULT;
    }

    @Override
    public PackCompatibility getCompatibility() {
        return PackCompatibility.COMPATIBLE;
    }

    // ------------------------------------------------------------------
    // What the row refuses to do
    // ------------------------------------------------------------------

    @Override
    public boolean isFixedPosition() {
        return false;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isSelected() {
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

    /** Used only for the folder stats line; {@link FolderStats} builds the text. */
    public static Component describe(FolderStats stats, String countKey) {
        return stats.describe(countKey, null, false);
    }
}
