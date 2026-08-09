package dev.miklires.folders.mixin.client;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.identity.PackIdentityResolver;
import dev.miklires.folders.client.integration.pack.FolderPack;
import dev.miklires.folders.client.integration.pack.PackListIntegration;
import dev.miklires.folders.client.integration.pack.PackScreens;
import dev.miklires.folders.core.data.Folder;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * What a click means on a resource pack row.
 *
 * <p>Folder rows on this screen are ordinary {@code PackEntry} widgets over a
 * {@link FolderPack}, so vanilla draws them and this decides what they do. They are recognised by
 * their pack id, which is the one thing a row carries that Folders controls.
 *
 * <p>Real packs keep every gesture they had. Right-clicking one — which vanilla ignores — offers to
 * file it in a folder.
 */
@Mixin(TransferableSelectionList.PackEntry.class)
public abstract class PackEntryMixin {

    @Shadow
    public abstract String getPackId();

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void folders$click(MouseButtonEvent click, boolean doubleClick,
                               CallbackInfoReturnable<Boolean> info) {
        Folders.guarded("handling a click on a resource pack row", () -> {
            UUID folderId = FolderPack.folderIdOf(getPackId());
            if (folderId != null) {
                folders$folderClick(folderId, click, info);
                return;
            }
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                folders$moveMenu(click, info);
            }
        });
    }

    /**
     * A folder row: left click opens or closes it, right click is the folder menu. Either way the
     * click stops here — vanilla must never treat a folder as a pack to enable.
     */
    @Unique
    private void folders$folderClick(UUID folderId, MouseButtonEvent click,
                                     CallbackInfoReturnable<Boolean> info) {
        PackListIntegration controller = PackScreens.at(click.x(), click.y()).orElse(null);
        if (controller == null) {
            return;
        }
        Optional<Folder> folder = controller.repository().folder(folderId);
        if (folder.isEmpty()) {
            return;
        }
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            controller.openContextMenu(folder.get(), (int) click.x(), (int) click.y());
        } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            controller.toggle(folder.get(), controller.widget().getBottom());
        }
        info.setReturnValue(true);
    }

    @Unique
    private void folders$moveMenu(MouseButtonEvent click, CallbackInfoReturnable<Boolean> info) {
        String itemId = PackIdentityResolver.idOfProfileId(getPackId());
        if (itemId == null) {
            return;
        }
        PackScreens.at(click.x(), click.y()).ifPresent(controller -> {
            controller.openMoveMenu(itemId, (int) click.x(), (int) click.y());
            info.setReturnValue(true);
        });
    }
}
