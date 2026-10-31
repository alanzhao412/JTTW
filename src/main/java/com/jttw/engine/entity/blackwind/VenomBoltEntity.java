package com.jttw.engine.entity.blackwind;

import com.jttw.engine.registry.BlackWindRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A gob of venom spat by the White-Robed Scholar, the white-flower snake: it hits for his spit and
 * poisons what it hits. It does not burn and lights nothing.
 */
public class VenomBoltEntity extends Fireball {

    public static final int POISON_TICKS = 100;

    private float damage = 4f;

    public VenomBoltEntity(EntityType<? extends VenomBoltEntity> type, Level level) {
        super(type, level);
        setItem(new ItemStack(Items.SLIME_BALL));
    }

    public VenomBoltEntity(Level level, LivingEntity owner, Vec3 dir, float damage) {
        super(BlackWindRegistry.VENOM_BOLT.get(), owner, dir, level);
        this.damage = damage;
        setItem(new ItemStack(Items.SLIME_BALL));
        setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
    }

    public float damage() {
        return damage;
    }

    /** Its hit on {@code e}: its spit and the poison. Returns whether it landed. */
    public boolean strike(Entity e) {
        if (e == getOwner() || e instanceof BlackWindMonster) return false;
        boolean hit = e.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity l ? l : null), damage);
        if (hit && e instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 1), getOwner());
        return hit;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
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
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
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
