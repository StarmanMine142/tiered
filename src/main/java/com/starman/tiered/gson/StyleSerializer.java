package com.starman.tiered.gson;

import java.lang.reflect.Type;

import org.jetbrains.annotations.Nullable;

import com.google.gson.*;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.network.chat.*;

public class StyleSerializer implements JsonDeserializer<Style>, JsonSerializer<Style> {
    @Override
    @Nullable
    public Style deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }
            Boolean boolean_ = getOptionalFlag(jsonObject, "bold");
            Boolean boolean2 = getOptionalFlag(jsonObject, "italic");
            Boolean boolean3 = getOptionalFlag(jsonObject, "underlined");
            Boolean boolean4 = getOptionalFlag(jsonObject, "strikethrough");
            Boolean boolean5 = getOptionalFlag(jsonObject, "obfuscated");
            TextColor textColor = getTextColor(jsonObject);
            String string = getInsertion(jsonObject);
            ResourceLocation resourceLocation = getFont(jsonObject);
            return Style.EMPTY
                    .withColor(textColor).withBold(boolean_).withItalic(boolean2)
                    .withUnderlined(boolean3).withStrikethrough(boolean4).withObfuscated(boolean5)
                    .withInsertion(string).withFont(resourceLocation);
        }
        return null;
    }

    @Nullable
    private static ResourceLocation getFont(JsonObject json) {
        if (json.has("font")) {
            String string = GsonHelper.getAsString(json, "font");
            try {
                return ResourceLocation.parse(string);
            }
            catch (ResourceLocationException resourceLocationException) {
                throw new JsonSyntaxException("Invalid font name: " + string);
            }
        }
        return null;
    }

    @Nullable
    private static String getInsertion(JsonObject json) {
        return GsonHelper.getAsString(json, "insertion", null);
    }

    @Nullable
    private static TextColor getTextColor(JsonObject json) {
        if (json.has("color")) {
            String string = GsonHelper.getAsString(json, "color");
            return TextColor.parseColor(string).getOrThrow();
        }
        return null;
    }

    @Nullable
    private static Boolean getOptionalFlag(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return json.get(memberName).getAsBoolean();
        }
        return null;
    }

    @Override
    @Nullable
    public JsonElement serialize(Style style, Type type, JsonSerializationContext jsonSerializationContext) {
        if (style.isEmpty()) {
            return null;
        }
        JsonObject jsonObject = new JsonObject();

        if (style.getColor() != null) {
            jsonObject.addProperty("color", style.getColor().serialize());
        }
        return jsonObject;
    }
}