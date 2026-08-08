package dev.miklires.folders;

import dev.miklires.folders.core.FoldersData;
import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.core.drag.DragManager;
import dev.miklires.folders.core.storage.FoldersSettings;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod-wide handles. Kept deliberately thin: this is a lookup point, not a place
 * for logic (§44).
 */
public final class Folders {
    public static final String MOD_ID = "folders";
    public static final Logger LOGGER = LoggerFactory.getLogger("Folders");

    /** One drag manager for the whole game — see §15. */
    private static final DragManager DRAG_MANAGER = new DragManager();

    private static FoldersData data;

    private Folders() {
    }

    static void initialise() {
        data = FoldersData.load(FabricLoader.getInstance().getConfigDir());
    }

    /**
     * Lazily loads on first use so a screen opened before the initialiser ran
     * still works, and so the tests never need a Fabric environment.
     */
    public static FoldersData data() {
        if (data == null) {
            initialise();
        }
        return data;
    }

    public static FolderRepository repository(FolderType type) {
        return data().repository(type);
    }

    public static FoldersSettings settings() {
        return data().settings();
    }

    public static DragManager dragManager() {
        return DRAG_MANAGER;
    }

    /**
     * Runs an action that must never take a vanilla screen down with it (§74).
     *
     * @return true if the action completed
     */
    public static boolean guarded(String what, Runnable action) {
        try {
            action.run();
            return true;
        } catch (Throwable error) {
            LOGGER.error("Folders failed while {}; the vanilla screen was left alone", what, error);
            return false;
        }
    }
}
