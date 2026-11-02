package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Wind-Following Ear (顺风耳), who with Thousand-Mile Eye watches the world from the Southern Heavenly
 * Gate: a god with great fanned ears, a hand cupped behind one. A miniboss.
 *
 * <p>His <b>echo</b> (every {@value #EVERY} ticks, a foe within {@value #REACH} blocks): he cups both ears
 * and gathers every sound in Heaven ({@value #WIND_UP} ticks, the ring marked), then lets it go at once:
 * a thunder of echoes striking all round him ({@value #ECHO} as a regular's, a miniboss's hardest: seven
 * kill), reeling them (Nausea) and throwing them back.
 */
public class WindFollowingEarEntity extends HeavenMonster {

    public static final double HITS = 13, BLOW = 0.11, ECHO = 0.12, REACH = 6;
    public static final int WIND_UP = 25, EVERY = 150;

    public WindFollowingEarEntity(EntityType<? extends WindFollowingEarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.3, 0.6).add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 48);
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
        return new double[]{ECHO};
    }

    @Override
    public boolean alwaysMiniboss() {
        return true;
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, ECHO,
                ParticleTypes.NOTE, SoundEvents.WARDEN_LISTENING, SoundEvents.WARDEN_SONIC_BOOM),
                (e, from) -> {
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    Vec3 out = e.position().subtract(from).multiply(1, 0, 1);
                    if (out.lengthSqr() > 1e-4) e.push(out.normalize().x * 0.9, 0.25, out.normalize().z * 0.9);
                    e.hurtMarked = true;
                });
    }
}
