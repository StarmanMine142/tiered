package com.starman.tiered.data;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.TierPool;

import java.util.*;

import com.google.common.collect.Maps;
import com.google.gson.*;

import org.apache.logging.log4j.*;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.*;

public class PoolDataLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String PARSING_ERROR_MESSAGE = "Parsing error loading recipe {}";
    private static final String LOADED_RECIPES_MESSAGE = "Loaded {} item pools";
    private static final Logger LOGGER = LogManager.getLogger();

    private Map<ResourceLocation, TierPool> itemPools = new HashMap<>();

    public PoolDataLoader() {
        super(GSON, "tiered_modifiers/pools");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, TierPool> readItemPools = Maps.newHashMap();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation identifier = entry.getKey();

            try {
                TierPool itemPool = GSON.fromJson(entry.getValue(), TierPool.class);
                if (!itemPool.getTiers().isEmpty())
                    readItemPools.put(identifier, itemPool);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, identifier, exception);
            }
        }

        itemPools = readItemPools;
        LOGGER.info(LOADED_RECIPES_MESSAGE, readItemPools.size());
    }

    public Map<ResourceLocation, TierPool> getPools() {
        return itemPools;
    }
    public void clear() {
        itemPools.clear();
    }
    public void replace(Map<ResourceLocation, TierPool> i){
        itemPools = i;
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(Tiered.ID, "pool_data");
    }
}