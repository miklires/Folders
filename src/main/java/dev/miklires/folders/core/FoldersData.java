package dev.miklires.folders.core;

import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.core.storage.FoldersSettings;
import dev.miklires.folders.core.storage.JsonStorage;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Everything persistent the mod owns, in one place: the three repositories, the
 * settings, and the storage that backs them.
 *
 * <p>Saving is driven by the dirty flag, so calling {@link #saveIfDirty()} after
 * an operation or on screen close is cheap and calling it in a loop is harmless
 * (§51). Nothing here is called during rendering.
 */
public final class FoldersData implements AutoCloseable {

    private final JsonStorage storage;
    private final FoldersSettings settings;
    private final Path settingsFile;
    private final Map<FolderType, FolderRepository> repositories = new EnumMap<>(FolderType.class);

    private FoldersData(JsonStorage storage, FoldersSettings settings, Path settingsFile) {
        this.storage = storage;
        this.settings = settings;
        this.settingsFile = settingsFile;
    }

    /** @param configDirectory usually {@code .minecraft/config} */
    public static FoldersData load(Path configDirectory) {
        Path root = configDirectory.resolve("folders");
        JsonStorage storage = new JsonStorage(root);
        Path settingsFile = root.resolve("settings.json");
        FoldersData data = new FoldersData(storage, FoldersSettings.load(settingsFile), settingsFile);
        for (FolderType type : FolderType.values()) {
            data.repositories.put(type, new FolderRepository(type, storage.load(type)));
        }
        return data;
    }

    public FolderRepository repository(FolderType type) {
        return repositories.get(type);
    }

    public FoldersSettings settings() {
        return settings;
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

    public void saveSettings() {
        settings.save(settingsFile);
    }

    @Override
    public void close() {
        saveIfDirty();
        storage.close();
    }
}
