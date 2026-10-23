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
 *       the sea and well under the sky.</li>
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
    /** The city's floor is never lower than the sea and this, nor higher than the sky less this. */
    public static final int ABOVE_SEA = 6, UNDER_SKY = 72;

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

    /** How far from the city local (dx, dz) is: 0 on its apron, rising to 1 where the ridge's own ground is. */
    public static double cityShare(int dx, int dz) {
        int q = Math.max(Math.abs(dx), Math.abs(dz));
        return smooth(APRON, APRON + BLEND, q);
    }

    /** Whether local (dx, dz) is on the city's level apron. */
    public static boolean onApron(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz)) <= APRON;
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
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)),
                k -> Mth.clamp(land.applyAsInt(cx, cz), minFloor, maxFloor));
    }
}
