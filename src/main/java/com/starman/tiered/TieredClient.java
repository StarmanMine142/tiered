package com.starman.tiered;

import com.starman.tiered.api.TieredAttributes;
import com.starman.tiered.grammar.TierGrammarManager;
import com.starman.tiered.network.protocol.game.ClientboundTierSyncerPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TieredClient implements ClientModInitializer {

    private static double getDrawSpeedFromItemStack(ItemStack stack) {
        net.minecraft.world.item.component.ItemAttributeModifiers modifiers = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 1.0;
        }

        double speed = 1.0;

        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().is(TieredAttributes.DRAW_SPEED)) {
                speed += entry.modifier().amount();
            }
        }

        return speed;
    }

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new TierGrammarManager());
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundTierSyncerPacket.TYPE,
                (payload, context) -> {
                    context.client().execute(payload::handleOnClient);
                }
        );

        ItemProperties.register(
                Items.BOW,
                ResourceLocation.fromNamespaceAndPath("minecraft", "pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;

                    if (!entity.isUsingItem() || entity.getUseItem() != stack) {
                        return 0.0F;
                    }

                    double drawSpeed = getDrawSpeedFromItemStack(stack);

                    int ticksUsed = stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();
                    float progress = (float) (ticksUsed * drawSpeed) / 20.0F;

                    return Math.min(1.0F, progress);
                }
        );
    }
}