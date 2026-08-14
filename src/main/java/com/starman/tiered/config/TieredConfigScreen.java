package com.starman.tiered.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TieredConfigScreen {
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

		return builder.build();
	}
}