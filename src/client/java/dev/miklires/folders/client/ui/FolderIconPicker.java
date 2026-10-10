package dev.miklires.folders.client.ui;

import dev.miklires.folders.Folders;
import dev.miklires.folders.core.data.FolderIcon;
import dev.miklires.folders.core.data.FolderRepository;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The "Default / Choose PNG / Reset" flow
 *
 * <p>Minecraft 26.3 moved its window and input backend from GLFW/tinyfd to SDL.
 * SDL's file chooser is asynchronous, and the callback is retained until SDL
 * reports a selection or cancellation.
 */
public final class FolderIconPicker {
    private static final Set<SDL_DialogFileCallback> ACTIVE_DIALOGS = ConcurrentHashMap.newKeySet();

    private FolderIconPicker() {
    }

    /** Opens the system file chooser and applies the chosen PNG to the folder. */
    public static void chooseCustom(FolderRepository repository, UUID folderId, Runnable onChanged) {
        promptForPng().thenAcceptAsync(selected -> {
                    if (selected == null) {
                        return;
                    }
                    FolderIcon icon = IconManager.importIcon(folderId, selected);
                    repository.setIcon(folderId, icon);
                    onChanged.run();
                }, Minecraft.getInstance());
    }

    public static void useDefault(FolderRepository repository, UUID folderId, Runnable onChanged) {
        IconManager.removeIcon(folderId);
        repository.setIcon(folderId, FolderIcon.DEFAULT);
        onChanged.run();
    }

    private static CompletableFuture<Path> promptForPng() {
        CompletableFuture<Path> result = new CompletableFuture<>();
        SDL_DialogFileCallback callback = new SDL_DialogFileCallback() {
            @Override
            public void invoke(long userdata, long fileList, int filter) {
                try {
                    long firstPath = fileList == MemoryUtil.NULL ? MemoryUtil.NULL : MemoryUtil.memGetAddress(fileList);
                    result.complete(firstPath == MemoryUtil.NULL ? null : Path.of(MemoryUtil.memUTF8(firstPath)));
                } catch (Throwable error) {
                    Folders.LOGGER.warn("Could not read the selected icon path", error);
                    result.complete(null);
                } finally {
                    ACTIVE_DIALOGS.remove(this);
                    free();
                }
            }
        };
        ACTIVE_DIALOGS.add(callback);
        try {
            SDLDialog.SDL_ShowOpenFileDialog(
                    callback,
                    MemoryUtil.NULL,
                    Minecraft.getInstance().getWindow().handle(),
                    (SDL_DialogFileFilter.Buffer) null,
                    (CharSequence) null,
                    false
            );
        } catch (Throwable error) {
            Folders.LOGGER.warn("Could not open the icon file chooser", error);
            ACTIVE_DIALOGS.remove(callback);
            callback.free();
            result.complete(null);
        }
        return result;
    }
}
