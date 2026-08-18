package com.starman.tiered.data;

import com.starman.tiered.Tiered;

import net.minecraft.core.Registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class TieredDataComponents {

    public static final DataComponentType<ResourceLocation> MODIFIER = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(Tiered.ID, "tiered_modifier"),
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static void register() {
        Tiered.LOGGER.info("Registering data components for " + Tiered.ID);
    }
}
