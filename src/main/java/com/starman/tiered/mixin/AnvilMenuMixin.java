package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.ModifierUtils;
import com.starman.tiered.api.PotentialAttribute;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

	@Shadow private DataSlot cost;

	public AnvilMenuMixin(MenuType<?> type, int syncId, Inventory inventory, ContainerLevelAccess access) {
		super(type, syncId, inventory, access);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void tiered$createResult(CallbackInfo ci) {
		ItemStack inputItem = this.inputSlots.getItem(0);
		ItemStack materialItem = this.inputSlots.getItem(1);

		if (Tiered.hasModifier(inputItem) && isValidHammerFor(inputItem, materialItem)) {
			ResourceLocation modifierId = inputItem.get(Tiered.MODIFIER);
			if (modifierId != null) {
				PotentialAttribute potential = Tiered.TIER_DATA.getTiers().get(modifierId);
				if (potential != null) {
					ItemStack result = inputItem.copy();
					this.resultSlots.setItem(0, result);

					if (this.cost != null) {
						this.cost.set((int) potential.getReforgeExperienceCost());
					}

					this.broadcastChanges();
					ci.cancel();
				}
			}
		}
	}

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void tiered$onTake(Player player, ItemStack outputStack, CallbackInfo ci) {
		ItemStack materialItem = this.inputSlots.getItem(1);

		if (Tiered.hasModifier(outputStack) && isValidHammerFor(outputStack, materialItem)) {
			ResourceLocation currentModifierId = outputStack.get(Tiered.MODIFIER);
			if (currentModifierId != null) {
				PotentialAttribute potential = Tiered.TIER_DATA.getTiers().get(currentModifierId);
				if (potential != null) {
					ResourceLocation newModifierId = currentModifierId;
					int attempts = 0;
					while ((newModifierId.equals(currentModifierId) || newModifierId.equals(ModifierUtils.BLANK)) && attempts < 5) {
						newModifierId = ModifierUtils.getRandomAttributeIDFor(outputStack.getItem());
						attempts++;
					}

					if (!newModifierId.equals(ModifierUtils.BLANK)) {
						outputStack.set(Tiered.MODIFIER, newModifierId);
					}

					this.inputSlots.setItem(0, ItemStack.EMPTY);

					int durabilityCost = potential.getReforgeDurabilityCost();

					if (materialItem.isDamageableItem()) {
						if ((materialItem.getMaxDamage() - materialItem.getDamageValue()) <= durabilityCost) {
							materialItem.shrink(1);
						} else {
							materialItem.setDamageValue(materialItem.getDamageValue() + durabilityCost);
						}
					} else {
						materialItem.shrink(1);
					}

					this.inputSlots.setItem(1, materialItem);

					int expCost = (int) potential.getReforgeExperienceCost();
					if (!player.getAbilities().instabuild && expCost > 0) {
						player.giveExperienceLevels(-expCost);
					}

					if (this.cost != null) {
						this.cost.set(0);
					}

					this.broadcastChanges();
					ci.cancel();
				}
			}
		}
	}

	private boolean isValidHammerFor(ItemStack targetItem, ItemStack hammerItem) {
		if (targetItem.getItem() instanceof ArmorItem) {
			return hammerItem.is(Tiered.ARMORERS_HAMMER);
		} else if (targetItem.getItem() instanceof SwordItem) {
			return hammerItem.is(Tiered.WEAPONSMITHS_HAMMER);
		} else if (targetItem.getItem() instanceof TieredItem) {
			return hammerItem.is(Tiered.TOOLSMITHS_HAMMER);
		}
		return false;
	}
}