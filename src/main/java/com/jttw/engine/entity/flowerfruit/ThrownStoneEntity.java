package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.FlowerFruitRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A stone thrown by one of Flower-Fruit Mountain's gibbons: it arcs, hits for its thrower's blow,
 * and breaks where it lands. It never hurts the thrower's own side.
 */
public class ThrownStoneEntity extends ThrowableItemProjectile {

    private float damage = 3f;

    public ThrownStoneEntity(EntityType<? extends ThrownStoneEntity> type, Level level) {
        super(type, level);
    }

    public ThrownStoneEntity(Level level, LivingEntity owner, float damage) {
        super(FlowerFruitRegistry.THROWN_STONE.get(), owner, level);
        this.damage = damage;
    }

    public float damage() {
        return damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    /** Its hit on {@code e}, unless {@code e} is on its thrower's side. Returns whether it landed. */
    public boolean strike(Entity e) {
        Entity owner = getOwner();
        if (owner != null && (e == owner || owner.isAlliedTo(e) || e.isAlliedTo(owner))) return false;
        if (owner instanceof ZoneMonster && e instanceof ZoneMonster) return false;
        return e.hurt(damageSources().thrown(this, owner), damage);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) strike(result.getEntity());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), getX(), getY(), getZ(), 12, 0.15, 0.15, 0.15, 0.1);
            }
            playSound(SoundEvents.STONE_BREAK, 0.8f, 1.2f);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }
}
