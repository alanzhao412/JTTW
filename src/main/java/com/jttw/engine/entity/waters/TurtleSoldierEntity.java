package com.jttw.engine.entity.waters;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.phys.Vec3;

/**
 * A Turtle Soldier (龟兵), the heavy infantry of the Dragon Kings' host (a sea turtle of the court,
 * not the playable Turtle): slow, shelled, armoured, with a great round shield on its left arm and a
 * melon-headed mace in its right flipper. It swims, but slowly, and hits hard.
 *
 * <p>Its defence is twofold. Its shield takes blows that come at it from in front: they do only a
 * share of their harm (not what no shield stops, as magic). And when it is badly hurt it draws its
 * head and limbs into its shell a while, and then almost nothing hurts it, nor moves it; but it
 * neither moves nor strikes either, and it can do so only once in a while.
 */
public class TurtleSoldierEntity extends SeaMonster {

    /** Ten of a same-level character's ordinary hits to kill; a blow of 12% of their health, before resistance. */
    public static final double HITS = 10, BLOW = 0.12;
    /** The share of a frontal blow its shield stops, and how far in front a blow must come from (the cosine of the angle off its facing). */
    public static final float BLOCK = 0.6f;
    public static final double FRONT = 0.35;
    /** Below this share of its health it withdraws into its shell, for so many ticks, at most once each so many; the share of harm its shell then stops. */
    public static final float WITHDRAW_AT = 0.5f, SHELL = 0.8f;
    public static final int SHELL_TICKS = 60, SHELL_EVERY = 240;
    /** The entity event that tells the client it blocked a blow (it raises its shield). */
    public static final byte BLOCKED = 81;

    private static final EntityDataAccessor<Boolean> WITHDRAWN = SynchedEntityData.defineId(TurtleSoldierEntity.class, EntityDataSerializers.BOOLEAN);

    private int shell, shellCooldown;
    /** Client: when it last blocked, in its ticks (for the shield's raising). */
    public int lastBlock = -100;
    /** Client: how far drawn into its shell it is, 0..1, now and a tick ago (eased, for the renderer). */
    private float tuck, tuckO;

    public TurtleSoldierEntity(EntityType<? extends TurtleSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // as a level-47 one; each sets its own by its level (become)
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WITHDRAWN, false);
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
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.6, 40));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Whether it is drawn into its shell. */
    public boolean isWithdrawn() {
        return entityData.get(WITHDRAWN);
    }

    /** Draws into its shell, if it may. Returns whether it did. */
    public boolean withdraw() {
        if (isWithdrawn() || shellCooldown > 0) return false;
        entityData.set(WITHDRAWN, true);
        shell = SHELL_TICKS;
        shellCooldown = SHELL_EVERY;
        getNavigation().stop();
        playSound(SoundEvents.TURTLE_SHAMBLE, 1.4f, 0.6f);
        return true;
    }

    /** Whether its shield takes a blow from {@code source}: one from in front of it, that a shield can stop. */
    public boolean blocks(DamageSource source) {
        if (isWithdrawn() || source.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        Vec3 from = source.getSourcePosition();
        if (from == null) return false;
        Vec3 to = from.subtract(position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-6) return false;
        Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
        return to.normalize().dot(facing) > FRONT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0 && !isInvulnerableTo(source)) {
            if (isWithdrawn() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                amount *= 1 - SHELL;
            } else if (blocks(source)) {
                amount *= 1 - BLOCK;
                level().broadcastEntityEvent(this, BLOCKED);
                playSound(SoundEvents.SHIELD_BLOCK, 1f, 0.8f + random.nextFloat() * 0.2f);
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive() && getHealth() < getMaxHealth() * WITHDRAW_AT) withdraw();
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == BLOCKED) lastBlock = tickCount;
        else super.handleEntityEvent(id);
    }

    /** In its shell it strikes nothing. */
    @Override
    public boolean doHurtTarget(Entity target) {
        return !isWithdrawn() && super.doHurtTarget(target);
    }

    /** In its shell nothing moves it. */
    @Override
    public void knockback(double strength, double x, double z) {
        if (!isWithdrawn()) super.knockback(strength, x, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tuckO = tuck;
            tuck += ((isWithdrawn() ? 1 : 0) - tuck) * 0.35f;
        }
    }

    /** Client: how far drawn into its shell it is at {@code partial} of a tick. */
    public float tucked(float partial) {
        return tuckO + (tuck - tuckO) * partial;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (shellCooldown > 0) shellCooldown--;
        if (isWithdrawn()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            if (--shell <= 0) entityData.set(WITHDRAWN, false);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShellCooldown", shellCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shellCooldown = tag.getInt("ShellCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isWithdrawn() ? SoundEvents.TURTLE_EGG_CRACK : SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }
}
