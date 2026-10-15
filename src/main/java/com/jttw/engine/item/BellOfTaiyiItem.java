package com.jttw.engine.item;

import com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler;
import com.jttw.engine.character.ninespirits.NineSpiritsCharacter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Bell of Taiyi, the bell Taiyi Tianzun hung on his lion. In the story the Grand Saint of
 * Nine Spirits obeyed only when his master called him by name.
 *
 * <ul>
 *   <li>Anyone: ringing it freezes the creatures within {@value #RANGE} blocks for 2 seconds.</li>
 *   <li>The Grand Saint: within {@value #SAINT_RANGE} blocks, for 3 seconds, and the rung take
 *       extra damage from his elemental heads while it lasts.</li>
 * </ul>
 * 20-second cooldown.
 */
public class BellOfTaiyiItem extends Item {

    public static final double RANGE = 6;
    public static final double SAINT_RANGE = 16;
    public static final int FREEZE_TICKS = 40;
    public static final int SAINT_FREEZE_TICKS = 60;
    public static final int COOLDOWN_TICKS = 400;

    public BellOfTaiyiItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ring((ServerPlayer) player);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    /** Rings the bell: freezes everything near, longer and wider for the Grand Saint. */
    public static int ring(ServerPlayer player) {
        boolean saint = NineSpiritsCharacter.is(player);
        double range = saint ? SAINT_RANGE : RANGE;
        int ticks = saint ? SAINT_FREEZE_TICKS : FREEZE_TICKS;
        ServerLevel level = player.serverLevel();
        long until = level.getGameTime() + ticks;
        int rung = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player) && e.distanceTo(player) <= range)) {
            // Held still: cannot move, jump or strike
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true), player);
            e.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 250, false, false), player);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 9, false, true), player);
            if (e instanceof Mob mob) mob.setTarget(null);
            if (saint) e.getPersistentData().putLong(NineSpiritsAbilityHandler.KEY_BELL_MARK, until);
            level.sendParticles(ParticleTypes.NOTE, e.getX(), e.getEyeY() + 0.5, e.getZ(), 1, 0, 0, 0, 0);
            rung++;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, saint ? 0.6F : 1.0F);
        return rung;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Taiyi Tianzun's bell").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Right-click: everything within 6 blocks freezes for 2 s").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Grand Saint of Nine Spirits: 16 blocks, 3 s,").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("and his heads hit the rung harder").withStyle(ChatFormatting.YELLOW));
    }
}
