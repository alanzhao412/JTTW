package com.jttw.engine.entity;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.erlang.XiaotianQuanManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Xiaotian Quan's howl projectile — a sonic wave that inflicts Nausea on hit.
 */
public class HowlProjectileEntity extends ThrowableItemProjectile {

    private static final int NAUSEA_DURATION = 100; // 5 seconds
    private static final int NAUSEA_AMPLIFIER = 0;
    private static final float DAMAGE = 3.0F;

    public HowlProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public HowlProjectileEntity(Level level, LivingEntity shooter) {
        super(JTTWEngine.HOWL_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL; // visual placeholder — nearly invisible with flat trajectory
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) return;

        if (result.getEntity() instanceof LivingEntity target) {
            // Don't hurt owner or allied XTQ
            if (this.getOwner() instanceof XiaotianQuanEntity xtq) {
                if (target == xtq) return;
                var owner = XiaotianQuanManager.resolveOwner(xtq);
                if (target == owner) return;
                if (owner != null && XiaotianQuanManager.isAllyXtq(target, owner.getUUID())) return;
            }

            target.hurt(this.damageSources().mobProjectile(this, (LivingEntity) this.getOwner()), DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION, NAUSEA_AMPLIFIER));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return 0.01F; // Nearly flat trajectory for a sonic wave
    }
}
