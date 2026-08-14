package com.starman.tiered.mixin;

import com.starman.tiered.api.TieredAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin extends ProjectileWeaponItem {

    public CrossbowItemMixin(Item.Properties properties) {
        super(properties);
    }

    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void tiered$modifyChargeDuration(ItemStack itemStack, LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
        double speed = 1.0;
        if (livingEntity.getAttribute(TieredAttributes.DRAW_SPEED) != null) {
            speed = livingEntity.getAttributeValue(TieredAttributes.DRAW_SPEED);
        }
        int originalTicks = cir.getReturnValue();
        int modifiedTicks = (int) (originalTicks / speed);
        cir.setReturnValue(Math.max(1, modifiedTicks));
    }

    @Redirect(
            method = "shootProjectile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"
            )
    )
    private void tiered$redirectShoot(Projectile projectile, double x, double y, double z, float velocity, float inaccuracy, LivingEntity livingEntity, Projectile targetProjectile, int i, float f, float g, float h, LivingEntity livingEntity2) {
        float accuracy = 1.0F;
        float arrowVelocity = 1.0F;
        float arrowDamage = 0.0F;

        if (livingEntity.getAttribute(TieredAttributes.ACCURACY) != null) {
            accuracy = (float) livingEntity.getAttributeValue(TieredAttributes.ACCURACY);
        }

        if (livingEntity.getAttribute(TieredAttributes.ARROW_VELOCITY) != null) {
            arrowVelocity = (float) livingEntity.getAttributeValue(TieredAttributes.ARROW_VELOCITY);
        }

        if (livingEntity.getAttribute(TieredAttributes.ARROW_DAMAGE) != null) {
            arrowDamage = (float) livingEntity.getAttributeValue(TieredAttributes.ARROW_DAMAGE);
        }

        projectile.shoot(x, y, z, velocity * arrowVelocity, 2.0F - accuracy * 2.0F);

        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() + arrowDamage);
        }
    }
}