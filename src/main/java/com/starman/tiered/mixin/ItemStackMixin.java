package com.starman.tiered.mixin;

import com.starman.tiered.item.TieredItems;
import com.starman.tiered.util.TierHelper;

import java.util.*;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	@Unique
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
				if (LEGACY_HAMMERS.contains(compoundTag.getString("id"))) {
					cir.setReturnValue(Optional.of(new ItemStack(TieredItems.SMITHING_HAMMER, stack.getCount())));
				}
			}
		}
	}

	@ModifyVariable(method = "parse", at = @At("HEAD"), argsOnly = true)
	private static Tag tiered$convertLegacyData(Tag tag) {
		if (!(tag instanceof CompoundTag compoundTag)) return tag;
		CompoundTag copyTag = compoundTag.copy();

		if (copyTag.contains("components", Tag.TAG_COMPOUND)) {
			CompoundTag components = copyTag.getCompound("components");

			if (components.contains("reforged:reforged_modifier")) {
				Tag modifierTag = components.get("reforged:reforged_modifier");
				components.remove("reforged:reforged_modifier");
				components.put("tiered:tiered_modifier", modifierTag);
			}

			if (components.contains("tiered:tiered_modifier", Tag.TAG_STRING)) {
				String val = components.getString("tiered:tiered_modifier");
				if (val.startsWith("reforged:")) {
					components.putString("tiered:tiered_modifier", val.replace("reforged:", "tiered:"));
				}
			}
		}
		return copyTag;
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
	private void tiered$appendAttributes(EquipmentSlotGroup slot, BiConsumer<Holder<Attribute>, AttributeModifier> pAction, CallbackInfo ci) {
		ItemStack thisStack = (ItemStack)(Object)this;
		TierHelper.AppendAttributesToOriginal(thisStack, slot, TierHelper.isPreferredEquipmentSlot(thisStack, slot), "AttributeModifiers",
				template -> template.getRequiredEquipmentSlot(),
				template -> template.getOptionalEquipmentSlot(),
				(template) -> template.realize(pAction, slot));
	}
}