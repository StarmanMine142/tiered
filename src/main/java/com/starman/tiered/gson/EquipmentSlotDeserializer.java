package com.starman.tiered.gson;

import java.lang.reflect.Type;
import java.util.Locale;

import com.google.gson.*;

import net.minecraft.world.entity.EquipmentSlotGroup;

public class EquipmentSlotDeserializer implements JsonDeserializer<EquipmentSlotGroup> {

    @Override
    public EquipmentSlotGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String lower = json.getAsString().toLowerCase(Locale.ROOT);

        for (EquipmentSlotGroup group : EquipmentSlotGroup.values()) {
            if (group.getSerializedName().equals(lower)) {
                return group;
            }
        }

        if (lower.contains("head")) return EquipmentSlotGroup.HEAD;
        if (lower.contains("chest")) return EquipmentSlotGroup.CHEST;
        if (lower.contains("legs")) return EquipmentSlotGroup.LEGS;
        if (lower.contains("feet")) return EquipmentSlotGroup.FEET;
        if (lower.contains("offhand")) return EquipmentSlotGroup.OFFHAND;
        if (lower.contains("mainhand") || lower.contains("hand")) return EquipmentSlotGroup.MAINHAND;
        if (lower.contains("armor")) return EquipmentSlotGroup.ARMOR;

        return EquipmentSlotGroup.ANY;
    }
}