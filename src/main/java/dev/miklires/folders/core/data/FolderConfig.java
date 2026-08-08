package dev.miklires.folders.core.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The whole persisted state for one {@link FolderType}.
 *
 * <p>{@code root} is the ordering of the top level: a flat list of tokens where
 * a folder is {@code folder:<uuid>} and anything else is an item id. Keeping the
 * order here is what stops a list from silently re-sorting itself into
 * "folders first, items after".
 */
public final class FolderConfig {
    /** Bump only alongside a migration in {@code FolderCodec}. */
    public static final int CURRENT_VERSION = 1;

    public static final String FOLDER_TOKEN_PREFIX = "folder:";

    private int version;
    private final List<Folder> folders;
    private final List<String> root;

    public FolderConfig(int version, List<Folder> folders, List<String> root) {
        this.version = version;
        this.folders = new ArrayList<>(folders);
        this.root = new ArrayList<>(root);
    }

    public static FolderConfig empty() {
        return new FolderConfig(CURRENT_VERSION, List.of(), List.of());
    }

    public static String folderToken(UUID id) {
        return FOLDER_TOKEN_PREFIX + id;
    }

    /** @return the folder id in this token, or {@code null} if it is an item token. */
    public static UUID parseFolderToken(String token) {
        if (token == null || !token.startsWith(FOLDER_TOKEN_PREFIX)) {
            return null;
        }
        try {
            return UUID.fromString(token.substring(FOLDER_TOKEN_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int version() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /** Live list — the repository owns mutation. */
    public List<Folder> folders() {
        return folders;
    }

    /** Live list — the repository owns mutation. */
    public List<String> root() {
        return root;
    }
}
