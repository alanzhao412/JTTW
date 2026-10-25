package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import javax.annotation.Nullable;

/**
 * The full stops: what an ability means when it says its mark is <b>stunned</b>, or <b>pinned</b>
 * (rooted, frozen in place, held, kneeling, "can't move"). Never a heavy Slowness that leaves a
 * quarter of a step: either holds its mark where it stands.
 * <ul>
 *   <li><b>Pinned</b> ({@link ModEffects#PINNED}): it cannot walk, swim, fly or jump, and a
 *       creature's path is dropped each tick; it may still strike what is within its reach.</li>
 *   <li><b>Stunned</b> ({@link ModEffects#STUNNED}): pinned, and it cannot strike either: a creature
 *       lets go of its target and takes up no other, and no blow of its own (nor its arrow, nor its
 *       ability) lands while it lasts.</li>
 * </ul>
 * Both work on players too (the client is told its speed and jump are nothing). Neither stops what
 * pushes them: a blow's knockback or a wave still carries them, and they stand where they land.
 * Each is an ordinary effect: when it ends, its hold on speed and jump ends with it.
 *
 * <p>Bosses (a {@link ZoneBoss}, or any of the {@link Tags.EntityTypes#BOSSES}) shrug it off in
 * half the time.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Stun {

    private Stun() {}

    /** Stuns {@code target} for {@code ticks} (half for a boss): it cannot move nor strike. */
    public static void stun(LivingEntity target, int ticks, @Nullable Entity source) {
        hold(target, ModEffects.STUNNED, ticks, source);
        if (target instanceof Mob m) m.setTarget(null);
    }

    /** Pins {@code target} for {@code ticks} (half for a boss): it cannot move, but may strike what is in reach. */
    public static void pin(LivingEntity target, int ticks, @Nullable Entity source) {
        hold(target, ModEffects.PINNED, ticks, source);
    }

    private static void hold(LivingEntity target, Holder<MobEffect> effect, int ticks, @Nullable Entity source) {
        int lasts = isBoss(target) ? Math.max(1, ticks / 2) : ticks;
        target.addEffect(new MobEffectInstance(effect, lasts, 0, false, true), source);
    }

    /** Whether {@code e} shrugs a stun or pin off in half the time. */
    public static boolean isBoss(LivingEntity e) {
        return e instanceof ZoneBoss || e.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** Whether {@code e} cannot move now (stunned or pinned). */
    public static boolean held(LivingEntity e) {
        return e.hasEffect(ModEffects.STUNNED) || e.hasEffect(ModEffects.PINNED);
    }

    /** Whether {@code e} is stunned now: it cannot strike. */
    public static boolean stunned(@Nullable Entity e) {
        return e instanceof LivingEntity l && l.hasEffect(ModEffects.STUNNED);
    }

    // ── The effects ─────────────────────────────────────────────────────

    /** Stunned or pinned: no speed afoot, afloat or awing, no jump; a creature's path dropped each tick. */
    public static final class Held extends MobEffect {
        private final boolean stuns;

        public Held(String name, int colour, boolean stuns) {
            super(MobEffectCategory.HARMFUL, colour);
            this.stuns = stuns;
            // x(1 - 1): every other bonus to these is multiplied by nothing
            AttributeModifier.Operation all = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            addAttributeModifier(Attributes.MOVEMENT_SPEED, JTTWEngine.id(name + "_speed"), -1, all);
            addAttributeModifier(Attributes.FLYING_SPEED, JTTWEngine.id(name + "_flying"), -1, all);
            addAttributeModifier(Attributes.JUMP_STRENGTH, JTTWEngine.id(name + "_jump"), -1, all);
            addAttributeModifier(NeoForgeMod.SWIM_SPEED, JTTWEngine.id(name + "_swim"), -1, all);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (!e.level().isClientSide && e instanceof Mob m) {
                m.getNavigation().stop();
                m.setJumping(false);
                m.setXxa(0);
                m.setYya(0);
                m.setZza(0);
                if (stuns) m.setTarget(null);
            }
            return true;
        }
    }

    // ── What the effects forbid ─────────────────────────────────────────

    /** No jump, not even one Jump Boost would give (on both sides: the client moves its player). */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity e = event.getEntity();
        if (!held(e)) return;
        Vec3 v = e.getDeltaMovement();
        if (v.y > 0) e.setDeltaMovement(v.x, 0, v.z);
    }

    /** The stunned take up no target. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && stunned(event.getEntity())) event.setCanceled(true);
    }

    /** A stunned player swings at nothing. */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (stunned(event.getEntity())) event.setCanceled(true);
    }

    /** No blow of the stunned lands: struck by its own hand, or by its own arrow. */
    @SubscribeEvent
    public static void onStunnedBlow(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity by = source.getEntity();
        if (!stunned(by)) return;
        Entity direct = source.getDirectEntity();
        if (direct == by || direct instanceof Projectile) event.setCanceled(true);
    }
}
