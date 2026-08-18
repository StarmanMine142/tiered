package com.starman.tiered.data;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.*;
import com.starman.tiered.gson.*;

import java.util.*;

import com.google.common.collect.Maps;
import com.google.gson.*;

import org.apache.logging.log4j.*;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class TierDataLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(AttributeModifier.class, new EntityAttributeModifierDeserializer())
            .registerTypeAdapter(AttributeModifier.class, new EntityAttributeModifierSerializer())
            .registerTypeAdapter(EquipmentSlotGroup.class, new EquipmentSlotDeserializer())
            .registerTypeHierarchyAdapter(Style.class, new StyleSerializer())
            .create();

    private static final String PARSING_ERROR_MESSAGE = "Parsing error loading recipe {}";
    private static final String LOADED_RECIPES_MESSAGE = "Loaded {} item tiers";
    private static final Logger LOGGER = LogManager.getLogger();

    private Map<ResourceLocation, PotentialAttribute> itemAttributes = new HashMap<>();

    public TierDataLoader() {
        super(GSON, "tiered_modifiers/tiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, PotentialAttribute> readItemAttributes = Maps.newHashMap();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation identifier = entry.getKey();

            try {
                PotentialAttribute itemAttribute = GSON.fromJson(entry.getValue(), PotentialAttribute.class);
                readItemAttributes.put(identifier, itemAttribute);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, identifier, exception);
            }
        }

        for (var attr : readItemAttributes.entrySet()) {
            List<AttributeTemplate> exists = new ArrayList<>();
            for (var att : attr.getValue().getUnfilteredAttributes()) {
                if (att.attributeExists(attr.getKey().toString())) exists.add(att);
            }
            attr.getValue().getAttributes().clear();
            attr.getValue().getAttributes().addAll(exists);
        }
        itemAttributes.clear();
        itemAttributes.putAll(readItemAttributes);
        LOGGER.info(LOADED_RECIPES_MESSAGE, readItemAttributes.size());
    }

    public Map<ResourceLocation, PotentialAttribute> getTiers() {
        return itemAttributes;
    }
    public void clear() {
        itemAttributes.clear();
    }
    public void replace(Map<ResourceLocation, PotentialAttribute> i){
        itemAttributes = i;
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(Tiered.ID, "data_loader");
    }
}