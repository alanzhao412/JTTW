package com.jttw.engine.world.fire;

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
 * The shape of the Flaming Mountains, worked out from the seed and the region alone, so every chunk
 * agrees on it whichever is made first (pure: unit-tested):
 * <ul>
 *   <li><b>the range</b> ({@link #riseAt}): from the region's edge the ground climbs over the outer
 *       {@value #EDGE_PERCENT}% of the way in into long chains of red mountains (ridged noise, two or
 *       three hundred blocks between crests, rounded summits along them), up to {@value #BASE} +
 *       {@value #RIDGE} + {@value #PEAKS} blocks over the land, with valleys between them where the burning earth lies
 *       ({@link #valley});</li>
 *   <li><b>Mount Haohao</b> (号山) at the heart: a mass rising {@value #HAOHAO} blocks more within
 *       {@value #HAOHAO_RADIUS} of the centre, with Red Boy's Fire Cloud Cave in it and under it
 *       ({@link FortressPlan}: its door at the foot of the mountain's south face, its floors going
 *       down under the centre);</li>
 *   <li><b>the gorge</b>, Withered Pine Ravine (枯松涧): a cleft cut through the range from the
 *       cave's great door southward to the region's edge, its floor level with the door's for a
 *       forecourt before it ({@link #gorgeShare}, {@link #floorShare}), then rising to meet the land;</li>
 *   <li><b>the outposts</b> ({@link #outposts}): watchtowers of the cave's demons over the range,
 *       a few on the ravine's rims, each with its band.</li>
 * </ul>
 * Local coordinates are block offsets from the region's centre (the gate toward +z, south).
 */
public final class RangeShape {

    private RangeShape() {}

    public static final Zones.Zone ZONE = Zones.FIRE_MOUNTAIN;

    /** The share of the way in over which the range climbs out of the land, as a percentage, and as a share. */
    public static final int EDGE_PERCENT = 22;
    private static final double EDGE = EDGE_PERCENT / 100.0;
    /** The range's least rise over the land, how much more its crests reach, and its lesser peaks on them. */
    public static final int BASE = 10, RIDGE = 62, PEAKS = 16;
    /** Mount Haohao's rise over the range, and how far from the centre it spreads. */
    public static final int HAOHAO = 55, HAOHAO_RADIUS = 140;
    /** Where the ridge noise falls under this, the ground is a valley: the burning earth's. */
    public static final double VALLEY = 0.22;
    /** The gorge: from just past the door's facade, its floor this wide either side of the middle, its walls rising over this far. */
    public static final int GORGE_FROM = FortressPlan.FACE_Z + 1, GORGE_HALF = 18, GORGE_WALL = 46;
    /** Its forecourt, level with the door, this long; then this long to climb to the land. */
    public static final int FORECOURT = 26, CLIMB = 70;

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0xF1AE5A7EL));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** The range's crest at (x, z): 1 on a ridge line, falling away to 0 in the valleys between. */
    public static double ridge(long seed, int x, int z) {
        double n = noise(seed).getValue(x / 160.0 + 71.3, z / 160.0 - 19.7);
        double r = 1 - Math.abs(n);
        return r * r;
    }

    /** Whether (x, z) is valley ground, where the burning earth lies (the range's own, the gorge's floor aside). */
    public static boolean valley(long seed, int x, int z) {
        return ridge(seed, x, z) < VALLEY;
    }

    /** Whether a seam of lava winds through (x, z): thin lines across the valleys, where a slow noise crosses zero. */
    public static boolean seam(long seed, int x, int z) {
        return Math.abs(noise(seed).getValue(x / 46.0 + 5.5, z / 46.0 - 8.25)) < 0.03;
    }

    /** How far the gorge cuts the range at local (dx, dz): 0 in its floor, rising to 1 past its walls (and north of the gate). */
    public static double gorgeShare(int dx, int dz) {
        if (dz < GORGE_FROM) return 1;
        return smooth(GORGE_HALF, GORGE_WALL, Math.abs(dx));
    }

    /**
     * How far the gorge's floor at local (dx, dz) has climbed from the hall's floor to the land's own
     * height: 0 in the forecourt, 1 out where it has met the land (and outside the gorge).
     */
    public static double floorShare(int dx, int dz) {
        if (dz < FortressPlan.FACE_Z || Math.abs(dx) > GORGE_WALL) return 1;
        return smooth(GORGE_FROM + FORECOURT, GORGE_FROM + FORECOURT + CLIMB, dz);
    }

    /** Whether local (dx, dz) is on the gorge's floor (forecourt and all), the way to the gate, which is kept clear. */
    public static boolean onGorgeFloor(int dx, int dz) {
        return dz > FortressPlan.FACE_Z && Math.abs(dx) <= GORGE_HALF + 1;
    }

    /** Whether local (dx, dz) is on the forecourt, the level ground before the gate. */
    public static boolean onForecourt(int dx, int dz) {
        return dz > FortressPlan.FACE_Z && dz <= GORGE_FROM + FORECOURT && Math.abs(dx) <= GORGE_HALF + 1;
    }

    /** How many blocks the range adds to the land at (x, z): 0 outside it. */
    public static int riseAt(long seed, int x, int z) {
        double t = ZONE.depth(seed, x, z);
        if (t <= 0) return 0;
        double edge = smooth(0, EDGE, t);
        double r = ridge(seed, x, z);
        double peaks = 0.5 + 0.5 * noise(seed).getValue(x / 45.0 - 33.1, z / 45.0 + 12.9); // rounded summits along the crests
        double h = BASE + RIDGE * r * (0.55 + 0.45 * t) + PEAKS * r * peaks;
        int[] c = ZONE.centre(seed, x, z);
        if (c != null) {
            int dx = x - c[0], dz = z - c[1];
            double d = Math.sqrt((double) dx * dx + (double) dz * dz);
            h += HAOHAO * (1 - smooth(0, HAOHAO_RADIUS, d)) * (0.85 + 0.15 * noise(seed).getValue(x / 18.0, z / 18.0));
            h *= gorgeShare(dx, dz);
        }
        return (int) Math.round(edge * h);
    }

    /** The cached hall floor of each region, by its centre. */
    private static final Map<Long, Integer> FLOORS = new ConcurrentHashMap<>();

    /**
     * The top of the hall's floor in the region centred at (cx, cz): the land's height there (by its
     * broad shape, {@code land}), never under {@code minFloor} (the sea's level, and a little). Worked
     * out once for each region.
     */
    public static int hallFloor(long seed, int cx, int cz, java.util.function.IntBinaryOperator land, int minFloor) {
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)),
                k -> Math.max(minFloor, land.applyAsInt(cx, cz)));
    }

    // ── The outposts ────────────────────────────────────────────────────

    /** One outpost: its middle (world x, z); whether it stands on the ravine's rim; whether its band roams (a patrol). */
    public record Outpost(int x, int z, boolean rim, boolean patrol) {}

    /** How far round its middle an outpost reaches; outposts are tried on a grid this wide. */
    public static final int OUTPOST_REACH = 6, OUTPOST_GRID = 72;

    private static final Map<Long, List<Outpost>> OUTPOSTS = new ConcurrentHashMap<>();

    private static int hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        return (int) Math.floorMod(h ^ (h >>> 29), 1000L);
    }

    /**
     * The outposts of the region centred at (cx, cz), worked out from the seed alone: one tried in
     * each cell of a grid {@value #OUTPOST_GRID} blocks wide over the range, kept where the ground
     * is fairly level, well inside the region, clear of Mount Haohao and of the ravine; and three
     * watchtowers along the ravine's rims, looking down on the way to the door. About one band in
     * three roams (a patrol).
     */
    public static List<Outpost> outposts(long seed, int cx, int cz) {
        return OUTPOSTS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)), key -> {
            List<Outpost> out = new ArrayList<>();
            int reach = ZONE.layout().margin();
            for (int i = -reach / OUTPOST_GRID - 1; i <= reach / OUTPOST_GRID + 1; i++) {
                for (int j = -reach / OUTPOST_GRID - 1; j <= reach / OUTPOST_GRID + 1; j++) {
                    if (hash(seed, i, j, 1) >= 760) continue;
                    // a few tries in the cell for level ground
                    for (int t = 0; t < 4; t++) {
                        int x = cx + i * OUTPOST_GRID + 10 + hash(seed, i, j, 2 + 3 * t) % (OUTPOST_GRID - 20) - OUTPOST_GRID / 2;
                        int z = cz + j * OUTPOST_GRID + 10 + hash(seed, i, j, 3 + 3 * t) % (OUTPOST_GRID - 20) - OUTPOST_GRID / 2;
                        int dx = x - cx, dz = z - cz;
                        double d = ZONE.layout().depth(seed, new int[]{cx, cz}, x, z);
                        if (d < 0.1 || d > 0.82 || Math.hypot(dx, dz) < HAOHAO_RADIUS * 0.8) continue;
                        if (dz > FortressPlan.FACE_Z - 20 && Math.abs(dx) < GORGE_WALL + 14) continue; // the ravine is kept clear
                        if (!level(seed, x, z)) continue;
                        out.add(new Outpost(x, z, false, hash(seed, i, j, 4) < 340));
                        break;
                    }
                }
            }
            // the watchtowers on the rims
            for (int k = 0; k < 3; k++) {
                int side = k % 2 == 0 ? 1 : -1;
                int x = cx + side * (GORGE_HALF + 16), z = cz + FortressPlan.FACE_Z + 46 + 58 * k;
                if (ZONE.layout().depth(seed, new int[]{cx, cz}, x, z) < 0.08) continue;
                out.add(new Outpost(x, z, true, false));
            }
            return List.copyOf(out);
        });
    }

    /** Whether the range is fairly level round (x, z): no more than ten blocks' rise across an outpost. */
    private static boolean level(long seed, int x, int z) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -OUTPOST_REACH; dx <= OUTPOST_REACH; dx += OUTPOST_REACH) for (int dz = -OUTPOST_REACH; dz <= OUTPOST_REACH; dz += OUTPOST_REACH) {
            int r = riseAt(seed, x + dx, z + dz);
            lo = Math.min(lo, r);
            hi = Math.max(hi, r);
        }
        return hi - lo <= 10;
    }

    /** The outpost whose reach holds (x, z) in the region centred at (cx, cz), or null. */
    @Nullable
    public static Outpost outpostAt(long seed, int cx, int cz, int x, int z) {
        for (Outpost o : outposts(seed, cx, cz)) {
            if (Math.abs(o.x() - x) <= OUTPOST_REACH && Math.abs(o.z() - z) <= OUTPOST_REACH) return o;
        }
        return null;
    }

    /** The centre of the region holding (x, z), or null. */
    @Nullable
    public static int[] centre(long seed, int x, int z) {
        return ZONE.centre(seed, x, z);
    }
}
