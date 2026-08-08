import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the mod's textures. Kept in the repository so the art is
 * reproducible and reviewable as code rather than as opaque binaries.
 *
 * Run: java .verify/GenerateTextures.java
 */
public final class GenerateTextures {

    // A muted, low-saturation palette so a folder sits next to a world
    // screenshot without shouting (spec §69, §21).
    static final int OUTLINE   = 0xFF2B2119;
    static final int FACE      = 0xFFC8A165;
    static final int FACE_DARK = 0xFF8F6B37;
    static final int SHADE     = 0xFF6B4F2A;
    static final int HILIGHT   = 0xFFE0BE85;
    static final int ARROW     = 0xFFF0F0F0;
    static final int ARROW_EDGE = 0xFF202020;
    static final int NONE      = 0x00000000;

    public static void main(String[] args) throws Exception {
        Path out = Path.of("src/main/resources/assets/folders/textures/gui");
        Files.createDirectories(out);

        write(out.resolve("folder_closed.png"), folder(false));
        write(out.resolve("folder_open.png"), folder(true));
        write(out.resolve("arrow_down.png"), arrow(false));
        write(out.resolve("arrow_up.png"), arrow(true));

        Path assets = Path.of("src/main/resources/assets/folders");
        write(assets.resolve("icon.png"), scale(folder(false), 128));
        System.out.println("textures written");
    }

    /** 32x32 folder. Closed: flat front. Open: front flap tilted forward. */
    static BufferedImage folder(boolean open) {
        BufferedImage image = blank(32);
        // Back panel with the tab, common to both states.
        rect(image, 3, 7, 26, 20, FACE_DARK);
        rect(image, 3, 4, 11, 4, FACE_DARK);
        outline(image, 3, 4, 11, 4);
        outline(image, 3, 7, 26, 20);

        if (!open) {
            rect(image, 3, 10, 26, 17, FACE);
            outline(image, 3, 10, 26, 17);
            // A single highlight line along the top edge reads as a lid without
            // needing a gradient.
            line(image, 4, 11, 27, 11, HILIGHT);
            line(image, 4, 26, 27, 26, SHADE);
        } else {
            // Front flap sits lower and inset, so the back panel shows above it.
            rect(image, 3, 14, 26, 13, FACE);
            outline(image, 3, 14, 26, 13);
            line(image, 4, 15, 27, 15, HILIGHT);
            line(image, 4, 26, 27, 26, SHADE);
            // Sliver of contents peeking out of the top.
            rect(image, 6, 11, 20, 3, HILIGHT);
            outline(image, 6, 11, 20, 3);
        }
        return image;
    }

    /** 16x16 chevron, outlined so it stays readable over any icon. */
    static BufferedImage arrow(boolean up) {
        BufferedImage image = blank(16);
        for (int step = 0; step < 5; step++) {
            int y = up ? 10 - step : 5 + step;
            int halfWidth = 5 - step;
            for (int x = 8 - halfWidth; x <= 7 + halfWidth; x++) {
                set(image, x, y, ARROW);
                set(image, x, y + (up ? 1 : -1), ARROW);
            }
        }
        // One-pixel dark rim: the same trick vanilla uses on small glyphs.
        BufferedImage rimmed = blank(16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (image.getRGB(x, y) != 0) {
                    rimmed.setRGB(x, y, ARROW);
                } else if (hasNeighbour(image, x, y)) {
                    rimmed.setRGB(x, y, ARROW_EDGE);
                }
            }
        }
        return rimmed;
    }

    static boolean hasNeighbour(BufferedImage image, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight()
                        && image.getRGB(nx, ny) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------

    static BufferedImage blank(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, NONE);
            }
        }
        return image;
    }

    static void set(BufferedImage image, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            image.setRGB(x, y, argb);
        }
    }

    static void rect(BufferedImage image, int x, int y, int w, int h, int argb) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                set(image, x + dx, y + dy, argb);
            }
        }
    }

    static void line(BufferedImage image, int x1, int y1, int x2, int y2, int argb) {
        for (int x = x1; x <= x2; x++) {
            set(image, x, y1, argb);
        }
    }

    static void outline(BufferedImage image, int x, int y, int w, int h) {
        for (int dx = 0; dx < w; dx++) {
            set(image, x + dx, y, OUTLINE);
            set(image, x + dx, y + h - 1, OUTLINE);
        }
        for (int dy = 0; dy < h; dy++) {
            set(image, x, y + dy, OUTLINE);
            set(image, x + w - 1, y + dy, OUTLINE);
        }
    }

    /** Nearest-neighbour so the pixel art stays crisp. */
    static BufferedImage scale(BufferedImage source, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int factor = size / source.getWidth();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                out.setRGB(x, y, source.getRGB(x / factor, y / factor));
            }
        }
        return out;
    }

    static void write(Path path, BufferedImage image) throws Exception {
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "PNG", new File(path.toString()));
        System.out.println("  " + path);
    }
}
