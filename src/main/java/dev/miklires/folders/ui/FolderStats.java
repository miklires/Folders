package dev.miklires.folders.ui;

import net.minecraft.text.Text;

/**
 * The small grey line under a folder name (§30).
 *
 * @param total     items in the folder
 * @param highlight servers online / packs enabled, or {@code -1} when the type has
 *                  no second number
 * @param busy      a ping sweep is running, so show "Refreshing…" instead (§31)
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
     * @param dropHint        true while a compatible payload hovers this folder (§39)
     */
    public Text describe(String countKey, String highlightKey, boolean dropHint) {
        if (dropHint) {
            return Text.translatable("folders.drop_here");
        }
        if (busy) {
            return Text.translatable("folders.refreshing");
        }
        if (isEmpty()) {
            return Text.translatable("folders.empty");
        }
        Text counts = Text.translatable(countKey, total);
        if (highlightKey == null || highlight < 0) {
            return counts;
        }
        return Text.translatable("folders.stats.join", counts, Text.translatable(highlightKey, highlight));
    }
}
