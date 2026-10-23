package com.jttw.engine.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The silk-lined tray (丹盘, its cinnabar lacquer lined with a silk cloth) in which the youths caught
 * the ginseng fruit they struck down with the Golden Striker. Held under a falling fruit, it catches
 * it before it touches the earth. Bright Moon leaves one when struck down; one can be made of gold and wool.
 */
public class SilkLinedTrayItem extends Item {

    public SilkLinedTrayItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.silk_lined_tray.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Hold it under a falling ginseng fruit to catch it").withStyle(ChatFormatting.AQUA));
    }
}
