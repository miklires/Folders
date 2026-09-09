package dev.miklires.folders.client.integration.pack;

import net.minecraft.client.gui.screens.packs.TransferableSelectionList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The pack lists currently on screen.
 *
 * <p>The pack screen is the only one with two folder lists, and a change on either side has to be
 * reflected on both: moving a pack into a folder while it is disabled has to show up in the same
 * folder on the enabled side the moment the pack is turned on. One place holding both lists is
 * simpler than each of them knowing about the other.
 *
 * <p>Cleared when the screen initialises, so a list belonging to a screen that has been closed is
 * never rebuilt.
 */
public final class PackScreens {

    private static final List<PackListIntegration> LISTS = new ArrayList<>();

    private PackScreens() {
    }

    static void register(PackListIntegration list) {
        LISTS.add(list);
    }

    /** Called from the screen's init, before either list has been built. */
    public static void reset() {
        LISTS.clear();
        PackRenameBar.end();
    }

    public static void rebuildAll() {
        for (PackListIntegration list : List.copyOf(LISTS)) {
            list.applyCached();
        }
    }

    static void invalidateAll() {
        for (PackListIntegration list : List.copyOf(LISTS)) {
            list.invalidate();
        }
    }

    /** Any live pack list, for actions that only need the shared repository. */
    public static Optional<PackListIntegration> any() {
        return LISTS.isEmpty() ? Optional.empty() : Optional.of(LISTS.getFirst());
    }

    /**
     * The list a point falls in, or any list if it falls in neither.
     *
     * <p>A menu is drawn and clicked by the list it was opened on, so on the one screen with two of
     * them the menu has to belong to the one the player is pointing at. Opening it on the other
     * side would draw it in the right place and then ignore every click.
     */
    public static Optional<PackListIntegration> at(double x, double y) {
        for (PackListIntegration list : LISTS) {
            TransferableSelectionList widget = list.widget();
            if (x >= widget.getX() && x < widget.getRight()
                    && y >= widget.getY() && y < widget.getBottom()) {
                return Optional.of(list);
            }
        }
        return any();
    }
}
