package com.jttw.engine.entity.fire;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.fire.SamadhiFire;
import net.minecraft.core.BlockPos;
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

import java.util.List;

/**
 * A Furnace-Brick Brute (炉砖怪): when the Monkey King kicked over Laozi's Eight Trigrams Furnace,
 * some of its bricks, still burning, fell to earth and became the Flaming Mountains (chapter 59);
 * a few of them stood up. A hulk of fire-brick held together by the furnace's fire, it walks slowly
 * and hits hard; and when its foe is close it raises both fists and, {@value #WINDUP} ticks later,
 * brings them down on the ground: all within {@value #SLAM_RANGE} blocks are struck, thrown up and
 * set alight. Then it must gather itself ({@value #SLAM_EVERY} ticks).
 */
public class FurnaceBruteEntity extends FireMonster {

    /** Twelve ordinary hits to kill; a blow of 30% of a same-level character's health, the slam 22%. */
    public static final double HITS = 12, BLOW = 0.30, SLAM = 0.22, SPEED = 0.2;
    public static final int WINDUP = 16, SLAM_EVERY = 90;
    public static final double SLAM_RANGE = 4.0;

    private static final EntityDataAccessor<Boolean> RAISING = SynchedEntityData.defineId(FurnaceBruteEntity.class, EntityDataSerializers.BOOLEAN);

    private int slam = SLAM_EVERY / 2, raised = -1;

    public FurnaceBruteEntity(EntityType<? extends FurnaceBruteEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.25);
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
        builder.define(RAISING, false);
    }

    /** Whether its fists are up, about to come down. */
    public boolean isRaising() {
        return entityData.get(RAISING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The slam's hurt. */
    public float slamDamage() {
        return (float) (blowAt(mobLevel(), isCaptain()) * SLAM / BLOW);
    }

    /** Brings its fists down: all near it (but the mountain's own) struck, thrown up, set alight. Returns who. */
    public List<LivingEntity> slam() {
        List<LivingEntity> caught = level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(SLAM_RANGE, 2, SLAM_RANGE),
                e -> e != this && e.isAlive() && !SamadhiFire.spared(e) && !isAlliedTo(e)
                        && e.distanceToSqr(this) <= (SLAM_RANGE + 0.5) * (SLAM_RANGE + 0.5)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), slamDamage());
            e.push(0, 0.6, 0);
            e.hurtMarked = true;
            e.igniteForSeconds(4);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.6f);
        if (level() instanceof ServerLevel server) {
            BlockState under = level().getBlockState(blockPosition().below());
            server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 40, SLAM_RANGE / 2, 0.1, SLAM_RANGE / 2, 0.15);
            server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 50, SLAM_RANGE / 2, 0.1, SLAM_RANGE / 2, 0.05);
            server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.4, getZ(), 8, 1, 0.2, 1, 0);
        }
        return caught;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (slam > 0) slam--;
        LivingEntity t = getTarget();
        if (raised >= 0) {
            getNavigation().stop();
            if (++raised >= WINDUP) {
                slam();
                raised = -1;
                slam = SLAM_EVERY;
                entityData.set(RAISING, false);
            }
            return;
        }
        if (slam == 0 && t != null && t.isAlive() && distanceToSqr(t) <= SLAM_RANGE * SLAM_RANGE) {
            raised = 0;
            entityData.set(RAISING, true);
            playSound(SoundEvents.BLAZE_AMBIENT, 1.2f, 0.4f);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + getBbHeight() * (0.5 + random.nextDouble() * 0.5), getRandomZ(0.6), 0, 0.04, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Slam", slam);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        slam = tag.getInt("Slam");
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.9f, 0.6f);
    }
}
