package dev.miklires.folders.client.integration.pack;

import dev.miklires.folders.Folders;
import dev.miklires.folders.client.ui.FolderContextMenu;
import dev.miklires.folders.core.profile.PackProfile;
import dev.miklires.folders.core.profile.PackProfileStore;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Saved sets of enabled resource packs, and the menu for working with them.
 *
 * <p>A profile is applied through {@link PackSelectionModel} exactly as if the packs had been
 * clicked across by hand: Minecraft stays the source of truth for what is on, the pack repository
 * is written by vanilla and only by vanilla, and Folders stores nothing but a list of ids.
 * "Restore" is therefore indistinguishable from doing it manually, which is the point — nothing
 * about the pack setup depends on this mod still being installed.
 */
public final class PackProfiles {

    private PackProfiles() {
    }

    // ------------------------------------------------------------------
    // Applying
    // ------------------------------------------------------------------

    /** The ids of every currently enabled pack, in the order Minecraft has them. */
    public static List<String> currentSelection(PackSelectionModel model) {
        return model.getSelected().map(PackSelectionModel.Entry::getId).toList();
    }

    /**
     * Switches the enabled packs to exactly those the profile names.
     *
     * <p>Off first, then on: turning a pack off can change what the others can do, and doing it in
     * this order means the "on" pass sees the final state rather than an intermediate one. Packs
     * the profile names but the player no longer has are skipped, and a pack that refuses to be
     * unselected — a required one — is left alone rather than forced.
     */
    public static void apply(PackSelectionModel model, PackProfile profile) {
        Set<String> wanted = new HashSet<>(profile.packIds());

        for (PackSelectionModel.Entry entry : model.getSelected().toList()) {
            if (!wanted.contains(entry.getId()) && entry.canUnselect()) {
                entry.unselect();
            }
        }

        for (String id : profile.packIds()) {
            // Re-queried each time: selecting a pack moves it between the model's two lists.
            model.getUnselected()
                    .filter(entry -> id.equals(entry.getId()))
                    .findFirst()
                    .filter(PackSelectionModel.Entry::canSelect)
                    .ifPresent(PackSelectionModel.Entry::select);
        }
    }

    // ------------------------------------------------------------------
    // The menu
    // ------------------------------------------------------------------

    /** The top-level profiles menu: save what is on now, or pick an existing profile. */
    public static void openMenu(PackSelectionModel model, PackListIntegration controller,
                                int mouseX, int mouseY) {
        PackProfileStore store = Folders.profiles();
        List<FolderContextMenu.Item> items = new ArrayList<>();

        items.add(FolderContextMenu.Item.of("folders.profile.save_current",
                () -> saveCurrent(model, controller)));

        if (store.isEmpty()) {
            items.add(FolderContextMenu.Item.of("folders.profile.none", () -> {
            }, false));
        } else {
            for (PackProfile profile : store.profiles()) {
                items.add(new FolderContextMenu.Item(
                        Component.literal(profile.name()),
                        () -> openProfileMenu(model, controller, profile, mouseX, mouseY),
                        true));
            }
        }
        controller.showMenu(items, mouseX, mouseY);
    }

    private static void openProfileMenu(PackSelectionModel model, PackListIntegration controller,
                                        PackProfile profile, int mouseX, int mouseY) {
        PackProfileStore store = Folders.profiles();
        controller.showMenu(List.of(
                new FolderContextMenu.Item(
                        Component.translatable("folders.profile.apply", profile.name()),
                        () -> apply(model, profile),
                        !profile.isEmpty()),
                FolderContextMenu.Item.of("folders.profile.overwrite", () -> {
                    store.update(profile.id(), currentSelection(model));
                    store.saveIfDirty();
                }),
                FolderContextMenu.Item.of("folders.profile.rename", () -> rename(profile)),
                FolderContextMenu.Item.of("folders.profile.delete", () -> {
                    store.remove(profile.id());
                    store.saveIfDirty();
                })
        ), mouseX, mouseY);
    }

    private static void saveCurrent(PackSelectionModel model, PackListIntegration controller) {
        PackProfileStore store = Folders.profiles();
        PackProfile profile = store.add(
                Component.translatable("folders.profile.default_name").getString(),
                currentSelection(model));
        store.saveIfDirty();
        // Straight into the name field, so a profile is never left called "Profile 4" by accident.
        rename(profile);
    }

    private static void rename(PackProfile profile) {
        PackProfileStore store = Folders.profiles();
        PackRenameBar.begin(profile.name(), "folders.profile.rename", name -> {
            if (store.rename(profile.id(), name)) {
                store.saveIfDirty();
            }
        });
    }
}
