package com.jttw.engine.entity;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Qi Wave — a destructive beam of energy fired from the Ruyi Jingu Bang.
 *
 * Travels 200 blocks in the direction the player was looking, destroying
 * all blocks (except bedrock) and damaging entities in an expanding cone.
 * Leaves fire in its wake.
 */
public class QiWaveEntity extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(QiWaveEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** Direction the wave travels (normalized). */
    private Vec3 direction = Vec3.ZERO;
    /** Player's ATK at the time of firing — determines damage. */
    private float attackDamage = 10.0f;
    /** How far the wave has traveled. */
    private double distanceTraveled = 0;
    /** Charge power from 0.0–1.0, affects all wave properties. */
    private float chargePower = 1.0f;
    /** STR factor 0.0–1.0 derived from ATK — interpolates between min and max values. */
    private float strFactor = 0.0f;
    /** When each creature was last struck, so it is struck once per damage window, not every step. */
    private final java.util.Map<Integer, Long> struckAt = new java.util.HashMap<>();
    /** The last step's sphere: the next step only needs what lies outside it. */
    private BlockPos lastCenter;
    private double lastRadius;
    /** Blocks processed per tick for smooth travel. */
    private static final int BLOCKS_PER_TICK = 8;
    /** Distance range: 5 blocks at 1 ATK → 200 blocks at max ATK. */
    private static final double MIN_DISTANCE = 5.0;
    private static final double MAX_DISTANCE = 200.0;
    /** Cone start radius range. */
    private static final double MIN_CONE_START = 1.0;
    private static final double MAX_CONE_START = 5.0;
    /** Cone end radius range. */
    private static final double MIN_CONE_END = 3.0;
    private static final double MAX_CONE_END = 40.0;
    /** ATK range for scaling: 1 = minimum (0.0), 80 = maximum (1.0). */
    private static final float ATK_MIN = 1.0f;
    private static final float ATK_MAX = 80.0f;

    public QiWaveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public QiWaveEntity(Level level, Player owner, Vec3 direction, float attackDamage) {
        this(ModEntities.QI_WAVE.get(), level);
        Vec3 dir = direction.normalize();
        // STR factor: 0.0 at 30 ATK → 1.0 at 200 ATK
        this.strFactor = Math.min(1.0f, Math.max(0.0f, (attackDamage - ATK_MIN) / (ATK_MAX - ATK_MIN)));
        // Offset spawn point forward so the wave doesn't destroy where the player stands
        double startRadius = MIN_CONE_START + (MAX_CONE_START - MIN_CONE_START) * strFactor;
        double offset = startRadius + 2.0;
        this.setPos(
                owner.getX() + dir.x * offset,
                owner.getEyeY() - 0.5 + dir.y * offset,
                owner.getZ() + dir.z * offset);
        this.direction = dir;
        this.attackDamage = attackDamage;
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
    }

    /**
     * Set charge power (0.0–1.0). Scales hardness budget, beam radius, and damage.
     * Must be called before the entity starts ticking.
     */
    public void setChargePower(float power) {
        this.chargePower = Math.max(0.0f, Math.min(1.0f, power));
        // Scale damage with charge
        this.attackDamage *= (0.4f + 0.6f * this.chargePower);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    // ── Tick: advance the wave ───────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || direction.equals(Vec3.ZERO)) {
            if (!level().isClientSide() && direction.equals(Vec3.ZERO)) discard();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();
        Player owner = getOwnerUUID() != null ? level().getPlayerByUUID(getOwnerUUID()) : null;
        // Interpolate cone and distance by STR factor
        double coneStart = MIN_CONE_START + (MAX_CONE_START - MIN_CONE_START) * strFactor;
        double coneEnd = MIN_CONE_END + (MAX_CONE_END - MIN_CONE_END) * strFactor;
        double maxDistance = MIN_DISTANCE + (MAX_DISTANCE - MIN_DISTANCE) * strFactor;
        // Charge power scales the end radius (min charge = smaller cone)
        double maxRadius = coneStart + (coneEnd - coneStart) * chargePower;

        java.util.List<BlockPos> carved = new java.util.ArrayList<>();
        for (int step = 0; step < BLOCKS_PER_TICK; step++) {
            if (distanceTraveled >= maxDistance) {
                // Final explosion at termination point
                Vec3 pos = position();
                com.jttw.engine.world.TerrainBreaker.breakBlocks(serverLevel, carved);
                com.jttw.engine.world.TerrainBreaker.explode(serverLevel, owner, pos.x, pos.y, pos.z, 3.0f);
                discard();
                return;
            }

            // Advance position
            Vec3 newPos = position().add(direction);
            setPos(newPos.x, newPos.y, newPos.z);
            distanceTraveled += 1.0;

            // Cone radius expands linearly with distance
            double t = Math.min(1.0, distanceTraveled / maxDistance);
            double effectiveRadius = coneStart + (maxRadius - coneStart) * t;

            // Process blocks in the cone radius
            BlockPos center = blockPosition();
            int r = (int) Math.ceil(effectiveRadius);

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dy * dy + dz * dz > effectiveRadius * effectiveRadius) continue;

                        BlockPos bp = center.offset(dx, dy, dz);
                        // Inside the last step's sphere: already carved
                        if (lastCenter != null && bp.distSqr(lastCenter) <= lastRadius * lastRadius) continue;
                        // Fluids and blocks alike; the breaker leaves air and bedrock
                        carved.add(bp);
                    }
                }
            }

            lastCenter = center;
            lastRadius = effectiveRadius;

            // Damage entities in the beam corridor
            AABB damageBox = new AABB(
                    newPos.x - effectiveRadius, newPos.y - effectiveRadius, newPos.z - effectiveRadius,
                    newPos.x + effectiveRadius, newPos.y + effectiveRadius, newPos.z + effectiveRadius);

            // Qi Wave deals full magic damage (bypasses armor)
            DamageSource dmgSource = level().damageSources().magic();

            List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, damageBox,
                    e -> e != owner && e.isAlive());
            long now = level().getGameTime();
            for (LivingEntity target : targets) {
                // Once per damage window (vanilla ignores hits inside it), not on every step
                Long last = struckAt.get(target.getId());
                if (last != null && now - last < 10) continue;
                struckAt.put(target.getId(), now);
                target.hurt(dmgSource, attackDamage);
                target.setSecondsOnFire(5);

                // Massive explosion on impact
                com.jttw.engine.world.TerrainBreaker.explode(serverLevel, owner,
                        target.getX(), target.getY(), target.getZ(), 4.0f + 2.0f * chargePower);

                // 10-block knockback in the wave's travel direction
                Vec3 knockback = direction.normalize().scale(10.0);
                target.setDeltaMovement(knockback.x, 0.5, knockback.z);
                target.hasImpulse = true;
            }

            // Particles — flame trail along the beam
            int particleCount = (int)(4 + 6 * chargePower);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    newPos.x, newPos.y, newPos.z, particleCount,
                    effectiveRadius * 0.4, effectiveRadius * 0.4, effectiveRadius * 0.4, 0.02);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    newPos.x, newPos.y, newPos.z, particleCount / 2,
                    effectiveRadius * 0.3, effectiveRadius * 0.3, effectiveRadius * 0.3, 0.01);
        }

        com.jttw.engine.world.TerrainBreaker.breakBlocks(serverLevel, carved);

        // Sound every few ticks
        if (tickCount % 3 == 0) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                    2.0f, 0.5f + level().random.nextFloat() * 0.3f);
        }
    }

    // ── Serialization ────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        attackDamage = tag.getFloat("AttackDamage");
        chargePower = tag.getFloat("ChargePower");
        strFactor = tag.contains("StrFactor") ? tag.getFloat("StrFactor") : 0.0f;
        distanceTraveled = tag.getDouble("DistanceTraveled");
        direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("AttackDamage", attackDamage);
        tag.putFloat("ChargePower", chargePower);
        tag.putFloat("StrFactor", strFactor);
        tag.putDouble("DistanceTraveled", distanceTraveled);
        tag.putDouble("DirX", direction.x);
        tag.putDouble("DirY", direction.y);
        tag.putDouble("DirZ", direction.z);
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
    }
}
