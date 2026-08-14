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
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow private DataSlot cost;

    @Unique private ItemStack tiered$lastInputItem = ItemStack.EMPTY;
    @Unique private ResourceLocation tiered$cachedNextModifier = null;

    public AnvilMenuMixin(MenuType<?> type, int syncId, Inventory inventory, ContainerLevelAccess access) {
        super(type, syncId, inventory, access);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void tiered$createResult(CallbackInfo ci) {
        ItemStack inputItem = this.inputSlots.getItem(0);
        ItemStack materialItem = this.inputSlots.getItem(1);

        if (Tiered.hasModifier(inputItem) && isValidHammer(materialItem)) {
            ResourceLocation currentModifierId = inputItem.get(Tiered.MODIFIER);
            if (currentModifierId != null) {
                if (tiered$cachedNextModifier == null || !ItemStack.isSameItemSameComponents(inputItem, tiered$lastInputItem)) {
                    tiered$lastInputItem = inputItem.copy();
                    ResourceLocation newModifierId = ModifierUtils.getRandomAttributeIDFor(inputItem.getItem());
                    int attempts = 0;
                    while ((newModifierId.equals(currentModifierId) || newModifierId.equals(ModifierUtils.BLANK)) && attempts < 5) {
                        newModifierId = ModifierUtils.getRandomAttributeIDFor(inputItem.getItem());
                        attempts++;
                    }
                    tiered$cachedNextModifier = newModifierId;
                }

                PotentialAttribute nextPotential = Tiered.TIER_DATA.getTiers().get(tiered$cachedNextModifier);
                if (nextPotential != null) {
                    ItemStack result = inputItem.copy();
                    result.remove(Tiered.MODIFIER);
                    this.resultSlots.setItem(0, result);

                    if (this.cost != null) {
                        this.cost.set((int) nextPotential.getReforgeExperienceCost());
                    }

                    this.broadcastChanges();
                    ci.cancel();
                }
            }
        } else {
            tiered$cachedNextModifier = null;
            tiered$lastInputItem = ItemStack.EMPTY;
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void tiered$onTake(Player player, ItemStack outputStack, CallbackInfo ci) {
        ItemStack materialItem = this.inputSlots.getItem(1);
        ItemStack inputItem = this.inputSlots.getItem(0);

        if (Tiered.hasModifier(inputItem) && isValidHammer(materialItem)) {
            if (tiered$cachedNextModifier != null) {
                PotentialAttribute nextPotential = Tiered.TIER_DATA.getTiers().get(tiered$cachedNextModifier);
                if (nextPotential != null) {
                    outputStack.set(Tiered.MODIFIER, tiered$cachedNextModifier);

                    this.inputSlots.setItem(0, ItemStack.EMPTY);

                    int durabilityCost = nextPotential.getReforgeDurabilityCost();

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

                    int expCost = (int) nextPotential.getReforgeExperienceCost();
                    if (!player.getAbilities().instabuild && expCost > 0) {
                        player.giveExperienceLevels(-expCost);
                    }

                    if (this.cost != null) {
                        this.cost.set(0);
                    }

                    tiered$cachedNextModifier = null;
                    tiered$lastInputItem = ItemStack.EMPTY;

                    this.broadcastChanges();
                    ci.cancel();
                }
            }
        }
    }

    @Unique
    private boolean isValidHammer(ItemStack hammerItem) {
        return hammerItem.is(Tiered.SMITHING_HAMMER);
    }
}