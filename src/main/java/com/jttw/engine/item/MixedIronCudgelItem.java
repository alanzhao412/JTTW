package com.jttw.engine.item;

import com.jttw.engine.character.bulldemon.BullDemonAbilityHandler;
import com.jttw.engine.character.bulldemon.BullDemonCharacter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Mixed-Iron Cudgel, the Bull Demon King's weapon.
 *
 * <ul>
 *   <li>Anyone: a heavy, slow club.</li>
 *   <li>The Bull Demon King: hold right-click to raise it, let go to smash the ground. Held 1 /
 *       2 / 3 seconds, the shockwave runs 6 / 9 / 12 blocks, tossing all it passes; it breaks
 *       nothing.</li>
 * </ul>
 */
public class MixedIronCudgelItem extends SwordItem implements Charged {

    public static final int TICKS_PER_CHARGE = 20;
    public static final int COOLDOWN = 40;

    public MixedIronCudgelItem() {
        super(Tiers.NETHERITE, new Properties().rarity(Rarity.EPIC).fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -3.2f)));
    }

    /** Charge from ticks held: 0 (too short) to 3. */
    public static int charge(int heldTicks) {
        return Math.min(3, heldTicks / TICKS_PER_CHARGE);
    }

    @Override
    @Nullable
    public Charge charge(ItemStack stack, Player user, int ticks, java.util.Set<String> nodes) {
        // shown smoothly; it strikes at the whole steps it has reached (6 / 9 / 12 blocks)
        return new Charge(Math.min(1f, ticks / (3f * TICKS_PER_CHARGE)), charge(ticks) >= 1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BullDemonCharacter.is(player)) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR; // raised overhead
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !BullDemonCharacter.is(p)) return;
        int charge = charge(getUseDuration(stack, entity) - timeLeft);
        if (charge < 1) return;
        BullDemonAbilityHandler.startShockwave(p, charge);
        p.getCooldowns().addCooldown(this, COOLDOWN);
        p.swing(p.getUsedItemHand(), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A heavy club.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Bull Demon King:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" hold right-click and let go").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" to send a shockwave, 6 / 9 / 12").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" blocks by how long you held.").withStyle(ChatFormatting.GOLD));
    }
}
