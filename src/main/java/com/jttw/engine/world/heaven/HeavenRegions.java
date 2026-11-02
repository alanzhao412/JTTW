package com.jttw.engine.world.heaven;

import com.jttw.engine.dimension.CelestialIslandsDensityFunction;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction.Island;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Heaven is wild grass: one long road of every level, walked (or flown) inward. Its host's level is
 * read off how far a place is from the Jade Palace ({@link HeavenFeatures#palace}): {@value #TOP} at the
 * palace, one less every {@value #BLOCKS_PER_LEVEL} blocks out, down to {@value #BOTTOM} on the outermost
 * clouds ({@link #roadLevel}). The road falls in five regions of twenty levels ({@link Zones#HEAVEN_REGIONS}):
 *
 * <pre>
 *   region                 levels   from the Jade Palace   its head (top and three)
 *   the Outer Clouds        1-20     2,880 blocks and out   King Chiguo of the East, 23
 *   the Cloud Road         21-40     2,160-2,880            King Zengzhang of the South, 43
 *   the Star Palaces       41-60     1,440-2,160            King Guangmu of the West, 63
 *   the Thunder Court      61-80       720-1,440            King Duowen of the North, 83
 *   the Jade Palace        81-100        0-720              Li Jing 103 (Nezha, Erlang Shen 102)
 * </pre>
 *
 * Its regulars roll a level either way of the road's where they stand, never out of their region's band
 * ({@link #regularLevel}); its captains three to five over ({@link #captainLevel}), its minibosses five
 * ({@link #minibossLevel}), neither more than one over the band's top.
 */
public final class HeavenRegions {

    private HeavenRegions() {}

    /** The road's level at the Jade Palace, and on the outermost clouds. */
    public static final int TOP = 100, BOTTOM = 1;
    /** How many blocks out from the Jade Palace the road falls a level. */
    public static final int BLOCKS_PER_LEVEL = 36;
    /** How many levels a captain stands over the road (one either way), a miniboss; and how far either may stand over its band's top. */
    public static final int CAPTAIN_OVER = 4, MINIBOSS_OVER = 5, ABOVE_BAND = 1;

    /** Where the road's levels are measured from: the Jade Palace. */
    public static BlockPos palace(long seed) {
        return HeavenFeatures.palace(seed);
    }

    /** How far (x, z) is from the Jade Palace, flat. */
    public static double fromPalace(long seed, double x, double z) {
        BlockPos p = palace(seed);
        return Math.hypot(x - (p.getX() + 0.5), z - (p.getZ() + 0.5));
    }

    /** The road's level at (x, z): {@value #TOP} at the palace, one less every {@value #BLOCKS_PER_LEVEL} blocks out. */
    public static int roadLevel(long seed, double x, double z) {
        return levelAtDistance(fromPalace(seed, x, z));
    }

    /** The road's level {@code d} blocks from the palace. */
    public static int levelAtDistance(double d) {
        return Mth.clamp(TOP - (int) Math.floor(d / BLOCKS_PER_LEVEL), BOTTOM, TOP);
    }

    /** How far out from the palace the road stands at {@code level}: the middle of that level's ring. */
    public static double distanceFor(int level) {
        return (TOP - Mth.clamp(level, BOTTOM, TOP) + 0.5) * BLOCKS_PER_LEVEL;
    }

    /** The region whose band holds {@code level} (the Outer Clouds below it, the Jade Palace past it). */
    public static Zones.Zone regionOf(int level) {
        for (Zones.Zone z : Zones.HEAVEN_REGIONS) if (level <= z.maxLevel()) return z;
        return Zones.HEAVEN_REGIONS.get(Zones.HEAVEN_REGIONS.size() - 1);
    }

    /** The region at (x, z). */
    public static Zones.Zone regionAt(long seed, double x, double z) {
        return regionOf(roadLevel(seed, x, z));
    }

    /** Which region (0, the Outer Clouds, to 4, the Jade Palace) a zone is, or -1. */
    public static int indexOf(Zones.Zone region) {
        return Zones.HEAVEN_REGIONS.indexOf(region);
    }

    /** A regular's level on a road of {@code road}: one either way of it, within its region's band. */
    public static int regularLevel(int road, RandomSource r) {
        Zones.Zone z = regionOf(road);
        return Mth.clamp(road - 1 + r.nextInt(3), z.minLevel(), z.maxLevel());
    }

    /** A captain's level on a road of {@code road}: three to five over it, never more than one over its region's top. */
    public static int captainLevel(int road, RandomSource r) {
        Zones.Zone z = regionOf(road);
        return Math.min(z.maxLevel() + ABOVE_BAND, Math.max(z.minLevel(), road) + CAPTAIN_OVER - 1 + r.nextInt(3));
    }

    /** A miniboss's level on a road of {@code road}: five over it, never more than one over its region's top. */
    public static int minibossLevel(int road) {
        Zones.Zone z = regionOf(road);
        return Math.min(z.maxLevel() + ABOVE_BAND, Math.max(z.minLevel(), road) + MINIBOSS_OVER);
    }

    /** Heaven's region names, outermost first. */
    public static final List<String> NAMES = List.of("the Outer Clouds", "the Cloud Road", "the Star Palaces", "the Thunder Court", "the Jade Palace");

    public static String nameOf(Zones.Zone region) {
        int i = indexOf(region);
        return i < 0 ? "Heaven" : NAMES.get(i);
    }

    // ── Islands ─────────────────────────────────────────────────────────

    /**
     * The island nearest (x, z) whose heart lies in {@code region} (within a cell either way of the
     * point's), or null.
     */
    @Nullable
    public static Island islandNear(long seed, double x, double z, @Nullable Zones.Zone region) {
        int cell = CelestialIslandsDensityFunction.CELL;
        int cx = Math.floorDiv((int) Math.floor(x), cell), cz = Math.floorDiv((int) Math.floor(z), cell);
        Island best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            Island i = CelestialIslandsDensityFunction.island(cx + dx, cz + dz);
            if (i == null) continue;
            if (region != null && regionAt(seed, i.x(), i.z()) != region) continue;
            double d = Math.hypot(i.x() - x, i.z() - z);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** The point {@code d} blocks from the palace toward {@code degrees} (0 east, 90 south), as {x, z}. */
    public static double[] toward(long seed, double degrees, double d) {
        BlockPos p = palace(seed);
        double a = Math.toRadians(degrees);
        return new double[]{p.getX() + 0.5 + Math.cos(a) * d, p.getZ() + 0.5 + Math.sin(a) * d};
    }
}
