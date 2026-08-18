package com.starman.tiered.config;

import java.io.*;

import com.google.gson.*;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.gui.screens.Screen;

public class TieredConfig {
	public static boolean enableReforgeExpCost = true;
	public static String selectedConfigType = "none";

	private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "tiered.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


	public static void load() {
		if (FILE.exists()) {
			try (FileReader reader = new FileReader(FILE)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				if (json != null) {
					if (json.has("enableReforgeExpCost")) {
						enableReforgeExpCost = json.get("enableReforgeExpCost").getAsBoolean();
					}
					if (json.has("selectedConfigType")) {
						selectedConfigType = json.get("selectedConfigType").getAsString();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			save();
		}
	}

	public static void save() {
		try {
			JsonObject json = new JsonObject();
			json.addProperty("enableReforgeExpCost", enableReforgeExpCost);
			json.addProperty("selectedConfigType", selectedConfigType);

			try (FileWriter writer = new FileWriter(FILE)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void resetSelection() {
		selectedConfigType = "none";
		save();
	}

	public static Screen getPreferredScreen(Screen parent) {
		load();

		if (selectedConfigType.equals("none") || !isConfigTypeAvailable(selectedConfigType)) {
			return new TieredSelectorScreen(parent);
		}

		return switch (selectedConfigType) {
			case "cloth" -> TieredClothConfig.create(parent);
			case "yacl" -> TieredYaclConfig.create(parent);
			default -> TieredVanillaConfig.create(parent);
		};
	}

	private static boolean isConfigTypeAvailable(String type) {
		return switch (type) {
			case "cloth" -> FabricLoader.getInstance().isModLoaded("cloth-config2");
			case "yacl" -> FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");
			default -> true;
		};
	}
}