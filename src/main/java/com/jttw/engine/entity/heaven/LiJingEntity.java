package com.jttw.engine.entity.heaven;

import com.jttw.engine.world.Stun;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.heaven.HeavenHostility;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Li Jing, the Pagoda-Bearing Heavenly King (托塔李天王), commander of Heaven's armies against the Monkey
 * King (chapters 4-6): the head of the Jade Palace, Heaven's highest, level 103, a golden pagoda held up
 * in his left hand, a sword in his right. Seventy ordinary hits of a level-100 character; his sword six kill.
 *
 * <p><b>The pagoda descends</b> (every {@value #EVERY} ticks): he raises the pagoda high and its golden
 * shadow marks a ring under each of his foes within {@value #REACH} blocks ({@value #WIND_UP} ticks); then
 * the pagoda's image comes down on every ring, striking all in it ({@value #PAGODA} of their health: six
 * kill) and holding them under it, pinned, for {@value #PINNED} ticks. Step out of its shadow.
 */
public class LiJingEntity extends HeavenBoss {

    public static final double REACH = 14, RING = 3.5, PAGODA = 0.18;
    public static final int WIND_UP = 30, EVERY = 200, PINNED = 30;

    public LiJingEntity(EntityType<? extends LiJingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributesFor(Zones.HEAVEN_JADE_PALACE.maxLevel(), 0.27);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.HEAVEN_JADE_PALACE;
    }

    @Override
    public int crime() {
        return HeavenHostility.LI_JING;
    }

    @Override
    public String crimeWhat() {
        return "you struck down Li Jing, the Pagoda-Bearing Heavenly King";
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AT_FOES, REACH, RING, WIND_UP, EVERY, PAGODA,
                new DustParticleOptions(new Vector3f(1f, 0.8f, 0.2f), 1.6f), SoundEvents.BEACON_ACTIVATE, SoundEvents.ANVIL_LAND),
                (e, from) -> Stun.pin(e, PINNED, this))
                .then(marks -> {
                    if (!(level() instanceof ServerLevel server)) return;
                    for (Vec3 m : marks) {
                        for (int i = 0; i < 10; i++) {
                            double a = i * Math.PI * 2 / 10;
                            server.sendParticles(ParticleTypes.WAX_OFF, m.x + Math.cos(a) * RING, m.y + 0.3, m.z + Math.sin(a) * RING, 3, 0, 1.5, 0, 0);
                        }
                    }
                });
    }
}
