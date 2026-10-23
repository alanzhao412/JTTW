package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * An Elephant-Demon (象精): one of the White Elephant's followers, a hulking grey elephant-headed
 * demon in rough armour with a heavy mace, slow and hard to fell. When its foe is
 * {@value #CHARGE_NEAR} to {@value #CHARGE_FAR} blocks off it lowers its tusks and <b>charges</b>
 * ({@value #CHARGE_TICKS} ticks, every {@value #CHARGE_EVERY} or so): what it meets is gored, a blow
 * of {@value #GORE_PERCENT}%, and flung aside.
 */
public class ElephantDemonEntity extends LionCamelMonster {

    public static final int GORE_PERCENT = 26;
    /** Fourteen ordinary hits; a blow of 30% of a same-level character's health. */
    public static final double HITS = 14, BLOW = 0.30, GORE = GORE_PERCENT / 100.0, SPEED = 0.22;
    public static final int CHARGE_EVERY = 120, CHARGE_TICKS = 18;
    public static final double CHARGE_NEAR = 4, CHARGE_FAR = 12;

    private int charge = CHARGE_EVERY / 2, charging;
    private Vec3 chargeDir = Vec3.ZERO;
    private final List<LivingEntity> gored = new ArrayList<>();

    public ElephantDemonEntity(EntityType<? extends ElephantDemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 1.4)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.FOLLOW_RANGE, 26)
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The gore's hurt. */
    public float goreDamage() {
        return (float) (blowAt(mobLevel(), isCaptain()) * GORE / BLOW);
    }

    /** Whether it is charging. */
    public boolean isCharging() {
        return charging > 0;
    }

    /** Lowers its tusks and charges toward {@code target}. */
    public boolean charge(LivingEntity target) {
        Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        if (flat.lengthSqr() < 1e-4) return false;
        chargeDir = flat.normalize();
        charging = CHARGE_TICKS;
        gored.clear();
        playSound(SoundEvents.RAVAGER_ROAR, 1f, 0.5f);
        return true;
    }

    /** One tick of its charge. Returns who it gored this tick. */
    public List<LivingEntity> chargeTick() {
        List<LivingEntity> hit = new ArrayList<>();
        setDeltaMovement(chargeDir.x * 0.75, getDeltaMovement().y, chargeDir.z * 0.75);
        setYRot((float) Math.toDegrees(Math.atan2(-chargeDir.x, chargeDir.z)));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        AABB box = getBoundingBox().inflate(0.4).move(chargeDir.scale(0.6));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof LionCamelMonster) && !(e instanceof DemonKingEntity)
                        && !gored.contains(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            e.hurt(damageSources().mobAttack(this), goreDamage());
            Vec3 side = new Vec3(-chargeDir.z, 0, chargeDir.x);
            if (side.dot(e.position().subtract(position())) < 0) side = side.scale(-1);
            e.push(side.x * 1.2 + chargeDir.x * 0.6, 0.5, side.z * 1.2 + chargeDir.z * 0.6);
            e.hurtMarked = true;
            gored.add(e);
            hit.add(e);
        }
        if (--charging <= 0 || horizontalCollision) charging = 0;
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (charging > 0) {
            getNavigation().stop();
            chargeTick();
            return;
        }
        if (charge > 0) charge--;
        LivingEntity t = getTarget();
        if (charge == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d >= CHARGE_NEAR && d <= CHARGE_FAR && hasLineOfSight(t) && charge(t)) charge = CHARGE_EVERY + random.nextInt(40);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Charge", charge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        charge = tag.getInt("Charge");
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.6f, 0.7f);
    }
}
