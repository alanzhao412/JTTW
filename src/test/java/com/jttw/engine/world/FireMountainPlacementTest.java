package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.fire.FortressPlan;
import com.jttw.engine.world.fire.RangeShape;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Where the Flaming Mountains lie (hot, dry country, never on another zone's ground), its band, and the range's shape. */
class FireMountainPlacementTest {

    private static final Zones.Zone F = Zones.FIRE_MOUNTAIN;

    @Test
    void itsHostsAreHotDryCountry() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/fire_mountain_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertEquals(Set.of("minecraft:desert", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
                    "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"), hosts);
        }
        assertEquals("jttw_engine:fire_mountain_hosts", F.hosts().location().toString());
    }

    @Test
    void itsBandAndHeights() {
        var band = F.levelBand();
        assertEquals(67, band.minLevel());
        assertEquals(77, band.maxLevel());
        assertEquals(130, band.baseGruntXp(), "stepped up from the waters' 110");
        assertEquals(Zones.Vertical.WHOLE_COLUMN, F.vertical());
        assertTrue(Zones.ALL.indexOf(F) > Zones.ALL.indexOf(Zones.MYSTICAL_WATERS), "after the waters: it keeps clear of every zone before it");
        assertTrue(Zones.MAP.zones().contains(F));
        assertSame(F, Zones.byBiome(Zones.FIRE_MOUNTAIN_BIOME));
    }

    @Test
    void itNeverSharesGroundWithAnotherZone() {
        int fire = 0;
        for (long seed : new long[]{1L, 42L, 12345L, -7L}) {
            int[] c = F.centres(seed)[0];
            double d = Math.hypot(c[0], c[1]);
            assertTrue(d >= 4500 && d <= 6000, "one range a world, 4.5-6 km out: " + d);
            int m = F.layout().margin();
            for (int x = c[0] - m; x <= c[0] + m; x += 3) for (int z = c[1] - m; z <= c[1] + m; z += 3) {
                if (!F.covers(seed, x, z)) continue;
                fire++;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other != F && other.vertical().overlaps(F.vertical())) {
                        assertFalse(other.covers(seed, x, z), "seed " + seed + " at " + x + " " + z + " is " + other.id() + "'s too");
                    }
                }
            }
        }
        assertTrue(fire > 4 * 25000, "it has ground, a great deal of it: " + fire);
    }

    @Test
    void itIsHugeAndStillOneAWorldInItsRing() {
        assertTrue(F.layout().radius() >= 320 && F.layout().radius() - F.layout().wobble() >= 270, "some 640 blocks across: " + F.layout());
        assertEquals(1, F.layout().regions());
        assertEquals(4500, F.layout().near());
        assertEquals(6000, F.layout().far());
        for (long seed = 1; seed <= 12; seed++) {
            int[] c = F.centres(seed)[0];
            for (Zones.Zone other : Zones.MAP.zones()) {
                if (other == F || !other.vertical().overlaps(F.vertical())) continue;
                for (int[] o : other.centres(seed)) {
                    double d = Math.hypot(o[0] - c[0], o[1] - c[1]);
                    assertTrue(d >= F.layout().margin() + other.layout().margin() + ZoneMap.GAP,
                            "seed " + seed + ": clear of " + other.id() + " (" + (int) d + " apart)");
                }
            }
        }
    }

    /** The regions of worlds of the first {@code many} seeds, from 1: each {seed, x, z}. */
    private static List<long[]> regions(int many) {
        List<long[]> found = new ArrayList<>();
        for (long seed = 1; seed <= many; seed++) {
            int[] c = F.centres(seed)[0];
            found.add(new long[]{seed, c[0], c[1]});
        }
        return found;
    }

    @Test
    void theRangeClimbsFromItsEdgeToMountHaohao() {
        for (long[] r : regions(6)) {
            long seed = r[0];
            int[] c = {(int) r[1], (int) r[2]};
            int peak = RangeShape.riseAt(seed, c[0], c[1]);
            assertTrue(peak >= RangeShape.BASE + RangeShape.HAOHAO * 0.8, "Mount Haohao over the hall: " + peak);
            int highest = 0, valleys = 0, all = 0;
            for (int dx = -380; dx <= 380; dx += 4) for (int dz = -380; dz <= 380; dz += 4) {
                int x = c[0] + dx, z = c[1] + dz;
                if (F.depth(seed, x, z) <= 0) {
                    assertEquals(0, RangeShape.riseAt(seed, x, z), "nothing raised outside");
                    continue;
                }
                all++;
                highest = Math.max(highest, RangeShape.riseAt(seed, x, z));
                if (RangeShape.valley(seed, x, z)) valleys++;
            }
            assertTrue(highest >= 70, "mountains: " + highest);
            double share = valleys / (double) all;
            assertTrue(share > 0.1 && share < 0.7, "valleys of burning earth between the ridges: " + share);
            // at the very edge it barely rises: the range climbs out of the land
            for (int d = 0; d < 400; d++) {
                if (F.depth(seed, c[0] + d, c[1]) > 0) continue;
                assertTrue(RangeShape.riseAt(seed, c[0] + d - 1, c[1]) <= 3, "low at the edge: " + RangeShape.riseAt(seed, c[0] + d - 1, c[1]));
                break;
            }
        }
    }

    @Test
    void theGorgeRunsFromTheGateOutLevelWithTheHall() {
        for (long[] r : regions(4)) {
            long seed = r[0];
            int[] c = {(int) r[1], (int) r[2]};
            // in the gorge's floor, nothing is raised, all the way out
            for (int dz = RangeShape.GORGE_FROM; F.depth(seed, c[0], c[1] + dz) > 0; dz++) {
                assertEquals(0, RangeShape.riseAt(seed, c[0], c[1] + dz), "the gorge's floor at " + dz);
            }
            // its forecourt level with the hall, then climbing to the land
            for (int dz = FortressPlan.FACE_Z + 1; dz <= RangeShape.GORGE_FROM + RangeShape.FORECOURT; dz++) {
                assertEquals(0, RangeShape.floorShare(0, dz), 1e-9, "level at " + dz);
                assertTrue(RangeShape.onForecourt(0, dz));
            }
            assertEquals(1, RangeShape.floorShare(0, RangeShape.GORGE_FROM + RangeShape.FORECOURT + RangeShape.CLIMB), 1e-9);
            // the mountain stands over the first floor and the gatehouse, the door in its face
            for (int dz = -FortressPlan.HALL; dz <= FortressPlan.FACE_Z; dz++) for (int dx = -24; dx <= 24; dx += 8) {
                assertTrue(RangeShape.riseAt(seed, c[0] + dx, c[1] + dz) > FortressPlan.FLOOR_H[0] + 5,
                        "rock over the cave at " + dx + " " + dz + ": " + RangeShape.riseAt(seed, c[0] + dx, c[1] + dz));
            }
            // and its walls rise either side
            assertTrue(RangeShape.riseAt(seed, c[0] + RangeShape.GORGE_WALL + 2, c[1] + RangeShape.GORGE_FROM + 4) > 10, "a wall to the east");
            assertTrue(RangeShape.riseAt(seed, c[0] - RangeShape.GORGE_WALL - 2, c[1] + RangeShape.GORGE_FROM + 4) > 10, "and to the west");
        }
    }

    @Test
    void theCaveFitsInsideTheRegionFarFromItsEdge() {
        for (long[] r : regions(6)) {
            long seed = r[0];
            int[] c = {(int) r[1], (int) r[2]};
            for (int x = FortressPlan.X_MIN; x <= FortressPlan.X_MAX; x += 2) for (int z = FortressPlan.Z_MIN; z <= FortressPlan.Z_MAX + RangeShape.FORECOURT; z += 2) {
                assertTrue(F.depth(seed, c[0] + x, c[1] + z) > 0.5, "the cave and its forecourt lie deep in its region");
            }
        }
    }

    @Test
    void theOutpostsStandOverTheRangeClearOfTheRavine() {
        for (long[] r : regions(6)) {
            long seed = r[0];
            int[] c = {(int) r[1], (int) r[2]};
            var posts = RangeShape.outposts(seed, c[0], c[1]);
            long rims = posts.stream().filter(RangeShape.Outpost::rim).count();
            assertTrue(posts.size() - rims >= 8, "seed " + seed + ": outposts over the range: " + (posts.size() - rims));
            assertTrue(rims >= 2, "watchtowers on the ravine's rims: " + rims);
            assertTrue(posts.stream().anyMatch(RangeShape.Outpost::patrol), "some bands roam");
            for (var o : posts) {
                int dx = o.x() - c[0], dz = o.z() - c[1];
                assertTrue(F.depth(seed, o.x(), o.z()) > 0.05, "inside the region");
                assertFalse(Math.abs(dx) <= FortressPlan.X_MAX + 8 && dz >= FortressPlan.Z_MIN - 8 && dz <= FortressPlan.Z_MAX + 8, "clear of the cave");
                if (!o.rim()) assertFalse(dz > FortressPlan.FACE_Z - 20 && Math.abs(dx) < RangeShape.GORGE_WALL + 14, "clear of the ravine");
                else assertTrue(Math.abs(dx) > RangeShape.GORGE_HALF + RangeShape.OUTPOST_REACH, "a rim's tower stands off the ravine's floor");
                for (var other : posts) {
                    if (other != o) assertTrue(Math.hypot(other.x() - o.x(), other.z() - o.z()) > 2 * RangeShape.OUTPOST_REACH + 4, "apart");
                }
            }
        }
    }
}
