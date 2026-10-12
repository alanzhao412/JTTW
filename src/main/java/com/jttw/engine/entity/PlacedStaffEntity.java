package com.jttw.engine.entity;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.RuyiJinguBangItem;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Ruyi Jingu Bang placed in the world as a scalable pillar. Wukong can scroll to resize it
 * (1–100×), then punch it to topple it. It falls as a heavy rod truly falls, pivoting on its foot
 * under gravity, and where it lands the ground is blasted; it comes to rest down in the crater it
 * made, rolls off whatever (or whichever other staff) it lands across, and stays where it lies
 * until he punches it again, which calls it back to his hand.
 */
public class PlacedStaffEntity extends Entity {

    private static final EntityDataAccessor<Float> STAFF_SCALE =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FALLING =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> TOPPLE_YAW =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.FLOAT);
    /** Fallen: its two ends (base, tip) and how far it has rolled about its length, for drawing. */
    private static final EntityDataAccessor<org.joml.Vector3f> ROD_BASE =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<org.joml.Vector3f> ROD_TIP =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> ROD_SPIN =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.FLOAT);
    /** Standing: the largest size it can grow to now, through what stands above it (see {@link #reachable}). */
    private static final EntityDataAccessor<Float> GROW_REACH =
            SynchedEntityData.defineId(PlacedStaffEntity.class, EntityDataSerializers.FLOAT);

    private int returnTimer = -1; // ticks until return, -1 = not returning
    private int fallAnimTicks = 0; // client-side animation counter

    // Scaling limits
    public static final float MIN_SCALE = 1.0f;
    public static final float MAX_SCALE = 100.0f;
    private static final float SCROLL_STEP = 1.0f;

    // Damage/explosion scaling — the staff should obliterate everything in its path
    private static final float BASE_EXPLOSION_POWER = 6.0f;
    private static final float EXPLOSION_SCALE_FACTOR = 0.5f; // at 100x → 6 + 50 = 56
    /** Strike damage is BASE_DAMAGE + size × DAMAGE_SCALE_FACTOR at level 1, times the owner's level power. */
    private static final float BASE_DAMAGE = 20.0f;
    private static final float DAMAGE_SCALE_FACTOR = 3.0f;     // at 100x → 20 + 300 = 320 at level 1

    public PlacedStaffEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setNoGravity(true);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public PlacedStaffEntity(Level level, double x, double y, double z, UUID owner) {
        this(ModEntities.PLACED_STAFF.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(OWNER_UUID, Optional.of(owner));
    }

    /**
     * A clone's staff: an echo of Wukong's, planted where the clone stands as he plants his. It
     * grows as his grows and topples as his topples, the same way, striking by his power (the clones
     * are his hairs); it heeds no wheel or blow of its own, and when his returns to his hand, it is
     * gone. Null for his own staff.
     */
    @Nullable private UUID master;
    /** Who struck this staff down, for its echoes to fall by. */
    @Nullable private ServerPlayer toppledBy;

    /** Plants a clone's echo of {@code master} (Wukong's own staff) at {@code x, y, z}. */
    public static PlacedStaffEntity echoOf(PlacedStaffEntity master, double x, double y, double z) {
        PlacedStaffEntity echo = new PlacedStaffEntity(master.level(), x, y, z, master.getOwnerUUID());
        echo.master = master.getUUID();
        echo.setStaffScale(master.getStaffScale());
        return echo;
    }

    public boolean isEcho() {
        return master != null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STAFF_SCALE, 1.0f);
        this.entityData.define(FALLING, false);
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(TOPPLE_YAW, 0.0f);
        this.entityData.define(ROD_BASE, new org.joml.Vector3f());
        this.entityData.define(ROD_TIP, new org.joml.Vector3f());
        this.entityData.define(ROD_SPIN, 0f);
        this.entityData.define(GROW_REACH, MAX_SCALE);
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public float getStaffScale() {
        return this.entityData.get(STAFF_SCALE);
    }

    public void setStaffScale(float scale) {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        this.entityData.set(STAFF_SCALE, scale);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAFF_SCALE.equals(key)) {
            refreshDimensions();
        }
        if (ROD_BASE.equals(key) || ROD_TIP.equals(key) || FALLING.equals(key)) setBoundingBox(makeBoundingBox());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float scale = getStaffScale();
        // Width stays close to the visual staff (thin pillar), height scales with size
        // Capped so Minecraft's entity tracking doesn't break
        float width = Math.min(0.5f + scale * 0.1f, 5.0f);
        float height = Math.min(VISUAL_HEIGHT * scale, 20.0f);
        return EntityDimensions.scalable(width, height);
    }

    /**
     * Expand the culling AABB so the renderer isn't clipped when the camera
     * looks away from the entity center.  The visual can extend far above
     * (and sideways when toppling) at large scales.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        float scale = getStaffScale();
        float reach = VISUAL_HEIGHT * scale;
        return super.getBoundingBoxForCulling().inflate(reach, reach, reach);
    }

    public boolean isFalling() {
        return this.entityData.get(FALLING);
    }

    // ── Being aimed at ───────────────────────────────────────────────

    /** How far its owner can reach it from: as far as it can be planted from. */
    public static final double REACH = 5.0;
    /** Slack on the server's distance check, for the owner's movement in flight to it. */
    private static final double REACH_SLACK = 1.5;

    /**
     * The staff as it is drawn, standing: a thin pillar a little wider than the visual, from
     * the base to the tip. The entity's own box only handles collision: it is capped, and the
     * game finds entities near their base, so it cannot be aimed at along a tall staff.
     */
    public AABB standingBox() {
        float scale = getStaffScale();
        double half = Math.max(0.3, 0.06 * scale + 0.15); // the drawn staff is 0.11 x scale wide
        double height = VISUAL_HEIGHT * scale;
        return new AABB(getX() - half, getY(), getZ() - half, getX() + half, getY() + height, getZ() + half);
    }

    /** Whether it can be aimed at now: standing, not falling or on its way back. */
    public boolean isStanding() {
        return !isFalling() && returnTimer < 0;
    }

    /** Distance along the look to where it is hit, or -1 if the look misses it within reach. */
    public double aimDistance(net.minecraft.world.phys.Vec3 eye, net.minecraft.world.phys.Vec3 look) {
        net.minecraft.world.phys.Vec3 end = eye.add(look.normalize().scale(REACH));
        if (isRod()) {
            // fallen: where the look passes within its thickness of the rod
            double[] st = closest(eye, end, rodBase(), rodTip());
            net.minecraft.world.phys.Vec3 p = eye.lerp(end, st[0]), q = rodBase().lerp(rodTip(), st[1]);
            return p.distanceTo(q) <= radius() + 0.3 ? eye.distanceTo(p) : -1;
        }
        if (!isStanding()) return -1;
        return standingBox().clip(eye, end).map(eye::distanceTo).orElse(-1.0);
    }

    /** Server-side check that a player is close enough to have reached it. */
    public boolean withinReachOf(Player player) {
        double r = REACH + REACH_SLACK;
        if (isRod()) {
            net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
            double[] st = closest(eye, eye, rodBase(), rodTip());
            return eye.distanceTo(rodBase().lerp(rodTip(), st[1])) <= r + radius();
        }
        return standingBox().distanceToSqr(player.getEyePosition()) <= r * r;
    }

    /**
     * The owner strikes it: standing, it topples the way they face; fallen, it comes back to their
     * hand (and its echoes are gone with it). Returns whether anything happened.
     */
    public boolean strikeBy(ServerPlayer player) {
        if (isRod()) {
            if (!isOwner(player) || isEcho()) return false;
            returnTo(player);
            return true;
        }
        return toppleBy(player);
    }

    /** The owner strikes it: it topples the way they face. Returns false if it could not. */
    public boolean toppleBy(ServerPlayer player) {
        if (!isOwner(player) || !isStanding() || isEcho()) return false;
        this.entityData.set(FALLING, true);
        this.entityData.set(TOPPLE_YAW, player.getYRot());
        toppledBy = player;
        topple(player, player.getYRot());
        return true;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public int getFallAnimTicks() {
        return fallAnimTicks;
    }

    public float getToppleYaw() {
        return this.entityData.get(TOPPLE_YAW);
    }

    public boolean isOwner(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    // ── How large it is drawn (client) ───────────────────────────────

    /**
     * Each tick the drawn size closes this share of the way to its true size, so a change in size
     * is seen to grow (or shrink) over a few ticks rather than jump: a few ticks for a notch of the
     * wheel, a little longer for many notches at once.
     */
    public static final float GROWTH_EASE = 0.3f;

    private float shownScale = -1, shownScaleO = -1;

    private void easeShownScale() {
        float target = getStaffScale();
        if (shownScale < 0) { // first seen: as it is, not grown from nothing
            shownScale = shownScaleO = target;
            return;
        }
        shownScaleO = shownScale;
        float gap = target - shownScale;
        shownScale = Math.abs(gap) < 0.01f ? target : shownScale + gap * GROWTH_EASE;
    }

    /** A fallen staff's ends as drawn: now and a tick ago, to blend between. */
    private net.minecraft.world.phys.Vec3[] shownRod, shownRodO;

    /** A fallen staff's foot, or (with {@code end} 1) tip, as drawn between ticks. */
    public net.minecraft.world.phys.Vec3 shownRodEnd(int end, float partialTick) {
        if (shownRod == null || shownRodO == null) return end == 0 ? rodBase() : rodTip();
        return shownRodO[end].lerp(shownRod[end], partialTick);
    }

    /** Seen from as far as its size warrants: a hundredfold staff is a landmark. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double far = 128 + length() * 4;
        return distanceSqr < far * far;
    }

    /** The size it is drawn at, between ticks: easing toward {@link #getStaffScale()}. */
    public float shownScale(float partialTick) {
        if (shownScale < 0) return getStaffScale();
        return shownScaleO + (shownScale - shownScaleO) * partialTick;
    }

    // ── Scroll to scale ──────────────────────────────────────────────

    public void adjustScale(float delta) {
        adjustScale(delta, null);
    }

    /** As {@link #adjustScale(float)}, grown by {@code by}: whose strength pays for what it drills through. */
    public void adjustScale(float delta, @Nullable Player by) {
        if (by != null) grower = by;
        if (isFalling() || returnTimer >= 0 || isEcho()) return;
        float oldScale = getStaffScale();
        float newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, oldScale + delta * SCROLL_STEP));
        if (newScale == oldScale) return;
        // Growing, it drills up through what it can pay for; it stops beneath what it cannot
        if (!level().isClientSide() && newScale > oldScale) newScale = drillTo(oldScale, newScale);
        if (newScale > oldScale || newScale < oldScale) setStaffScale(newScale);
        if (!level().isClientSide()) updateReach();
    }

    /** What its growing has paid so far (see Carving): its budget at any size is spent across all its growing. */
    private double drillPaid;
    /** Blocks its growing has paid for, gone to it though the breaker takes them over the next ticks. */
    private final java.util.Set<Long> drilled = new java.util.HashSet<>();

    /** The largest size it can grow to now (synced; on the server, as last worked out). */
    public float reachScale() {
        return entityData.get(GROW_REACH);
    }

    /** How tall it can grow now, in blocks. */
    public float reachHeight() {
        return VISUAL_HEIGHT * reachScale();
    }

    /** Whose strength pays for its growing: who last grew it, else its owner if online. */
    @Nullable
    private Player payer() {
        if (grower != null || getOwnerUUID() == null || level().getServer() == null) return grower;
        return level().getServer().getPlayerList().getPlayer(getOwnerUUID());
    }

    /**
     * The largest size, up to {@code upTo}, it can grow to from where it stands: layer by layer up
     * its column, each paid for (on top of what its growing has already paid) from its budget at
     * the size that passes through that layer. It stops beneath the first layer it cannot pay for:
     * an obsidian ceiling for the weak, bedrock for all. What it would break goes into
     * {@code breaks}, and what that would cost into {@code cost[0]}, when given.
     */
    private float reachable(float upTo, @Nullable Player payer, @Nullable java.util.List<BlockPos> breaks, @Nullable double[] cost) {
        Level lv = level();
        double strength = com.jttw.engine.world.tough.Tough.strength(payer), paid = drillPaid;
        int baseY = net.minecraft.util.Mth.floor(getY()), topY = net.minecraft.util.Mth.floor(getY() + VISUAL_HEIGHT * upTo);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int by = baseY; by <= topY; by++) {
            // each layer is priced the one way, however it is grown into: at the size whose tip
            // passes through it, with that size's width (a little wider than drawn) and budget
            float size = Math.max(MIN_SCALE, (float) ((by + 1 - getY()) / VISUAL_HEIGHT));
            float radius = 0.25f + size * 0.05f;
            int x0 = net.minecraft.util.Mth.floor(getX() - radius), x1 = net.minecraft.util.Mth.floor(getX() + radius);
            int z0 = net.minecraft.util.Mth.floor(getZ() - radius), z1 = net.minecraft.util.Mth.floor(getZ() + radius);
            double layer = 0;
            int marked = breaks == null ? 0 : breaks.size();
            for (int bx = x0; bx <= x1; bx++) for (int bz = z0; bz <= z1; bz++) {
                at.set(bx, by, bz);
                BlockState state = lv.getBlockState(at);
                if (state.isAir() || drilled.contains(at.asLong())) continue;
                layer += com.jttw.engine.world.tough.Carving.cost(lv, at, state, strength);
                if (breaks != null) breaks.add(at.immutable());
            }
            // a layer already paid for (or empty) costs nothing; a new one must fit, with all paid
            // below it, in the budget of the size that passes through it
            if (layer > 0 && paid + layer > carveBudget(size, payer)) {
                if (breaks != null) breaks.subList(marked, breaks.size()).clear();
                upTo = Math.max(MIN_SCALE, (float) ((by - getY()) / VISUAL_HEIGHT) - 0.01f);
                break;
            }
            paid += layer;
        }
        if (cost != null) cost[0] = paid - drillPaid;
        return upTo;
    }

    /** As tall as it can grow from {@code from} toward {@code to}, drilling what it can pay for on the way. */
    private float drillTo(float from, float to) {
        ServerLevel sl = (ServerLevel) level();
        java.util.List<BlockPos> column = new java.util.ArrayList<>();
        double[] cost = new double[1];
        float reached = Math.max(from, reachable(to, payer(), column, cost));
        drillPaid += cost[0];
        for (BlockPos pos : column) drilled.add(pos.asLong());
        if (!column.isEmpty()) {
            com.jttw.engine.world.TerrainBreaker.breakBlocks(sl, column); // no drops, spread over ticks
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + VISUAL_HEIGHT * reached, getZ(),
                    (int) (reached * 0.5f + 2), 0.4, 0.4, 0.4, 0.02);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                    Math.min(2.0f, 0.5f + reached * 0.02f), 1.2f);
        }
        if (reached < to) {
            level().playSound(null, getX(), getY() + VISUAL_HEIGHT * reached, getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8f, 0.6f);
        }
        return reached;
    }

    /** Works out again how tall it can grow now, for its owner's meter. */
    private void updateReach() {
        entityData.set(GROW_REACH, Math.max(getStaffScale(), reachable(MAX_SCALE, payer(), null, null)));
    }

    // ── Hit to topple ────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) return false;
        if (returnTimer >= 0) return false;

        // only its owner's own hand: not a blast (its own landing's, which is his), not an arrow
        return source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && source.getDirectEntity() instanceof ServerPlayer player && strikeBy(player);
    }

    /** Blasts (its own landing's among them) do not move or harm it: only he does. */
    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    // Staff length in blocks at scale 1 (model is 47 units / 16 ≈ 2.9375 blocks).
    // FIXED display context applies an additional 0.5× scale, so the visual height
    // in-world is STAFF_LENGTH * 0.5 per unit of entity scale.
    private static final float STAFF_LENGTH = 2.9375f;
    private static final float VISUAL_HEIGHT = STAFF_LENGTH * 0.5f; // ≈ 1.47 blocks at scale 1

    /**
     * Struck down: it begins to fall. It carves its own way as it goes, through whatever it can pay
     * for from its budget ({@link #carveBudget}); what it cannot holds it up. The blast comes where
     * and when it lands ({@link #land}).
     */
    private void topple(ServerPlayer owner, float yaw) {
        budget = carveBudget(getStaffScale(), owner);
        startFall(yaw);
    }

    /**
     * It lands: blasts along where it lies, from a few blocks out from its foot to its tip, harder
     * toward the tip; strikes everything along it, by its owner's power; sets the ground alight.
     */
    private void land(ServerPlayer owner) {
        float scale = getStaffScale();
        ServerLevel serverLevel = (ServerLevel) level();
        // Total explosion power scales with size — no cap, the staff is a divine weapon
        float totalPower = BASE_EXPLOSION_POWER + scale * EXPLOSION_SCALE_FACTOR;
        net.minecraft.world.phys.Vec3 foot = rodA, tip = rodB;
        float length = (float) foot.distanceTo(tip);
        float skipDist = 3.0f;

        int explosionCount = Math.max(1, (int)(length / 3.0f));
        for (int i = 0; i <= explosionCount; i++) {
            float t = (float) i / explosionCount; // 0 = base, 1 = tip
            if (length * t < skipDist) continue;
            net.minecraft.world.phys.Vec3 at = foot.lerp(tip, t);
            // Power ramps up toward the tip: base gets 30%, tip gets full
            float pointPower = totalPower * (0.3f + t * 0.7f);
            com.jttw.engine.world.TerrainBreaker.explode(serverLevel, owner, at.x, at.y, at.z, pointPower);
        }

        float damageRadius = 2.0f + scale * 0.3f;
        float damage = LevelPower.scaled(owner, BASE_DAMAGE + scale * DAMAGE_SCALE_FACTOR);
        AABB damageBox = new AABB(foot, tip).inflate(damageRadius, damageRadius, damageRadius);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageBox, e -> e != owner && e.isAlive())) {
            float distToLine = distanceToSegment(target.getX(), target.getZ(), foot.x, foot.z, tip.x, tip.z);
            float falloff = Math.max(0.2f, 1.0f - (distToLine / damageRadius));
            target.hurt(level().damageSources().playerAttack(owner), damage * falloff);
            target.setSecondsOnFire((int)(2 + scale * 0.1f));
        }

        java.util.List<BlockPos> firePoints = new java.util.ArrayList<>();
        for (int i = 0; i <= explosionCount; i++) {
            float t = (float) i / explosionCount;
            if (length * t < skipDist) continue;
            net.minecraft.world.phys.Vec3 at = foot.lerp(tip, t);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.5, 0.3, 0.5, 0.0);
            firePoints.add(BlockPos.containing(at));
        }
        com.jttw.engine.world.TerrainBreaker.afterQueued(serverLevel, () -> {
            for (BlockPos firePos : firePoints) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = firePos.above(dy);
                    if (serverLevel.isLoaded(pos) && serverLevel.getBlockState(pos).isAir()
                            && serverLevel.getBlockState(pos.below()).isSolidRender(serverLevel, pos.below())) {
                        serverLevel.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
        });
        level().playSound(null, tip.x, tip.y, tip.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                Math.min(4.0f, 1.0f + scale * 0.05f), 0.8f);
    }

    // ── Fallen: a heavy rod, its two ends held its length apart ──────
    //
    // Two weighted ends, a constraint holding them its length apart, gravity, and contact: every
    // point along it is a ball of its thickness that may not sit inside a solid block or another
    // staff. That is all; resting on the ground, in a crater, against a wall, across another
    // staff, at whatever angle it comes to, all follow from it. Where it presses into a block it
    // can pay for (see Carving), the block breaks and it goes on, a little slowed; what it cannot
    // pay for holds it.

    /** Gravity, blocks per tick per tick: as anything falls. */
    private static final double GRAVITY = 0.08;
    /**
     * Friction where it touches, as between iron and stone: a contact holds outright unless what
     * drives it along the surface is more than this share of what presses it in (so a staff leant
     * steeply on a wall stays leant), and slides held back by that much if it is.
     */
    private static final double FRICTION = 0.6;
    /** Passes over its constraints each step: more is stiffer and steadier. */
    private static final int PASSES = 3;
    /** Ticks it must lie still before it rests (and stops being simulated until disturbed). */
    private static final int SETTLE = 30;
    /**
     * How fast (blocks a tick) a point of it must be driving into a block to break it: a staff
     * crashing down smashes what it strikes; one standing or lying on the ground does not dig in.
     */
    public static final double BREAK_SPEED = 0.3;
    /** The length of the step being taken, as a share of a tick. */
    private double stepH = 1;
    /** The most steps a tick is cut into, so a fast end never skips through a block. */
    private static final int MAX_STEPS = 12;
    /** Its budget for carving (see Carving), per unit of size and per unit of its owner's level power. */
    public static final double BUDGET_BASE = 10, BUDGET_PER_SIZE = 2;

    private net.minecraft.world.phys.Vec3 rodA = net.minecraft.world.phys.Vec3.ZERO, rodB = net.minecraft.world.phys.Vec3.ZERO,
            velA = net.minecraft.world.phys.Vec3.ZERO, velB = net.minecraft.world.phys.Vec3.ZERO;
    private boolean landed, resting, touchA, touchB;
    private int stillTicks;
    private float spin;
    private UUID landingOwner;
    /** Where its ends were as this step began: how far a touching point has slid along what it touches. */
    private net.minecraft.world.phys.Vec3 stepA = net.minecraft.world.phys.Vec3.ZERO, stepB = net.minecraft.world.phys.Vec3.ZERO;
    /** Who last grew it (an echo: who grew its master), whose strength its drilling is paid by. */
    @Nullable private Player grower;
    /** What it can still pay to carve, falling (see Carving). */
    private double budget;
    /** Its owner's strength against the mod's tough stone, as it was struck. */
    private double strength = 1;
    /** Blocks it has paid to break, gone to it at once though the breaker takes them over the next ticks. */
    private final java.util.Set<Long> broken = new java.util.HashSet<>();
    private final java.util.List<BlockPos> toBreak = new java.util.ArrayList<>();

    /** How much a staff of this size, in this owner's hands, can carve: (10 + 2 x size) x their level power. */
    public static double carveBudget(float scale, @Nullable Player owner) {
        double power = owner == null ? 1 : LevelPower.of(owner);
        return (BUDGET_BASE + BUDGET_PER_SIZE * scale) * power;
    }

    /** How thick it is: half the drawn width. */
    public float radius() {
        return Math.max(0.06f, 0.055f * getStaffScale());
    }

    /** How long it is, foot to tip. */
    public float length() {
        return VISUAL_HEIGHT * getStaffScale();
    }

    /** Whether it lies (or falls) as a rod in the world, rather than standing. */
    public boolean isRod() {
        return isFalling();
    }

    /** Its foot and tip as last known (on the client: as last told), for drawing and aiming. */
    public net.minecraft.world.phys.Vec3 rodBase() {
        return level().isClientSide ? new net.minecraft.world.phys.Vec3(entityData.get(ROD_BASE)) : rodA;
    }

    public net.minecraft.world.phys.Vec3 rodTip() {
        return level().isClientSide ? new net.minecraft.world.phys.Vec3(entityData.get(ROD_TIP)) : rodB;
    }

    public float rodSpin() {
        return entityData.get(ROD_SPIN);
    }

    /** What it has left to carve with. */
    public double budgetLeft() {
        return budget;
    }

    /** Upright on its foot, tipped a few degrees the way it is to fall, with a nudge that way. */
    private void startFall(float yaw) {
        float yawRad = (float) Math.toRadians(yaw);
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(-net.minecraft.util.Mth.sin(yawRad), 0, net.minecraft.util.Mth.cos(yawRad));
        double L = length(), tilt = Math.toRadians(4);
        rodA = position();
        rodB = rodA.add(dir.scale(Math.sin(tilt) * L)).add(0, Math.cos(tilt) * L, 0);
        velA = net.minecraft.world.phys.Vec3.ZERO;
        velB = dir.scale(0.02 * Math.max(1, L / 10));
        landed = false;
        resting = false;
        stillTicks = 0;
        landingOwner = getOwnerUUID();
        strength = com.jttw.engine.world.tough.Tough.strength(toppledBy);
        syncRod();
    }

    /** One tick of the fallen rod: as many small steps as its speed needs. */
    private void rodTick(ServerLevel level) {
        if (resting) {
            // at rest it is still watched: if what bears it is blasted away, or another staff lands on it, it moves again
            if (tickCount % 10 == 0 && !supported(level)) wake();
            if (resting) return;
        }
        double fastest = Math.max(velA.length(), velB.length());
        int steps = Math.max(1, Math.min(MAX_STEPS, (int) Math.ceil(fastest / (Math.max(0.25, radius()) * 0.8))));
        double h = 1.0 / steps;
        stepH = h;
        net.minecraft.world.phys.Vec3 before = rodA.add(rodB).scale(0.5);
        touchA = touchB = false;
        for (int step = 0; step < steps; step++) {
            net.minecraft.world.phys.Vec3 a0 = rodA, b0 = rodB;
            stepA = a0;
            stepB = b0;
            velA = velA.add(0, -GRAVITY * h, 0);
            velB = velB.add(0, -GRAVITY * h, 0);
            net.minecraft.world.phys.Vec3 freeA = velA, freeB = velB;
            rodA = rodA.add(velA.scale(h));
            rodB = rodB.add(velB.scale(h));
            for (int pass = 0; pass < PASSES; pass++) {
                keepLength(length());
                collideWithBlocks(level);
                pushOffOtherStaffs(level);
            }
            // what moved is what it now moves at; but what holds it (the ground, a wall, another
            // staff, its own length) can only turn or stop its motion, never quicken it: being
            // pushed out of what it touches does not fling it off
            velA = noFaster(rodA.subtract(a0).scale(1 / h).scale(0.998), freeA);
            velB = noFaster(rodB.subtract(b0).scale(1 / h).scale(0.998), freeB);
        }
        if (!toBreak.isEmpty()) {
            com.jttw.engine.world.TerrainBreaker.breakBlocks(level, new java.util.ArrayList<>(toBreak));
            toBreak.clear();
        }
        // rolling: sideways travel of its middle, across its length, turns it about its length
        net.minecraft.world.phys.Vec3 axis = rodB.subtract(rodA);
        net.minecraft.world.phys.Vec3 across = new net.minecraft.world.phys.Vec3(-axis.z, 0, axis.x);
        if (across.lengthSqr() > 1e-6) {
            net.minecraft.world.phys.Vec3 moved = rodA.add(rodB).scale(0.5).subtract(before);
            spin += (float) (moved.dot(across.normalize()) / radius());
        }
        double motion = Math.max(velA.lengthSqr(), velB.lengthSqr());
        stillTicks = motion < 0.004 * 0.004 ? stillTicks + 1 : 0;
        // it strikes where its upper half first meets something (ground, wall, another staff), or,
        // held up before that, where it comes to a stop
        if (!landed && (touchB || stillTicks > 5)) {
            landed = true;
            stillTicks = 0;
            ServerPlayer owner = landingOwner == null ? null : level.getServer().getPlayerList().getPlayer(landingOwner);
            if (owner == null && toppledBy != null) owner = toppledBy;
            if (owner != null) land(owner);
        }
        if (landed && stillTicks > SETTLE) {
            resting = true;
            velA = velB = net.minecraft.world.phys.Vec3.ZERO;
        }
        if (rodA.y < level.getMinBuildHeight() - 64) { // fell out of the world
            discard();
            return;
        }
        setPos(rodA.x, rodA.y, rodA.z);
        syncRod();
    }

    private void wake() {
        resting = false;
        stillTicks = 0;
    }

    /**
     * The point a share {@code t} along it is pushed out of what it touches, along {@code n}, by
     * {@code depth}; and friction holds back its sliding along the surface this step: all of it if
     * it slid less than {@link #FRICTION} times the depth (it sticks), else that much of it.
     */
    private void contact(double t, net.minecraft.world.phys.Vec3 n, double depth) {
        moveAt(t, n.scale(depth));
        net.minecraft.world.phys.Vec3 slid = rodA.lerp(rodB, t).subtract(stepA.lerp(stepB, t));
        net.minecraft.world.phys.Vec3 along = slid.subtract(n.scale(slid.dot(n)));
        double far = along.length(), hold = FRICTION * depth;
        if (far < 1e-9) return;
        moveAt(t, along.scale(-(far <= hold ? 1 : hold / far)));
    }

    /**
     * Where its two weights sit, as shares of its length from each end: half its mass at each, at
     * 1/2 - 1/(2 sqrt 3) (about 0.211) in from either end, is a solid rod of even weight in how it
     * turns as well as how it falls (the same moment about its middle, mL^2/12). Its ends follow
     * them, as all of it does: a rigid line through the two.
     */
    private static final double WEIGHT_AT = 0.5 - 0.5 / Math.sqrt(3), BEYOND = WEIGHT_AT / (1 - 2 * WEIGHT_AT);

    /** {@code v}, no faster than {@code free} (what gravity and its own momentum alone would have it moving at). */
    private static net.minecraft.world.phys.Vec3 noFaster(net.minecraft.world.phys.Vec3 v, net.minecraft.world.phys.Vec3 free) {
        double most = free.length(), now = v.length();
        return now > most && now > 1e-9 ? v.scale(most / now) : v;
    }

    /** Moves the rod so the point a share {@code t} of the way from foot to tip moves by {@code by}. */
    private void moveAt(double t, net.minecraft.world.phys.Vec3 by) {
        // the point's place between the two weights (below 0 or past 1 out toward the ends)
        double u = (t - WEIGHT_AT) / (1 - 2 * WEIGHT_AT), wa = 1 - u, wb = u, k = 1 / (wa * wa + wb * wb);
        net.minecraft.world.phys.Vec3 pa = by.scale(wa * k), pb = by.scale(wb * k);
        // and the ends, which lie on the line through the weights, beyond them
        rodA = rodA.add(pa.scale(1 + BEYOND)).subtract(pb.scale(BEYOND));
        rodB = rodB.add(pb.scale(1 + BEYOND)).subtract(pa.scale(BEYOND));
    }

    private void keepLength(double L) {
        net.minecraft.world.phys.Vec3 d = rodB.subtract(rodA);
        double len = d.length();
        if (len < 1e-6) return;
        net.minecraft.world.phys.Vec3 fix = d.scale((len - L) / len * 0.5);
        rodA = rodA.add(fix);
        rodB = rodB.subtract(fix);
    }

    /** Points along it, each a ball of its thickness: spaced about its thickness apart, 4 to 64. */
    private int samples() {
        return Math.max(4, Math.min(64, (int) Math.ceil(length() / Math.max(0.3, radius()))));
    }

    /** Whether the block here stands in its way: solid, and not already broken by it. */
    private boolean solid(ServerLevel level, BlockPos pos) {
        if (broken.contains(pos.asLong()) || !level.isLoaded(pos)) return false;
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * It meets a block: if it can pay to break it, it does (and loses a little of its speed doing
     * so); if not, the block holds. Returns whether it broke.
     */
    private boolean breakInto(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        double cost = com.jttw.engine.world.tough.Carving.cost(level, pos, state, strength);
        if (cost > budget) return false;
        budget -= cost;
        broken.add(pos.asLong());
        toBreak.add(pos.immutable());
        double slow = 1 / (1 + 0.02 * cost);
        velA = velA.scale(slow);
        velB = velB.scale(slow);
        return true;
    }

    /**
     * Each point along it is a ball of its thickness: where one sits in a solid block it either
     * breaks through (if it can pay for the block) or is pushed out of it, by the shortest way.
     * Floors, walls, ceilings and craters are all only blocks.
     */
    private void collideWithBlocks(ServerLevel level) {
        double r = radius();
        int n = samples();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        // the balls are kept inside the drawn cylinder: the end ones a radius in from its flat ends,
        // so stood on its foot it meets the ground there, not half sunk in it
        double inset = Math.min(r, length() / 2) / length();
        for (int i = 0; i <= n; i++) {
            double t = inset + (1 - 2 * inset) * i / n;
            net.minecraft.world.phys.Vec3 p = rodA.lerp(rodB, t);
            int x0 = net.minecraft.util.Mth.floor(p.x - r), x1 = net.minecraft.util.Mth.floor(p.x + r);
            int y0 = net.minecraft.util.Mth.floor(p.y - r), y1 = net.minecraft.util.Mth.floor(p.y + r);
            int z0 = net.minecraft.util.Mth.floor(p.z - r), z1 = net.minecraft.util.Mth.floor(p.z + r);
            // the deepest overlap with a block that will not give: pushed out of that one first
            for (int bx = x0; bx <= x1; bx++) for (int by = y0; by <= y1; by++) for (int bz = z0; bz <= z1; bz++) {
                at.set(bx, by, bz);
                if (!solid(level, at)) continue;
                // nearest point of the block to the ball's centre
                double cx = Math.max(bx, Math.min(bx + 1, p.x)), cy = Math.max(by, Math.min(by + 1, p.y)), cz = Math.max(bz, Math.min(bz + 1, p.z));
                net.minecraft.world.phys.Vec3 d = p.subtract(cx, cy, cz);
                double dist = d.length();
                if (dist >= r && !(dist == 0)) continue; // not touching
                net.minecraft.world.phys.Vec3 out;
                double pen;
                if (dist > 1e-6) {
                    out = d.scale(1 / dist);
                    pen = r - dist;
                } else { // its centre inside the block: out by the nearest face
                    double[] faces = {p.x - bx, bx + 1 - p.x, p.y - by, by + 1 - p.y, p.z - bz, bz + 1 - p.z};
                    int best = 0;
                    for (int f = 1; f < 6; f++) if (faces[f] < faces[best]) best = f;
                    out = switch (best) {
                        case 0 -> new net.minecraft.world.phys.Vec3(-1, 0, 0);
                        case 1 -> new net.minecraft.world.phys.Vec3(1, 0, 0);
                        case 2 -> new net.minecraft.world.phys.Vec3(0, -1, 0);
                        case 3 -> new net.minecraft.world.phys.Vec3(0, 1, 0);
                        case 4 -> new net.minecraft.world.phys.Vec3(0, 0, -1);
                        default -> new net.minecraft.world.phys.Vec3(0, 0, 1);
                    };
                    pen = faces[best] + r;
                }
                // it breaks the block only if it strikes it hard enough: how fast this point is
                // driving into it; resting on it, its weight alone breaks nothing
                double into = -p.subtract(stepA.lerp(stepB, t)).dot(out) / stepH;
                if (into >= BREAK_SPEED && breakInto(level, at)) continue;
                contact(t, out, Math.min(pen, 1.0));
                p = rodA.lerp(rodB, t);
                if (t < 0.5) touchA = true;
                else touchB = true;
            }
        }
    }

    /** Whether, lying at rest, it is still borne up: something solid just beneath it at two points or more. */
    private boolean supported(ServerLevel level) {
        double r = radius();
        int n = samples(), bearing = 0;
        for (int i = 0; i <= n; i++) {
            net.minecraft.world.phys.Vec3 p = rodA.lerp(rodB, (double) i / n);
            if (solid(level, BlockPos.containing(p.x, p.y - r - 0.15, p.z))) bearing++;
            else for (PlacedStaffEntity o : level.getEntitiesOfClass(PlacedStaffEntity.class, new AABB(p, p).inflate(r + 0.5), x -> x != this)) {
                bearing++;
                break;
            }
        }
        return bearing >= 2;
    }

    /**
     * Staffs do not pass through each other. Where this one overlaps another, both are pushed apart
     * at the points that touch, along the line between them: a staff landing across another slides
     * and rolls off its round side. A standing staff does not give way.
     */
    private void pushOffOtherStaffs(ServerLevel level) {
        float r = radius();
        AABB near = new AABB(rodA, rodB).inflate(r + 6);
        for (PlacedStaffEntity other : level.getEntitiesOfClass(PlacedStaffEntity.class, near, o -> o != this && o.isAlive())) {
            boolean moves = other.isRod();
            net.minecraft.world.phys.Vec3 oa = moves ? other.rodA : other.position();
            net.minecraft.world.phys.Vec3 ob = moves ? other.rodB : other.position().add(0, other.length(), 0);
            double[] st = closest(rodA, rodB, oa, ob);
            net.minecraft.world.phys.Vec3 p = rodA.lerp(rodB, st[0]), q = oa.lerp(ob, st[1]);
            net.minecraft.world.phys.Vec3 d = p.subtract(q);
            double dist = d.length(), reach = r + other.radius();
            if (dist >= reach) continue;
            net.minecraft.world.phys.Vec3 n = dist > 1e-5 ? d.scale(1 / dist) : new net.minecraft.world.phys.Vec3(0, 1, 0);
            double pen = Math.min(1.0, reach - dist), share = moves ? 0.5 : 1.0;
            contact(st[0], n, pen * share);
            if (moves) {
                other.moveAt(st[1], n.scale(-pen * share));
                other.wake();
            }
            if (st[0] < 0.5) touchA = true;
            else touchB = true;
        }
    }

    /** The shares {s, t} along segments a0-a1 and b0-b1 of their two closest points. */
    public static double[] closest(net.minecraft.world.phys.Vec3 a0, net.minecraft.world.phys.Vec3 a1,
                                    net.minecraft.world.phys.Vec3 b0, net.minecraft.world.phys.Vec3 b1) {
        net.minecraft.world.phys.Vec3 d1 = a1.subtract(a0), d2 = b1.subtract(b0), r = a0.subtract(b0);
        double a = d1.dot(d1), e = d2.dot(d2), f = d2.dot(r), s, t;
        if (a < 1e-9 && e < 1e-9) return new double[]{0, 0};
        if (a < 1e-9) return new double[]{0, Math.max(0, Math.min(1, f / e))};
        double c = d1.dot(r);
        if (e < 1e-9) return new double[]{Math.max(0, Math.min(1, -c / a)), 0};
        double b = d1.dot(d2), denom = a * e - b * b;
        s = denom > 1e-9 ? Math.max(0, Math.min(1, (b * f - c * e) / denom)) : 0;
        t = (b * s + f) / e;
        if (t < 0) { t = 0; s = Math.max(0, Math.min(1, -c / a)); }
        else if (t > 1) { t = 1; s = Math.max(0, Math.min(1, (b - c) / a)); }
        return new double[]{s, t};
    }

    private void syncRod() {
        entityData.set(ROD_BASE, rodA.toVector3f());
        entityData.set(ROD_TIP, rodB.toVector3f());
        entityData.set(ROD_SPIN, spin);
    }

    /** Lying fallen, it covers the ground from foot to tip. */
    @Override
    protected AABB makeBoundingBox() {
        if (!isRod()) return super.makeBoundingBox();
        net.minecraft.world.phys.Vec3 a = rodBase(), b = rodTip();
        if (a.equals(b)) return super.makeBoundingBox();
        float r = radius();
        return new AABB(a, b).inflate(r, r, r);
    }

    /** Distance from point (px,pz) to the line segment (ax,az)-(bx,bz). */
    private static float distanceToSegment(double px, double pz,
                                           double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double lenSq = dx * dx + dz * dz;
        if (lenSq == 0) return (float) Math.sqrt((px - ax) * (px - ax) + (pz - az) * (pz - az));
        float t = (float) Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / lenSq));
        double projX = ax + t * dx, projZ = az + t * dz;
        return (float) Math.sqrt((px - projX) * (px - projX) + (pz - projZ) * (pz - projZ));
    }

    // ── Tick ─────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Client-side: count animation ticks while falling
        if (level().isClientSide()) {
            easeShownScale();
            shownRodO = shownRod;
            shownRod = new net.minecraft.world.phys.Vec3[]{rodBase(), rodTip()};
            if (shownRodO == null) shownRodO = shownRod;
            if (isFalling()) {
                fallAnimTicks++;
            }
            return;
        }

        if (isEcho()) {
            followMaster();
            return;
        }

        if (isRod() && level() instanceof ServerLevel server) {
            rodTick(server);
            if (!isAlive()) return;
        } else if (isStanding() && tickCount % 10 == 0) {
            updateReach(); // what stands above it may have changed
        }

        // Return countdown
        if (returnTimer > 0) {
            returnTimer--;
        } else if (returnTimer == 0) {
            returnToOwner();
            return;
        }

        // Remove if owner disconnects (after 60 seconds)
        if (tickCount > 1200 && getOwnerUUID() != null) {
            Player owner = level().getPlayerByUUID(getOwnerUUID());
            if (owner == null) {
                discard();
            }
        }
    }

    /** An echo's tick: its master's size (drilling as it grows), its master's fall; gone with its master. */
    private void followMaster() {
        if (!(level() instanceof ServerLevel server)) return;
        Entity found = server.getEntity(master);
        if (!(found instanceof PlacedStaffEntity m) || !m.isAlive() || m.returnTimer == 0) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            discard(); // a hair's staff, back to a hair
            return;
        }
        if (isFalling()) {
            rodTick(server); // fallen, it lies by its own weight
            return;
        }
        float mine = getStaffScale(), theirs = m.getStaffScale();
        if (theirs > mine) {
            grower = m.grower;
            float reached = drillTo(mine, theirs);
            if (reached > mine) setStaffScale(reached);
        } else if (theirs < mine) {
            setStaffScale(theirs);
        }
        if (m.isFalling()) {
            ServerPlayer owner = m.toppledBy;
            if (owner == null) return;
            this.entityData.set(FALLING, true);
            this.entityData.set(TOPPLE_YAW, m.getToppleYaw());
            toppledBy = owner;
            topple(owner, m.getToppleYaw());
        }
    }

    private void returnToOwner() {
        UUID ownerUUID = getOwnerUUID();
        returnTo(ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID));
    }

    /** Back into {@code owner}'s hand (or at their feet, if their hands are full); gone from the ground. */
    private void returnTo(@Nullable Player owner) {
        {
            if (owner != null) {
                // Give staff back
                ItemStack staff = new ItemStack(ModItems.RUYI_JINGU_BANG.get());
                if (!owner.getInventory().add(staff)) {
                    owner.drop(staff, false);
                }

                // Return effect
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD,
                            getX(), getY() + 0.5, getZ(), 10,
                            0.3, 0.3, 0.3, 0.05);
                }
            }
        }
        discard();
    }

    // ── Interaction ──────────────────────────────────────────────────

    @Override
    public boolean isPickable() {
        return true; // needed for raycasting / hitting
    }

    @Override
    public boolean canBeCollidedWith() {
        return true; // needed for raycasting and player push-back
    }

    @Override
    public boolean canCollideWith(Entity other) {
        // Only block players from walking through — don't interfere with other entities
        return other instanceof Player;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        // Handle the topple directly here — for non-LivingEntity,
        // this is the only reliable attack entry point
        if (attacker instanceof ServerPlayer player) strikeBy(player);
        return true; // handled here, or a non-owner's swing that does nothing
    }

    // ── Serialization ────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStaffScale(tag.getFloat("StaffScale"));
        drillPaid = tag.getDouble("DrillPaid");
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        if (tag.getBoolean("Fallen")) {
            entityData.set(FALLING, true);
            rodA = new net.minecraft.world.phys.Vec3(tag.getDouble("FootX"), tag.getDouble("FootY"), tag.getDouble("FootZ"));
            rodB = new net.minecraft.world.phys.Vec3(tag.getDouble("TipX"), tag.getDouble("TipY"), tag.getDouble("TipZ"));
            velA = velB = net.minecraft.world.phys.Vec3.ZERO;
            spin = tag.getFloat("Spin");
            landed = true; // a staff saved fallen has landed; its blast is long done
            syncRod();
        }
        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("StaffScale", getStaffScale());
        tag.putDouble("DrillPaid", drillPaid);
        if (master != null) tag.putUUID("Master", master);
        if (isRod()) {
            tag.putBoolean("Fallen", true);
            tag.putDouble("FootX", rodA.x); tag.putDouble("FootY", rodA.y); tag.putDouble("FootZ", rodA.z);
            tag.putDouble("TipX", rodB.x); tag.putDouble("TipY", rodB.y); tag.putDouble("TipZ", rodB.z);
            tag.putFloat("Spin", spin);
        }
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}
