package dev.miklires.folders.core.drag;

import dev.miklires.folders.core.data.FolderType;

import java.util.Objects;
import java.util.UUID;

/**
 * What is being dragged: either a list item or a whole folder.
 *
 * <p>{@code displayName} is carried only so the ghost preview has something to
 * draw without reaching back into the screen.
 */
public sealed interface DragPayload {

    FolderType type();

    String displayName();

    record Item(FolderType type, String itemId, String displayName, UUID sourceFolder) implements DragPayload {
        public Item {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(itemId, "itemId");
        }

        /** @return true when the item is being dragged out of a folder rather than the root. */
        public boolean fromFolder() {
            return sourceFolder != null;
        }
    }

    record FolderHandle(FolderType type, UUID folderId, String displayName) implements DragPayload {
        public FolderHandle {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(folderId, "folderId");
        }
    }
}
