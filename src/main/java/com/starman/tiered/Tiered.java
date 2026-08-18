package com.starman.tiered;

import com.starman.tiered.item.TieredItems;
import com.starman.tiered.util.TierTickHandler;
import com.starman.tiered.api.TieredAttributes;
import com.starman.tiered.config.TieredConfig;
import com.starman.tiered.network.TierNetwork;
import com.starman.tiered.data.*;

import org.apache.logging.log4j.*;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.api.ModInitializer;

import net.minecraft.server.packs.PackType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class Tiered implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "tiered";

    @Override
    public void onInitialize() {
        TieredConfig.load();
        TierNetwork.register();
        TierTickHandler.register();
        TieredDataComponents.register();
        TieredItems.register();
        TieredAttributes.register();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TIER_DATA);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(POOL_DATA);
    }

    public static final TierDataLoader TIER_DATA = new TierDataLoader();
    public static final PoolDataLoader POOL_DATA = new PoolDataLoader();

    public static final ResourceLocation[] MODIFIERS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(ID, "any"),
            ResourceLocation.fromNamespaceAndPath(ID, "mainhand"),
            ResourceLocation.fromNamespaceAndPath(ID, "offhand"),
            ResourceLocation.fromNamespaceAndPath(ID, "hand"),
            ResourceLocation.fromNamespaceAndPath(ID, "boots"),
            ResourceLocation.fromNamespaceAndPath(ID, "leggings"),
            ResourceLocation.fromNamespaceAndPath(ID, "chestplates"),
            ResourceLocation.fromNamespaceAndPath(ID, "helmets"),
            ResourceLocation.fromNamespaceAndPath(ID, "armor"),
            ResourceLocation.fromNamespaceAndPath(ID, "body"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory1"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory2"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory3"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory4"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory5"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory6"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory7"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory8"),
            ResourceLocation.fromNamespaceAndPath(ID, "accessory9"),
            ResourceLocation.fromNamespaceAndPath(ID, "necklaces"),
            ResourceLocation.fromNamespaceAndPath(ID, "backs"),
            ResourceLocation.fromNamespaceAndPath(ID, "rings")
    };

    public static final DataComponentType<ResourceLocation> MODIFIER = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(ID, "tiered_modifier"),
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}