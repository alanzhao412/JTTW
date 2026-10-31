package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
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
 * A Great Baboon (巨狒): one of the hulking baboons of Flower-Fruit Mountain's army, the weight of
 * its ranks, a mane of grey and a dog's long muzzle full of fangs, bands of iron on its arms. It
 * walks on its knuckles and hits hard; and when its foe is close it rears up and beats its chest
 * ({@value #WINDUP} ticks, the warning) and then brings both fists down on the ground: all within
 * {@value #POUND_RANGE} blocks are struck and thrown up.
 */
public class GreatBaboonEntity extends FlowerFruitMonster {

    /** Eleven ordinary hits to kill; a blow of 11.5% of a same-level character's health, the pound 11%. */
    public static final double HITS = 11, BLOW = 0.115, POUND = 0.11, SPEED = 0.24;
    public static final int WINDUP = 20, POUND_EVERY = 100;
    public static final double POUND_RANGE = 4.5;

    private static final EntityDataAccessor<Boolean> BEATING = SynchedEntityData.defineId(GreatBaboonEntity.class, EntityDataSerializers.BOOLEAN);

    private int cooldown = POUND_EVERY / 2, windup = -1;

    public GreatBaboonEntity(EntityType<? extends GreatBaboonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 6)
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
    protected double[] specialShares() {
        return new double[]{POUND};
    }

    @Override
    public int escorts() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEATING, false);
    }

    /** Whether it is up on its legs beating its chest, about to pound (the warning). */
    public boolean isBeating() {
        return entityData.get(BEATING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public float poundDamage() {
        return hitOf(POUND);
    }

    /** Rears up and beats its chest; the pound comes {@value #WINDUP} ticks later. */
    public void rearUp() {
        windup = 0;
        entityData.set(BEATING, true);
        playSound(SoundEvents.RAVAGER_ROAR, 0.9f, 1.3f);
    }

    /** Brings both fists down: all near it (but the mountain's own) struck and thrown up. Returns who. */
    public List<LivingEntity> pound() {
        List<LivingEntity> caught = level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(POUND_RANGE, 2, POUND_RANGE),
                e -> e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof FlowerFruitMonster)
                        && e.distanceToSqr(this) <= (POUND_RANGE + 0.5) * (POUND_RANGE + 0.5)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), poundDamage());
            e.push(0, 0.55, 0);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.9f);
        if (level() instanceof ServerLevel server) {
            BlockState under = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
            if (!under.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 50, POUND_RANGE / 2, 0.1, POUND_RANGE / 2, 0.15);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 2, 1, 0, 1, 0);
        }
        return caught;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        LivingEntity t = getTarget();
        if (windup >= 0) {
            getNavigation().stop();
            if (++windup >= WINDUP) {
                windup = -1;
                entityData.set(BEATING, false);
                pound();
                cooldown = POUND_EVERY;
            } else if (windup % 5 == 0) {
                playSound(SoundEvents.WARDEN_HEARTBEAT, 1f, 1.4f);
            }
            return;
        }
        if (cooldown == 0 && t != null && t.isAlive() && distanceToSqr(t) <= POUND_RANGE * POUND_RANGE) rearUp();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Pound", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("Pound");
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
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
}
