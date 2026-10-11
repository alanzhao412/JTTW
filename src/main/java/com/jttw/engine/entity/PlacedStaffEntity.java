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
        if (isFalling() || returnTimer >= 0 || isEcho()) return;
        float oldScale = getStaffScale();
        float newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, oldScale + delta * SCROLL_STEP));
        if (newScale == oldScale) return;
        setStaffScale(newScale);

        // Destroy blocks the staff now occupies as it grows upward
        if (!level().isClientSide() && newScale > oldScale) {
            drillBlocks(newScale);
        }
    }

    /**
     * Destroy all non-bedrock blocks inside the staff's vertical column
     * at the current scale. Simulates the staff drilling through terrain.
     */
    private void drillBlocks(float scale) {
        float staffHeight = VISUAL_HEIGHT * scale;
        float staffRadius = 0.25f + scale * 0.05f; // slightly larger than visual

        int minBX = net.minecraft.util.Mth.floor(getX() - staffRadius);
        int maxBX = net.minecraft.util.Mth.ceil(getX() + staffRadius);
        int minBZ = net.minecraft.util.Mth.floor(getZ() - staffRadius);
        int maxBZ = net.minecraft.util.Mth.ceil(getZ() + staffRadius);
        int minBY = net.minecraft.util.Mth.floor(getY());
        int maxBY = net.minecraft.util.Mth.ceil(getY() + staffHeight);

        java.util.List<BlockPos> column = new java.util.ArrayList<>();
        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int bz = minBZ; bz <= maxBZ; bz++) {
                for (int by = minBY; by <= maxBY; by++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    BlockState state = level().getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0) column.add(pos);
                }
            }
        }
        if (!column.isEmpty() && level() instanceof ServerLevel sl) {
            com.jttw.engine.world.TerrainBreaker.breakBlocks(sl, column, // no drops, spread over ticks
                    getOwnerUUID() == null ? null : sl.getPlayerByUUID(getOwnerUUID()));
            sl.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + staffHeight, getZ(),
                    (int)(scale * 0.5f + 2), 0.4, 0.4, 0.4, 0.02);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                    Math.min(2.0f, 0.5f + scale * 0.02f), 1.2f);
        }
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

    /** Number of angle steps for the arc sweep (vertical → horizontal). */
    private static final int ARC_STEPS = 12;

    /**
     * Struck down: its path is cleared (what it sweeps through as it falls is broken, as before),
     * and it begins to fall. The blast comes where and when it lands ({@link #land}).
     */
    private void topple(ServerPlayer owner, float yaw) {
        float scale = getStaffScale();
        ServerLevel serverLevel = (ServerLevel) level();

        // Fall direction: the way the owner faced as they struck (an echo: as its master fell)
        float yawRad = (float) Math.toRadians(yaw);
        float dirX = -net.minecraft.util.Mth.sin(yawRad);
        float dirZ = net.minecraft.util.Mth.cos(yawRad);

        // Staff length at current scale (visual, accounting for FIXED 0.5×)
        float length = VISUAL_HEIGHT * scale;

        // ── Phase 1: Arc sweep — destroy blocks along the falling arc ──
        // The staff pivots at the base from vertical (0°) to horizontal (90°).
        // At angle θ from vertical, a point at distance d from the base is at:
        //   y = d × cos(θ),  horizontal = d × sin(θ)
        // Skip the first 3 blocks near the base so we don't destroy where
        // the pole is standing.
        float staffRadius = 0.25f + scale * 0.05f;
        int staffSegments = Math.max(2, (int)(length / 2.0f)); // sample points along staff
        float skipDist = 3.0f; // don't destroy blocks within 3 blocks of the base
        java.util.List<BlockPos> arc = new java.util.ArrayList<>();

        for (int step = 0; step <= ARC_STEPS; step++) {
            float theta = (float)(Math.PI / 2.0 * step / ARC_STEPS); // 0 → π/2
            float cosT = (float) Math.cos(theta);
            float sinT = (float) Math.sin(theta);

            // Trace along the staff at this angle
            for (int seg = 1; seg <= staffSegments; seg++) {
                float d = length * seg / staffSegments;
                // Skip points close to the base
                if (d < skipDist) continue;
                double bx = getX() + dirX * d * sinT;
                double bz = getZ() + dirZ * d * sinT;
                double by = getY() + d * cosT;

                // Destroy blocks in a small radius around this point
                int minBX = net.minecraft.util.Mth.floor(bx - staffRadius);
                int maxBX = net.minecraft.util.Mth.ceil(bx + staffRadius);
                int minBZ = net.minecraft.util.Mth.floor(bz - staffRadius);
                int maxBZ = net.minecraft.util.Mth.ceil(bz + staffRadius);
                int minBY = net.minecraft.util.Mth.floor(by - staffRadius);
                int maxBY = net.minecraft.util.Mth.ceil(by + staffRadius);

                for (int ix = minBX; ix <= maxBX; ix++) {
                    for (int iz = minBZ; iz <= maxBZ; iz++) {
                        for (int iy = minBY; iy <= maxBY; iy++) {
                            arc.add(new BlockPos(ix, iy, iz)); // the breaker skips air and the unbreakable
                        }
                    }
                }
            }
        }

        com.jttw.engine.world.TerrainBreaker.breakBlocks(serverLevel, arc, owner);

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

    /** Gravity, blocks per tick per tick: as anything falls. */
    private static final double GRAVITY = 0.08;
    /** How much of its sideways motion a resting end keeps each tick, touching ground. */
    private static final double GROUND_GRIP = 0.55;
    /** Passes over the rod's constraints each tick: more is stiffer and steadier. */
    private static final int PASSES = 4;
    /** Ticks it must lie still before it rests (and stops being simulated until disturbed). */
    private static final int SETTLE = 30;

    private net.minecraft.world.phys.Vec3 rodA = net.minecraft.world.phys.Vec3.ZERO, rodB = net.minecraft.world.phys.Vec3.ZERO,
            rodAPrev = net.minecraft.world.phys.Vec3.ZERO, rodBPrev = net.minecraft.world.phys.Vec3.ZERO;
    private boolean landed, resting, touchA, touchB;
    private int stillTicks;
    private float spin;
    private UUID landingOwner;

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

    /** Upright on its foot, tipped a few degrees the way it is to fall, with a nudge that way. */
    private void startFall(float yaw) {
        float yawRad = (float) Math.toRadians(yaw);
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(-net.minecraft.util.Mth.sin(yawRad), 0, net.minecraft.util.Mth.cos(yawRad));
        double L = length(), tilt = Math.toRadians(4);
        rodA = position();
        rodB = rodA.add(dir.scale(Math.sin(tilt) * L)).add(0, Math.cos(tilt) * L, 0);
        rodAPrev = rodA;
        rodBPrev = rodB.subtract(dir.scale(0.02 * Math.max(1, L / 10)));
        landed = false;
        resting = false;
        stillTicks = 0;
        landingOwner = getOwnerUUID();
        syncRod();
    }

    /** One tick of the fallen rod. */
    private void rodTick(ServerLevel level) {
        if (resting) {
            // at rest it is still watched: if what bears it is blasted away, or another staff lands on it, it moves again
            if (tickCount % 10 == 0 && !supported(level)) wake();
            if (resting) return;
        }
        net.minecraft.world.phys.Vec3 g = new net.minecraft.world.phys.Vec3(0, -GRAVITY, 0);
        net.minecraft.world.phys.Vec3 va = rodA.subtract(rodAPrev).scale(0.995), vb = rodB.subtract(rodBPrev).scale(0.995);
        rodAPrev = rodA;
        rodBPrev = rodB;
        rodA = rodA.add(va).add(g);
        rodB = rodB.add(vb).add(g);
        touchA = touchB = false;
        double L = length();
        for (int pass = 0; pass < PASSES; pass++) {
            keepLength(L);
            restOnGround(level);
            pushOffOtherStaffs(level);
        }
        // friction where it touches: a resting end keeps little of its sideways slide
        if (touchA) rodAPrev = gripped(rodA, rodAPrev);
        if (touchB) rodBPrev = gripped(rodB, rodBPrev);
        // rolling: sideways travel of its middle, across its length, turns it about its length
        net.minecraft.world.phys.Vec3 axis = rodB.subtract(rodA);
        net.minecraft.world.phys.Vec3 across = new net.minecraft.world.phys.Vec3(-axis.z, 0, axis.x);
        if (across.lengthSqr() > 1e-6) {
            net.minecraft.world.phys.Vec3 moved = rodA.add(rodB).scale(0.5).subtract(rodAPrev.add(rodBPrev).scale(0.5));
            spin += (float) (moved.dot(across.normalize()) / radius());
        }
        if (!landed && (touchB || angleFromUp() > 75) && rodB.y - rodBPrev.y > -0.2 && angleFromUp() > 40) {
            landed = true;
            ServerPlayer owner = landingOwner == null ? null : level.getServer().getPlayerList().getPlayer(landingOwner);
            if (owner == null && toppledBy != null) owner = toppledBy;
            if (owner != null) land(owner);
        }
        double motion = Math.max(rodA.distanceToSqr(rodAPrev), rodB.distanceToSqr(rodBPrev));
        stillTicks = motion < 0.003 * 0.003 && landed ? stillTicks + 1 : 0;
        if (stillTicks > SETTLE) {
            resting = true;
            rodAPrev = rodA;
            rodBPrev = rodB;
        }
        if (rodA.y < level.getMinBuildHeight() - 64) { // fell out of the world
            discard();
            return;
        }
        setPos(rodA.x, rodA.y, rodA.z);
        syncRod();
    }

    private double angleFromUp() {
        net.minecraft.world.phys.Vec3 axis = rodB.subtract(rodA);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, axis.y / Math.max(1e-6, axis.length())))));
    }

    private static net.minecraft.world.phys.Vec3 gripped(net.minecraft.world.phys.Vec3 now, net.minecraft.world.phys.Vec3 prev) {
        // keep GROUND_GRIP of the sideways velocity: move prev toward now
        net.minecraft.world.phys.Vec3 v = now.subtract(prev);
        return now.subtract(new net.minecraft.world.phys.Vec3(v.x * GROUND_GRIP, v.y, v.z * GROUND_GRIP));
    }

    private void wake() {
        resting = false;
        stillTicks = 0;
    }

    /** Moves the rod so the point a share {@code t} of the way from foot to tip moves by {@code by}. */
    private void moveAt(double t, net.minecraft.world.phys.Vec3 by) {
        double wa = 1 - t, wb = t, k = 1 / (wa * wa + wb * wb);
        rodA = rodA.add(by.scale(wa * k));
        rodB = rodB.add(by.scale(wb * k));
    }

    private void keepLength(double L) {
        net.minecraft.world.phys.Vec3 d = rodB.subtract(rodA);
        double len = d.length();
        if (len < 1e-6) return;
        net.minecraft.world.phys.Vec3 fix = d.scale((len - L) / len * 0.5);
        rodA = rodA.add(fix);
        rodB = rodB.subtract(fix);
    }

    /** How many points along it are tested against the ground: about one per half block, up to 64. */
    private int samples() {
        return Math.max(4, Math.min(64, (int) Math.ceil(length() / 0.5)));
    }

    /**
     * Each point along it rests on the highest solid surface beneath it: so in a crater it settles
     * down onto the crater's floor, tilting with it, and lies on a ridge if it lands across one.
     */
    private void restOnGround(ServerLevel level) {
        float r = radius();
        int n = samples();
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            net.minecraft.world.phys.Vec3 p = rodA.lerp(rodB, t);
            double top = surfaceBelow(level, p, r);
            double bottom = p.y - r;
            if (top <= bottom) continue;
            double push = Math.min(1.0, top - bottom); // a little at a time: no leaping out of the ground
            moveAt(t, new net.minecraft.world.phys.Vec3(0, push, 0));
            if (t < 0.5) touchA = true;
            else touchB = true;
        }
    }

    /** The top of the highest solid block under {@code p} reaching above its lowest point, or -inf. */
    private static double surfaceBelow(ServerLevel level, net.minecraft.world.phys.Vec3 p, double r) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int x = net.minecraft.util.Mth.floor(p.x), z = net.minecraft.util.Mth.floor(p.z);
        int from = net.minecraft.util.Mth.floor(p.y + r), to = net.minecraft.util.Mth.floor(p.y - r) - 1;
        for (int y = from; y >= to; y--) {
            at.set(x, y, z);
            if (!level.isLoaded(at)) return Double.NEGATIVE_INFINITY;
            var shape = level.getBlockState(at).getCollisionShape(level, at);
            if (!shape.isEmpty()) return y + shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return Double.NEGATIVE_INFINITY;
    }

    /** Whether, lying at rest, it is still borne up: every few points still find ground under them. */
    private boolean supported(ServerLevel level) {
        float r = radius();
        int n = samples(), bearing = 0;
        for (int i = 0; i <= n; i++) {
            net.minecraft.world.phys.Vec3 p = rodA.lerp(rodB, (double) i / n);
            if (surfaceBelow(level, p.add(0, -0.3, 0), r) > Double.NEGATIVE_INFINITY) bearing++;
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
            moveAt(st[0], n.scale(pen * share));
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
        if (theirs != mine) {
            setStaffScale(theirs);
            if (theirs > mine) drillBlocks(theirs);
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
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        if (tag.getBoolean("Fallen")) {
            entityData.set(FALLING, true);
            rodA = rodAPrev = new net.minecraft.world.phys.Vec3(tag.getDouble("FootX"), tag.getDouble("FootY"), tag.getDouble("FootZ"));
            rodB = rodBPrev = new net.minecraft.world.phys.Vec3(tag.getDouble("TipX"), tag.getDouble("TipY"), tag.getDouble("TipZ"));
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
