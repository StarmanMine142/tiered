package com.starman.tiered.network;

import com.starman.tiered.Tiered;
import com.starman.tiered.api.PotentialAttribute;
import com.starman.tiered.data.TierDataLoader;

import java.util.*;

import com.google.common.collect.Maps;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class ClientboundTierSyncerPacket implements CustomPacketPayload {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Tiered.ID, "tier_sync");
	public static final CustomPacketPayload.Type<ClientboundTierSyncerPacket> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTierSyncerPacket> STREAM_CODEC = CustomPacketPayload.codec(
			ClientboundTierSyncerPacket::write,
			ClientboundTierSyncerPacket::new
	);

	private final int size;
	private final Map<ResourceLocation, PotentialAttribute> attribute;
	public static final Map<ResourceLocation, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

	public ClientboundTierSyncerPacket(Map<ResourceLocation, PotentialAttribute> attribute) {
		this.attribute = attribute;
		this.size = attribute.size();
	}

	public ClientboundTierSyncerPacket(RegistryFriendlyByteBuf buf) {
		this.size = buf.readInt();
		this.attribute = Maps.newHashMap();
		for (int i = 0; i < this.size; i++) {
			ResourceLocation id = buf.readResourceLocation();
			PotentialAttribute pa = TierDataLoader.GSON.fromJson(buf.readUtf(), PotentialAttribute.class);
			this.attribute.put(id, pa);
		}
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeInt(this.size);
		this.attribute.forEach((id, attribute) -> {
			buf.writeResourceLocation(id);
			buf.writeUtf(TierDataLoader.GSON.toJson(attribute));
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public void handleOnClient() {
		CACHED_ATTRIBUTES.putAll(Tiered.TIER_DATA.getTiers());
		Tiered.TIER_DATA.clear();

		Tiered.TIER_DATA.replace(this.attribute);
		if (Tiered.TIER_DATA.getTiers().size() == 0) {
			Tiered.TIER_DATA.replace(CACHED_ATTRIBUTES);
		}
	}
}