package com.starman.tiered.item;

import com.starman.tiered.Tiered;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public class TieredItems {
    public static final Item SMITHING_HAMMER = registerItem("smithing_hammer", new Item(new Item.Properties().durability(20)));
    public static final Item TIER_DEBUG_ITEM = registerItem("tier_debug_item", new Item(new Item.Properties().rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(Tiered.ID, name), item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.addAfter(Items.BRUSH, SMITHING_HAMMER);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(content -> {
            content.addAfter(Items.DEBUG_STICK, TIER_DEBUG_ITEM);
        });
    }
}