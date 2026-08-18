package com.starman.tiered.api;

import com.starman.tiered.Tiered;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.*;

public class ItemVerifier {
    public static final Codec<ItemVerifier> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            Codec.STRING.optionalFieldOf("id").forGetter(v -> Optional.ofNullable(v.id)),
                            Codec.STRING.optionalFieldOf("tag").forGetter(v -> Optional.ofNullable(v.tag))
                    )
                    .apply(i, (id, tag) -> new ItemVerifier(id.orElse(null), tag.orElse(null))));

    private final String id;
    private final String tag;

    public ItemVerifier(String id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    public boolean isValid(ResourceLocation itemID) {
        return isValid(itemID.toString());
    }

    public boolean isValid(String itemID) {
        if (id != null) {
            return itemID.equals(id);
        } else if (tag != null) {
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));

            if (itemTag != null) {
                return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemID))).is(itemTag);
            } else {
                Tiered.LOGGER.error(tag + " was specified as an item verifier tag, but it does not exist!");
            }
        }

        return false;
    }
}