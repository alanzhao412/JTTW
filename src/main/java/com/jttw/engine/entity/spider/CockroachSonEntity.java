package com.jttw.engine.entity.spider;

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
import net.minecraft.world.phys.Vec3;

/**
 * The Cockroach Son (蜚蠊精): the heaviest of the Seven Spider Spirits' adopted sons, a squat,
 * broad insect demon in a glossy shell of chestnut and black, long feelers sweeping before it,
 * six spined legs, armoured like a war-cart. It walks slowly and hits hard; and when its quarry is
 * a few blocks off it lowers its head and scrapes the ground for {@value #SCRAPE} ticks, then
 * charges straight at where it was, up to {@value #CHARGE_FOR} ticks: what it meets is struck
 * ({@value #RAM}) and thrown back. It cannot turn in its charge: step aside.
 */
public class CockroachSonEntity extends SilkMonster {

    /** Eleven ordinary hits to kill; a blow of 11.5%, its ram 12%. */
    public static final double HITS = 11, BLOW = 0.115, RAM = 0.12, SPEED = 0.22, CHARGE_SPEED = 0.65, THROW = 1.3;
    public static final int SCRAPE = 16, CHARGE_FOR = 20, CHARGE_EVERY = 110;
    public static final double CHARGE_NEAR = 3, CHARGE_FAR = 12;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CockroachSonEntity.class, EntityDataSerializers.INT);
    public static final int WALKING = 0, SCRAPING = 1, CHARGING = 2;

    private int cooldown = CHARGE_EVERY / 2, phase = -1;
    private Vec3 heading = Vec3.ZERO;

    public CockroachSonEntity(EntityType<? extends CockroachSonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.1);
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
        return new double[]{RAM};
    }

    @Override
    public int escorts() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, WALKING);
    }

    /** Walking, scraping the ground before a charge, or charging. */
    public int chargeState() {
        return entityData.get(STATE);
    }

    /** Shows it walking, scraping or charging (the gallery's pose). */
    public void showChargeState(int state) {
        entityData.set(STATE, state);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The ram's hurt. */
    public float ramDamage() {
        return hitOf(RAM);
    }

    /** Lowers its head at {@code target} and begins to scrape the ground: the charge comes after. */
    public void beginCharge(LivingEntity target) {
        phase = 0;
        Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
        heading = to.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, getYRot()) : to.normalize();
        setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
        setYBodyRot(getYRot());
        entityData.set(STATE, SCRAPING);
        playSound(SoundEvents.RAVAGER_STEP, 1.2f, 1.5f);
    }

    /** Its ram on {@code e}: struck, thrown back along its charge. Returns whether it struck. */
    public boolean ram(LivingEntity e) {
        e.invulnerableTime = 0;
        boolean hit = e.hurt(damageSources().mobAttack(this), ramDamage());
        if (hit) {
            e.push(heading.x * THROW, 0.4, heading.z * THROW);
            e.hurtMarked = true;
            setLastHurtMob(e);
        }
        playSound(SoundEvents.RAVAGER_ATTACK, 1.2f, 1.3f);
        return hit;
    }

    private void endCharge() {
        phase = -1;
        cooldown = CHARGE_EVERY;
        entityData.set(STATE, WALKING);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (phase >= 0) {
            getNavigation().stop();
            phase++;
            if (phase < SCRAPE) {
                if (level() instanceof ServerLevel server && phase % 3 == 0) {
                    BlockState under = level().getBlockState(blockPosition().below());
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX() - heading.x, getY() + 0.1, getZ() - heading.z,
                            6, 0.3, 0.05, 0.3, 0.1);
                }
                return;
            }
            if (phase == SCRAPE) {
                entityData.set(STATE, CHARGING);
                playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.6f);
            }
            setDeltaMovement(heading.x * CHARGE_SPEED, getDeltaMovement().y, heading.z * CHARGE_SPEED);
            setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
            setYBodyRot(getYRot());
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4),
                    e -> e != this && e.isAlive() && !(e instanceof SilkKin) && !isAlliedTo(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                ram(e);
                endCharge();
                return;
            }
            if (horizontalCollision || phase >= SCRAPE + CHARGE_FOR) endCharge();
            return;
        }
        if (cooldown > 0) cooldown--;
        if (cooldown == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d >= CHARGE_NEAR && d <= CHARGE_FAR && hasLineOfSight(t)) beginCharge(t);
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.7f, 0.5f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Charge", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("Charge");
    }
}
