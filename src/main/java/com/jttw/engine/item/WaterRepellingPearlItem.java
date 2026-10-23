package com.jttw.engine.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Water-Repelling Pearl (避水珠): carried anywhere about one, it parts the sea round its bearer,
 * so they breathe under it and see and work there as though in air (Conduit Power while in water;
 * see SeaBreath). Crafted from a Heart of the Sea set in prismarine crystals and gold, so a player
 * can carry one down into the Mystical Waters' trenches before ever meeting their creatures.
 */
public class WaterRepellingPearlItem extends Item {

    public WaterRepellingPearlItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    /** Whether {@code p} carries a pearl anywhere about them (inventory, hands, armour). */
    public static boolean carries(Player p) {
        return p.getInventory().hasAnyMatching(s -> s.getItem() instanceof WaterRepellingPearlItem);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.water_repelling_pearl.desc").withStyle(ChatFormatting.AQUA));
    }
}
