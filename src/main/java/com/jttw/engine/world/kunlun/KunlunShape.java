package com.jttw.engine.world.kunlun;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The shape of Mount Kunlun, worked out from the seed and the region alone, so every chunk agrees on
 * it whichever is made first (pure: unit-tested):
 * <ul>
 *   <li><b>its summit</b> ({@link #summit}): a level top, a circle {@value #TOP} blocks out from the
 *       region's centre, room for the Queen Mother's palace; well over every mountain round it (the
 *       highest land within {@value #LOOK} blocks and {@value #HEADROOM} more, and at least
 *       {@value #MIN_RISE} over its foot), never under y {@value #MIN_SUMMIT}, well under the sky;</li>
 *   <li><b>its flanks</b> ({@link #height}): from there the ground falls back to the land's own
 *       over {@value #SLOPE} blocks or so, steepest under the summit, gentler toward the foot (the
 *       profile of a great peak), stretched out along five ridges that run down from it, broken by
 *       crags and stepped into ledges and cliffs on the way down; past {@value #REACH} blocks, the
 *       land's own;</li>
 *   <li><b>the stair</b> ({@link #path}): a stone way that winds up round the peak, keeping to its
 *       flank at an even climb (never more than {@value #MAX_CLIMB} a block, so it is walked in half
 *       steps), from its foot to the summit's south edge, where the palace's gate faces it;</li>
 *   <li><b>the snow</b> ({@link #snowLine}) from about {@value #SNOW_BELOW} blocks under the summit,
 *       and <b>the clouds</b> ({@link #cloud}) that cling round its shoulders, about {@value #CLOUD_BELOW} under it.</li>
 * </ul>
 * Local coordinates are block offsets from the region's centre (the stair arrives from +z, south).
 */
public final class KunlunShape {

    private KunlunShape() {}

    public static final Zones.Zone ZONE = Zones.KUNLUN;

    /** The summit's level top (a circle, from the centre), and its flanks' run to the foot. */
    public static final int TOP = 25, SLOPE = 112;
    /** How far the line of its flanks wanders either way, and how much farther they run out along a ridge (a share). */
    public static final int WANDER = 8;
    public static final double SPUR = 0.3;
    /** The farthest anything of the mountain reaches from the centre: past it, the land's own. */
    public static final int REACH = 148;
    /** The profile's power: the higher, the steeper under the summit and the gentler at the foot. */
    public static final double POWER = 1.25;
    /** The crags' height, and the ledges' rise (the flanks are stepped into ledges and cliffs this high). */
    public static final double CRAG = 7, LEDGE = 7, LEDGES = 0.8;
    /** The summit: so far over the highest land within {@value #LOOK} blocks, so far over its foot at least, never under this y. */
    public static final int HEADROOM = 40, LOOK = 240, MIN_RISE = 150, MIN_SUMMIT = 248;
    /** The summit is never higher than the sky less this (room for the palace over it). */
    public static final int UNDER_SKY = 32;
    /** The stair: its half-width (the way), and how far out its edge reaches (a rail on the drop side). */
    public static final double PATH_HALF = 1.6, SHOULDER = 2.8;
    /** Past its edge, the flank is cut back from the stair in a bank this wide (gently on the drop side, steeply on the uphill), so it is never a trench. */
    public static final double BANK = 7;
    /** How steeply it climbs, as laid out and at most, in blocks up a block along. */
    public static final double CLIMB = 0.25, MAX_CLIMB = 0.33;
    /** It sets out this far from the centre, at the foot. */
    public static final int PATH_FROM = REACH - 14;
    /** The snow from this far under the summit (wandering a little), never under this y. */
    public static final int SNOW_BELOW = 95, MIN_SNOW = 160;
    /** The clouds round its shoulders, this far under the summit (their middle), this thick either way. */
    public static final int CLOUD_BELOW = 48, CLOUD_HALF = 6;

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0x6B0A1F5EL));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** Whether local (dx, dz) is on the summit's level top. */
    public static boolean onTop(int dx, int dz) {
        return dx * dx + dz * dz <= TOP * TOP;
    }

    /** How far each of the five ridges runs out (0..1, on the angle it runs at), from the seed. */
    static double spur(long seed, double angle) {
        double[] ridges = ridges(seed);
        double best = 0;
        for (int k = 0; k < 5; k++) {
            double c = Math.max(0, Math.cos(angle - ridges[2 * k]));
            best = Math.max(best, ridges[2 * k + 1] * Math.pow(c, 6));
        }
        return best;
    }

    private static long ridgeSeed = Long.MIN_VALUE;
    private static double[] ridgeAt;

    /** The five ridges' bearings and how far each runs out, from the seed, as pairs. */
    private static synchronized double[] ridges(long seed) {
        if (ridgeAt == null || seed != ridgeSeed) {
            RandomSource r = RandomSource.create(seed ^ 0x5B0A2EL);
            double base = r.nextDouble() * Math.PI * 2;
            double[] out = new double[10];
            for (int k = 0; k < 5; k++) {
                out[2 * k] = base + k * Math.PI * 2 / 5 + (r.nextDouble() - 0.5) * 0.5;
                out[2 * k + 1] = 0.6 + 0.4 * r.nextDouble();
            }
            ridgeAt = out;
            ridgeSeed = seed;
        }
        return ridgeAt;
    }

    /**
     * How far down the flank local (dx, dz) is: 0 on the summit, rising to 1 at the foot (the line of
     * it wandering, stretched out along the ridges), and 1 from {@value #REACH} out whatever.
     */
    public static double down(long seed, int x, int z, int dx, int dz) {
        double r = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (r <= TOP) return 0;
        double wander = WANDER * noise(seed).getValue(x / 36.0 + 3.1, z / 36.0 - 7.7) * smooth(TOP, TOP + 16, r);
        double re = (r + wander) / (1 + SPUR * spur(seed, Math.atan2(dz, dx)) * (1 - smooth(REACH - 50, REACH - 10, r)));
        double t = Mth.clamp((re - TOP) / SLOPE, 0, 1);
        return Math.max(t, smooth(REACH - 18, REACH, r));
    }

    /** How much of the land's own height a place this far down the flank ({@link #down}) keeps: 0 at the summit, 1 at the foot. */
    public static double share(double down) {
        return 1 - Math.pow(1 - down, POWER);
    }

    /**
     * The height the ground at local (dx, dz) is brought to (world x, z), the land's own there being
     * {@code natural}: the summit on the top, the land's own past the foot, the flank between, its
     * crags and ledges.
     */
    public static double height(long seed, int x, int z, int dx, int dz, int natural, int summit) {
        double g = share(down(seed, x, z, dx, dz));
        if (g <= 0) return summit;
        if (g >= 1) return natural;
        double h = summit + (natural - summit) * g;
        double mid = 4 * g * (1 - g); // the flanks' middle: 1 halfway down, 0 at the summit and the foot
        h += CRAG * mid * (noise(seed).getValue(x / 14.0 - 9.4, z / 14.0 + 2.2) + 0.5 * noise(seed).getValue(x / 5.0, z / 5.0));
        double f = h / LEDGE, k = Math.floor(f);
        double stepped = (k + smooth(0.5, 1, f - k)) * LEDGE;
        return Mth.lerp(LEDGES * Math.min(1, mid * 1.6), h, stepped);
    }

    /** How deep in white local (x, z) is from: under this, its own ground; over it, snow. */
    public static double snowLine(long seed, int x, int z, int summit) {
        return Math.max(MIN_SNOW, summit - SNOW_BELOW + 6 * noise(seed).getValue(x / 24.0 + 40.5, z / 24.0 - 11.3));
    }

    /**
     * How cloudy it is at (x, y, z), local (dx, dz), the summit {@code summit}: over 0, a cloud. The
     * clouds lie in a band round the mountain's shoulders, {@value #CLOUD_BELOW} under the summit,
     * puffed up in drifts and broken, hugging the flank (out from where it falls to their height, the
     * land round its foot being at {@code foot}).
     */
    public static double cloud(long seed, int x, int y, int z, int dx, int dz, int summit, int foot) {
        double mid = summit - CLOUD_BELOW;
        double off = Math.abs(y - mid) / CLOUD_HALF;
        if (off >= 1) return -1;
        double r = Math.sqrt((double) dx * dx + (double) dz * dz), at = radiusAt(CLOUD_BELOW / (double) Math.max(1, summit - foot));
        double out = (r - at) / 26.0;
        if (out < -0.2 || out > 1.4) return -1;
        double n = noise(seed).getValue(x / 16.0 + 7.7, y / 6.0, z / 16.0 - 3.3) + 0.5 * noise(seed).getValue(x / 6.0, y / 4.0 + 50, z / 6.0);
        return n - 0.35 - off * off * 0.9 - Math.abs(out - 0.4) * 0.6;
    }

    /** About how far out the flank has fallen this share of the way from the summit to the foot (the profile alone). */
    static double radiusAt(double fallen) {
        double g = Mth.clamp(fallen, 0, 1);
        return TOP + SLOPE * (1 - Math.pow(1 - g, 1 / POWER));
    }

    // ── The summit's height ─────────────────────────────────────────────

    private static final Map<Long, Integer> SUMMITS = new ConcurrentHashMap<>();

    private static long key(long seed, int cx, int cz) {
        return seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL));
    }

    /**
     * The summit's top (the palace's ground) in the region centred at (cx, cz): the highest of the
     * land ({@code land}) within {@value #LOOK} blocks and {@value #HEADROOM} more, at least
     * {@value #MIN_RISE} over the land round its foot; not under {@code minSummit} nor over
     * {@code maxSummit}. Worked out once for each region.
     */
    public static int summit(long seed, int cx, int cz, IntBinaryOperator land, int minSummit, int maxSummit) {
        return SUMMITS.computeIfAbsent(key(seed, cx, cz), k -> {
            int highest = land.applyAsInt(cx, cz);
            for (int r = 40; r <= LOOK; r += 40) {
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8;
                    highest = Math.max(highest, land.applyAsInt(cx + (int) Math.round(Math.cos(a) * r), cz + (int) Math.round(Math.sin(a) * r)));
                }
            }
            return Mth.clamp(Math.max(highest + HEADROOM, foot(seed, cx, cz, land) + MIN_RISE), minSummit, maxSummit);
        });
    }

    private static final Map<Long, Integer> FEET = new ConcurrentHashMap<>();

    /** The land's height round the mountain's foot in the region centred at (cx, cz), on average: worked out once a region. */
    public static int foot(long seed, int cx, int cz, IntBinaryOperator land) {
        return FEET.computeIfAbsent(key(seed, cx, cz), k -> {
            int sum = 0;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                sum += land.applyAsInt(cx + (int) Math.round(Math.cos(a) * REACH), cz + (int) Math.round(Math.sin(a) * REACH));
            }
            return Math.round(sum / 8f);
        });
    }

    // ── The stair ───────────────────────────────────────────────────────

    /** The nearest of the stair to a place: how far off its middle, the height of its way there, the stretch, and whether the place is on its outer (drop) side. */
    public record Hit(double d, double y, int index, boolean outer) {}

    /**
     * The stair up the mountain: its middle line, a point a block along, from the foot ({@code 0}) to
     * the summit's south edge, as local x, z and the height of its way (to the half block: under it
     * whole blocks, over a half, a slab).
     */
    public static final class Path {

        private static final int CELL = 8;
        public final double[] x, z, y;
        private final Map<Long, int[]> cells = new HashMap<>();

        Path(double[] x, double[] z, double[] y) {
            this.x = x;
            this.z = z;
            this.y = y;
            Map<Long, List<Integer>> build = new HashMap<>();
            int pad = (int) Math.ceil(SHOULDER + BANK) + 2;
            for (int i = 0; i < x.length; i++) {
                for (int cx = Math.floorDiv((int) Math.floor(x[i]) - pad, CELL); cx <= Math.floorDiv((int) Math.floor(x[i]) + pad, CELL); cx++) {
                    for (int cz = Math.floorDiv((int) Math.floor(z[i]) - pad, CELL); cz <= Math.floorDiv((int) Math.floor(z[i]) + pad, CELL); cz++) {
                        build.computeIfAbsent(((long) cx << 32) ^ (cz & 0xFFFFFFFFL), k -> new ArrayList<>()).add(i);
                    }
                }
            }
            build.forEach((k, v) -> cells.put(k, v.stream().mapToInt(Integer::intValue).toArray()));
        }

        public int size() {
            return x.length;
        }

        /** The nearest of the stair to local (dx, dz) (a block's middle), if within {@code within} of its middle line; else null. */
        @Nullable
        public Hit nearest(int dx, int dz, double within) {
            double px = dx + 0.5, pz = dz + 0.5;
            int[] near = cells.get(((long) Math.floorDiv(dx, CELL) << 32) ^ (Math.floorDiv(dz, CELL) & 0xFFFFFFFFL));
            if (near == null) return null;
            double best = within * within;
            Hit hit = null;
            for (int i : near) {
                if (i + 1 >= x.length) continue;
                double ax = x[i], az = z[i], bx = x[i + 1] - ax, bz = z[i + 1] - az;
                double len2 = bx * bx + bz * bz;
                double t = len2 <= 0 ? 0 : Mth.clamp(((px - ax) * bx + (pz - az) * bz) / len2, 0, 1);
                double qx = ax + bx * t, qz = az + bz * t;
                double d2 = (px - qx) * (px - qx) + (pz - qz) * (pz - qz);
                if (d2 <= best) {
                    best = d2;
                    boolean outer = (px - qx) * qx + (pz - qz) * qz > 0;
                    hit = new Hit(Math.sqrt(d2), Mth.lerp(t, y[i], y[i + 1]), t < 0.5 ? i : i + 1, outer);
                }
            }
            return hit;
        }
    }

    private static final Map<Long, Path> PATHS = new ConcurrentHashMap<>();

    /** Which way the stair winds round, from the seed: 1 or -1. */
    public static int turning(long seed) {
        return (seed & 1) == 0 ? 1 : -1;
    }

    /**
     * The stair in the region centred at (cx, cz), its summit {@code summit}, the land's own height
     * {@code land} (worked out once a region). Laid out from the summit's south edge down, walked
     * a block at a time round the peak the way it winds ({@link #turning}): each step turning
     * gently, whichever way keeps the ground {@value #AHEAD} blocks ahead nearest to falling
     * {@value #CLIMB} a block (the flank as it is really shaped there), never back toward the
     * summit and never near a turn of its own laid before, until it is down on the land at the
     * foot. Then its way follows the ground, kept to a climb of {@value #MAX_CLIMB} a block (and
     * eased), up to the summit.
     */
    public static Path path(long seed, int cx, int cz, int summit, IntBinaryOperator land) {
        return PATHS.computeIfAbsent(key(seed, cx, cz) ^ summit, k -> {
            int dir = turning(seed);
            Map<Long, Integer> lands = new HashMap<>();
            Ground ground = (lx, lz) -> Math.min(summit, height(seed, cx + lx, cz + lz, lx, lz, landNear(lands, land, cx + lx, cz + lz), summit));
            List<double[]> pts = new ArrayList<>();
            Map<Long, List<Integer>> crowd = new HashMap<>();
            double px = 0, pz = TOP + 1, h = summit;
            double heading = dir > 0 ? Math.PI : 0; // going down, round the peak the way it winds
            pts.add(new double[]{px, pz});
            for (int i = 1; i < 4000; i++) {
                double best = Double.MAX_VALUE, bh = heading;
                // each rate of turning tried a stretch ahead, kept to: the one whose way keeps nearest the climb, going on round
                for (int turn = -TURNS; turn <= TURNS; turn++) {
                    double a = heading, qx = px, qz = pz, cost = Math.abs(turn * TURN) * 2;
                    for (int ahead = 1; ahead <= AHEAD; ahead++) {
                        a += turn * TURN;
                        qx += Math.cos(a);
                        qz += Math.sin(a);
                        double rq = Math.hypot(qx, qz);
                        if (rq < TOP + 0.5) cost += 100; // never back onto the summit
                        if (rq > REACH - 4) cost += 20;
                        cost += Math.abs(ground.at((int) Math.floor(qx), (int) Math.floor(qz)) - (h - ahead * CLIMB)) / AHEAD;
                        double round = (-qz * Math.cos(a) + qx * Math.sin(a)) / rq * dir; // how much it goes on round the peak
                        if (round < 0.3) cost += 10 * (0.3 - round) / AHEAD;
                        if (ahead <= 2) {
                            for (int j : near(crowd, qx, qz)) {
                                if (j < i - 40 && Math.hypot(pts.get(j)[0] - qx, pts.get(j)[1] - qz) < 2 * SHOULDER + 6) cost += 30;
                            }
                        }
                    }
                    if (cost < best) {
                        best = cost;
                        bh = heading + turn * TURN;
                    }
                }
                heading = bh;
                px += Math.cos(heading);
                pz += Math.sin(heading);
                h -= CLIMB;
                pts.add(new double[]{px, pz});
                crowd.computeIfAbsent(cell(px, pz), c -> new ArrayList<>()).add(i);
                int lx = (int) Math.floor(px), lz = (int) Math.floor(pz);
                double r = Math.hypot(px, pz);
                if (r >= REACH - 5 || r >= PATH_FROM - 30 && share(down(seed, cx + lx, cz + lz, lx, lz)) >= 0.98) break; // down on the land
            }
            java.util.Collections.reverse(pts);
            int n = pts.size();
            double[] x = new double[n], z = new double[n], raw = new double[n], y = new double[n];
            for (int i = 0; i < n; i++) {
                x[i] = pts.get(i)[0];
                z[i] = pts.get(i)[1];
                raw[i] = ground.at((int) Math.floor(x[i]), (int) Math.floor(z[i]));
            }
            climb(raw, y, summit);
            // eased: a running mean, then kept to the climb again
            double[] eased = new double[n];
            for (int i = 0; i < n; i++) {
                double sum = 0;
                int count = 0;
                for (int j = Math.max(0, i - 4); j <= Math.min(n - 1, i + 4); j++) {
                    sum += y[j];
                    count++;
                }
                eased[i] = sum / count;
            }
            climb(eased, y, summit);
            return new Path(x, z, y);
        });
    }

    /** The ground's height at local (x, z), as the mountain shapes it. */
    @FunctionalInterface
    private interface Ground {
        double at(int lx, int lz);
    }

    /** How far the stair may turn a block, in how many steps either way it is tried, and how far ahead each is looked along. */
    private static final double TURN = 0.025;
    private static final int TURNS = 8, AHEAD = 14;

    /** The land's height at (x, z), from its heights on a grid of four blocks ({@code lands}, filled as asked), between them in a straight line. */
    private static int landNear(Map<Long, Integer> lands, IntBinaryOperator land, int x, int z) {
        int gx = Math.floorDiv(x, 4), gz = Math.floorDiv(z, 4);
        double fx = (x - gx * 4) / 4.0, fz = (z - gz * 4) / 4.0;
        double h = 0;
        for (int i = 0; i <= 1; i++) for (int j = 0; j <= 1; j++) {
            int ax = (gx + i) * 4, az = (gz + j) * 4;
            int v = lands.computeIfAbsent(((long) ax << 32) ^ (az & 0xFFFFFFFFL), k -> land.applyAsInt(ax, az));
            h += v * (i == 0 ? 1 - fx : fx) * (j == 0 ? 1 - fz : fz);
        }
        return (int) Math.round(h);
    }

    private static long cell(double x, double z) {
        return ((long) Math.floorDiv((int) Math.floor(x), 8) << 32) ^ (Math.floorDiv((int) Math.floor(z), 8) & 0xFFFFFFFFL);
    }

    /** The points laid so far in the cells round (x, z). */
    private static List<Integer> near(Map<Long, List<Integer>> crowd, double x, double z) {
        List<Integer> out = new ArrayList<>();
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            List<Integer> c = crowd.get(cell(x + i * 8, z + j * 8));
            if (c != null) out.addAll(c);
        }
        return out;
    }

    /** {@code out}: the heights {@code want}, the last brought to the summit, each kept within {@value #MAX_CLIMB} of the next. */
    private static void climb(double[] want, double[] out, int summit) {
        int n = want.length;
        out[n - 1] = summit;
        for (int i = n - 2; i >= 0; i--) out[i] = Mth.clamp(want[i], out[i + 1] - MAX_CLIMB, Math.min(summit, out[i + 1] + MAX_CLIMB));
    }
}
