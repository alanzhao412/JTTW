package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A thunder drummer (雷鼓手) of the host: war drums slung on a frame at its back, two drumsticks. It
 * keeps the host's time: when its foe comes near it raises both sticks ({@value #WIND_UP} ticks, the ring
 * marked) and beats a roll that strikes all round it within {@value #REACH} blocks ({@value #DRUM}) and
 * quickens the host near it (Speed I, five seconds).
 */
public class ThunderDrummerEntity extends HeavenMonster {

    public static final double HITS = 5, BLOW = 0.07, DRUM = 0.08, REACH = 4, QUICKEN = 10;
    public static final int WIND_UP = 18, EVERY = 140;

    public ThunderDrummerEntity(EntityType<? extends ThunderDrummerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.3, 0.55);
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
        return new double[]{DRUM};
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, DRUM,
                ParticleTypes.NOTE, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundEvents.GENERIC_EXPLODE.value()),
                (e, from) -> {
                    Vec3 out = e.position().subtract(from).multiply(1, 0, 1);
                    if (out.lengthSqr() > 1e-4) e.push(out.normalize().x * 0.5, 0.2, out.normalize().z * 0.5);
                    e.hurtMarked = true;
                }).then(at -> {
                    for (Mob m : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(QUICKEN), m -> m instanceof HeavenFolk)) {
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
                    }
                });
    }
}
