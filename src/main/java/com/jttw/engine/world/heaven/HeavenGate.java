package com.jttw.engine.world.heaven;

import com.jttw.engine.dimension.CelestialIslandsDensityFunction.Island;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.dimension.RealmArrival;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

/**
 * The way up for everyone (the Heaven Pass, {@link com.jttw.engine.item.HeavenPassItem}): Heaven is wild
 * grass for every level, so a traveller is set down on an island whose road is of their own level
 * ({@link HeavenRegions#roadLevel}, within {@value #WITHIN} either way), on the far side of the palace from
 * nowhere in particular (a direction at random), not at the Jade Palace's arrival. Sun Wukong's and the
 * Dragon's realm leaps still come down at Heaven's arrival, as ever.
 */
public final class HeavenGate {

    private HeavenGate() {}

    /** How far from one's own level the island one is set down on may be. */
    public static final int WITHIN = 2;
    /** How many directions are tried for such an island before one is laid. */
    public static final int TRIES = 24;
    /** The height a cloud is laid at where no island will do. */
    public static final int LAID_Y = 64;

    /** The island a traveller of {@code level} is set down on, or null if none was found. */
    @Nullable
    public static Island islandFor(long seed, int level, RandomSource r) {
        int lv = Mth.clamp(level, HeavenRegions.BOTTOM, HeavenRegions.TOP);
        double d = HeavenRegions.distanceFor(lv);
        for (int i = 0; i < TRIES; i++) {
            double[] at = HeavenRegions.toward(seed, r.nextDouble() * 360, d);
            Island island = HeavenRegions.islandNear(seed, at[0], at[1], null);
            if (island == null) continue;
            int road = HeavenRegions.roadLevel(seed, island.x(), island.z());
            if (Math.abs(road - lv) <= WITHIN || lv == HeavenRegions.BOTTOM && road <= HeavenRegions.BOTTOM + WITHIN) return island;
        }
        return null;
    }

    /** Where a traveller of {@code level} stands on arriving: on an island of their road's level, or a cloud laid for them there. */
    public static BlockPos landing(ServerLevel heaven, int level, RandomSource r) {
        long seed = heaven.getSeed();
        Island island = islandFor(seed, level, r);
        if (island != null) {
            BlockPos spot = RealmArrival.findSafeSpot(heaven, island.x(), island.z(), heaven.getMinBuildHeight() + 1,
                    heaven.getMaxBuildHeight() - 3, Math.max(4, island.radius() - 2));
            if (spot != null) return spot;
        }
        double[] at = HeavenRegions.toward(seed, r.nextDouble() * 360, HeavenRegions.distanceFor(level));
        BlockPos feet = new BlockPos(Mth.floor(at[0]), LAID_Y, Mth.floor(at[1]));
        layCloud(heaven, feet, 4);
        return feet;
    }

    /** A disc of cloud under {@code feet}, {@code r} across, room cleared over it. */
    public static void layCloud(ServerLevel level, BlockPos feet, int r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r + 1) continue;
            level.setBlock(p.set(feet.getX() + dx, feet.getY() - 1, feet.getZ() + dz), ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState(), Block.UPDATE_ALL);
            for (int up = 0; up < 3; up++) {
                if (!level.getBlockState(p.set(feet.getX() + dx, feet.getY() + up, feet.getZ() + dz)).isAir()) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Takes {@code p} up to Heaven, to the clouds of their level. Returns whether they went. */
    public static boolean up(ServerPlayer p) {
        ServerLevel heaven = p.server.getLevel(JTTWDimensions.HEAVEN);
        if (heaven == null) return false;
        BlockPos at = landing(heaven, Math.max(1, PlayerLevel.getLevel(p)), p.getRandom());
        p.teleportTo(heaven, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        heaven.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.02);
        return true;
    }

    /** Takes {@code p} home from Heaven, to the world below as a realm leap would. Returns whether they went. */
    public static boolean down(ServerPlayer p) {
        p.fallDistance = 0;
        return RealmArrival.travel(p, Level.OVERWORLD);
    }
}
