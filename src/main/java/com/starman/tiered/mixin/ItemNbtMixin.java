package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(ItemStack.class)
public class ItemNbtMixin {

    private static final Set<String> LEGACY_HAMMERS = Set.of(
            "tiered:weaponsmiths_hammer",
            "tiered:toolsmiths_hammer",
            "tiered:armorers_hammer",
            "reforged:weaponsmiths_hammer",
            "reforged:toolsmiths_hammer",
            "reforged:armorers_hammer"
    );

    @Inject(method = "parse", at = @At("RETURN"), cancellable = true)
    private static void tiered$fixOldHammerId(HolderLookup.Provider provider, Tag tag, CallbackInfoReturnable<Optional<ItemStack>> cir) {
        Optional<ItemStack> optionalStack = cir.getReturnValue();
        if (optionalStack.isPresent() && !optionalStack.get().isEmpty()) {
            ItemStack stack = optionalStack.get();
            if (tag instanceof CompoundTag compoundTag && compoundTag.contains("id")) {
                String itemId = compoundTag.getString("id");
                if (LEGACY_HAMMERS.contains(itemId)) {
                    ItemStack newStack = new ItemStack(Tiered.SMITHING_HAMMER, stack.getCount());
                    cir.setReturnValue(Optional.of(newStack));
                }
            }
        }
    }
}