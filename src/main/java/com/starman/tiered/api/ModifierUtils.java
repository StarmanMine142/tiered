package com.starman.tiered.api;

import com.starman.tiered.Tiered;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ModifierUtils {

	public static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath(Tiered.ID, "blank");

	public static ResourceLocation getRandomAttributeIDFor(Item item) {
		ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
		TierPool pool = getRandomFrom(Tiered.POOL_DATA.getPools().values(), (p) -> p.isValid(itemKey));
		PotentialAttribute chosen_tier;
		if (pool == null)
			chosen_tier = getRandomFrom(Tiered.TIER_DATA.getTiers().values(), (a) -> a.isValid(itemKey));
		else {
			List<PotentialAttribute> attris = new ArrayList<>();
			pool.getTiers().forEach((t) -> attris.add(Tiered.TIER_DATA.getTiers().get(ResourceLocation.parse(t))));
			attris.removeIf((attr) -> attr == null);
			chosen_tier = getRandomFrom(attris, null);
		}
		if (chosen_tier != null) {
			if (false)
				return ResourceLocation.parse(chosen_tier.getID());
			else for (Entry<ResourceLocation, PotentialAttribute> cho : Tiered.TIER_DATA.getTiers().entrySet())
				if (cho.getValue() == chosen_tier) return cho.getKey();
		}

		List<ResourceLocation> potentialAttributes = new ArrayList<>();
		Tiered.TIER_DATA.getTiers().forEach((id, attribute) -> {
			if(attribute.isValid(itemKey)) potentialAttributes.add(id);
		});
		if(potentialAttributes.size() > 0) return potentialAttributes.get(new Random().nextInt(potentialAttributes.size()));
		else return BLANK;
	}

	public static ResourceLocation getBlankAttributeIDFor(Item item) {
		ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
		PotentialAttribute chosen_tier = null;
		for (TierPool pool : Tiered.POOL_DATA.getPools().values()){
			if (pool == null)
				chosen_tier = getRandomFrom(Tiered.TIER_DATA.getTiers().values(), (a) -> a.isValid(itemKey) && a.getAttributes().size() == 0);
			else {
				List<PotentialAttribute> attris = new ArrayList<>();
				pool.getTiers().forEach((t) -> attris.add(Tiered.TIER_DATA.getTiers().get(ResourceLocation.parse(t))));
				attris.removeIf((attr) -> attr == null || attr.getAttributes().size() > 0);
				chosen_tier = getRandomFrom(attris, null);
			}
			if (chosen_tier != null) break;
		}
		if (chosen_tier != null) {
			if (false)
				return ResourceLocation.parse(chosen_tier.getID());
			else for (Entry<ResourceLocation, PotentialAttribute> cho : Tiered.TIER_DATA.getTiers().entrySet())
				if (cho.getValue() == chosen_tier) return cho.getKey();
		}

		List<ResourceLocation> potentialAttributes = new ArrayList<>();
		Tiered.TIER_DATA.getTiers().forEach((id, attribute) -> {
			if(attribute.isValid(itemKey)) potentialAttributes.add(id);
		});
		if(potentialAttributes.size() > 0) return potentialAttributes.get(new Random().nextInt(potentialAttributes.size()));
		else return BLANK;
	}

	private static <T> T getRandomFrom(Iterable<T> collection, Predicate<T> predicate) {
		List<T> list = new ArrayList<>();
		for (T item : collection) {
			if (predicate == null || predicate.test(item)) {
				list.add(item);
			}
		}
		if (list.isEmpty()) return null;
		return list.get(new Random().nextInt(list.size()));
	}

	private ModifierUtils() {
	}
}