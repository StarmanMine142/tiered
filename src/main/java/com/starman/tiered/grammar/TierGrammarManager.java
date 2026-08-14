package com.starman.tiered.grammar;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TierGrammarManager implements SimpleSynchronousResourceReloadListener {
    private static final Map<String, Map<ResourceLocation, Map<Item, Integer>>> LOCALIZED_GRAMMAR_MAP = new HashMap<>();
    private static final Set<String> LOWERCASE_LOCALES = new HashSet<>();
    private static final Gson GSON = new Gson();

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath("tiered", "grammar_manager");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        LOCALIZED_GRAMMAR_MAP.clear();
        LOWERCASE_LOCALES.clear();

        resourceManager.listResources("lang", path -> path.getPath().endsWith("lowercase_langs.json")).forEach((location, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonArray array = GSON.fromJson(reader, JsonArray.class);
                if (array != null) {
                    for (JsonElement element : array) {
                        if (element.isJsonPrimitive()) {
                            LOWERCASE_LOCALES.add(element.getAsString().toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        resourceManager.listResources("lang/grammar", path -> path.getPath().endsWith(".json")).forEach((location, resource) -> {
            try {
                String pathStr = location.getPath();
                String[] parts = pathStr.split("/");
                if (parts.length >= 4 && parts[0].equals("lang") && parts[1].equals("grammar")) {
                    String locale = parts[2].toLowerCase();

                    StringBuilder tierPathBuilder = new StringBuilder();
                    for (int i = 3; i < parts.length; i++) {
                        tierPathBuilder.append(parts[i]);
                        if (i < parts.length - 1) {
                            tierPathBuilder.append("/");
                        }
                    }
                    String fullFileName = tierPathBuilder.toString();
                    String tierPath = fullFileName.substring(0, fullFileName.length() - 5);
                    ResourceLocation tierId = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), tierPath);

                    try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                        JsonArray array = GSON.fromJson(reader, JsonArray.class);
                        Map<Item, Integer> itemIndexMap = new HashMap<>();

                        if (array != null) {
                            for (JsonElement element : array) {
                                if (element.isJsonObject()) {
                                    JsonObject obj = element.getAsJsonObject();
                                    int index = obj.get("index").getAsInt();
                                    JsonArray itemsArray = obj.getAsJsonArray("items");

                                    if (itemsArray != null) {
                                        for (JsonElement itemElem : itemsArray) {
                                            ResourceLocation itemId = ResourceLocation.parse(itemElem.getAsString());
                                            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                                                Item item = BuiltInRegistries.ITEM.get(itemId);
                                                itemIndexMap.put(item, index);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LOCALIZED_GRAMMAR_MAP
                                .computeIfAbsent(locale, k -> new HashMap<>())
                                .put(tierId, itemIndexMap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean shouldLowercaseItemName() {
        String currentLocale = getCurrentLocale();
        return LOWERCASE_LOCALES.contains(currentLocale) ||
                LOWERCASE_LOCALES.contains(currentLocale.replace('-', '_')) ||
                LOWERCASE_LOCALES.contains(currentLocale.replace('_', '-'));
    }

    public static int getIndexFor(ResourceLocation tierId, Item item) {
        String currentLocale = getCurrentLocale();
        Map<Item, Integer> itemMap = null;

        String[] possibleLocales = {
                currentLocale,
                currentLocale.replace('-', '_'),
                currentLocale.replace('_', '-'),
                "en_us"
        };

        for (String locale : possibleLocales) {
            Map<ResourceLocation, Map<Item, Integer>> tierMap = LOCALIZED_GRAMMAR_MAP.get(locale);
            if (tierMap != null) {
                itemMap = tierMap.get(tierId);
                if (itemMap == null) {
                    for (Map.Entry<ResourceLocation, Map<Item, Integer>> entry : tierMap.entrySet()) {
                        ResourceLocation regId = entry.getKey();
                        if (regId.getPath().equals(tierId.getPath()) ||
                                regId.getPath().endsWith("/" + tierId.getPath())) {
                            itemMap = entry.getValue();
                            break;
                        }
                    }
                }
                if (itemMap != null) break;
            }
        }

        if (itemMap == null) {
            for (Map<ResourceLocation, Map<Item, Integer>> tierMap : LOCALIZED_GRAMMAR_MAP.values()) {
                for (Map.Entry<ResourceLocation, Map<Item, Integer>> entry : tierMap.entrySet()) {
                    ResourceLocation regId = entry.getKey();
                    if (regId.getPath().equals(tierId.getPath()) ||
                            regId.getPath().endsWith("/" + tierId.getPath())) {
                        itemMap = entry.getValue();
                        break;
                    }
                }
                if (itemMap != null) break;
            }
        }

        if (itemMap != null && itemMap.containsKey(item)) {
            return itemMap.get(item);
        }

        return 0;
    }

    private static String getCurrentLocale() {
        try {
            return Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase();
        } catch (Exception e) {
            return "en_us";
        }
    }
}