package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
 * A cloud crane (云鹤), the immortals' bird, white with black-tipped wings and a red crown, that
 * keeps the skies of Mount Longevity: it flies, never falls, and harries any stranger who comes up
 * to the mountain (not the temple's guests). From above it <b>swoops</b>: a dive at its quarry, a
 * stab of its beak and its blow and a half, and whoever it strikes is thrown back (beware the
 * island's edge). It keeps within {@value #RANGE} blocks of where it came into the world. A fight
 * with one is no deed either way.
 */
public class CloudCraneEntity extends WuzhuangMonster {

    /** Four of a same-level character's ordinary hits; a beak of 11% of their health. */
    public static final double HITS = 4, BLOW = 0.11;
    public static final int LEVEL = 80, RANGE = 40;
    /** The swoop: from how high over its quarry, how far off, how fast; how much harder it strikes, how far it throws; how often. */
    public static final double SWOOP_OVER = 3, SWOOP_WITHIN = 16, SWOOP_SPEED = 0.9, SWOOP_HIT = 1.5, THROW = 1.2;
    public static final int SWOOP_EVERY = 100, SWOOP_FOR = 30;

    private static final EntityDataAccessor<Boolean> SWOOPING = SynchedEntityData.defineId(CloudCraneEntity.class, EntityDataSerializers.BOOLEAN);

    private int swoopCooldown = 10;

    public CloudCraneEntity(EntityType<? extends CloudCraneEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 15, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.FOLLOW_RANGE, 32);
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
        builder.define(SWOOPING, false);
    }

    /** Whether it is diving at its quarry (for its pose: wings swept back). */
    public boolean isSwooping() {
        return entityData.get(SWOOPING);
    }

    public void showSwooping(boolean swooping) {
        entityData.set(SWOOPING, swooping);
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
        goalSelector.addGoal(1, new SwoopGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p instanceof Player pl && !Wuzhuang.isGuest(pl)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        restrictTo(blockPosition(), RANGE);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (swoopCooldown > 0) swoopCooldown--;
    }

    /** Its swoop's strike on {@code target}: its blow and a half, and the target thrown back. Returns whether it struck. */
    public boolean swoopHit(LivingEntity target) {
        target.invulnerableTime = 0;
        boolean hit = target.hurt(damageSources().mobAttack(this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * SWOOP_HIT));
        if (hit) {
            Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : away.normalize();
            target.push(away.x * THROW, 0.35, away.z * THROW);
            target.hurtMarked = true;
            setLastHurtMob(target);
        }
        playSound(SoundEvents.PARROT_HURT, 1.2f, 0.6f);
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public float getVoicePitch() {
        return 0.55f + random.nextFloat() * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    /** From above its quarry: a dive straight at it, striking what it meets. */
    static final class SwoopGoal extends Goal {

        private final CloudCraneEntity crane;
        private int left;

        SwoopGoal(CloudCraneEntity crane) {
            this.crane = crane;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (crane.swoopCooldown > 0) return false;
            LivingEntity t = crane.getTarget();
            if (t == null || !t.isAlive() || !crane.hasLineOfSight(t)) return false;
            double over = crane.getY() - t.getY();
            double across = Math.hypot(crane.getX() - t.getX(), crane.getZ() - t.getZ());
            return over >= SWOOP_OVER && across <= SWOOP_WITHIN;
        }

        @Override
        public boolean canContinueToUse() {
            return left > 0 && crane.getTarget() != null && crane.getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            left = SWOOP_FOR;
            crane.getNavigation().stop();
            crane.showSwooping(true);
            crane.playSound(SoundEvents.PHANTOM_SWOOP, 1.2f, 1.4f);
        }

        @Override
        public void stop() {
            crane.showSwooping(false);
            crane.swoopCooldown = SWOOP_EVERY;
            left = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = crane.getTarget();
            if (t == null) return;
            left--;
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(crane.position());
            Vec3 v = to.lengthSqr() < 1e-4 ? Vec3.ZERO : to.normalize().scale(SWOOP_SPEED);
            crane.setDeltaMovement(v);
            crane.getLookControl().setLookAt(t, 30f, 30f);
            crane.setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
            crane.setYBodyRot(crane.getYRot());
            if (crane.level() instanceof ServerLevel server && left % 3 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, crane.getX(), crane.getY() + 0.5, crane.getZ(), 1, 0.2, 0.2, 0.2, 0.01);
            }
            if (crane.getBoundingBox().inflate(0.4).intersects(t.getBoundingBox())) {
                crane.swoopHit(t);
                left = 0;
            }
        }
    }
}
