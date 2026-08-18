package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.AttributeTemplate;
import com.starman.tiered.api.PotentialAttribute;
import com.starman.tiered.api.TieredAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Mixin(Player.class)
public class PlayerMixin {

    @Unique
    private final Map<EquipmentSlot, ItemStack> tiered$lastEquippedItems = new HashMap<>();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void tiered$onPlayerTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide()) {
            return;
        }

        EquipmentSlot[] monitoredSlots = new EquipmentSlot[]{
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : monitoredSlots) {
            ItemStack currentItem = player.getItemBySlot(slot);
            ItemStack lastItem = tiered$lastEquippedItems.getOrDefault(slot, ItemStack.EMPTY);

            if (!ItemStack.matches(currentItem, lastItem)) {
                tiered$applyOrRemoveTierAttributes(player, lastItem, slot, false);
                tiered$applyOrRemoveTierAttributes(player, currentItem, slot, true);

                tiered$lastEquippedItems.put(slot, currentItem.copy());
            }
        }
    }

    @Unique
    private void tiered$applyOrRemoveTierAttributes(Player player, ItemStack stack, EquipmentSlot slot, boolean apply) {
        if (stack.isEmpty() || !Tiered.hasModifier(stack)) {
            return;
        }

        var tierId = stack.get(Tiered.MODIFIER);
        PotentialAttribute tier = Tiered.TIER_DATA.getTiers().get(tierId);

        if (tier != null) {
            tier.getAttributes().forEach(template -> {
                if (tiered$isSlotAllowed(template, slot)) {
                    EquipmentSlotGroup slotGroup = switch (slot) {
                        case MAINHAND -> EquipmentSlotGroup.MAINHAND;
                        case OFFHAND -> EquipmentSlotGroup.OFFHAND;
                        case HEAD, CHEST, LEGS, FEET -> EquipmentSlotGroup.ARMOR;
                        default -> EquipmentSlotGroup.ANY;
                    };

                    template.realize((attributeHolder, modifier) -> {
                        AttributeInstance instance = player.getAttribute(attributeHolder);
                        if (instance != null) {
                            if (apply) {
                                instance.removeModifier(modifier.id());
                                instance.addTransientModifier(modifier);
                            } else {
                                instance.removeModifier(modifier.id());
                            }
                        }
                    }, slotGroup);
                }
            });
        }
    }

    @Unique
    private boolean tiered$isSlotAllowed(AttributeTemplate template, EquipmentSlot slot) {
        EquipmentSlot[] requiredSlots = template.getRequiredLiteralEquipmentSlot();
        if (requiredSlots != null && requiredSlots.length > 0) {
            boolean matched = Arrays.asList(requiredSlots).contains(slot);
            if (!matched) return false;
        }

        EquipmentSlot[] optionalSlots = template.getOptionalLiteralEquipmentSlot();
        if (optionalSlots != null && optionalSlots.length > 0) {
            return Arrays.asList(optionalSlots).contains(slot);
        }

        return true;
    }

    @ModifyVariable(
            method = "attack",
            at = @At(
                    value = "LOAD",
                    ordinal = 0
            ),
            name = "bl3",
            ordinal = 2
    )
    private boolean tiered$attackCritRate(boolean bl3) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide()) {
            return bl3;
        }

        double critRate = player.getAttributeValue(TieredAttributes.CRITICAL_RATE);
        if (critRate > 0.0 && !bl3 && player.getRandom().nextDouble() < critRate) {
            return true;
        }

        return bl3;
    }

    @ModifyConstant(
            method = "attack",
            constant = @Constant(floatValue = 1.5F)
    )
    private float tiered$modifyCritDamage(float f) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide()) {
            return f;
        }

        return (float) player.getAttributeValue(TieredAttributes.CRITICAL_DAMAGE);
    }
}