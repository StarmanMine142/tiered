package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

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
					cir.setReturnValue(Optional.of(new ItemStack(Tiered.SMITHING_HAMMER, stack.getCount())));
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
		Tiered.AppendAttributesToOriginal(thisStack, slot, Tiered.isPreferredEquipmentSlot(thisStack, slot), "AttributeModifiers",
				template -> template.getRequiredEquipmentSlot(),
				template -> template.getOptionalEquipmentSlot(),
				(template) -> template.realize(pAction, slot));
	}
}