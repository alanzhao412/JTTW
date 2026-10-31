package com.jttw.engine.entity.spider;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.spider.Venom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * One of the stinging sons (chapter 73: when the Monkey King came against the cave, the Seven Spider
 * Spirits' adopted sons turned into a swarm of stinging insects and covered him): a waist-high
 * insect demon on whirring wings, of three kinds by its colours: the <b>Honeybee Son</b> (蜜蜂), the
 * <b>Hornet Son</b> (马蜂) and the <b>Gadfly Son</b> (牛蜢). It flies (never falls), harries its quarry
 * and stings, the sting carrying the ridge's venom; and now and then it hangs still in the air,
 * buzzing, for {@value #BUZZ} ticks, then darts straight at its quarry: a sting of
 * {@value #DART}, more venom.
 */
public class StingerSonEntity extends SilkMonster {

    /** Three ordinary hits to kill; a sting of 7%, its dart 9%. */
    public static final double HITS = 3, STING = 0.07, DART = 0.09;
    public static final int DART_EVERY = 90, BUZZ = 14, DART_FOR = 24, STING_VENOM = 3, DART_VENOM = 5;
    public static final double DART_WITHIN = 12, DART_SPEED = 0.85;
    /** Its three kinds, by colour. */
    public static final String[] KINDS = {"honeybee_son", "hornet_son", "gadfly_son"};
    public static final int HONEYBEE = 0, HORNET = 1, GADFLY = 2;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(StingerSonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(StingerSonEntity.class, EntityDataSerializers.INT);
    /** What it is doing, for its pose: flying, buzzing before a dart, darting. */
    public static final int FLYING = 0, BUZZING = 1, DARTING = 2;

    private int dartCooldown = DART_EVERY / 2;

    public StingerSonEntity(EntityType<? extends StingerSonEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, STING * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return STING;
    }

    @Override
    protected double[] specialShares() {
        return new double[]{DART};
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
        builder.define(MODE, FLYING);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public void setKind(int kind) {
        entityData.set(KIND, Math.floorMod(kind, KINDS.length));
    }

    public int mode() {
        return entityData.get(MODE);
    }

    void setMode(int mode) {
        entityData.set(MODE, mode);
    }

    /** Its kind's name: the Honeybee, Hornet or Gadfly Son. */
    @Override
    protected Component getTypeName() {
        return nameWithLevel(Component.translatable("entity.jttw_engine." + KINDS[kind()]), mobLevel(), isCaptain());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        setKind(random.nextInt(KINDS.length));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** A captain's escorts are of its kind. */
    @Override
    public void bringEscorts(ServerLevel level) {
        super.bringEscorts(level);
        for (StingerSonEntity s : level.getEntitiesOfClass(StingerSonEntity.class, getBoundingBox().inflate(5), s -> s != this && !s.isCaptain())) {
            s.setKind(kind());
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DartGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (dartCooldown > 0) dartCooldown--;
    }

    /** Its sting carries the ridge's venom. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity l) Venom.envenom(l, STING_VENOM, 0, this);
        return hit;
    }

    /** Its dart's sting on {@code target}: {@value #DART} of a same-level character's health, and venom. Returns whether it struck. */
    public boolean dartSting(LivingEntity target) {
        target.invulnerableTime = 0;
        boolean hit = target.hurt(damageSources().mobAttack(this), hitOf(DART));
        if (hit) {
            Venom.envenom(target, DART_VENOM, 0, this);
            setLastHurtMob(target);
        }
        playSound(SoundEvents.BEE_STING, 1.2f, 0.8f);
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && mode() == BUZZING && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.CRIT, getRandomX(0.6), getY(0.6), getRandomZ(0.6), 0, 0, 0);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public float getVoicePitch() {
        return kind() == HORNET ? 0.7f : kind() == GADFLY ? 0.9f : 1.15f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP_AGGRESSIVE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(tag.getInt("Kind"));
    }

    /** Hangs still, buzzing, then darts straight at its quarry, stinging what it meets. */
    static final class DartGoal extends Goal {

        private final StingerSonEntity son;
        private int buzz, left;

        DartGoal(StingerSonEntity son) {
            this.son = son;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (son.dartCooldown > 0) return false;
            LivingEntity t = son.getTarget();
            return t != null && t.isAlive() && son.hasLineOfSight(t) && son.distanceTo(t) <= DART_WITHIN && son.distanceTo(t) > 2.5;
        }

        @Override
        public boolean canContinueToUse() {
            return (buzz > 0 || left > 0) && son.getTarget() != null && son.getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            buzz = BUZZ;
            left = DART_FOR;
            son.getNavigation().stop();
            son.setMode(BUZZING);
            son.playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 1.5f, 1.4f);
        }

        @Override
        public void stop() {
            son.setMode(FLYING);
            son.dartCooldown = DART_EVERY;
            buzz = left = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = son.getTarget();
            if (t == null) return;
            son.getLookControl().setLookAt(t, 30f, 30f);
            if (buzz > 0) {
                // hangs where it is, trembling, its sting drawn
                buzz--;
                son.setDeltaMovement(son.getDeltaMovement().scale(0.5));
                if (buzz == 0) {
                    son.setMode(DARTING);
                    son.playSound(SoundEvents.BEE_STING, 1f, 1.6f);
                }
                return;
            }
            left--;
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(son.position());
            Vec3 v = to.lengthSqr() < 1e-4 ? Vec3.ZERO : to.normalize().scale(DART_SPEED);
            son.setDeltaMovement(v);
            son.setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
            son.setYBodyRot(son.getYRot());
            if (son.getBoundingBox().inflate(0.4).intersects(t.getBoundingBox())) {
                son.dartSting(t);
                left = 0;
            }
        }
    }
}
