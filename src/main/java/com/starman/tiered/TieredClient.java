package com.starman.tiered;

import com.starman.tiered.network.protocol.game.ClientboundTierSyncerPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TieredClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundTierSyncerPacket.TYPE,
                (payload, context) -> {
                    context.client().execute(payload::handleOnClient);
                }
        );
    }
}