package com.starman.tiered;

import com.starman.tiered.config.TieredConfig;
import com.starman.tiered.item.TieredItems;
import com.starman.tiered.network.ClientboundTierSyncerPacket;
import com.starman.tiered.data.*;
import com.starman.tiered.api.*;

import java.util.*;
import java.util.function.*;

import org.apache.logging.log4j.*;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

public class Tiered implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "tiered";

    @Override
    public void onInitialize() {
        TieredConfig.load();
        TieredAttributes.register();
        TieredItems.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 10 == 0) {
                    for (ItemStack stack : player.containerMenu.getItems()) {
                        if (!stack.isEmpty()) {
                            Tiered.attemptToAffixTier(stack);
                        }
                    }
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player.level().isClientSide) return;
            ServerPlayNetworking.send(handler.player, new ClientboundTierSyncerPacket(TIER_DATA.getTiers()));
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TIER_DATA);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(POOL_DATA);

        PayloadTypeRegistry.playS2C().register(ClientboundTierSyncerPacket.TYPE, ClientboundTierSyncerPacket.STREAM_CODEC);
    }

    public static final TierDataLoader TIER_DATA = new TierDataLoader();
    public static final PoolDataLoader POOL_DATA = new PoolDataLoader();

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

    public static final DataComponentType<ResourceLocation> MODIFIER = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(ID, "tiered_modifier"),
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static ResourceLocation getTier(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.get(MODIFIER);
    }

    public static ResourceLocation getKey(PotentialAttribute tier) {
        return TIER_DATA.getTiers().entrySet().stream()
                .filter(entry -> tier.equals(entry.getValue()))
                .map(Map.Entry::getKey).findFirst().get();
    }

    public static boolean hasModifier(ItemStack stack) {
        ResourceLocation tier = getTier(stack);
        return tier != null && !tier.equals(ModifierUtils.BLANK) && TIER_DATA.getTiers().containsKey(tier);
    }

    public static void attemptToAffixTier(ItemStack stack) {
        if (!stack.isEmpty()) {
            ResourceLocation tier = getTier(stack);

            if (tier == null || tier.equals(ModifierUtils.BLANK) || !TIER_DATA.getTiers().containsKey(tier)) {
                ResourceLocation potentialAttributeID = ModifierUtils.getRandomAttributeIDFor(stack.getItem());
                if (potentialAttributeID != ModifierUtils.BLANK) {
                    stack.set(MODIFIER, potentialAttributeID);
                    applyModifiersToItemStack(stack, potentialAttributeID);
                }
            } else {
                var currentModifiers = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
                boolean hasTierModifiers = currentModifiers != null && currentModifiers.modifiers().stream()
                        .anyMatch(entry -> entry.modifier().id().getNamespace().equals(ID));

                if (!hasTierModifiers) {
                    applyModifiersToItemStack(stack, tier);
                }
            }
        }
    }

    private static void applyModifiersToItemStack(ItemStack stack, ResourceLocation tierId) {
        PotentialAttribute potentialAttribute = TIER_DATA.getTiers().get(tierId);
        if (potentialAttribute == null) return;

        net.minecraft.world.item.component.ItemAttributeModifiers defaultModifiers = stack.getItem().components().get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);

        net.minecraft.world.item.component.ItemAttributeModifiers.Builder modifierBuilder =
                net.minecraft.world.item.component.ItemAttributeModifiers.builder();

        if (defaultModifiers != null) {
            defaultModifiers.modifiers().forEach(entry ->
                    modifierBuilder.add(entry.attribute(), entry.modifier(), entry.slot())
            );
        }

        potentialAttribute.getAttributes().forEach(template -> {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                boolean isPreferred = isPreferredEquipmentSlot(stack, slot);

                boolean matches = false;
                if (template.getRequiredEquipmentSlot() != null) {
                    for (EquipmentSlotGroup group : template.getRequiredEquipmentSlot()) {
                        if (group.test(slot)) { matches = true; break; }
                    }
                }
                if (!matches && template.getOptionalEquipmentSlot() != null && isPreferred) {
                    for (EquipmentSlotGroup group : template.getOptionalEquipmentSlot()) {
                        if (group.test(slot)) { matches = true; break; }
                    }
                }

                if (matches) {
                    template.realizeForComponent((holder, modifier) -> {
                        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
                        modifierBuilder.add(holder, modifier, group);
                    }, slot);
                }
            }
        });

        stack.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, modifierBuilder.build());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlotGroup slot) {
        if (stack.getItem() instanceof ShieldItem) {
            return slot.test(EquipmentSlot.MAINHAND) || slot.test(EquipmentSlot.OFFHAND);
        }
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return slot.test(armorItem.getEquipmentSlot());
        }
        return slot.test(EquipmentSlot.MAINHAND);
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof ShieldItem) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return slot == armorItem.getEquipmentSlot();
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