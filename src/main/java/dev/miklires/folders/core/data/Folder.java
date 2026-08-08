package dev.miklires.folders.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One folder. Identity is the {@link #id()} and only the id — renaming a folder
 * never touches its contents (§5).
 *
 * <p>Mutation goes through {@link FolderRepository} so the dirty flag and the
 * "an item lives in at most one folder" invariant stay honest; the setters here
 * are package-private for that reason.
 */
public final class Folder {
    /** Generous but bounded, per §12. */
    public static final int MAX_NAME_LENGTH = 48;

    private final UUID id;
    private String name;
    private FolderIcon icon;
    private boolean expanded;
    private final List<String> items;

    Folder(UUID id, String name, FolderIcon icon, boolean expanded, List<String> items) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = name;
        this.icon = icon == null ? FolderIcon.DEFAULT : icon;
        this.expanded = expanded;
        this.items = new ArrayList<>(items);
    }

    public static Folder create(UUID id, String name) {
        return new Folder(id, sanitizeName(name), FolderIcon.DEFAULT, false, List.of());
    }

    /**
     * Rebuilds a folder read from disk. Only the storage layer should need this —
     * everything else goes through {@link FolderRepository}.
     */
    public static Folder restore(UUID id, String name, FolderIcon icon, boolean expanded, List<String> items) {
        return new Folder(id, name, icon, expanded, items);
    }

    /**
     * Trims, collapses control characters and clamps to {@link #MAX_NAME_LENGTH}.
     * Returns {@code null} for a name that cannot be salvaged — callers decide
     * whether that means "reject the rename" or "fall back to a default".
     */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            cleaned.append(Character.isISOControl(c) ? ' ' : c);
        }
        String trimmed = cleaned.toString().trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public FolderIcon icon() {
        return icon;
    }

    public boolean expanded() {
        return expanded;
    }

    /** Unmodifiable view; mutate through the repository. */
    public List<String> items() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean contains(String itemId) {
        return items.contains(itemId);
    }

    void setName(String name) {
        this.name = name;
    }

    void setIcon(FolderIcon icon) {
        this.icon = icon == null ? FolderIcon.DEFAULT : icon;
    }

    void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    List<String> mutableItems() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Folder other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Folder[" + id + " \"" + name + "\" items=" + items.size() + "]";
    }
}
