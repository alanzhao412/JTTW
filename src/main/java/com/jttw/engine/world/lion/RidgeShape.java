package com.jttw.engine.world.lion;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The shape of Lion-Camel Ridge, worked out from the seed and the region alone, so every chunk
 * agrees on it whichever is made first (pure: unit-tested):
 * <ul>
 *   <li><b>the crags</b> ({@link #riseAt}): over the mountains it takes, the ridge is broken into
 *       grim crags, ridged noise adding up to {@value #CRAGS} blocks along its crest lines, climbing
 *       out of the land over the outer {@value #EDGE_PERCENT}% of the way in;</li>
 *   <li><b>the city's ground</b> ({@link #cityShare}): Lion-Camel City stands on a level apron cut
 *       into (or built up on) the mountain round the region's centre, {@value #APRON} blocks out
 *       from it each way (a square, as the walls are), and from there the ground climbs or falls
 *       back to the ridge's over {@value #BLEND} blocks;</li>
 *   <li><b>its level</b> ({@link #cityFloor}): the land's broad height at the centre, kept between
 *       the sea and well under the sky, and high enough over the world's floor for the fortress
 *       under it ({@link LionFortress}, {@value #FORTRESS_ROOM} blocks of it);</li>
 *   <li><b>the forecourt</b> behind the city (north), before the fortress's great door, level with
 *       the city ({@link #cityShare} counts it as the apron's);</li>
 *   <li><b>the mountain</b> over the fortress ({@link #massifAt}): a great crag rising from a cliff
 *       across the forecourt's end (the door in it) to {@value #MASSIF_H} blocks over the city, its
 *       back falling away north to the ridge's ground.</li>
 * </ul>
 * Local coordinates are block offsets from the region's centre (the main gate toward +z, south).
 */
public final class RidgeShape {

    private RidgeShape() {}

    public static final Zones.Zone ZONE = Zones.LION_CAMEL;

    /** The share of the way in over which the crags climb out of the land, as a percentage, and as a share. */
    public static final int EDGE_PERCENT = 18;
    private static final double EDGE = EDGE_PERCENT / 100.0;
    /** How high the crags rise over the land at most. */
    public static final int CRAGS = 30;
    /** The city's level apron (from the centre, each way) and the ground's climb back to the ridge's. */
    public static final int APRON = LionCamelCity.OUTER + 6, BLEND = 40;
    /** The city's floor is never lower than the sea and this, nor higher than the sky less this, nor than {@value #HIGHEST} (the sky may be raised). */
    public static final int ABOVE_SEA = 6, UNDER_SKY = 72, HIGHEST = 228;
    /** How much room the fortress needs under the city's floor, over the world's floor. */
    public static final int FORTRESS_ROOM = -LionFortress.BOTTOM + 4;
    /** The mountain over the fortress: its height over the city at most, its half-widths (x, z), its middle (local z), and its cliff's foot (local z, along its straight middle stretch of {@value #FACE_FLAT} each way). */
    public static final int MASSIF_H = 48, MASSIF_RX = 102, MASSIF_RZ = 80, MASSIF_Z = -122, FACE_Z = LionFortress.DOOR_LZ - 1, FACE_FLAT = 44;
    /** The level forecourt: half its width, its north and south ends (local z). */
    public static final int COURT_HALF = 40, COURT_N = LionFortress.DOOR_LZ - 3, COURT_S = -56;

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0x11C0CA3EL));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** The crags' crest at (x, z): 1 on a crest line, falling away fast to 0 between. */
    public static double crest(long seed, int x, int z) {
        double n = noise(seed).getValue(x / 70.0 - 13.1, z / 70.0 + 41.7);
        double r = 1 - Math.abs(n);
        return r * r * r;
    }

    /** How many blocks the crags add to the land at (x, z), the city aside: 0 outside the zone. */
    public static int cragAt(long seed, int x, int z) {
        double t = ZONE.depth(seed, x, z);
        if (t <= 0) return 0;
        double edge = smooth(0, EDGE, t);
        double tops = 0.55 + 0.45 * noise(seed).getValue(x / 23.0 + 7.7, z / 23.0 - 2.9); // jagged tops along them
        return (int) Math.round(edge * CRAGS * crest(seed, x, z) * tops);
    }

    /** How far from the city local (dx, dz) is: 0 on its apron (the forecourt behind it too), rising to 1 where the ridge's own ground is. */
    public static double cityShare(int dx, int dz) {
        int q = Math.max(Math.abs(dx), Math.abs(dz));
        int court = Math.max(Math.max(Math.abs(dx) - COURT_HALF, 0), Math.max(COURT_N - dz, dz - COURT_S));
        return Math.min(smooth(APRON, APRON + BLEND, q), smooth(0, BLEND, Math.max(court, 0)));
    }

    /** Whether local (dx, dz) is on the city's level apron (or the forecourt). */
    public static boolean onApron(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz)) <= APRON
                || Math.abs(dx) <= COURT_HALF && dz >= COURT_N && dz <= COURT_S;
    }

    /**
     * How high the mountain over the fortress stands over the city's floor at local (dx, dz): 0 off
     * it and before its cliff; a rough dome, its crest broken by the ridge's noise, its back falling
     * to the ridge's own ground.
     */
    public static int massifAt(long seed, int dx, int dz) {
        int off = Math.max(0, Math.abs(dx) - FACE_FLAT);
        if (dz > FACE_Z - off * off / 60) return 0;
        double ex = dx / (double) MASSIF_RX, ez = (dz - MASSIF_Z) / (double) MASSIF_RZ;
        double e2 = ex * ex + ez * ez;
        if (e2 >= 1) return 0;
        double shape = Math.pow(1 - e2, 0.7);
        double n = noise(seed).getValue(dx / 17.0 + 3.1, dz / 17.0 - 8.3);
        double crag = 0.9 + 0.1 * n + 0.25 * crest(seed, dx * 2 + 9000, dz * 2 - 9000);
        return (int) Math.round(MASSIF_H * shape * crag);
    }

    /**
     * How much the mountain over the fortress has its way at local (dx, dz), 0 to 1: wholly over its
     * middle and at its cliff, giving way to the ridge's own ground toward its back and sides, so it
     * rises out of the ridge rather than standing on it like a drum.
     */
    public static double massifBlend(int dx, int dz) {
        double ex = dx / (double) MASSIF_RX, ez = (dz - MASSIF_Z) / (double) MASSIF_RZ;
        double e = Math.sqrt(ex * ex + ez * ez);
        if (dz > MASSIF_Z + MASSIF_RZ * 0.35) e = Math.min(e, Math.abs(ex) * 1.15); // its front: a cliff all across
        return 1 - smooth(0.62, 1.0, e);
    }

    /** How many blocks the crags add at (x, z), less where the city's ground takes over (local to {@code c}, the region's centre). */
    public static int riseAt(long seed, int x, int z, int[] c) {
        int crag = cragAt(seed, x, z);
        if (c == null) return crag;
        return (int) Math.round(crag * cityShare(x - c[0], z - c[1]));
    }

    /** The height the ground at (x, z) is brought to: the city's floor on its apron, the ridge's own ({@code natural}, crags and all) past it, between them a slope. */
    public static int groundAt(int natural, int floor, int dx, int dz) {
        return (int) Math.round(Mth.lerp(cityShare(dx, dz), floor, natural));
    }

    /** The cached city floor of each region, by its centre. */
    private static final Map<Long, Integer> FLOORS = new ConcurrentHashMap<>();

    /**
     * The top of the city's floor in the region centred at (cx, cz): the land's broad height there
     * ({@code land}), between the sea's level and a little ({@code minFloor}) and well under the
     * sky ({@code maxFloor}). Worked out once for each region.
     */
    public static int cityFloor(long seed, int cx, int cz, IntBinaryOperator land, int minFloor, int maxFloor) {
        int top = Math.max(minFloor, maxFloor);
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)),
                k -> Mth.clamp(land.applyAsInt(cx, cz), minFloor, top));
    }
}
