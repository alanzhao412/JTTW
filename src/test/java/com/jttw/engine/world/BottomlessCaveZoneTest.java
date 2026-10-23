package com.jttw.engine.world;

import com.jttw.engine.world.feature.Labyrinth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Where the Bottomless Cave lies: under its regions only, below its height only, never where the ridge is. */
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
            for (int x = -30000; x < 30000; x += 47) for (int z = -30000; z < 30000; z += 53) {
                if (!CAVE.covers(seed, x, z)) continue;
                found++;
                assertEquals(CAVE, Zones.MAP.zoneAt(seed, x, Zones.BOTTOMLESS_CAVE_TOP - 1, z), "just under its top, the cave");
                assertEquals(CAVE, Zones.MAP.zoneAt(seed, x, Labyrinth.FLOOR[Labyrinth.LEVELS - 1], z), "at its palace's floor");
                assertNotEquals(CAVE, Zones.MAP.zoneAt(seed, x, Zones.BOTTOMLESS_CAVE_TOP, z), "at its top, not");
                assertNotEquals(CAVE, Zones.MAP.zoneAt(seed, x, 120, z), "at the mountain's surface, not");
                assertFalse(CAVE.covers(seed, x, Zones.BOTTOMLESS_CAVE_TOP, z));
            }
        }
        assertTrue(found > 1000, "it has regions: " + found);
    }

    @Test
    void itNeverLiesUnderTheRidge() {
        int ridge = 0, cave = 0;
        for (long seed : SEEDS) {
            for (int x = -40000; x < 40000; x += 41) for (int z = -40000; z < 40000; z += 43) {
                boolean r = Zones.WHITE_BONE.covers(seed, x, z), c = CAVE.covers(seed, x, z);
                if (r) ridge++;
                if (c) cave++;
                assertFalse(r && c, "seed " + seed + " at " + x + "," + z + ": both");
            }
        }
        assertTrue(ridge > 0 && cave > 0, "both have ground: ridge " + ridge + ", cave " + cave);
        // and its regions keep clear of the ridge's by their reaches, as the map has them give way
        for (long seed : SEEDS) {
            for (int x = -40000; x < 40000; x += 997) for (int z = -40000; z < 40000; z += 1009) {
                int[] c = CAVE.centre(seed, x, z);
                if (c == null) continue;
                for (int a = 0; a < 32; a++) {
                    double t = a * Math.PI / 16;
                    int px = c[0] + (int) (Math.cos(t) * CAVE.layout().margin()), pz = c[1] + (int) (Math.sin(t) * CAVE.layout().margin());
                    assertFalse(Zones.WHITE_BONE.covers(seed, px, pz), "the ridge within the cave's reach");
                }
            }
        }
    }

    @Test
    void itsLabyrinthLiesWhollyWithinItsRegion() {
        // every open block of the labyrinth is at most this far from the centre
        double reach = Labyrinth.REACH + Labyrinth.PITCH;
        int regions = 0;
        for (long seed : SEEDS) {
            for (int x = -30000; x < 30000; x += 769) for (int z = -30000; z < 30000; z += 773) {
                int[] c = CAVE.centre(seed, x, z);
                if (c == null) continue;
                regions++;
                for (int a = 0; a < 64; a++) {
                    double t = a * Math.PI / 32;
                    assertTrue(CAVE.covers(seed, c[0] + (int) Math.round(Math.cos(t) * reach), c[1] + (int) Math.round(Math.sin(t) * reach)),
                            "the labyrinth's rim is in the region");
                }
            }
        }
        assertTrue(regions > 20, "regions: " + regions);
    }
}
