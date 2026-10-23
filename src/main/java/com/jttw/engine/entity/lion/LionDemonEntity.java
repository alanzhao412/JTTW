package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Lion-Demon (狮子精): one of the Azure Lion's kin, of the lion host that swarms on the ridge
 * (chapter 74: "lion-demons, elephant-demons... the ridge was all monsters"). A lion-headed demon
 * soldier with a curved sabre, quick on its feet. Every {@value #POUNCE_EVERY} ticks or so, a foe
 * {@value #POUNCE_NEAR} to {@value #POUNCE_FAR} blocks off, it <b>pounces</b>: it bounds at the
 * foe and comes down on him, a blow of {@value #POUNCE_PERCENT}% that staggers (Slowness). Strike
 * one and the pride turns on you.
 */
public class LionDemonEntity extends LionCamelMonster {

    public static final int POUNCE_PERCENT = 26;
    /** Nine ordinary hits; a sabre cut of 22% of a same-level character's health. */
    public static final double HITS = 9, BLOW = 0.22, POUNCE = POUNCE_PERCENT / 100.0, SPEED = 0.31;
    public static final int POUNCE_EVERY = 90;
    public static final double POUNCE_NEAR = 4, POUNCE_FAR = 11;

    private int pounce = POUNCE_EVERY / 2;
    private boolean leaping;

    public LionDemonEntity(EntityType<? extends LionDemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.FOLLOW_RANGE, 30)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The pounce's hurt. */
    public float pounceDamage() {
        return (float) (blowAt(mobLevel(), isCaptain()) * POUNCE / BLOW);
    }

    /** Bounds at {@code target}. Returns whether it did (only from the ground). */
    public boolean pounce(LivingEntity target) {
        if (!onGround()) return false;
        Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        double d = flat.length();
        if (d < 1e-3) return false;
        Vec3 lean = flat.scale(Math.min(1.3, 0.11 * d + 0.25) / d);
        setDeltaMovement(lean.x, 0.55, lean.z);
        hurtMarked = true;
        leaping = true;
        lookAt(target, 360f, 360f);
        playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.6f);
        return true;
    }

    /** Coming down on {@code target}: its blow, and a stagger. Returns whether it struck. */
    public boolean landOn(LivingEntity target) {
        if (isAlliedTo(target) || !target.isAlive()) return false;
        boolean hit = target.hurt(damageSources().mobAttack(this), pounceDamage());
        if (hit) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), this);
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (pounce > 0) pounce--;
        LivingEntity t = getTarget();
        if (leaping) {
            if (t != null && t.isAlive() && getBoundingBox().inflate(0.6).intersects(t.getBoundingBox())) {
                landOn(t);
                leaping = false;
            } else if (onGround() && getDeltaMovement().y <= 0) {
                leaping = false;
            }
        }
        if (pounce == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d >= POUNCE_NEAR && d <= POUNCE_FAR && hasLineOfSight(t) && pounce(t)) pounce = POUNCE_EVERY + random.nextInt(30);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return distance > 6 && super.causeFallDamage(distance - 6, multiplier, source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Pounce", pounce);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pounce = tag.getInt("Pounce");
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }
}
