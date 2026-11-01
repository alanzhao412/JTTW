package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.flowerfruit.FlowerFruit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Six-Eared Macaque (六耳猕猴, chapters 56-58), who took the Monkey King's shape so exactly that
 * neither the Bodhisattva Guanyin nor the Jade Emperor nor the Judge of the Dead could tell them
 * apart, and only the Buddha named him: a monkey of the four kinds that are of no kind, whose six
 * ears hear all things a thousand li off. He beat the Tang Monk, stole his luggage and took
 * Flower-Fruit Mountain, meaning to go west himself with a false pilgrimage of his own.
 *
 * <p>The strongest boss in the world, and an optional one: <b>level {@value #LEVEL}</b>, past the
 * players' cap, in a sanctum of his own at the back of the Water Curtain Cave (no one has to face him
 * to have the rest of the mountain). Measured against a capped character ({@link ZoneBoss#measuredAt}):
 * {@value #HITS} of a level-100 character's ordinary hits, more than any other boss; his staff's blow
 * {@value #STAFF} of one's health, the top of a boss's band, so three or four kill and none one-shots.
 * A group of capped players' fight, and out of reach below the cap. His kit is the Monkey King's own,
 * mirrored:
 * <ul>
 *   <li>his <b>iron staff</b>, banded in gold: blows of a third of a character's health;</li>
 *   <li><b>the whirling staff</b>: he spins it over his head ({@value #SPIN_WINDUP} ticks, the
 *       warning) and sweeps it round him: all within {@value #SPIN_RANGE} blocks struck, thrown back;</li>
 *   <li><b>the cloud somersault</b> (筋斗云): he crouches as a cloud gathers under him
 *       ({@value #DASH_WINDUP} ticks), then shoots along a line up to {@value #DASH_REACH} blocks on it,
 *       striking all in his path, and stands past his foe;</li>
 *   <li><b>hair clones</b> (身外身法, from two thirds): he plucks a pinch of hair, chews it and blows
 *       ({@value #CLONE_WINDUP} ticks): {@value #CLONES} of himself spring up round him ({@value #CLONES_LATE}
 *       from a third), frail, a light mob's blow, gone in half a minute; again every
 *       {@value #CLONE_EVERY} ticks. They show no bar;</li>
 *   <li><b>the Dharma image</b> (法天象地, at a third): he grows half again his height, quicker, and his
 *       staff grows with him: <b>the heaven-splitting blow</b>, the staff raised high
 *       ({@value #SLAM_WINDUP} ticks, its line lit gold on the ground) and brought down, a shock along
 *       the line {@value #SLAM_LENGTH} blocks long;</li>
 *   <li><b>six ears</b>: nothing is hidden from him: he finds the invisible, the sneaking, as well as any.</li>
 * </ul>
 * He keeps to his sanctum: should anything bring him far out of it he is back at once.
 */
public class SixEaredMacaqueEntity extends ZoneBoss {

    public static final int LEVEL = 120;
    /** The level of character he is measured against: the players' cap ({@link ZoneBoss#measuredAt}). */
    public static final int MEASURED = ZoneBoss.measuredAt(LEVEL);
    /** The hardest boss there is: 430 ordinary hits of a capped character; his staff's blow 33% of one's health, his specials no harder. */
    public static final double HITS = 430, STAFF = 0.33, SPIN = 0.30, DASH = 0.28, SLAM = 0.33;
    public static final double HEALTH = HITS * MobScaling.typicalHit(MEASURED);
    public static final int SPIN_WINDUP = 20, SPIN_EVERY = 120, DASH_WINDUP = 12, DASH_EVERY = 150,
            CLONE_WINDUP = 16, CLONE_EVERY = 600, CLONES = 3, CLONES_LATE = 5, SLAM_WINDUP = 26, SLAM_EVERY = 140, BREATHER = 30;
    public static final double SPIN_RANGE = 5.5, DASH_REACH = 16, DASH_STEP = 2.2, DASH_WIDTH = 1.6, SLAM_LENGTH = 14, SLAM_WIDTH = 1.6;
    /** How much larger he stands in his Dharma image. */
    public static final float GIANT = 1.6f;
    /** How far from his lair he keeps; how far he may be brought before he is back. */
    public static final int LAIR = 21, LEASH = 27;

    public static final int IDLE = 0, SPINNING = 1, CROUCHING = 2, DASHING = 3, PLUCKING = 4, RAISING = 5;

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SixEaredMacaqueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GIANT_FORM = SynchedEntityData.defineId(SixEaredMacaqueEntity.class, EntityDataSerializers.BOOLEAN);

    private int windup, spinCd = SPIN_EVERY / 2, dashCd = DASH_EVERY / 2, cloneCd, slamCd, breather;
    private Vec3 dashDir = Vec3.ZERO, slamDir = Vec3.ZERO;
    private int dashLeft;
    private final List<LivingEntity> dashHit = new ArrayList<>();
    private boolean greeted;

    public SixEaredMacaqueEntity(EntityType<? extends SixEaredMacaqueEntity> type, Level level) {
        super(type, level);
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, STAFF * MobScaling.typicalHealth(MEASURED))
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 4.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    public int mobLevel() {
        return LEVEL;
    }

    @Override
    public Zones.Zone zone() {
        return Zones.FLOWER_FRUIT;
    }

    /** He knows the true Monkey King, and fights him hardest of all. */
    @Override
    public java.util.function.Predicate<Player> allies() {
        return com.jttw.engine.world.ZoneAllies.NONE;
    }

    @Override
    public int lairRadius() {
        return LAIR;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.YELLOW;
    }

    @Override
    public double bossBarRange() {
        return 40;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            say("Which of us is the true Great Sage? Look again!");
            cloneCd = 0;
        } else {
            becomeGiant();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, IDLE);
        builder.define(GIANT_FORM, false);
    }

    /** What he is about: {@link #IDLE}, or the warning or deed of one of his specials. */
    public int action() {
        return entityData.get(ACTION);
    }

    private void act(int a) {
        entityData.set(ACTION, a);
    }

    /** Whether he stands in his Dharma image, half again his height. */
    public boolean isGiant() {
        return entityData.get(GIANT_FORM);
    }

    @Override
    public float getScale() {
        return isGiant() ? GIANT : 1f;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GIANT_FORM.equals(key)) refreshDimensions();
    }

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + LEVEL);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ── What he says ────────────────────────────────────────────────────

    /** Says {@code words} to everyone near. */
    public void say(String words) {
        if (!(level() instanceof ServerLevel server)) return;
        Component line = Component.literal("§6<").append(getName()).append(Component.literal("§6> §e" + words));
        for (ServerPlayer p : server.players()) if (p.distanceToSqr(this) < 48 * 48) p.sendSystemMessage(line);
    }

    // ── His specials ────────────────────────────────────────────────────

    private List<LivingEntity> foes(AABB box, java.util.function.Predicate<LivingEntity> also) {
        return level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !isAlliedTo(e)
                && !(e instanceof MacaqueCloneEntity) && !(e instanceof FlowerFruitMonster)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && also.test(e));
    }

    public float spinDamage() {
        return (float) (SPIN * MobScaling.typicalHealth(MEASURED));
    }

    public float dashDamage() {
        return (float) (DASH * MobScaling.typicalHealth(MEASURED));
    }

    public float slamDamage() {
        return (float) (SLAM * MobScaling.typicalHealth(MEASURED));
    }

    private double scaled(double d) {
        return d * getScale();
    }

    /** Begins the whirling staff: it sweeps round {@value #SPIN_WINDUP} ticks later. */
    public void startSpin() {
        act(SPINNING);
        windup = 0;
        playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.5f, 0.7f);
    }

    /** The whirling staff sweeps round him: all within reach struck and thrown back. Returns who. */
    public List<LivingEntity> spin() {
        double r = scaled(SPIN_RANGE);
        List<LivingEntity> caught = foes(getBoundingBox().inflate(r, 2, r), e -> e.distanceToSqr(this) <= r * r);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), spinDamage());
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 1.6, 0.4, away.z * 1.6);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.5f);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * r * 0.7, getY() + 1, getZ() + Math.sin(a) * r * 0.7, 1, 0, 0, 0, 0);
            }
        }
        return caught;
    }

    /** Crouches for the cloud somersault toward {@code target}: he goes {@value #DASH_WINDUP} ticks later. */
    public void startDash(LivingEntity target) {
        Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) flat = getLookAngle().multiply(1, 0, 1);
        dashDir = flat.normalize();
        double reach = Math.min(DASH_REACH, flat.length() + 4);
        dashLeft = (int) Math.ceil(reach / DASH_STEP);
        act(CROUCHING);
        windup = 0;
        playSound(SoundEvents.PHANTOM_FLAP, 1.5f, 0.6f);
    }

    /** One step of the somersault along its line; returns whether he goes on. */
    private boolean dashStep() {
        Vec3 next = position().add(dashDir.scale(DASH_STEP));
        // keep to the sanctum, and never through rock
        if (hasRestriction()) {
            BlockPos c = getRestrictCenter();
            if (Math.hypot(next.x - c.getX() - 0.5, next.z - c.getZ() - 0.5) > LAIR) return false;
        }
        AABB box = getBoundingBox().move(next.subtract(position()));
        if (!level().noCollision(this, box)) return false;
        Vec3 from = position();
        setPos(next.x, next.y, next.z);
        for (LivingEntity e : foes(new AABB(from, next).inflate(DASH_WIDTH, 1.5, DASH_WIDTH), e -> !dashHit.contains(e))) {
            dashHit.add(e);
            e.hurt(damageSources().mobAttack(this), dashDamage());
            e.push(dashDir.x * 0.8, 0.5, dashDir.z * 0.8);
            e.hurtMarked = true;
        }
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 12, 0.6, 0.2, 0.6, 0.02);
        return --dashLeft > 0;
    }

    /** The whole somersault at {@code target} at once, no warning (for tests and scenes): returns whom it struck. */
    public List<LivingEntity> somersaultNow(LivingEntity target) {
        startDash(target);
        dashHit.clear();
        while (dashStep()) {
            // on along the line
        }
        act(IDLE);
        return List.copyOf(dashHit);
    }

    /** Begins plucking his hairs: the clones spring up {@value #CLONE_WINDUP} ticks later. */
    public void startClones() {
        act(PLUCKING);
        windup = 0;
        playSound(SoundEvents.PLAYER_BREATH, 2f, 1.4f);
    }

    /** His hair clones, round him, at his foe. Returns them. */
    public List<MacaqueCloneEntity> clones() {
        List<MacaqueCloneEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return out;
        int n = phase() >= 2 ? CLONES_LATE : CLONES;
        for (int i = 0; i < n; i++) {
            MacaqueCloneEntity c = FlowerFruitRegistry.MACAQUE_CLONE.get().create(server);
            if (c == null) continue;
            double a = i * Math.PI * 2 / n + random.nextDouble() * 0.4;
            BlockPos at = ZoneMonster.standingSpot(server, c, BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3));
            c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), 0);
            c.become(LEVEL, false);
            if (hasRestriction()) c.restrictTo(getRestrictCenter(), LAIR);
            server.addFreshEntity(c);
            if (getTarget() != null) c.setTarget(getTarget());
            server.sendParticles(ParticleTypes.POOF, c.getX(), c.getY() + 1, c.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
            out.add(c);
        }
        playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2f, 1f);
        return out;
    }

    /** His clones about now. */
    public List<MacaqueCloneEntity> livingClones() {
        return level().getEntitiesOfClass(MacaqueCloneEntity.class, getBoundingBox().inflate(LAIR * 2));
    }

    /** The Dharma image: he grows half again his height, quicker. */
    public void becomeGiant() {
        if (isGiant()) return;
        entityData.set(GIANT_FORM, true);
        refreshDimensions();
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
        say("Heaven and earth are my image! Bow to the Great Sage Equal to Heaven!");
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 2f, 1.3f);
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
        slamCd = 20;
    }

    /** Raises his grown staff over {@code target}'s line: it comes down {@value #SLAM_WINDUP} ticks later. */
    public void startSlam(LivingEntity target) {
        Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
        slamDir = flat.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1).normalize() : flat.normalize();
        act(RAISING);
        windup = 0;
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 1.4f);
    }

    /** Whether (x, z) lies on the line his heaven-splitting blow comes down on. */
    public boolean onSlamLine(Vec3 at) {
        Vec3 to = at.subtract(position()).multiply(1, 0, 1);
        double along = to.dot(slamDir);
        if (along < 0 || along > SLAM_LENGTH) return false;
        return to.subtract(slamDir.scale(along)).length() <= SLAM_WIDTH;
    }

    /** The heaven-splitting blow: a shock along its line. Returns who it caught. */
    public List<LivingEntity> slam() {
        List<LivingEntity> caught = foes(getBoundingBox().inflate(SLAM_LENGTH + 2, 3, SLAM_LENGTH + 2), e -> onSlamLine(e.position()));
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), slamDamage());
            e.push(0, 0.7, 0);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.6f);
        if (level() instanceof ServerLevel server) {
            for (double d = 1; d <= SLAM_LENGTH; d += 1) {
                Vec3 p = position().add(slamDir.scale(d));
                server.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y + 0.3, p.z, 1, 0.2, 0, 0.2, 0);
            }
        }
        return caught;
    }

    private void markSlamLine() {
        if (!(level() instanceof ServerLevel server)) return;
        for (double d = 1; d <= SLAM_LENGTH; d += 0.7) {
            Vec3 p = position().add(slamDir.scale(d));
            server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.78f, 0.15f), 1.6f), p.x, p.y + 0.15, p.z, 2, SLAM_WIDTH * 0.5, 0, SLAM_WIDTH * 0.5, 0);
        }
    }

    // ── His fight ───────────────────────────────────────────────────────

    /** He keeps to his sanctum: brought far out of it, he is back at once. */
    private void keepToLair() {
        if (!hasRestriction()) return;
        BlockPos c = getRestrictCenter();
        if (Math.hypot(getX() - c.getX() - 0.5, getZ() - c.getZ() - 0.5) > LEASH || Math.abs(getY() - c.getY()) > 12) {
            teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            getNavigation().stop();
            act(IDLE);
            dashLeft = 0;
        }
    }

    /** Six ears: he hears where anyone is near him, seen or not. */
    private void hear() {
        if (getTarget() != null && getTarget().isAlive()) return;
        Player best = null;
        double bestD = 32 * 32;
        for (Player p : level().players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator() || isAlliedTo(p)) continue;
            if (hasRestriction() && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(getRestrictCenter())) > (LEASH + 6) * (LEASH + 6)) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best != null) setTarget(best);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 == 0) {
            keepToLair();
            hear();
        }
        LivingEntity t = getTarget();
        if (t != null && !greeted) {
            greeted = true;
            say(FlowerFruit.isKing(t) ? "You? You are the impostor, monkey. There is one Great Sage, and he sits on this throne."
                    : "Mortals in the Water Curtain Cave! I am Sun Wukong, the Great Sage Equal to Heaven!");
        }
        if (spinCd > 0) spinCd--;
        if (dashCd > 0) dashCd--;
        if (cloneCd > 0) cloneCd--;
        if (slamCd > 0) slamCd--;
        if (breather > 0) breather--;
        int a = action();
        if (a == DASHING) {
            if (!dashStep()) {
                act(IDLE);
                breather = BREATHER;
            }
            return;
        }
        if (a != IDLE) {
            getNavigation().stop();
            if (t != null && a != CROUCHING && a != RAISING) getLookControl().setLookAt(t, 30f, 30f);
            windup++;
            switch (a) {
                case SPINNING -> {
                    if (windup >= SPIN_WINDUP) {
                        spin();
                        done(SPINNING);
                    }
                }
                case CROUCHING -> {
                    if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 6, 0.8, 0.1, 0.8, 0.01);
                    if (windup >= DASH_WINDUP) {
                        dashHit.clear();
                        act(DASHING);
                        playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2f, 0.7f);
                        dashCd = DASH_EVERY;
                    }
                }
                case PLUCKING -> {
                    if (windup >= CLONE_WINDUP) {
                        clones();
                        done(PLUCKING);
                    }
                }
                case RAISING -> {
                    if (windup % 3 == 0) markSlamLine();
                    if (windup >= SLAM_WINDUP) {
                        slam();
                        done(RAISING);
                    }
                }
                default -> act(IDLE);
            }
            return;
        }
        if (t == null || !t.isAlive() || breather > 0) return;
        double d = distanceTo(t);
        if (phase() >= 1 && cloneCd == 0) startClones();
        else if (phase() >= 2 && slamCd == 0 && d < SLAM_LENGTH && hasLineOfSight(t)) startSlam(t);
        else if (spinCd == 0 && (d < 3.5 || foes(getBoundingBox().inflate(SPIN_RANGE), e -> e.distanceToSqr(this) <= SPIN_RANGE * SPIN_RANGE).size() >= 2)) startSpin();
        else if (dashCd == 0 && d > 6 && d < DASH_REACH + 2 && hasLineOfSight(t)) startDash(t);
    }

    private void done(int what) {
        act(IDLE);
        breather = BREATHER;
        switch (what) {
            case SPINNING -> spinCd = SPIN_EVERY;
            case PLUCKING -> cloneCd = CLONE_EVERY;
            case RAISING -> slamCd = SLAM_EVERY;
            default -> {}
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (dead) {
            say("The Buddha... named me... the six ears hear their end...");
            livingClones().forEach(MacaqueCloneEntity::vanish);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AGGRO;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Giant", isGiant());
        tag.putBoolean("Greeted", greeted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(GIANT_FORM, tag.getBoolean("Giant"));
        greeted = tag.getBoolean("Greeted");
    }
}
