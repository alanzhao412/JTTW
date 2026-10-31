package com.jttw.engine.entity.blackwind;

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
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Lingxuzi (凌虚子, "Master of the Void"), the grey-wolf spirit who passes for a Daoist immortal: an old
 * grey wolf in a Daoist's blue-grey robe and black-edged cloak, his hair in a topknot under a jade crown,
 * a long grey beard, a horsetail whisk in his left hand and a straight sword in his right, a gourd of
 * pills at his belt; on his way to the Black Bear's Buddha-Robe Feast with a tray of elixirs when the
 * Monkey King struck him down (chapter 17). A miniboss before the cave's door.
 * <ul>
 *   <li><b>his sword</b>;</li>
 *   <li><b>the wolf's pounce</b>: every {@value #POUNCE_EVERY} ticks or so he crouches low ({@value #CROUCH}
 *       ticks, his robes gathering) and springs at his foe, coming down on all within {@value #LANDING} blocks;</li>
 *   <li><b>the elixir</b>: once, brought under half his health, he stops ({@value #DRINK} ticks) and swallows
 *       one of his pills, and is a fifth of his health the better for it.</li>
 * </ul>
 */
public class LingxuziEntity extends BlackWindChief {

    /** Ten ordinary hits (a captain's two and a half times, and more); a blow of 12%, his pounce 11% (as a regular's: a miniboss's hits are scaled up). */
    public static final double HITS = 10, BLOW = 0.12, POUNCE = 0.11, SPEED = 0.3, ELIXIR = 0.2;
    public static final int POUNCE_EVERY = 100, CROUCH = 12, DRINK = 24;
    public static final double LANDING = 2.8, POUNCE_FAR = 14;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(LingxuziEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, CROUCHING = 1, LEAPING = 2, DRINKING = 3;

    private int pounce = POUNCE_EVERY / 2, stage = -1, airborne;
    private boolean drank;

    public LingxuziEntity(EntityType<? extends LingxuziEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(CHIEF_LEVEL) * CAPTAIN_HEALTH * HEALTH_OVER)
                .add(Attributes.ATTACK_DAMAGE, MINIBOSS_HIT * MobScaling.typicalHealth(CHIEF_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 8)
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
    protected double[] specialShares() {
        return new double[]{POUNCE};
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
    }

    /** What he is doing: {@link #IDLE}, {@link #CROUCHING} to spring, {@link #LEAPING}, {@link #DRINKING} his elixir. */
    public int state() {
        return entityData.get(STATE);
    }

    /** Holds it in {@code state} (the gallery's pictures; its own wind-ups set it as they go). */
    public void pose(int state) {
        entityData.set(STATE, state);
    }


    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public float pounceDamage() {
        return hitOf(POUNCE);
    }

    /** Crouches to spring at {@code target}. */
    public void crouch() {
        stage = 0;
        entityData.set(STATE, CROUCHING);
        playSound(SoundEvents.WOLF_GROWL, 1.4f, 0.7f);
    }

    /** Comes down from his spring: all near where he lands struck and thrown. Returns who. */
    public List<LivingEntity> land() {
        List<LivingEntity> hit = caught(LANDING, 1.5);
        for (LivingEntity e : hit) {
            e.hurt(damageSources().mobAttack(this), pounceDamage());
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) e.knockback(0.9, -away.x, -away.z);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.7f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.8, getZ(), 6, LANDING / 2, 0.2, LANDING / 2, 0);
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 20, LANDING / 2, 0.05, LANDING / 2, 0.05);
        }
        return hit;
    }

    /** Swallows one of his elixirs: a fifth of his health back, once. */
    public void drink() {
        stage = 0;
        drank = true;
        entityData.set(STATE, DRINKING);
        playSound(SoundEvents.GENERIC_DRINK, 1.2f, 0.8f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (pounce > 0) pounce--;
        LivingEntity t = getTarget();
        int s = state();
        if (s == DRINKING) {
            getNavigation().stop();
            if (++stage >= DRINK) {
                heal((float) (getMaxHealth() * ELIXIR));
                if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.6, getZ(), 16, 0.5, 0.6, 0.5, 0);
                playSound(SoundEvents.PLAYER_LEVELUP, 0.7f, 1.6f);
                entityData.set(STATE, IDLE);
                stage = -1;
            }
            return;
        }
        if (s == CROUCHING) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (++stage >= CROUCH) {
                if (t != null && t.isAlive()) {
                    Vec3 to = t.position().subtract(position());
                    double d = Math.max(1, to.horizontalDistance());
                    setDeltaMovement(to.x / d * Math.min(1.4, d * 0.16), 0.55 + d * 0.02, to.z / d * Math.min(1.4, d * 0.16));
                    hurtMarked = true;
                }
                entityData.set(STATE, LEAPING);
                airborne = 0;
                playSound(SoundEvents.WOLF_AMBIENT, 1.2f, 0.6f);
            }
            return;
        }
        if (s == LEAPING) {
            airborne++;
            if ((onGround() && airborne > 3) || airborne > 40) {
                land();
                entityData.set(STATE, IDLE);
                pounce = POUNCE_EVERY;
                stage = -1;
            }
            return;
        }
        if (!drank && getHealth() < getMaxHealth() * 0.5f) {
            drink();
            return;
        }
        if (pounce == 0 && t != null && t.isAlive() && distanceTo(t) < POUNCE_FAR && hasLineOfSight(t)) crouch();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Pounce", pounce);
        tag.putBoolean("Drank", drank);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pounce = tag.getInt("Pounce");
        drank = tag.getBoolean("Drank");
    }
}
