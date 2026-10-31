package com.jttw.engine.world.spider;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The shape of Silk-Web Ridge's country (盘丝岭), worked out from the seed and the region alone, so
 * every chunk agrees on it whichever is made first (pure: unit-tested). Local coordinates are
 * block offsets from the region's centre, +z south.
 * <ul>
 *   <li><b>the valley</b>: within {@value #VALLEY} blocks of the centre the land is brought down (or
 *       up) to the valley's floor {@code F} ({@link #floor}), rolling a little, then back to its own
 *       over {@value #VALLEY_BLEND} more;</li>
 *   <li><b>the ridge</b> ({@link #ridge}): a long, craggy ridge some 400 blocks from end to end across
 *       the valley, east to west, its crest a little north of the centre, steep on the south where
 *       the cave's mouth is, long on the north; four peaks along it, the highest some
 *       {@value #RIDGE_HIGH} and more over the valley; under it, the Spider Cave
 *       ({@link SpiderCavePlan}), always under {@value #COVER} blocks of rock at least;</li>
 *   <li><b>the level grounds</b> ({@link SilkRidgeWorks}): the thatched cottage's yard at the
 *       ridge's southern foot, before the cave's mouth; the Purifying Spring to the south-east; the
 *       Yellow Flower Temple's terrace to the south-west;</li>
 *   <li><b>the paths</b> ({@link #onPath}): from the valley's south edge up to the cottage, with a
 *       branch to the spring and one to the temple's gate.</li>
 * </ul>
 */
public final class SilkRidgeShape {

    private SilkRidgeShape() {}

    public static final int VALLEY = 185, VALLEY_BLEND = 30;
    public static final int RIDGE_HIGH = 62, RIDGE_SOUTH = 58, RIDGE_NORTH = 120, RIDGE_LONG = 120, RIDGE_END = 200;
    /** Rock kept over the cave's vault. */
    public static final int COVER = 5;
    /** The valley's floor is never lower than the sea and this, nor higher than the land's top less this. */
    public static final int ABOVE_SEA = 4, UNDER_TOP = 140;
    public static final double PATH_HALF = 1.6;

    /** The peaks along the crest: x, z offset from the crest, how high over the ridge, how wide. */
    private static final double[][] PEAKS = {{-95, -18, 28, 30}, {-28, -14, 34, 32}, {48, -24, 26, 28}, {122, -10, 18, 24}};

    /** The paths, as polylines of local (x, z). */
    public static final int[][][] PATHS = {
            {{0, 215}, {0, 150}, {2, 110}, {0, SilkRidgeWorks.COTTAGE_Z + SilkRidgeWorks.YARD_S}},
            {{2, 125}, {40, 118}, {78, 112}, {SilkRidgeWorks.SPRING_X, SilkRidgeWorks.SPRING_Z + 18}},
            {{0, 150}, {-50, 150}, {-90, 140}, {SilkRidgeWorks.TEMPLE_X, SilkRidgeWorks.TEMPLE_Z + SilkRidgeWorks.TEMPLE_S + 3}}};

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0x5B1DC0FEL));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** The crest's line: its z at x. */
    public static double crest(long seed, int lx) {
        return -12 + 7 * Math.sin(lx / 53.0) + 4 * noise(seed).getValue(lx / 70.0, 3.7);
    }

    /** How high the ridge stands over the valley floor at local (lx, lz) (0 off it). */
    public static double ridge(long seed, int x, int z, int lx, int lz) {
        double d = lz - crest(seed, lx);
        double w = d > 0 ? RIDGE_SOUTH : RIDGE_NORTH;
        double across = Math.abs(d) / w;
        if (across >= 1) return 0;
        double along = 1 - smooth(RIDGE_LONG, RIDGE_END, Math.abs(lx));
        if (along <= 0) return 0;
        double profile = Math.pow(1 - across * across, 1.3) * along;
        SimplexNoise n = noise(seed);
        double crag = 0.78 + 0.32 * (1 - Math.abs(n.getValue(x / 38.0, z / 38.0))) + 0.12 * n.getValue(x / 11.0 + 4.1, z / 11.0 - 2.3);
        double h = RIDGE_HIGH * profile * crag;
        for (double[] pk : PEAKS) {
            double px = pk[0], pz = crest(seed, (int) pk[0]) + pk[1];
            double r = Math.hypot(lx - px, lz - pz) / pk[3];
            if (r < 1) h += pk[2] * Math.pow(1 - r * r, 2) * (0.85 + 0.25 * n.getValue(x / 7.0, z / 7.0));
        }
        return h;
    }

    /** How much the valley takes over the land's own height at local (lx, lz): 1 inside it, falling to 0 past its edge. */
    public static double valley(int lx, int lz) {
        return 1 - smooth(VALLEY, VALLEY + VALLEY_BLEND, Math.hypot(lx, lz));
    }

    /** The valley floor's gentle roll at (x, z), a few blocks either way. */
    public static double roll(long seed, int x, int z) {
        SimplexNoise n = noise(seed);
        return 2.5 * n.getValue(x / 48.0 + 11.3, z / 48.0 - 7.7) + 1.2 * n.getValue(x / 13.0 - 1.9, z / 13.0 + 5.5);
    }

    /** How far (lx, lz) is from the nearest path's line. */
    public static double pathDistance(int lx, int lz) {
        double best = Double.MAX_VALUE;
        for (int[][] path : PATHS) {
            for (int i = 0; i + 1 < path.length; i++) {
                double ax = path[i][0], az = path[i][1], bx = path[i + 1][0], bz = path[i + 1][1];
                double dx = bx - ax, dz = bz - az, len2 = dx * dx + dz * dz;
                double t = Mth.clamp(((lx - ax) * dx + (lz - az) * dz) / len2, 0, 1);
                best = Math.min(best, Math.hypot(lx - ax - dx * t, lz - az - dz * t));
            }
        }
        return best;
    }

    /** Whether (lx, lz) is on a path. */
    public static boolean onPath(int lx, int lz) {
        return pathDistance(lx, lz) <= PATH_HALF;
    }

    /**
     * The height the ground's top block at (x, z), local (lx, lz), is brought to: the land's own
     * ({@code natural}) far out; the valley's floor ({@code floor - 1}, its standing level less one)
     * rolling a little, the ridge over it; the level grounds flat; the paths smoothed; and over the
     * cave, rock enough to keep its vault.
     */
    public static int groundAt(long seed, int x, int z, int lx, int lz, int natural, int floor) {
        double v = valley(lx, lz);
        if (v <= 0) return natural;
        double ground = floor - 1 + roll(seed, x, z) * (1 - SilkRidgeWorks.levelShare(lx, lz));
        double ridge = ridge(seed, x, z, lx, lz);
        // the paths keep low and smooth: no ridge is on them, and they wear the roll down
        double path = 1 - smooth(PATH_HALF, PATH_HALF + 4, pathDistance(lx, lz));
        ground += ridge * (1 - SilkRidgeWorks.levelShare(lx, lz)) * (1 - path);
        ground -= roll(seed, x, z) * path * 0.7;
        double h = natural + (ground - natural) * v;
        int roof = SpiderCavePlan.roof(lx, lz);
        if (roof >= 0) h = Math.max(h, floor + roof + COVER);
        return (int) Math.round(h);
    }

    /** The cached valley floor of each region, by its centre. */
    private static final Map<Long, Integer> FLOORS = new ConcurrentHashMap<>();

    /**
     * The valley's floor (its standing level: the first block over the ground) in the region centred
     * at (cx, cz): the land's broad height ({@code land}) round the valley, on average, at the centre
     * and at eight points {@value #VALLEY} out, and one more; between the sea's level and a little
     * ({@code minFloor}) and well under the land's top ({@code maxFloor}). Worked out once a region.
     */
    public static int floor(long seed, int cx, int cz, IntBinaryOperator land, int minFloor, int maxFloor) {
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)), k -> {
            int sum = land.applyAsInt(cx, cz), r = VALLEY - 40;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                sum += land.applyAsInt(cx + (int) Math.round(Math.cos(a) * r), cz + (int) Math.round(Math.sin(a) * r));
            }
            return Mth.clamp(Math.round(sum / 9f) + 1, minFloor, maxFloor);
        });
    }

    /** How patchy the valley's ground is at (x, z), about -1..1: high, dark (podzol); low, mossy. */
    public static double patch(long seed, int x, int z) {
        SimplexNoise n = noise(seed);
        return n.getValue(x / 17.0 + 31.7, z / 17.0 - 12.9) * 0.8 + n.getValue(x / 5.0 - 3.3, z / 5.0 + 9.1) * 0.25;
    }
}
