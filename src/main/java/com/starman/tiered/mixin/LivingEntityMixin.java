package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.*;

import java.util.*;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow @Final private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Shadow public abstract double getAttributeValue(Holder<Attribute> attribute);

    @Unique
    private final Map<EquipmentSlot, ItemStack> tiered$lastEquippedItems = new EnumMap<>(EquipmentSlot.class);

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void tiered$addCustomAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue()
                .add(TieredAttributes.CRITICAL_RATE, 0.0)
                .add(TieredAttributes.CRITICAL_DAMAGE, 1.5)
                .add(TieredAttributes.DRAW_SPEED, 1.0)
                .add(TieredAttributes.ACCURACY, 0.5)
                .add(TieredAttributes.ARROW_VELOCITY, 1.0)
                .add(TieredAttributes.ARROW_DAMAGE, 0.0);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void tiered$onLivingTick(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;

        if (livingEntity.level().isClientSide()) {
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
            ItemStack currentItem = livingEntity.getItemBySlot(slot);
            ItemStack lastItem = tiered$lastEquippedItems.getOrDefault(slot, ItemStack.EMPTY);

            if (!ItemStack.matches(currentItem, lastItem)) {
                tiered$applyOrRemoveTierAttributes(livingEntity, lastItem, slot, false);
                tiered$applyOrRemoveTierAttributes(livingEntity, currentItem, slot, true);

                tiered$lastEquippedItems.put(slot, currentItem.copy());
            }
        }
    }

    @Unique
    private void tiered$applyOrRemoveTierAttributes(LivingEntity entity, ItemStack stack, EquipmentSlot slot, boolean apply) {
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
                        AttributeInstance instance = entity.getAttribute(attributeHolder);
                        if (instance != null) {
                            if (apply) {
                                instance.removeModifier(modifier.id());
                                instance.addTransientModifier(modifier);
                            } else {
                                instance.removeModifier(modifier.id());
                            }
                        }
                    }, slot);
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
            method = "hurt",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float tiered$applyLivingCrit(float amount, DamageSource source) {
        LivingEntity target = (LivingEntity) (Object) this;

        if (target.level().isClientSide()) {
            return amount;
        }

        if (source.getEntity() instanceof LivingEntity attacker) {
            double critRate = attacker.getAttributeValue(TieredAttributes.CRITICAL_RATE);

            if (critRate > 0.0 && attacker.getRandom().nextDouble() < critRate) {
                double critDamage = attacker.getAttributeValue(TieredAttributes.CRITICAL_DAMAGE);
                if (critDamage > 0.0) {
                    return (float) (amount * critDamage);
                }
            }
        }

        return amount;
    }

    @Redirect(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void trustOverflowHealth(LivingEntity livingEntity, float health) {
        this.entityData.set(DATA_HEALTH_ID, health);
    }
}