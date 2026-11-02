package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One of the Nine Luminaries (九曜星君), the star lords of the sun, the moon, the five planets and the two
 * shadow stars, minibosses each at his own post in Heaven. One body for the nine, robed and crowned in
 * his star's colour, his star's emblem held up ({@link #LUMINARIES}).
 *
 * <p>His <b>starfall</b> (every {@value #EVERY} ticks): he raises his emblem and a ring of starlight
 * marks the ground under each of his foes within {@value #REACH} blocks ({@value #WIND_UP} ticks); then a
 * star falls on each ring, striking all in it ({@value #STARFALL} as a regular's, a miniboss's hardest:
 * seven kill) and, by his star, more besides (the sun's sets afire, the moon's and Rahu's darken the eyes,
 * Saturn's and Ketu's slow, Mars's burns, Venus's cuts deep, Jupiter's and Mercury's fling back).
 */
public class StarLordEntity extends HeavenMonster {

    public static final double HITS = 14, BLOW = 0.11, STARFALL = 0.12, REACH = 12, RING = 2.5;
    public static final int WIND_UP = 30, EVERY = 160;

    /** The nine: the Sun, the Moon, Wood (Jupiter), Fire (Mars), Earth (Saturn), Metal (Venus), Water (Mercury), Rahu, Ketu. */
    public static final String[] LUMINARIES = {"sun", "moon", "jupiter", "mars", "saturn", "venus", "mercury", "rahu", "ketu"};
    public static final int SUN = 0, MOON = 1, JUPITER = 2, MARS = 3, SATURN = 4, VENUS = 5, MERCURY = 6, RAHU = 7, KETU = 8;

    public StarLordEntity(EntityType<? extends StarLordEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.28, 0.55).add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.6);
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
        return new double[]{STARFALL};
    }

    @Override
    public boolean alwaysMiniboss() {
        return true;
    }

    @Override
    public int variants() {
        return LUMINARIES.length;
    }

    @Override
    protected Component kindName() {
        return Component.translatable("entity.jttw_engine.star_lord." + LUMINARIES[variant()]);
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AT_FOES, REACH, RING, WIND_UP, EVERY, STARFALL,
                ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST),
                (e, from) -> {
                    switch (variant()) {
                        case SUN, MARS -> e.igniteForSeconds(4);
                        case MOON, RAHU -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        case SATURN, KETU -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                        case VENUS -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                        default -> {
                            Vec3 out = e.position().subtract(from).multiply(1, 0, 1);
                            if (out.lengthSqr() > 1e-4) e.push(out.normalize().x * 0.7, 0.3, out.normalize().z * 0.7);
                            e.hurtMarked = true;
                        }
                    }
                }).then(marks -> {
                    if (!(level() instanceof ServerLevel server)) return;
                    for (Vec3 m : marks) {
                        for (int i = 0; i < 12; i++) server.sendParticles(ParticleTypes.END_ROD, m.x, m.y + 6 - i * 0.5, m.z, 2, 0.1, 0.1, 0.1, 0);
                        server.sendParticles(ParticleTypes.FLASH, m.x, m.y + 0.3, m.z, 1, 0, 0, 0, 0);
                    }
                });
    }
}
