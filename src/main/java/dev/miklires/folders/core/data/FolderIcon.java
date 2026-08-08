package dev.miklires.folders.core.data;

import java.util.Objects;
import java.util.Optional;

/**
 * A folder's icon: either the built-in texture or a user supplied PNG that has
 * been copied into {@code config/folders/icons/}.
 *
 * <p>The file name is always derived from the folder UUID by the icon manager,
 * never from the folder name — This class only carries the name and
 * refuses anything that looks like a path.
 */
public final class FolderIcon {
    public static final FolderIcon DEFAULT = new FolderIcon(Kind.DEFAULT, null);

    public enum Kind {
        DEFAULT("default"),
        CUSTOM("custom");

        private final String serialized;

        Kind(String serialized) {
            this.serialized = serialized;
        }

        public String serialized() {
            return serialized;
        }

        public static Kind fromSerialized(String value) {
            for (Kind kind : values()) {
                if (kind.serialized.equalsIgnoreCase(value)) {
                    return kind;
                }
            }
            return DEFAULT;
        }
    }

    private final Kind kind;
    private final String file;

    private FolderIcon(Kind kind, String file) {
        this.kind = kind;
        this.file = file;
    }

    /**
     * @return a custom icon, or {@link #DEFAULT} if the file name is unusable
     *         (empty, or containing separators / traversal segments).
     */
    public static FolderIcon custom(String file) {
        if (!isSafeFileName(file)) {
            return DEFAULT;
        }
        return new FolderIcon(Kind.CUSTOM, file);
    }

    /**
     * A usable icon file name is a bare name: no directory separators, no
     * traversal, no NUL, and it must be a PNG.
     */
    public static boolean isSafeFileName(String file) {
        if (file == null || file.isBlank()) {
            return false;
        }
        if (file.indexOf('/') >= 0 || file.indexOf('\\') >= 0 || file.indexOf('\0') >= 0) {
            return false;
        }
        if (file.equals(".") || file.equals("..") || file.contains("..")) {
            return false;
        }
        return file.toLowerCase(java.util.Locale.ROOT).endsWith(".png");
    }

    public Kind kind() {
        return kind;
    }

    public boolean isCustom() {
        return kind == Kind.CUSTOM && file != null;
    }

    public Optional<String> file() {
        return Optional.ofNullable(file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof FolderIcon other && kind == other.kind && Objects.equals(file, other.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, file);
    }

    @Override
    public String toString() {
        return isCustom() ? "FolderIcon[custom " + file + "]" : "FolderIcon[default]";
    }
}
