package com.starman.tiered.network;

import com.starman.tiered.Tiered;

import net.fabricmc.fabric.api.networking.v1.*;

public class TierNetwork {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(
                ClientboundTierSyncerPacket.TYPE,
                ClientboundTierSyncerPacket.STREAM_CODEC
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player.level().isClientSide()) {
                return;
            }
            ServerPlayNetworking.send(handler.player, new ClientboundTierSyncerPacket(Tiered.TIER_DATA.getTiers()));
        });
    }
}