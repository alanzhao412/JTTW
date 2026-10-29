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
 * A goblin (山魈), a demon of the mountains: as tall as a man but hunched, long-limbed and lean, with
 * soot-indigo skin and a wild white mane, a demon's face with tusks and a gaping mouth of fangs, eyes
 * burning amber under a heavy brow, one great horn (the other broken), bone ornaments and torn
 * talismans worn as trophies, and a jagged iron chopper in its fist. They hunt at night on the hills,
 * in the forests and over the plains, in packs of three to five, quick on their feet.
 *
 * <p>They are pack hunters, and fight as a pack. One that is hurt below {@value #NERVE_PERCENT}% of
 * its health, or finds fewer than {@value #COURAGE} of its pack about it, falls back from its foe:
 * crouched, hissing, its claws up, its eyes on it (and comes on again once the pack is round it).
 * Strike one and the pack turns on you.
 */
public class GoblinEntity extends WildYaoguai {

    /** Four ordinary hits; a hack of its chopper of 6% of a same-level character's health. Quick. */
    public static final double HITS = 4, BLOW = 0.06, SPEED = 0.31;
    public static final int MIN_LEVEL = 2, MAX_LEVEL = 8;
    /** Hurt below this share of its health, it falls back. */
    public static final int NERVE_PERCENT = 30;
    /** How many other goblins it needs within {@value #GANG_RANGE} blocks to keep pressing on. */
    public static final int COURAGE = 1;
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

    /** Whether it is falling back from its foe (crouched and hissing as it goes). */
    public boolean isFleeing() {
        return entityData.get(FLEEING);
    }

    /** For the gallery: shown falling back. */
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

    /** How many others of its pack are about it. */
    public int gangAround() {
        return level().getEntitiesOfClass(GoblinEntity.class, getBoundingBox().inflate(GANG_RANGE), g -> g != this && g.isAlive()).size();
    }

    /** Whether it would fall back: hurt too far, or too few of its pack about it. */
    public boolean afraid() {
        return getHealth() < getMaxHealth() * NERVE_PERCENT / 100f || gangAround() < COURAGE;
    }

    /** Falls back from its foe while it is afraid of it: backing off, its eyes on it, hissing. */
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
            goblin.playSound(SoundEvents.CAT_HISS, 1.0f, 0.55f);
            recheck = 20;
            run();
        }

        @Override
        public void tick() {
            LivingEntity t = goblin.getTarget();
            if (t != null) goblin.getLookControl().setLookAt(t, 30f, 30f);
            if (goblin.getNavigation().isDone()) run();
        }

        private void run() {
            LivingEntity t = goblin.getTarget();
            if (t == null) return;
            Vec3 away = DefaultRandomPos.getPosAway(goblin, 9, 5, t.position());
            if (away != null) goblin.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
        }

        @Override
        public void stop() {
            goblin.entityData.set(FLEEING, false);
            goblin.getNavigation().stop();
        }
    }

    // ── Its voice: a growl ──────────────────────────────────────────────

    @Override
    public float getVoicePitch() {
        return 1.15f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_BRUTE_ANGRY;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_BRUTE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }
}
