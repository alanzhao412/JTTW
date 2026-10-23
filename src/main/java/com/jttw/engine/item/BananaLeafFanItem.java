package com.jttw.engine.item;

import com.jttw.engine.character.bulldemon.BullDemonAbilityHandler;
import com.jttw.engine.character.bulldemon.BullDemonCharacter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Banana-Leaf Fan of Princess Iron Fan, the Bull Demon King's wife; one wave of it blew the
 * Monkey King fifty thousand li.
 *
 * <ul>
 *   <li>Anyone: a gust that throws back everything in front, within {@value #RANGE} blocks.</li>
 *   <li>The Bull Demon King: within {@value #BULL_RANGE} blocks, and what it fans is blown away
 *       out of sight, landing as far as 1000 blocks off; the stronger he is, the further.</li>
 *   <li>Either: the fan that puts out the Flaming Mountains: each wave quenches the burning earth and
 *       the fire in its sweep for a minute (see {@link com.jttw.engine.world.fire.FireMountain#quench}),
 *       out to 12 blocks, 20 in the Bull Demon King's hand. Not the True Samadhi Fire.</li>
 * </ul>
 */
public class BananaLeafFanItem extends Item {

    public static final double RANGE = 6;
    public static final double BULL_RANGE = 10;
    public static final int COOLDOWN = 60;
    public static final int BULL_COOLDOWN = 200;

    public BananaLeafFanItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        boolean bull = BullDemonCharacter.is(player);
        fan((ServerPlayer) player);
        player.getCooldowns().addCooldown(this, bull ? BULL_COOLDOWN : COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    /** One wave of the fan. Returns everything it caught. */
    public static List<LivingEntity> fan(ServerPlayer p) {
        boolean bull = BullDemonCharacter.is(p);
        double range = bull ? BULL_RANGE : RANGE;
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        List<LivingEntity> caught = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range), e -> {
            if (e == p || !e.isAlive()) return false;
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            return to.length() <= range && to.normalize().dot(look) >= 0.5; // a 120-degree sweep
        });
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        double reach = BullDemonAbilityHandler.fanReach(p);
        for (LivingEntity e : caught) {
            if (bull) {
                BullDemonAbilityHandler.blowAway(p, e, flat, reach);
            } else {
                e.push(flat.x * 1.5, 0.4, flat.z * 1.5);
                e.hurtMarked = true;
            }
        }
        ServerLevel level = p.serverLevel();
        // and it puts out the mountain's fire
        com.jttw.engine.world.fire.FireMountain.quench(level, eye, look,
                bull ? com.jttw.engine.world.fire.FireMountain.BULL_QUENCH_REACH : com.jttw.engine.world.fire.FireMountain.QUENCH_REACH);
        for (int i = 1; i <= range; i++) {
            Vec3 at = eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y - 0.4, at.z, bull ? 6 : 3, i * 0.12, 0.2, i * 0.12, 0.05);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, bull ? 0.6f : 1.4f);
        return caught;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A gust that throws back").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("everything in front.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Puts out burning ground").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("and fire in its sweep a while.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Bull Demon King:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" blows them away, up to").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" 1000 blocks by his strength.").withStyle(ChatFormatting.GOLD));
    }
}
