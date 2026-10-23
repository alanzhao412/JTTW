package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ox-Head (牛头), one of the two guards of the underworld who fetch souls to judgement: a huge
 * man with the head of an ox, in black armour, with a steel fork. Kept at a distance, he lowers
 * his horns and <b>charges</b>: whatever he runs down is gored and thrown back.
 */
public class OxHeadEntity extends HellMonster {

    /** Seven of a same-level character's ordinary hits to kill; a blow of 16% of their health. */
    public static final double HITS = 7, BLOW = 0.16;
    public static final int LEVEL = 58;
    /** His charge: how far off his quarry must be, how fast he runs. */
    public static final double CHARGE_FROM = 4.5, CHARGE_TO = 16, CHARGE_SPEED = 0.85;
    /** How long he lowers his horns, how long he runs at most, how often he charges, in ticks. */
    public static final int WIND_UP = 12, RUN = 16, CHARGE_EVERY = 110;
    /** A goring: so much more than his blow, thrown back so far. */
    public static final double GORE = 1.5, THROW = 1.6;

    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(OxHeadEntity.class, EntityDataSerializers.BOOLEAN);

    /** Ticks until he may charge again (the first comes soon after he has a quarry). */
    private int chargeCooldown = 10;

    public OxHeadEntity(EntityType<? extends OxHeadEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32)
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
    public int escorts() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
    }

    /** Whether his horns are down for a charge (for his pose). */
    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    /** Lowers or raises his horns (the charge does; so does the gallery, to show it). */
    public void showCharging(boolean charging) {
        entityData.set(CHARGING, charging);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (chargeCooldown > 0) chargeCooldown--;
    }

    /** Gores {@code target}: his blow and a half, and it is thrown back. Returns whether it struck. */
    public boolean gore(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        target.invulnerableTime = 0;
        boolean hit = target.hurt(damageSources().mobAttack(this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * GORE));
        if (hit) {
            Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : away.normalize();
            target.push(away.x * THROW, 0.45, away.z * THROW);
            target.hurtMarked = true;
            setLastHurtMob(target);
        }
        playSound(SoundEvents.RAVAGER_ATTACK, 1.2f, 0.8f);
        return hit;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    /** Head down, a moment; then a run straight at his quarry; whatever he meets on the way, he gores. */
    static final class ChargeGoal extends Goal {

        private final OxHeadEntity ox;
        private int wind, run;
        private Vec3 heading = Vec3.ZERO;

        ChargeGoal(OxHeadEntity ox) {
            this.ox = ox;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (ox.chargeCooldown > 0) return false;
            LivingEntity t = ox.getTarget();
            if (t == null || !t.isAlive() || !ox.onGround() || !ox.hasLineOfSight(t)) return false;
            double d = ox.distanceTo(t);
            return d >= CHARGE_FROM && d <= CHARGE_TO;
        }

        @Override
        public boolean canContinueToUse() {
            return wind > 0 || run > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            wind = WIND_UP;
            run = 0;
            ox.getNavigation().stop();
            ox.entityData.set(CHARGING, true);
            ox.playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 0.9f);
        }

        @Override
        public void stop() {
            ox.entityData.set(CHARGING, false);
            ox.chargeCooldown = CHARGE_EVERY;
            wind = run = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = ox.getTarget();
            if (wind > 0) {
                if (t != null) ox.getLookControl().setLookAt(t, 30f, 30f);
                if (--wind == 0) {
                    Vec3 to = t == null ? ox.getLookAngle() : t.position().subtract(ox.position());
                    to = to.multiply(1, 0, 1);
                    heading = to.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : to.normalize();
                    run = RUN;
                }
                return;
            }
            if (run <= 0) return;
            run--;
            ox.setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
            ox.setYBodyRot(ox.getYRot());
            ox.setDeltaMovement(heading.x * CHARGE_SPEED, ox.getDeltaMovement().y, heading.z * CHARGE_SPEED);
            if (ox.level() instanceof ServerLevel server && run % 2 == 0) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, ox.getX(), ox.getY() + 0.2, ox.getZ(), 1, 0.2, 0, 0.2, 0.01);
            }
            for (LivingEntity hit : ox.level().getEntitiesOfClass(LivingEntity.class, ox.getBoundingBox().inflate(0.6),
                    e -> e != ox && e.isAlive() && !(e instanceof HellMonster) && ox.canAttack(e))) {
                ox.gore(hit);
                run = 0;
            }
            if (ox.horizontalCollision) run = 0;
        }
    }
}
