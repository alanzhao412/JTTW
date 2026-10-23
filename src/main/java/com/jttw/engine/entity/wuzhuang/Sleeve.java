package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import javax.annotation.Nullable;

/**
 * The Universe in the Sleeve (袖里乾坤): the Great Immortal Zhenyuan opens his wide sleeve, and
 * whatever stands before him is swept into it, as the pilgrims, horse and baggage and all, were
 * swept in. Inside it is a universe of its own: the one held there is out of the world (unseen, it
 * cannot be struck nor strike), blind, and held at his side wherever he goes, for {@value #HOLD}
 * ticks, then cast out, bruised (a tenth of their health). It can <b>struggle free</b> sooner:
 * {@value #STRUGGLES} blows at him from within, and it bursts out unhurt.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Sleeve {

    private Sleeve() {}

    public static final int HOLD = 100, STRUGGLES = 5;
    /** What being cast out costs, as a share of health. */
    public static final double BRUISE = 0.1;
    public static final String KEY_UNTIL = "jttw_sleeve_until", KEY_BY = "jttw_sleeve_by", KEY_STRUGGLES = "jttw_sleeve_struggles";

    /** Whether {@code e} is held in a sleeve now. */
    public static boolean isHeld(Entity e) {
        CompoundTag d = e.getPersistentData();
        return d.contains(KEY_UNTIL) && d.getLong(KEY_UNTIL) > e.level().getGameTime() - 40;
    }

    /** Who holds {@code e}, if anyone and they are here. */
    @Nullable
    public static ZhenyuanEntity holder(Entity e) {
        if (!isHeld(e) || !e.getPersistentData().hasUUID(KEY_BY) || !(e.level() instanceof ServerLevel server)) return null;
        return server.getEntity(e.getPersistentData().getUUID(KEY_BY)) instanceof ZhenyuanEntity z && z.isAlive() ? z : null;
    }

    /** How many blows it has struck from within. */
    public static int struggles(Entity e) {
        return e.getPersistentData().getInt(KEY_STRUGGLES);
    }

    /** Whether {@code e} could be swept in: alive, not already held, not of the temple, not a creative player. */
    public static boolean canHold(LivingEntity e) {
        if (!e.isAlive() || isHeld(e) || e instanceof TempleFolk) return false;
        return !(e instanceof Player p) || !(p.isCreative() || p.isSpectator());
    }

    /** Sweeps {@code e} into {@code by}'s sleeve. Returns whether it was. */
    public static boolean hold(ZhenyuanEntity by, LivingEntity e) {
        if (!canHold(e)) return false;
        CompoundTag d = e.getPersistentData();
        d.putLong(KEY_UNTIL, by.level().getGameTime() + HOLD);
        d.putUUID(KEY_BY, by.getUUID());
        d.putInt(KEY_STRUGGLES, 0);
        e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, HOLD + 5, 0, false, false), by);
        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, HOLD + 20, 0, false, false), by);
        pin(by, e);
        if (e instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("entity.jttw_engine.zhenyuan.sleeve_held"), true);
        if (by.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY(0.5), e.getZ(), 40, 0.4, 0.6, 0.4, 0.2);
        }
        by.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.5f);
        return true;
    }

    /** Where the held are kept: in his sleeve, at his side. */
    public static Vec3 sleevePoint(ZhenyuanEntity by) {
        Vec3 side = Vec3.directionFromRotation(0, by.yBodyRot - 90).scale(0.7);
        return by.position().add(side.x, 0.3, side.z);
    }

    /** Keeps {@code e} where the sleeve holds it. */
    public static void pin(ZhenyuanEntity by, LivingEntity e) {
        Vec3 at = sleevePoint(by);
        if (e instanceof ServerPlayer sp) sp.connection.teleport(at.x, at.y, at.z, sp.getYRot(), sp.getXRot()); // the client moves players
        e.setPos(at.x, at.y, at.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0;
    }

    /**
     * Lets {@code e} out: struggled free, it bursts out beside him unhurt; held to the end, it is cast
     * out and bruised. Without a holder (he is gone), it is simply let go where it is.
     */
    public static void release(LivingEntity e, @Nullable ZhenyuanEntity by, boolean struggledFree) {
        CompoundTag d = e.getPersistentData();
        d.remove(KEY_UNTIL);
        d.remove(KEY_BY);
        d.remove(KEY_STRUGGLES);
        e.removeEffect(MobEffects.INVISIBILITY);
        e.removeEffect(MobEffects.BLINDNESS);
        if (by == null) return;
        by.letGo(e);
        Vec3 look = by.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = by.position().add(look.scale(2.2)).add(0, 0.2, 0);
        if (e instanceof ServerPlayer sp) sp.connection.teleport(at.x, at.y, at.z, sp.getYRot(), sp.getXRot());
        e.setPos(at.x, at.y, at.z);
        e.push(look.x * 0.9, 0.35, look.z * 0.9);
        e.hurtMarked = true;
        if (!struggledFree) {
            e.invulnerableTime = 0;
            e.hurt(by.damageSources().indirectMagic(by, by), (float) (e.getMaxHealth() * BRUISE));
        }
        if (e instanceof ServerPlayer p) p.displayClientMessage(Component.translatable(struggledFree
                ? "entity.jttw_engine.zhenyuan.sleeve_burst" : "entity.jttw_engine.zhenyuan.sleeve_cast_out"), true);
        if (by.level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 1, at.z, 20, 0.4, 0.5, 0.4, 0.05);
        by.playSound(struggledFree ? SoundEvents.WOOL_BREAK : SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.8f);
    }

    /** {@code e} strikes at its holder from within. Returns whether it burst free. */
    public static boolean struggle(LivingEntity e) {
        ZhenyuanEntity by = holder(e);
        if (by == null) return false;
        int n = struggles(e) + 1;
        e.getPersistentData().putInt(KEY_STRUGGLES, n);
        by.playSound(SoundEvents.WOOL_HIT, 1f, 0.7f + n * 0.1f);
        if (n < STRUGGLES) return false;
        release(e, by, true);
        by.stagger();
        return true;
    }

    // ── The held, out of the world ──────────────────────────────────────

    /** A blow at him from within is a struggle; at anything else, nothing. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player p = event.getEntity();
        if (!isHeld(p)) return;
        event.setCanceled(true);
        if (!p.level().isClientSide && event.getTarget() == holder(p)) struggle(p);
    }

    /** Nothing in the world touches the held, and the held touch nothing in it. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        Entity attacker = event.getSource().getEntity();
        if (isHeld(event.getEntity()) && !(attacker instanceof ZhenyuanEntity)) event.setCanceled(true);
        else if (attacker != null && isHeld(attacker)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isHeld(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isHeld(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(PlayerInteractEvent.LeftClickBlock event) {
        if (isHeld(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (isHeld(event.getEntity())) event.setCanceled(true);
    }

    /** One held by someone no longer here (gone, dead, unloaded) is let go. */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide) return;
        if (!e.getPersistentData().contains(KEY_UNTIL)) return;
        ZhenyuanEntity by = holder(e);
        if (by == null || !isHeld(e)) release(e, null, true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getPersistentData().contains(KEY_UNTIL)) release(event.getEntity(), null, true);
    }
}
