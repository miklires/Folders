package dev.miklires.folders.core.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory folder model for one {@link FolderType} and the single place where
 * it is mutated.
 *
 * <p>Invariants it maintains, all of them from §9:
 * <ul>
 *   <li>an item is either at the root or in exactly one folder, never both and
 *       never in two folders;</li>
 *   <li>deleting a folder returns its contents to the root, in place;</li>
 *   <li>only ids belonging to this type are ever stored.</li>
 * </ul>
 *
 * <p>No Minecraft types, no file access, no rendering — see §79. Persistence is
 * driven by {@link #isDirty()}; nothing here writes to disk (§50, §51).
 */
public final class FolderRepository {
    private final FolderType type;
    private FolderConfig config;

    /** itemId -> owning folder, kept in step with every mutation. */
    private final Map<String, Folder> itemIndex = new HashMap<>();
    private final Map<UUID, Folder> folderIndex = new HashMap<>();

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean dirty;

    public FolderRepository(FolderType type, FolderConfig config) {
        this.type = type;
        this.config = config == null ? FolderConfig.empty() : config;
        reindex();
    }

    public static FolderRepository empty(FolderType type) {
        return new FolderRepository(type, FolderConfig.empty());
    }

    public FolderType type() {
        return type;
    }

    /** Snapshot for the storage layer. */
    public FolderConfig config() {
        return config;
    }

    /** Replaces the whole model, e.g. after a reload from disk. */
    public void replace(FolderConfig newConfig) {
        this.config = newConfig == null ? FolderConfig.empty() : newConfig;
        reindex();
        notifyChanged();
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<Folder> folders() {
        return List.copyOf(config.folders());
    }

    public Optional<Folder> folder(UUID id) {
        return Optional.ofNullable(folderIndex.get(id));
    }

    /** @return the folder holding this item, if any. */
    public Optional<Folder> folderContaining(String itemId) {
        return Optional.ofNullable(itemIndex.get(itemId));
    }

    public boolean isInAnyFolder(String itemId) {
        return itemIndex.containsKey(itemId);
    }

    /**
     * The top level in display order. Tokens that no longer resolve are skipped
     * rather than reported — {@link #sync} is what removes them for good.
     */
    public List<RootEntry> rootEntries() {
        List<RootEntry> out = new ArrayList<>(config.root().size());
        for (String token : config.root()) {
            UUID folderId = FolderConfig.parseFolderToken(token);
            if (folderId != null) {
                Folder folder = folderIndex.get(folderId);
                if (folder != null) {
                    out.add(new RootEntry.FolderRef(folder));
                }
            } else if (!itemIndex.containsKey(token)) {
                out.add(new RootEntry.ItemRef(token));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    /**
     * Creates a folder at the top of the root list.
     *
     * @param baseName already-localised default name; a numeric suffix is added
     *                 when that name is taken ("New folder 2", §11).
     */
    public Folder createFolder(String baseName) {
        String base = Folder.sanitizeName(baseName);
        if (base == null) {
            base = "Folder";
        }
        Folder folder = Folder.create(UUID.randomUUID(), uniqueName(base));
        config.folders().add(folder);
        folderIndex.put(folder.id(), folder);
        config.root().add(0, FolderConfig.folderToken(folder.id()));
        markDirty();
        return folder;
    }

    private String uniqueName(String base) {
        Set<String> taken = new HashSet<>();
        for (Folder f : config.folders()) {
            taken.add(f.name());
        }
        if (!taken.contains(base)) {
            return base;
        }
        for (int n = 2; n < 10_000; n++) {
            String candidate = Folder.sanitizeName(base + " " + n);
            if (candidate != null && !taken.contains(candidate)) {
                return candidate;
            }
        }
        return base;
    }

    /** @return false if the name is empty/unusable, in which case nothing changes (§12). */
    public boolean renameFolder(UUID id, String newName) {
        Folder folder = folderIndex.get(id);
        if (folder == null) {
            return false;
        }
        String clean = Folder.sanitizeName(newName);
        if (clean == null) {
            return false;
        }
        if (clean.equals(folder.name())) {
            return true;
        }
        folder.setName(clean);
        markDirty();
        return true;
    }

    /**
     * Deletes a folder. Its items are spliced back into the root at exactly the
     * position the folder occupied, so nothing jumps to the bottom of the list
     * (§9, §13). No real world/server/pack is touched.
     */
    public boolean deleteFolder(UUID id) {
        Folder folder = folderIndex.remove(id);
        if (folder == null) {
            return false;
        }
        config.folders().remove(folder);

        int at = config.root().indexOf(FolderConfig.folderToken(id));
        if (at < 0) {
            at = config.root().size();
        } else {
            config.root().remove(at);
        }
        List<String> released = new ArrayList<>(folder.items());
        config.root().addAll(at, released);
        for (String item : released) {
            itemIndex.remove(item);
        }
        markDirty();
        return true;
    }

    public boolean setExpanded(UUID id, boolean expanded) {
        Folder folder = folderIndex.get(id);
        if (folder == null || folder.expanded() == expanded) {
            return false;
        }
        folder.setExpanded(expanded);
        markDirty();
        return true;
    }

    public boolean setIcon(UUID id, FolderIcon icon) {
        Folder folder = folderIndex.get(id);
        if (folder == null || folder.icon().equals(icon)) {
            return false;
        }
        folder.setIcon(icon);
        markDirty();
        return true;
    }

    /**
     * Moves an item into a folder, detaching it from wherever it currently is.
     *
     * @param index insertion point inside the target, clamped; {@code -1} appends
     * @return false if the id does not belong to this type or the folder is gone
     */
    public boolean moveToFolder(String itemId, UUID folderId, int index) {
        if (!type.ownsId(itemId)) {
            return false;
        }
        Folder target = folderIndex.get(folderId);
        if (target == null) {
            return false;
        }
        Folder current = itemIndex.get(itemId);
        if (current == target) {
            return reorderInFolder(folderId, itemId, index);
        }
        detach(itemId);
        List<String> items = target.mutableItems();
        int at = (index < 0 || index > items.size()) ? items.size() : index;
        items.add(at, itemId);
        itemIndex.put(itemId, target);
        markDirty();
        return true;
    }

    /**
     * Moves an item out to the root.
     *
     * @param rootIndex position among root tokens, clamped; {@code -1} appends
     */
    public boolean moveToRoot(String itemId, int rootIndex) {
        if (!type.ownsId(itemId)) {
            return false;
        }
        detach(itemId);
        List<String> root = config.root();
        int at = (rootIndex < 0 || rootIndex > root.size()) ? root.size() : rootIndex;
        root.add(at, itemId);
        markDirty();
        return true;
    }

    /** Repositions a folder among the root tokens (§61). */
    public boolean moveFolder(UUID folderId, int rootIndex) {
        if (!folderIndex.containsKey(folderId)) {
            return false;
        }
        String token = FolderConfig.folderToken(folderId);
        List<String> root = config.root();
        int from = root.indexOf(token);
        if (from >= 0) {
            root.remove(from);
        }
        int at = (rootIndex < 0 || rootIndex > root.size()) ? root.size() : rootIndex;
        root.add(at, token);
        markDirty();
        return true;
    }

    /** Reorders an item that is already inside {@code folderId} (§19). */
    public boolean reorderInFolder(UUID folderId, String itemId, int index) {
        Folder folder = folderIndex.get(folderId);
        if (folder == null || !folder.contains(itemId)) {
            return false;
        }
        List<String> items = folder.mutableItems();
        int from = items.indexOf(itemId);
        items.remove(from);
        int at = (index < 0 || index > items.size()) ? items.size() : index;
        items.add(at, itemId);
        if (at != from) {
            markDirty();
        }
        return true;
    }

    /** Removes the item from its folder (if any) and from the root token list. */
    private void detach(String itemId) {
        Folder current = itemIndex.remove(itemId);
        if (current != null) {
            current.mutableItems().remove(itemId);
        }
        config.root().remove(itemId);
    }

    // ------------------------------------------------------------------
    // Synchronisation with the vanilla list
    // ------------------------------------------------------------------

    /**
     * Reconciles the model with the list Minecraft actually has (§53, §58).
     *
     * <p>Repairs everything §8 asks about: ids in two folders at once, ids of the
     * wrong type, duplicated root tokens, folders missing from the root order.
     * Items Minecraft knows about but the model has never seen are inserted next
     * to their vanilla neighbour rather than dumped at the bottom, so a freshly
     * created world still shows up where the player expects it.
     *
     * @param vanillaOrder ids currently in the vanilla list, in vanilla order
     * @param prune        whether {@code vanillaOrder} is a complete snapshot. Pass
     *                     false while the list is still loading — stale references
     *                     are then kept rather than deleted.
     * @return true if anything changed
     */
    public boolean sync(List<String> vanillaOrder, boolean prune) {
        Set<String> known = new LinkedHashSet<>();
        for (String id : vanillaOrder) {
            if (type.ownsId(id)) {
                known.add(id);
            }
        }

        boolean changed = dedupeFolders();
        changed |= cleanFolderItems(known, prune);
        changed |= rebuildRoot(known, prune);
        changed |= insertUnseen(new ArrayList<>(known));

        if (changed) {
            markDirty();
        }
        return changed;
    }

    private boolean dedupeFolders() {
        Set<UUID> seen = new HashSet<>();
        boolean changed = false;
        for (Iterator<Folder> it = config.folders().iterator(); it.hasNext(); ) {
            Folder folder = it.next();
            if (!seen.add(folder.id())) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            reindexFolders();
        }
        return changed;
    }

    /** Drops wrong-type ids, cross-folder duplicates and (when pruning) orphans. */
    private boolean cleanFolderItems(Set<String> known, boolean prune) {
        boolean changed = false;
        Set<String> claimed = new HashSet<>();
        itemIndex.clear();
        for (Folder folder : config.folders()) {
            for (Iterator<String> it = folder.mutableItems().iterator(); it.hasNext(); ) {
                String itemId = it.next();
                boolean bad = !type.ownsId(itemId)
                        || !claimed.add(itemId)
                        || (prune && !known.contains(itemId));
                if (bad) {
                    it.remove();
                    changed = true;
                } else {
                    itemIndex.put(itemId, folder);
                }
            }
        }
        return changed;
    }

    /** Keeps resolvable root tokens in order, then appends any folder that lost its token. */
    private boolean rebuildRoot(Set<String> known, boolean prune) {
        List<String> rebuilt = new ArrayList<>(config.root().size());
        Set<String> seen = new HashSet<>();
        Set<UUID> placedFolders = new HashSet<>();

        for (String token : config.root()) {
            UUID folderId = FolderConfig.parseFolderToken(token);
            if (folderId != null) {
                if (folderIndex.containsKey(folderId) && placedFolders.add(folderId) && seen.add(token)) {
                    rebuilt.add(token);
                }
                continue;
            }
            if (!type.ownsId(token) || itemIndex.containsKey(token) || !seen.add(token)) {
                continue;
            }
            if (prune && !known.contains(token)) {
                continue;
            }
            rebuilt.add(token);
        }

        for (Folder folder : config.folders()) {
            if (placedFolders.add(folder.id())) {
                rebuilt.add(FolderConfig.folderToken(folder.id()));
            }
        }

        if (rebuilt.equals(config.root())) {
            return false;
        }
        config.root().clear();
        config.root().addAll(rebuilt);
        return true;
    }

    /**
     * Adds items Minecraft has but the model has not seen. Each one goes directly
     * after its nearest preceding vanilla neighbour's row (that neighbour's own
     * root token, or the folder that swallowed it); an item with no placed
     * predecessor goes to the top. That keeps a brand new world at the top of a
     * last-played-sorted list and a brand new server at the bottom, without the
     * model having to know which list it is looking at.
     */
    private boolean insertUnseen(List<String> vanillaOrder) {
        Map<String, Integer> rowOf = new HashMap<>();
        List<String> root = config.root();
        for (int i = 0; i < root.size(); i++) {
            String token = root.get(i);
            UUID folderId = FolderConfig.parseFolderToken(token);
            if (folderId == null) {
                rowOf.put(token, i);
            } else {
                Folder folder = folderIndex.get(folderId);
                if (folder != null) {
                    for (String item : folder.items()) {
                        rowOf.put(item, i);
                    }
                }
            }
        }

        record Pending(String itemId, int target, int vanillaIndex) {
        }
        List<Pending> pending = new ArrayList<>();
        for (int i = 0; i < vanillaOrder.size(); i++) {
            String itemId = vanillaOrder.get(i);
            if (rowOf.containsKey(itemId)) {
                continue;
            }
            int target = 0;
            for (int j = i - 1; j >= 0; j--) {
                Integer row = rowOf.get(vanillaOrder.get(j));
                if (row != null) {
                    target = row + 1;
                    break;
                }
            }
            pending.add(new Pending(itemId, target, i));
        }
        if (pending.isEmpty()) {
            return false;
        }

        pending.sort(Comparator.comparingInt(Pending::target).thenComparingInt(Pending::vanillaIndex));
        int offset = 0;
        for (Pending p : pending) {
            root.add(Math.min(p.target() + offset, root.size()), p.itemId());
            offset++;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Indexing / dirty tracking
    // ------------------------------------------------------------------

    private void reindex() {
        reindexFolders();
        itemIndex.clear();
        for (Folder folder : config.folders()) {
            for (String item : folder.items()) {
                itemIndex.putIfAbsent(item, folder);
            }
        }
    }

    private void reindexFolders() {
        folderIndex.clear();
        for (Folder folder : config.folders()) {
            folderIndex.putIfAbsent(folder.id(), folder);
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
        notifyChanged();
    }

    public void markClean() {
        dirty = false;
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
