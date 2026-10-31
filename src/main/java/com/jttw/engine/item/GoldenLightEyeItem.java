package com.jttw.engine.item;

import com.jttw.engine.world.Stun;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * The Golden-Light Eye (金光眼): the brightest of the Hundred-Eyed Demon Lord's thousand eyes, taken
 * from him. Raised, it flashes his golden light: every hostile creature within {@value #RANGE}
 * blocks of its bearer is dazzled (blinded, and held where it stands for {@value #HOLD} ticks).
 * Then it must dim for {@value #REST} ticks.
 */
public class GoldenLightEyeItem extends Item {

    public static final double RANGE = 8;
    public static final int HOLD = 40, REST = 400;

    public GoldenLightEyeItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    /** Flashes the light from {@code bearer}: every hostile creature near dazzled. Returns them. */
    public static List<LivingEntity> flash(Player bearer) {
        List<LivingEntity> dazzled = bearer.level().getEntitiesOfClass(LivingEntity.class, bearer.getBoundingBox().inflate(RANGE),
                e -> e != bearer && e.isAlive() && e instanceof Enemy && e.distanceToSqr(bearer) <= RANGE * RANGE);
        for (LivingEntity e : dazzled) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, HOLD * 2, 0), bearer);
            Stun.pin(e, HOLD, bearer);
        }
        bearer.level().playSound(null, bearer.getX(), bearer.getY(), bearer.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.6f);
        if (bearer.level() instanceof ServerLevel server) {
            DustParticleOptions gold = new DustParticleOptions(new Vector3f(1f, 0.82f, 0.25f), 2f);
            for (int k = 0; k < 32; k++) {
                double a = k * Math.PI / 16;
                for (double d = 1; d <= RANGE; d += 2) {
                    server.sendParticles(gold, bearer.getX() + Math.cos(a) * d, bearer.getY(0.6), bearer.getZ() + Math.sin(a) * d, 1, 0.1, 0.3, 0.1, 0);
                }
            }
            server.sendParticles(ParticleTypes.END_ROD, bearer.getX(), bearer.getEyeY(), bearer.getZ(), 30, 0.3, 0.3, 0.3, 0.2);
        }
        return dazzled;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) flash(player);
        player.getCooldowns().addCooldown(this, REST);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
