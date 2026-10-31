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
 * The Six-Eared Macaque's iron staff (六耳猕猴的铁棒): his copy of the Monkey King's As-You-Will
 * Gold-Banded Staff, so like it that it rang the same against it for a hundred bouts (chapter 58),
 * but only an iron staff banded in gold, that neither grows nor shrinks. The best plain weapon there
 * is, as he is the hardest foe: +{@value #DAMAGE_PERCENT}% damage, a staff's quick pace, a long reach.
 * Nothing more: no use, nothing on a hit. Anyone may wield it.
 */
public class MacaqueStaffItem extends Item {

    public static final int DAMAGE_PERCENT = 60;
    /** What it takes from its bearer's attack speed (4 a second bare-handed: 1.6 with it). */
    public static final double SPEED = -2.4;
    /** How much farther it reaches than a hand. */
    public static final double REACH = 1.0;

    public MacaqueStaffItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(JTTWEngine.id("macaque_staff_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(JTTWEngine.id("macaque_staff_speed"),
                        SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(JTTWEngine.id("macaque_staff_reach"),
                        REACH, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.macaque_staff.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage, +" + (int) REACH + " reach").withStyle(ChatFormatting.YELLOW));
    }
}
