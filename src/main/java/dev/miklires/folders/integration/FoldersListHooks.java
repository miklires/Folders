package dev.miklires.folders.integration;

import dev.miklires.folders.Folders;
import dev.miklires.folders.ui.GhostRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;

import java.util.List;

/**
 * The handful of calls a list mixin makes.
 *
 * <p>Each mixin is then three or four lines of {@code @Inject} that delegate here,
 * which is the split §44 asks for: mixins connect, this decides.
 */
public final class FoldersListHooks {

    private FoldersListHooks() {
    }

    /**
     * Replaces a widget's children with the folder-aware ordering and installs the
     * row layout.
     *
     * @param complete false while the list is still loading (§53)
     *
     * <p>MAPPING NOTE: this mutates {@code children()} in place because the
     * vanilla {@code clearEntries()} also resets scroll and selection, which would
     * make the list jump every time a folder is renamed. If {@code children()}
     * stops being publicly reachable, an accessor mixin on {@code EntryListWidget}
     * is the replacement — not {@code clearEntries()}.
     */
    public static <E extends EntryListWidget.Entry<E>> void applyEntries(
            EntryListWidget<E> widget, FolderListController<E> controller, boolean complete) {

        Folders.guarded("rebuilding a list", () -> {
            List<E> vanilla = List.copyOf(widget.children());
            List<E> ordered = controller.buildEntries(vanilla, complete);

            widget.children().clear();
            widget.children().addAll(ordered);

            ((FoldersListAccess) widget).folders$setLayout(controller.layout());
        });
    }

    /** Advances animations and refreshes drop targets. Call at the head of render. */
    public static void beforeRender(EntryListWidget<?> widget, FolderListController<?> controller) {
        Folders.guarded("updating folder animations", () ->
                controller.tick(widget.getRowLeft(), widget.getY(), widget.getRowWidth(), widget.getScrollAmount()));
    }

    /** Ghost preview and context menu, drawn above the list. Call at the tail of render. */
    public static void afterRender(DrawContext context, FolderListController<?> controller,
                                   int mouseX, int mouseY) {
        Folders.guarded("drawing the folder overlay", () -> {
            controller.contextMenu().ifPresent(menu -> menu.render(context, mouseX, mouseY));
            GhostRenderer.render(context, Folders.dragManager());
        });
    }

    /**
     * @return true when Folders consumed the click and the vanilla handler should
     *         be skipped — only ever for the context menu, so an ordinary click on
     *         a world or server still does exactly what it always did (§16)
     */
    public static boolean mouseClicked(FolderListController<?> controller, double mouseX, double mouseY) {
        return controller.contextMenuClicked(mouseX, mouseY);
    }

    public static boolean mouseDragged(FolderListController<?> controller, double mouseX, double mouseY) {
        return controller.mouseDragged(mouseX, mouseY);
    }

    public static boolean mouseReleased(FolderListController<?> controller, double mouseX, double mouseY) {
        return controller.mouseReleased(mouseX, mouseY);
    }
}
