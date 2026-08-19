package com.starman.tiered.gson;

import java.lang.reflect.Type;

import com.google.gson.*;

public class EquipmentSlotDeserializer implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return json.getAsString();
    }
}
