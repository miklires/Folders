package dev.miklires.folders.core.profile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * A named set of enabled resource packs, in the order they are applied.
 *
 * <p>Orthogonal to folders. A folder says where a pack is filed; a profile says which packs are
 * switched on at once. The two share nothing but the pack ids, which is why this lives beside the
 * folder model rather than inside it.
 *
 * <p>Only ids are stored, never a copy of the pack. A profile naming a pack that has since been
 * deleted simply skips it when applied — no repair pass, and nothing on disk to go stale.
 *
 * @param packIds pack profile ids ({@code file/shaders.zip}), in application order
 */
public record PackProfile(UUID id, String name, List<String> packIds) {

    public static final int MAX_NAME_LENGTH = 48;

    public PackProfile {
        if (id == null) {
            throw new IllegalArgumentException("A profile needs an id");
        }
        name = sanitizeName(name);
        // Order matters and duplicates do not: a pack listed twice would be enabled twice, which
        // vanilla treats as an error rather than a no-op.
        packIds = List.copyOf(new LinkedHashSet<>(nonNull(packIds)));
    }

    public static PackProfile of(String name, List<String> packIds) {
        return new PackProfile(UUID.randomUUID(), name, packIds);
    }

    public PackProfile withName(String newName) {
        return new PackProfile(id, newName, packIds);
    }

    public PackProfile withPacks(List<String> newPacks) {
        return new PackProfile(id, name, newPacks);
    }

    public boolean isEmpty() {
        return packIds.isEmpty();
    }

    /**
     * Trims a user-supplied name to something that can be drawn and stored.
     *
     * <p>Control characters go because a newline in a list row is a broken row; the length cap is
     * the same one folder names use, so the two read as one feature.
     */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder clean = new StringBuilder(Math.min(raw.length(), MAX_NAME_LENGTH));
        raw.codePoints()
                .filter(codePoint -> codePoint >= ' ' && codePoint != 127)
                .forEach(codePoint -> {
                    if (clean.length() < MAX_NAME_LENGTH) {
                        clean.appendCodePoint(codePoint);
                    }
                });
        return clean.toString().trim();
    }

    private static List<String> nonNull(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                out.add(id);
            }
        }
        return out;
    }
}
