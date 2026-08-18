package com.starman.tiered.mixin;

import com.starman.tiered.Tiered;
import com.starman.tiered.grammar.TierGrammarManager;
import com.starman.tiered.api.PotentialAttribute;
import com.starman.tiered.util.TierHelper;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin implements DataComponentHolder {

    private boolean isTiered = false;

    @SuppressWarnings("rawtypes")
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;amount()D"), method = "addModifierTooltip")
    private void storeAttributeModifier(Consumer arg0, Player arg1, Holder arg2, AttributeModifier pModfier, CallbackInfo ci) {
        isTiered = pModfier.id().toString().contains("tiered_");
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 1), method = "addModifierTooltip")
    private MutableComponent getTextFormatting(MutableComponent translatableText, ChatFormatting formatting) {
        if(TierHelper.hasModifier((ItemStack)(Object)this) && isTiered) {
            ResourceLocation tier = ((ItemStack)(Object)this).get(Tiered.MODIFIER);
            PotentialAttribute attribute = Tiered.TIER_DATA.getTiers().get(tier);

            return translatableText.setStyle(attribute.getStyle());
        } else {
            return translatableText.withStyle(formatting);
        }
    }

    @Inject(
            method = "getHoverName",
            at = @At("RETURN"),
            cancellable = true
    )
    private void modifyName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        if(this.get(DataComponents.CUSTOM_NAME) == null && TierHelper.hasModifier(stack)) {
            ResourceLocation tier = stack.get(Tiered.MODIFIER);

            PotentialAttribute potentialAttribute = Tiered.TIER_DATA.getTiers().get(tier);

            if(potentialAttribute != null) {
                MutableComponent title;
                if (potentialAttribute.getLiteralName() != null) {
                    title = Component.literal(potentialAttribute.getLiteralName());
                } else {
                    String descriptionId = Util.makeDescriptionId("tier", TierHelper.getKey(potentialAttribute));

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
                Component finalItemName;

                if (TierGrammarManager.shouldLowercaseItemName()) {
                    finalItemName = lowerCaseFirstLetterComponent(originalItemName);
                } else {
                    finalItemName = originalItemName;
                }

                cir.setReturnValue(title.append(" ").append(finalItemName).setStyle(potentialAttribute.getStyle()));
            }
        }
    }

    private Component lowerCaseFirstLetterComponent(Component component) {
        String text = component.getString();
        if (text == null || text.isEmpty()) {
            return component;
        }

        String lowerText = Character.toLowerCase(text.charAt(0)) + text.substring(1);

        MutableComponent modified = Component.literal(lowerText).setStyle(component.getStyle());
        for (Component sibling : component.getSiblings()) {
            modified.append(sibling);
        }

        return modified;
    }
}