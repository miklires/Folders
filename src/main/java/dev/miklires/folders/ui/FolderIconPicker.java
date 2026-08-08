package dev.miklires.folders.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.core.data.FolderIcon;
import dev.miklires.folders.core.data.FolderRepository;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The "Default / Choose PNG / Reset" flow from §23.
 *
 * <p>The native file chooser blocks, so it runs off-thread and hops back onto the
 * client thread to touch the model. Minecraft ships tinyfd, which is the same
 * dialog vanilla uses for screenshots and world imports, so this needs no extra
 * dependency.
 */
public final class FolderIconPicker {

    private FolderIconPicker() {
    }

    /** Opens the system file chooser and applies the chosen PNG to the folder. */
    public static void chooseCustom(FolderRepository repository, UUID folderId, Runnable onChanged) {
        CompletableFuture
                .supplyAsync(FolderIconPicker::promptForPng)
                .thenAcceptAsync(selected -> {
                    if (selected == null) {
                        return;
                    }
                    FolderIcon icon = IconManager.importIcon(folderId, selected);
                    repository.setIcon(folderId, icon);
                    onChanged.run();
                }, MinecraftClient.getInstance());
    }

    public static void useDefault(FolderRepository repository, UUID folderId, Runnable onChanged) {
        IconManager.removeIcon(folderId);
        repository.setIcon(folderId, FolderIcon.DEFAULT);
        onChanged.run();
    }

    private static Path promptForPng() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                    net.minecraft.text.Text.translatable("folders.icon.choose").getString(),
                    null, filters, "PNG (*.png)", false);
            return chosen == null ? null : Path.of(chosen);
        } catch (Throwable error) {
            // A missing or refused native dialog must not break the screen (§74).
            Folders.LOGGER.warn("Could not open the icon file chooser", error);
            return null;
        }
    }
}
