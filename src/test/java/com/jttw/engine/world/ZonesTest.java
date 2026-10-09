package com.jttw.engine.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Where the mod's zones lie: a function of the seed alone (the layout, free of the registries). */
class ZonesTest {

    private static final ZoneLayout Z = ZoneLayout.WHITE_BONE;

    /** Share of the ground in a square of this half-size the zone covers, sampled every 16 blocks. */
    private static double share(long seed, int half) {
        int in = 0, all = 0;
        for (int x = -half; x < half; x += 16) for (int z = -half; z < half; z += 16) {
            all++;
            if (Z.covers(seed, x, z)) in++;
        }
        return (double) in / all;
    }

    @Test
    void theSameSeedLaysTheSameZonesAndAnotherDoesNot() {
        int same = 0, differ = 0;
        for (int x = -20000; x < 20000; x += 97) for (int z = -20000; z < 20000; z += 389) {
            assertEquals(Z.covers(42L, x, z), Z.covers(42L, x, z));
            if (Z.covers(42L, x, z) != Z.covers(1234L, x, z)) differ++; else same++;
        }
        assertTrue(differ > 0, "another seed, other places");
    }

    @Test
    void zonesCoverAFewPercentOfTheGround() {
        // 60% of 2048-block cells hold a roughly 260-block circle: about 3% of all ground,
        // before the host biomes narrow it further
        for (long seed : new long[]{1L, 42L, 987654321L}) {
            double s = share(seed, 16384);
            assertTrue(s > 0.015 && s < 0.05, "seed " + seed + ": " + s);
        }
    }

    @Test
    void aZoneNeverCrossesItsCell() {
        long seed = 7L;
        for (int x = -16384; x < 16384; x += 8) {
            // the cell border lines: no zone touches them
            assertFalse(Z.covers(seed, x, 0) && Math.floorMod(x, Z.cellSize()) == 0, "on a border at " + x);
        }
        for (int i = -8; i <= 8; i++) {
            int border = i * Z.cellSize();
            for (int z = -16384; z < 16384; z += 8) assertFalse(Z.covers(seed, border, z), "on the border x=" + border);
        }
    }

    @Test
    void negativeCoordinatesAreZonedLikePositiveOnes() {
        double west = 0, east = 0;
        for (long seed = 1; seed <= 20; seed++) {
            for (int x = 0; x < 8192; x += 32) for (int z = 0; z < 8192; z += 32) {
                if (Z.covers(seed, x, z)) east++;
                if (Z.covers(seed, -x - 1, -z - 1)) west++;
            }
        }
        assertEquals(1.0, west / east, 0.2, "no lopsided half of the world");
    }
}
