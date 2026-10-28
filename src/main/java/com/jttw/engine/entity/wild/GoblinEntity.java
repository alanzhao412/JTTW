package com.jttw.engine.entity.wild;

import com.jttw.engine.entity.MobScaling;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A goblin (山魈小妖), a mountain imp: a small, wiry, moss-skinned yaoguai with huge ears, a hooked
 * nose and a mouthful of crooked teeth, in a patched hemp robe and a string of wooden beads, a
 * stolen Daoist talisman or two on it, a sack of its thefts on its back, a crude bronze dagger in
 * its fist and a sling at its belt. They come out at night on the hills, in the forests and over
 * the plains, in gangs of three to five, quick on their feet.
 *
 * <p>They are cowards. One that is hurt below {@value #NERVE_PERCENT}% of its health, or finds
 * fewer than {@value #COURAGE} of its fellows about it, runs from its foe, cowering (and comes
 * back once its gang is round it again). Strike one and the gang turns on you.
 */
public class GoblinEntity extends WildYaoguai {

    /** Three ordinary hits; a dagger stab of 9% of a same-level character's health. Very quick. */
    public static final double HITS = 3, BLOW = 0.09, SPEED = 0.34;
    public static final int MIN_LEVEL = 1, MAX_LEVEL = 6;
    /** Hurt below this share of its health, it runs. */
    public static final int NERVE_PERCENT = 50;
    /** How many other goblins it needs within {@value #GANG_RANGE} blocks to keep its nerve. */
    public static final int COURAGE = 2;
    public static final double GANG_RANGE = 10;

    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(GoblinEntity.class, EntityDataSerializers.BOOLEAN);

    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(MIN_LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(MIN_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override public int minLevel() { return MIN_LEVEL; }
    @Override public int maxLevel() { return MAX_LEVEL; }
    @Override protected double hitsToKill() { return HITS; }
    @Override protected double blowShare() { return BLOW; }
    @Override public int vanillaXpShare() { return 1; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLEEING, false);
    }

    /** Whether it is running from its foe (and cowering as it runs). */
    public boolean isFleeing() {
        return entityData.get(FLEEING);
    }

    /** For the gallery: shown cowering. */
    public void showFleeing(boolean on) {
        entityData.set(FLEEING, on);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LoseNerveGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** How many other goblins are about it. */
    public int gangAround() {
        return level().getEntitiesOfClass(GoblinEntity.class, getBoundingBox().inflate(GANG_RANGE), g -> g != this && g.isAlive()).size();
    }

    /** Whether it has lost its nerve: hurt too far, or too few of its gang about it. */
    public boolean afraid() {
        return getHealth() < getMaxHealth() * NERVE_PERCENT / 100f || gangAround() < COURAGE;
    }

    /** Runs from its foe while it is afraid of it. */
    static final class LoseNerveGoal extends Goal {
        private final GoblinEntity goblin;
        private int recheck;

        LoseNerveGoal(GoblinEntity goblin) {
            this.goblin = goblin;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = goblin.getTarget();
            return t != null && t.isAlive() && goblin.distanceToSqr(t) < 16 * 16 && goblin.afraid();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = goblin.getTarget();
            if (t == null || !t.isAlive()) return false;
            if (--recheck <= 0) {
                recheck = 20;
                return goblin.afraid() && goblin.distanceToSqr(t) < 20 * 20;
            }
            return true;
        }

        @Override
        public void start() {
            goblin.entityData.set(FLEEING, true);
            recheck = 20;
            run();
        }

        @Override
        public void tick() {
            if (goblin.getNavigation().isDone()) run();
        }

        private void run() {
            LivingEntity t = goblin.getTarget();
            if (t == null) return;
            Vec3 away = DefaultRandomPos.getPosAway(goblin, 12, 6, t.position());
            if (away != null) goblin.getNavigation().moveTo(away.x, away.y, away.z, 1.45);
        }

        @Override
        public void stop() {
            goblin.entityData.set(FLEEING, false);
            goblin.getNavigation().stop();
        }
    }

    // ── Its voice: a cackle ─────────────────────────────────────────────

    @Override
    public float getVoicePitch() {
        return 1.55f + (random.nextFloat() - 0.5f) * 0.25f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_CELEBRATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }
}
