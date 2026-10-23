package com.jttw.engine.world.hell;

/**
 * The lie of the land in Hell (Diyu), worked out from a column's place alone (pure Java, the same
 * in every world, as Heaven's islands are): plains of soul ash rolling a few blocks either way,
 * hills of black stone rising out of them, side rivers winding through, and across all Hell from
 * west to east the Naihe (奈何), the river every soul must cross, by the one bridge, on its way to
 * the Ten Courts.
 *
 * <p>Three places are levelled at {@link #GROUND}: the arrival plaza round the Gate of Ghosts
 * ({@link #ARRIVAL_X}, {@link #ARRIVAL_Z}), where travellers come in and where the Book of Life and
 * Death is kept; the road north from it; and the ground of the Ten Courts' fortress
 * ({@link TenCourts}). The Naihe cuts the road; the Bridge of Helplessness spans it there.
 *
 * <p>{@code HellHeightDensity} hands {@link #height} to the dimension's noise generator, whose
 * surface rules lay the ash, black stone and soul soil, and whose sea level fills the rivers.
 */
public final class HellTerrain {

    private HellTerrain() {}

    /** The rivers' water reaches up to y {@code SEA_LEVEL - 1}. */
    public static final int SEA_LEVEL = 37;
    /** The top block of the plain's middle height, the plaza, the road and the fortress's ground. */
    public static final int GROUND = 42;
    /** Where travellers arrive (JTTWDimensions.findSafeSpawn searches out from here). */
    public static final int ARRIVAL_X = 0, ARRIVAL_Z = 0;
    /** The plaza is level out to this far from the arrival, and blends into the land by this far. */
    public static final double PLAZA_FLAT = 40, PLAZA_BLEND = 72;
    /** The Naihe: its course's mean line (z), its water's half width, and the slope of its banks (rise a block). */
    public static final int NAIHE_Z = -160, NAIHE_HALF = 10;
    public static final double BANK_SLOPE = 0.7;
    /** The road: level within this many blocks of its middle (x 0), blending into the land by this far. */
    public static final int ROAD_HALF = 4, ROAD_BLEND = 14;
    /** Side rivers keep this far from the plaza, the road and the fortress. */
    public static final double SIDE_RIVERS_AWAY = 120;

    /** Where the Naihe's middle runs at {@code x}: wandering either side of {@link #NAIHE_Z}, and at it where the road crosses (x 0). */
    public static double naiheCentre(double x) {
        return NAIHE_Z + 34 * Math.sin(x / 140.0) + 12 * Math.sin(x / 53.0);
    }

    /** How far (x, z) is from the Naihe's middle, north or south. */
    public static double naiheDistance(double x, double z) {
        return Math.abs(z - naiheCentre(x));
    }

    /** How deep the land may be at this distance from the Naihe's middle: its bed, then its banks rising. */
    public static double naiheFloor(double d) {
        if (d < NAIHE_HALF) return SEA_LEVEL - 6 + 5 * (d / NAIHE_HALF) * (d / NAIHE_HALF);
        return SEA_LEVEL - 1 + (d - NAIHE_HALF) * BANK_SLOPE;
    }

    /** Whether (x, z) lies along the road: x near 0, between the plaza and the fortress gate. */
    public static double roadDistance(double x, double z) {
        if (z > ARRIVAL_Z) return Math.hypot(x, z - ARRIVAL_Z);
        if (z < TenCourts.GATE_Z) return Math.hypot(x, TenCourts.GATE_Z - z);
        return Math.abs(x);
    }

    /** How much (x, z) belongs to a levelled place (plaza, road, fortress): 1 within, 0 well away. */
    public static double levelled(double x, double z) {
        double plaza = 1 - smooth(PLAZA_FLAT, PLAZA_BLEND, Math.hypot(x - ARRIVAL_X, z - ARRIVAL_Z));
        double road = 1 - smooth(ROAD_HALF, ROAD_BLEND, roadDistance(x, z));
        double fort = 1 - smooth(4, 40, TenCourts.distanceOutside(x, z));
        return Math.max(plaza, Math.max(road, fort));
    }

    /** How far (x, z) is from the plaza, the road and the fortress, whichever is nearest. */
    public static double distanceFromWorks(double x, double z) {
        return Math.min(Math.hypot(x - ARRIVAL_X, z - ARRIVAL_Z), Math.min(roadDistance(x, z), TenCourts.distanceOutside(x, z)));
    }

    /** The height of the land's top block at (x, z) (fractional; the generator rounds it). */
    public static double height(double x, double z) {
        // the ash plains: a gentle roll
        double h = GROUND + 2.5 * fbm(11, x / 60, z / 60, 3) + 1.0 * noise(12, x / 17, z / 17);
        // hills of black stone where the broad noise runs high, ridged
        double hills = smooth(0.12, 0.55, fbm(21, x / 260, z / 260, 2));
        if (hills > 0) h += hills * (14 + 12 * (1 - Math.abs(noise(22, x / 45, z / 45))) + 4 * noise(23, x / 13, z / 13));
        // side rivers winding where a broad noise crosses zero, kept away from the works
        double away = smooth(SIDE_RIVERS_AWAY, SIDE_RIVERS_AWAY + 60, distanceFromWorks(x, z));
        if (away > 0) {
            double t = Math.abs(fbm(31, x / 210, z / 210, 2));
            double channel = 1 - smooth(0.015, 0.06, t);
            if (channel > 0) h += (SEA_LEVEL - 4 - h) * channel * away;
        }
        // the plaza, the road and the fortress's ground: level
        double level = levelled(x, z);
        if (level > 0) h += (GROUND - h) * level;
        // the Naihe cuts through everything, the road too (the bridge spans it)
        h = Math.min(h, naiheFloor(naiheDistance(x, z)));
        return Math.max(8, Math.min(100, h));
    }

    /** The top block's height at (x, z), as the generator makes it. */
    public static int surface(int x, int z) {
        return (int) Math.floor(height(x, z) + 0.5);
    }

    /** Whether (x, z) is under a river's water. */
    public static boolean underWater(int x, int z) {
        return surface(x, z) < SEA_LEVEL - 1;
    }

    // ── The Bridge of Helplessness ──────────────────────────────────────

    /** Half the bridge's length: over the water and down the banks to where the land is at the road's height again. */
    public static final int BRIDGE_HALF = NAIHE_HALF + (int) Math.ceil((GROUND - (SEA_LEVEL - 1)) / BANK_SLOPE) + 1;

    /** The bridge's ends (z: the north end first), where the road meets it. */
    public static int[] bridgeEnds() {
        int c = (int) Math.round(naiheCentre(0));
        return new int[]{c - BRIDGE_HALF, c + BRIDGE_HALF};
    }

    /** How high the bridge's walk rises at {@code z}, in half blocks over the road's: an arch, 6 at its middle. */
    public static int bridgeRise(int z) {
        int[] br = bridgeEnds();
        double mid = (br[0] + br[1]) / 2.0, t = (z - mid) / (BRIDGE_HALF + 0.5);
        return (int) Math.round(6 * (1 - t * t));
    }

    // ── noise ───────────────────────────────────────────────────────────

    static double smooth(double from, double to, double v) {
        if (v <= from) return 0;
        if (v >= to) return 1;
        double t = (v - from) / (to - from);
        return t * t * (3 - 2 * t);
    }

    private static long hash(long salt, long a, long b) {
        long z = salt * 0x632BE59BD9B4E019L + a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double lattice(long salt, long a, long b) {
        return (hash(salt, a, b) >>> 11) * 0x1.0p-52 - 1; // [-1, 1)
    }

    /** Smooth value noise in [-1, 1], one unit to a lattice cell. */
    static double noise(long salt, double x, double z) {
        long xi = (long) Math.floor(x), zi = (long) Math.floor(z);
        double fx = x - xi, fz = z - zi;
        double u = fx * fx * fx * (fx * (fx * 6 - 15) + 10), v = fz * fz * fz * (fz * (fz * 6 - 15) + 10);
        double a = lattice(salt, xi, zi), b = lattice(salt, xi + 1, zi), c = lattice(salt, xi, zi + 1), d = lattice(salt, xi + 1, zi + 1);
        return (a + (b - a) * u) + ((c + (d - c) * u) - (a + (b - a) * u)) * v;
    }

    /** A few octaves of {@link #noise}, about [-1, 1]. */
    static double fbm(long salt, double x, double z, int octaves) {
        double sum = 0, amp = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise(salt + o * 101, x, z);
            norm += amp;
            amp *= 0.5;
            x *= 2.03;
            z *= 2.03;
        }
        return sum / norm;
    }

    /** A hash of a block for choices made block by block: [0, 1). */
    public static double grain(int x, int y, int z) {
        return (hash(0xD1EBL, x * 31L + y, z * 17L - y) >>> 11) * 0x1.0p-53;
    }
}
