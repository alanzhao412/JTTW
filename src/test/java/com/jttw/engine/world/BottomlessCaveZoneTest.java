package com.jttw.engine.world;

import com.jttw.engine.world.feature.Labyrinth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where the Bottomless Cave lies: once a world, 2-3 km out, about 380 blocks across, below its height
 * only, never where the ridge is; its fortress, its mountain's outposts and its door within it.
 */
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
                assertEquals(CAVE, Zones.MAP.zoneAt(seed, x, Labyrinth.THRONE, z), "at its palace's floor");
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
    void itIsLargeAboutThreeHundredAndEightyBlocksAcross() {
        assertEquals(190, CAVE.layout().radius());
        assertTrue(CAVE.layout().radius() - CAVE.layout().wobble() >= 160, "never under 320 across");
        assertTrue(CAVE.layout().radius() >= 2 * 110 - 30, "much larger than it was (110)");
    }

    @Test
    void itsFortressMountainAndOutpostsLieWhollyWithinItsRegion() {
        // every open block of the fortress is at most this far from the centre
        double reach = com.jttw.engine.world.cave.BottomlessCave.FORTRESS_REACH;
        int regions = 0;
        for (long seed = -20; seed <= 20; seed++) {
            int[] c = CAVE.centres(seed)[0];
            regions++;
            for (int a = 0; a < 64; a++) {
                double t = a * Math.PI / 32;
                assertTrue(CAVE.covers(seed, c[0] + (int) Math.round(Math.cos(t) * reach), c[1] + (int) Math.round(Math.sin(t) * reach)),
                        "the fortress's rim is in the region");
            }
            Labyrinth lab = Labyrinth.of(seed, c[0], c[1]);
            var pit = new com.jttw.engine.world.feature.PitHole(lab, Labyrinth.Land.flat(80));
            for (var o : pit.outposts()) {
                for (int dx = -6; dx <= 6; dx += 6) for (int dz = -6; dz <= 6; dz += 6) {
                    assertTrue(CAVE.covers(seed, o.x() + dx, o.z() + dz), "seed " + seed + ": an outpost in the region");
                }
            }
            int[] door = lab.beforeDoor(PitHole_FORE);
            assertTrue(CAVE.covers(seed, door[0], door[2]), "the forecourt");
        }
        assertTrue(regions > 20, "regions: " + regions);
    }

    private static final int PitHole_FORE = com.jttw.engine.world.feature.PitHole.FORE_OUT + 2;

    @Test
    void itsDoorIsWhereTheEarthGodSaysAndKeepsClearOfTheOtherZones() {
        for (long seed : SEEDS) {
            int[] c = CAVE.centres(seed)[0];
            Labyrinth lab = Labyrinth.of(seed, c[0], c[1]);
            int[] door = lab.door(), told = Labyrinth.doorOf(seed, c[0], c[1]);
            assertEquals(door[0], told[0]);
            assertEquals(door[2], told[1]);
            assertEquals(Labyrinth.DOOR_AT, (int) Math.round(Math.hypot(door[0] - c[0], door[2] - c[1])), "the door on the mountain's flank");
            for (Zones.Zone other : Zones.MAP.zones()) {
                if (other == CAVE || !other.vertical().overlaps(CAVE.vertical())) continue;
                for (int[] o : other.centres(seed)) {
                    assertTrue(Math.hypot(o[0] - c[0], o[1] - c[1]) >= CAVE.layout().margin() + other.layout().margin(),
                            "seed " + seed + ": clear of " + other.id());
                }
            }
        }
    }
}
