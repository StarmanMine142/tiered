package com.starman.tiered.command;

import com.starman.tiered.Tiered;

import java.util.*;

import net.fabricmc.loader.api.FabricLoader;

import dev.emi.trinkets.api.TrinketsApi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.*;
import net.minecraft.world.entity.*;

public class TieredCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(Tiered.ID)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("updatetier")
                                .then(Commands.literal("everything")
                                        .executes(context -> executeUpdate(context, true, Collections.singleton(context.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(context -> executeUpdate(context, true, EntityArgument.getEntities(context, "targets")))
                                        )
                                )
                                .then(Commands.literal("only")
                                        .executes(context -> executeUpdate(context, false, Collections.singleton(context.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(context -> executeUpdate(context, false, EntityArgument.getEntities(context, "targets")))
                                        )
                                )
                        )
        );
    }

    private static int executeUpdate(CommandContext<CommandSourceStack> context, boolean everything, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int affectedEntitiesCount = 0;
        int totalUpdatedItems = 0;
        Entity singleEntity = null;
        ItemStack targetItemStack = ItemStack.EMPTY;
        boolean hasAnyItemInHand = false;

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                int updatedForThisEntity = 0;

                if (everything) {
                    if (living instanceof Player player) {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (updateItemStack(stack)) {
                                updatedForThisEntity++;
                            }
                        }
                    } else {
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            ItemStack stack = living.getItemBySlot(slot);
                            if (updateItemStack(stack)) {
                                updatedForThisEntity++;
                            }
                        }
                    }

                    if (FabricLoader.getInstance().isModLoaded("trinkets")) {
                        var componentOpt = TrinketsApi.getTrinketComponent(living);
                        if (componentOpt.isPresent()) {
                            final int[] trinketUpdated = {0};
                            componentOpt.get().forEach((slotReference, stack) -> {
                                if (updateItemStack(stack)) {
                                    trinketUpdated[0]++;
                                }
                            });
                            updatedForThisEntity += trinketUpdated[0];
                        }
                    }

                } else {
                    ItemStack stack = living.getMainHandItem();
                    if (!stack.isEmpty()) {
                        hasAnyItemInHand = true;
                        if (targetItemStack.isEmpty()) {
                            targetItemStack = stack;
                        }
                        if (updateItemStack(stack)) {
                            updatedForThisEntity++;
                        }
                    }
                }

                if (updatedForThisEntity > 0) {
                    affectedEntitiesCount++;
                    totalUpdatedItems += updatedForThisEntity;
                    singleEntity = living;
                }
            }
        }

        final int itemsFinal = totalUpdatedItems;
        final int entitiesFinal = affectedEntitiesCount;
        final Entity finalEntity = singleEntity;

        final ItemStack finalItemStack = targetItemStack;
        Entity firstTarget = targets.isEmpty() ? null : targets.iterator().next();

        Component message;
        if (affectedEntitiesCount == 0) {
            if (everything) {
                if (targets.size() == 1 && firstTarget != null) {
                    message = Component.translatable("commands.tiered.updatetier.everything.fail.one", 0, firstTarget.getDisplayName());
                } else {
                    message = Component.translatable("commands.tiered.updatetier.everything.fail.all", 0, targets.size());
                }
            } else {
                if (!hasAnyItemInHand) {
                    message = Component.translatable("commands.tiered.updatetier.only.fail.noitem");
                } else {
                    Component itemName = finalItemStack.isEmpty()
                            ? Component.literal("Item")
                            : finalItemStack.getItem().getDefaultInstance().getDisplayName();

                    if (targets.size() == 1 && firstTarget != null) {
                        message = Component.translatable("commands.tiered.updatetier.only.fail.one", itemName, firstTarget.getDisplayName());
                    } else {
                        message = Component.translatable("commands.tiered.updatetier.only.fail.all", itemName, targets.size());
                    }
                }
            }
            context.getSource().sendFailure(message);
            return 0;
        }

        if (everything) {
            if (entitiesFinal == 1 && finalEntity != null) {
                message = Component.translatable("commands.tiered.updatetier.everything.success.one", itemsFinal, finalEntity.getDisplayName());
            } else {
                message = Component.translatable("commands.tiered.updatetier.everything.success.all", itemsFinal, entitiesFinal);
            }
        } else {
            Component itemName = finalItemStack.isEmpty()
                    ? Component.literal("Item")
                    : finalItemStack.getItem().getDefaultInstance().getDisplayName();

            if (entitiesFinal == 1 && finalEntity != null) {
                message = Component.translatable("commands.tiered.updatetier.only.success.one", itemName, finalEntity.getDisplayName());
            } else {
                message = Component.translatable("commands.tiered.updatetier.only.success.all", itemName, entitiesFinal);
            }
        }

        context.getSource().sendSuccess(() -> message, true);
        return entitiesFinal;
    }

    private static boolean updateItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean hadModifier = Tiered.hasModifier(stack);
        if (hadModifier) {
            stack.remove(Tiered.MODIFIER);
        }

        Tiered.attemptToAffixTier(stack);

        return Tiered.hasModifier(stack);
    }
}