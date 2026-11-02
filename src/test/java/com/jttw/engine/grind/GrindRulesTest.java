package com.jttw.engine.grind;

import com.jttw.engine.village.VillageSites;
import com.jttw.engine.world.Zones;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The grinding grounds' rings of levels, and where the infested wilds may lie. */
class GrindRulesTest {

    private static final long SEED = 12345L;

    @Test
    void levelsClimbWithTheZonesRings() {
        assertEquals(1, RingLevels.levelAt(0), 1e-9, "at spawn, level 1");
        assertEquals(20, RingLevels.levelAt(1000), 1e-9, "White Bone Ridge's ring begins at its band's bottom");
        assertEquals(37, RingLevels.levelAt(2000), 1e-9, "the Bottomless Cave's");
        assertEquals(47, RingLevels.levelAt(3000), 1e-9, "the Mystical Waters'");
        assertEquals(67, RingLevels.levelAt(4500), 1e-9, "the Flaming Mountains'");
        assertEquals(78, RingLevels.levelAt(6000), 1e-9, "Mount Wuzhuang's and Silk-Web Ridge's");
        assertEquals(89, RingLevels.levelAt(7500), 1e-9, "Lion-Camel Ridge's");
        assertEquals(100, RingLevels.levelAt(9000), 1e-9, "the last");
        assertEquals(100, RingLevels.levelAt(20000), 1e-9, "and beyond");
        double last = 0;
        for (int d = 0; d <= 10000; d += 50) {
            double l = RingLevels.levelAt(d);
            assertTrue(l >= last, "never lower farther out");
            assertTrue(l - last < 1.5, "smoothly: no leap between " + (d - 50) + " and " + d);
            last = l;
        }
        // each zone's ring holds its own band's levels
        for (Zones.Zone z : Zones.ALL) {
            if (z.layout() == null || !z.hasLevelBand() || z == Zones.FLOWER_FRUIT) continue;
            double mid = RingLevels.levelAt((z.layout().near() + z.layout().far()) / 2.0);
            assertTrue(mid >= z.minLevel() - 1 && mid <= z.maxLevel() + 11, z.id() + "'s ring at " + mid);
        }
    }

    @Test
    void aBandIsSevenLevelsRoundItsRingFixedByPlace() {
        for (int x = -12000; x <= 12000; x += 333) {
            for (int z = -12000; z <= 12000; z += 777) {
                RingLevels.Band b = RingLevels.bandAt(SEED, x, z);
                assertEquals(b, RingLevels.bandAt(SEED, x, z), "fixed");
                assertEquals(RingLevels.WIDTH, b.hi() - b.lo() + 1);
                assertTrue(b.lo() >= 1 && b.hi() <= 100, b.toString());
                double ring = RingLevels.levelAt(Math.hypot(x, z));
                if (ring > 5 && ring < 96) assertTrue(Math.abs((b.lo() + b.hi()) / 2.0 - ring) <= RingLevels.JITTER + 0.5, "round " + ring + ": " + b);
                assertTrue(b.leader() == b.hi() + 3 && b.miniboss() == b.hi() + 1 && b.challenger() == Math.min(b.hi(), 100));
                assertTrue(b.captainOver(b.lo(), 2) <= b.hi() + 1 && b.captainOver(b.lo(), 0) == b.lo() + 3);
            }
        }
    }

    @Test
    void infestedPatchesAreAFewHundredBlocksAcrossAndClearOfSpawnAndZones() {
        int candidates = 0, cells = 0;
        for (int cx = -20; cx <= 20; cx++) for (int cz = -20; cz <= 20; cz++) {
            cells++;
            Infestation.Candidate c = Infestation.candidate(SEED, cx, cz);
            if (c == null) continue;
            candidates++;
            assertEquals(c, Infestation.candidate(SEED, cx, cz), "the seed alone says");
            assertTrue(c.radius() >= Infestation.LEAST_RADIUS && c.radius() <= Infestation.MOST_RADIUS);
            assertTrue(c.minibosses() == 1 || c.minibosses() == 2);
            // wholly within its cell: no two patches overlap
            assertTrue(c.x() - c.radius() >= cx * Infestation.CELL && c.x() + c.radius() < (cx + 1) * Infestation.CELL);
            assertTrue(c.z() - c.radius() >= cz * Infestation.CELL && c.z() + c.radius() < (cz + 1) * Infestation.CELL);
            if (Math.hypot(c.x(), c.z()) < 400 + c.radius()) assertFalse(Infestation.clearOfSpawn(c), "never by spawn");
            if (Infestation.clearOfZones(SEED, c)) {
                for (Zones.Zone zone : Zones.MAP.zones()) {
                    assertTrue(zone.distanceToCentre(SEED, c.x(), c.z()) >= zone.layout().margin() + c.radius(), "clear of " + zone.id());
                }
            }
        }
        double share = candidates / (double) cells;
        assertTrue(Math.abs(share - Infestation.CHANCE_PERCENT / 100.0) < 0.06, "about one cell in two has a candidate: " + share);
        // a zone's heart is never infested
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int[] c : zone.centres(SEED)) {
                assertFalse(Infestation.clearOfZones(SEED, new Infestation.Candidate(0, 0, c[0], c[1], 100, 1)), zone.id());
            }
        }
        assertTrue(VillageSites.clearOfZones(SEED, 300, 200));
    }
}
