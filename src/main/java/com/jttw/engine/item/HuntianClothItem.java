package com.jttw.engine.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Rarity;

/**
 * Huntian Cloth — Nezha's chestplate.
 * Provides magic defense (handled in NezhaAbilityHandler).
 * Uses leather base for minimal physical armor.
 */
public class HuntianClothItem extends ArmorItem {

    public HuntianClothItem() {
        super(ArmorMaterials.LEATHER, Type.CHESTPLATE,
                new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }
}
