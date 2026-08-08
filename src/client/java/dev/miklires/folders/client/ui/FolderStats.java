package dev.miklires.folders.client.ui;

import net.minecraft.network.chat.Component;

/**
 * The small grey line under a folder name.
 *
 * @param total     items in the folder
 * @param highlight servers online / packs enabled, or {@code -1} when the type has
 *                  no second number
 * @param busy      a ping sweep is running, so show "Refreshing…" instead
 */
public record FolderStats(int total, int highlight, boolean busy) {

    public static FolderStats of(int total) {
        return new FolderStats(total, -1, false);
    }

    public static FolderStats of(int total, int highlight) {
        return new FolderStats(total, highlight, false);
    }

    public FolderStats refreshing() {
        return new FolderStats(total, highlight, true);
    }

    public boolean isEmpty() {
        return total <= 0;
    }

    /**
     * @param countKey        e.g. {@code folders.count.worlds}
     * @param highlightKey    e.g. {@code folders.count.online}, or {@code null}
     * @param dropHint        true while a compatible payload hovers this folder
     */
    public Component describe(String countKey, String highlightKey, boolean dropHint) {
        if (dropHint) {
            return Component.translatable("folders.drop_here");
        }
        if (busy) {
            return Component.translatable("folders.refreshing");
        }
        if (isEmpty()) {
            return Component.translatable("folders.empty");
        }
        Component counts = Component.translatable(countKey, total);
        if (highlightKey == null || highlight < 0) {
            return counts;
        }
        return Component.translatable("folders.stats.join", counts, Component.translatable(highlightKey, highlight));
    }
}
