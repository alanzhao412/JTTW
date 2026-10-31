package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.BlackWindRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * The Brocade Kasaya (锦襕袈裟), the Buddha's own robe that Guanyin gave the Tang Monk for his journey
 * (chapter 12): a patchwork of red silk framed in gold, a jade ring at its clasp. The abbot of the Guanyin
 * Chan Temple burned his own temple down for it, and the Black Bear Demon stole it out of the fire; it is
 * his treasure, and what he leaves when he is subdued. "Who wears it will not fall into the wheel of
 * rebirth; no fire can burn it, no poison or wild beast harm him." Worn over the chest: as diamond against
 * blows, tougher still; fire does not touch its wearer, and poison leaves him.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public class BrocadeKasayaItem extends ArmorItem {

    public BrocadeKasayaItem() {
        super(BlackWindRegistry.KASAYA_MATERIAL, Type.CHESTPLATE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .durability(Type.CHESTPLATE.getDurability(45)));
    }

    /** Whether {@code e} wears the kasaya. */
    public static boolean wears(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BrocadeKasayaItem;
    }

    /** No fire can burn its wearer. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && wears(event.getEntity())) event.setCanceled(true);
    }

    /** Nor poison stay in him; and he is never alight. */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide || e.tickCount % 10 != 0 || !wears(e)) return;
        if (e.hasEffect(MobEffects.POISON)) e.removeEffect(MobEffects.POISON);
        if (e.getRemainingFireTicks() > 0) e.clearFire();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.brocade_kasaya.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.jttw_engine.brocade_kasaya.power").withStyle(ChatFormatting.AQUA));
    }
}
