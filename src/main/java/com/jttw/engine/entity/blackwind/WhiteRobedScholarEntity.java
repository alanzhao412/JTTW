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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * The White-Robed Scholar (白衣秀士), a white-flower snake spirit (白花蛇怪) in a scholar's shape: slender,
 * in flowing white robes edged in black, a scholar's black cap, a folding fan; but a serpent's slit eyes
 * and forked tongue in his pale face, scaled white hands, and from under his hem the coils of his own
 * white tail. He sat on the grass with the Black Bear and Lingxuzi when the Monkey King came on them, and
 * was the first struck dead (chapter 17). A miniboss before the cave's door.
 * <ul>
 *   <li><b>his fan</b>, slashing;</li>
 *   <li><b>venom</b>: every {@value #SPIT_EVERY} ticks, his foe a little way off, he draws his head back
 *       hissing ({@value #REAR} ticks) and spits a gob of venom (a hit and poison);</li>
 *   <li><b>the coils</b>: his foe close, he gathers himself ({@value #COIL} ticks) and lashes his tail round
 *       him: all within {@value #LASH_RANGE} blocks struck and flung aside, and slowed.</li>
 * </ul>
 */
public class WhiteRobedScholarEntity extends BlackWindChief {

    /** Nine ordinary hits (a captain's two and a half times, and more); a blow of 11%, his venom and his lash 10% (scaled as a miniboss's). */
    public static final double HITS = 9, BLOW = 0.11, VENOM = 0.10, LASH = 0.10, SPEED = 0.29;
    public static final int SPIT_EVERY = 70, REAR = 12, LASH_EVERY = 90, COIL = 12;
    public static final double SPIT_NEAR = 4, SPIT_FAR = 16, LASH_RANGE = 3.5;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(WhiteRobedScholarEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, REARING = 1, COILING = 2;

    private int spit = SPIT_EVERY / 2, lash = LASH_EVERY / 2, stage = -1;

    public WhiteRobedScholarEntity(EntityType<? extends WhiteRobedScholarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(CHIEF_LEVEL) * CAPTAIN_HEALTH * HEALTH_OVER)
                .add(Attributes.ATTACK_DAMAGE, MINIBOSS_HIT * MobScaling.typicalHealth(CHIEF_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 6)
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
        return new double[]{VENOM, LASH};
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
    }

    /** What he is doing: {@link #IDLE}, {@link #REARING} to spit, {@link #COILING} to lash. */
    public int state() {
        return entityData.get(STATE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public float venomDamage() {
        return hitOf(VENOM);
    }

    public float lashDamage() {
        return hitOf(LASH);
    }

    /** Spits a gob of venom at {@code target}. */
    public VenomBoltEntity spit(LivingEntity target) {
        Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY(), target.getZ() - getZ());
        VenomBoltEntity bolt = new VenomBoltEntity(level(), this, to, venomDamage());
        level().addFreshEntity(bolt);
        playSound(SoundEvents.LLAMA_SPIT, 1.2f, 0.8f);
        return bolt;
    }

    /** Lashes his tail round him: all near struck, flung aside and slowed. Returns who. */
    public List<LivingEntity> lash() {
        List<LivingEntity> hit = caught(LASH_RANGE, 1.5);
        for (LivingEntity e : hit) {
            e.hurt(damageSources().mobAttack(this), lashDamage());
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            Vec3 side = new Vec3(-away.z, 0, away.x); // the tail sweeps round, flinging them sideways and out
            Vec3 fling = away.normalize().add(side.normalize()).normalize();
            if (away.lengthSqr() > 1e-4) e.knockback(1.0, -fling.x, -fling.z);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            e.hurtMarked = true;
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 2.2, getY() + 0.4, getZ() + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
            }
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (spit > 0) spit--;
        if (lash > 0) lash--;
        LivingEntity t = getTarget();
        int s = state();
        if (s != IDLE) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            int windup = s == REARING ? REAR : COIL;
            if (++stage >= windup) {
                if (s == REARING) {
                    if (t != null && t.isAlive()) spit(t);
                    spit = SPIT_EVERY;
                } else {
                    lash();
                    lash = LASH_EVERY;
                }
                entityData.set(STATE, IDLE);
                stage = -1;
            }
            return;
        }
        if (t == null || !t.isAlive()) return;
        double d = distanceTo(t);
        if (lash == 0 && d <= LASH_RANGE) {
            stage = 0;
            entityData.set(STATE, COILING);
            playSound(SoundEvents.ARMADILLO_ROLL, 1.2f, 0.6f);
        } else if (spit == 0 && d > SPIT_NEAR && d < SPIT_FAR && hasLineOfSight(t)) {
            stage = 0;
            entityData.set(STATE, REARING);
            playSound(SoundEvents.CAT_HISS, 1.4f, 0.6f);
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.9f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_HISS;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Spit", spit);
        tag.putInt("Lash", lash);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spit = tag.getInt("Spit");
        lash = tag.getInt("Lash");
    }
}
