package com.jttw.engine.item;

import net.minecraft.world.item.ItemStack;

/**
 * One of a matched pair of blades (Nezha's Yin-Yang Swords, Lady Earth Flow's twin swords): held
 * in the main hand, it draws its partner from the inventory into the off hand, and when put away
 * the partner goes back ({@link WieldRules#pairSwords}).
 */
public interface PairedBlade {

    /** Whether {@code other} is the partner of {@code self} (this item's stack). */
    boolean pairsWith(ItemStack self, ItemStack other);

    /** Whether a living thing holds a blade of a pair in one hand and its partner in the other. */
    static boolean paired(net.minecraft.world.entity.LivingEntity holder) {
        ItemStack main = holder.getMainHandItem(), off = holder.getOffhandItem();
        return main.getItem() instanceof PairedBlade blade && blade.pairsWith(main, off);
    }
}
