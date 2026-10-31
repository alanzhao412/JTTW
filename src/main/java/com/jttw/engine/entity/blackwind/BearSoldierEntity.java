package com.jttw.engine.entity.blackwind;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A Black Bear Soldier (黑熊兵), of the Black Bear Demon's own kind and his household guard: a black bear
 * risen on its hind legs, half again a man's height, the pale crescent on its chest, in black lamellae
 * laced in red, a horned iron helm, a great iron-studded mace. It walks slowly and hits hard; and when its
 * foe is close it rears up, mace high ({@value #WINDUP} ticks), and crashes down: all within
 * {@value #SLAM_RANGE} blocks are struck and thrown up. Then it must gather itself.
 */
public class BearSoldierEntity extends BlackWindMonster {

    /** Eleven ordinary hits to kill; a blow of 12% of a same-level character's health, the slam 11%. */
    public static final double HITS = 11, BLOW = 0.12, SLAM = 0.11, SPEED = 0.22;
    public static final int WINDUP = 18, SLAM_EVERY = 100;
    public static final double SLAM_RANGE = 3.5;

    private static final EntityDataAccessor<Boolean> RAISING = SynchedEntityData.defineId(BearSoldierEntity.class, EntityDataSerializers.BOOLEAN);

    private int slam = SLAM_EVERY / 2, raised = -1;

    public BearSoldierEntity(EntityType<? extends BearSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 1.4)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.ARMOR, 9)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.25);
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
    protected double[] specialShares() {
        return new double[]{SLAM};
    }

    @Override
    public int escorts() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAISING, false);
    }

    /** Whether it is reared up, mace high, about to crash down. */
    public boolean isRaising() {
        return entityData.get(RAISING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public float slamDamage() {
        return hitOf(SLAM);
    }

    /** Rears up, the slam to come. */
    public void rear() {
        raised = 0;
        entityData.set(RAISING, true);
        playSound(SoundEvents.POLAR_BEAR_WARNING, 1.4f, 0.6f);
    }

    /** Crashes down: all near it (but the mountain's own) struck and thrown up. Returns who. */
    public List<LivingEntity> slam() {
        List<LivingEntity> hit = caught(SLAM_RANGE, 2);
        for (LivingEntity e : hit) {
            e.hurt(damageSources().mobAttack(this), slamDamage());
            e.push(0, 0.6, 0);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.7f);
        if (level() instanceof ServerLevel server) {
            BlockState under = level().getBlockState(blockPosition().below());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 40, SLAM_RANGE / 2, 0.1, SLAM_RANGE / 2, 0.15);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 24, SLAM_RANGE / 2, 0.1, SLAM_RANGE / 2, 0.03);
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (slam > 0) slam--;
        LivingEntity t = getTarget();
        if (raised >= 0) {
            getNavigation().stop();
            if (++raised >= WINDUP) {
                slam();
                raised = -1;
                slam = SLAM_EVERY;
                entityData.set(RAISING, false);
            }
            return;
        }
        if (slam == 0 && t != null && t.isAlive() && distanceToSqr(t) <= SLAM_RANGE * SLAM_RANGE) rear();
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.POLAR_BEAR_STEP, 0.9f, 0.7f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Slam", slam);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        slam = tag.getInt("Slam");
    }
}
