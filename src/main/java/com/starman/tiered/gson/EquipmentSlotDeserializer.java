package com.starman.tiered.gson;

import java.lang.reflect.Type;

import com.google.gson.*;

import net.minecraft.world.entity.EquipmentSlotGroup;

public class EquipmentSlotDeserializer implements JsonDeserializer<EquipmentSlotGroup> {

    @Override
    public EquipmentSlotGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    	return EquipmentSlotGroup.valueOf(json.getAsString().toUpperCase());
    }
}
