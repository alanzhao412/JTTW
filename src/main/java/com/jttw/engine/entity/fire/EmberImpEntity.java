package com.jttw.engine.entity.fire;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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

import java.util.ArrayList;
import java.util.List;

/**
 * An Ember Imp (火焰小妖): one of the little fire demons of the Flaming Mountains, a child's height
 * of red-hot hide under a crest of living flame. Quick and frail, it keeps its distance and flings
 * embers ({@link EmberBoltEntity}) every few seconds, a captain three at once, fanned; come close
 * and its claws set you alight. Strike one and the rest near it turn on you. Like a blaze, water
 * hurts it.
 */
public class EmberImpEntity extends FireMonster {

    /** Four of a same-level character's ordinary hits to kill; a claw of a tenth of their health, an ember of 12%. */
    public static final double HITS = 4, CLAW = 0.10, EMBER = 0.12, SPEED = 0.33;
    /** How often it flings an ember, in ticks, and from how near to how far. */
    public static final int EMBER_EVERY = 50;
    public static final double EMBER_NEAR = 3, EMBER_FAR = 16;

    private int ember = EMBER_EVERY / 2;

    public EmberImpEntity(EntityType<? extends EmberImpEntity> type, Level level) {
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
    public int escorts() {
        return 4;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** An ember's hurt: its claw's, scaled as an ember is to a claw. */
    public float emberDamage() {
        return (float) (blowAt(mobLevel(), isCaptain()) * EMBER / CLAW);
    }

    /** Flings an ember at {@code target}; a captain three, fanned. Returns them. */
    public List<EmberBoltEntity> fling(LivingEntity target) {
        List<EmberBoltEntity> flung = new ArrayList<>();
        Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY(), target.getZ() - getZ());
        int fan = isCaptain() ? 1 : 0;
        for (int i = -fan; i <= fan; i++) {
            EmberBoltEntity bolt = new EmberBoltEntity(level(), this, to.yRot((float) Math.toRadians(14 * i)), emberDamage());
            level().addFreshEntity(bolt);
            flung.add(bolt);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.BLAZE_SHOOT, 1f, 1.4f);
        return flung;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (ember > 0) ember--;
        LivingEntity t = getTarget();
        if (ember == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d > EMBER_NEAR && d < EMBER_FAR && hasLineOfSight(t)) {
                fling(t);
                ember = EMBER_EVERY;
            }
        }
    }

    /** Its claws set what they strike alight. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.igniteForSeconds(3);
        return hit;
    }

    /** Water hurts it, as it does a blaze. */
    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.FLAME, getRandomX(0.4), getY() + getBbHeight() * 0.9, getRandomZ(0.4), 0, 0.03, 0);
        }
    }

    @Override
    public float getVoicePitch() {
        return 1.6f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }
}
