package com.jttw.engine.world.waters;

import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import javax.annotation.Nullable;

/**
 * The shape of the Mystical Waters' trench, from the region (how far into it, its centre) and the
 * seed alone, so every chunk agrees on it whichever is made first: its floor just over the world's
 * bedrock, its walls falling near-sheer from the sea floor a few blocks in from the region's edge,
 * their line wandering, broken by ledges; the floor rolling in low mounds, laid level round the
 * palace. Laid by {@code TrenchFeature}.
 */
public final class TrenchShape {

    private TrenchShape() {}

    /** The zone it sinks. */
    public static final Zones.Zone ZONE = Zones.MYSTICAL_WATERS;
    /** The floor's top over the world's bottom: just over the bedrock (which reaches 4 up). */
    public static final int FLOOR_OVER_BOTTOM = 6;
    /** Where, by depth into the region, the walls begin to fall and where they reach the floor. */
    public static final double RIM = 0.02, FOOT = 0.16;
    /** The height of the walls' ledges. */
    private static final double LEDGE = 12;
    /** How much the floor rises in its mounds, at most. */
    public static final int MOUNDS = 6;

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0x7E2C4DA1L));
            noiseSeed = seed;
        }
        return noise;
    }

    private static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** The trench floor's top over a world whose bottom is {@code minY}, before its mounds. */
    public static int floorY(int minY) {
        return minY + FLOOR_OVER_BOTTOM;
    }

    /**
     * The top of the ground at (x, z) once the trench is sunk, where the sea floor's top was
     * {@code top}: the floor (and its mounds), or on its walls a height between; {@code top} outside it.
     */
    public static int groundAt(long seed, int x, int z, int top, int minY) {
        double t = ZONE.depth(seed, x, z);
        if (t <= 0) return top;
        SimplexNoise n = noise(seed);
        // the walls' line wanders, broadly and in small bites
        double tt = t + 0.035 * n.getValue(x / 52.0, z / 52.0) + 0.012 * n.getValue(x / 11.0 + 40, z / 11.0 - 40);
        double fall = smooth(RIM, FOOT, tt);
        if (fall <= 0) return top;
        double floor = floorY(minY) + mounds(seed, x, z) * (1 - levelled(seed, x, z));
        double y = top - (top - floor) * fall;
        if (fall < 1) {
            // ledges: the wall pulled toward a level every LEDGE blocks, most of the way down
            double ledge = Math.floor(y / LEDGE) * LEDGE;
            y += (ledge - y) * 0.6 * Math.sin(Math.PI * fall);
        }
        return (int) Math.round(Math.max(floor, Math.min(top, y)));
    }

    /** The floor's mounds at (x, z), 0 to {@value #MOUNDS} blocks. */
    private static double mounds(long seed, int x, int z) {
        SimplexNoise n = noise(seed);
        double broad = Math.max(0, n.getValue(x / 34.0 + 7, z / 34.0 + 3));
        double small = Math.max(0, n.getValue(x / 9.0 - 21, z / 9.0 + 17) - 0.45);
        return Math.min(MOUNDS, broad * 4.5 + small * 3);
    }

    /** How level the floor is laid at (x, z): 1 round the palace, falling to 0 further off. */
    private static double levelled(long seed, int x, int z) {
        double d = ZONE.distanceToCentre(seed, x, z);
        return 1 - smooth(DragonPalace.LEVEL_RADIUS, DragonPalace.LEVEL_RADIUS + 14, d);
    }

    /** The throne of the palace in the region holding (x, z), on the floor's top; null where there is none. */
    @Nullable
    public static BlockPos throne(long seed, int x, int z, int minY) {
        int[] c = ZONE.centre(seed, x, z);
        return c == null ? null : new BlockPos(c[0], floorY(minY), c[1]);
    }

}
