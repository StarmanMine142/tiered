package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    @Inject(method = "sendAllDataToRemote", at = @At("HEAD"))
    private void tiered$onMenuOpen(CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        for (ItemStack stack : menu.getItems()) {
            Tiered.attemptToAffixTier(stack);
        }
    }
}