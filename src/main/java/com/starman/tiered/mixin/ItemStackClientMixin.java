package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.grammar.TierGrammarManager;
import com.starman.tiered.api.PotentialAttribute;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;

@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin implements DataComponentHolder {

    private ResourceLocation tiered$activeModifierId = null;

    @SuppressWarnings("rawtypes")
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;amount()D"), method = "addModifierTooltip")
    private void tiered$captureModifier(Consumer arg0, Player arg1, Holder arg2, AttributeModifier modifier, CallbackInfo ci) {
        tiered$activeModifierId = modifier.id();
    }

    @Redirect(
            method = "addModifierTooltip",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 1)
    )
    private MutableComponent tiered$colorizeTooltip(MutableComponent translatableText, ChatFormatting formatting) {
        ItemStack stack = (ItemStack)(Object)this;

        if (tiered$activeModifierId != null && tiered$activeModifierId.getNamespace().equals(Tiered.ID)) {
            if (Tiered.hasModifier(stack)) {
                ResourceLocation tier = stack.get(Tiered.MODIFIER);
                PotentialAttribute attribute = Tiered.TIER_DATA.getTiers().get(tier);

                if (attribute != null) {
                    return translatableText.setStyle(attribute.getStyle());
                }
            }
        }

        return translatableText.withStyle(formatting);
    }

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void tiered$modifyName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        if (this.get(DataComponents.CUSTOM_NAME) == null && Tiered.hasModifier(stack)) {
            ResourceLocation tier = stack.get(Tiered.MODIFIER);
            PotentialAttribute potentialAttribute = Tiered.TIER_DATA.getTiers().get(tier);

            if (potentialAttribute != null) {
                MutableComponent title;
                if (potentialAttribute.getLiteralName() != null) {
                    title = Component.literal(potentialAttribute.getLiteralName());
                } else {
                    String descriptionId = Util.makeDescriptionId("tier", Tiered.getKey(potentialAttribute));
                    String rawTranslation = I18n.exists(descriptionId) ? I18n.get(descriptionId) : descriptionId;

                    if (rawTranslation.contains("|")) {
                        String[] forms = rawTranslation.split("\\|");
                        int formIndex = TierGrammarManager.getIndexFor(tier, stack.getItem());
                        String selectedForm = forms[Math.min(formIndex, forms.length - 1)];
                        title = Component.literal(selectedForm);
                    } else {
                        title = Component.translatable(descriptionId);
                    }
                }

                Component originalItemName = cir.getReturnValue();
                Component finalItemName = TierGrammarManager.shouldLowercaseItemName()
                        ? tiered$lowerCaseFirstLetter(originalItemName)
                        : originalItemName;

                cir.setReturnValue(title.append(" ").append(finalItemName).setStyle(potentialAttribute.getStyle()));
            }
        }
    }

    private Component tiered$lowerCaseFirstLetter(Component component) {
        String text = component.getString();
        if (text == null || text.isEmpty()) return component;

        String lowerText = Character.toLowerCase(text.charAt(0)) + text.substring(1);
        MutableComponent modified = Component.literal(lowerText).setStyle(component.getStyle());
        for (Component sibling : component.getSiblings()) {
            modified.append(sibling);
        }
        return modified;
    }
}