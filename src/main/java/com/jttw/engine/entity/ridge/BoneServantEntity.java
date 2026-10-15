package com.jttw.engine.entity.ridge;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * A Bone Servant: a skeleton four blocks tall, clumsy and slow, that swings a great bone like a
 * club. Each blow is telegraphed: it stops, raises the club over its head for most of a second,
 * then brings it down, and whatever is still in reach takes a hit that can take a third to half of
 * a level-20 character's health and throws them back. Then it must lift the club again.
 */
public class BoneServantEntity extends RidgeMonster {

    /** Ticks from raising the club to bringing it down. */
    public static final int WINDUP = 16;
    /** Ticks after a blow before it can start the next. */
    public static final int RECOVERY = 30;
    /** How far its blow reaches from its edge. */
    public static final double REACH = 2.6;

    /** Eight of a same-level character's ordinary hits to kill; a blow of half their health, before resistance. */
    public static final double HITS = 8, BLOW = 0.5, SPEED = 0.17;

    private static final EntityDataAccessor<Boolean> RAISING = SynchedEntityData.defineId(BoneServantEntity.class, EntityDataSerializers.BOOLEAN);

    public BoneServantEntity(EntityType<? extends BoneServantEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // as a level-20 one; each sets its own by its level (become)
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.5);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAISING, false);
    }

    /** Whether its club is up, about to come down. */
    public boolean isRaising() {
        return entityData.get(RAISING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new SlamGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE)); // at its size, a club
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** Whether {@code target} is within its blow. */
    public boolean inReach(LivingEntity target) {
        double r = getBbWidth() / 2 + REACH + target.getBbWidth() / 2;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        return dx * dx + dz * dz <= r * r && target.getY() < getY() + getBbHeight() && target.getY() + target.getBbHeight() > getY() - 1;
    }

    /** Brings the club down: the target, if still in reach, takes the blow; the ground shakes either way. */
    public void slam(@Nullable LivingEntity target) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.RAVAGER_ATTACK, 1.4f, 0.6f);
        if (level() instanceof ServerLevel server) {
            BlockState under = level().getBlockState(blockPosition().below());
            var ahead = getLookAngle().scale(2.0).add(position());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), ahead.x, getY() + 0.1, ahead.z, 30, 0.8, 0.1, 0.8, 0.15);
        }
        if (target != null && target.isAlive() && inReach(target)) doHurtTarget(target);
    }

    // ── A heavy thing ───────────────────────────────────────────────────

    @Override
    public float getVoicePitch() {
        return 0.5f + (random.nextFloat() - 0.5f) * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SKELETON_STEP, 0.8f, 0.45f);
    }

    @Override
    protected float getSoundVolume() {
        return 1.5f;
    }

    /** Close in, stop, raise the club, bring it down, recover; again. */
    static final class SlamGoal extends Goal {

        private final BoneServantEntity mob;
        private int raised = -1, rest;

        SlamGoal(BoneServantEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            raised = -1;
            mob.entityData.set(RAISING, false);
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 20f, 20f);
            if (rest > 0) rest--;
            if (raised >= 0) {
                // committed: it does not move or turn its blow until it lands
                mob.getNavigation().stop();
                if (++raised >= WINDUP) {
                    mob.slam(t);
                    raised = -1;
                    rest = RECOVERY;
                    mob.entityData.set(RAISING, false);
                }
                return;
            }
            if (rest == 0 && mob.inReach(t)) {
                raised = 0;
                mob.entityData.set(RAISING, true);
                mob.playSound(SoundEvents.SKELETON_AMBIENT, 1.2f, 0.4f);
                return;
            }
            if (mob.getNavigation().isDone() || mob.tickCount % 10 == 0) mob.getNavigation().moveTo(t, 1.0);
        }
    }
}
