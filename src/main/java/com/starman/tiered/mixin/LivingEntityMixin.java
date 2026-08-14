package com.starman.tiered.mixin;

import com.starman.tiered.api.TieredAttributes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow @Final private static EntityDataAccessor<Float> DATA_HEALTH_ID;

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

    @Redirect(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void trustOverflowHealth(LivingEntity livingEntity, float health) {
        this.entityData.set(DATA_HEALTH_ID, health);
    }
}