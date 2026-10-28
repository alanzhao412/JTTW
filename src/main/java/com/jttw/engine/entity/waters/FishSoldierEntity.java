package com.jttw.engine.entity.waters;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Fish Soldier (鱼兵), a carp of the Dragon Kings' host, stood up as a man and armed with a
 * halberd: the quickest of the sea's rank and file and the frailest. In the water it swims faster
 * than any of them, and from a few blocks off it lunges at its foe with a beat of its tail, closing
 * the distance at once; out of the water it is only a quick soldier on its feet.
 */
public class FishSoldierEntity extends SeaMonster {

    /** Four of a same-level character's ordinary hits to kill; a thrust of 12% of their health, before resistance. */
    public static final double HITS = 4, THRUST = 0.12;
    /** How much further than arm's length its halberd reaches. */
    public static final double HALBERD = 1.1;
    /** How much of its walking speed it swims at: more than the others' {@link SeaMonster#SWIM}. */
    public static final float FAST_SWIM = 0.3f;
    /** It lunges at a foe between these distances, in the water, this hard, once each so many ticks. */
    public static final double LUNGE_NEAR = 2.5, LUNGE_FAR = 9, LUNGE = 0.9;
    public static final int LUNGE_EVERY = 50;

    private int lunge = LUNGE_EVERY / 2;

    public FishSoldierEntity(EntityType<? extends FishSoldierEntity> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 20, FAST_SWIM, 0.6f, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // as a level-47 one; each sets its own by its level (become)
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, THRUST * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return THRUST;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 20));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Its halberd: it strikes from further than arm's length, and up or down a little. */
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(HALBERD, 0.6, HALBERD);
    }

    /** Whether it may lunge now (its lunge is ready). */
    public boolean lungeReady() {
        return lunge <= 0;
    }

    /** Lunges at {@code target} with a beat of its tail. Returns whether it did. */
    public boolean lungeAt(LivingEntity target) {
        if (lunge > 0) return false;
        Vec3 to = target.getEyePosition().subtract(position().add(0, getBbHeight() * 0.5, 0));
        if (to.lengthSqr() < 1e-4) return false;
        Vec3 v = to.normalize().scale(LUNGE);
        setDeltaMovement(v);
        hurtMarked = true;
        lunge = LUNGE_EVERY;
        playSound(SoundEvents.FISH_SWIM, 1.2f, 1.4f);
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (lunge > 0) lunge--;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && isInWater() && lunge <= 0 && hasLineOfSight(target)) {
            double d = distanceTo(target);
            if (d > LUNGE_NEAR && d < LUNGE_FAR) lungeAt(target);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SALMON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }
}
