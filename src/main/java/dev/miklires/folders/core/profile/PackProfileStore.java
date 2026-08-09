package dev.miklires.folders.core.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads, holds and saves the resource pack profiles.
 *
 * <p>Same rules as the folder configuration, for the same reason: a profile file that has been
 * hand-edited into nonsense must cost the player their profiles and nothing else. The file is moved
 * aside rather than deleted, an unreadable entry is dropped rather than aborting the load, and a
 * file written by a newer build is quarantined rather than mangled.
 *
 * <p>Writes are synchronous, unlike the folder configuration. A profile changes only when the
 * player creates, renames, deletes or overwrites one — never during rendering — so there is nothing
 * to keep off the frame thread, and a synchronous write is one fewer thread to reason about.
 */
public final class PackProfileStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("folders/profiles");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final int VERSION = 1;
    public static final String FILE_NAME = "pack_profiles.json";

    private final Path file;
    private final List<PackProfile> profiles = new ArrayList<>();
    private boolean dirty;

    private PackProfileStore(Path file, List<PackProfile> loaded) {
        this.file = file;
        this.profiles.addAll(loaded);
    }

    /** @param directory the {@code config/folders} directory */
    public static PackProfileStore load(Path directory) {
        Path file = directory.resolve(FILE_NAME);
        return new PackProfileStore(file, read(file));
    }

    public List<PackProfile> profiles() {
        return List.copyOf(profiles);
    }

    public Optional<PackProfile> profile(UUID id) {
        return profiles.stream().filter(profile -> profile.id().equals(id)).findFirst();
    }

    public boolean isEmpty() {
        return profiles.isEmpty();
    }

    public PackProfile add(String name, List<String> packIds) {
        PackProfile profile = PackProfile.of(uniqueName(name, null), packIds);
        profiles.add(profile);
        dirty = true;
        return profile;
    }

    public boolean rename(UUID id, String name) {
        String clean = PackProfile.sanitizeName(name);
        if (clean.isEmpty()) {
            return false;
        }
        return replace(id, profile -> profile.withName(uniqueName(clean, id)));
    }

    /** Points an existing profile at a new set of packs — "overwrite with what is on now". */
    public boolean update(UUID id, List<String> packIds) {
        return replace(id, profile -> profile.withPacks(packIds));
    }

    public boolean remove(UUID id) {
        boolean removed = profiles.removeIf(profile -> profile.id().equals(id));
        dirty |= removed;
        return removed;
    }

    private boolean replace(UUID id, java.util.function.UnaryOperator<PackProfile> change) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id().equals(id)) {
                profiles.set(i, change.apply(profiles.get(i)));
                dirty = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Appends a counter to a name another profile already uses.
     *
     * <p>Profiles are picked from a menu by name, so two identically named ones would be
     * indistinguishable at the only moment that matters.
     */
    private String uniqueName(String raw, UUID ignoring) {
        String base = PackProfile.sanitizeName(raw);
        if (base.isEmpty()) {
            base = "Profile";
        }
        String candidate = base;
        int suffix = 2;
        while (taken(candidate, ignoring)) {
            candidate = base + " " + suffix++;
        }
        return candidate;
    }

    private boolean taken(String name, UUID ignoring) {
        for (PackProfile profile : profiles) {
            if (!profile.id().equals(ignoring) && profile.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public boolean isDirty() {
        return dirty;
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        dirty = false;
        write();
    }

    private void write() {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonArray array = new JsonArray();
        for (PackProfile profile : profiles) {
            JsonObject json = new JsonObject();
            json.addProperty("id", profile.id().toString());
            json.addProperty("name", profile.name());
            JsonArray packs = new JsonArray();
            profile.packIds().forEach(packs::add);
            json.add("packs", packs);
            array.add(json);
        }
        root.add("profiles", array);

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save {}", file, e);
            // Left dirty-free on purpose: retrying every action would just repeat the same failure.
        }
    }

    private static List<PackProfile> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }

        String raw;
        try {
            raw = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not read {}, starting with no profiles", file, e);
            return List.of();
        }
        if (raw.isBlank()) {
            return List.of();
        }

        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("root element is not an object");
            }
            root = parsed.getAsJsonObject();
        } catch (Exception e) {
            quarantine(file, "malformed JSON: " + e.getMessage());
            return List.of();
        }

        int version = root.has("version") && root.get("version").isJsonPrimitive()
                ? root.get("version").getAsInt()
                : VERSION;
        if (version > VERSION) {
            quarantine(file, "profile version " + version + " is newer than this build understands");
            return List.of();
        }

        List<PackProfile> out = new ArrayList<>();
        JsonElement array = root.get("profiles");
        if (array == null || !array.isJsonArray()) {
            return out;
        }
        for (JsonElement element : array.getAsJsonArray()) {
            readOne(element).ifPresent(out::add);
        }
        return out;
    }

    /** One unreadable profile costs that profile, never the file. */
    private static Optional<PackProfile> readOne(JsonElement element) {
        try {
            if (!element.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject json = element.getAsJsonObject();
            UUID id = UUID.fromString(json.get("id").getAsString());
            String name = json.has("name") ? json.get("name").getAsString() : "";
            List<String> packs = new ArrayList<>();
            JsonElement packArray = json.get("packs");
            if (packArray != null && packArray.isJsonArray()) {
                for (JsonElement pack : packArray.getAsJsonArray()) {
                    if (pack.isJsonPrimitive()) {
                        packs.add(pack.getAsString());
                    }
                }
            }
            return Optional.of(new PackProfile(id, name, packs));
        } catch (Exception e) {
            LOGGER.warn("Dropped an unreadable resource pack profile: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static void quarantine(Path file, String reason) {
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        try {
            if (Files.exists(backup)) {
                backup = file.resolveSibling(file.getFileName() + "." + Instant.now().toEpochMilli() + ".bak");
            }
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.error("{} could not be loaded ({}). It was saved as {} and Folders started with no profiles.",
                    file, reason, backup.getFileName());
        } catch (IOException e) {
            LOGGER.error("{} could not be loaded ({}) and could not be backed up either", file, reason, e);
        }
    }
}
