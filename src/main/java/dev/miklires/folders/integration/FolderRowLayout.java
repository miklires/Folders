package dev.miklires.folders.integration;

/**
 * Row geometry supplied by Folders to a vanilla list widget.
 *
 * <p>Vanilla {@code EntryListWidget} gives every row the same height, which is
 * fine until a folder is half-open and the rows below it need to sit at a
 * fractional offset. The list mixin asks this instead of multiplying by
 * {@code itemHeight}; when no layout is installed, vanilla behaviour is untouched.
 */
public interface FolderRowLayout {

    int rowCount();

    /** Top of a row in content space, 0 being the top of the scrollable content. */
    int rowTop(int index);

    int rowHeight(int index);

    /** Total content height, which is what drives the scrollbar (§28). */
    int contentHeight();

    /** @return the row index at a content-space y, or {@code -1} */
    default int rowAt(int contentY) {
        for (int i = 0; i < rowCount(); i++) {
            int top = rowTop(i);
            if (contentY >= top && contentY < top + rowHeight(i)) {
                return i;
            }
        }
        return -1;
    }
}
