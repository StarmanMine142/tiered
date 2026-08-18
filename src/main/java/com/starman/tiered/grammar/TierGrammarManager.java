package com.starman.tiered.grammar;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.gson.*;

import com.starman.tiered.Tiered;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

public class TierGrammarManager implements SimpleSynchronousResourceReloadListener {

    public static class GrammarEntry {
        private final int index;
        private final Set<Item> items = new HashSet<>();
        private final Set<TagKey<Item>> tags = new HashSet<>();
        private final Set<Item> excludedItems = new HashSet<>();
        private final Set<TagKey<Item>> excludedTags = new HashSet<>();

        public GrammarEntry(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public boolean matches(Item item) {
            ItemStack stack = new ItemStack(item);

            if (excludedItems.contains(item)) return false;
            for (TagKey<Item> tag : excludedTags) {
                if (stack.is(tag)) return false;
            }

            if (items.contains(item)) return true;
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final Map<String, Map<ResourceLocation, List<GrammarEntry>>> LOCALIZED_GRAMMAR_MAP = new HashMap<>();
    private static final Set<String> LOWERCASE_LOCALES = new HashSet<>();
    private static final Gson GSON = new Gson();

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(Tiered.ID, "grammar_manager");
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
                        List<GrammarEntry> grammarEntries = new ArrayList<>();

                        if (array != null) {
                            int autoIndex = 0;
                            for (JsonElement element : array) {
                                if (element.isJsonObject()) {
                                    JsonObject obj = element.getAsJsonObject();

                                    int index;
                                    if (obj.has("index")) {
                                        index = obj.get("index").getAsInt();
                                        autoIndex = index;
                                    } else {
                                        index = autoIndex;
                                    }

                                    JsonArray itemsArray = obj.getAsJsonArray("items");

                                    GrammarEntry entry = new GrammarEntry(index);

                                    if (itemsArray != null) {
                                        for (JsonElement itemElem : itemsArray) {
                                            String rawVal = itemElem.getAsString();
                                            boolean isExcluded = rawVal.startsWith("-");
                                            String idOrTag = isExcluded ? rawVal.substring(1) : rawVal;

                                            if (idOrTag.startsWith("#")) {
                                                ResourceLocation tagLocation = ResourceLocation.parse(idOrTag.substring(1));
                                                TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagLocation);
                                                if (isExcluded) entry.excludedTags.add(itemTag);
                                                else entry.tags.add(itemTag);
                                            } else {
                                                ResourceLocation itemId = ResourceLocation.parse(idOrTag);
                                                if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                                                    Item item = BuiltInRegistries.ITEM.get(itemId);
                                                    if (isExcluded) entry.excludedItems.add(item);
                                                    else entry.items.add(item);
                                                }
                                            }
                                        }
                                    }
                                    grammarEntries.add(entry);

                                    if (!obj.has("index")) {
                                        autoIndex++;
                                    }
                                }
                            }
                        }

                        LOCALIZED_GRAMMAR_MAP
                                .computeIfAbsent(locale, k -> new HashMap<>())
                                .put(tierId, grammarEntries);
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
        List<GrammarEntry> entries = null;

        String[] possibleLocales = {
                currentLocale,
                currentLocale.replace('-', '_'),
                currentLocale.replace('_', '-'),
                "en_us"
        };

        for (String locale : possibleLocales) {
            Map<ResourceLocation, List<GrammarEntry>> tierMap = LOCALIZED_GRAMMAR_MAP.get(locale);
            if (tierMap != null) {
                entries = tierMap.get(tierId);
                if (entries == null) {
                    for (Map.Entry<ResourceLocation, List<GrammarEntry>> entry : tierMap.entrySet()) {
                        ResourceLocation regId = entry.getKey();
                        if (regId.getPath().equals(tierId.getPath()) ||
                                regId.getPath().endsWith("/" + tierId.getPath())) {
                            entries = entry.getValue();
                            break;
                        }
                    }
                }
                if (entries != null) break;
            }
        }

        if (entries == null) {
            for (Map<ResourceLocation, List<GrammarEntry>> tierMap : LOCALIZED_GRAMMAR_MAP.values()) {
                for (Map.Entry<ResourceLocation, List<GrammarEntry>> entry : tierMap.entrySet()) {
                    ResourceLocation regId = entry.getKey();
                    if (regId.getPath().equals(tierId.getPath()) ||
                            regId.getPath().endsWith("/" + tierId.getPath())) {
                        entries = entry.getValue();
                        break;
                    }
                }
                if (entries != null) break;
            }
        }

        if (entries != null) {
            for (GrammarEntry entry : entries) {
                if (entry.matches(item)) {
                    return entry.getIndex();
                }
            }
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