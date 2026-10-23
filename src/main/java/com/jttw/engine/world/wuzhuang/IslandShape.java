package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of Mount Longevity (万寿山), a mountain floating high over the clouds at the heart of a
 * Mount Wuzhuang region, worked out from the seed and the region's centre alone, so every chunk agrees
 * on it whichever is made first (laid by {@code MountWuzhuangFeature}). Pure: no world is read.
 *
 * <ul>
 *   <li><b>The island</b>: a rough disc about {@value #RADIUS} blocks round the centre, its edge
 *       wandering; its top a terrace at y {@value #TERRACE} where the temple stands (laid dead level
 *       over the temple and the road to its gate), rising elsewhere in rolling ground and in peaks,
 *       the highest to the north-west behind the temple's garden; falling off in cliffs at its rim.
 *       Its underside hangs below it like the roots of a mountain torn from the earth, never lower
 *       than y {@value #FLOOR}, over the clouds of the world below (y 192);</li>
 *   <li><b>the cloud stair</b> (云梯): a helix of cloud steps out beyond the island's southern rim,
 *       winding down from the terrace to the ground in half-block steps, {@value #STEPS_PER_TURN}
 *       to a turn (a turn falls 16 blocks), and a cloud bridge from its top to the island's edge,
 *       at the head of the road to the temple gate;</li>
 *   <li><b>pines</b> on the slopes, <b>waterfalls</b> pouring off the rim, and banks of <b>cloud</b>
 *       round the island's flanks and wisps under it.</li>
 * </ul>
 * Local coordinates are offsets from the region's centre; the temple's gate faces south (+z).
 */
public final class IslandShape {

    /** The zone it floats in. */
    public static final Zones.Zone ZONE = Zones.MOUNT_WUZHUANG;
    /** The terrace's top (the surface block's y) where the temple stands; one stands at y + 1. */
    public static final int TERRACE = 236;
    /** The lowest the island's underside hangs. */
    public static final int FLOOR = 196;
    /** The island's rough radius, and how far its edge wanders from it at most. */
    public static final int RADIUS = 74, WANDER = 12;
    /** How deep its underside hangs under the terrace at the middle. */
    public static final int HANG = 40;
    /** The temple's ground, kept level: local x and z bounds (the wall, the gate's steps). */
    public static final int TEMPLE_X = 23, TEMPLE_NORTH = -41, TEMPLE_SOUTH = 35;
    /** The road from the gate to the rim, either side of x 0. */
    public static final int ROAD_HALF = 3;
    /** The cloud stair: its axis this far beyond the southern rim; its steps between these radii. */
    public static final int STAIR_OUT = 9;
    public static final double STAIR_INNER = 5.0, STAIR_OUTER = 8.5;
    public static final int STEPS_PER_TURN = 32;
    /** The island's farthest reach from the centre (rim and stair): what a chunk must be within to hold any of it. */
    public static final int REACH = RADIUS + WANDER + STAIR_OUT + 10;

    /** A peak: where (local), how wide and how high over the terrace. */
    public record Peak(int x, int z, int radius, int height) {}

    public static final List<Peak> PEAKS = List.of(new Peak(-40, -50, 28, 42), new Peak(42, -30, 19, 26),
            new Peak(-46, 22, 15, 16), new Peak(38, 42, 12, 10));

    public final long seed;
    public final int cx, cz;
    private final double[] phase = new double[3];
    private final SimplexNoise noise;
    private final double[] falls;

    private IslandShape(long seed, int cx, int cz) {
        this.seed = seed;
        this.cx = cx;
        this.cz = cz;
        // the rim's wobble is its region's cell's (the same for any centre in the cell)
        RandomSource r = RandomSource.create(seed ^ ZONE.layout().cellOf(cx, cz) * 341873128712L ^ 0x5A11E7A5L);
        for (int i = 0; i < phase.length; i++) phase[i] = r.nextDouble() * Math.PI * 2;
        noise = new SimplexNoise(RandomSource.create(seed ^ 0x77A1C0DEL));
        falls = findFalls();
    }

    /** The island of the region centred at (cx, cz). */
    public static IslandShape of(long seed, int cx, int cz) {
        return new IslandShape(seed, cx, cz);
    }

    /** The island of the region holding (x, z), or null where no region is (its host is not checked). */
    public static IslandShape at(long seed, int x, int z) {
        int[] c = ZONE.centre(seed, x, z);
        return c == null ? null : of(seed, c[0], c[1]);
    }

    private static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    // ── The footprint ───────────────────────────────────────────────────

    /** The rim's distance from the centre in direction {@code a} (radians, atan2(dz, dx)). */
    public double radius(double a) {
        double r = RADIUS + 6 * Math.sin(3 * a + phase[0]) + 4 * Math.sin(5 * a + phase[1]) + 2 * Math.sin(9 * a + phase[2]);
        // the south, where the road runs out to the stair, keeps its full reach
        double south = Math.abs(Mth.wrapDegrees(Math.toDegrees(a) - 90)) / 90.0;
        return south < 0.25 ? Math.max(r, RADIUS) : r;
    }

    /** The rim's distance due south, where the road meets the stair's bridge. */
    public int southRim() {
        return (int) Math.floor(radius(Math.PI / 2));
    }

    /** How far out (x, z) lies, as a share of the rim's distance that way: under 1 on the island. */
    public double edge(int x, int z) {
        double dx = x - cx, dz = z - cz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1) return 0;
        return d / radius(Math.atan2(dz, dx));
    }

    public boolean inIsland(int x, int z) {
        return edge(x, z) < 1;
    }

    /** Whether local (lx, lz) is on the temple's level ground. */
    public static boolean onTempleGround(int lx, int lz) {
        return Math.abs(lx) <= TEMPLE_X && lz >= TEMPLE_NORTH && lz <= TEMPLE_SOUTH;
    }

    /** Whether local (lx, lz) is on the road from the gate to the southern rim. */
    public static boolean onRoad(int lx, int lz) {
        return Math.abs(lx) <= ROAD_HALF + 1 && lz >= TEMPLE_SOUTH;
    }

    /** How far local (lx, lz) is from the temple's ground or the road (0 on them). */
    private static double fromLevelGround(int lx, int lz) {
        double ox = Math.max(0, Math.abs(lx) - TEMPLE_X), oz = Math.max(0, Math.max(TEMPLE_NORTH - lz, lz - TEMPLE_SOUTH));
        double temple = Math.hypot(ox, oz);
        double road = lz >= TEMPLE_SOUTH ? Math.max(0, Math.abs(lx) - ROAD_HALF - 1) : Double.MAX_VALUE;
        return Math.min(temple, road);
    }

    // ── Heights ─────────────────────────────────────────────────────────

    /** The peaks' rise over the terrace at local (lx, lz). */
    public double peaks(int lx, int lz) {
        double h = 0;
        for (Peak p : PEAKS) {
            double r = Math.hypot(lx - p.x, lz - p.z) / p.radius;
            if (r >= 1) continue;
            double crag = 1 + 0.18 * noise.getValue((cx + lx) / 7.0 + 31, (cz + lz) / 7.0 - 17);
            h = Math.max(h, p.height * Math.pow(1 - r, 1.4) * crag);
        }
        return h;
    }

    /** The island's top at (x, z) (the surface block's y); meaningless off the island. */
    public int top(int x, int z) {
        int lx = x - cx, lz = z - cz;
        double off = fromLevelGround(lx, lz);
        if (off <= 0) return TERRACE;
        double rolling = TERRACE + 1 + 3 * noise.getValue(x / 31.0, z / 31.0) + 1.2 * noise.getValue(x / 9.0 + 50, z / 9.0);
        double natural = rolling + peaks(lx, lz);
        // the rim falls away in cliffs
        double e = edge(x, z);
        natural -= 9 * smooth(0.8, 1.0, e);
        double w = 1 - smooth(4, 16, off); // level ground blended into the hills round it
        return (int) Math.round(TERRACE * w + natural * (1 - w));
    }

    /** The island's underside at (x, z): the lowest block of it. */
    public int bottom(int x, int z) {
        double e = edge(x, z);
        double hang = HANG * Math.pow(Math.max(0, 1 - e), 0.75) * (0.85 + 0.3 * noise.getValue(x / 23.0 - 70, z / 23.0 + 9));
        // roots of rock hanging down here and there
        double root = noise.getValue(x / 6.0 + 120, z / 6.0 - 40);
        if (root > 0.45 && e < 0.85) hang += (root - 0.45) * 24;
        int y = (int) Math.round(TERRACE - 3 - hang);
        return Math.max(FLOOR, Math.min(y, top(x, z) - 2));
    }

    /** A spread of 0..99 from a place and a salt, the same each time. */
    public int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z) ^ seed, 100L);
    }

    // ── The cloud stair ─────────────────────────────────────────────────

    /** The stair's axis, as {x, z}. */
    public int[] stairAxis() {
        return new int[]{cx, cz + southRim() + STAIR_OUT};
    }

    /**
     * The stair's step index at (x, z): which of its steps a turn this column holds (0 the top,
     * at the north, nearest the island); -1 where the column is no step's.
     */
    public int stairStep(int x, int z) {
        int[] a = stairAxis();
        double dx = x + 0.5 - (a[0] + 0.5), dz = z + 0.5 - (a[1] + 0.5);
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r < STAIR_INNER || r > STAIR_OUTER) return -1;
        double from = Math.atan2(dz, dx) - (-Math.PI / 2); // turning from north toward east, going down
        double k = Mth.positiveModulo(from, Math.PI * 2) / (Math.PI * 2) * STEPS_PER_TURN;
        return (int) Math.round(k) % STEPS_PER_TURN;
    }

    /** The standing height on step {@code k} (counted down from the top, any turn): the terrace's at the top, half a block lower each step. */
    public static double stepTop(int k) {
        return TERRACE + 1 - k / 2.0;
    }

    /** The block a step stands in: its y; a full block for even steps, a bottom slab for odd. */
    public static int stepY(int k) {
        return (int) Math.floor(stepTop(k) - 0.01);
    }

    /** Whether step {@code k} is a slab (half a block) rather than a full block. */
    public static boolean stepIsSlab(int k) {
        return k % 2 == 1;
    }

    /** Whether (x, z) is on the cloud bridge from the stair's top to the island's southern rim. */
    public boolean onBridge(int x, int z) {
        int lx = x - cx, lz = z - cz;
        int rim = southRim();
        return Math.abs(lx) <= 1 && lz >= rim - 3 && lz <= rim + STAIR_OUT - 5;
    }

    // ── Pines ───────────────────────────────────────────────────────────

    /** A pine: its root (world x, z), the ground's top there, its height. */
    public record Pine(int x, int z, int ground, int height) {}

    public static final int PINE_CELL = 9, PINE_REACH = 3;

    /** The pines whose root is within {@code r} of the box (x0, z0)..(x1, z1). */
    public List<Pine> pinesNear(int x0, int z0, int x1, int z1, int r) {
        List<Pine> out = new ArrayList<>();
        for (int i = Math.floorDiv(x0 - r - cx, PINE_CELL); i <= Math.floorDiv(x1 + r - cx, PINE_CELL); i++)
            for (int j = Math.floorDiv(z0 - r - cz, PINE_CELL); j <= Math.floorDiv(z1 + r - cz, PINE_CELL); j++) {
                Pine p = pine(i, j);
                if (p != null && p.x >= x0 - r && p.x <= x1 + r && p.z >= z0 - r && p.z <= z1 + r) out.add(p);
            }
        return out;
    }

    /** The pine of grid cell (i, j), if it has one. */
    public Pine pine(int i, int j) {
        int x = cx + i * PINE_CELL + 1 + hash(i, j, 11) % (PINE_CELL - 2);
        int z = cz + j * PINE_CELL + 1 + hash(i, j, 12) % (PINE_CELL - 2);
        if (hash(i, j, 13) >= 58) return null;
        int lx = x - cx, lz = z - cz;
        if (fromLevelGround(lx, lz) < 5 || edge(x, z) > 0.86) return null;
        int g = top(x, z);
        // not on the steepest crags
        if (Math.abs(top(x + 2, z) - top(x - 2, z)) > 5 || Math.abs(top(x, z + 2) - top(x, z - 2)) > 5) return null;
        return new Pine(x, z, g, 7 + hash(i, j, 14) % 6);
    }

    /** What a pine of height {@code h} has at (dx, dy, dz) from its root (dy 1 the first block over the ground): 0 nothing, 1 trunk, 2 needles. */
    public static int pineAt(int dx, int dy, int dz, int h) {
        if (dx == 0 && dz == 0 && dy >= 1 && dy <= h) return 1;
        if (dy < 3 || dy > h + 1) return 0;
        if (dy == h + 1) return dx == 0 && dz == 0 ? 2 : 0;
        // tiers: each widens downward, then steps back in
        int fromTop = h - dy;
        double r = Math.min(3.2, 0.9 + fromTop * 0.42) - ((fromTop % 3 == 2) ? 0.9 : 0);
        double d = Math.sqrt(dx * dx + dz * dz);
        return d <= r && !(dx == 0 && dz == 0) ? 2 : 0;
    }

    // ── Waterfalls ──────────────────────────────────────────────────────

    /** The waterfalls' directions from the centre (radians): three, never toward the stair, nor into the main peak. */
    public double[] waterfalls() {
        return falls.clone();
    }

    private double[] findFalls() {
        double[] out = new double[3];
        RandomSource r = RandomSource.create(seed ^ (cx * 31L + cz) ^ 0xFA11L);
        int n = 0;
        for (int tries = 0; tries < 64 && n < 3; tries++) {
            double a = r.nextDouble() * Math.PI * 2;
            double south = Math.abs(Mth.wrapDegrees(Math.toDegrees(a) - 90));
            if (south < 55) continue;
            boolean apart = true;
            for (int i = 0; i < n; i++) if (Math.abs(Mth.wrapDegrees(Math.toDegrees(a - out[i]))) < 60) apart = false;
            if (!apart) continue;
            out[n++] = a;
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /** The waterfall's channel along {@code a}: the columns from 6 in to the rim's last, as {x, z} pairs (world). */
    public List<int[]> channel(double a) {
        List<int[]> out = new ArrayList<>();
        double rim = radius(a);
        for (double d = rim - 7; d < rim; d += 0.5) {
            int x = cx + (int) Math.round(Math.cos(a) * d), z = cz + (int) Math.round(Math.sin(a) * d);
            if (!inIsland(x, z)) break;
            // two wide: this column and the one beside it, across the stream
            int sx = cx + (int) Math.round(Math.cos(a) * d - Math.sin(a)), sz = cz + (int) Math.round(Math.sin(a) * d + Math.cos(a));
            for (int[] c : new int[][]{{x, z}, {sx, sz}}) {
                if (!inIsland(c[0], c[1])) continue;
                boolean seen = false;
                for (int[] o : out) if (o[0] == c[0] && o[1] == c[1]) seen = true;
                if (!seen) out.add(c);
            }
        }
        return out;
    }

    // ── Clouds ──────────────────────────────────────────────────────────

    /** Whether a bank of cloud fills (x, y, z): round the island's flanks below its rim, and in wisps under it. */
    public boolean cloud(int x, int y, int z) {
        if (!cloudColumn(x, z)) return false;
        double e = edge(x, z);
        return cloudAt(x, y, z, e, e < 1 ? bottom(x, z) : Integer.MAX_VALUE);
    }

    /** Whether column (x, z) may hold cloud at all: near the island, and clear of the stair, the road and the waterfalls. */
    public boolean cloudColumn(int x, int z) {
        double e = edge(x, z);
        if (e > 1.22 || (e > 0.75 && e < 0.88)) return false;
        int[] s = stairAxis();
        if (Math.hypot(x - s[0], z - s[1]) < STAIR_OUTER + 5) return false;
        if (Math.abs(x - cx) <= 3 && z - cz > 0) return false; // under the road and the bridge
        double ang = Math.atan2(z - cz, x - cx);
        for (double a : falls) if (Math.abs(Mth.wrapDegrees(Math.toDegrees(ang - a))) < 9) return false;
        return true;
    }

    /** Whether cloud fills (x, y, z) of a cloud column whose edge share is {@code e} and island bottom {@code under} (MAX_VALUE off the island). */
    public boolean cloudAt(int x, int y, int z, double e, int under) {
        if (y >= under) return false;
        boolean band = e >= 0.9 && y >= TERRACE - 24 && y <= TERRACE - 12;
        boolean wisp = e < 0.75 && y < under && y >= under - 5;
        if (!band && !wisp) return false;
        double n = noise.getValue(x / 11.0, y / 4.0, z / 11.0) + 0.35 * noise.getValue(x / 4.0 + 9, y / 3.0, z / 4.0 - 9);
        if (band) return n + 0.3 * (1 - Math.abs(y - (TERRACE - 18)) / 6.0) > 0.88;
        return n > 0.75;
    }

    /** The lowest and highest y a cloud may be at. */
    public static final int CLOUD_LOW = FLOOR - 6, CLOUD_HIGH = TERRACE - 10;
}
