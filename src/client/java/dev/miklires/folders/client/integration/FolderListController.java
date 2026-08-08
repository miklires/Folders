package dev.miklires.folders.client.integration;

import dev.miklires.folders.client.config.FoldersConfig;
import dev.miklires.folders.Folders;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderRepository;
import dev.miklires.folders.core.data.FolderType;
import dev.miklires.folders.core.drag.DragManager;
import dev.miklires.folders.core.drag.DragPayload;
import dev.miklires.folders.core.drag.DropTarget;
import dev.miklires.folders.core.view.DisplayRow;
import dev.miklires.folders.core.view.FolderViewModel;
import dev.miklires.folders.client.ui.FolderContextMenu;
import dev.miklires.folders.client.ui.FolderIconPicker;
import dev.miklires.folders.client.ui.FolderRowRenderer;
import dev.miklires.folders.client.ui.FolderStats;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The behaviour half of the mod: everything folders do inside a list, minus the
 * parts that differ between the three screens.
 *
 * <p>Mixins hand events here and go back to being three lines long. Worlds,
 * servers and packs each subclass this and fill in the handful of type-specific
 * questions, so nothing about drag, animation, ordering or the context menu is
 * written three times.
 *
 * @param <E> the vanilla entry type of the list being extended. Deliberately a
 *            type parameter rather than a hard dependency on any particular
 *            {@code AbstractSelectionList.Entry}.
 */
public abstract class FolderListController<E> {

    protected final FolderType type;
    protected final FolderRepository repository;
    protected final FolderViewModel viewModel;
    protected final DragManager drag = Folders.dragManager();

    /** Vanilla entries by stable id, in vanilla order. */
    private final Map<String, E> vanillaById = new LinkedHashMap<>();
    private final Map<UUID, E> folderEntries = new LinkedHashMap<>();

    private List<DisplayRow> rows = List.of();
    private final List<DropTarget> dropTargets = new ArrayList<>();

    private FolderContextMenu contextMenu;
    private UUID renaming;

    /** Screen-space geometry of the list, refreshed every frame. */
    private int listX;
    private int listTop;
    private int rowWidth = 220;
    private double scrollAmount;

    private boolean pendingScrollCorrection;
    private int scrollAnchorBottom;

    protected FolderListController(FolderType type) {
        this.type = type;
        this.repository = Folders.repository(type);
        this.viewModel = new FolderViewModel(repository, FoldersConfig.get());
    }

    // ------------------------------------------------------------------
    // Type-specific hooks
    // ------------------------------------------------------------------

    /** Stable id for a vanilla entry, from the matching identity resolver. */
    protected abstract String idOf(E entry);

    protected abstract String displayNameOf(E entry);

    /** The grey line under the folder name. */
    protected abstract FolderStats statsFor(Folder folder);

    protected abstract String countKey();

    /** Translation key for the second statistic, or {@code null} if there is none. */
    protected String highlightKey() {
        return null;
    }

    /** Online dot for closed server folders; {@code NONE} elsewhere. */
    protected FolderRowRenderer.OnlineState onlineStateFor(Folder folder) {
        return FolderRowRenderer.OnlineState.NONE;
    }

    /** Extra context-menu items for this type: "Refresh ping", "Enable all"…. */
    protected List<FolderContextMenu.Item> typeMenuItems(Folder folder) {
        return List.of();
    }

    /** Asks the screen to rebuild its widget children from {@link #buildEntries}. */
    protected abstract void requestRebuild();

    /** Wraps a folder in whatever entry class this screen's list requires. */
    protected abstract E createFolderEntry(Folder folder);

    // ------------------------------------------------------------------
    // Building the list
    // ------------------------------------------------------------------

    /**
     * Reconciles with the vanilla list and produces the entries the widget should
     * hold, in display order.
     *
     * @param vanillaEntries the entries Minecraft built, in vanilla order
     * @param complete       false while the list is still loading, so stale
     *                       references are kept rather than pruned
     */
    public List<E> buildEntries(List<E> vanillaEntries, boolean complete) {
        vanillaById.clear();
        for (E entry : vanillaEntries) {
            String id = idOf(entry);
            if (id != null) {
                vanillaById.putIfAbsent(id, entry);
            }
        }

        repository.sync(List.copyOf(vanillaById.keySet()), complete);
        Folders.data().saveIfDirty(type);

        rows = viewModel.layout(vanillaById.keySet());

        List<E> out = new ArrayList<>(rows.size());
        Set<UUID> live = new LinkedHashSet<>();
        for (DisplayRow row : rows) {
            if (row instanceof DisplayRow.FolderRow folderRow) {
                UUID id = folderRow.folder().id();
                live.add(id);
                out.add(folderEntries.computeIfAbsent(id, ignored -> createFolderEntry(folderRow.folder())));
            } else if (row instanceof DisplayRow.ItemRow itemRow) {
                E entry = vanillaById.get(itemRow.itemId());
                if (entry != null) {
                    out.add(entry);
                }
            }
        }
        folderEntries.keySet().retainAll(live);

        // Entries Minecraft has that no row claimed (a resolver returned null, say)
        // are appended rather than silently hidden.
        for (E entry : vanillaEntries) {
            if (!out.contains(entry)) {
                out.add(entry);
            }
        }
        return out;
    }

    /** Row geometry for the list mixin. */
    public FolderRowLayout layout() {
        return new FolderRowLayout() {
            @Override
            public int rowCount() {
                return rows.size();
            }

            @Override
            public int rowTop(int index) {
                return index >= 0 && index < rows.size() ? rows.get(index).y() : index * viewModel.rowHeight();
            }

            @Override
            public int rowHeight(int index) {
                return index >= 0 && index < rows.size() ? rows.get(index).height() : viewModel.rowHeight();
            }

            @Override
            public int contentHeight() {
                return viewModel.totalHeight();
            }
        };
    }

    public List<DisplayRow> rows() {
        return rows;
    }

    public Optional<DisplayRow> rowFor(int index) {
        return index >= 0 && index < rows.size() ? Optional.of(rows.get(index)) : Optional.empty();
    }

    public FolderViewModel viewModel() {
        return viewModel;
    }

    /** Ids Minecraft currently has, in vanilla order. */
    public Set<String> availableIds() {
        return vanillaById.keySet();
    }

    protected Optional<E> vanillaEntry(String itemId) {
        return Optional.ofNullable(vanillaById.get(itemId));
    }

    /**
     * Items of a folder that Minecraft still has. A save deleted outside the game
     * must not be counted.
     */
    protected int countPresent(Folder folder) {
        int present = 0;
        for (String id : folder.items()) {
            if (vanillaById.containsKey(id)) {
                present++;
            }
        }
        return present;
    }

    /** Items of a folder that Minecraft still has, as vanilla entries. */
    protected List<E> presentEntries(Folder folder) {
        List<E> present = new ArrayList<>(folder.items().size());
        for (String id : folder.items()) {
            E entry = vanillaById.get(id);
            if (entry != null) {
                present.add(entry);
            }
        }
        return present;
    }

    public FolderRepository repository() {
        return repository;
    }

    // ------------------------------------------------------------------
    // Frame update
    // ------------------------------------------------------------------

    /**
     * Advances animations and refreshes drop targets. Called once per frame from
     * the list's render; does no file IO and allocates nothing per row beyond the
     * target list.
     */
    public void tick(int listX, int listTop, int rowWidth, double scrollAmount) {
        this.listX = listX;
        this.listTop = listTop;
        this.rowWidth = rowWidth;
        this.scrollAmount = scrollAmount;

        // While a folder is only animating, the set of rows is unchanged and just
        // their positions move — so re-lay-out, but do not rebuild the widget's
        // children. Swapping the children list mid-render would be both wasteful
        // and a good way to trip over a concurrent modification.
        if (viewModel.tick(now())) {
            rows = viewModel.layout(vanillaById.keySet());
        }
        refreshDropTargets();
    }

    protected static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    /** Screen-space y for a row, accounting for scroll. */
    public int screenY(DisplayRow row) {
        return listTop + row.y() - (int) scrollAmount;
    }

    public int rowX(DisplayRow row) {
        return listX + (row instanceof DisplayRow.ItemRow item ? item.indent() : 0);
    }

    public int rowWidth(DisplayRow row) {
        return rowWidth - (row instanceof DisplayRow.ItemRow item ? item.indent() : 0);
    }

    // ------------------------------------------------------------------
    // Drag and drop
    // ------------------------------------------------------------------

    private void refreshDropTargets() {
        dropTargets.clear();
        for (DisplayRow row : rows) {
            if (row instanceof DisplayRow.FolderRow folderRow) {
                dropTargets.add(new FolderDropTarget(folderRow));
            }
        }
        // The root itself accepts anything dragged out of a folder.
        dropTargets.add(new RootDropTarget());
        drag.setTargets(dropTargets);
    }

    /** A folder row swallowing an item. Never accepts another folder. */
    private final class FolderDropTarget implements DropTarget {
        private final UUID folderId;
        private final int top;
        private final int bottom;

        private FolderDropTarget(DisplayRow.FolderRow row) {
            this.folderId = row.folder().id();
            this.top = screenY(row);
            this.bottom = this.top + row.height();
        }

        @Override
        public boolean accepts(DragPayload payload) {
            if (payload.type() != type) {
                return false;
            }
            if (!(payload instanceof DragPayload.Item item)) {
                return false;
            }
            // Dropping an item back into the folder it already lives in is a no-op,
            // so do not offer it as a target.
            return !folderId.equals(item.sourceFolder());
        }

        @Override
        public boolean containsPoint(double mouseX, double mouseY) {
            return mouseY >= top && mouseY < bottom;
        }

        @Override
        public void drop(DragPayload payload) {
            if (payload instanceof DragPayload.Item item) {
                repository.moveToFolder(item.itemId(), folderId, -1);
                afterModelChange();
            }
        }

        @Override
        public int priority() {
            return 10;
        }
    }

    /** Anywhere in the list that is not a folder row: means "put this at the root". */
    private final class RootDropTarget implements DropTarget {

        @Override
        public boolean accepts(DragPayload payload) {
            if (payload.type() != type) {
                return false;
            }
            // Only meaningful for something currently inside a folder.
            return payload instanceof DragPayload.Item item && item.fromFolder();
        }

        @Override
        public boolean containsPoint(double mouseX, double mouseY) {
            return true;
        }

        @Override
        public void drop(DragPayload payload) {
            if (!(payload instanceof DragPayload.Item item)) {
                return;
            }
            repository.moveToRoot(item.itemId(), rootIndexAt(drag.mouseY()));
            afterModelChange();
        }

        @Override
        public int priority() {
            return -10;
        }

        @Override
        public boolean highlightWhenHovered() {
            return false;
        }
    }

    /** Which root position a screen-space y corresponds to. */
    private int rootIndexAt(double mouseY) {
        int contentY = (int) (mouseY - listTop + scrollAmount);
        int index = 0;
        for (DisplayRow row : rows) {
            if (row instanceof DisplayRow.ItemRow item && item.isInFolder()) {
                continue;
            }
            if (contentY < row.y() + row.height() / 2) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /** Starts a potential drag; the gesture is still a click until it moves. */
    public void pressItem(String itemId, String displayName, double mouseX, double mouseY) {
        UUID owner = repository.folderContaining(itemId).map(Folder::id).orElse(null);
        drag.press(new DragPayload.Item(type, itemId, displayName, owner), mouseX, mouseY, now());
    }

    public void pressFolder(Folder folder, double mouseX, double mouseY) {
        drag.press(new DragPayload.FolderHandle(type, folder.id(), folder.name()), mouseX, mouseY, now());
    }

    /** @return true if this became a drag, meaning the vanilla click is suppressed */
    public boolean mouseDragged(double mouseX, double mouseY) {
        return drag.move(mouseX, mouseY);
    }

    /** @return true if a drop happened and the vanilla release should be skipped */
    public boolean mouseReleased(double mouseX, double mouseY) {
        boolean dropped = drag.release(mouseX, mouseY);
        if (dropped) {
            requestRebuild();
        }
        return dropped;
    }

    public boolean isDropTarget(Folder folder) {
        return drag.hoveredTarget()
                .filter(target -> target instanceof FolderDropTarget folderTarget
                        && folderTarget.folderId.equals(folder.id()))
                .isPresent();
    }

    // ------------------------------------------------------------------
    // Folder actions
    // ------------------------------------------------------------------

    public Folder createFolder() {
        Folder folder = repository.createFolder(Component.translatable("folders.default_name").getString());
        afterModelChange();
        beginRename(folder.id());
        return folder;
    }

    /** Opens or closes a folder, scrolling if the contents would open off-screen. */
    public void toggle(Folder folder, int viewportBottom) {
        boolean opening = !folder.expanded();
        viewModel.setExpanded(folder.id(), opening);
        if (opening) {
            pendingScrollCorrection = true;
            scrollAnchorBottom = viewportBottom;
        }
        afterModelChange();
    }

    /**
     * How far the list should scroll so a just-opened folder's contents are
     * visible. Returns 0 when nothing needs to move.
     */
    public int consumeScrollCorrection(int viewportHeight) {
        if (!pendingScrollCorrection) {
            return 0;
        }
        pendingScrollCorrection = false;
        int settled = viewModel.settledHeight(vanillaById.keySet());
        int overflow = settled - viewportHeight - (int) scrollAmount;
        return Math.max(0, Math.min(overflow, scrollAnchorBottom));
    }

    public void deleteFolder(Folder folder) {
        // Contents go back to the root; no world, server or pack is touched.
        repository.deleteFolder(folder.id());
        afterModelChange();
    }

    public void beginRename(UUID folderId) {
        this.renaming = folderId;
    }

    public void cancelRename() {
        this.renaming = null;
    }

    public boolean isRenaming(UUID folderId) {
        return folderId.equals(renaming);
    }

    public Optional<UUID> renamingFolder() {
        return Optional.ofNullable(renaming);
    }

    /** @return true if the name was accepted; an empty name is refused */
    public boolean commitRename(UUID folderId, String newName) {
        if (!repository.renameFolder(folderId, newName)) {
            return false;
        }
        renaming = null;
        afterModelChange();
        return true;
    }

    /** Persists and refreshes. The only place either happens. */
    protected void afterModelChange() {
        Folders.data().saveIfDirty(type);
        requestRebuild();
    }

    // ------------------------------------------------------------------
    // Context menu
    // ------------------------------------------------------------------

    public void openContextMenu(Folder folder, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        Window window = client.getWindow();

        List<FolderContextMenu.Item> items = new ArrayList<>();
        items.add(FolderContextMenu.Item.of(
                folder.expanded() ? "folders.menu.collapse" : "folders.menu.expand",
                () -> toggle(folder, mouseY)));
        items.add(FolderContextMenu.Item.of("folders.menu.rename", () -> beginRename(folder.id())));
        items.add(FolderContextMenu.Item.of("folders.menu.icon", () -> openIconMenu(folder, mouseX, mouseY)));
        items.addAll(typeMenuItems(folder));
        items.add(FolderContextMenu.Item.of("folders.menu.delete", () -> deleteFolder(folder)));

        contextMenu = FolderContextMenu.open(items, mouseX, mouseY,
                window.getScaledWidth(), window.getScaledHeight());
    }

    private void openIconMenu(Folder folder, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        Window window = client.getWindow();
        contextMenu = FolderContextMenu.open(List.of(
                FolderContextMenu.Item.of("folders.icon.default",
                        () -> FolderIconPicker.useDefault(repository, folder.id(), this::afterModelChange)),
                FolderContextMenu.Item.of("folders.icon.choose",
                        () -> FolderIconPicker.chooseCustom(repository, folder.id(), this::afterModelChange))
        ), mouseX, mouseY, window.getScaledWidth(), window.getScaledHeight());
    }

    public Optional<FolderContextMenu> contextMenu() {
        return Optional.ofNullable(contextMenu);
    }

    public void closeContextMenu() {
        contextMenu = null;
    }

    /** @return true when the menu consumed the click */
    public boolean contextMenuClicked(double mouseX, double mouseY) {
        if (contextMenu == null) {
            return false;
        }
        boolean inside = contextMenu.mouseClicked(mouseX, mouseY);
        if (!inside) {
            contextMenu = null;
            return false;
        }
        // An item that opened a submenu has already replaced the menu.
        if (contextMenu != null && !contextMenu.contains(mouseX, mouseY)) {
            return true;
        }
        contextMenu = null;
        return true;
    }

    // ------------------------------------------------------------------
    // Row context for the renderer
    // ------------------------------------------------------------------

    public FolderRowRenderer.RowContext rowContextFor(Folder folder, int rowX, int rowY,
                                                      double mouseX, double mouseY,
                                                      boolean hovered, float alpha) {
        boolean dropTarget = isDropTarget(folder);
        FolderStats stats = statsFor(folder);
        return new FolderRowRenderer.RowContext(
                folder,
                stats,
                countKey(),
                highlightKey(),
                folder.expanded() ? FolderRowRenderer.OnlineState.NONE : onlineStateFor(folder),
                hovered,
                hovered && FolderRowRenderer.isOverIcon(rowX, rowY, mouseX, mouseY),
                dropTarget,
                viewModel.expansionOf(folder.id()),
                alpha);
    }

    /** Narration text, e.g. "Survival, folder, 5 worlds, collapsed". */
    public Component narrationFor(Folder folder) {
        FolderStats stats = statsFor(folder);
        return Component.translatable("folders.narration",
                folder.name(),
                Component.translatable(countKey(), stats.total()),
                Component.translatable(folder.expanded() ? "folders.state.expanded" : "folders.state.collapsed"));
    }

    /** Tooltip; suppressed when the player has turned tooltips off. */
    public Optional<List<Component>> tooltipFor(Folder folder) {
        if (!FoldersConfig.get().showTooltips) {
            return Optional.empty();
        }
        FolderStats stats = statsFor(folder);
        return Optional.of(List.of(
                Component.literal(folder.name()),
                stats.describe(countKey(), highlightKey(), false),
                Component.translatable("folders.tooltip.left_click"),
                Component.translatable("folders.tooltip.right_click")));
    }

}
