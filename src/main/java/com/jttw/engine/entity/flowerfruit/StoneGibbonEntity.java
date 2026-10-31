package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.MobScaling;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A Stone-Throwing Gibbon (投石长臂猿): one of the long-armed gibbons of Flower-Fruit Mountain's
 * army, quick and frail. It keeps its distance and throws stones: it rears back with a stone over its
 * head for {@value #WINDUP} ticks (the warning), then hurls it, every few seconds, a captain three at
 * once, fanned; come close and it claws. Strike one and the rest near it turn on you.
 */
public class StoneGibbonEntity extends FlowerFruitMonster {

    /** Three of a same-level character's ordinary hits to kill; a claw of 6.5% of their health, a stone of 7%. */
    public static final double HITS = 3, CLAW = 0.065, STONE = 0.07, SPEED = 0.34;
    public static final int THROW_EVERY = 45, WINDUP = 10;
    public static final double THROW_NEAR = 3, THROW_FAR = 18;

    private static final EntityDataAccessor<Boolean> REARING = SynchedEntityData.defineId(StoneGibbonEntity.class, EntityDataSerializers.BOOLEAN);

    private int cooldown = THROW_EVERY / 2, windup = -1;

    public StoneGibbonEntity(EntityType<? extends StoneGibbonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, CLAW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.1);
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
        return new double[]{STONE};
    }

    @Override
    public int escorts() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REARING, false);
    }

    /** Whether it has a stone up over its head, about to throw (the warning). */
    public boolean isRearing() {
        return entityData.get(REARING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** A stone's hurt. */
    public float stoneDamage() {
        return hitOf(STONE);
    }

    /** Starts its throw at its target: the stone up over its head; it flies {@value #WINDUP} ticks later. */
    public void rearBack() {
        windup = 0;
        entityData.set(REARING, true);
        playSound(SoundEvents.FOX_SCREECH, 0.8f, 1.3f);
    }

    /** Hurls a stone at {@code target}, arcing; a captain three, fanned. Returns them. */
    public List<ThrownStoneEntity> hurl(LivingEntity target) {
        List<ThrownStoneEntity> thrown = new ArrayList<>();
        int fan = isCaptain() ? 1 : 0;
        for (int i = -fan; i <= fan; i++) {
            ThrownStoneEntity stone = new ThrownStoneEntity(level(), this, stoneDamage());
            stone.setPos(getX(), getEyeY() + 0.3, getZ());
            Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.4) - stone.getY(), target.getZ() - getZ()).yRot((float) Math.toRadians(12 * i));
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            stone.shoot(to.x, to.y + flat * 0.12, to.z, 1.35f, 1.5f);
            level().addFreshEntity(stone);
            thrown.add(stone);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.SNOWBALL_THROW, 1f, 0.6f);
        return thrown;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        LivingEntity t = getTarget();
        if (windup >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (++windup >= WINDUP) {
                windup = -1;
                entityData.set(REARING, false);
                if (t != null && t.isAlive()) hurl(t);
                cooldown = THROW_EVERY;
            }
            return;
        }
        if (cooldown == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d > THROW_NEAR && d < THROW_FAR && hasLineOfSight(t)) rearBack();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Throw", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("Throw");
    }

    @Override
    public float getVoicePitch() {
        return 1.5f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }
}
