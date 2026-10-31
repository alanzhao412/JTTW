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
import net.minecraft.world.phys.Vec3;

/**
 * A Boar Demon (野猪精) of the Black Bear's host: a man and a half tall, a wild boar's head on a soldier's
 * body, bristling black-brown hide, yellow tusks, a vest of iron scales over leather, a heavy bronze axe.
 * When its foe is a little way off it lowers its head and paws the ground ({@value #PAW} ticks, snorting,
 * the dirt flying), then charges headlong: the first it meets is gored and thrown.
 */
public class BoarDemonEntity extends BlackWindMonster {

    /** Six ordinary hits to kill; a blow of 9% of a same-level character's health, the gore 10%. */
    public static final double HITS = 6, BLOW = 0.09, GORE = 0.10, SPEED = 0.26;
    /** How often it charges, how long it paws first, how long it runs at most, how fast, and from how near to how far. */
    public static final int CHARGE_EVERY = 110, PAW = 16, RUN = 26;
    public static final double RUN_SPEED = 0.62, CHARGE_NEAR = 4, CHARGE_FAR = 15, GORE_REACH = 1.4;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(BoarDemonEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, PAWING = 1, CHARGING = 2;

    private int charge = CHARGE_EVERY / 2, stage = -1;
    private Vec3 heading = Vec3.ZERO;

    public BoarDemonEntity(EntityType<? extends BoarDemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 0.8)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ARMOR, 5)
                .add(Attributes.FOLLOW_RANGE, 28)
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
    protected double[] specialShares() {
        return new double[]{GORE};
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
    }

    /** What it is doing: {@link #IDLE}, {@link #PAWING} the ground, or {@link #CHARGING}. */
    public int state() {
        return entityData.get(STATE);
    }

    /** Holds it in {@code state} (the gallery's pictures; its own wind-ups set it as they go). */
    public void pose(int state) {
        entityData.set(STATE, state);
    }


    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public float goreDamage() {
        return hitOf(GORE);
    }

    /** Lowers its head and paws the ground, the charge to come at {@code target}. */
    public void paw(LivingEntity target) {
        stage = 0;
        entityData.set(STATE, PAWING);
        heading = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()).normalize();
        playSound(SoundEvents.HOGLIN_ANGRY, 1.2f, 0.8f);
    }

    /** Its tusks in {@code e}: hurt, thrown up and away. Returns whether it landed. */
    public boolean gore(LivingEntity e) {
        boolean hit = e.hurt(damageSources().mobAttack(this), goreDamage());
        if (hit) {
            e.knockback(1.3, -heading.x, -heading.z);
            e.push(0, 0.45, 0);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.HOGLIN_ATTACK, 1.2f, 0.8f);
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (charge > 0) charge--;
        LivingEntity t = getTarget();
        if (stage >= 0) {
            stage++;
            if (stage <= PAW) {
                getNavigation().stop();
                if (t != null) {
                    getLookControl().setLookAt(t, 30f, 30f);
                    heading = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ()).normalize();
                }
                if (stage % 5 == 0 && level() instanceof ServerLevel server) {
                    BlockState under = level().getBlockState(blockPosition().below());
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 12, 0.4, 0.05, 0.4, 0.1);
                    playSound(SoundEvents.HOGLIN_STEP, 1f, 0.7f);
                }
                if (stage == PAW) {
                    entityData.set(STATE, CHARGING);
                    playSound(SoundEvents.RAVAGER_ROAR, 0.6f, 1.4f);
                }
                return;
            }
            setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
            setYBodyRot(getYRot());
            setDeltaMovement(heading.x * RUN_SPEED, getDeltaMovement().y, heading.z * RUN_SPEED);
            hurtMarked = true;
            if (level() instanceof ServerLevel server && stage % 2 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 2, 0.3, 0.05, 0.3, 0.01);
            }
            for (LivingEntity e : caught(GORE_REACH, 1.0)) {
                gore(e);
                stage = PAW + RUN;
                break;
            }
            if (stage >= PAW + RUN || horizontalCollision) {
                stage = -1;
                charge = CHARGE_EVERY;
                entityData.set(STATE, IDLE);
            }
            return;
        }
        if (charge == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d > CHARGE_NEAR && d < CHARGE_FAR && hasLineOfSight(t)) paw(t);
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.8f + (random.nextFloat() - 0.5f) * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HOGLIN_STEP, 0.6f, 0.9f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Charge", charge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        charge = tag.getInt("Charge");
    }
}
