package com.jttw.engine.entity.wild;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * An ogre (山夜叉), a mountain yaksha: a brutish giant over three blocks tall, red-skinned and
 * pot-bellied, two horns in a wild black mane, tusks jutting up from its jaw and eyes like lamps, a
 * tiger-skin loincloth, iron bracers studded with spikes, a necklace of skulls, and a huge iron
 * wolf-tooth club (狼牙棒) over its shoulder. Rare: one alone, at night, in the mountains and the
 * swamps.
 *
 * <p>Slow, but every blow is heavy and throws its foe back; it is hardly moved itself. Every
 * {@value #SLAM_EVERY} ticks or so, a foe within {@value #SLAM_REACH} blocks, it heaves the club up
 * over its head ({@value #SLAM_WINDUP} ticks: time to get clear) and brings it down on the
 * ground: everything round it within {@value #SLAM_REACH} blocks takes {@value #SLAM_PERCENT}% of
 * its blow and is flung up and away.
 */
public class OgreEntity extends WildYaoguai {

    /** Twelve ordinary hits; a club's blow of 12% of a same-level character's health. Slow. */
    public static final double HITS = 12, BLOW = 0.12, SPEED = 0.21;
    public static final int MIN_LEVEL = 15, MAX_LEVEL = 20;
    public static final int SLAM_EVERY = 140, SLAM_WINDUP = 20, SLAM_PERCENT = 75;
    public static final double SLAM_REACH = 4.0;

    private static final EntityDataAccessor<Integer> SLAM = SynchedEntityData.defineId(OgreEntity.class, EntityDataSerializers.INT);

    private int slamCooldown = SLAM_EVERY / 2;

    public OgreEntity(EntityType<? extends OgreEntity> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(MIN_LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(MIN_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ATTACK_KNOCKBACK, 1.8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override public int minLevel() { return MIN_LEVEL; }
    @Override public int maxLevel() { return MAX_LEVEL; }
    @Override protected double hitsToKill() { return HITS; }
    @Override protected double blowShare() { return BLOW; }
    @Override public double hardestShare() { return Math.max(BLOW, BLOW * SLAM_PERCENT / 100.0); }
    @Override public int vanillaXpShare() { return 5; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLAM, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SlamGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** How far through heaving its club up for a slam it is (0 when not): the model raises the club. */
    public int slamWindup() {
        return entityData.get(SLAM);
    }

    /** For the gallery: shown with the club heaved up. */
    public void showSlam() {
        entityData.set(SLAM, SLAM_WINDUP / 2);
    }

    /** The slam's hurt. */
    public float slamDamage() {
        return (float) (blowAt(mobLevel()) * SLAM_PERCENT / 100.0);
    }

    /** Brings the club down: everything round within {@value #SLAM_REACH} blocks, but its own kind, hurt and flung. Returns them. */
    public List<LivingEntity> slam() {
        List<LivingEntity> struck = level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(SLAM_REACH, 2, SLAM_REACH),
                e -> e != this && e.isAlive() && !(e instanceof OgreEntity) && !isAlliedTo(e) && distanceToSqr(e) <= SLAM_REACH * SLAM_REACH * 1.5
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : struck) {
            if (e.hurt(damageSources().mobAttack(this), slamDamage())) {
                Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
                away = away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : away.normalize().scale(1.1 * (1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
                e.push(away.x, 0.55, away.z);
                e.hurtMarked = true;
            }
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.6f);
        playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
        if (level() instanceof ServerLevel server) {
            BlockState ground = level().getBlockState(blockPosition().below());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 80, SLAM_REACH * 0.5, 0.1, SLAM_REACH * 0.5, 0.2);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 2, 0.6, 0, 0.6, 0);
        }
        return struck;
    }

    /** Heaves the club up, stands, and slams it down. */
    static final class SlamGoal extends Goal {
        private final OgreEntity ogre;

        SlamGoal(OgreEntity ogre) {
            this.ogre = ogre;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = ogre.getTarget();
            return ogre.slamCooldown <= 0 && t != null && t.isAlive() && ogre.onGround()
                    && ogre.distanceToSqr(t) <= SLAM_REACH * SLAM_REACH;
        }

        @Override
        public boolean canContinueToUse() {
            return ogre.slamWindup() > 0;
        }

        @Override
        public void start() {
            ogre.entityData.set(SLAM, SLAM_WINDUP);
            ogre.getNavigation().stop();
            ogre.playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 0.6f);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ogre.getNavigation().stop();
            LivingEntity t = ogre.getTarget();
            if (t != null) ogre.getLookControl().setLookAt(t, 30f, 30f);
            int w = ogre.slamWindup() - 1;
            ogre.entityData.set(SLAM, w);
            if (w == 0) {
                ogre.slam();
                ogre.slamCooldown = SLAM_EVERY + ogre.random.nextInt(40);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (slamCooldown > 0) slamCooldown--;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return distance > 5 && super.causeFallDamage(distance - 5, multiplier, source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SlamCooldown", slamCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        slamCooldown = tag.getInt("SlamCooldown");
    }

    @Override
    public float getVoicePitch() {
        return 0.55f + (random.nextFloat() - 0.5f) * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.6f, 0.7f);
    }
}
