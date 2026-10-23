package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.world.lion.LionCamelCity;
import com.jttw.engine.world.lion.RidgeShape;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where Lion-Camel Ridge lies (mountain country, from y 40 up: over the Bottomless Cave's heights,
 * never on a surface zone's ground), its band at the levels' cap, its allies, and the ridge's shape
 * round Lion-Camel City.
 */
class LionCamelPlacementTest {

    private static final Zones.Zone L = Zones.LION_CAMEL;

    @Test
    void itsHostsAreMountainAndHighlandCountry() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/lion_camel_ridge_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertEquals(Set.of("minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks", "minecraft:snowy_slopes",
                    "minecraft:grove", "minecraft:meadow", "minecraft:cherry_grove", "minecraft:windswept_hills",
                    "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
                    "minecraft:windswept_savanna", "minecraft:savanna_plateau"), hosts);
        }
        assertEquals("jttw_engine:lion_camel_ridge_hosts", L.hosts().location().toString());
        assertEquals(256, L.hostY(), "judged over any mountain's top, as the cave's are");
    }

    @Test
    void itsBandIsTheLastToTheCap() {
        var band = L.levelBand();
        assertEquals(89, band.minLevel());
        assertEquals(100, band.maxLevel());
        assertEquals(LevelConstants.MAX_LEVEL, band.maxLevel(), "the levels' cap");
        assertEquals(160, band.baseGruntXp(), "stepped up from the Flaming Mountains' 130 and Mount Wuzhuang's 145");
        assertEquals(1248, band.xpForTier(com.jttw.engine.progression.level.EnemyTier.GRUNT), "160 x 7.8 a grunt");
        assertSame(L, Zones.byBiome(Zones.LION_CAMEL_RIDGE_BIOME));
        assertTrue(Zones.MAP.zones().contains(L));
        assertTrue(Zones.ALL.indexOf(L) > Zones.ALL.indexOf(Zones.FIRE_MOUNTAIN), "after the Flaming Mountains");
        assertEquals(L, Zones.ALL.stream().filter(Zones.Zone::hasLevelBand).reduce((a, b) -> b).orElseThrow(), "the last zone of a band");
    }

    @Test
    void withItEveryLevelPastTheVanillaPhaseHasABand() {
        for (int level = LevelConstants.VANILLA_CAP_LEVEL; level <= LevelConstants.MAX_LEVEL; level++) {
            final int lv = level;
            assertTrue(Zones.ALL.stream().anyMatch(z -> z.minLevel() <= lv && lv <= z.maxLevel()), "a band for level " + lv);
        }
        assertTrue(L.vertical().overlaps(Zones.MOUNT_WUZHUANG.vertical()), "it keeps clear of Mount Wuzhuang's sky too");
    }

    @Test
    void itSparesThePengAndTakesTheColumnFromForty() {
        assertSame(ZoneAllies.PENG, L.allies(), "allied to the Peng: the ridge is his brothers'");
        assertEquals(Zones.Vertical.above(Zones.LION_CAMEL_FLOOR), L.vertical());
        assertEquals(Zones.BOTTOMLESS_CAVE_TOP, Zones.LION_CAMEL_FLOOR);
        assertFalse(L.vertical().overlaps(Zones.BOTTOMLESS.vertical()), "no heights shared with the Bottomless Cave");
        assertTrue(L.vertical().overlaps(Zones.WHITE_BONE.vertical()) && L.vertical().overlaps(Zones.FIRE_MOUNTAIN.vertical())
                && L.vertical().overlaps(Zones.MYSTICAL_WATERS.vertical()), "but the surface zones' it does");
        assertTrue(L.vertical().contains(40) && !L.vertical().contains(39));
    }

    @Test
    void itNeverSharesGroundWithASurfaceZone() {
        int ridge = 0;
        for (long seed : new long[]{1L, 42L, 12345L, -7L}) {
            int[] c = L.centres(seed)[0];
            for (int x = c[0] - 230; x <= c[0] + 230; x += 3) for (int z = c[1] - 230; z <= c[1] + 230; z += 3) {
                if (!L.covers(seed, x, z)) continue;
                ridge++;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other != L && other.vertical().overlaps(L.vertical())) {
                        assertFalse(other.covers(seed, x, z), "seed " + seed + " at " + x + " " + z + " is " + other.id() + "'s too");
                    }
                }
            }
        }
        assertTrue(ridge > 1000, "it has ground: " + ridge);
    }

    /** The regions of worlds of the first {@code many} seeds, from 1: each {seed, x, z}. */
    private static List<long[]> regions(int many) {
        List<long[]> found = new ArrayList<>();
        for (long seed = 1; seed <= many; seed++) {
            int[] c = L.centres(seed)[0];
            found.add(new long[]{seed, c[0], c[1]});
        }
        return found;
    }

    @Test
    void itLiesOnceAWorldFarthestOut() {
        for (long seed : new long[]{1L, 42L, 12345L, -7L}) {
            assertEquals(1, L.centres(seed).length, "one ridge a world");
            int[] c = L.centres(seed)[0];
            double d = Math.hypot(c[0], c[1]);
            assertTrue(d >= 7500 && d <= 9000, "7.5-9 km out: " + d);
            for (Zones.Zone other : Zones.MAP.zones()) {
                if (other == L || !other.hasLevelBand()) continue; // Fengdu, a place of no band, lies past it
                for (int[] o : other.centres(seed)) assertTrue(Math.hypot(o[0], o[1]) < d, other.id() + " nearer spawn");
            }
        }
    }

    @Test
    void theCityFitsInsideItsRegionWithItsApronAndSlope() {
        int reach = RidgeShape.APRON + RidgeShape.BLEND;
        assertTrue(LionCamelCity.OUTER < RidgeShape.APRON, "the apron is wider than the city");
        for (long[] r : regions(8)) {
            long seed = r[0];
            int[] c = {(int) r[1], (int) r[2]};
            for (int d = -reach; d <= reach; d += 4) {
                for (int[] at : new int[][]{{d, -reach}, {d, reach}, {-reach, d}, {reach, d}}) {
                    assertTrue(L.depth(seed, c[0] + at[0], c[1] + at[1]) > 0, "the city's ground lies within its region at " + at[0] + " " + at[1]);
                }
            }
        }
    }

    @Test
    void theRidgesCragsAndTheCitysGround() {
        for (long[] r : regions(6)) {
            long seed = r[0];
            int[] c = {(int) r[1], (int) r[2]};
            int highest = 0;
            for (int dx = -260; dx <= 260; dx += 4) for (int dz = -260; dz <= 260; dz += 4) {
                int x = c[0] + dx, z = c[1] + dz;
                int rise = RidgeShape.riseAt(seed, x, z, c);
                if (L.depth(seed, x, z) <= 0) {
                    assertEquals(0, RidgeShape.cragAt(seed, x, z), "nothing raised outside");
                    continue;
                }
                assertTrue(rise >= 0 && rise <= RidgeShape.CRAGS, "crags within bounds: " + rise);
                if (RidgeShape.onApron(dx, dz)) assertEquals(0, rise, "the apron is level");
                highest = Math.max(highest, rise);
            }
            assertTrue(highest >= RidgeShape.CRAGS / 2, "crags rise: " + highest);
        }
        // the ground: the city's floor on the apron, the ridge's own past the slope, between the two between
        assertEquals(100, RidgeShape.groundAt(160, 100, 0, 0));
        assertEquals(100, RidgeShape.groundAt(160, 100, RidgeShape.APRON, -RidgeShape.APRON));
        assertEquals(160, RidgeShape.groundAt(160, 100, RidgeShape.APRON + RidgeShape.BLEND, 3));
        int mid = RidgeShape.groundAt(160, 100, RidgeShape.APRON + RidgeShape.BLEND / 2, 0);
        assertTrue(mid > 100 && mid < 160, "a slope between: " + mid);
        // the floor is worked out once a region, and kept between the sea and the sky
        assertEquals(69, RidgeShape.cityFloor(99L, 10, 10, (x, z) -> 12, 69, 248));
        assertEquals(248, RidgeShape.cityFloor(99L, 20, 20, (x, z) -> 300, 69, 248));
        assertEquals(69, RidgeShape.cityFloor(99L, 10, 10, (x, z) -> 200, 69, 248), "cached");
    }

    @Test
    void theCitysPlacesAreWhereTheyShouldBe() {
        assertTrue(LionCamelCity.inPalace(0, 0) && !LionCamelCity.inCourt(0, 0));
        assertTrue(LionCamelCity.inCourt(0, LionCamelCity.COURT_Z) && !LionCamelCity.inPalace(0, LionCamelCity.COURT_Z));
        assertTrue(LionCamelCity.inside(0, LionCamelCity.WALL_IN - 1) && !LionCamelCity.inside(0, LionCamelCity.WALL_IN));
        // the court lies behind the palace, apart from it, inside the walls
        assertTrue(LionCamelCity.COURT_S < -LionCamelCity.PALACE && LionCamelCity.COURT_N > -LionCamelCity.WALL_IN);
        // the plots keep off the avenues, the palace's ground and the ring road
        for (int a = -LionCamelCity.WALL_IN; a <= LionCamelCity.WALL_IN; a++) {
            if (Math.abs(a) <= LionCamelCity.AVENUE + 1 || Math.abs(a) >= LionCamelCity.RING) assertEquals(0, LionCamelCity.plotIndex(a), "no plot at " + a);
        }
        // the two kings' lairs are far enough apart that each fight is its own
        assertTrue(Math.abs(LionCamelCity.COURT_Z) > 32, "the court and the hall more than a boss bar's range apart");
    }
}
