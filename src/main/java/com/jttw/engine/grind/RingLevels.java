package com.jttw.engine.grind;

/**
 * The levels of the open world's grinding grounds (the raided villages and the infested wilds), by
 * how far from spawn (the world's origin, round which the zones' rings lie) they stand: the farther
 * out, the higher, along the zones' own rings ({@code ZoneLayout}): within 1 km the early road's
 * levels 1-20; White Bone Ridge's ring (1-2 km) its band, 20-36; the Bottomless Cave's (2-3 km)
 * 37-46; the Mystical Waters' (3-4.5 km) 47-56 in its nearer half and Hell's 57-66 (Hell has no
 * ring of its own) in its farther; the Flaming Mountains' (4.5-6 km) 67-77; Mount Wuzhuang's and
 * Silk-Web Ridge's (6-7.5 km) 78-88; Lion-Camel Ridge's (7.5-9 km) 89-100; and 100 beyond. Within a
 * ring the level climbs smoothly ({@link #levelAt}); a place's band is {@value #WIDTH} levels round
 * that, jittered a little by where it is ({@link #bandAt}), and fixed: the same place, the same band.
 */
public final class RingLevels {

    private RingLevels() {}

    /** Distances from the origin (blocks) and the level there; straight lines between. */
    static final int[] DIST = {0, 1000, 2000, 3000, 3750, 4500, 6000, 7500, 9000};
    static final int[] LEVEL = {1, 20, 37, 47, 57, 67, 78, 89, 100};

    /** How many levels wide a place's band is, and how far its middle may stray from its ring's level either way. */
    public static final int WIDTH = 7, JITTER = 2;
    /** The players' range of levels. */
    public static final int LOWEST = 1, HIGHEST = 100;

    /** A band of levels, {@code lo} to {@code hi} inclusive. */
    public record Band(int lo, int hi) {
        public boolean contains(int level) {
            return level >= lo && level <= hi;
        }

        /** A captain's level over a regular of {@code regular}: three to five over, never more than one over the band's top. */
        public int captainOver(int regular, int roll) {
            return Math.min(regular + 3 + Math.floorMod(roll, 3), hi + 1);
        }

        /** A miniboss's level: one over the band's top, as a zone's elite trainers stand at most. */
        public int miniboss() {
            return hi + 1;
        }

        /** A raid's leader's level: the band's top and three, as a zone's gym leader stands over its band. */
        public int leader() {
            return hi + 3;
        }

        /** The level of character a leader of this band is built for: the band's top (the players' cap, past it). */
        public int challenger() {
            return Math.min(hi, HIGHEST);
        }

        @Override
        public String toString() {
            return lo + "–" + hi;
        }
    }

    /** The ring's level at {@code distance} blocks from the origin, smoothly between the rings' marks; 100 past the last. */
    public static double levelAt(double distance) {
        if (distance <= 0) return LEVEL[0];
        for (int i = 1; i < DIST.length; i++) {
            if (distance <= DIST[i]) {
                double t = (distance - DIST[i - 1]) / (DIST[i] - DIST[i - 1]);
                return LEVEL[i - 1] + t * (LEVEL[i] - LEVEL[i - 1]);
            }
        }
        return HIGHEST;
    }

    /** The band of a place at (x, z): {@value #WIDTH} levels round its ring's level, jittered by up to {@value #JITTER} by where it is, within 1-100. */
    public static Band bandAt(long seed, int x, int z) {
        double middle = levelAt(Math.sqrt((double) x * x + (double) z * z));
        int jitter = Math.floorMod(hash(seed, x, z), 2 * JITTER + 1) - JITTER;
        int lo = (int) Math.round(middle) + jitter - WIDTH / 2;
        lo = Math.max(LOWEST, Math.min(HIGHEST - WIDTH + 1, lo));
        return new Band(lo, lo + WIDTH - 1);
    }

    static int hash(long seed, int x, int z) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0x632BE59BD9B4E019L + z * 0x85157AF5L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) h;
    }
}
