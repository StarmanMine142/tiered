package com.starman.tiered.util;

import com.starman.tiered.Tiered;
import com.starman.tiered.data.TieredDataComponents;
import com.starman.tiered.api.*;

import java.util.*;
import java.util.function.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

public class TierHelper {

    public static ResourceLocation getKey(PotentialAttribute tier) {
        return Tiered.TIER_DATA.getTiers().entrySet().stream()
                .filter(entry -> tier.equals(entry.getValue()))
                .map(Map.Entry::getKey).findFirst().get();
    }

    public static ResourceLocation getTier(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.get(TieredDataComponents.MODIFIER);
    }

    public static boolean hasModifier(ItemStack stack) {
        ResourceLocation tier = getTier(stack);
        return tier != null && !tier.equals(ModifierUtils.BLANK) && Tiered.TIER_DATA.getTiers().containsKey(tier);
    }

    public static void attemptToAffixTier(ItemStack stack) {
        if (!hasModifier(stack) && !stack.isEmpty()) {
            ResourceLocation potentialAttributeID = ModifierUtils.getRandomAttributeIDFor(stack.getItem());
            if (potentialAttributeID != ModifierUtils.BLANK) {
                stack.set(TieredDataComponents.MODIFIER, potentialAttributeID);
            }
        }
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