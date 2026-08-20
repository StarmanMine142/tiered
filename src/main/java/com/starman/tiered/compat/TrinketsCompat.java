package com.starman.tiered.compat;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.*;

import java.util.*;

import dev.emi.trinkets.api.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;

public class TrinketsCompat {

    private static final Map<LivingEntity, Map<String, ItemStack>> LAST_TRINKET_ITEMS = new WeakHashMap<>();

    public static void tickTrinkets(LivingEntity livingEntity) {
        TrinketsApi.getTrinketComponent(livingEntity).ifPresent(component -> {
            Map<String, ItemStack> lastItems = LAST_TRINKET_ITEMS.computeIfAbsent(livingEntity, k -> new HashMap<>());

            component.forEach((slotReference, stack) -> {
                SlotType slotType = slotReference.inventory().getSlotType();

                String slotId = slotType.getId();
                int index = slotReference.index();

                String trackingKey = slotId + "/" + index;
                ItemStack lastItem = lastItems.getOrDefault(trackingKey, ItemStack.EMPTY);

                if (!ItemStack.matches(stack, lastItem)) {
                    applyOrRemoveTrinketAttributes(livingEntity, lastItem, slotType, false);
                    applyOrRemoveTrinketAttributes(livingEntity, stack, slotType, true);

                    lastItems.put(trackingKey, stack.copy());
                }
            });
        });
    }

    private static void applyOrRemoveTrinketAttributes(LivingEntity entity, ItemStack stack, SlotType slotType, boolean apply) {
        if (stack.isEmpty() || !Tiered.hasModifier(stack)) {
            return;
        }

        ResourceLocation tierId = stack.get(Tiered.MODIFIER);
        PotentialAttribute tier = Tiered.TIER_DATA.getTiers().get(tierId);

        if (tier != null) {
            tier.getAttributes().forEach(template -> {
                if (isTrinketSlotAllowed(template, slotType)) {
                    template.realize((attributeHolder, modifier) -> {
                        AttributeInstance instance = entity.getAttribute(attributeHolder);
                        if (instance != null) {
                            ResourceLocation uniqueId = ResourceLocation.fromNamespaceAndPath(
                                    Tiered.ID,
                                    modifier.id().getPath() + "_trinket_" + slotType.getGroup() + "_" + slotType.getName()
                            );

                            AttributeModifier cloneModifier = new AttributeModifier(
                                    uniqueId,
                                    modifier.amount(),
                                    modifier.operation()
                            );

                            instance.removeModifier(cloneModifier.id());
                            if (apply) {
                                instance.addTransientModifier(cloneModifier);
                            }
                        }
                    }, EquipmentSlot.MAINHAND);
                }
            });
        }
    }

    private static boolean isTrinketSlotAllowed(AttributeTemplate template, SlotType slotType) {
        String group = slotType.getGroup().toLowerCase(Locale.ROOT);
        String name = slotType.getName().toLowerCase(Locale.ROOT);
        String fullId = slotType.getId().toLowerCase(Locale.ROOT);

        var requiredGroups = template.getRequiredEquipmentSlot();
        if (requiredGroups != null && requiredGroups.length > 0) {
            boolean matchesRequired = false;
            for (var slotGroupEnum : requiredGroups) {
                String target = slotGroupEnum.toString().toLowerCase(Locale.ROOT);

                if (fullId.equals(target) || group.equals(target) || name.equals(target)) {
                    matchesRequired = true;
                    break;
                }
            }
            if (!matchesRequired) return false;
        }

        var optionalGroups = template.getOptionalEquipmentSlot();
        if (optionalGroups != null && optionalGroups.length > 0) {
            boolean matchesOptional = false;
            for (var slotGroupEnum : optionalGroups) {
                String target = slotGroupEnum.toString().toLowerCase(Locale.ROOT);

                if (fullId.equals(target) || group.equals(target) || name.equals(target)) {
                    matchesOptional = true;
                    break;
                }
            }
            if (!matchesOptional) return false;
        }

        return true;
    }
}