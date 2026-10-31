package com.jttw.engine.entity.spider;

import com.jttw.engine.entity.MobScaling;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Mantis Son (螳螂精): one of the Seven Spider Spirits' adopted sons, a tall, lean insect demon
 * standing upright on four thin legs, its head a green triangle with great compound eyes, its
 * forelimbs two folded scythes. Its blow is a scythe's cut; and when its foe is close it raises
 * both scythes high for {@value #RAISE} ticks, then sweeps them through all before it, within
 * {@value #SWEEP_RANGE} blocks and {@value #SWEEP_ANGLE} degrees either side: a cut of
 * {@value #SWEEP}. Step aside or back out of it.
 */
public class MantisSonEntity extends SilkMonster {

    /** Six ordinary hits to kill; a cut of 9%, its sweep 10%. */
    public static final double HITS = 6, CUT = 0.09, SWEEP = 0.10, SPEED = 0.3;
    public static final int RAISE = 14, SWEEP_EVERY = 70;
    public static final double SWEEP_RANGE = 3.4, SWEEP_ANGLE = 60;

    private static final EntityDataAccessor<Boolean> RAISED = SynchedEntityData.defineId(MantisSonEntity.class, EntityDataSerializers.BOOLEAN);

    private int sweep = SWEEP_EVERY / 2, raised = -1;

    public MantisSonEntity(EntityType<? extends MantisSonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, CUT * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return CUT;
    }

    @Override
    protected double[] specialShares() {
        return new double[]{SWEEP};
    }

    @Override
    public int escorts() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAISED, false);
    }

    /** Whether its scythes are raised, about to sweep. */
    public boolean isRaised() {
        return entityData.get(RAISED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The sweep's hurt. */
    public float sweepDamage() {
        return hitOf(SWEEP);
    }

    /** Whether {@code e} stands in the sweep's arc before it. */
    public boolean inArc(LivingEntity e) {
        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
        if (to.lengthSqr() > (SWEEP_RANGE + e.getBbWidth() / 2) * (SWEEP_RANGE + e.getBbWidth() / 2)) return false;
        if (to.lengthSqr() < 0.25) return true;
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        return to.normalize().dot(look) >= Math.cos(Math.toRadians(SWEEP_ANGLE));
    }

    /** Sweeps both scythes through all before it. Returns who was cut. */
    public List<LivingEntity> sweep() {
        List<LivingEntity> cut = level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(SWEEP_RANGE + 1, 2, SWEEP_RANGE + 1),
                e -> e != this && e.isAlive() && !(e instanceof SilkKin) && !isAlliedTo(e) && inArc(e)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : cut) {
            e.invulnerableTime = 0;
            e.hurt(damageSources().mobAttack(this), sweepDamage());
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        if (level() instanceof ServerLevel server) {
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            for (int i = -2; i <= 2; i++) {
                Vec3 at = position().add(look.yRot((float) Math.toRadians(i * 25)).scale(2.0));
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, getY(0.6), at.z, 1, 0, 0, 0, 0);
            }
        }
        return cut;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (sweep > 0) sweep--;
        LivingEntity t = getTarget();
        if (raised >= 0) {
            getNavigation().stop();
            if (++raised >= RAISE) {
                sweep();
                raised = -1;
                sweep = SWEEP_EVERY;
                entityData.set(RAISED, false);
            }
            return;
        }
        if (sweep == 0 && t != null && t.isAlive() && distanceToSqr(t) <= SWEEP_RANGE * SWEEP_RANGE) {
            raised = 0;
            entityData.set(RAISED, true);
            lookAt(t, 360f, 0f);
            setYBodyRot(getYRot());
            playSound(SoundEvents.SPIDER_AMBIENT, 1.2f, 1.6f);
        }
    }

    @Override
    public float getVoicePitch() {
        return 1.4f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Sweep", sweep);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sweep = tag.getInt("Sweep");
    }
}
