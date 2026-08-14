package com.starman.tiered.mixin;

import com.starman.tiered.api.TieredAttributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public class PlayerMixin {

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