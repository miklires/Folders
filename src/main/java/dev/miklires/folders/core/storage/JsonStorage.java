package dev.miklires.folders.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.miklires.folders.core.data.FolderConfig;
import dev.miklires.folders.core.data.FolderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Reads and writes {@code config/folders/*.json}.
 *
 * <p>Two rules drive the design:
 * <ul>
 *   <li><b>A broken file never stops the game (§8).</b> Anything unreadable is
 *       moved aside as a {@code .bak}, logged, and replaced by an empty config.</li>
 *   <li><b>No file IO on the render thread (§50, §77.11).</b> Saves serialise the
 *       model synchronously — cheap, and it snapshots the state before the player
 *       can change it again — then write on a single background thread. One chain
 *       per type means two saves can never interleave or land out of order.</li>
 * </ul>
 */
public final class JsonStorage implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("folders/storage");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path directory;
    private final ExecutorService io;
    private final Map<FolderType, CompletableFuture<Void>> writeChains = new EnumMap<>(FolderType.class);

    public JsonStorage(Path directory) {
        this.directory = directory;
        this.io = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "folders-io");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Path directory() {
        return directory;
    }

    public Path iconDirectory() {
        return directory.resolve("icons");
    }

    public Path fileFor(FolderType type) {
        return directory.resolve(type.fileName());
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /** Never throws: worst case you get an empty config and a line in the log. */
    public FolderConfig load(FolderType type) {
        Path file = fileFor(type);
        if (!Files.isRegularFile(file)) {
            return FolderConfig.empty();
        }

        String raw;
        try {
            raw = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not read {}, starting with an empty configuration", file, e);
            return FolderConfig.empty();
        }

        if (raw.isBlank()) {
            LOGGER.warn("{} is empty, starting with an empty configuration", file);
            return FolderConfig.empty();
        }

        JsonObject json;
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("root element is " + parsed.getClass().getSimpleName() + ", expected an object");
            }
            json = parsed.getAsJsonObject();
        } catch (Exception e) {
            quarantine(file, "malformed JSON: " + e.getMessage());
            return FolderConfig.empty();
        }

        try {
            FolderCodec.Result result = FolderCodec.read(json, type);
            if (result.repaired()) {
                LOGGER.warn("{} contained entries Folders could not use; they were dropped", file);
            }
            return result.config();
        } catch (FolderCodec.UnknownVersionException e) {
            quarantine(file, "config version " + e.version() + " is newer than this build understands");
            return FolderConfig.empty();
        } catch (Exception e) {
            LOGGER.error("Unexpected failure reading {}", file, e);
            quarantine(file, "unexpected failure: " + e);
            return FolderConfig.empty();
        }
    }

    /**
     * Moves an unusable file aside so the player can recover it by hand. An older
     * backup is never overwritten — the second one gets a timestamp.
     */
    private void quarantine(Path file, String reason) {
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        try {
            if (Files.exists(backup)) {
                backup = file.resolveSibling(file.getFileName() + "." + Instant.now().toEpochMilli() + ".bak");
            }
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.error("{} could not be loaded ({}). It was saved as {} and Folders started empty.",
                    file, reason, backup.getFileName());
        } catch (IOException e) {
            LOGGER.error("{} could not be loaded ({}) and could not be backed up either", file, reason, e);
        }
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    /**
     * Snapshots the config now and writes it in the background.
     *
     * @return a future completing once this particular write has landed; callers
     *         normally ignore it, {@link #flush()} is what waits.
     */
    public CompletableFuture<Void> saveAsync(FolderType type, FolderConfig config) {
        String payload;
        try {
            payload = GSON.toJson(FolderCodec.write(config));
        } catch (Exception e) {
            LOGGER.error("Could not serialise the {} folder configuration; not saving", type.configName(), e);
            return CompletableFuture.completedFuture(null);
        }

        Path target = fileFor(type);
        synchronized (writeChains) {
            CompletableFuture<Void> previous = writeChains.getOrDefault(type, CompletableFuture.completedFuture(null));
            CompletableFuture<Void> next = previous
                    .handle((ignored, error) -> null)
                    .thenRunAsync(() -> writeAtomically(target, payload), io)
                    .exceptionally(error -> {
                        LOGGER.error("Could not save {}", target, error);
                        return null;
                    });
            writeChains.put(type, next);
            return next;
        }
    }

    private void writeAtomically(Path target, String payload) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, payload, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("write failed", e);
        }
    }

    /** Waits for queued writes. Called when the game is shutting down. */
    public void flush() {
        CompletableFuture<?>[] pending;
        synchronized (writeChains) {
            pending = writeChains.values().toArray(new CompletableFuture<?>[0]);
        }
        try {
            CompletableFuture.allOf(pending).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.warn("Timed out waiting for folder configuration writes", e);
        }
    }

    @Override
    public void close() {
        flush();
        io.shutdown();
    }
}
