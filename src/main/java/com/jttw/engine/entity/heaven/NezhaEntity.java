package com.jttw.engine.entity.heaven;

import com.jttw.engine.world.Zones;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Nezha, the Third Lotus Prince (哪吒三太子), Li Jing's son and Heaven's champion: a youth on his
 * Wind-Fire Wheels, the Universe Ring (乾坤圈) on his arm, the red Huntian sash streaming about him, the
 * Fire-Tipped Spear in his hand. A lesser great name of the Jade Palace, level 102.
 *
 * <p>His <b>wheels' blaze</b> (every {@value #EVERY} ticks, a foe within {@value #REACH} blocks): his wheels
 * spin up and roar, a ring of fire marked round him ({@value #WIND_UP} ticks); then the Samadhi fire bursts
 * out from them, striking all round him ({@value #BLAZE} of their health: six kill) and setting them alight.
 * A player who is Nezha he will not fight ({@link HeavenChampion}).
 */
public class NezhaEntity extends HeavenChampion {

    public static final double REACH = 6, BLAZE = 0.17;
    public static final int WIND_UP = 25, EVERY = 180;

    public NezhaEntity(EntityType<? extends NezhaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributesFor(Zones.HEAVEN_JADE_PALACE.maxLevel(), 0.34);
    }

    @Override
    public boolean isHimself(Player p) {
        return NEZHA.test(p);
    }

    @Override
    protected Component refusal() {
        return Component.literal("§6Nezha lowers his spear: §e\"I will not raise the Ring against my own self.\"");
    }

    @Override
    public String crimeWhat() {
        return "you struck down Nezha, the Third Lotus Prince";
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, BLAZE,
                ParticleTypes.FLAME, SoundEvents.BLAZE_AMBIENT, SoundEvents.FIRECHARGE_USE),
                (e, from) -> e.igniteForSeconds(4));
    }
}
