package com.jttw.engine.entity.flowerfruit;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A Spear Ape (持枪猿兵): a macaque soldier of Flower-Fruit Mountain's army, drilled in the Monkey
 * King's days with arms carried off from the armoury of Aolai, in a leather cuirass, a red headband
 * and a long spear. It fights in the ranks; and when its foe is a few strides off it draws its spear
 * back ({@value #WINDUP} ticks, the warning) and lunges {@value #REACH} blocks, the spear striking
 * all in its line.
 */
public class SpearApeEntity extends FlowerFruitMonster {

    /** Six ordinary hits to kill; a thrust of 9% of a same-level character's health, the lunge 10%. */
    public static final double HITS = 6, BLOW = 0.09, LUNGE = 0.10, SPEED = 0.28;
    public static final int LUNGE_EVERY = 80, WINDUP = 14;
    /** From how near to how far it lunges, and how far the lunge carries its spear. */
    public static final double LUNGE_NEAR = 2.5, LUNGE_FAR = 6.5, REACH = 5.5, WIDTH = 1.3;

    private static final EntityDataAccessor<Boolean> DRAWING = SynchedEntityData.defineId(SpearApeEntity.class, EntityDataSerializers.BOOLEAN);

    private int cooldown = LUNGE_EVERY / 2, windup = -1;

    public SpearApeEntity(EntityType<? extends SpearApeEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.5);
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
        return new double[]{LUNGE};
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAWING, false);
    }

    /** Whether it has its spear drawn back, about to lunge (the warning). */
    public boolean isDrawing() {
        return entityData.get(DRAWING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public float lungeDamage() {
        return hitOf(LUNGE);
    }

    /** Draws its spear back to lunge; it comes {@value #WINDUP} ticks later. */
    public void drawBack() {
        windup = 0;
        entityData.set(DRAWING, true);
        playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 0.7f);
    }

    /** The lunge, along {@code dir}: it springs forward, its spear striking all in its line. Returns who. */
    public List<LivingEntity> lunge(Vec3 dir) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        List<LivingEntity> caught = new ArrayList<>();
        if (flat.lengthSqr() < 1e-4) return caught;
        flat = flat.normalize();
        Vec3 from = position().add(0, 0.9, 0);
        Vec3 to = from.add(flat.scale(REACH));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(WIDTH),
                e -> e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof FlowerFruitMonster)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            Vec3 at = e.position().add(0, e.getBbHeight() / 2, 0).subtract(from);
            double along = at.dot(flat);
            if (along < -0.5 || along > REACH + 0.5) continue;
            if (at.subtract(flat.scale(along)).horizontalDistance() > WIDTH + e.getBbWidth() / 2) continue;
            e.hurt(damageSources().mobAttack(this), lungeDamage());
            e.push(flat.x * 0.5, 0.15, flat.z * 0.5);
            e.hurtMarked = true;
            caught.add(e);
        }
        setDeltaMovement(flat.x * 0.9, 0.1, flat.z * 0.9);
        hurtMarked = true;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.8f);
        if (level() instanceof ServerLevel server) {
            for (int i = 1; i <= 5; i++) {
                Vec3 p = from.add(flat.scale(REACH * i / 5));
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
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
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (++windup >= WINDUP) {
                windup = -1;
                entityData.set(DRAWING, false);
                if (t != null) lunge(t.position().subtract(position()));
                cooldown = LUNGE_EVERY;
            }
            return;
        }
        if (cooldown == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d > LUNGE_NEAR && d < LUNGE_FAR && hasLineOfSight(t)) drawBack();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Lunge", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("Lunge");
    }

    @Override
    public float getVoicePitch() {
        return 1.25f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }
}
