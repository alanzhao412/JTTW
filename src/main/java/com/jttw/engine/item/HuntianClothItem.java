package com.jttw.engine.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Rarity;

/**
 * Huntian Cloth — Nezha's chestplate.
 * Leather-grade physical armor for anyone; on Nezha it also gives
 * {@value #MAGIC_RESISTANCE} magic resistance (applied in NezhaAbilityHandler).
 */
public class HuntianClothItem extends ArmorItem {

    public static final double MAGIC_RESISTANCE = 25.0;

    public HuntianClothItem() {
        super(ArmorMaterials.LEATHER, Type.CHESTPLATE,
                new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }
}
