package com.jttw.engine.entity;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Erlang Shen's thrown spear (Spear Mastery I). It flies in a straight line, tearing through the
 * terrain: each block it destroys spends its hardness from a budget set by the thrower's
 * strength. It stops at an unbreakable block, when the budget runs out, or after
 * {@value #MAX_DISTANCE} blocks, and stays there until the spear returns to its thrower
 * (see {@link com.jttw.engine.item.SpearReturn}). Blocks it breaks are destroyed, not dropped.
 * Everything it passes through is hit once.
 */
public class ThrownSpearEntity extends Projectile implements ItemSupplier {

    public static final double MAX_DISTANCE = 100.0;
    /** Blocks per tick. */
    public static final double SPEED = 3.0;
    private static final double STEP = 0.5;
    /** Even a block that breaks instantly slows the spear a little. */
    public static final double MIN_BLOCK_COST = 0.5;

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownSpearEntity.class, EntityDataSerializers.ITEM_STACK);

    private Vec3 direction = Vec3.ZERO;
    private double budget;
    private double travelled;
    private float damage;
    private boolean stuck;
    private final Set<Integer> hit = new HashSet<>();

    public ThrownSpearEntity(EntityType<? extends ThrownSpearEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ThrownSpearEntity(Level level, LivingEntity thrower, ItemStack spear, Vec3 direction,
                             double budget, float damage) {
        this(ModEntities.THROWN_SPEAR.get(), level);
        setOwner(thrower);
        entityData.set(DATA_ITEM, spear.copy());
        this.direction = direction.normalize();
        this.budget = budget;
        this.damage = damage;
        setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
        setDeltaMovement(this.direction.scale(SPEED));
        setYRot((float) (Math.atan2(this.direction.x, this.direction.z) * (180F / Math.PI)));
        setXRot((float) (Math.atan2(this.direction.y, this.direction.horizontalDistance()) * (180F / Math.PI)));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItem() {
        ItemStack s = entityData.get(DATA_ITEM);
        return s.isEmpty() ? new ItemStack(ModItems.THREE_POINTED_SPEAR.get()) : s;
    }

    public boolean isStuck() { return stuck; }
    public double travelled() { return travelled; }
    public double budgetLeft() { return budget; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            setPos(position().add(getDeltaMovement()));
            return;
        }
        if (stuck) return;
        for (double moved = 0; moved < SPEED && !stuck; moved += STEP) {
            advance();
        }
        setDeltaMovement(stuck ? Vec3.ZERO : direction.scale(SPEED));
    }

    /** Moves half a block: through the block there, and through whatever stands in it. */
    private void advance() {
        Vec3 next = position().add(direction.scale(STEP));
        BlockPos pos = BlockPos.containing(next);
        if (!tearThrough(pos)) {
            stop();
            return;
        }
        setPos(next);
        travelled += STEP;
        hitEntitiesAt(next);
        if (travelled >= MAX_DISTANCE) stop();
    }

    /** Clears the block at pos if the budget allows; false if the spear stops here. */
    private boolean tearThrough(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(level(), pos).isEmpty()) return true;
        float hardness = state.getDestroySpeed(level(), pos);
        if (hardness < 0) return false; // bedrock, barriers, end portal frames
        // the mod's own places stand up to it, unless its thrower is very strong (see HardenedGround)
        if (!com.jttw.engine.world.HardenedGround.carvable(level(), pos, state,
                com.jttw.engine.world.HardenedGround.strength(getOwner()))) return false;
        double cost = Math.max(MIN_BLOCK_COST, hardness);
        if (cost > budget) return false;
        if (getOwner() instanceof ServerPlayer owner
                && MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level(), pos, state, owner))) {
            return false; // a protected area
        }
        level().destroyBlock(pos, false, getOwner());
        budget -= cost;
        return true;
    }

    private void hitEntitiesAt(Vec3 at) {
        Entity owner = getOwner();
        AABB box = new AABB(at, at).inflate(0.6);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && !hit.contains(e.getId()))) {
            hit.add(target.getId());
            target.hurt(damageSources().trident(this, owner == null ? this : owner), damage);
        }
    }

    private void stop() {
        stuck = true;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** The model is about 3 blocks long; keep it drawn while any of it is on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.0);
    }

    /** The thrower's note brings the spear back, so the entity itself is never saved. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
