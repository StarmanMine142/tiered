package com.starman.tiered.api;

import com.starman.tiered.Tiered;

import java.util.function.BiConsumer;
import java.util.*;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.*;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;

public class AttributeTemplate {
	public static final Codec<EquipmentSlotGroup> LENIENT_SLOT_GROUP_CODEC = Codec.STRING.xmap(
			s -> {
				String lower = s.toLowerCase(Locale.ROOT);
				for (EquipmentSlotGroup group : EquipmentSlotGroup.values()) {
					if (group.getSerializedName().equals(lower)) return group;
				}
				throw new IllegalArgumentException("Unknown EquipmentSlotGroup: " + s);
			},
			EquipmentSlotGroup::getSerializedName
	);
	private static final Codec<AttributeModifier.Operation> LENIENT_OPERATION_CODEC = Codec.STRING.xmap(
			s -> {
				String lower = s.toLowerCase(Locale.ROOT);
				for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
					if (op.getSerializedName().equals(lower)) return op;
				}
				throw new IllegalArgumentException("Unknown AttributeModifier.Operation: " + s);
			},
			AttributeModifier.Operation::getSerializedName
	);
	public static final MapCodec<AttributeModifier> MAP_CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
							ResourceLocation.CODEC.fieldOf("id").forGetter(AttributeModifier::id),
							Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::amount),
							LENIENT_OPERATION_CODEC.fieldOf("operation").forGetter(AttributeModifier::operation)
					)
					.apply(i, AttributeModifier::new)
	);

	public static final Codec<AttributeTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("type").forGetter(t -> t.attributeTypeID),
			MAP_CODEC.codec().fieldOf("modifier").forGetter(t -> t.attributeModifier),
			LENIENT_SLOT_GROUP_CODEC.listOf().optionalFieldOf("required_equipment_slots", List.of())
					.forGetter(t -> t.requiredEquipmentSlotTypes == null ? List.of() : Arrays.asList(t.requiredEquipmentSlotTypes)),
			LENIENT_SLOT_GROUP_CODEC.listOf().optionalFieldOf("optional_equipment_slots", List.of())
					.forGetter(t -> t.optionalEquipmentSlotTypes == null ? List.of() : Arrays.asList(t.optionalEquipmentSlotTypes))
	).apply(instance, (type, modifier, reqEquip, optEquip) ->
			new AttributeTemplate(
					type, modifier,
					reqEquip.toArray(new EquipmentSlotGroup[0]),
					optEquip.toArray(new EquipmentSlotGroup[0])
			)
	));

	@SerializedName("type")
	private final String attributeTypeID;

	@SerializedName("modifier")
	private final AttributeModifier attributeModifier;

	@SerializedName("required_equipment_slots")
	private final EquipmentSlotGroup[] requiredEquipmentSlotTypes;

	@SerializedName("optional_equipment_slots")
	private final EquipmentSlotGroup[] optionalEquipmentSlotTypes;

	public AttributeTemplate(String attributeTypeID, AttributeModifier AttributeModifier,
							 EquipmentSlotGroup[]  requiredEquipmentSlotTypes, EquipmentSlotGroup[]  optionalEquipmentSlotTypes) {
		this.attributeTypeID = attributeTypeID;
		this.attributeModifier = AttributeModifier;
		this.requiredEquipmentSlotTypes = requiredEquipmentSlotTypes;
		this.optionalEquipmentSlotTypes = optionalEquipmentSlotTypes;
	}

	public EquipmentSlotGroup[] getRequiredEquipmentSlot() {
		return requiredEquipmentSlotTypes;
	}

	public EquipmentSlotGroup[] getOptionalEquipmentSlot() {
		return optionalEquipmentSlotTypes;
	}

	public EquipmentSlot[] getRequiredLiteralEquipmentSlot() {
		List<EquipmentSlot> slots = new ArrayList<EquipmentSlot>();
		if (requiredEquipmentSlotTypes != null)
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (!slots.contains(slot)) {
					for (EquipmentSlotGroup group : requiredEquipmentSlotTypes) {
						if (group.test(slot)) slots.add(slot);
					}
				}
			}
		return slots.toArray(new EquipmentSlot[0]);
	}

	public EquipmentSlot[] getOptionalLiteralEquipmentSlot() {
		List<EquipmentSlot> slots = new ArrayList<EquipmentSlot>();
		if (optionalEquipmentSlotTypes != null)
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (!slots.contains(slot)) {
					for (EquipmentSlotGroup group : optionalEquipmentSlotTypes) {
						if (group.test(slot)) slots.add(slot);
					}
				}
			}
		return slots.toArray(new EquipmentSlot[0]);
	}

	public void realize(BiConsumer<Holder<Attribute>, AttributeModifier> actions, EquipmentSlot slot) {
		ResourceLocation uniqueId = ResourceLocation.fromNamespaceAndPath(
				Tiered.ID,
				attributeModifier.id().getPath() + "_" + slot.getName()
		);

		realize(actions, uniqueId);
	}

	public void realizeGroup(BiConsumer<Holder<Attribute>, AttributeModifier> actions, EquipmentSlotGroup slot) {
		realize(actions, Tiered.MODIFIERS[slot.ordinal()]);
	}

	private void realize(BiConsumer<Holder<Attribute>, AttributeModifier> actions, ResourceLocation id) {
		AttributeModifier cloneModifier = new AttributeModifier(
				id.withPrefix("tiered_"+attributeModifier.id().getPath()),
				attributeModifier.amount(),
				attributeModifier.operation()
		);

		Optional<Holder.Reference<Attribute>> key = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attributeTypeID));
		if(key == null || key.isEmpty()) {
			Tiered.LOGGER.warn(String.format("%s was referenced as an attribute type, but it does not exist! A data file in /tiered/item_attributes/ has an invalid type property.", attributeTypeID));
		} else {
			actions.accept(key.get(), cloneModifier);
		}
	}

	public boolean attributeExists(String keyChecked) {
		Optional<Holder.Reference<Attribute>> key = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attributeTypeID));
		if (key == null || key.isEmpty()) {
			Tiered.LOGGER.warn(String.format("%s was referenced as an attribute type in %s, but it does not exist!", attributeTypeID, keyChecked));
			return false;
		}
		return true;
	}
}