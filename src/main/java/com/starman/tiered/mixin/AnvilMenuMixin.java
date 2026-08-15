package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.ModifierUtils;
import com.starman.tiered.api.PotentialAttribute;
import com.starman.tiered.config.TieredConfig;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow private DataSlot cost;

    @Unique private ItemStack tiered$lastInputItem = ItemStack.EMPTY;
    @Unique private ResourceLocation tiered$cachedNextModifier = null;

    public AnvilMenuMixin(MenuType<?> type, int syncId, Inventory inventory, ContainerLevelAccess access) {
        super(type, syncId, inventory, access);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void tiered$mayPickup(Player player, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        ItemStack inputItem = this.inputSlots.getItem(0);
        ItemStack materialItem = this.inputSlots.getItem(1);

        if (Tiered.hasModifier(inputItem) && isValidHammer(materialItem)) {
            if (!TieredConfig.enableReforgeExpCost) {
                cir.setReturnValue(true);
            }
        }
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
                        int expCost = TieredConfig.enableReforgeExpCost ? (int) nextPotential.getReforgeExperienceCost() : 0;
                        this.cost.set(expCost);
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

                    if (TieredConfig.enableReforgeExpCost && !player.getAbilities().instabuild) {
                        int expCost = (int) nextPotential.getReforgeExperienceCost();
                        if (expCost > 0) {
                            player.giveExperienceLevels(-expCost);
                        }
                    }

                    final boolean finalHammerBroken = hammerBroken;

                    this.access.execute((level, pos) -> {
                        level.playSound(
                                null,
                                pos,
                                SoundEvents.ANVIL_USE,
                                SoundSource.BLOCKS,
                                0.5F,
                                level.random.nextFloat() * 0.1F + 0.9F
                        );

                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    ParticleTypes.END_ROD,
                                    pos.getX() + 0.5D,
                                    pos.getY() + 1.1D,
                                    pos.getZ() + 0.5D,
                                    10,
                                    0.3D,
                                    0.2D,
                                    0.3D,
                                    0.1D
                            );

                            if (finalHammerBroken) {
                                level.playSound(
                                        null,
                                        pos,
                                        SoundEvents.ITEM_BREAK,
                                        SoundSource.BLOCKS,
                                        0.8F,
                                        0.8F + level.random.nextFloat() * 0.4F
                                );

                                serverLevel.sendParticles(
                                        new ItemParticleOption(ParticleTypes.ITEM, hammerForParticles),
                                        pos.getX() + 0.5D,
                                        pos.getY() + 1.1D,
                                        pos.getZ() + 0.5D,
                                        15,
                                        0.2D,
                                        0.1D,
                                        0.2D,
                                        0.05D
                                );
                            }
                        }
                    });

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