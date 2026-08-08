package dev.miklires.folders.client.integration;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.GhostRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;

import java.util.List;

/**
 * The handful of calls a list mixin makes.
 *
 * <p>Each mixin is then three or four lines of {@code @Inject} that delegate here,
 * which keeps the split honest: mixins connect, this decides.
 */
public final class FoldersListHooks {

    private FoldersListHooks() {
    }

    /**
     * Replaces a widget's children with the folder-aware ordering and installs the
     * row layout.
     *
     * @param complete false while the list is still loading
     *
     * <p>MAPPING NOTE: this mutates {@code children()} in place because the
     * vanilla {@code clearEntries()} also resets scroll and selection, which would
     * make the list jump every time a folder is renamed. If {@code children()}
     * stops being publicly reachable, an accessor mixin on {@code AbstractSelectionList}
     * is the replacement — not {@code clearEntries()}.
     */
    public static <E extends AbstractSelectionList.Entry<E>> void applyEntries(
            AbstractSelectionList<E> widget, FolderListController<E> controller, boolean complete) {

        Folders.guarded("rebuilding a list", () -> {
            List<E> vanilla = List.copyOf(widget.children());
            List<E> ordered = controller.buildEntries(vanilla, complete);

            widget.children().clear();
            widget.children().addAll(ordered);

            ((FoldersListAccess) widget).folders$setLayout(controller.layout());
        });
    }

    /** Advances animations and refreshes drop targets. Call at the head of render. */
    public static void beforeRender(AbstractSelectionList<?> widget, FolderListController<?> controller) {
        Folders.guarded("updating folder animations", () ->
                controller.tick(widget.getRowLeft(), widget.getY(), widget.getRowWidth(), widget.scrollAmount()));
    }

    /** Ghost preview and context menu, drawn above the list. Call at the tail of render. */
    public static void afterRender(GuiGraphicsExtractor graphics, FolderListController<?> controller,
                                   int mouseX, int mouseY) {
        Folders.guarded("drawing the folder overlay", () -> {
            controller.contextMenu().ifPresent(menu -> menu.render(graphics, mouseX, mouseY));
            GhostRenderer.render(graphics, Folders.dragManager());
        });
    }

    /**
     * @return true when Folders consumed the click and the vanilla handler should
     *         be skipped — only ever for the context menu, so an ordinary click on
     *         a world or server still does exactly what it always did
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
