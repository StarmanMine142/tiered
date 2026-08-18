package com.starman.tiered.config;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;

import me.shedaniel.clothconfig2.api.*;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TieredClothConfig {
	public static Screen create(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("gui.tiered.config"));

		builder.setSavingRunnable(TieredConfig::save);

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.tiered.category.general"));

		general.addEntry(entryBuilder.startBooleanToggle(
						Component.translatable("config.tiered.enable_reforge_experience_cost"), TieredConfig.enableReforgeExpCost)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.tiered.enable_reforge_experience_cost.comment"))
				.setSaveConsumer(val -> TieredConfig.enableReforgeExpCost = val)
				.build());

		List<String> optionsList = new ArrayList<>();
		optionsList.add("vanilla");
		if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
			optionsList.add("cloth");
		}
		if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
			optionsList.add("yacl");
		}
		optionsList.add("none");
		String[] options = optionsList.toArray(new String[0]);

		String currentSelection = TieredConfig.selectedConfigType;
		if (!optionsList.contains(currentSelection)) {
			currentSelection = "none";
			TieredConfig.resetSelection();
		}

		general.addEntry(entryBuilder.startSelector(
						Component.translatable("config.tiered.selector_mode"),
						options,
						currentSelection
				)
				.setDefaultValue("none")
				.setNameProvider(value -> Component.translatable("config.tiered.type." + value))
				.setTooltip(Component.translatable("config.tiered.selector_mode.tooltip"))
				.setSaveConsumer(newValue -> {
					TieredConfig.selectedConfigType = newValue;
					if (newValue.equals("none")) {
						TieredConfig.resetSelection();
					} else {
						TieredConfig.save();
					}
				})
				.build());

		return builder.build();
	}
}