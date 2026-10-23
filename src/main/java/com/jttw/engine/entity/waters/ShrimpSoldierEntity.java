package com.jttw.engine.entity.waters;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * A Shrimp Soldier (虾兵), the rank and file of the Dragon Kings' host: a shrimp the height of a man,
 * upright, red-shelled, its feelers long, armed with a spear. It swims quick and darts in to thrust
 * from a little further off than a sword reaches; struck, it may flick its tail and shoot backward
 * out of reach, as a shrimp does.
 */
public class ShrimpSoldierEntity extends SeaMonster {

    /** Six of a same-level character's ordinary hits to kill; a thrust of 14% of their health, before resistance. */
    public static final double HITS = 6, THRUST = 0.14;
    /** How much further than arm's length its spear reaches. */
    public static final double SPEAR = 1.3;
    /** The chance a blow sends it darting back, and how hard. */
    public static final float DART_CHANCE = 0.35f;
    public static final double DART = 1.1;

    public ShrimpSoldierEntity(EntityType<? extends ShrimpSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // as a level-47 one; each sets its own by its level (become)
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, THRUST * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4)
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
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8, 30));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Its spear: it strikes from further than arm's length, and up or down a little. */
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(SPEAR, 0.6, SPEAR);
    }

    /** Darts backward, away from {@code from}, with a flick of its tail. */
    public void dart(Entity from) {
        Vec3 away = position().subtract(from.position()).multiply(1, 0.3, 1);
        if (away.lengthSqr() < 1e-4) away = new Vec3(0, 0, 1);
        away = away.normalize().scale(DART);
        setDeltaMovement(getDeltaMovement().add(away.x, away.y + 0.1, away.z));
        hurtMarked = true;
        playSound(SoundEvents.COD_FLOP, 1f, 1.6f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive() && isInWater() && source.getEntity() != null && random.nextFloat() < DART_CHANCE) {
            dart(source.getEntity());
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }
}
