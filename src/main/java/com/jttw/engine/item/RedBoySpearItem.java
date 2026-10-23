package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Red Boy's own fire-tipped spear (红孩儿的火尖枪), the one he fought the Monkey King with on Mount
 * Haohao (chapters 40–41): a slender lacquered shaft ringed with gold, a red tassel, a gilded head.
 * His, not Nezha's (Nezha's spear is {@link FireTipSpearItem}, with its fire and its fireball): this
 * one is only a good spear, +{@value #DAMAGE_PERCENT}% damage, a spear's pace. No use, nothing on a
 * hit. Anyone may wield it.
 */
public class RedBoySpearItem extends Item {

    public static final int DAMAGE_PERCENT = 45;
    /** What it takes from its bearer's attack speed (4 a second bare-handed: 1.4 with it). */
    public static final double SPEED = -2.6;

    public RedBoySpearItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(JTTWEngine.id("red_boy_spear_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(JTTWEngine.id("red_boy_spear_speed"),
                        SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.red_boy_spear.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage").withStyle(ChatFormatting.YELLOW));
    }
}
