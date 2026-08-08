package dev.miklires.folders.core.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderConfig;
import dev.miklires.folders.core.data.FolderIcon;
import dev.miklires.folders.core.data.FolderType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Turns a {@link FolderConfig} into JSON and back.
 *
 * <p>Reading is deliberately tolerant: every field is probed for the type it
 * should have and anything unusable is dropped, so a hand-edited file with one
 * bad entry loses that entry instead of the whole configuration. A structurally
 * broken file (not an object, unparseable) is the caller's problem — see
 * {@link JsonStorage}, which backs it up and starts clean.
 */
public final class FolderCodec {

    /** Reported by {@link #read} so the caller can decide to rewrite the file. */
    public record Result(FolderConfig config, boolean repaired) {
    }

    private FolderCodec() {
    }

    public static Result read(JsonObject json, FolderType type) throws UnknownVersionException {
        boolean repaired = false;

        int version = FolderConfig.CURRENT_VERSION;
        JsonElement versionElement = json.get("version");
        if (versionElement != null && versionElement.isJsonPrimitive() && versionElement.getAsJsonPrimitive().isNumber()) {
            version = versionElement.getAsInt();
        } else {
            repaired = true;
        }
        if (version > FolderConfig.CURRENT_VERSION) {
            // Written by a newer Folders. Refuse rather than silently mangling it.
            throw new UnknownVersionException(version);
        }
        // version < CURRENT_VERSION would be migrated here.

        List<Folder> folders = new ArrayList<>();
        Set<UUID> seenIds = new HashSet<>();
        JsonElement foldersElement = json.get("folders");
        if (foldersElement != null && foldersElement.isJsonArray()) {
            for (JsonElement element : foldersElement.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    repaired = true;
                    continue;
                }
                Folder folder = readFolder(element.getAsJsonObject(), type);
                if (folder == null || !seenIds.add(folder.id())) {
                    repaired = true;
                    continue;
                }
                folders.add(folder);
            }
        } else if (foldersElement != null) {
            repaired = true;
        }

        List<String> root = new ArrayList<>();
        JsonElement rootElement = json.get("root");
        if (rootElement != null && rootElement.isJsonArray()) {
            for (JsonElement element : rootElement.getAsJsonArray()) {
                String token = asString(element);
                if (token == null) {
                    repaired = true;
                    continue;
                }
                root.add(token);
            }
        } else if (rootElement != null) {
            repaired = true;
        }

        return new Result(new FolderConfig(FolderConfig.CURRENT_VERSION, folders, root), repaired);
    }

    private static Folder readFolder(JsonObject json, FolderType type) {
        UUID id = asUuid(json.get("id"));
        if (id == null) {
            return null;
        }
        String name = Folder.sanitizeName(asString(json.get("name")));
        if (name == null) {
            name = "Folder";
        }
        boolean expanded = asBoolean(json.get("expanded"), false);
        FolderIcon icon = readIcon(json.get("icon"));

        List<String> items = new ArrayList<>();
        JsonElement itemsElement = json.get("items");
        if (itemsElement != null && itemsElement.isJsonArray()) {
            for (JsonElement element : itemsElement.getAsJsonArray()) {
                String itemId = asString(element);
                if (itemId != null && type.ownsId(itemId) && !items.contains(itemId)) {
                    items.add(itemId);
                }
            }
        }
        return Folder.restore(id, name, icon, expanded, items);
    }

    private static FolderIcon readIcon(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return FolderIcon.DEFAULT;
        }
        JsonObject json = element.getAsJsonObject();
        FolderIcon.Kind kind = FolderIcon.Kind.fromSerialized(asString(json.get("type")));
        if (kind != FolderIcon.Kind.CUSTOM) {
            return FolderIcon.DEFAULT;
        }
        // FolderIcon.custom falls back to DEFAULT for anything path-shaped.
        return FolderIcon.custom(asString(json.get("file")));
    }

    public static JsonObject write(FolderConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("version", FolderConfig.CURRENT_VERSION);

        JsonArray folders = new JsonArray();
        for (Folder folder : config.folders()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", folder.id().toString());
            entry.addProperty("name", folder.name());

            JsonObject icon = new JsonObject();
            icon.addProperty("type", folder.icon().kind().serialized());
            folder.icon().file().ifPresent(file -> icon.addProperty("file", file));
            entry.add("icon", icon);

            entry.addProperty("expanded", folder.expanded());

            JsonArray items = new JsonArray();
            folder.items().forEach(items::add);
            entry.add("items", items);

            folders.add(entry);
        }
        json.add("folders", folders);

        JsonArray root = new JsonArray();
        config.root().forEach(root::add);
        json.add("root", root);

        return json;
    }

    private static String asString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        return primitive.isString() ? primitive.getAsString() : null;
    }

    private static UUID asUuid(JsonElement element) {
        String raw = asString(element);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean asBoolean(JsonElement element, boolean fallback) {
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        return primitive.isBoolean() ? primitive.getAsBoolean() : fallback;
    }

    /** Thrown for a file written by a future format version. */
    public static final class UnknownVersionException extends Exception {
        private static final long serialVersionUID = 1L;

        private final int version;

        public UnknownVersionException(int version) {
            super("Unsupported folders config version " + version);
            this.version = version;
        }

        public int version() {
            return version;
        }
    }
}
