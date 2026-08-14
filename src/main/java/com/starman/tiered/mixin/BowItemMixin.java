package com.starman.tiered.mixin;

import com.starman.tiered.api.TieredAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BowItem.class)
public abstract class BowItemMixin extends ProjectileWeaponItem {

    public BowItemMixin(Item.Properties properties) {
        super(properties);
    }

    @Redirect(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"
            )
    )
    private static float tiered$redirectGetPowerForTime(int charge, ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        double speed = 1.0;
        if (entityLiving.getAttribute(TieredAttributes.DRAW_SPEED) != null) {
            speed = entityLiving.getAttributeValue(TieredAttributes.DRAW_SPEED);
        }

        float effectiveCharge = charge * (float) speed;

        float f = effectiveCharge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }

    @Redirect(
            method = "shootProjectile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"
            )
    )
    private void tiered$redirectShootFromRotation(Projectile projectile, Entity shooter, float x, float y, float z, float velocity, float inaccuracy) {
        float accuracy = 1.0F;
        float arrowVelocity = 1.0F;
        float arrowDamage = 0.0F;

        if (shooter instanceof LivingEntity livingEntity) {
            if (livingEntity.getAttribute(TieredAttributes.ACCURACY) != null) {
                accuracy = (float) livingEntity.getAttributeValue(TieredAttributes.ACCURACY);
            }

            if (livingEntity.getAttribute(TieredAttributes.ARROW_VELOCITY) != null) {
                arrowVelocity = (float) livingEntity.getAttributeValue(TieredAttributes.ARROW_VELOCITY);
            }

            if (livingEntity.getAttribute(TieredAttributes.ARROW_DAMAGE) != null) {
                arrowDamage = (float) livingEntity.getAttributeValue(TieredAttributes.ARROW_DAMAGE);
            }
        }

        projectile.shootFromRotation(shooter, x, y, z, velocity * arrowVelocity, 2.0F - accuracy * 2.0F);

        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() + arrowDamage);
        }
    }
}