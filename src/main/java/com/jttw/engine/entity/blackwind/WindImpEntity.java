package com.jttw.engine.entity.blackwind;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Black-Wind Imp (黑风小妖): one of the little wind demons that run the Black Bear's errands over the
 * mountain, a child's height of soot-grey hide, great fan ears, long clawed arms, a rag of black cloth
 * streaming from its neck; it goes about in a little whirl of the black wind. Quick and frail. Every few
 * seconds, its foe a few blocks off, it spins up a gust about itself ({@value #WHIRL} ticks, the black
 * wind gathering round it) and rides it at its foe: whatever it reaches is struck and thrown back.
 * Strike one and the rest near it turn on you.
 */
public class WindImpEntity extends BlackWindMonster {

    /** Three ordinary hits to kill; a claw of 6.5% of a same-level character's health, its gust 7%. */
    public static final double HITS = 3, CLAW = 0.065, GUST = 0.07, SPEED = 0.34;
    /** How often it rides the wind, how long it spins up first, how long it rides, and from how near to how far. */
    public static final int GUST_EVERY = 70, WHIRL = 10, RIDE = 14;
    public static final double GUST_NEAR = 3, GUST_FAR = 11, GUST_REACH = 1.7;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(WindImpEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, WHIRLING = 1, RIDING = 2;

    private int gust = GUST_EVERY / 2, stage = -1;
    private Vec3 heading = Vec3.ZERO;

    public WindImpEntity(EntityType<? extends WindImpEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, CLAW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return CLAW;
    }

    @Override
    protected double[] specialShares() {
        return new double[]{GUST};
    }

    @Override
    public int escorts() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
    }

    /** What it is doing: {@link #IDLE}, {@link #WHIRLING} (spinning up its gust) or {@link #RIDING} it. */
    public int state() {
        return entityData.get(STATE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Its gust's hurt. */
    public float gustDamage() {
        return hitOf(GUST);
    }

    /** Begins spinning up its gust toward {@code target}. */
    public void whirl(LivingEntity target) {
        stage = 0;
        entityData.set(STATE, WHIRLING);
        heading = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()).normalize();
        playSound(SoundEvents.BREEZE_INHALE, 1f, 1.6f);
    }

    /** Its gust strikes {@code e}: hurt and thrown back along its way. Returns whether it landed. */
    public boolean buffet(LivingEntity e) {
        boolean hit = e.hurt(damageSources().mobAttack(this), gustDamage());
        if (hit) {
            e.knockback(1.1, -heading.x, -heading.z);
            e.hurtMarked = true;
        }
        playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1f, 1.3f);
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (gust > 0) gust--;
        LivingEntity t = getTarget();
        if (stage >= 0) {
            stage++;
            if (stage <= WHIRL) {
                getNavigation().stop();
                if (t != null) {
                    getLookControl().setLookAt(t, 30f, 30f);
                    heading = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ()).normalize();
                }
                if (stage == WHIRL) entityData.set(STATE, RIDING);
                return;
            }
            setDeltaMovement(heading.x * 0.75, Math.max(getDeltaMovement().y, 0.05), heading.z * 0.75);
            hurtMarked = true;
            for (LivingEntity e : caught(GUST_REACH, 1.2)) {
                buffet(e);
                stage = WHIRL + RIDE;
                break;
            }
            if (stage >= WHIRL + RIDE || horizontalCollision) {
                stage = -1;
                gust = GUST_EVERY;
                entityData.set(STATE, IDLE);
            }
            return;
        }
        if (gust == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d > GUST_NEAR && d < GUST_FAR && hasLineOfSight(t)) whirl(t);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            int s = state();
            if (random.nextInt(s == IDLE ? 3 : 1) == 0) {
                double a = tickCount * 0.6 + random.nextDouble();
                double r = s == IDLE ? 0.35 : 0.8;
                level().addParticle(ParticleTypes.SMOKE, getX() + Math.cos(a) * r, getY() + 0.1 + random.nextDouble() * (s == IDLE ? 0.3 : 1.2),
                        getZ() + Math.sin(a) * r, -Math.sin(a) * 0.1, 0.03, Math.cos(a) * 0.1);
            }
            if (s != IDLE) blackWind(2, 0.9);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Gust", gust);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        gust = tag.getInt("Gust");
    }
}
