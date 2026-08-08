package dev.miklires.folders.core;

import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.core.storage.JsonStorage;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * The three folder repositories and the storage that backs them.
 *
 * <p>Saving is driven by the dirty flag, so calling {@link #saveIfDirty()} after an operation or on
 * screen close is cheap and calling it in a loop is harmless. Nothing here runs while the game is
 * drawing: the write itself happens on the storage thread.
 */
public final class FoldersData implements AutoCloseable {

    private final JsonStorage storage;
    private final Map<FolderType, FolderRepository> repositories = new EnumMap<>(FolderType.class);

    private FoldersData(JsonStorage storage) {
        this.storage = storage;
    }

    /** @param configDirectory usually {@code .minecraft/config} */
    public static FoldersData load(Path configDirectory) {
        Path root = configDirectory.resolve("folders");
        JsonStorage storage = new JsonStorage(root);
        FoldersData data = new FoldersData(storage);
        for (FolderType type : FolderType.values()) {
            data.repositories.put(type, new FolderRepository(type, storage.load(type)));
        }
        return data;
    }

    public FolderRepository repository(FolderType type) {
        return repositories.get(type);
    }

    public JsonStorage storage() {
        return storage;
    }

    /** Writes any repository with pending changes. */
    public void saveIfDirty() {
        for (FolderType type : FolderType.values()) {
            saveIfDirty(type);
        }
    }

    public void saveIfDirty(FolderType type) {
        FolderRepository repository = repositories.get(type);
        if (repository == null || !repository.isDirty()) {
            return;
        }
        // Clear first: the config is snapshotted synchronously inside saveAsync,
        // so a change arriving after this point correctly marks it dirty again.
        repository.markClean();
        storage.saveAsync(type, repository.config());
    }

    @Override
    public void close() {
        saveIfDirty();
        storage.close();
    }
}
