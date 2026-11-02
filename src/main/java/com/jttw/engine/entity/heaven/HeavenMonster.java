package com.jttw.engine.entity.heaven;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.heaven.HeavenHostility;
import com.jttw.engine.world.heaven.HeavenRegions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * One of Heaven's host below its great names: its soldiers (regulars), the Twenty-Eight Lunar Mansions
 * (captains) and its gods and star lords (minibosses). Of {@link Zones#HEAVEN}, every level: its level
 * is the road's where it stands ({@link HeavenRegions#roadLevel}), a regular a level either way of it
 * within its region's band, a captain three to five over, a miniboss five.
 *
 * <p>All ride clouds: they fly, never fall, and with no foe hover low over the island they keep to,
 * drifting about their post ({@link HoverPatrolGoal}). They fight as {@link HeavenHostility} says: on
 * sight a demon or one of bad karma; anyone else only once struck, and then those near rally.
 * Thrown off their island they ride back to it.
 */
public abstract class HeavenMonster extends ZoneMonster implements HeavenFolk {

    /** How far from its post one of the wild keeps, drifting about its island. */
    public static final int WANDER = 10;
    /** How far under its post it may fall or be thrown before it rides its cloud back. */
    public static final double LOST_BELOW = 10;

    private static final EntityDataAccessor<Integer> WINDING = SynchedEntityData.defineId(HeavenMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(HeavenMonster.class, EntityDataSerializers.INT);

    private final Grudge grudge = new Grudge();
    @Nullable
    private AreaBlow blow;

    protected HeavenMonster(EntityType<? extends HeavenMonster> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.HEAVEN;
    }

    /** Attributes of one of {@code hits} ordinary hits and a blow of {@code blow}, as a level-20 one (each is set to its own level). */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(double hits, double blow, double speed, double flying) {
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, hits * com.jttw.engine.entity.MobScaling.typicalHit(20))
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, blow * com.jttw.engine.entity.MobScaling.typicalHealth(20))
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, speed)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED, flying)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 32)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    /** Its area blow, made once (none for most regulars). */
    @Nullable
    protected AreaBlow makeBlow() {
        return null;
    }

    @Nullable
    public AreaBlow blow() {
        if (blow == null) blow = makeBlow();
        return blow;
    }

    /** Whether every one of its kind is a miniboss (a star lord, a god): those of its kind the court seats. */
    public boolean alwaysMiniboss() {
        return false;
    }

    @Override
    public boolean isMiniboss() {
        return alwaysMiniboss() || super.isMiniboss();
    }

    /** Always a miniboss's strength and level, whoever brings one of a miniboss's kind. */
    @Override
    public void become(int level, boolean captain) {
        super.become(level, captain || alwaysMiniboss());
    }

    // ── Its level: the road's ───────────────────────────────────────────

    /** The road's level where it stands. */
    @Override
    public int routeLevel() {
        if (!(level() instanceof ServerLevel server)) return regularLevel();
        return HeavenRegions.roadLevel(server.getSeed(), getX(), getZ());
    }

    @Override
    public int rollRegularLevel(RandomSource r) {
        return HeavenRegions.regularLevel(routeLevel(), r);
    }

    @Override
    public int rollCaptainLevel(RandomSource r) {
        return alwaysMiniboss() ? HeavenRegions.minibossLevel(routeLevel()) : HeavenRegions.captainLevel(routeLevel(), r);
    }

    /** Captains are set by whoever brings them (the spawner, the court): none rolls one itself. */
    @Override
    public float captainChance() {
        return alwaysMiniboss() ? 1f : 0f;
    }

    @Override
    public int escorts() {
        return 0;
    }

    // ── What it is ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDING, 0);
        builder.define(VARIANT, 0);
    }

    /** Which of its kind it is (a Lunar Mansion's, a star lord's), 0 for one kind of one. */
    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.floorMod(v, Math.max(1, variants())));
    }

    /** How many of its kind there are (28 Lunar Mansions, 9 star lords); 1 for most. */
    public int variants() {
        return 1;
    }

    /** Ticks left of its area blow's wind-up (0: none): for its pose. */
    public int winding() {
        return entityData.get(WINDING);
    }

    @Override
    public void setWinding(int ticks) {
        entityData.set(WINDING, Math.max(0, ticks));
    }

    @Override
    public Grudge grudge() {
        return grudge;
    }

    @Override
    public int crime() {
        return isMiniboss() ? HeavenHostility.GENERAL : isCaptain() ? HeavenHostility.STAR_GENERAL : HeavenHostility.SOLDIER;
    }

    @Override
    public String crimeWhat() {
        return isMiniboss() ? "you struck down a general of Heaven" : isCaptain() ? "you struck down a star general of Heaven"
                : "you struck down a soldier of Heaven";
    }

    @Override
    public int vanillaXpShare() {
        return isMiniboss() ? 10 : isCaptain() ? 6 : 2;
    }

    /** Its kind's name, its level: white, purple for a captain or a miniboss ("Captain" for a soldier's captain). */
    @Override
    protected Component getTypeName() {
        if (!isCaptain() || !namedRank()) return nameWithLevel(kindName(), mobLevel(), isCaptain());
        return Component.empty().append(kindName()).append(" · Lv " + mobLevel())
                .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(PURPLE)));
    }

    /** Whether its captain or miniboss goes by a name of its own (a Lunar Mansion's, a star lord's), not "Captain". */
    protected boolean namedRank() {
        return alwaysMiniboss();
    }

    /** Its kind's name (a Lunar Mansion's own, a star lord's own). */
    protected Component kindName() {
        return getType().getDescription();
    }

    // ── How it moves: on its cloud ──────────────────────────────────────

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        addAttackGoals();
        goalSelector.addGoal(5, new HoverPatrolGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, HeavenHostility::attacksOnSight));
    }

    /** Its fighting goals, at priorities 1 to 4 (a melee charge by default). */
    protected void addAttackGoals() {
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return !windingUp() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !windingUp() && super.canContinueToUse();
            }
        });
    }

    /** Whether it is winding up its area blow (it holds still). */
    public boolean windingUp() {
        AreaBlow b = blow();
        return b != null && b.winding();
    }

    /**
     * With no foe, it drifts about its post a little over the island it keeps to: now and then to a spot
     * of ground within {@value #WANDER} blocks of it, hovering just over it.
     */
    public static final class HoverPatrolGoal extends Goal {
        private final HeavenMonster mob;
        @Nullable
        private Vec3 to;

        public HoverPatrolGoal(HeavenMonster mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.windingUp() || mob.getRandom().nextInt(60) != 0) return false;
            to = spot();
            return to != null;
        }

        @Nullable
        private Vec3 spot() {
            BlockPos home = mob.hasRestriction() ? mob.getRestrictCenter() : mob.blockPosition();
            int reach = mob.hasRestriction() ? (int) Math.min(WANDER, mob.getRestrictRadius()) : WANDER;
            for (int i = 0; i < 6; i++) {
                int x = home.getX() + mob.getRandom().nextInt(2 * reach + 1) - reach, z = home.getZ() + mob.getRandom().nextInt(2 * reach + 1) - reach;
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, home.getY() + 4, z);
                for (int dy = 0; dy < 12; dy++, p.move(0, -1, 0)) {
                    BlockState below = mob.level().getBlockState(p.below());
                    if (below.isSolid() && mob.level().getBlockState(p).isAir() && mob.level().getBlockState(p.above()).isAir()) {
                        return new Vec3(x + 0.5, p.getY() + 0.3, z + 0.5);
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return to != null && mob.getTarget() == null && !mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(to.x, to.y, to.z, 0.7);
        }

        @Override
        public void stop() {
            to = null;
        }
    }

    // ── Whom it fights ──────────────────────────────────────────────────

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

    /**
     * Those a blow on all near it falls on: the players it fights (its foe, those it attacks on sight,
     * whoever has struck it), never its allies; and whatever is fighting it.
     */
    @Override
    public List<LivingEntity> foesWithin(double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r), e -> e != this && e.isAlive())) {
            if (e.distanceTo(this) <= r + e.getBbWidth() && isFoe(e)) out.add(e);
        }
        return out;
    }

    /** Whether a blow on all near it falls on {@code e}. */
    public boolean isFoe(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof HeavenFolk || isAlliedTo(e)) return false;
        if (e instanceof Player p) {
            return !p.isSpectator() && !p.isCreative() && canAttack(p)
                    && (p == getTarget() || HeavenHostility.attacksOnSight(p) || grudge.struckFirst(p) || p == getLastHurtByMob());
        }
        return e == getTarget() || e instanceof Mob m && m.getTarget() == this;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        AreaBlow b = blow();
        if (b != null) b.tick(getTarget());
        if (tickCount % 20 == 3) rideBack();
    }

    /** Fallen or thrown far under its post, it rides its cloud back to it. Returns whether it did. */
    public boolean rideBack() {
        if (!hasRestriction()) return false;
        BlockPos post = getRestrictCenter();
        if (getY() > post.getY() - LOST_BELOW) return false;
        getNavigation().stop();
        teleportTo(post.getX() + 0.5, post.getY() + 0.5, post.getZ() + 0.5);
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 16, 0.5, 0.3, 0.5, 0.02);
        }
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    /** The wild ones go when no one is near; those the court seats stay. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isPersistenceRequired();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return !isPersistenceRequired();
    }

    /** For tests and scenes: those of Heaven's host round {@code at}. */
    public static List<HeavenMonster> near(Level level, Vec3 at, double r) {
        return level.getEntitiesOfClass(HeavenMonster.class, new AABB(at, at).inflate(r));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        grudge.save(tag);
        if (hasRestriction()) {
            tag.put("Post", NbtUtils.writeBlockPos(getRestrictCenter()));
            tag.putFloat("PostReach", getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        grudge.load(tag);
        NbtUtils.readBlockPos(tag, "Post").ifPresent(post -> restrictTo(post, (int) tag.getFloat("PostReach")));
    }
}
