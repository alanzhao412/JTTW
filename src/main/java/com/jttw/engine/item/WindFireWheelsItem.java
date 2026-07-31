package com.jttw.engine.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Rarity;

/**
 * Wind and Fire Wheels — Nezha's boots.
 * Required for flight. Provides armor + movement speed.
 */
public class WindFireWheelsItem extends ArmorItem {

    public WindFireWheelsItem() {
        super(ArmorMaterials.NETHERITE, Type.BOOTS,
                new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }
}
