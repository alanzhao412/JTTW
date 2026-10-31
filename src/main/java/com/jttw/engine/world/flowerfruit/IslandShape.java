package com.jttw.engine.world.flowerfruit;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shape of Flower-Fruit Mountain (花果山), worked out from the seed and a column's place in its
 * region alone, so every chunk agrees on it whichever is made first (pure: unit-tested). Local
 * coordinates are block offsets from the region's centre; the waterfall faces south (+z).
 * <ul>
 *   <li><b>the island</b> ({@link #top}): a towering mountain raised out of the sea, its coast some
 *       {@value #COAST} blocks out (wandering), its flanks climbing in shelves (the peach orchards'
 *       terraces) and spurs to a sharp peak {@value #SUMMIT_Y} high, a little north of the centre,
 *       where the immortal stone egg stands ({@link #egg}); round it the sea, shoaling up to its
 *       beaches;</li>
 *   <li><b>the rock pillars</b> ({@link #pillarAt}): fantastic sheer columns of stone standing out
 *       of its flanks and out of the sea round it, pines on their heads;</li>
 *   <li><b>the waterfall</b>: on its south face a great cliff ({@link #CLIFF_Z}), a river running down a
 *       gorge to its lip ({@link #LIP_Y}) and falling as a sheet ({@link #curtain}) into a pool in the
 *       basin at its foot, {@link #BASIN_Y}, the pool's stream running on south to the sea; behind the
 *       sheet, in the cliff, the mouth of the Water Curtain Cave ({@link CavePlan}).</li>
 * </ul>
 */
public final class IslandShape {

    private IslandShape() {}

    public static final Zones.Zone ZONE = Zones.FLOWER_FRUIT;

    /** The sea's level the island rises from. */
    public static final int SEA = 63;
    /** The coast's distance from the centre, on the average, and how far it wanders. */
    public static final int COAST = 222, COAST_WANDER = 26;
    /** How high the mountain's body rises over the sea at its middle, and its peak over that. */
    public static final int RISE = 128, PEAK = 44, PEAK_RADIUS = 80;
    /** The summit's place (north of the centre) and the height of its ground. */
    public static final int SUMMIT_X = 0, SUMMIT_Z = -28, SUMMIT_FLAT = 10;
    public static final int SUMMIT_Y = SEA + 1 + RISE + PEAK - 6;
    /** The terraces: shelves this high a step. */
    public static final int TERRACE = 6;

    /** The basin at the foot of the waterfall: its floor (the top block), and the cliff's line. */
    public static final int BASIN_Y = SEA + 5, CLIFF_Z = 70;
    /** The basin's half-width and how far south it runs before it climbs out to the land. */
    public static final int BASIN_HALF = 40, BASIN_END = 150;
    /** The waterfall's lip: the river's water at it; the river's half-width. */
    public static final int LIP_Y = 128, RIVER_HALF = 9;
    /** The sheet of falling water: its half-width, how far out from the cliff it falls. */
    public static final int FALL_HALF = 10, FALL_Z = CLIFF_Z + 3;
    /** The overhang the river runs out over to its lip. */
    public static final int LIP_OUT = 2;
    /** The gorge the river comes down: from the spring this far north of the cliff. */
    public static final int SPRING_Z = CLIFF_Z - 58, SPRING_R = 11;
    /** The pool at the waterfall's foot: its middle and radii. */
    public static final int POOL_Z = CLIFF_Z + 13, POOL_RX = 22, POOL_RZ = 15, POOL_DEEP = 5;

    // ── Noise ───────────────────────────────────────────────────────────

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0xF10E7F2BA4L));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    private static long hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 32);
    }

    private static double unit(long seed, int a, int b, int salt) {
        return (hash(seed, a, b, salt) >>> 11) * 0x1.0p-53;
    }

    // ── The island ──────────────────────────────────────────────────────

    /** The coast's distance from the centre in the direction of local (lx, lz). */
    public static double coast(long seed, int lx, int lz) {
        double a = Math.atan2(lz, lx);
        double n = noise(seed).getValue(Math.cos(a) * 1.4 + 11.3, Math.sin(a) * 1.4 - 4.1) * 0.7
                + noise(seed).getValue(Math.cos(a) * 3.1 - 7.7, Math.sin(a) * 3.1 + 2.9) * 0.3;
        return COAST + COAST_WANDER * n;
    }

    /** How far out toward the coast (lx, lz) lies: 0 at the centre, 1 on the coast, over 1 at sea. */
    public static double out(long seed, int lx, int lz) {
        return Math.sqrt((double) lx * lx + (double) lz * lz) / coast(seed, lx, lz);
    }

    /** Whether local (lx, lz) is in the waterfall's basin, its cliff or the gorge over it: no pillar, no spur there. */
    public static boolean inFallsCountry(int lx, int lz) {
        return Math.abs(lx) < BASIN_HALF + 28 && lz > SPRING_Z - 16 && lz < BASIN_END + 30;
    }

    /** The mountain's own height at (lx, lz), before the falls and the pillars: its body, spurs, terraces and peak. */
    public static double body(long seed, int lx, int lz) {
        double u = out(seed, lx, lz);
        SimplexNoise n = noise(seed);
        if (u >= 1) return SEA + 1 - (u - 1) * 120;
        double h = SEA + 1 + RISE * Math.pow(1 - u, 1.55);
        // spurs and the ravines between them, running down the flanks
        double ridge = 1 - Math.abs(n.getValue(lx / 64.0 + 3.3, lz / 64.0 - 8.1));
        ridge = ridge * ridge;
        h += 26 * (ridge - 0.45) * Math.sin(Math.PI * Math.min(1, u * 1.2));
        h += 5 * n.getValue(lx / 23.0, lz / 23.0);
        // the peak, a sharp horn over the body, a little north of the centre
        double ds = Math.hypot(lx - SUMMIT_X, lz - SUMMIT_Z);
        h += PEAK * Math.pow(1 - smooth(0, PEAK_RADIUS, ds), 1.4);
        // its shelves: terraced flanks, the orchards' ground
        double w = smooth(0.14, 0.24, u) * (1 - smooth(0.84, 0.95, u));
        if (w > 0) {
            double t = (h + 4 * n.getValue(lx / 41.0 - 2.2, lz / 41.0 + 6.6)) / TERRACE;
            double f = t - Math.floor(t);
            double stepped = (Math.floor(t) + smooth(0.68, 1.0, f)) * TERRACE - 4 * n.getValue(lx / 41.0 - 2.2, lz / 41.0 + 6.6);
            h = Mth.lerp(w * 0.85, h, stepped);
        }
        // the summit's ground, levelled round the egg
        if (ds < SUMMIT_FLAT + 4) h = Mth.lerp(1 - smooth(SUMMIT_FLAT, SUMMIT_FLAT + 4, ds), h, SUMMIT_Y);
        // a beach where it meets the sea
        if (u > 0.93) h = Math.min(h, Mth.lerp(smooth(0.93, 1.0, u), h, SEA + 1));
        return h;
    }

    /**
     * The top block's height at local (lx, lz): the island, cut by the falls country and stood with
     * pillars; at sea, the seabed shoaling up to it from {@code nativeFloor} (the sea's own floor there).
     */
    public static int top(long seed, int lx, int lz, int nativeFloor) {
        double u = out(seed, lx, lz);
        double h = body(seed, lx, lz);
        if (u >= 1) {
            // the shoals, the sea's floor rising toward the beach
            double shoal = Math.max(SEA - 2 - (u - 1) * 110, nativeFloor);
            h = Mth.lerp(smooth(1.0, 1.32, u), SEA - 2 - (u - 1) * 40, nativeFloor);
            h = Math.min(h, SEA - 1);
            h = Math.max(h, Math.min(shoal, SEA - 1));
        }
        h = falls(lx, lz, h);
        Pillar p = pillarAt(seed, lx, lz);
        if (p != null) h = Math.max(h, p.topAt(lx, lz));
        return (int) Math.round(h);
    }

    /** The falls country over the mountain's height {@code h}: the basin cut down, the cliff stood up over it, the gorge cut into it. */
    public static double falls(int lx, int lz, double h) {
        int ax = Math.abs(lx);
        if (lz > CLIFF_Z) {
            // the basin, level, walled at its sides and south end by the land climbing back
            double out = Math.max(smooth(BASIN_HALF, BASIN_HALF + 24, ax), smooth(BASIN_END, BASIN_END + 26, lz));
            double floor = BASIN_Y;
            // past the basin, the stream's valley runs on down to the sea
            if (lz > BASIN_END - 10 && ax < 22) {
                double valley = Mth.lerp(smooth(BASIN_END - 10, BASIN_END + 70, lz), BASIN_Y, SEA + 1) + Math.max(0, ax - 6) * 1.3;
                h = Math.min(h, valley);
            }
            return Math.min(h, Mth.lerp(out, floor, Math.max(h, floor)));
        }
        if (ax <= BASIN_HALF + 4 && lz > SPRING_Z - 30) {
            // the cliff: the mountain at least as high as the lip, standing sheer over the basin
            double cliff = LIP_Y + 1 + (CLIFF_Z - lz) * 0.55 + Math.max(0, ax - FALL_HALF - 6) * 0.2;
            double side = smooth(BASIN_HALF - 6, BASIN_HALF + 4, ax);
            h = Math.max(h, Mth.lerp(side, cliff, h));
        }
        // the gorge the river comes down to the lip, from its spring
        if (lz >= SPRING_Z - SPRING_R && lz <= CLIFF_Z) {
            double half = lz <= SPRING_Z ? Math.sqrt(Math.max(0, SPRING_R * SPRING_R - (double) (lz - SPRING_Z) * (lz - SPRING_Z))) : RIVER_HALF;
            if (ax <= half) return LIP_Y - 2;
            if (ax <= half + 6) h = Math.min(h, LIP_Y + (ax - half) * 2.2);
        }
        return h;
    }

    /** Whether the river's water lies on (lx, lz), a block over its bed ({@link #LIP_Y} - 1), out to the lip over the cliff. */
    public static boolean river(int lx, int lz) {
        int ax = Math.abs(lx);
        if (lz > CLIFF_Z + LIP_OUT || lz < SPRING_Z - SPRING_R) return false;
        if (lz > CLIFF_Z) return ax <= RIVER_HALF;
        if (lz <= SPRING_Z) return (double) lx * lx + (double) (lz - SPRING_Z) * (lz - SPRING_Z) <= SPRING_R * SPRING_R;
        return ax <= RIVER_HALF;
    }

    /** Whether (lx, lz) is under the lip's overhang, out from the cliff toward the falling sheet. */
    public static boolean overhang(int lx, int lz) {
        return lz > CLIFF_Z && lz <= CLIFF_Z + LIP_OUT && Math.abs(lx) <= FALL_HALF + 2;
    }

    /** Whether the sheet of falling water stands in column (lx, lz). */
    public static boolean curtain(int lx, int lz) {
        return lz == FALL_Z && Math.abs(lx) <= FALL_HALF;
    }

    /** Whether (lx, lz) lies in the pool at the waterfall's foot. */
    public static boolean pool(int lx, int lz) {
        double dx = lx / (double) POOL_RX, dz = (lz - POOL_Z) / (double) POOL_RZ;
        return dx * dx + dz * dz <= 1 && lz >= FALL_Z;
    }

    /** How deep the pool is at (lx, lz): deepest under the fall. */
    public static int poolDepth(int lx, int lz) {
        double dx = lx / (double) POOL_RX, dz = (lz - POOL_Z) / (double) POOL_RZ;
        return 1 + (int) Math.round((POOL_DEEP - 1) * (1 - Math.sqrt(Math.min(1, dx * dx + dz * dz))));
    }

    /** Whether the pool's stream runs through (lx, lz), winding south to the sea. */
    public static boolean stream(long seed, int lx, int lz) {
        if (lz < POOL_Z + POOL_RZ - 2) return false;
        double wind = 5 * Math.sin(lz / 23.0) + 3 * noise(seed).getValue(lz / 50.0, 3.7);
        return Math.abs(lx - wind) <= 1.6 && out(seed, lx, lz) < 1.02;
    }

    /** The ledge along the cliff's foot behind the falling sheet, the way to the cave's mouth. */
    public static boolean ledge(int lx, int lz) {
        return lz > CLIFF_Z && lz < FALL_Z && Math.abs(lx) <= BASIN_HALF - 4;
    }

    /** How far vines hang down the cliff's face at column x (0 for none), from its top: curtains of green either side of the falls. */
    public static int vines(int lx) {
        int ax = Math.abs(lx);
        if (ax <= FALL_HALF + 3 || ax > BASIN_HALF + 2) return 0;
        long h = hash(0L, lx, 7, 41);
        int n = (int) Math.floorMod(h, 100L);
        return n < 25 ? 0 : 6 + n % 34;
    }

    // ── The immortal stone egg ──────────────────────────────────────────

    /** The egg's half-width and half-height. */
    public static final double EGG_R = 5.6, EGG_H = 8.5;

    /**
     * The immortal stone egg (仙石) on the summit, from which the Monkey King was born, split open:
     * 0 for no stone at (dx, dy, dz) from its foot (dy 0 the first block over the summit's ground), 1
     * for its shell, 2 for the crack's broken edges. It stands split down its middle (east-west, the
     * crack widening upward), hollow inside.
     */
    public static int egg(int dx, int dy, int dz) {
        double cy = EGG_H;
        double ex = (dx) / EGG_R, ey = (dy - cy + 0.5) / EGG_H, ez = dz / (EGG_R * 0.92);
        // a little narrower at the top, as an egg is
        if (ey > 0) ex /= (1 - 0.18 * ey);
        double r = ex * ex + ey * ey + ez * ez;
        if (r > 1 || dy < 0) return 0;
        double gap = 0.5 + Math.max(0, dy - 3) * 0.16;
        if (Math.abs(dx) < gap && dy > 1) return 0;           // the crack
        double inner = (dx / (EGG_R - 1.3)) * (dx / (EGG_R - 1.3)) + ((dy - cy + 0.5) / (EGG_H - 1.3)) * ((dy - cy + 0.5) / (EGG_H - 1.3))
                + (dz / (EGG_R * 0.92 - 1.3)) * (dz / (EGG_R * 0.92 - 1.3));
        if (inner < 1 && dy > 1) return 0;                    // hollow
        return Math.abs(dx) < gap + 1.2 ? 2 : 1;
    }

    // ── The rock pillars ────────────────────────────────────────────────

    /** A sheer pillar of rock: its middle (local), radius, and the height of its head. */
    public record Pillar(int x, int z, double r, int head, boolean sea) {
        /** The top at (lx, lz) it stands to: its head within its radius, a skirt of fallen rock round its foot. */
        public double topAt(int lx, int lz) {
            double d = Math.hypot(lx - x, lz - z);
            if (d <= r) return head - 3 * Math.pow(d / r, 6);
            return -1e9;
        }
    }

    /** Pillars are tried one in each cell of a grid this wide. */
    public static final int PILLAR_CELL = 30;

    private static final Map<Long, Pillar> PILLARS = new ConcurrentHashMap<>();
    private static final Pillar NONE = new Pillar(0, 0, 0, 0, false);

    /** The pillar of grid cell (i, j), or null: worked out once a cell. */
    @Nullable
    public static Pillar pillar(long seed, int i, int j) {
        Pillar p = PILLARS.computeIfAbsent(seed * 961 + i * 65599L + j, k -> {
            if (unit(seed, i, j, 1) > 0.32) return NONE;
            int x = i * PILLAR_CELL + 6 + (int) (unit(seed, i, j, 2) * (PILLAR_CELL - 12));
            int z = j * PILLAR_CELL + 6 + (int) (unit(seed, i, j, 3) * (PILLAR_CELL - 12));
            double u = out(seed, x, z);
            if (u < 0.3 || u > 1.3 || inFallsCountry(x, z) || CavePlan.near(x, z, 14)) return NONE;
            if (Math.hypot(x - SUMMIT_X, z - SUMMIT_Z) < PEAK_RADIUS + 10) return NONE;
            double r = 3.4 + unit(seed, i, j, 4) * 4.4;
            boolean sea = u > 0.97;
            double base = sea ? SEA - 2 : body(seed, x, z);
            int head = (int) Math.round(base + (sea ? 18 : 22) + unit(seed, i, j, 5) * (sea ? 30 : 46) + (1 - Math.min(1, u)) * 10);
            return new Pillar(x, z, r, head, sea);
        });
        return p == NONE ? null : p;
    }

    /** The pillar standing in column (lx, lz), or null. */
    @Nullable
    public static Pillar pillarAt(long seed, int lx, int lz) {
        int ci = Math.floorDiv(lx, PILLAR_CELL), cj = Math.floorDiv(lz, PILLAR_CELL);
        for (int i = ci - 1; i <= ci + 1; i++) for (int j = cj - 1; j <= cj + 1; j++) {
            Pillar p = pillar(seed, i, j);
            if (p != null && Math.hypot(lx - p.x(), lz - p.z()) <= p.r()) return p;
        }
        return null;
    }

    /** Every pillar of the region. */
    public static List<Pillar> pillars(long seed) {
        List<Pillar> out = new ArrayList<>();
        int reach = ZONE.layout().margin() / PILLAR_CELL + 1;
        for (int i = -reach; i <= reach; i++) for (int j = -reach; j <= reach; j++) {
            Pillar p = pillar(seed, i, j);
            if (p != null) out.add(p);
        }
        return out;
    }

    // ── The monkeys' camps ─────────────────────────────────────────────

    /** A camp of the monkey army on the mountain: its middle (local), and whether its band roams. */
    public record Camp(int x, int z, boolean patrol) {}

    public static final int CAMP_GRID = 64, CAMP_REACH = 6;
    private static final Map<Long, List<Camp>> CAMPS = new ConcurrentHashMap<>();

    /** The camps of the island: one tried in each cell of a grid, kept on fairly level ground of the flanks. */
    public static List<Camp> camps(long seed) {
        return CAMPS.computeIfAbsent(seed, k -> {
            List<Camp> out = new ArrayList<>();
            int reach = COAST / CAMP_GRID + 1;
            for (int i = -reach; i <= reach; i++) for (int j = -reach; j <= reach; j++) {
                if (unit(seed, i, j, 21) > 0.8) continue;
                for (int t = 0; t < 5; t++) {
                    int x = i * CAMP_GRID + 8 + (int) (unit(seed, i, j, 22 + 2 * t) * (CAMP_GRID - 16));
                    int z = j * CAMP_GRID + 8 + (int) (unit(seed, i, j, 23 + 2 * t) * (CAMP_GRID - 16));
                    double u = out(seed, x, z);
                    if (u < 0.3 || u > 0.86 || inFallsCountry(x, z) || CavePlan.near(x, z, 12)) continue;
                    if (!levelAround(seed, x, z)) continue;
                    out.add(new Camp(x, z, unit(seed, i, j, 40) < 0.35));
                    break;
                }
            }
            return List.copyOf(out);
        });
    }

    private static boolean levelAround(long seed, int x, int z) {
        double lo = 1e9, hi = -1e9;
        for (int dx = -CAMP_REACH; dx <= CAMP_REACH; dx += CAMP_REACH) for (int dz = -CAMP_REACH; dz <= CAMP_REACH; dz += CAMP_REACH) {
            if (pillarAt(seed, x + dx, z + dz) != null) return false;
            double h = body(seed, x + dx, z + dz);
            lo = Math.min(lo, h);
            hi = Math.max(hi, h);
        }
        return hi - lo <= 9;
    }

    /** The camp whose ground holds (lx, lz), or null. */
    @Nullable
    public static Camp campAt(long seed, int lx, int lz) {
        for (Camp c : camps(seed)) if (Math.abs(c.x() - lx) <= CAMP_REACH && Math.abs(c.z() - lz) <= CAMP_REACH) return c;
        return null;
    }

    /** A camp's paving height: the island's at its middle. */
    public static int campBase(long seed, Camp c) {
        return (int) Math.round(body(seed, c.x(), c.z()));
    }
}
