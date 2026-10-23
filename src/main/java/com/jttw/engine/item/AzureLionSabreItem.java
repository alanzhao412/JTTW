package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Azure Lion's broadsword (青狮宝刀), from the eldest demon king of Lion-Camel City: a great
 * curved blade, heavy and slow (+{@value #DAMAGE_PERCENT}% damage). Its swing cleaves: what stands
 * within {@value #CLEAVE_REACH} blocks of the one it strikes, on the far side from its wielder, is
 * cut too, for {@value #CLEAVE_PERCENT}% of the blow (never one the wielder is allied to). Anyone
 * may wield it.
 */
public class AzureLionSabreItem extends Item {

    public static final int DAMAGE_PERCENT = 60, CLEAVE_PERCENT = 45;
    public static final double CLEAVE_REACH = 2.5;

    public AzureLionSabreItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(JTTWEngine.id("azure_lion_sabre_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(JTTWEngine.id("azure_lion_sabre_speed"),
                        -2.8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) cleave(target, attacker);
        return true;
    }

    /** The cleave beyond {@code target}. Returns who it cut. */
    public static List<LivingEntity> cleave(LivingEntity target, LivingEntity attacker) {
        float blow = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * CLEAVE_PERCENT / 100.0);
        Vec3 away = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        List<LivingEntity> cut = attacker.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(CLEAVE_REACH),
                e -> e != target && e != attacker && e.isAlive() && !e.isAlliedTo(attacker) && !attacker.isAlliedTo(e)
                        && e.distanceToSqr(target) <= CLEAVE_REACH * CLEAVE_REACH
                        && e.position().subtract(attacker.position()).multiply(1, 0, 1).dot(away) > 0
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : cut) {
            e.hurt(attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker), blow);
        }
        return cut;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.azure_lion_sabre.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Cleaves what stands beyond its mark (" + CLEAVE_PERCENT + "%)").withStyle(ChatFormatting.YELLOW));
    }
}
