package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Thunder Lord (雷公), the god of thunder of the Thunder Court: a bird's beak and claws, bat-like
 * wings, a ring of drums about his back, a mallet in one hand and a chisel in the other. A miniboss.
 *
 * <p>His <b>thunderbolt</b> (every {@value #EVERY} ticks): he spreads his wings and lifts mallet and chisel,
 * and a ring of crackling light marks the ground under each of his foes within {@value #REACH} blocks
 * ({@value #WIND_UP} ticks); then he strikes the chisel and the thunder falls on every ring ({@value #BOLT}
 * as a regular's, a miniboss's hardest: seven kill), numbing those it strikes (Slowness II).
 */
public class ThunderLordEntity extends HeavenMonster {

    public static final double HITS = 15, BLOW = 0.11, BOLT = 0.12, REACH = 14, RING = 3;
    public static final int WIND_UP = 30, EVERY = 150;

    public ThunderLordEntity(EntityType<? extends ThunderLordEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.3, 0.75);
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
        return new double[]{BOLT};
    }

    @Override
    public boolean alwaysMiniboss() {
        return true;
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AT_FOES, REACH, RING, WIND_UP, EVERY, BOLT,
                ParticleTypes.ELECTRIC_SPARK, SoundEvents.TRIDENT_THUNDER.value(), SoundEvents.LIGHTNING_BOLT_IMPACT),
                (e, from) -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)))
                .then(marks -> {
                    if (!(level() instanceof ServerLevel server)) return;
                    for (Vec3 m : marks) {
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                        if (bolt == null) continue;
                        bolt.moveTo(m.x, m.y, m.z);
                        bolt.setVisualOnly(true);
                        server.addFreshEntity(bolt);
                    }
                });
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }
}
