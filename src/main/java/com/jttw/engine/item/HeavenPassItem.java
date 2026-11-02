package com.jttw.engine.item;

import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.world.heaven.HeavenGate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Heaven Pass (天门通牒), a jade tally that opens Heaven's gates to any traveller, of any level: used
 * below, it carries its holder up on a cloud to an island of Heaven whose host is of their own level
 * ({@link HeavenGate#up}); used in Heaven, it carries them home ({@link HeavenGate#down}). It is kept, with
 * {@value #COOLDOWN_TICKS} ticks between uses. Crafted from paper, a gold nugget, a feather and white wool.
 * (Sun Wukong's and the Dragon's realm leaps go to Heaven too, as ever; this is the way up for everyone else.)
 */
public class HeavenPassItem extends Item {

    /** Between two uses: 30 s. */
    public static final int COOLDOWN_TICKS = 600;

    public HeavenPassItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer p = (ServerPlayer) player;
        boolean inHeaven = level.dimension().equals(JTTWDimensions.HEAVEN);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, inHeaven ? 0.7f : 1.3f);
        boolean went = inHeaven ? HeavenGate.down(p) : HeavenGate.up(p);
        if (!went) return InteractionResultHolder.fail(stack);
        p.displayClientMessage(Component.literal(inHeaven ? "§7The cloud carries you down to the world below."
                : "§eA cloud rises under you and carries you up through Heaven's gates."), true);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use: up to the clouds of Heaven whose host is of your level").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("In Heaven: home to the world below").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Heaven's host attacks demons and the wicked on sight").withStyle(ChatFormatting.DARK_GRAY));
    }
}
