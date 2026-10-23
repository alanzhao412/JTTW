package com.jttw.engine.item;

import com.jttw.engine.world.EarthGod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Earth God's Luopan (土地罗盘), the geomancer's compass of wood and bronze that reads the ground:
 * used, it calls up the place's Earth God, who tells its holder where the demons of their strength
 * dwell (the zone for their level), which way and how far ({@link EarthGod}); held, its needle points
 * there, as a compass's does, updated every few seconds as its holder goes (in Hell, while its holder
 * sneaks, to the way back up instead of the Ten Courts). Each player is given one
 * on first joining a world; more are crafted from planks, a copper ingot and red dye.
 */
public class EarthGodLuopanItem extends Item {

    /** Between two callings of the Earth God. */
    public static final int COOLDOWN_TICKS = 60;
    /** How often a held luopan's needle is set again (as its holder's level and the nearest sea change). */
    public static final int AIM_EVERY = 40;

    public EarthGodLuopanItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        EarthGod.consult((ServerPlayer) player, stack);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || !(holder instanceof ServerPlayer player) || holder.tickCount % AIM_EVERY != 0) return;
        if (!selected && player.getOffhandItem() != stack) return;
        EarthGod.aim(stack, EarthGod.answer(player).target());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use: call up the Earth God, who tells where the demons of your strength dwell").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Held: its needle points the way").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("In Diyu, sneaking: the way back up to the living").withStyle(ChatFormatting.DARK_GRAY));
    }
}
