package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * King Yama's Vermilion Brush of Judgement (朱笔): with it he writes the sentences of the dead.
 * Wielded, it strikes for +{@value #DAMAGE_PERCENT}% damage, and what it strikes is <b>judged</b>:
 * it glows and is weakened for a few seconds.
 */
public class JudgeBrushItem extends Item {

    public static final int DAMAGE_PERCENT = 40, JUDGED = 80;

    public JudgeBrushItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(JTTWEngine.id("judge_brush_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(JTTWEngine.id("judge_brush_speed"),
                        -2.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, JUDGED, 0), attacker);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, JUDGED, 0), attacker);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.yama_brush.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("What it strikes is judged: it glows and is weakened").withStyle(ChatFormatting.AQUA));
    }
}
