package dev.miklires.folders.core.view;

import dev.miklires.folders.core.animation.Animation;
import dev.miklires.folders.core.animation.Easing;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.RootEntry;
import dev.miklires.folders.core.storage.FoldersSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns the model into a flat list of rows with the accordion animation applied
 * (§26–§29).
 *
 * <p>The whole animation is one number per folder: how far open it is. Row
 * positions fall out of that, so the list grows and shrinks smoothly without any
 * entry needing its own timer (§66).
 */
public final class FolderViewModel {

    /** §67 asks for 150–250 ms. */
    public static final long EXPAND_DURATION_MS = 200L;

    private final FolderRepository repository;
    private final FoldersSettings settings;

    private final Map<UUID, Animation> expansion = new HashMap<>();

    private int rowHeight = 36;
    private int childIndent = 12;
    private long lastUpdateMs = Long.MIN_VALUE;

    private List<DisplayRow> rows = List.of();
    private int totalHeight;

    public FolderViewModel(FolderRepository repository, FoldersSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public FolderRepository repository() {
        return repository;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = Math.max(1, rowHeight);
    }

    public int rowHeight() {
        return rowHeight;
    }

    public void setChildIndent(int childIndent) {
        this.childIndent = Math.max(0, childIndent);
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    /**
     * Advances every running animation using real elapsed time (§67).
     *
     * @param nowMs a monotonic clock in milliseconds
     * @return true if anything is still moving, i.e. the layout must be rebuilt
     */
    public boolean tick(long nowMs) {
        long delta = lastUpdateMs == Long.MIN_VALUE ? 0L : Math.max(0L, nowMs - lastUpdateMs);
        lastUpdateMs = nowMs;

        boolean running = false;
        for (Animation animation : expansion.values()) {
            animation.update(delta);
            running |= animation.isRunning();
        }
        return running;
    }

    public boolean isAnimating() {
        for (Animation animation : expansion.values()) {
            if (animation.isRunning()) {
                return true;
            }
        }
        return false;
    }

    /** Opens or closes a folder, persisting the new state (§59). */
    public void toggle(UUID folderId) {
        repository.folder(folderId).ifPresent(folder -> setExpanded(folderId, !folder.expanded()));
    }

    public void setExpanded(UUID folderId, boolean expanded) {
        // Seed the animation before the model changes: animationFor() falls back
        // to the folder's stored state, and reading it after the write would
        // start the animation already at its destination.
        Animation animation = animationFor(folderId);
        repository.setExpanded(folderId, expanded);
        animation.animateTo(expanded ? 1.0f : 0.0f, settings.scaledDuration(EXPAND_DURATION_MS));
    }

    /** How far open a folder is, 0 closed to 1 open. */
    public float expansionOf(UUID folderId) {
        Animation animation = expansion.get(folderId);
        return animation == null ? 0.0f : animation.value();
    }

    private Animation animationFor(UUID folderId) {
        return expansion.computeIfAbsent(folderId, id -> {
            boolean expanded = repository.folder(id).map(Folder::expanded).orElse(false);
            return new Animation(expanded ? 1.0f : 0.0f, Easing.EASE_OUT_CUBIC);
        });
    }

    /** Forgets animation state for folders that no longer exist. */
    private void pruneAnimations(Set<UUID> alive) {
        for (Iterator<Map.Entry<UUID, Animation>> it = expansion.entrySet().iterator(); it.hasNext(); ) {
            if (!alive.contains(it.next().getKey())) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /**
     * Recomputes the row list.
     *
     * @param availableItems ids Minecraft currently has. Anything a folder still
     *                       references but Minecraft has dropped is left out
     *                       rather than drawn as a broken row (§58).
     */
    public List<DisplayRow> layout(Set<String> availableItems) {
        List<DisplayRow> out = new ArrayList<>();
        Set<UUID> aliveFolders = new HashSet<>();
        int y = 0;

        for (RootEntry entry : repository.rootEntries()) {
            if (entry instanceof RootEntry.ItemRef item) {
                if (!availableItems.contains(item.itemId())) {
                    continue;
                }
                out.add(new DisplayRow.ItemRow(item.itemId(), null, y, rowHeight, 0, 1.0f, y, y + rowHeight));
                y += rowHeight;
                continue;
            }

            Folder folder = ((RootEntry.FolderRef) entry).folder();
            aliveFolders.add(folder.id());

            float progress = animationFor(folder.id()).value();
            out.add(new DisplayRow.FolderRow(folder, y, rowHeight, progress));
            y += rowHeight;

            List<String> visibleChildren = new ArrayList<>();
            for (String childId : folder.items()) {
                if (availableItems.contains(childId)) {
                    visibleChildren.add(childId);
                }
            }
            if (visibleChildren.isEmpty() || progress <= 0.0f) {
                continue;
            }

            int fullBand = visibleChildren.size() * rowHeight;
            int band = Math.round(progress * fullBand);
            if (band <= 0) {
                continue;
            }

            int bandTop = y;
            int bandBottom = y + band;
            for (int i = 0; i < visibleChildren.size(); i++) {
                int childY = bandTop + i * rowHeight;
                if (childY >= bandBottom) {
                    break;
                }
                // Fade a row in over the last row-height of its reveal, so the
                // bottom of the band is never a hard edge.
                float visible = Math.min(rowHeight, bandBottom - childY) / (float) rowHeight;
                out.add(new DisplayRow.ItemRow(
                        visibleChildren.get(i),
                        folder.id(),
                        childY,
                        rowHeight,
                        childIndent,
                        Math.clamp(visible, 0.0f, 1.0f),
                        bandTop,
                        bandBottom));
            }
            y = bandBottom;
        }

        pruneAnimations(aliveFolders);
        this.rows = List.copyOf(out);
        this.totalHeight = y;
        return this.rows;
    }

    public List<DisplayRow> rows() {
        return rows;
    }

    public int totalHeight() {
        return totalHeight;
    }

    /**
     * Content height the list will settle at once every animation finishes.
     * Used to decide how far to scroll so a folder opening near the bottom edge
     * brings its contents into view (§28).
     */
    public int settledHeight(Set<String> availableItems) {
        int height = 0;
        for (RootEntry entry : repository.rootEntries()) {
            if (entry instanceof RootEntry.ItemRef item) {
                if (availableItems.contains(item.itemId())) {
                    height += rowHeight;
                }
                continue;
            }
            Folder folder = ((RootEntry.FolderRef) entry).folder();
            height += rowHeight;
            if (!folder.expanded()) {
                continue;
            }
            for (String childId : folder.items()) {
                if (availableItems.contains(childId)) {
                    height += rowHeight;
                }
            }
        }
        return height;
    }
}
