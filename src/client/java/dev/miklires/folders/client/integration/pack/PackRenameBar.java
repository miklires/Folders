package dev.miklires.folders.client.integration.pack;

import java.util.function.Consumer;

/**
 * The pending "type a name here" request on the pack screen.
 *
 * <p>Worlds and servers edit a name inside the row. Pack rows are vanilla widgets built from a
 * title, with nowhere to put a caret and no character events to read, so the pack screen asks for
 * names in a real {@code EditBox} it puts above the list. This is the handoff between the thing
 * that wants a name — a folder being renamed, a profile being saved — and the screen that owns the
 * field.
 *
 * <p>State rather than a widget, because the widget belongs to the screen and this does not: a
 * controller can ask for a rename without holding a reference to a screen that may already be gone.
 */
public final class PackRenameBar {

    private static String initial = "";
    private static String label = "";
    private static Consumer<String> onChange;

    private PackRenameBar() {
    }

    /**
     * @param onChange called on every keystroke, so the folder or profile is renamed live and there
     *                 is no "did that save?" moment
     */
    public static void begin(String initial, Consumer<String> onChange) {
        begin(initial, "folders.rename.folder", onChange);
    }

    public static void begin(String initial, String labelKey, Consumer<String> onChange) {
        PackRenameBar.initial = initial == null ? "" : initial;
        PackRenameBar.label = labelKey;
        PackRenameBar.onChange = onChange;
    }

    public static void end() {
        onChange = null;
        initial = "";
        label = "";
    }

    public static boolean isActive() {
        return onChange != null;
    }

    public static String initial() {
        return initial;
    }

    public static String labelKey() {
        return label;
    }

    public static void changed(String value) {
        Consumer<String> listener = onChange;
        if (listener != null) {
            listener.accept(value);
        }
    }
}
