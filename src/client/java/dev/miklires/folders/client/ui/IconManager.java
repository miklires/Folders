package dev.miklires.folders.client.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.core.data.Folder;
import dev.miklires.folders.core.data.FolderIcon;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches user-supplied folder icons.
 *
 * <p>Three rules:
 * <ul>
 *   <li>the file on disk is named after the folder UUID, so a folder called
 *       {@code ../../server} cannot reach outside the icon directory;</li>
 *   <li>the source PNG is copied in, so deleting the original does not break the
 *       icon;</li>
 *   <li>anything that fails validation quietly becomes the default icon — a bad
 *       image must not take the multiplayer screen down.</li>
 * </ul>
 */
public final class IconManager {

    /** Big enough for a 512×512 PNG, small enough that nothing silly gets loaded. */
    public static final long MAX_FILE_BYTES = 2L * 1024L * 1024L;
    public static final int MAX_DIMENSION = 1024;

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FAILED = new HashMap<>();

    private IconManager() {
    }

    /** The texture to draw for a folder: its custom icon, or the built-in one. */
    public static Identifier textureFor(Folder folder) {
        FolderIcon icon = folder.icon();
        if (!icon.isCustom()) {
            return folder.expanded() ? FolderTextures.FOLDER_OPEN : FolderTextures.FOLDER_CLOSED;
        }
        String file = icon.file().orElse(null);
        if (file == null || Boolean.TRUE.equals(FAILED.get(file))) {
            return FolderTextures.FOLDER_CLOSED;
        }
        Identifier cached = CACHE.get(file);
        if (cached != null) {
            return cached;
        }
        return load(file).orElse(FolderTextures.FOLDER_CLOSED);
    }

    private static synchronized Optional<Identifier> load(String file) {
        Identifier existing = CACHE.get(file);
        if (existing != null) {
            return Optional.of(existing);
        }
        Path path = Folders.data().storage().iconDirectory().resolve(file);
        try {
            if (!Files.isRegularFile(path)) {
                throw new IOException("no such icon file");
            }
            if (Files.size(path) > MAX_FILE_BYTES) {
                throw new IOException("icon is larger than " + MAX_FILE_BYTES + " bytes");
            }
            NativeImage image;
            try (InputStream in = Files.newInputStream(path)) {
                image = NativeImage.read(in);
            }
            if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                image.close();
                throw new IOException("icon is larger than " + MAX_DIMENSION + "px");
            }

            Identifier id = Identifier.fromNamespaceAndPath(Folders.MOD_ID, "icon/" + sanitiseForIdentifier(file));
            Minecraft.getInstance().getTextureManager()
                    .register(id, new DynamicTexture(() -> id.toString(), image));
            CACHE.put(file, id);
            return Optional.of(id);
        } catch (Exception e) {
            // Remembered so a broken icon is not retried every single frame.
            FAILED.put(file, Boolean.TRUE);
            Folders.LOGGER.warn("Could not load folder icon {}, falling back to the default", path, e);
            return Optional.empty();
        }
    }

    /**
     * Copies a PNG the player picked into the mod's icon directory.
     *
     * @return the icon to store on the folder, or {@link FolderIcon#DEFAULT} if the
     *         file was not usable
     */
    public static FolderIcon importIcon(UUID folderId, Path source) {
        String fileName = folderId + ".png";
        try {
            if (!Files.isRegularFile(source)) {
                throw new IOException("not a file");
            }
            if (Files.size(source) > MAX_FILE_BYTES) {
                throw new IOException("larger than " + MAX_FILE_BYTES + " bytes");
            }
            if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                throw new IOException("not a .png");
            }
            // Decode before committing, so an unusable image never reaches the config.
            try (InputStream in = Files.newInputStream(source)) {
                NativeImage probe = NativeImage.read(in);
                int width = probe.getWidth();
                int height = probe.getHeight();
                probe.close();
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new IOException("larger than " + MAX_DIMENSION + "px");
                }
            }

            Path directory = Folders.data().storage().iconDirectory();
            Files.createDirectories(directory);
            Files.copy(source, directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            forget(fileName);
            return FolderIcon.custom(fileName);
        } catch (Exception e) {
            Folders.LOGGER.warn("Could not import folder icon {}", source, e);
            return FolderIcon.DEFAULT;
        }
    }

    /** Drops a cached texture so the next draw reloads it. */
    public static void forget(String fileName) {
        CACHE.remove(fileName);
        FAILED.remove(fileName);
    }

    /** Deletes a folder's icon file when the player resets it. */
    public static void removeIcon(UUID folderId) {
        String fileName = folderId + ".png";
        forget(fileName);
        try {
            Files.deleteIfExists(Folders.data().storage().iconDirectory().resolve(fileName));
        } catch (IOException e) {
            Folders.LOGGER.warn("Could not delete folder icon {}", fileName, e);
        }
    }

    private static String sanitiseForIdentifier(String file) {
        return file.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }
}
