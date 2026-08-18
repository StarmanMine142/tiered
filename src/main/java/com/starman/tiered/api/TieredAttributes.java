package com.starman.tiered.api;

import com.starman.tiered.Tiered;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;

public class TieredAttributes {

    public static final Holder<Attribute> CRITICAL_RATE = register("generic.critical_rate", 0.0, 0.0, 1.0);
    public static final Holder<Attribute> CRITICAL_DAMAGE = register("generic.critical_damage", 1.5, 0.0, 2048.0);
    public static final Holder<Attribute> DRAW_SPEED = register("generic.draw_speed", 1.0, 1.0E-4, 1024.0);
    public static final Holder<Attribute> ACCURACY = register("generic.accuracy", 0.5, 0.0, 1.0);
    public static final Holder<Attribute> ARROW_VELOCITY = register("generic.arrow_velocity", 1.0, 0.0, 10.0);
    public static final Holder<Attribute> ARROW_DAMAGE = register("generic.arrow_damage", 0.0, -1024.0, 1024.0);

    private static Holder<Attribute> register(String name, double defaultValue, double min, double max) {
        return Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                ResourceLocation.fromNamespaceAndPath(Tiered.ID, name),
                new RangedAttribute("attribute.name." + name, defaultValue, min, max).setSyncable(true)
        );
    }

    public static void register() {
    }
}