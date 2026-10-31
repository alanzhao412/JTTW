package com.jttw.engine.entity.spider;

import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Stun;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A strand of silk spat by a spider demon of Silk-Web Ridge: it flies straight, and what it strikes
 * (but the ridge's own) it hurts a little and <b>pins</b> ({@link Stun#pin}) for {@link #pinTicks()}:
 * held where it stands, though it may still strike what is in reach.
 */
public class SilkStrandEntity extends Fireball {

    private float damage = 2f;
    private int pin = 30;

    public SilkStrandEntity(EntityType<? extends SilkStrandEntity> type, Level level) {
        super(type, level);
        setItem(new ItemStack(SpiderCaveRegistry.SPIDER_SILK.get()));
    }

    /** One spat by {@code owner} along {@code dir}, hitting for {@code damage}, pinning for {@code pinTicks}. */
    public SilkStrandEntity(Level level, LivingEntity owner, Vec3 dir, float damage, int pinTicks) {
        super(SpiderCaveRegistry.SILK_STRAND.get(), owner, dir, level);
        this.damage = damage;
        this.pin = pinTicks;
        setItem(new ItemStack(SpiderCaveRegistry.SPIDER_SILK.get()));
        setPos(owner.getX(), owner.getY(0.55), owner.getZ());
    }

    public float damage() {
        return damage;
    }

    public int pinTicks() {
        return pin;
    }

    /** Its strike on {@code e}: its hurt, and pinned, unless {@code e} is the ridge's own. Returns whether it landed. */
    public boolean strike(Entity e) {
        if (e instanceof SilkKin) return false;
        boolean hit = e.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity l ? l : null), damage);
        if (hit && e instanceof LivingEntity l) Stun.pin(l, pin, getOwner());
        return hit;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide) strike(result.getEntity());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) discard();
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.WHITE_ASH;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Pin", pin);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        pin = tag.getInt("Pin");
    }
}
