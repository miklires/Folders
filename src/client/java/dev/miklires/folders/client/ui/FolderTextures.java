package dev.miklires.folders.client.ui;

import dev.miklires.folders.Folders;
import net.minecraft.resources.Identifier;

/**
 * The mod's own textures. Nothing vanilla is replaced — these are only ever used
 * for folder rows.
 */
public final class FolderTextures {

    public static final Identifier FOLDER_CLOSED = of("textures/gui/folder_closed.png");
    public static final Identifier FOLDER_OPEN = of("textures/gui/folder_open.png");

    /** Overlay shown on the icon while the row is hovered. */
    public static final Identifier ARROW_DOWN = of("textures/gui/arrow_down.png");
    public static final Identifier ARROW_UP = of("textures/gui/arrow_up.png");

    private FolderTextures() {
    }

    private static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(Folders.MOD_ID, path);
    }
}
