package com.jttw.engine.entity;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.RuyiJinguBangItem;
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
 * The Ruyi Jingu Bang placed in the world as a scalable pillar.
 * Wukong can scroll to resize (1–100×), then punch it to topple it,
 * causing a scaled explosion. Returns to inventory after 2 seconds.
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

    private int returnTimer = -1; // ticks until return, -1 = not returning
    private int fallAnimTicks = 0; // client-side animation counter

    // Scaling limits
    public static final float MIN_SCALE = 1.0f;
    public static final float MAX_SCALE = 100.0f;
    private static final float SCROLL_STEP = 1.0f;

    // Damage/explosion scaling — the staff should obliterate everything in its path
    private static final float BASE_EXPLOSION_POWER = 6.0f;
    private static final float EXPLOSION_SCALE_FACTOR = 0.5f; // at 100x → 6 + 50 = 56
    private static final float BASE_DAMAGE = 20.0f;
    private static final float DAMAGE_SCALE_FACTOR = 3.0f;     // at 100x → 20 + 300 = 320

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
        this(JTTWEngine.PLACED_STAFF.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(OWNER_UUID, Optional.of(owner));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STAFF_SCALE, 1.0f);
        this.entityData.define(FALLING, false);
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(TOPPLE_YAW, 0.0f);
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

    // ── Scroll to scale ──────────────────────────────────────────────

    public void adjustScale(float delta) {
        if (isFalling() || returnTimer >= 0) return;
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

        boolean destroyedAny = false;
        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int bz = minBZ; bz <= maxBZ; bz++) {
                for (int by = minBY; by <= maxBY; by++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    BlockState state = level().getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0) {
                        level().destroyBlock(pos, true);
                        destroyedAny = true;
                    }
                }
            }
        }
        if (destroyedAny && level() instanceof ServerLevel sl) {
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
        if (isFalling() || returnTimer >= 0) return false;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player) || !isOwner(player)) {
            return false;
        }

        // Start the topple — fall in the direction the owner is looking
        this.entityData.set(FALLING, true);
        this.entityData.set(TOPPLE_YAW, player.getYRot());
        topple(player);
        return true;
    }

    // Staff length in blocks at scale 1 (model is 47 units / 16 ≈ 2.9375 blocks).
    // FIXED display context applies an additional 0.5× scale, so the visual height
    // in-world is STAFF_LENGTH * 0.5 per unit of entity scale.
    private static final float STAFF_LENGTH = 2.9375f;
    private static final float VISUAL_HEIGHT = STAFF_LENGTH * 0.5f; // ≈ 1.47 blocks at scale 1

    /** Number of angle steps for the arc sweep (vertical → horizontal). */
    private static final int ARC_STEPS = 12;

    private void topple(ServerPlayer owner) {
        float scale = getStaffScale();
        ServerLevel serverLevel = (ServerLevel) level();

        // Total explosion power scales with size — no cap, the staff is a divine weapon
        float totalPower = BASE_EXPLOSION_POWER + scale * EXPLOSION_SCALE_FACTOR;

        // Fall direction from owner's yaw
        float yawRad = (float) Math.toRadians(owner.getYRot());
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
                            BlockPos pos = new BlockPos(ix, iy, iz);
                            BlockState state = level().getBlockState(pos);
                            if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0) {
                                level().destroyBlock(pos, true);
                            }
                        }
                    }
                }
            }
        }

        // ── Phase 2: Final impact explosions along the horizontal line ──
        // Start explosions 3 blocks out from the base
        int explosionCount = Math.max(1, (int)(length / 3.0f));
        for (int i = 0; i <= explosionCount; i++) {
            float t = (float) i / explosionCount; // 0 = base, 1 = tip
            if (length * t < skipDist) continue;
            double ex = getX() + dirX * length * t;
            double ez = getZ() + dirZ * length * t;
            // Power ramps up toward the tip: base gets 30%, tip gets full
            float ramp = 0.3f + t * 0.7f;
            float pointPower = totalPower * ramp;
            level().explode(owner, ex, getY(), ez,
                    pointPower, Level.ExplosionInteraction.TNT);
        }

        // ── Phase 3: Entity damage in the full swept area ──
        float damageRadius = 2.0f + scale * 0.3f;
        float damage = BASE_DAMAGE + scale * DAMAGE_SCALE_FACTOR;

        double tipX = getX() + dirX * length;
        double tipZ = getZ() + dirZ * length;

        // AABB covering the arc zone (base to tip + height + radius)
        double minX = Math.min(getX(), tipX) - damageRadius;
        double maxX = Math.max(getX(), tipX) + damageRadius;
        double minZ = Math.min(getZ(), tipZ) - damageRadius;
        double maxZ = Math.max(getZ(), tipZ) + damageRadius;
        AABB damageBox = new AABB(minX, getY() - 1, minZ,
                                  maxX, getY() + length + 1, maxZ);

        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, damageBox,
                e -> e != owner && e.isAlive());

        for (LivingEntity target : targets) {
            double tx = target.getX();
            double tz = target.getZ();
            float distToLine = distanceToSegment(tx, tz, getX(), getZ(), tipX, tipZ);
            float falloff = Math.max(0.2f, 1.0f - (distToLine / damageRadius));
            target.hurt(level().damageSources().playerAttack(owner), damage * falloff);
            target.setSecondsOnFire((int)(2 + scale * 0.1f));
        }

        // ── Phase 4: Fire and particles along impact line ──
        for (int i = 0; i <= explosionCount; i++) {
            float t = (float) i / explosionCount;
            if (length * t < skipDist) continue;
            double px = getX() + dirX * length * t;
            double pz = getZ() + dirZ * length * t;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    px, getY(), pz, 1, 0.5, 0.3, 0.5, 0.0);
            BlockPos firePos = BlockPos.containing(px, getY(), pz);
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = firePos.above(dy);
                if (level().getBlockState(pos).isAir()
                        && level().getBlockState(pos.below()).isSolidRender(level(), pos.below())) {
                    level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }
        }
        serverLevel.sendParticles(ParticleTypes.FLAME,
                getX() + dirX * length * 0.5, getY() + 1, getZ() + dirZ * length * 0.5,
                (int)(scale * 2), length * 0.4, 1.0, length * 0.4, 0.02);

        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                Math.min(4.0f, 1.0f + scale * 0.05f), 0.8f);

        // Start 2-second return timer
        returnTimer = 40;
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
            if (isFalling()) {
                fallAnimTicks++;
            }
            return;
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

    private void returnToOwner() {
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null) {
            Player owner = level().getPlayerByUUID(ownerUUID);
            if (owner != null) {
                // Give staff back
                ItemStack staff = new ItemStack(JTTWEngine.RUYI_JINGU_BANG.get());
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
        if (attacker instanceof ServerPlayer player && isOwner(player)) {
            if (!isFalling() && returnTimer < 0) {
                this.entityData.set(FALLING, true);
                this.entityData.set(TOPPLE_YAW, player.getYRot());
                topple(player);
            }
            return true; // we handled it
        }
        return true; // ignore non-owner attacks
    }

    // ── Serialization ────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStaffScale(tag.getFloat("StaffScale"));
        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("StaffScale", getStaffScale());
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}
