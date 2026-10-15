package com.jttw.engine.item;

import com.jttw.engine.character.peng.PengAbilityHandler;
import com.jttw.engine.character.peng.PengCharacter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Yin-Yang Two-Qi Vase, in which the Gold-Winged Peng's brothers caught the Monkey King:
 * whoever answers their name inside it melts away within the hour.
 *
 * <ul>
 *   <li>Anyone: right-click a creature to seal it inside, right-click a block to let it out
 *       there. One at a time; never players or bosses.</li>
 *   <li>The Peng: what he seals dissolves while it is inside, and when he lets it out it bursts
 *       in a yin-yang blast. He alone can catch players and bosses: held fast for 5 seconds,
 *       dissolving, before they burst free.</li>
 * </ul>
 */
public class YinYangVaseItem extends Item {

    public static final String TAG_SEALED = "jttw_vase_sealed";
    public static final String TAG_SEALED_AT = "jttw_vase_sealed_at";
    public static final String TAG_SEALED_NAME = "jttw_vase_sealed_name";
    /** Set when the Peng sealed it: it dissolves inside. */
    public static final String TAG_DISSOLVING = "jttw_vase_dissolving";
    public static final int JAIL_COOLDOWN = 200;

    public YinYangVaseItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static boolean holds(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_SEALED);
    }

    /** Players and bosses are too great for the vase, but not for the Peng's hold. */
    public static boolean tooGreat(LivingEntity e) {
        return e instanceof Player || !(e instanceof Mob) || e.getType().is(Tags.EntityTypes.BOSSES);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;
        boolean peng = PengCharacter.is(sp);
        if (tooGreat(target)) {
            if (!peng || PengAbilityHandler.isJailed(target) || sp.getCooldowns().isOnCooldown(this)) {
                if (!peng) sp.displayClientMessage(Component.literal("§7Too great for the vase."), true);
                return InteractionResult.FAIL;
            }
            PengAbilityHandler.jail(sp, target);
            sp.getCooldowns().addCooldown(this, JAIL_COOLDOWN);
            return InteractionResult.CONSUME;
        }
        if (holds(stack)) {
            sp.displayClientMessage(Component.literal("§7The vase already holds "
                    + stack.getTag().getString(TAG_SEALED_NAME) + "."), true);
            return InteractionResult.FAIL;
        }
        CompoundTag saved = new CompoundTag();
        if (!target.saveAsPassenger(saved)) return InteractionResult.FAIL;
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_SEALED, saved);
        tag.putLong(TAG_SEALED_AT, sp.level().getGameTime());
        tag.putString(TAG_SEALED_NAME, target.getName().getString());
        tag.putBoolean(TAG_DISSOLVING, peng);
        ServerLevel level = sp.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                30, target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, 0.1);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1f, 0.7f);
        target.ejectPassengers();
        target.stopRiding();
        target.discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!holds(stack)) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        release(stack, (ServerLevel) level, Vec3.atBottomCenterOf(at), context.getPlayer() instanceof ServerPlayer sp ? sp : null);
        return InteractionResult.CONSUME;
    }

    /**
     * Lets out what the vase holds at {@code at}. What the Peng sealed has been dissolving and may
     * be gone; when the Peng lets it out, it bursts. Returns the released creature, or null.
     */
    @Nullable
    public static Entity release(ItemStack stack, ServerLevel level, Vec3 at, @Nullable ServerPlayer by) {
        CompoundTag tag = stack.getTag();
        CompoundTag saved = tag.getCompound(TAG_SEALED);
        long sealedFor = level.getGameTime() - tag.getLong(TAG_SEALED_AT);
        boolean dissolving = tag.getBoolean(TAG_DISSOLVING);
        tag.remove(TAG_SEALED);
        tag.remove(TAG_SEALED_AT);
        tag.remove(TAG_SEALED_NAME);
        tag.remove(TAG_DISSOLVING);

        Entity released = EntityType.loadEntityRecursive(saved, level, e -> {
            e.moveTo(at.x, at.y, at.z, e.getYRot(), e.getXRot());
            return e;
        });
        if (released instanceof LivingEntity living && dissolving) {
            float left = living.getHealth() - PengAbilityHandler.DISSOLVE_PER_SECOND * living.getMaxHealth() * (sealedFor / 20f);
            if (left <= 0) {
                level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, 20, 0.3, 0.5, 0.3, 0.02);
                if (by != null) by.displayClientMessage(Component.literal("§6" + living.getName().getString() + " dissolved in the vase."), true);
                released = null;
            } else {
                living.setHealth(left);
            }
        }
        if (released != null) level.tryAddFreshEntityWithPassengers(released);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1f, 0.7f);
        if (by != null && PengCharacter.is(by)) PengAbilityHandler.yinYangBlast(by, at);
        return released;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return holds(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (holds(stack)) {
            tooltip.add(Component.literal("Holds: " + stack.getTag().getString(TAG_SEALED_NAME)).withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(Component.literal("Right-click a creature to seal it;").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("right-click a block to let it out.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Gold-Winged Peng:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" the sealed dissolve; released,").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" they burst. Holds even players").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" and bosses, for 5 seconds.").withStyle(ChatFormatting.GOLD));
    }
}
