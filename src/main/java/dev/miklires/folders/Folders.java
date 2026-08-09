package dev.miklires.folders;

import dev.miklires.folders.core.FoldersData;
import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.core.drag.DragManager;
import dev.miklires.folders.core.profile.PackProfileStore;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod-wide handles: the loaded folder data, the drag state, and the log.
 *
 * <p>A lookup point, not a place for logic. Everything that decides anything lives in the
 * controllers, and everything that persists anything lives in {@code core.storage}.
 */
public final class Folders {

    public static final String MOD_ID = "folders";

    public static final Logger LOGGER = LoggerFactory.getLogger("Folders");

    /**
     * One drag manager for the whole game.
     *
     * <p>Three screens sharing one state machine is what stops each of them growing its own subtly
     * different idea of when a press becomes a drag.
     */
    private static final DragManager DRAG_MANAGER = new DragManager();

    private static FoldersData data;
    private static PackProfileStore profiles;

    private Folders() {
    }

    public static void initialise() {
        data = FoldersData.load(FabricLoader.getInstance().getConfigDir());
    }

    /**
     * Loads on first use if the initialiser has not run yet, so a screen opened unusually early
     * still works instead of throwing.
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

    /**
     * The resource pack profiles, loaded on first use.
     *
     * <p>Kept apart from {@link FoldersData} because it answers a different question. Folders say
     * where a pack is filed and are read on three screens; profiles say which packs are switched on
     * together and are read on one. Sharing a file would mean a corrupt profile costing someone
     * their folders.
     */
    public static PackProfileStore profiles() {
        if (profiles == null) {
            profiles = PackProfileStore.load(data().storage().directory());
        }
        return profiles;
    }

    public static DragManager dragManager() {
        return DRAG_MANAGER;
    }

    /**
     * Runs something that must never take a vanilla screen down with it.
     *
     * <p>A broken icon, a malformed config or a mapping that moved should cost the folder row, not
     * the multiplayer menu. Every entry point from a mixin goes through here.
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
