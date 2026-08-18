package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.*;
import com.starman.tiered.config.TieredConfig;
import com.starman.tiered.item.TieredItems;

import com.starman.tiered.util.TierHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Final
    @Shadow private DataSlot cost;

    @Unique private static final String TIERED_PENDING_MODIFIER = "tiered_pending_reforge";

    public AnvilMenuMixin(MenuType<?> type, int syncId, Inventory inventory, ContainerLevelAccess access) {
        super(type, syncId, inventory, access);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void tiered$mayPickup(Player player, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        ItemStack inputItem = this.inputSlots.getItem(0);
        ItemStack materialItem = this.inputSlots.getItem(1);

        if (TierHelper.hasModifier(inputItem) && isValidHammer(materialItem)) {
            if (!TieredConfig.enableReforgeExpCost || player.getAbilities().instabuild || player.experienceLevel >= getExpCost(inputItem)) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void tiered$createResult(CallbackInfo ci) {
        ItemStack inputItem = this.inputSlots.getItem(0);
        ItemStack materialItem = this.inputSlots.getItem(1);

        if (TierHelper.hasModifier(inputItem) && isValidHammer(materialItem)) {
            ResourceLocation currentModifierId = inputItem.get(Tiered.MODIFIER);
            if (currentModifierId != null) {

                ResourceLocation pendingModifier = getPendingModifier(inputItem);

                if (pendingModifier == null) {
                    pendingModifier = ModifierUtils.getRandomAttributeIDFor(inputItem.getItem());
                    int attempts = 0;
                    while ((pendingModifier.equals(currentModifierId) || pendingModifier.equals(ModifierUtils.BLANK)) && attempts < 5) {
                        pendingModifier = ModifierUtils.getRandomAttributeIDFor(inputItem.getItem());
                        attempts++;
                    }
                    setPendingModifier(inputItem, pendingModifier);
                }

                PotentialAttribute nextPotential = Tiered.TIER_DATA.getTiers().get(pendingModifier);
                if (nextPotential != null) {
                    ItemStack result = inputItem.copy();
                    result.set(Tiered.MODIFIER, pendingModifier);
                    result.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);

                    this.resultSlots.setItem(0, result);

                    int expCost = TieredConfig.enableReforgeExpCost ? nextPotential.getReforgeExperienceCost() : 0;
                    if (this.cost != null) {
                        this.cost.set(expCost);
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
        ItemStack inputItem = this.inputSlots.getItem(0);

        if (TierHelper.hasModifier(inputItem) && isValidHammer(materialItem)) {
            ResourceLocation pendingModifier = getPendingModifier(inputItem);

            if (pendingModifier != null) {
                PotentialAttribute nextPotential = Tiered.TIER_DATA.getTiers().get(pendingModifier);
                if (nextPotential != null) {
                    outputStack.set(Tiered.MODIFIER, pendingModifier);

                    ItemStack hammerForParticles = materialItem.copy();

                    this.inputSlots.setItem(0, ItemStack.EMPTY);
                    this.resultSlots.setItem(0, ItemStack.EMPTY);

                    boolean hammerBroken = false;

                    if (!player.getAbilities().instabuild) {
                        int durabilityCost = nextPotential.getReforgeDurabilityCost();

                        if (materialItem.isDamageableItem()) {
                            if ((materialItem.getMaxDamage() - materialItem.getDamageValue()) <= durabilityCost) {
                                materialItem.shrink(1);
                                hammerBroken = true;
                            } else {
                                materialItem.setDamageValue(materialItem.getDamageValue() + durabilityCost);
                            }
                        } else {
                            materialItem.shrink(1);
                            hammerBroken = true;
                        }

                        this.inputSlots.setItem(1, materialItem);
                    }

                    int expCost = TieredConfig.enableReforgeExpCost ? (int) nextPotential.getReforgeExperienceCost() : 0;
                    if (expCost > 0 && !player.getAbilities().instabuild) {
                        player.giveExperienceLevels(-expCost);
                    }

                    final boolean finalHammerBroken = hammerBroken;

                    this.access.execute((level, pos) -> {
                        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);

                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 10, 0.3D, 0.2D, 0.3D, 0.1D);

                            if (finalHammerBroken) {
                                level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
                                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, hammerForParticles), pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 15, 0.2D, 0.1D, 0.2D, 0.05D);
                            }
                        }
                    });

                    if (this.cost != null) {
                        this.cost.set(0);
                    }

                    this.broadcastChanges();
                    ci.cancel();
                }
            }
        }
    }

    @Unique
    private boolean isValidHammer(ItemStack hammerItem) {
        return hammerItem.is(TieredItems.SMITHING_HAMMER);
    }

    @Unique
    private int getExpCost(ItemStack inputItem) {
        ResourceLocation pending = getPendingModifier(inputItem);
        if (pending != null) {
            PotentialAttribute attr = Tiered.TIER_DATA.getTiers().get(pending);
            return attr != null ? (int) attr.getReforgeExperienceCost() : 0;
        }
        return 0;
    }

    @Unique
    private ResourceLocation getPendingModifier(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains(TIERED_PENDING_MODIFIER)) {
                return ResourceLocation.tryParse(tag.getString(TIERED_PENDING_MODIFIER));
            }
        }
        return null;
    }

    @Unique
    private void setPendingModifier(ItemStack stack, ResourceLocation modifierId) {
        var customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        var updatedTag = customData.update(tag -> tag.putString(TIERED_PENDING_MODIFIER, modifierId.toString()));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, updatedTag);
    }
}