package com.jttw.engine.world;

import com.jttw.engine.world.feature.Labyrinth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Where the Bottomless Cave lies: once a world, 2-3 km out, below its height only, never where the ridge is. */
class BottomlessCaveZoneTest {

    private static final Zones.Zone CAVE = Zones.BOTTOMLESS;
    private static final long[] SEEDS = {1L, 42L, 987654321L, -7L};

    @Test
    void itIsAsDesigned() {
        var band = CAVE.levelBand();
        assertEquals(Zones.BOTTOMLESS_CAVE_BIOME.location(), band.biome());
        assertEquals(37, band.minLevel());
        assertEquals(46, band.maxLevel());
        assertEquals(100, band.baseGruntXp(), "stepped up from the ridge's 80");
        assertEquals(Zones.Vertical.below(Zones.BOTTOMLESS_CAVE_TOP), CAVE.vertical());
        assertEquals(256, CAVE.hostY(), "its host judged over any mountain's top");
        assertSame(ZoneAllies.NONE, CAVE.allies());
        assertTrue(Zones.ALL.indexOf(CAVE) > Zones.ALL.indexOf(Zones.WHITE_BONE), "after the ridge: it gives way to it");
    }

    @Test
    void itsBiomeIsOnlyBelowItsHeight() {
        int found = 0;
        for (long seed : SEEDS) {
            int[] c = CAVE.centres(seed)[0];
            for (int x = c[0] - 140; x <= c[0] + 140; x += 3) for (int z = c[1] - 140; z <= c[1] + 140; z += 3) {
                if (!CAVE.covers(seed, x, z)) continue;
                found++;
                assertEquals(CAVE, Zones.MAP.zoneAt(seed, x, Zones.BOTTOMLESS_CAVE_TOP - 1, z), "just under its top, the cave");
                assertEquals(CAVE, Zones.MAP.zoneAt(seed, x, Labyrinth.FLOOR[Labyrinth.LEVELS - 1], z), "at its palace's floor");
                assertNotEquals(CAVE, Zones.MAP.zoneAt(seed, x, Zones.BOTTOMLESS_CAVE_TOP, z), "at its top, not");
                assertNotEquals(CAVE, Zones.MAP.zoneAt(seed, x, 120, z), "at the mountain's surface, not");
                assertFalse(CAVE.covers(seed, x, Zones.BOTTOMLESS_CAVE_TOP, z));
            }
        }
        assertTrue(found > 1000, "it has ground: " + found);
    }

    @Test
    void itLiesOnceAWorldBeyondTheRidge() {
        for (long seed : SEEDS) {
            assertEquals(1, CAVE.centres(seed).length, "one cave a world");
            int[] c = CAVE.centres(seed)[0], r = Zones.WHITE_BONE.centres(seed)[0];
            double d = Math.hypot(c[0], c[1]);
            assertTrue(d >= 2000 && d <= 3000, "2-3 km out: " + d);
            assertTrue(d > Math.hypot(r[0], r[1]), "farther out than the ridge");
            // its region keeps clear of the ridge's by both their reaches
            assertTrue(Math.hypot(c[0] - r[0], c[1] - r[1]) >= CAVE.layout().margin() + Zones.WHITE_BONE.layout().margin(), "clear of the ridge");
            for (int a = 0; a < 32; a++) {
                double t = a * Math.PI / 16;
                int px = c[0] + (int) (Math.cos(t) * CAVE.layout().margin()), pz = c[1] + (int) (Math.sin(t) * CAVE.layout().margin());
                assertFalse(Zones.WHITE_BONE.covers(seed, px, pz), "the ridge within the cave's reach");
            }
        }
    }

    @Test
    void itsLabyrinthLiesWhollyWithinItsRegion() {
        // every open block of the labyrinth is at most this far from the centre
        double reach = Labyrinth.REACH + Labyrinth.PITCH;
        int regions = 0;
        for (long seed = -20; seed <= 20; seed++) {
            int[] c = CAVE.centres(seed)[0];
            regions++;
            for (int a = 0; a < 64; a++) {
                double t = a * Math.PI / 32;
                assertTrue(CAVE.covers(seed, c[0] + (int) Math.round(Math.cos(t) * reach), c[1] + (int) Math.round(Math.sin(t) * reach)),
                        "the labyrinth's rim is in the region");
            }
        }
        assertTrue(regions > 20, "regions: " + regions);
    }
}
