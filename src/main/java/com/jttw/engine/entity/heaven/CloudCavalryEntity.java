package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cloud cavalry (云骑): a rider of the host on a celestial steed whose hooves tread cloud, a long lance
 * couched. Heavy: eight ordinary hits to kill, a blow of {@value #BLOW}; and when its foe is a few strides
 * off it lowers the lance ({@value #WIND_UP} ticks, its line marked) and charges {@value #REACH} blocks,
 * striking all in its line ({@value #CHARGE}) and flinging them aside.
 */
public class CloudCavalryEntity extends HeavenMonster {

    public static final double HITS = 8, BLOW = 0.10, CHARGE = 0.11, REACH = 7, WIDTH = 1.2;
    public static final int WIND_UP = 14, EVERY = 110;

    public CloudCavalryEntity(EntityType<? extends CloudCavalryEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.36, 0.7);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    @Override
    protected double[] specialShares() {
        return new double[]{CHARGE};
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.LINE, REACH, WIDTH, WIND_UP, EVERY, CHARGE,
                ParticleTypes.CLOUD, SoundEvents.HORSE_ANGRY, SoundEvents.HORSE_GALLOP),
                (e, from) -> {
                    Vec3 out = e.position().subtract(from).multiply(1, 0, 1);
                    if (out.lengthSqr() > 1e-4) e.push(out.normalize().x * 0.8, 0.35, out.normalize().z * 0.8);
                    e.hurtMarked = true;
                }).then(at -> {
                    Vec3 dir = getLookAngle().multiply(1, 0, 1);
                    if (dir.lengthSqr() > 1e-4) setDeltaMovement(dir.normalize().scale(1.4));
                    hurtMarked = true;
                });
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_BREATHE;
    }
}
