package com.jttw.engine.item;

import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A ginseng fruit (人参果), from the tree in Zhenyuan's garden: shaped like a child not three days
 * old. Eaten, it makes whole; the first a soul eats lengthens its life for good (see
 * {@link Wuzhuang#eat}). Set down on the bare earth, it sinks into it; in water it dissolves
 * ({@link Wuzhuang#fruitTick}).
 */
public class GinsengFruitItem extends Item {

    private static final FoodProperties FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(1.0f).alwaysEdible().build();

    public GinsengFruitItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack left = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer p) Wuzhuang.eat(p);
        return left;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return Wuzhuang.fruitTick(entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.ginseng_fruit.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("The first eaten: +" + Wuzhuang.LONGEVITY_PERCENT + "% max health for good").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Sinks into bare earth; dissolves in water").withStyle(ChatFormatting.GRAY));
    }
}
