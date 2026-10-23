package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/**
 * A wandering ghost (游魂): a soul not yet judged, drifting over the ash plains in its grave
 * clothes, pale, half there, its feet never quite on the ground. Weak alone, they drift in
 * threes and fours; their touch is the grave's <b>chill</b>: it slows, and it leaves one hungry.
 * The only creatures of Hell met outside the fortress (never near the arrival).
 */
public class WanderingGhostEntity extends HellMonster {

    /** Two and a half of a same-level character's ordinary hits; a touch of 7% of their health. */
    public static final double HITS = 2.5, BLOW = 0.07;
    public static final int LEVEL = 57;
    /** The chill: how long it slows, how long it starves. */
    public static final int CHILL = 50, HUNGER = 120;

    public WanderingGhostEntity(EntityType<? extends WanderingGhostEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public int regularLevel() {
        return LEVEL;
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
    public boolean isOfficial() {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity l) chill(l);
        return hit;
    }

    /** The grave's chill: slowed, and hungry. */
    public void chill(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER, 1), this);
    }

    /** One of the restless dead: healing harms it, harming heals it (and laying it to rest is the undead's Heavenly deed). */
    @Override
    public boolean isInvertedHealAndHarm() {
        return true;
    }

    /** It drifts down, it does not fall. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!onGround() && getDeltaMovement().y < -0.08) setDeltaMovement(getDeltaMovement().multiply(1, 0.6, 1));
    }

    @Override
    public float getVoicePitch() {
        return 0.8f + random.nextFloat() * 0.3f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }
}
