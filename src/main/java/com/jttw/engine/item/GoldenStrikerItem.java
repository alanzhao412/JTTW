package com.jttw.engine.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Golden Striker (金击子), a rod of gold two feet long with a knob at its end, kept in Zhenyuan's
 * temple to knock down the ginseng fruit, which falls whole only to gold. A gold weapon of a sort
 * (it counts as gold for the fruit, as any golden tool does). Clear Wind leaves it when struck down.
 */
public class GoldenStrikerItem extends SwordItem {

    public GoldenStrikerItem() {
        super(Tiers.GOLD, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(Tiers.GOLD, 2, -2.2f)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.golden_striker.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Gold: a ripe ginseng fruit struck with it falls whole").withStyle(ChatFormatting.AQUA));
    }
}
