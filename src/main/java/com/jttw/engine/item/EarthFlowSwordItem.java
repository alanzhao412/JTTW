package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.List;

/**
 * Lady Earth Flow's twin swords (雌雄双股剑), the male blade and the female, a matched pair: she
 * fought Sun Wukong, Zhu Bajie and Sha Wujing with them at once. Either one is a quick sword
 * (+{@value #DAMAGE_PERCENT}% damage); held as a pair ({@link PairedBlade}: one in the main hand
 * draws the other into the off hand), every cut is followed by a second from the off hand, for
 * {@value #SECOND_CUT_PERCENT}% of the first, and the female blade's cut slows what it strikes.
 * Anyone may wield them: they are no god's.
 */
public class EarthFlowSwordItem extends Item implements PairedBlade {

    public static final int DAMAGE_PERCENT = 35, SECOND_CUT_PERCENT = 40;
    /** How long the female blade's cut slows, in ticks. */
    public static final int SLOW_TICKS = 40;

    private final boolean male;

    public EarthFlowSwordItem(boolean male) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.male = male;
    }

    public boolean male() {
        return male;
    }

    @Override
    public boolean pairsWith(ItemStack self, ItemStack other) {
        return other.getItem() instanceof EarthFlowSwordItem o && o.male != male;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        String side = male ? "male" : "female";
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(id("earth_flow_" + side + "_atk"),
                        DAMAGE_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(id("earth_flow_" + side + "_speed"),
                        -2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static ResourceLocation id(String path) {
        return JTTWEngine.id(path);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!male) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0), attacker);
        if (!attacker.level().isClientSide && PairedBlade.paired(attacker) && target.isAlive()) secondCut(target, attacker);
        return true;
    }

    /** The off hand's cut, straight after the main hand's. */
    public static void secondCut(LivingEntity target, LivingEntity attacker) {
        float damage = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * SECOND_CUT_PERCENT / 100.0);
        target.invulnerableTime = 0;
        target.hurt(attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker), damage);
        attacker.swing(InteractionHand.OFF_HAND, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.earth_flow_sword.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("+" + DAMAGE_PERCENT + "% damage").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("As a pair: every cut is followed by a second (" + SECOND_CUT_PERCENT + "%)").withStyle(ChatFormatting.YELLOW));
        if (!male) tooltip.add(Component.literal("Its cut slows").withStyle(ChatFormatting.AQUA));
    }
}
