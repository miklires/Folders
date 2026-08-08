package dev.miklires.folders.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.miklires.folders.client.config.ConfigScreen;

/** Puts the settings behind the Mod Menu cog. The mod adds no menu of its own. */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }
}
