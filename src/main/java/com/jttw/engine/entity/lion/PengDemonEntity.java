package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.MobScaling;
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
import net.minecraft.world.phys.Vec3;

/**
 * A Peng-Demon (鹏精): one of the Great Peng's followers, a bird-demon with a hooked beak, dark
 * pinions and talons, a spear in its hands; the city's demons were the three kings' kin and
 * followers, and these are the Peng's. Light and quick. Every {@value #SWOOP_EVERY} ticks or so,
 * a foe {@value #SWOOP_NEAR} to {@value #SWOOP_FAR} blocks off, it beats up into the air and
 * <b>swoops</b>: down on the foe, a blow of {@value #SWOOP_PERCENT}% and knocked flat. It glides
 * down rather than falls, and takes no harm from a fall.
 */
public class PengDemonEntity extends LionCamelMonster {

    public static final int SWOOP_PERCENT = 24;
    /** Six ordinary hits; a spear thrust of 16% of a same-level character's health. */
    public static final double HITS = 6, BLOW = 0.16, SWOOP = SWOOP_PERCENT / 100.0, SPEED = 0.32;
    public static final int SWOOP_EVERY = 100;
    public static final double SWOOP_NEAR = 3, SWOOP_FAR = 14;

    private static final EntityDataAccessor<Boolean> ALOFT = SynchedEntityData.defineId(PengDemonEntity.class, EntityDataSerializers.BOOLEAN);

    private int swoop = SWOOP_EVERY / 2;
    /** 0 on the ground, 1 rising, 2 diving. */
    private int flight;

    public PengDemonEntity(EntityType<? extends PengDemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 32);
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
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ALOFT, false);
    }

    /** Whether it is on the wing (for its model: wings spread). */
    public boolean isAloft() {
        return entityData.get(ALOFT);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The swoop's hurt. */
    public float swoopDamage() {
        return (float) (blowAt(mobLevel(), isCaptain()) * SWOOP / BLOW);
    }

    /** Spreads its pinions as on the wing, or folds them (as it lands; the gallery shows them spread). */
    public void spreadWings(boolean spread) {
        entityData.set(ALOFT, spread);
    }

    /** Beats up into the air, to swoop. Returns whether it did (only from the ground). */
    public boolean rise() {
        if (!onGround()) return false;
        setDeltaMovement(getDeltaMovement().x * 0.3, 1.05, getDeltaMovement().z * 0.3);
        hurtMarked = true;
        flight = 1;
        entityData.set(ALOFT, true);
        playSound(SoundEvents.PHANTOM_FLAP, 1.2f, 0.8f);
        return true;
    }

    /** Dives at {@code target} from where it is. */
    public void dive(LivingEntity target) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
        if (to.lengthSqr() < 1e-4) return;
        setDeltaMovement(to.normalize().scale(1.25));
        hurtMarked = true;
        flight = 2;
        playSound(SoundEvents.PHANTOM_SWOOP, 1.2f, 1.0f);
    }

    /** Comes down on {@code target}: its blow, knocked flat. Returns whether it struck. */
    public boolean strike(LivingEntity target) {
        if (isAlliedTo(target) || !target.isAlive()) return false;
        boolean hit = target.hurt(damageSources().mobAttack(this), swoopDamage());
        if (hit) {
            Vec3 push = getDeltaMovement().multiply(1, 0, 1);
            if (push.lengthSqr() > 1e-4) push = push.normalize();
            target.push(push.x * 0.8, 0.3, push.z * 0.8);
            target.hurtMarked = true;
        }
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (swoop > 0) swoop--;
        LivingEntity t = getTarget();
        if (flight == 1 && getDeltaMovement().y <= 0.05) {
            if (t != null && t.isAlive()) dive(t);
            else flight = 0;
        }
        if (flight == 2 && t != null && t.isAlive() && getBoundingBox().inflate(0.7).intersects(t.getBoundingBox())) {
            strike(t);
            flight = 0;
        }
        if (flight != 0 && onGround() && getDeltaMovement().y <= 0 && tickCount % 2 == 0) flight = 0;
        if (flight == 0 && isAloft() && onGround()) entityData.set(ALOFT, false);
        if (swoop == 0 && flight == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d >= SWOOP_NEAR && d <= SWOOP_FAR && hasLineOfSight(t) && rise()) swoop = SWOOP_EVERY + random.nextInt(40);
        }
    }

    /** It glides rather than falls. */
    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < -0.12 && flight != 2) setDeltaMovement(v.x, -0.12, v.z);
        if (level().isClientSide && isAloft() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.CLOUD, getRandomX(0.8), getY() + 1.2, getRandomZ(0.8), 0, -0.02, 0);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Swoop", swoop);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        swoop = tag.getInt("Swoop");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }
}
