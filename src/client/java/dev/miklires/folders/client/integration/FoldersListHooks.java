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
     * Works out the entries a list should hold, in display order.
     *
     * <p>The caller applies them with {@code replaceEntries}, which is the only supported way to
     * swap a list's contents: 26.2 positions rows by walking them and accumulating heights, and
     * that pass only runs from the list's own mutators. Editing {@code children()} in place leaves
     * every row where the previous layout put it.
     *
     * @param complete false while the list is still loading
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> orderedEntries(AbstractSelectionList<?> widget,
                                             FolderListController<E> controller, boolean complete) {
        // AbstractSelectionList.Entry is protected, so the element type cannot be named from here;
        // the controller does know it, and is the only thing that touches the contents.
        List<E> vanilla = (List<E>) List.copyOf(widget.children());
        return controller.buildEntries(vanilla, complete);
    }

    /** Advances animations and refreshes drop targets. Call at the head of render. */
    public static void beforeRender(AbstractSelectionList<?> widget, FolderListController<?> controller) {
        // Scroll offset is passed as 0: AbstractSelectionList exposes setScrollAmount but no
        // confirmed getter, and the only consumer is drop-target hit testing, which is not wired
        // up yet. It becomes real the moment drag does.
        ScreenFolders.setActive(controller);
        Folders.guarded("updating folder animations", () ->
                controller.tick(widget.getRowLeft(), widget.getY(), widget.getRowWidth(), 0.0));
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
