package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Thousand-Mile Eye (千里眼), who with Wind-Following Ear watches the world below from the Southern
 * Heavenly Gate and saw the stone monkey born (chapter 1): a god with great bulging eyes, one hand shading
 * them. A miniboss.
 *
 * <p>His <b>piercing gaze</b> (every {@value #EVERY} ticks): he fixes his eyes on his foe and a line
 * {@value #REACH} blocks long is marked toward them ({@value #WIND_UP} ticks); then his gaze lances down
 * it, striking all in it ({@value #GAZE} as a regular's, a miniboss's hardest: seven kill), holding them
 * (Slowness II) and showing them up (Glowing). Step out of its line.
 */
public class ThousandMileEyeEntity extends HeavenMonster {

    public static final double HITS = 12, BLOW = 0.11, GAZE = 0.12, REACH = 16, WIDTH = 1.4;
    public static final int WIND_UP = 28, EVERY = 140;

    public ThousandMileEyeEntity(EntityType<? extends ThousandMileEyeEntity> type, Level level) {
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
        return new double[]{GAZE};
    }

    @Override
    public boolean alwaysMiniboss() {
        return true;
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.LINE, REACH, WIDTH, WIND_UP, EVERY, GAZE,
                new DustParticleOptions(new Vector3f(1f, 0.85f, 0.2f), 1.4f), SoundEvents.ENDER_EYE_LAUNCH, SoundEvents.GUARDIAN_ATTACK),
                (e, from) -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                });
    }
}
