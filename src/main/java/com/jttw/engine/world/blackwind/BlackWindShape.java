package com.jttw.engine.world.blackwind;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The shape of Black Wind Mountain (黑风山), worked out from the seed and the region alone, so every
 * chunk agrees on it whichever is made first (pure: unit-tested):
 * <ul>
 *   <li><b>the range</b> ({@link #riseAt}): from the region's edge the ground climbs over the outer
 *       {@value #EDGE_PERCENT}% of the way in into ridges of dark, pine-clad mountains (ridged noise,
 *       broken crags along the crests), up to {@value #BASE} + {@value #RIDGE} + {@value #CRAGS} blocks
 *       over the land;</li>
 *   <li><b>the black peak</b> at the heart: a mass rising {@value #PEAK} blocks more round a point a
 *       little north of the centre ({@link #PEAK_Z}), its south face a cliff over the Black Wind Cave's
 *       great door ({@link BlackWindPlan}), its summit bare black rock;</li>
 *   <li><b>the valley road</b>: a valley cut from the cave's door southward to the region's edge, its
 *       floor level before the door (the forecourt), then falling (or rising) in two ramps to the land
 *       at its mouth, level again across the terrace where the burnt Guanyin Chan Temple stands
 *       ({@link TemplePlan}) ({@link #valleyShare}, {@link #floorAt});</li>
 *   <li><b>the camps</b> ({@link #camps}): the Black Bear's watch-camps on the range, each with its band.</li>
 * </ul>
 * Local coordinates are block offsets from the region's centre (the door toward +z, south).
 */
public final class BlackWindShape {

    private BlackWindShape() {}

    public static final Zones.Zone ZONE = Zones.BLACK_WIND;

    /** The share of the way in over which the range climbs out of the land, as a percentage. */
    public static final int EDGE_PERCENT = 20;
    private static final double EDGE = EDGE_PERCENT / 100.0;
    /** The range's least rise over the land, how much more its ridges reach, and the crags along them. */
    public static final int BASE = 6, RIDGE = 52, CRAGS = 14;
    /** The black peak: its rise over the range, how far it spreads, and its centre (local z, north of the door). */
    public static final int PEAK = 150, PEAK_RADIUS = 200, PEAK_Z = -40;

    /** The valley: how wide its floor is either side of the road (along the ramps), how far its walls take to rise. */
    public static final int ROAD_HALF = 11, WALL = 30;
    /** Its floor is this wide before the door (the forecourt) and across the temple's terrace. */
    public static final int COURT_HALF = 24, TERRACE_HALF = 40;
    /** Where it runs, local z: the forecourt from the cliff to {@link #COURT_END}; the first ramp; the temple's terrace; the second ramp to the land at the mouth. */
    public static final int COURT_START = BlackWindPlan.FACE_Z + 1, COURT_END = 96, TERRACE_START = 168, TERRACE_END = 248, MOUTH = 318;

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0xB1AC3B1D5EEDL));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** The range's ridges at (x, z): 1 on a crest, falling to 0 between them. */
    public static double ridge(long seed, int x, int z) {
        double n = noise(seed).getValue(x / 130.0 - 41.7, z / 130.0 + 13.1);
        double r = 1 - Math.abs(n);
        return r * r;
    }

    /** Whether (x, z) is a hollow between the ridges, where the pines grow thickest and the ground is dark earth. */
    public static boolean hollow(long seed, int x, int z) {
        return ridge(seed, x, z) < 0.25;
    }

    /** How far the valley cuts the range at local (dx, dz): 0 on its floor, rising to 1 past its walls (and north of the cliff). */
    public static double valleyShare(int dx, int dz) {
        if (dz < COURT_START) return 1;
        return smooth(halfWidth(dz), halfWidth(dz) + WALL, Math.abs(dx));
    }

    /** The valley floor's half-width at local z: wide at the forecourt and the terrace, the road's between, easing from one to the next. */
    public static double halfWidth(int dz) {
        double court = 1 - smooth(COURT_END - 6, COURT_END + 14, dz);
        double terrace = smooth(TERRACE_START - 20, TERRACE_START, dz) * (1 - smooth(TERRACE_END, TERRACE_END + 20, dz));
        return ROAD_HALF + (COURT_HALF - ROAD_HALF) * court + (TERRACE_HALF - ROAD_HALF) * terrace;
    }

    /** Whether local (dx, dz) is on the valley's floor (forecourt, road and terrace), kept clear of trees and camps. */
    public static boolean onValleyFloor(int dx, int dz) {
        return dz >= COURT_START && dz <= MOUTH + 10 && Math.abs(dx) <= halfWidth(dz) + 1;
    }

    /** Whether local (dx, dz) is on the road itself, paved, up the valley's middle. */
    public static boolean onRoad(int dx, int dz) {
        return dz >= COURT_START && dz <= MOUTH + 10 && Math.abs(dx) <= 2;
    }

    /** The heights the valley's floor is laid at, worked out once for each region: before the door, across the terrace, and at the mouth. */
    public record Floors(int court, int terrace, int mouth) {}

    private static final Map<Long, Floors> FLOORS = new ConcurrentHashMap<>();

    /**
     * The floors of the region centred at (cx, cz): the mouth's at the land's own height there (by its
     * broad shape, {@code land}), never under {@code minFloor}; the forecourt's (the cave's floor) at the
     * land's at the centre, kept within {@value #COURT_BELOW} under and {@value #COURT_ABOVE} over the
     * mouth's, so neither ramp is steep; the terrace's two fifths of the way from the mouth's to it.
     */
    public static Floors floors(long seed, int cx, int cz, IntBinaryOperator land, int minFloor) {
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)), k -> {
            int mouth = Math.max(minFloor, land.applyAsInt(cx, cz + MOUTH));
            int court = Mth.clamp(Math.max(minFloor, land.applyAsInt(cx, cz)), mouth - COURT_BELOW, mouth + COURT_ABOVE);
            int terrace = mouth + Math.round((court - mouth) * 0.4f);
            return new Floors(court, terrace, mouth);
        });
    }

    public static final int COURT_BELOW = 8, COURT_ABOVE = 24;

    /**
     * The valley floor's height at local z: the forecourt's to {@link #COURT_END}, easing down (or up) to
     * the terrace's, level across it, then easing to the mouth's; NaN outside the valley's run.
     */
    public static double floorAt(Floors f, int dz) {
        if (dz < COURT_START) return f.court();
        if (dz <= COURT_END) return f.court();
        if (dz < TERRACE_START) return Mth.lerp(smooth(COURT_END, TERRACE_START, dz), f.court(), f.terrace());
        if (dz <= TERRACE_END) return f.terrace();
        if (dz < MOUTH) return Mth.lerp(smooth(TERRACE_END, MOUTH, dz), f.terrace(), f.mouth());
        return f.mouth();
    }

    /** How many blocks the range adds to the land at (x, z): 0 outside it. Before the valley's cut (see the range feature). */
    public static int riseAt(long seed, int x, int z) {
        double t = ZONE.depth(seed, x, z);
        if (t <= 0) return 0;
        double edge = smooth(0, EDGE, t);
        double r = ridge(seed, x, z);
        double crags = Math.max(0, noise(seed).getValue(x / 23.0 + 9.2, z / 23.0 - 3.3));
        double h = BASE + RIDGE * r * (0.6 + 0.4 * t) + CRAGS * r * crags;
        int[] c = ZONE.centre(seed, x, z);
        if (c != null) {
            int dx = x - c[0], dz = z - c[1] - PEAK_Z;
            double d = Math.sqrt((double) dx * dx + (double) dz * dz);
            // the peak: a cone with a rounded shoulder, its flanks broken by gullies
            double cone = 1 - smooth(0, PEAK_RADIUS, d);
            double gully = 0.82 + 0.18 * noise(seed).getValue(Math.atan2(dz, dx) * 3.2, d / 40.0);
            h += PEAK * cone * cone * gully + 40 * cone;
        }
        return (int) Math.round(edge * h);
    }

    // ── The camps ───────────────────────────────────────────────────────

    /** One watch-camp: its middle (world x, z); whether its band roams (a patrol). */
    public record Camp(int x, int z, boolean patrol) {}

    /** How far round its middle a camp reaches; camps are tried on a grid this wide. */
    public static final int CAMP_REACH = 7, CAMP_GRID = 84;

    private static final Map<Long, List<Camp>> CAMPS = new ConcurrentHashMap<>();

    private static int hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        return (int) Math.floorMod(h ^ (h >>> 29), 1000L);
    }

    /**
     * The watch-camps of the region centred at (cx, cz), from the seed alone: one tried in each cell of a
     * grid {@value #CAMP_GRID} blocks wide over the range, kept where the ground is fairly level, well
     * inside the region, off the peak and clear of the valley; about one band in three roams.
     */
    public static List<Camp> camps(long seed, int cx, int cz) {
        return CAMPS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)), key -> {
            List<Camp> out = new ArrayList<>();
            int reach = ZONE.layout().margin();
            for (int i = -reach / CAMP_GRID - 1; i <= reach / CAMP_GRID + 1; i++) {
                for (int j = -reach / CAMP_GRID - 1; j <= reach / CAMP_GRID + 1; j++) {
                    if (hash(seed, i, j, 1) >= 700) continue;
                    for (int t = 0; t < 5; t++) {
                        int x = cx + i * CAMP_GRID + 12 + hash(seed, i, j, 2 + 3 * t) % (CAMP_GRID - 24) - CAMP_GRID / 2;
                        int z = cz + j * CAMP_GRID + 12 + hash(seed, i, j, 3 + 3 * t) % (CAMP_GRID - 24) - CAMP_GRID / 2;
                        int dx = x - cx, dz = z - cz;
                        double d = ZONE.layout().depth(seed, new int[]{cx, cz}, x, z);
                        if (d < 0.12 || d > 0.8 || Math.hypot(dx, dz - PEAK_Z) < PEAK_RADIUS * 0.62) continue;
                        if (dz > COURT_START - 30 && Math.abs(dx) < TERRACE_HALF + WALL + 16) continue; // the valley is kept clear
                        if (!level(seed, x, z)) continue;
                        out.add(new Camp(x, z, hash(seed, i, j, 4) < 330));
                        break;
                    }
                }
            }
            return List.copyOf(out);
        });
    }

    /** Whether the range is fairly level round (x, z): no more than eight blocks' rise across a camp. */
    private static boolean level(long seed, int x, int z) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -CAMP_REACH; dx <= CAMP_REACH; dx += CAMP_REACH) for (int dz = -CAMP_REACH; dz <= CAMP_REACH; dz += CAMP_REACH) {
            int r = riseAt(seed, x + dx, z + dz);
            lo = Math.min(lo, r);
            hi = Math.max(hi, r);
        }
        return hi - lo <= 8;
    }

    /** The camp whose reach holds (x, z) in the region centred at (cx, cz), or null. */
    @Nullable
    public static Camp campAt(long seed, int cx, int cz, int x, int z) {
        for (Camp o : camps(seed, cx, cz)) {
            if (Math.abs(o.x() - x) <= CAMP_REACH && Math.abs(o.z() - z) <= CAMP_REACH) return o;
        }
        return null;
    }
}
