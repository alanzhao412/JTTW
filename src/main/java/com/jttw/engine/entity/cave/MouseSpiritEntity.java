package com.jttw.engine.entity.cave;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A Mouse Spirit: one of Lady Earth Flow's kin, a rat-demon not quite as tall as a man's waist,
 * upright, quick and frail. They come in packs, and strike one of them and the rest of the pack
 * turns on you. Each darts in, bites, and scurries back out of reach before it darts in again, so
 * a pack keeps circling: a bite is little, but they are many.
 */
public class MouseSpiritEntity extends CaveMonster {

    /** Two of a same-level character's ordinary hits to kill; a bite of 7% of their health, before resistance. */
    public static final double HITS = 2, BITE = 0.07, SPEED = 0.36;
    /** How long it scurries back after a bite, and how long before it can bite again, in ticks. */
    public static final int SCURRY = 14, BITE_EVERY = 22;
    /** How many of its kind a captain brings. */
    public static final int PACK = 4;

    public MouseSpiritEntity(EntityType<? extends MouseSpiritEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // as a level-37 one; each sets its own by its level (become)
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BITE * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BITE;
    }

    @Override
    public int escorts() {
        return PACK;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new BiteAndScurryGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // strike one, and the pack comes
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Whether {@code target} is close enough to bite. */
    public boolean inReach(LivingEntity target) {
        double r = getBbWidth() / 2 + 1.1 + target.getBbWidth() / 2;
        return distanceToSqr(target) <= r * r + 0.5;
    }

    @Override
    public float getVoicePitch() {
        return 1.7f + (random.nextFloat() - 0.5f) * 0.3f;
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.3f, 1.6f);
    }

    /** Dart in, bite, scurry back a few blocks; again. */
    static final class BiteAndScurryGoal extends Goal {

        private final MouseSpiritEntity mob;
        private int scurry, rest;

        BiteAndScurryGoal(MouseSpiritEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30f, 30f);
            if (rest > 0) rest--;
            if (scurry > 0) {
                scurry--;
                if (scurry % 7 == 6) {
                    Vec3 away = mob.position().subtract(t.position()).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
                    // off to one side as well as back, so a pack wheels round its prey
                    Vec3 side = new Vec3(-away.z, 0, away.x).scale(mob.getId() % 2 == 0 ? 0.7 : -0.7);
                    Vec3 to = mob.position().add(away.scale(4)).add(side.scale(2));
                    mob.getNavigation().moveTo(to.x, to.y, to.z, 1.3);
                }
                return;
            }
            if (rest == 0 && mob.inReach(t)) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(t);
                rest = BITE_EVERY;
                scurry = SCURRY;
                return;
            }
            if (mob.getNavigation().isDone() || mob.tickCount % 8 == 0) mob.getNavigation().moveTo(t, 1.25);
        }
    }
}
