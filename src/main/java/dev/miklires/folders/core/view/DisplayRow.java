package dev.miklires.folders.core.view;

import dev.miklires.folders.core.data.Folder;

import java.util.UUID;

/**
 * One laid-out row, in list-content coordinates (0 = top of the scrollable
 * content).
 *
 * <p>The integration layer turns these into whatever entry class the vanilla
 * widget wants. Nothing here knows about Minecraft — that is the point of §45.
 */
public sealed interface DisplayRow {

    int y();

    int height();

    /** Rows outside the animating band are fully opaque. */
    float alpha();

    record FolderRow(Folder folder, int y, int height, float expansion) implements DisplayRow {
        @Override
        public float alpha() {
            return 1.0f;
        }

        public boolean isAnimating() {
            return expansion > 0.0f && expansion < 1.0f;
        }
    }

    /**
     * @param owner        folder this item is displayed inside, or {@code null} at the root
     * @param indent       horizontal offset in pixels (§29)
     * @param clipTop      top of the band this row must be scissored to
     * @param clipBottom   bottom of that band; equals {@code y + height} for root rows
     */
    record ItemRow(String itemId, UUID owner, int y, int height, int indent, float alpha,
                   int clipTop, int clipBottom) implements DisplayRow {

        public boolean isInFolder() {
            return owner != null;
        }

        /** True when the row is partly outside its band and needs scissoring. */
        public boolean needsClip() {
            return clipTop > y || clipBottom < y + height;
        }
    }
}
