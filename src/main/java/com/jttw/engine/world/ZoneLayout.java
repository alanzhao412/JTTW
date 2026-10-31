package com.jttw.engine.world;

/**
 * Where a zone's regions lie: one region a world (or, for the Four Seas, one in each quarter of the
 * compass round the origin), a rough circle centred somewhere in a ring round the origin, between
 * {@code near} and {@code far} blocks out: the higher the zone's levels, the farther out, so the
 * journey west lengthens as the pilgrim grows. Where in the ring follows from the seed: the places
 * this layout offers ({@link #candidate}) are tried in turn, and the first clear of other zones and
 * on host ground is taken ({@link ZoneMap}).
 *
 * @param near    the least distance from the origin of a region's centre, in blocks
 * @param far     the greatest (unless no place in the ring will do: then the search widens outward)
 * @param radius  rough radius of a region, in blocks
 * @param wobble  how far its edge strays from the circle, in blocks
 * @param salt    keeps zones of different kinds from trying the same places
 * @param perSea  one region in each quarter of the compass (east, south, west, north), not one in all
 * @param quarter the one quarter of the compass its region is tried in (0 east, 1 south, 2 west, 3
 *                north, as {@link #quarterOf}), as a sea's is; {@link #ANY_QUARTER} for anywhere round
 */
public record ZoneLayout(int near, int far, int radius, int wobble, long salt, boolean perSea, int quarter) {

    /** No quarter of its own: its region may lie anywhere round the ring. */
    public static final int ANY_QUARTER = -1;
    /** The quarter of the compass to the west (-x), where the journey goes. */
    public static final int WEST = 2;

    /** A layout whose region may lie anywhere round its ring (or, {@code perSea}, one in each quarter). */
    public ZoneLayout(int near, int far, int radius, int wobble, long salt, boolean perSea) {
        this(near, far, radius, wobble, salt, perSea, ANY_QUARTER);
    }

    /**
     * White Bone Ridge, the first band: a region about 840 blocks across (never under 720), room for
     * the bone mountain over the White Bone Cave at its heart, the road up to its door and the
     * outposts round it; 1-2 km from spawn.
     */
    public static final ZoneLayout WHITE_BONE = new ZoneLayout(1000, 2000, 420, 60, 0x5EEDB0E1L, false);

    /**
     * The Bottomless Cave: a region about 380 blocks across, never less than 330 (radius less all its
     * wobble), round Pit-Hole Mountain, its road and outposts, and the fortress under it (its widest
     * floor some 200 blocks across); 2-3 km out.
     */
    public static final ZoneLayout BOTTOMLESS_CAVE = new ZoneLayout(2000, 3000, 190, 25, 0xB0770C4EL, false);

    /**
     * The Mystical Waters: four regions about 440 blocks across (a trench and its palace), one in each
     * sea, east, south, west and north of spawn, 3-4.5 km out.
     */
    public static final ZoneLayout MYSTICAL_WATERS = new ZoneLayout(3000, 4500, 220, 45, 0x5EA5D4A6L, true);

    /**
     * The Flaming Mountains: a range about 640 blocks across (never under 540), room for the demons'
     * outposts over it and the ravine up to the Fire Cloud Cave's door; 4.5-6 km out.
     */
    public static final ZoneLayout FIRE_MOUNTAIN = new ZoneLayout(4500, 6000, 320, 50, 0xF1A3E5A7L, false);

    /**
     * Mount Wuzhuang: a region about 260 blocks across (never under 224), round a floating island about
     * 150 blocks across and the cloud stair beyond its rim; 6-7.5 km out.
     */
    public static final ZoneLayout MOUNT_WUZHUANG = new ZoneLayout(6000, 7500, 128, 16, 0x3A1FC10DL, false);

    /**
     * Lion-Camel Ridge, the last band: a region about 680 blocks across (never under 600): the city,
     * the mountain behind it with the demon kings' fortress in it, and the ridge round them crawling
     * with their hordes, its roads and outposts; 7.5-9 km out.
     */
    public static final ZoneLayout LION_CAMEL = new ZoneLayout(7500, 9000, 340, 40, 0x11C0CA3EL, false);

    /**
     * Fengdu, the Ghost City, the gateway to Hell: no band of its own; a region about 240 blocks
     * across (never under 208) round its hill and the city on it, past every zone, 9-10.5 km out.
     */
    public static final ZoneLayout FENGDU = new ZoneLayout(9000, 10500, 120, 16, 0xF3D6D0C1L, false);

    /**
     * Mount Kunlun, the far-western massif: no band of its own; a region about 930 blocks across
     * (never under 870) round its range, 5-8 km out and within 30 degrees of due west, as a sea's
     * region keeps to its quarter (widening, it keeps to the west still).
     */
    public static final ZoneLayout KUNLUN = new ZoneLayout(5000, 8000, 465, 30, 0x6B0A1F5EL, false, WEST);

    /**
     * Flower-Fruit Mountain, in White Bone Ridge's band and ring: a region about 640 blocks across
     * (never under 580), the island and the sea round it; 1-2 km out, in the sea, clear of the ridge
     * and Black Wind Mountain.
     */
    public static final ZoneLayout FLOWER_FRUIT = new ZoneLayout(1000, 2000, 320, 30, 0xF10E7F2BL, false);

    /**
     * Silk-Web Ridge and the Spider Cave, Mount Wuzhuang's band on the ground: a region about 480 blocks
     * across (never under 430), its valley, the ridge across it with the cave inside, the cottage, the
     * spring and the temple; 6-7.5 km out, as Mount Wuzhuang (whose region it keeps clear of).
     */
    public static final ZoneLayout SPIDER_CAVE = new ZoneLayout(6000, 7500, 240, 24, 0x5B1DC0FEL, false);

    /**
     * Black Wind Mountain, in White Bone Ridge's band and ring: a region about 700 blocks across (never
     * under 640), room for the dark range, the black peak with the Black Wind Cave in its cliff, the
     * valley road up to its door past the burnt Guanyin Chan Temple, and the bear's camps; 1-2 km out,
     * clear of the ridge.
     */
    public static final ZoneLayout BLACK_WIND = new ZoneLayout(1000, 2000, 350, 30, 0xB1AC3B1DL, false);

    /**
     * Each quarter's half-width, in degrees, a sea's region (or one kept to a quarter) is tried in:
     * well inside its 45, so the four lie plainly round the compass.
     */
    private static final double SEA_HALF_ARC = 30;

    /** How many regions a world holds: four for the Four Seas, else one. */
    public int regions() {
        return perSea ? 4 : 1;
    }

    /** The farthest a region reaches from its centre: its radius and all its wobble. */
    public int margin() {
        return radius + wobble;
    }

    /**
     * The {@code i}th place tried for region {@code region} (for the seas, its quarter: 0 east, 1
     * south, 2 west, 3 north, as {@code Sea}; for a layout kept to a quarter, in that one), between {@code from} and {@code to} blocks out, as {x, z}.
     * Spread evenly over the ring's area, by the seed alone.
     */
    public int[] candidate(long seed, int region, int i, int from, int to) {
        long h = mix(seed ^ salt, region * 1_000_003L + i, from);
        int q = perSea ? region : quarter;
        double a = q >= 0 ? Math.toRadians(q * 90 + (unit(mix(h, 1, 0)) * 2 - 1) * SEA_HALF_ARC)
                : unit(mix(h, 1, 0)) * 2 * Math.PI;
        double d = Math.sqrt((double) from * from + unit(mix(h, 2, 0)) * ((double) to * to - (double) from * from));
        return new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)};
    }

    /** The quarter of the compass (x, z) lies in from the origin: 0 east, 1 south (+z), 2 west, 3 north, as {@code Sea.of}. */
    public static int quarterOf(int x, int z) {
        if (Math.abs(x) >= Math.abs(z)) return x >= 0 ? 0 : 2;
        return z >= 0 ? 1 : 3;
    }

    /**
     * A key to the region centred at (x, z), as one number, the same for any of its points near the
     * centre: its quarter for the seas, else 0 (a world holds one). The name is the old cell grid's.
     */
    public long cellOf(int x, int z) {
        return perSea ? quarterOf(x, z) : 0;
    }

    /** How far into the region centred at {@code c} (x, z) lies: 0 at its edge or outside, rising to 1 at its centre. */
    public double depth(long seed, int[] c, int x, int z) {
        double dx = x - (double) c[0], dz = z - (double) c[1];
        double d2 = dx * dx + dz * dz;
        double margin = margin();
        if (d2 > margin * margin) return 0;
        long h = mix(seed ^ salt, c[0], c[1]);
        double a = Math.atan2(dz, dx);
        double edge = radius + wobble * (0.5 * Math.sin(3 * a + 6.283 * unit(mix(h, 3, 0)))
                + 0.3 * Math.sin(5 * a + 6.283 * unit(mix(h, 4, 0)))
                + 0.2 * Math.sin(7 * a + 6.283 * unit(mix(h, 5, 0))));
        double d = Math.sqrt(d2);
        return d >= edge ? 0 : 1 - d / edge;
    }

    static long mix(long base, long a, long b) {
        long z = base + a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A hash as a number in [0, 1). */
    static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
