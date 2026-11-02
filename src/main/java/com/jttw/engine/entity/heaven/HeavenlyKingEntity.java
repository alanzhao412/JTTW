package com.jttw.engine.entity.heaven;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.heaven.HeavenHostility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

/**
 * One of the Four Heavenly Kings (四大天王), the guardians of the four quarters, each at one of Heaven's
 * four gates, the head of his region (a gym's leader, its band's top and three). They stand a ring apart:
 * the farther out the gate, the lower the region, so a road of every level has a King to end it:
 * <ul>
 *   <li><b>Chiguo, of the East</b> (东方持国天王), with his pipa, at the Eastern Gate in the Outer Clouds
 *       (level 23): his <b>discord</b>, the strings swept, a ring of {@value Chiguo#REACH} round him (seven
 *       kill), reeling all in it;</li>
 *   <li><b>Zengzhang, of the South</b> (南方增长天王), with his sword, at the Southern Gate on the Cloud Road
 *       (43): the <b>blade's circle</b>, a whirling cut {@value Zengzhang#REACH} round him (six kill), flinging
 *       them back;</li>
 *   <li><b>Guangmu, of the West</b> (西方广目天王), with his serpent, at the Western Gate in the Star Palaces
 *       (63): the <b>serpent's lash</b>, a cone {@value Guangmu#REACH} before him (six kill), poisoning;</li>
 *   <li><b>Duowen, of the North</b> (北方多闻天王), with his umbrella, at the Northern Gate in the Thunder
 *       Court (83): the <b>umbrella opened</b>, darkness {@value Duowen#REACH} round him (seven kill),
 *       blinding.</li>
 * </ul>
 * Each is seventy ordinary hits of a character of his region's top, his blow six kill.
 */
public abstract class HeavenlyKingEntity extends HeavenBoss {

    protected HeavenlyKingEntity(EntityType<? extends HeavenlyKingEntity> type, Level level) {
        super(type, level);
    }

    /** Which he is: 0 East (pipa), 1 South (sword), 2 West (serpent), 3 North (umbrella). */
    public abstract int quarter();

    @Override
    public int crime() {
        return HeavenHostility.HEAVENLY_KING;
    }

    @Override
    public String crimeWhat() {
        return "you struck down a Heavenly King, a guardian of Heaven's gates";
    }

    private static BiConsumer<LivingEntity, Vec3> throwBack(double force) {
        return (e, from) -> {
            Vec3 out = e.position().subtract(from).multiply(1, 0, 1);
            if (out.lengthSqr() > 1e-4) e.push(out.normalize().x * force, 0.3, out.normalize().z * force);
            e.hurtMarked = true;
        };
    }

    /** Chiguo, King of the East, with his pipa: the Outer Clouds' head. */
    public static class Chiguo extends HeavenlyKingEntity {
        public static final double REACH = 7, DISCORD = 0.16;
        public static final int WIND_UP = 30, EVERY = 220;

        public Chiguo(EntityType<? extends Chiguo> type, Level level) {
            super(type, level);
        }

        @Override
        public int quarter() {
            return 0;
        }

        @Override
        public Zones.Zone zone() {
            return Zones.HEAVEN_OUTER_CLOUDS;
        }

        @Override
        protected AreaBlow makeBlow() {
            return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, DISCORD,
                    ParticleTypes.NOTE, SoundEvents.NOTE_BLOCK_HARP.value(), SoundEvents.NOTE_BLOCK_GUITAR.value()),
                    (e, from) -> {
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    });
        }
    }

    /** Zengzhang, King of the South, with his sword: the Cloud Road's head. */
    public static class Zengzhang extends HeavenlyKingEntity {
        public static final double REACH = 5.5, CIRCLE = 0.18;
        public static final int WIND_UP = 25, EVERY = 200;

        public Zengzhang(EntityType<? extends Zengzhang> type, Level level) {
            super(type, level);
        }

        @Override
        public int quarter() {
            return 1;
        }

        @Override
        public Zones.Zone zone() {
            return Zones.HEAVEN_CLOUD_ROAD;
        }

        @Override
        protected AreaBlow makeBlow() {
            return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, CIRCLE,
                    ParticleTypes.SWEEP_ATTACK, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundEvents.PLAYER_ATTACK_SWEEP), throwBack(1.0));
        }
    }

    /** Guangmu, King of the West, with his serpent: the Star Palaces' head. */
    public static class Guangmu extends HeavenlyKingEntity {
        public static final double REACH = 9, HALF_ANGLE = 35, LASH = 0.17;
        public static final int WIND_UP = 28, EVERY = 200;

        public Guangmu(EntityType<? extends Guangmu> type, Level level) {
            super(type, level);
        }

        @Override
        public int quarter() {
            return 2;
        }

        @Override
        public Zones.Zone zone() {
            return Zones.HEAVEN_STAR_PALACES;
        }

        @Override
        protected AreaBlow makeBlow() {
            return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.CONE, REACH, HALF_ANGLE, WIND_UP, EVERY, LASH,
                    new DustParticleOptions(new Vector3f(0.3f, 0.85f, 0.4f), 1.3f), SoundEvents.CAT_HISS, SoundEvents.EVOKER_FANGS_ATTACK),
                    (e, from) -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0)));
        }
    }

    /** Duowen, King of the North, with his umbrella: the Thunder Court's head. */
    public static class Duowen extends HeavenlyKingEntity {
        public static final double REACH = 8, DARKNESS = 0.16;
        public static final int WIND_UP = 30, EVERY = 220;

        public Duowen(EntityType<? extends Duowen> type, Level level) {
            super(type, level);
        }

        @Override
        public int quarter() {
            return 3;
        }

        @Override
        public Zones.Zone zone() {
            return Zones.HEAVEN_THUNDER_COURT;
        }

        @Override
        protected AreaBlow makeBlow() {
            return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, DARKNESS,
                    ParticleTypes.SQUID_INK, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundEvents.WITHER_SHOOT),
                    (e, from) -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)));
        }
    }
}
