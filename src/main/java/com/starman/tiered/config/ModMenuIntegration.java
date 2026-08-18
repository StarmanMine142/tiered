package com.starman.tiered.config;

import com.terraformersmc.modmenu.api.*;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> TieredConfig.getPreferredScreen(parent);
	}
}