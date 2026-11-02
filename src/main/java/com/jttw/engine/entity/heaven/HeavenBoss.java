package com.jttw.engine.entity.heaven;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.world.heaven.HeavenHostility;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
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

import javax.annotation.Nullable;

/**
 * One of Heaven's great names: a Heavenly King at his gate, Li Jing, Nezha, Erlang Shen. A gym's leader
 * of his region ({@link #zone()}, one of {@link com.jttw.engine.world.Zones#HEAVEN_REGIONS}): of its band's
 * top and three (a lesser one, two), built for one character of the band's top: {@value #HITS} of their
 * ordinary hits, a blow of {@value #BLOW} of their health (six kill), and an area blow no harder, telegraphed
 * ({@link AreaBlow}). He fights as all Heaven's host does ({@link HeavenHostility}): a demon or one of bad
 * karma on sight, anyone else once struck.
 *
 * <p>He keeps no lair a usurper could take: Heaven's court sends him back to his post twenty minutes after
 * he falls ({@link com.jttw.engine.world.heaven.HeavenCourt}).
 */
public abstract class HeavenBoss extends ZoneBoss implements HeavenFolk {

    /** Ordinary hits of a character of his region's top to kill, and his blow, of their health: a gym's leader's. */
    public static final double HITS = 70, BLOW = 0.19;
    /** How far from his post he keeps, and how far under it he may be thrown before he is back on it. */
    public static final int POST = 14, LOST_BELOW = 12;

    private static final EntityDataAccessor<Integer> WINDING = SynchedEntityData.defineId(HeavenBoss.class, EntityDataSerializers.INT);

    private final Grudge grudge = new Grudge();
    @Nullable
    private AreaBlow blow;

    protected HeavenBoss(EntityType<? extends HeavenBoss> type, Level level) {
        super(type, level);
        xpReward = 200;
    }

    /** His attributes, as a gym's leader of a region whose top is {@code top}. */
    public static AttributeSupplier.Builder attributesFor(int top, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(top))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(top))
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.5);
    }

    /** Whether he is a lesser one of his region, a level under its head. */
    public boolean lesser() {
        return false;
    }

    @Override
    public int nativeLevel() {
        return lesser() ? lesserLevelOver(zone()) : levelOver(zone());
    }

    @Override
    public int lairRadius() {
        return POST;
    }

    /** Heaven's court keeps his post: no usurper takes it. */
    @Override
    public boolean headsLair() {
        return false;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.YELLOW;
    }

    /** His area blow, made once. */
    protected abstract AreaBlow makeBlow();

    public AreaBlow blow() {
        if (blow == null) blow = makeBlow();
        return blow;
    }

    @Override
    public double[] specialShares() {
        return new double[]{blow().spec().share()};
    }

    // ── What he is ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDING, 0);
    }

    public int winding() {
        return entityData.get(WINDING);
    }

    @Override
    public void setWinding(int ticks) {
        entityData.set(WINDING, Math.max(0, ticks));
    }

    public boolean windingUp() {
        return blow().winding();
    }

    @Override
    public Grudge grudge() {
        return grudge;
    }

    @Override
    public int vanillaXpShare() {
        return 20;
    }

    // ── How he fights ───────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return !windingUp() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !windingUp() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, HeavenHostility::attacksOnSight));
    }

    /** He fights the players Heaven's host fights: a demon or one of bad karma, his foe, whoever struck him lately. */
    @Override
    protected boolean fights(Player p) {
        return HeavenHostility.attacksOnSight(p) || struckLately(p) || p == getTarget();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null && getTarget() == target) grudge.targeted(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof Player p && !isAlliedTo(p)) grudge.struckBy(p);
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && !isAlliedTo(attacker)) {
            HeavenHostility.rally(this, attacker);
        }
        return hurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        blow().tick(getTarget());
        if (tickCount % 20 == 5) backToPost();
    }

    /** Thrown far under his post, he is back on it at once. Returns whether he was. */
    public boolean backToPost() {
        if (!hasRestriction()) return false;
        BlockPos post = getRestrictCenter();
        if (getY() > post.getY() - LOST_BELOW) return false;
        getNavigation().stop();
        teleportTo(post.getX() + 0.5, post.getY() + 0.2, post.getZ() + 0.5);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0;
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 24, 0.6, 0.5, 0.6, 0.02);
        }
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        grudge.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        grudge.load(tag);
    }
}
