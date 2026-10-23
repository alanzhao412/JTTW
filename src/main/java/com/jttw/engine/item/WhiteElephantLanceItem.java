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
 * The White Elephant's lance (白象长枪), from the second demon king of Lion-Camel City: long and
 * heavy (+{@value #DAMAGE_PERCENT}% damage), it reaches {@value #REACH} blocks further than an arm
 * and throws back what it strikes. Anyone may wield it.
 */
public class WhiteElephantLanceItem extends Item {

    public static final int DAMAGE_PERCENT = 50;
    public static final double REACH = 1.5, KNOCKBACK = 1.0;

    public WhiteElephantLanceItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(JTTWEngine.id("white_elephant_lance_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(JTTWEngine.id("white_elephant_lance_speed"),
                        -2.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(JTTWEngine.id("white_elephant_lance_reach"),
                        REACH, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(JTTWEngine.id("white_elephant_lance_knockback"),
                        KNOCKBACK, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.white_elephant_lance.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("+" + REACH + " blocks of reach; throws back what it strikes").withStyle(ChatFormatting.YELLOW));
    }
}
