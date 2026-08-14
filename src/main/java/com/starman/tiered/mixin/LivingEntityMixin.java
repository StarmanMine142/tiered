package com.starman.tiered.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow @Final private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void trustOverflowHealth(LivingEntity livingEntity, float health) {
        this.entityData.set(DATA_HEALTH_ID, health);
    }
}