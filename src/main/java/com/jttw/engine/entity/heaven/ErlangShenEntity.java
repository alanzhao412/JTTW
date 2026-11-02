package com.jttw.engine.entity.heaven;

import com.jttw.engine.world.Zones;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Erlang Shen (二郎神), the Illustrious Sage True Lord, who alone fought the Monkey King to a standstill
 * and took him (chapter 6): silver armour, the third eye in his brow, the Three-Pointed Two-Edged Spear,
 * his hound Xiaotian (哮天犬) at his heel. A lesser great name of the Jade Palace, level 102.
 *
 * <p><b>The third eye's light</b> (every {@value #EVERY} ticks): his third eye opens and burns, a cone of
 * {@value #REACH} blocks before him marked ({@value #WIND_UP} ticks); then its light lances out over it,
 * striking all in it ({@value #LIGHT} of their health: six kill) and showing them up (Glowing). A player who
 * is Erlang Shen he will not fight ({@link HeavenChampion}).
 */
public class ErlangShenEntity extends HeavenChampion {

    public static final double REACH = 14, HALF_ANGLE = 25, LIGHT = 0.18;
    public static final int WIND_UP = 30, EVERY = 200;

    public ErlangShenEntity(EntityType<? extends ErlangShenEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributesFor(Zones.HEAVEN_JADE_PALACE.maxLevel(), 0.3);
    }

    @Override
    public boolean isHimself(Player p) {
        return ERLANG.test(p);
    }

    @Override
    protected Component refusal() {
        return Component.literal("§6Erlang Shen regards you with his third eye: §e\"I do not cross blades with myself.\"");
    }

    @Override
    public String crimeWhat() {
        return "you struck down Erlang Shen, the Illustrious Sage";
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.CONE, REACH, HALF_ANGLE, WIND_UP, EVERY, LIGHT,
                new DustParticleOptions(new Vector3f(1f, 0.95f, 0.6f), 1.5f), SoundEvents.BEACON_POWER_SELECT, SoundEvents.GUARDIAN_ATTACK),
                (e, from) -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0)));
    }
}
