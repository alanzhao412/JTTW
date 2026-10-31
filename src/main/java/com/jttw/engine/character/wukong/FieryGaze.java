package com.jttw.engine.character.wukong;

import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;

import java.util.Set;
import java.util.UUID;

/**
 * The Fiery Golden Eyes' fire (the effect "Fiery Gaze"): what the laser strikes burns, without end,
 * until it dies. Neither water nor rain nor Fire Resistance puts it out, nor milk; only a totem's
 * death-defiance clears it (as it clears every effect), and death.
 *
 * <p>Each second it takes a beat, set when it was lit: {@link #BEAT} by the output (at level 1),
 * scaled by Wukong's power then, as every hit of his is. A second shot lights it no fiercer than
 * the fiercer of the two (it never stacks). It burns bosses as it burns anything: at most a slow
 * chip against a boss's great health (output V alone takes some twenty minutes to burn down a boss
 * of his own level). It shows as fire on it, and slows it: Slowness I at outputs I and II, II at
 * III and IV, III at V, for as long as it burns.
 */
public final class FieryGaze {

    private FieryGaze() {}

    /** Each second's beat at level 1, by output (I-V). */
    public static final float[] BEAT = {0.5f, 0.8f, 1.2f, 1.8f, 2.5f};
    /** On the burning one: what each beat takes, and whose fire it is. */
    public static final String KEY_BEAT = "jttw_fiery_gaze_beat", KEY_BY = "jttw_fiery_gaze_by";
    /** The fire's colour: gold going to orange. */
    public static final int COLOUR = 0xFFA41C;

    /** The Slowness amplifier at an output (1-5). */
    public static int slowness(int output) {
        return (output - 1) / 2;
    }

    /** Sets {@code e} burning with his eyes' fire, at this output, for good. */
    public static void ignite(LivingEntity e, ServerPlayer by, int output) {
        float beat = LevelPower.scaled(by, BEAT[Math.max(1, Math.min(5, output)) - 1]);
        var data = e.getPersistentData();
        if (beat > data.getFloat(KEY_BEAT) || !burning(e)) {
            data.putFloat(KEY_BEAT, beat);
            data.putUUID(KEY_BY, by.getUUID());
        }
        int amp = output - 1;
        MobEffectInstance now = e.getEffect(ModEffects.FIERY_GAZE);
        if (now == null || now.getAmplifier() < amp) {
            e.addEffect(new MobEffectInstance(ModEffects.FIERY_GAZE, MobEffectInstance.INFINITE_DURATION, amp, false, true, true));
        }
        slow(e, amp);
    }

    public static boolean burning(LivingEntity e) {
        return e.hasEffect(ModEffects.FIERY_GAZE);
    }

    /** What each beat takes from {@code e}. */
    public static float beatOf(LivingEntity e) {
        return e.getPersistentData().getFloat(KEY_BEAT);
    }

    private static void slow(LivingEntity e, int amp) {
        MobEffectInstance s = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        int want = slowness(amp + 1);
        if (s == null || s.getAmplifier() < want || s.getDuration() < 30) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, want, false, false, true));
        }
    }

    /** One beat on {@code e}: its hurt (by Wukong, if he is about), flames, and its Slowness kept up. */
    static void beat(LivingEntity e, int amp) {
        if (!(e.level() instanceof ServerLevel level) || !e.isAlive()) return;
        float amount = beatOf(e);
        slow(e, amp);
        if (amount <= 0) return;
        Entity by = null;
        var data = e.getPersistentData();
        if (data.hasUUID(KEY_BY)) {
            UUID id = data.getUUID(KEY_BY);
            by = level.getServer().getPlayerList().getPlayer(id);
        }
        DamageSource src = level.damageSources().source(DamageTypes.MAGIC, by);
        e.invulnerableTime = 0; // a beat of it always lands
        e.hurt(src, amount);
        level.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY(0.5), e.getZ(), 6,
                e.getBbWidth() * 0.4, e.getBbHeight() * 0.4, e.getBbWidth() * 0.4, 0.02);
        if (e.isInWaterRainOrBubble()) level.sendParticles(ParticleTypes.LAVA, e.getX(), e.getY(0.8), e.getZ(), 2, 0.3, 0.3, 0.3, 0);
    }

    /** The effect: fire shown on it every tick (water cannot put that out either), a beat each second. */
    public static final class Burning extends MobEffect {
        public Burning() {
            super(MobEffectCategory.HARMFUL, COLOUR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide) return true;
            e.setSharedFlagOnFire(true); // seen burning, whatever it stands in
            if (e.tickCount % 20 == 0) beat(e, amplifier);
            return true;
        }

        /** No milk; a totem still clears it, as it clears every effect. */
        @Override
        public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
            cures.add(EffectCures.PROTECTED_BY_TOTEM);
        }
    }
}
