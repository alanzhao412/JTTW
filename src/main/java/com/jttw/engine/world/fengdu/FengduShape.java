package com.jttw.engine.world.fengdu;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The shape of Fengdu's hill (as Fengdu stands on Mount Ming over the Yangtze), worked out from the
 * seed and the region alone, so every chunk agrees on it whichever is made first (pure: unit-tested):
 * <ul>
 *   <li><b>its top</b> ({@link #onTop}): a level top, a rounded square {@value #TOP} blocks out
 *       from the region's centre each way, room for the city, its archway and a ring of ground
 *       round its walls;</li>
 *   <li><b>its slopes</b> ({@link #share}): from there the ground falls (or climbs) back to the
 *       land's own over {@value #SLOPE} blocks or so, the line of its foot wandering a little;</li>
 *   <li><b>its height</b> ({@link #cityFloor}): the land's broad height at the centre or round its
 *       foot, whichever is higher, and {@value #RISE} more, kept over the sea and well under the sky;</li>
 *   <li><b>the Road to the Yellow Springs</b> ({@link #onRoad}): the paved way up the slope to the
 *       city's gate, from the south.</li>
 * </ul>
 * Local coordinates are block offsets from the region's centre (the main gate toward +z, south).
 */
public final class FengduShape {

    private FengduShape() {}

    public static final Zones.Zone ZONE = Zones.FENGDU;

    /** The hill's level top (from the centre, as a rounded square), and its slopes' run. */
    public static final int TOP = 49, SLOPE = 44;
    /** How far its foot's line wanders either way. */
    public static final int WANDER = 6;
    /** How far the hill stands over the land at its centre. */
    public static final int RISE = 14;
    /** The city's floor is never lower than the sea and this, nor higher than the sky less this. */
    public static final int ABOVE_SEA = 8, UNDER_SKY = 96;
    /** The road: this wide either side of its middle (x 0). */
    public static final int ROAD_HALF = 2;
    /** The farthest anything of the hill reaches from the centre. */
    public static final int REACH = TOP + SLOPE + WANDER;

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0xF3D6D0C1L));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** How far out local (dx, dz) is, as a rounded square: the walls' corners no farther than their sides by much. */
    public static double reach(int dx, int dz) {
        double a = Math.abs(dx), b = Math.abs(dz);
        return Math.sqrt(Math.sqrt(a * a * a * a + b * b * b * b));
    }

    /** Whether local (dx, dz) is on the hill's level top. */
    public static boolean onTop(int dx, int dz) {
        return reach(dx, dz) <= TOP;
    }

    /** How much of the land's own height (x, z) keeps, local (dx, dz): 0 on the top, rising down the slope to 1 past its foot. */
    public static double share(long seed, int x, int z, int dx, int dz) {
        double r = reach(dx, dz);
        if (r <= TOP) return 0;
        double wander = WANDER * noise(seed).getValue(x / 38.0 + 5.3, z / 38.0 - 8.1) * smooth(TOP, TOP + 12, r);
        return smooth(TOP, TOP + SLOPE, r + wander);
    }

    /** How patchy the ground is at (x, z), about -1..1: high, it goes dark (podzol); low, bare (coarse earth). Broad, soft patches. */
    public static double patch(long seed, int x, int z) {
        return noise(seed).getValue(x / 16.0 + 31.7, z / 16.0 - 12.9) * 0.8 + noise(seed).getValue(x / 5.0 - 3.3, z / 5.0 + 9.1) * 0.25;
    }

    /** The height the ground at local (dx, dz) is brought to: the floor on the top, the land's own ({@code natural}) past the foot, a slope between. */
    public static int groundAt(int natural, int floor, double share) {
        return (int) Math.round(Mth.lerp(share, floor, natural));
    }

    /** Whether local (dx, dz) is on the Road to the Yellow Springs: from the gate down the slope south, to its foot. */
    public static boolean onRoad(int dx, int dz) {
        return Math.abs(dx) <= ROAD_HALF && dz > FengduCity.OUTER && dz <= TOP + SLOPE + WANDER;
    }

    /** The cached city floor of each region, by its centre. */
    private static final Map<Long, Integer> FLOORS = new ConcurrentHashMap<>();

    /**
     * The top of the city's floor in the region centred at (cx, cz): the land's broad height
     * ({@code land}) at the centre or, if higher, on average round the hill's foot (so the city
     * stands over the country round it, not in a bowl of it), and {@value #RISE} more; between the
     * sea's level and a little ({@code minFloor}) and well under the sky ({@code maxFloor}). Worked
     * out once for each region.
     */
    public static int cityFloor(long seed, int cx, int cz, IntBinaryOperator land, int minFloor, int maxFloor) {
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)), k -> {
            int foot = 0, r = TOP + SLOPE;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                foot += land.applyAsInt(cx + (int) Math.round(Math.cos(a) * r), cz + (int) Math.round(Math.sin(a) * r));
            }
            return Mth.clamp(Math.max(land.applyAsInt(cx, cz), Math.round(foot / 8f)) + RISE, minFloor, maxFloor);
        });
    }
}
