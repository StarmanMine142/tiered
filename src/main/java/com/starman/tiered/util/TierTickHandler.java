package com.starman.tiered.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class TierTickHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 10 == 0) {
                    for (ItemStack stack : player.getInventory().items) {
                        if (!stack.isEmpty()) TierHelper.attemptToAffixTier(stack);
                    }
                    for (ItemStack stack : player.getInventory().armor) {
                        if (!stack.isEmpty()) TierHelper.attemptToAffixTier(stack);
                    }
                    for (ItemStack stack : player.getInventory().offhand) {
                        if (!stack.isEmpty()) TierHelper.attemptToAffixTier(stack);
                    }

                    AbstractContainerMenu menu = player.containerMenu;
                    if (!(menu instanceof ItemCombinerMenu)) {
                        for (int i = 0; i < menu.slots.size(); i++) {
                            Slot slot = menu.slots.get(i);
                            if (slot.container != player.getInventory()) {
                                ItemStack stack = slot.getItem();
                                if (!stack.isEmpty()) {
                                    TierHelper.attemptToAffixTier(stack);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}