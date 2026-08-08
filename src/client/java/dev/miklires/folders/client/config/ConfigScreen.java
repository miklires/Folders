package dev.miklires.folders.client.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Builds the YACL screen. One category; there is not enough here to warrant more. */
public final class ConfigScreen {

    private ConfigScreen() {
    }

    public static Screen create(Screen parent) {
        FoldersConfig defaults = new FoldersConfig();
        FoldersConfig config = FoldersConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("folders.settings.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("folders.settings.title"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("folders.settings.animations"))
                                .description(OptionDescription.of(
                                        Component.translatable("folders.settings.animations.description")))
                                .binding(defaults.animations,
                                        () -> config.animations,
                                        value -> config.animations = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("folders.settings.animation_speed"))
                                .binding(defaults.animationSpeed,
                                        () -> config.animationSpeed,
                                        value -> config.animationSpeed = value)
                                .controller(option -> FloatSliderControllerBuilder.create(option)
                                        .range(0.25f, 4.0f)
                                        .step(0.25f))
                                .available(config.animations)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("folders.settings.show_statistics"))
                                .description(OptionDescription.of(
                                        Component.translatable("folders.settings.show_statistics.description")))
                                .binding(defaults.showStatistics,
                                        () -> config.showStatistics,
                                        value -> config.showStatistics = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("folders.settings.show_online_indicator"))
                                .binding(defaults.showOnlineIndicator,
                                        () -> config.showOnlineIndicator,
                                        value -> config.showOnlineIndicator = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("folders.settings.show_tooltips"))
                                .binding(defaults.showTooltips,
                                        () -> config.showTooltips,
                                        value -> config.showTooltips = value)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .save(FoldersConfig::save)
                .build()
                .generateScreen(parent);
    }
}
