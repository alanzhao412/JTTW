package com.jttw.engine.world.fire;

import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Flaming Mountains' rules (火焰山, chapters 59–61): eight hundred li of fire where nothing grows,
 * and the fan that puts it out.
 * <ul>
 *   <li><b>The heat</b>: lit {@link BurningEarthBlock burning earth} scorches whatever stands on it,
 *       once a second, {@value #HEAT_SHARE_PERCENT}% of its health (as magma's hot floor, so armour
 *       softens it) and sets it alight for {@value #IGNITE_SECONDS} s. Fire Resistance, or being
 *       fire-proof (the mountains' own creatures are), keeps it off entirely, as it does the fire on
 *       the ground; the mountains' rock and ash, and fanned earth, are safe to stand on.</li>
 *   <li><b>The fan</b>: Princess Iron Fan's Banana-Leaf Fan (芭蕉扇) puts the mountain's fire out:
 *       one wave quenches the burning earth and the ordinary fire in its sweep, out to
 *       {@value #QUENCH_REACH} blocks ({@value #BULL_QUENCH_REACH} in the Bull Demon King's hand),
 *       for a minute ({@link #quench}). It does nothing to the True Samadhi Fire.</li>
 * </ul>
 */
public final class FireMountain {

    private FireMountain() {}

    public static final Zones.Zone ZONE = Zones.FIRE_MOUNTAIN;

    /** The heat: a share of health a second, as a percentage for the docs. */
    public static final int HEAT_SHARE_PERCENT = 4;
    public static final float HEAT_SHARE = HEAT_SHARE_PERCENT / 100f;
    /** How often the burning earth scorches, in ticks, and how long it sets one alight. */
    public static final int HEAT_EVERY = 20, IGNITE_SECONDS = 3;
    /** How far a wave of the fan puts the fire out: anyone's, the Bull Demon King's. */
    public static final double QUENCH_REACH = 12, BULL_QUENCH_REACH = 20;

    /** Whether the heat leaves {@code e} alone: fire-proof, under Fire Resistance, or a player out of play. */
    public static boolean heatproof(LivingEntity e) {
        return e.fireImmune() || e.hasEffect(MobEffects.FIRE_RESISTANCE)
                || (e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** What the burning earth takes from {@code e} at a time: a share of its health, at least 1. */
    public static float heatOf(LivingEntity e) {
        return Math.max(1f, e.getMaxHealth() * HEAT_SHARE);
    }

    /** The burning earth scorches {@code e}: a share of its health, and alight. Returns whether it was. */
    public static boolean scorch(LivingEntity e) {
        if (!e.isAlive() || heatproof(e)) return false;
        e.igniteForSeconds(IGNITE_SECONDS);
        return e.hurt(e.damageSources().hotFloor(), heatOf(e));
    }

    /**
     * One wave of the fan from {@code eye} toward {@code look}: the burning earth in its sweep (ahead,
     * within 60° either side, out to {@code reach}, from a few blocks under the fanner's feet to a
     * couple over their head) is put out for a minute, and ordinary fire there with it. The True
     * Samadhi Fire is not touched. Returns how many blocks it put out.
     */
    public static int quench(ServerLevel level, Vec3 eye, Vec3 look, double reach) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        boolean ahead = flat.lengthSqr() > 1e-4; // looking straight down, all round
        if (ahead) flat = flat.normalize();
        int r = Mth.ceil(reach), quenched = 0;
        BlockPos centre = BlockPos.containing(eye);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double hx = centre.getX() + 0.5 - eye.x + dx, hz = centre.getZ() + 0.5 - eye.z + dz;
            double d = Math.sqrt(hx * hx + hz * hz);
            if (d > reach) continue;
            if (ahead && d > 1.5 && (hx * flat.x + hz * flat.z) / d < 0.5) continue;
            for (int dy = -5; dy <= 2; dy++) {
                p.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                BlockState s = level.getBlockState(p);
                boolean out = false;
                if (BurningEarthBlock.isLit(s)) out = BurningEarthBlock.quench(level, p);
                else if (s.is(BlockTags.FIRE)) out = level.removeBlock(p, false);
                if (out) {
                    quenched++;
                    if (quenched % 3 == 0) level.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.02);
                }
            }
        }
        if (quenched > 0) level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.8f);
        return quenched;
    }
}
