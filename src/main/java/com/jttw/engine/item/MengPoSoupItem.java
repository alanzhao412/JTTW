package com.jttw.engine.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Meng Po's Soup (孟婆汤), the Soup of Forgetting, ladled by Granny Meng at the Bridge of
 * Helplessness to every soul going back to be reborn, so that it remembers nothing of its last
 * life. Drunk: every effect on the drinker, good and bad, is forgotten, and it reels a moment. The
 * bowl is left. King Zhuanlun of the Tenth Court leaves one.
 */
public class MengPoSoupItem extends Item {

    public static final int REELS = 60;

    public MengPoSoupItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    /** Drinks it: every effect forgotten, a moment's reeling. */
    public static void drink(LivingEntity drinker) {
        drinker.removeAllEffects();
        drinker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, REELS, 0));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            drink(entity);
            if (entity instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("item.jttw_engine.meng_po_soup.drunk"), true);
        }
        if (entity instanceof Player p && p.getAbilities().instabuild) return stack;
        stack.shrink(1);
        if (stack.isEmpty()) return new ItemStack(Items.BOWL);
        if (entity instanceof Player p && !p.getInventory().add(new ItemStack(Items.BOWL))) p.drop(new ItemStack(Items.BOWL), false);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.meng_po_soup.desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Drink: forget every effect on you").withStyle(ChatFormatting.AQUA));
    }
}
