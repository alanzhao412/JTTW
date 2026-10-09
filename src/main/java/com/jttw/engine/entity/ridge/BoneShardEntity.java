package com.jttw.engine.entity.ridge;

import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A shard of bone Lady White Bone hurls: flies nearly straight, and hurts what it strikes. */
public class BoneShardEntity extends ThrowableItemProjectile {

    private float damage = 16f;

    public BoneShardEntity(EntityType<? extends BoneShardEntity> type, Level level) {
        super(type, level);
    }

    public BoneShardEntity(Level level, LivingEntity thrower, float damage) {
        super(ModEntities.BONE_SHARD.get(), thrower, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BONE;
    }

    @Override
    protected float getGravity() {
        return 0.005f;
    }

    /** It passes through her own dead. */
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity e) {
        return super.canHitEntity(e) && !(e instanceof RidgeMonster);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() == getOwner()) return;
        hit.getEntity().hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), damage);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE)), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.08);
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
    }
}
