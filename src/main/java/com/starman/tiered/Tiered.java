package com.starman.tiered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.starman.tiered.api.TieredAttributes;
import com.starman.tiered.config.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.starman.tiered.api.AttributeTemplate;
import com.starman.tiered.api.ModifierUtils;
import com.starman.tiered.api.PotentialAttribute;
import com.starman.tiered.data.PoolDataLoader;
import com.starman.tiered.data.TierDataLoader;
import com.starman.tiered.network.protocol.game.ClientboundTierSyncerPacket;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;

public class Tiered implements ModInitializer {

    public static final TierDataLoader TIER_DATA = new TierDataLoader();
    public static final PoolDataLoader POOL_DATA = new PoolDataLoader();

    public static ResourceLocation getKey(PotentialAttribute tier) {
        return TIER_DATA.getTiers().entrySet().stream()
                .filter(entry -> tier.equals(entry.getValue()))
                .map(Map.Entry::getKey).findFirst().get();
    }

    public static final ResourceLocation[] MODIFIERS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath("tiered", "any"),
            ResourceLocation.fromNamespaceAndPath("tiered", "mainhand"),
            ResourceLocation.fromNamespaceAndPath("tiered", "offhand"),
            ResourceLocation.fromNamespaceAndPath("tiered", "hand"),
            ResourceLocation.fromNamespaceAndPath("tiered", "boots"),
            ResourceLocation.fromNamespaceAndPath("tiered", "leggings"),
            ResourceLocation.fromNamespaceAndPath("tiered", "chestplates"),
            ResourceLocation.fromNamespaceAndPath("tiered", "helmets"),
            ResourceLocation.fromNamespaceAndPath("tiered", "armor"),
            ResourceLocation.fromNamespaceAndPath("tiered", "body"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory1"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory2"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory3"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory4"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory5"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory6"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory7"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory8"),
            ResourceLocation.fromNamespaceAndPath("tiered", "accessory9"),
            ResourceLocation.fromNamespaceAndPath("tiered", "necklaces"),
            ResourceLocation.fromNamespaceAndPath("tiered", "backs"),
            ResourceLocation.fromNamespaceAndPath("tiered", "rings")
    };

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "tiered";
    public static Tiered instance;

    public static final DataComponentType<ResourceLocation> MODIFIER = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(ID, "reforged_modifier"),
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static final Item SMITHING_HAMMER = registerItem("smithing_hammer", new Item(new Item.Properties().durability(20)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(ID, name), item);
    }

    @Override
    public void onInitialize() {
        TieredConfig.load();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TIER_DATA);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(POOL_DATA);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.addAfter(Items.BRUSH, SMITHING_HAMMER);
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 20 == 0) {
                    for (ItemStack stack : player.getInventory().items) {
                        attemptToAffixTier(stack);
                    }
                }
            }
        });

        TieredAttributes.registerAttributes();

        instance = this;

        PayloadTypeRegistry.playS2C().register(ClientboundTierSyncerPacket.TYPE, ClientboundTierSyncerPacket.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player.level().isClientSide) return;
            ServerPlayNetworking.send(handler.player, new ClientboundTierSyncerPacket(TIER_DATA.getTiers()));
        });
    }

    public static ResourceLocation getTier(ItemStack stack) {
        ResourceLocation id = stack.get(MODIFIER);
        if (id == null) return null;

        if ("reforged".equals(id.getNamespace())) {
            ResourceLocation fixed = ResourceLocation.fromNamespaceAndPath(ID, id.getPath());
            stack.set(MODIFIER, fixed);
            return fixed;
        }
        return id;
    }

    public static boolean hasModifier(ItemStack stack) {
        ResourceLocation tier = getTier(stack);
        return tier != null && !tier.equals(ModifierUtils.BLANK);
    }

    public static void attemptToAffixTier(ItemStack stack) {
        if (!hasModifier(stack) && !stack.isEmpty()) {
            ResourceLocation potentialAttributeID = ModifierUtils.getRandomAttributeIDFor(stack.getItem());
            if (potentialAttributeID != ModifierUtils.BLANK) {
                stack.set(MODIFIER, potentialAttributeID);
            }
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlotGroup slot) {
        if (stack.getItem() instanceof ShieldItem) {
            return slot.test(EquipmentSlot.MAINHAND) || slot.test(EquipmentSlot.OFFHAND);
        }
        return slot.test(EquipmentSlot.MAINHAND);
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof ShieldItem) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }
        return slot == EquipmentSlot.MAINHAND;
    }

    public static <T> void AppendAttributesToOriginal(ItemStack stack, T slot, boolean isPreferredSlot, String customAttributes,
                                                      Function<AttributeTemplate, T[]> requiredSlotsArray,
                                                      Function<AttributeTemplate, T[]> optionalSlotsArray, Consumer<AttributeTemplate> realize) {
        if (hasModifier(stack)) {
            ResourceLocation tier = getTier(stack);
            PotentialAttribute potentialAttribute = Tiered.TIER_DATA.getTiers().get(tier);

            if (potentialAttribute != null) {
                potentialAttribute.getAttributes().forEach(template -> {
                    if (requiredSlotsArray.apply(template) != null) {
                        List<T> requiredSlots = new ArrayList<>(Arrays.asList(requiredSlotsArray.apply(template)));
                        if (requiredSlots.contains(slot))
                            realize.accept(template);
                    }

                    if (optionalSlotsArray.apply(template) != null) {
                        List<T> optionalSlots = new ArrayList<>(Arrays.asList(optionalSlotsArray.apply(template)));
                        if (optionalSlots.contains(slot) && isPreferredSlot)
                            realize.accept(template);
                    }
                });
            }
        }
    }
}