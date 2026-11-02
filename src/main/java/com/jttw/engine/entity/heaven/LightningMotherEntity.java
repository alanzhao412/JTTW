package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * The Lightning Mother (电母), goddess of lightning of the Thunder Court, the Thunder Lord's consort: a
 * lady in flowing robes and ribbons with a round mirror in each hand. A miniboss.
 *
 * <p>Her <b>mirror-flash</b> (every {@value #EVERY} ticks): she holds both mirrors up, light gathering in
 * them, a cone of {@value #REACH} blocks before her marked ({@value #WIND_UP} ticks); then she turns them
 * on it and lightning flashes from them, striking all in it ({@value #FLASH} as a regular's, a miniboss's
 * hardest: seven kill) and dazzling them (Blindness, two seconds).
 */
public class LightningMotherEntity extends HeavenMonster {

    public static final double HITS = 13, BLOW = 0.108, FLASH = 0.12, REACH = 11, HALF_ANGLE = 45;
    public static final int WIND_UP = 25, EVERY = 140;

    public LightningMotherEntity(EntityType<? extends LightningMotherEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.3, 0.7);
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
        return new double[]{FLASH};
    }

    @Override
    public boolean alwaysMiniboss() {
        return true;
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.CONE, REACH, HALF_ANGLE, WIND_UP, EVERY, FLASH,
                ParticleTypes.ELECTRIC_SPARK, SoundEvents.BEACON_POWER_SELECT, SoundEvents.LIGHTNING_BOLT_THUNDER),
                (e, from) -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0)));
    }
}
