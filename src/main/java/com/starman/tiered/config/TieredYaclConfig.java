package com.starman.tiered.config;

import net.fabricmc.loader.api.FabricLoader;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TieredYaclConfig {

    public enum ConfigType implements NameableEnum {
        VANILLA("vanilla", "config.tiered.type.vanilla"),
        CLOTH("cloth", "config.tiered.type.cloth"),
        YACL("yacl", "config.tiered.type.yacl"),
        NONE("none", "config.tiered.type.none");

        private final String id;
        private final String translationKey;

        ConfigType(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public String getId() {
            return id;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }

        public static ConfigType fromString(String id) {
            for (ConfigType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return NONE;
        }
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("gui.tiered.config"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("gui.tiered.config"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.tiered.enable_reforge_experience_cost"))
                                .description(OptionDescription.of(Component.translatable("config.tiered.enable_reforge_experience_cost.comment")))
                                .binding(true, () -> TieredConfig.enableReforgeExpCost, newVal -> TieredConfig.enableReforgeExpCost = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<ConfigType>createBuilder()
                                .name(Component.translatable("config.tiered.selector_mode"))
                                .description(OptionDescription.of(Component.translatable("config.tiered.selector_mode.tooltip")))
                                .binding(
                                        ConfigType.NONE,
                                        () -> {
                                            ConfigType type = ConfigType.fromString(TieredConfig.selectedConfigType);
                                            if ((type == ConfigType.CLOTH && !FabricLoader.getInstance().isModLoaded("cloth-config2")) ||
                                                    (type == ConfigType.YACL && !FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3"))) {
                                                return ConfigType.NONE;
                                            }
                                            return type;
                                        },
                                        newVal -> {
                                            String newValue = newVal.getId();
                                            if (newValue.equals("cloth") && !FabricLoader.getInstance().isModLoaded("cloth-config2")) return;
                                            if (newValue.equals("yacl") && !FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) return;

                                            TieredConfig.selectedConfigType = newValue;
                                            if (newValue.equals("none")) {
                                                TieredConfig.resetSelection();
                                            } else {
                                                TieredConfig.save();
                                            }
                                        }
                                )
                                .controller(opt -> EnumControllerBuilder.create(opt)
                                        .enumClass(ConfigType.class)
                                )
                                .build())

                        .build())
                .save(TieredConfig::save)
                .build()
                .generateScreen(parent);
    }
}