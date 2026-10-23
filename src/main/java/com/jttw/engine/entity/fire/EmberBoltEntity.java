package com.jttw.engine.entity.fire;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.fire.SamadhiFire;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An ember flung by a fire demon (an Ember Imp, or the Eye-Spark Officer's sparks): a little ball
 * of the mountain's fire that hits for its thrower's blow and sets what it hits alight. It is
 * ordinary fire: Fire Resistance turns it, and it spares the mountain's own. It lights nothing
 * where it lands.
 */
public class EmberBoltEntity extends Fireball {

    private float damage = 4f;

    public EmberBoltEntity(EntityType<? extends EmberBoltEntity> type, Level level) {
        super(type, level);
    }

    /** One flung by {@code owner} from its eyes along {@code dir}, hitting for {@code damage}. */
    public EmberBoltEntity(Level level, LivingEntity owner, Vec3 dir, float damage) {
        super(ModEntities.EMBER_BOLT.get(), owner, dir, level);
        this.damage = damage;
        setPos(owner.getX(), owner.getEyeY() - 0.15, owner.getZ());
    }

    public float damage() {
        return damage;
    }

    /** Its hit on {@code e}: its blow and alight, unless {@code e} is the mountain's own. Returns whether it landed. */
    public boolean strike(Entity e) {
        if (e instanceof LivingEntity l && SamadhiFire.spared(l)) return false;
        boolean hit = e.hurt(damageSources().fireball(this, getOwner()), damage);
        if (hit) e.igniteForSeconds(4);
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
