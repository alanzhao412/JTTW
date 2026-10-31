package com.jttw.engine.entity.spider;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.spider.Venom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A spider demon (蜘蛛小妖): one of the lesser spiders of Silk-Web Ridge, the Seven Spider Spirits'
 * brood that never learned a woman's shape, a spider the size of a calf under a mask-like face of
 * many eyes. Quick and frail: its bite carries the ridge's venom; and from {@value #STRAND_NEAR} to
 * {@value #STRAND_FAR} blocks it rears up and, {@value #REAR} ticks later, spits a strand of silk
 * that pins what it strikes ({@link SilkStrandEntity}); a captain spits two. Strike one and the rest
 * near it turn on you. It leaps at its quarry.
 */
public class SpiderDemonEntity extends SilkMonster {

    /** Three ordinary hits to kill; a bite of 6.5%, a strand of 5% that pins for {@value #STRAND_PIN} ticks. */
    public static final double HITS = 3, BITE = 0.065, STRAND = 0.05, SPEED = 0.34;
    public static final int STRAND_EVERY = 80, REAR = 12, STRAND_PIN = 30, BITE_VENOM = 3;
    public static final double STRAND_NEAR = 4, STRAND_FAR = 14;

    private static final EntityDataAccessor<Boolean> REARING = SynchedEntityData.defineId(SpiderDemonEntity.class, EntityDataSerializers.BOOLEAN);

    private int strand = STRAND_EVERY / 2, rearing = -1;

    public SpiderDemonEntity(EntityType<? extends SpiderDemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BITE * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BITE;
    }

    @Override
    protected double[] specialShares() {
        return new double[]{STRAND};
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REARING, false);
    }

    /** Whether it is reared up, about to spit. */
    public boolean isRearing() {
        return entityData.get(REARING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** A strand's hurt. */
    public float strandDamage() {
        return hitOf(STRAND);
    }

    /** Spits silk at {@code target}: one strand, a captain two, fanned. Returns them. */
    public List<SilkStrandEntity> spit(LivingEntity target) {
        List<SilkStrandEntity> spat = new ArrayList<>();
        Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.5) - getY(0.55), target.getZ() - getZ());
        int n = isCaptain() ? 2 : 1;
        for (int i = 0; i < n; i++) {
            float fan = n == 1 ? 0 : (i == 0 ? -8 : 8);
            SilkStrandEntity s = new SilkStrandEntity(level(), this, to.yRot((float) Math.toRadians(fan)), strandDamage(), STRAND_PIN);
            level().addFreshEntity(s);
            spat.add(s);
        }
        playSound(SoundEvents.LLAMA_SPIT, 1f, 0.7f);
        return spat;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (rearing >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (++rearing >= REAR) {
                rearing = -1;
                entityData.set(REARING, false);
                if (t != null && t.isAlive()) spit(t);
                strand = STRAND_EVERY;
            }
            return;
        }
        if (strand > 0) strand--;
        if (strand == 0 && t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (d > STRAND_NEAR && d < STRAND_FAR && hasLineOfSight(t)) {
                rearing = 0;
                entityData.set(REARING, true);
                playSound(SoundEvents.SPIDER_AMBIENT, 1f, 0.6f);
            }
        }
    }

    /** Its bite carries the ridge's venom. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity l) Venom.envenom(l, BITE_VENOM, 0, this);
        return hit;
    }

    @Override
    public boolean onClimbable() {
        return horizontalCollision;
    }

    @Override
    public float getVoicePitch() {
        return 0.75f + random.nextFloat() * 0.15f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.3f, 0.8f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Strand", strand);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        strand = tag.getInt("Strand");
    }
}
