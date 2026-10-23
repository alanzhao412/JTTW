package com.jttw.engine.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A relic of one of the Ten Courts of Hell, left by its king: the Shard of the Karma Mirror, the
 * Rime Shackle, a page of the Book of Life and Death and the rest. A forging material with its
 * story on it (lang key {@code item.jttw_engine.<id>.desc}).
 */
public class HellRelicItem extends Item {

    public HellRelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        tooltip.add(Component.translatable("item.jttw_engine." + id + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
