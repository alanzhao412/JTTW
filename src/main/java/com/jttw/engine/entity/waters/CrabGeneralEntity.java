package com.jttw.engine.entity.waters;

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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Crab General (蟹将): a great crab, broad as a cart, shelled like a fortress, under a general's
 * gilded helm. It does not swim; it walks the sea floor, heavy and hard to move, and when its foe
 * swims above it, it springs up off the bottom at them. Its pinch is a heavy blow, and what it
 * catches is held: slowed a while after.
 */
public class CrabGeneralEntity extends SeaMonster {

    /** Fourteen of a same-level character's ordinary hits to kill; a pinch of 28% of their health, before resistance. */
    public static final double HITS = 14, PINCH = 0.28;
    /** How long its pinch holds what it catches, in ticks. */
    public static final int HELD = 50;
    /** How far over it its foe must be for it to spring, how hard it springs, and how often. */
    public static final double SPRING_ABOVE = 2.5, SPRING = 0.95;
    public static final int SPRING_EVERY = 60;

    private int spring;

    public CrabGeneralEntity(EntityType<? extends CrabGeneralEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, PINCH * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ARMOR_TOUGHNESS, 2)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** It walks the floor: it does not swim. */
    @Override
    public boolean swims() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanFloat(false);
        return nav;
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return PINCH;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Its claws reach out from its broad shell, and a little up. */
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(0.6, 1.0, 0.6);
    }

    /** Its pinch: and what it catches is held, slowed. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HELD, 2), this);
            playSound(SoundEvents.TURTLE_EGG_CRACK, 1.2f, 0.6f);
        }
        return hit;
    }

    /** Springs up off the floor toward {@code target}. Returns whether it did. */
    public boolean springAt(LivingEntity target) {
        if (spring > 0) return false;
        Vec3 to = target.position().subtract(position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        Vec3 lean = flat.lengthSqr() > 1e-4 ? flat.normalize().scale(0.3) : Vec3.ZERO;
        setDeltaMovement(lean.x, SPRING, lean.z);
        hurtMarked = true;
        spring = SPRING_EVERY;
        playSound(SoundEvents.TURTLE_SHAMBLE, 1.5f, 0.6f);
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (spring > 0) spring--;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && target.getY() - getY() > SPRING_ABOVE
                && (onGround() || verticalCollisionBelow) && horizontalDistanceSqr(target) < 36) {
            springAt(target);
        }
    }

    private double horizontalDistanceSqr(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }
}
