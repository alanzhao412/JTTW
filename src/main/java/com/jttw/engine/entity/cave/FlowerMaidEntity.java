package com.jttw.engine.entity.cave;

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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * A Flower Maid: one of the flower spirits that wait on Lady Earth Flow in her palace, a girl in a
 * gown of petals with a flower in her hair. She fights with a hairpin, but what makes her
 * dangerous is her care of the household:
 * <ul>
 *   <li><b>tending</b>: every few seconds she mends the most wounded of the cave's creatures near
 *       her (a fifth of its health; of her mistress's, a little), so she is the one to kill first;</li>
 *   <li><b>fragrance</b>: pressed close, she breathes out a cloud of pollen that leaves everyone
 *       near her slow and weak for a few seconds.</li>
 * </ul>
 */
public class FlowerMaidEntity extends CaveMonster {

    /** Five of a same-level character's ordinary hits to kill; a stab of 15% of their health, before resistance. */
    public static final double HITS = 5, STAB = 0.15;
    /** How often she tends, how far, and how much she mends: of a creature's health, of her mistress's. */
    public static final int TEND_EVERY = 100;
    public static final double TEND_RANGE = 10, MEND = 0.2, MEND_MISTRESS = 0.03;
    /** Her pollen: how close one must be, how far it spreads, how often, how long it lasts. */
    public static final double POLLEN_NEAR = 3.5, POLLEN_RANGE = 4.5;
    public static final int POLLEN_EVERY = 160, POLLEN_LASTS = 80;

    private static final EntityDataAccessor<Integer> CASTING = SynchedEntityData.defineId(FlowerMaidEntity.class, EntityDataSerializers.INT);

    private int tend = TEND_EVERY / 2, pollen = POLLEN_EVERY / 4;

    public FlowerMaidEntity(EntityType<? extends FlowerMaidEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, STAB * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return STAB;
    }

    /** A captain brings two of her sisters. */
    @Override
    public int escorts() {
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, 0);
    }

    /** Whether her arms are raised in tending or breathing out pollen (for her pose). */
    public boolean isCasting() {
        return entityData.get(CASTING) > 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        int casting = entityData.get(CASTING);
        if (casting > 0) entityData.set(CASTING, casting - 1);
        if (--tend <= 0) {
            tend = TEND_EVERY;
            tendHousehold();
        }
        if (pollen > 0) pollen--;
        LivingEntity t = getTarget();
        if (pollen == 0 && t != null && t.isAlive() && distanceToSqr(t) <= POLLEN_NEAR * POLLEN_NEAR) {
            breatheOut(level().players());
            pollen = POLLEN_EVERY;
        }
    }

    /** The most wounded of the cave's creatures near her (never herself), or null if none is hurt. */
    @Nullable
    public LivingEntity mostWounded() {
        LivingEntity worst = null;
        double share = 1;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(TEND_RANGE),
                e -> e != this && e.isAlive() && (e instanceof CaveMonster || e instanceof LadyEarthFlowEntity))) {
            double s = e.getHealth() / e.getMaxHealth();
            if (s < share) {
                share = s;
                worst = e;
            }
        }
        return worst;
    }

    /** Mends the most wounded near her. Returns whom, or null. */
    @Nullable
    public LivingEntity tendHousehold() {
        LivingEntity patient = mostWounded();
        if (patient == null) return null;
        patient.heal((float) (patient.getMaxHealth() * (patient instanceof LadyEarthFlowEntity ? MEND_MISTRESS : MEND)));
        entityData.set(CASTING, 15);
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY(0.7), patient.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
            server.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY(1.1), getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        return patient;
    }

    /** Her pollen, over these players: those within its reach, and not watching or building, are slowed and weakened. Returns how many. */
    public int breatheOut(Collection<? extends Player> players) {
        int caught = 0;
        for (Player p : players) {
            if (p.isSpectator() || p.isCreative() || p.distanceToSqr(this) > POLLEN_RANGE * POLLEN_RANGE) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, POLLEN_LASTS, 1), this);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, POLLEN_LASTS, 0), this);
            caught++;
        }
        entityData.set(CASTING, 15);
        playSound(SoundEvents.AZALEA_LEAVES_BREAK, 1.2f, 0.7f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(1), getZ(), 80, POLLEN_RANGE / 2, 0.8, POLLEN_RANGE / 2, 0.01);
            server.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY(1), getZ(), 30, 1.5, 0.8, 1.5, 0.02);
        }
        return caught;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + (random.nextFloat() - 0.5f) * 0.15f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Tend", tend);
        tag.putInt("Pollen", pollen);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tend = tag.getInt("Tend");
        pollen = tag.getInt("Pollen");
    }
}
