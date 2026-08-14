package com.starman.tiered.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TieredConfig {
	public static boolean enableReforgeExpCost = true;

	private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "tiered.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void load() {
		if (FILE.exists()) {
			try (FileReader reader = new FileReader(FILE)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				if (json != null && json.has("enableReforgeExpCost")) {
					enableReforgeExpCost = json.get("enableReforgeExpCost").getAsBoolean();
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

			try (FileWriter writer = new FileWriter(FILE)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}