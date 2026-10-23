package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.wuzhuang.IslandShape;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where Mount Wuzhuang lies: over the clouds only, over its serene hosts, never on another zone's
 * ground at its heights (though it may float over the Bottomless Cave); and its island's shape:
 * within its region, wholly over the clouds, its temple on level ground, its cloud stair a continuous
 * walk of half steps from the ground to the terrace, joined to the island by its bridge.
 */
class WuzhuangZoneTest {

    private static final Zones.Zone W = Zones.MOUNT_WUZHUANG;
    private static final long[] SEEDS = {1L, 42L, 12345L, -7L};

    @Test
    void itIsAsDesigned() throws Exception {
        var band = W.levelBand();
        assertEquals(78, band.minLevel());
        assertEquals(88, band.maxLevel());
        assertEquals(145, band.baseGruntXp(), "stepped up from the Flaming Mountains' 130");
        assertEquals(Zones.Vertical.above(192), W.vertical(), "aerial: from over the world's clouds up");
        assertEquals(256, W.hostY());
        assertSame(ZoneAllies.NONE, W.allies());
        assertTrue(Zones.ALL.indexOf(W) > Zones.ALL.indexOf(Zones.FIRE_MOUNTAIN), "after the zones before it");
        assertSame(W, Zones.byBiome(Zones.MOUNT_WUZHUANG_BIOME));
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/mount_wuzhuang_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertEquals(Set.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:flower_forest",
                    "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:meadow", "minecraft:cherry_grove"), hosts);
        }
    }

    @Test
    void itTakesOnlyTheSkyOverItsRegions() {
        int found = 0;
        for (long seed : SEEDS) {
            for (int x = -30000; x < 30000; x += 47) for (int z = -30000; z < 30000; z += 53) {
                if (!W.covers(seed, x, z)) continue;
                found++;
                assertEquals(W, Zones.MAP.zoneAt(seed, x, 192, z), "at 192, the mountain");
                assertEquals(W, Zones.MAP.zoneAt(seed, x, IslandShape.TERRACE, z), "at its terrace");
                assertNotEquals(W, Zones.MAP.zoneAt(seed, x, 191, z), "under the clouds, not");
                assertNotEquals(W, Zones.MAP.zoneAt(seed, x, 70, z), "on the ground, not");
            }
        }
        assertTrue(found > 1000, "it has regions: " + found);
    }

    @Test
    void itNeverSharesGroundAtItsHeightsButMayFloatOverTheCave() {
        int mine = 0, overCave = 0;
        for (long seed : SEEDS) {
            for (int x = -40000; x < 40000; x += 37) for (int z = -40000; z < 40000; z += 41) {
                if (!W.covers(seed, x, z)) continue;
                mine++;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other == W) continue;
                    if (other.vertical().overlaps(W.vertical())) assertFalse(other.covers(seed, x, z), other.id() + " under it at " + x + "," + z);
                    else if (other.covers(seed, x, z)) overCave++;
                }
            }
        }
        assertTrue(mine > 0, "it has ground");
        assertTrue(overCave > 0, "it floats over the cave here and there");
    }

    @Test
    void theIslandStandsWhollyWithinItsRegionAndOverTheClouds() {
        int regions = 0;
        for (long seed : SEEDS) {
            for (int x = -30000; x < 30000; x += 1531) for (int z = -30000; z < 30000; z += 1543) {
                int[] c = W.centre(seed, x, z);
                if (c == null) continue;
                regions++;
                IslandShape is = IslandShape.of(seed, c[0], c[1]);
                for (int a = 0; a < 72; a++) {
                    double t = a * Math.PI / 36;
                    double r = is.radius(t) * 1.25; // the clouds round its flanks reach a little past its rim
                    assertTrue(W.covers(seed, c[0] + (int) Math.round(Math.cos(t) * r), c[1] + (int) Math.round(Math.sin(t) * r)), "its reach within the region");
                    assertTrue(is.radius(t) >= IslandShape.RADIUS - IslandShape.WANDER && is.radius(t) <= IslandShape.RADIUS + IslandShape.WANDER);
                }
                int[] s = is.stairAxis();
                for (int a = 0; a < 16; a++) {
                    double t = a * Math.PI / 8;
                    assertTrue(W.covers(seed, s[0] + (int) Math.round(Math.cos(t) * (IslandShape.STAIR_OUTER + 1)),
                            s[1] + (int) Math.round(Math.sin(t) * (IslandShape.STAIR_OUTER + 1))), "the stair within the region");
                }
                for (int dx = -90; dx <= 90; dx += 3) for (int dz = -90; dz <= 90; dz += 3) {
                    int x0 = c[0] + dx, z0 = c[1] + dz;
                    if (!is.inIsland(x0, z0)) continue;
                    int top = is.top(x0, z0), bottom = is.bottom(x0, z0);
                    assertTrue(bottom >= IslandShape.FLOOR && bottom > Zones.MOUNT_WUZHUANG_FLOOR, "over the clouds: " + bottom);
                    assertTrue(top >= bottom && top <= 300, "its top: " + top);
                    for (int y = IslandShape.CLOUD_LOW; y <= IslandShape.CLOUD_HIGH; y += 2) {
                        if (is.cloud(x0, y, z0)) assertTrue(y >= Zones.MOUNT_WUZHUANG_FLOOR, "its clouds are its own heights");
                    }
                }
            }
        }
        assertTrue(regions > 30, "regions: " + regions);
    }

    @Test
    void theTempleAndItsRoadStandOnLevelGround() {
        for (long seed : SEEDS) {
            int[] c = W.centre(seed, 0, 0);
            if (c == null) c = new int[]{1000, 1000};
            IslandShape is = IslandShape.of(seed, c[0], c[1]);
            for (int lx = -IslandShape.TEMPLE_X; lx <= IslandShape.TEMPLE_X; lx++)
                for (int lz = IslandShape.TEMPLE_NORTH; lz <= IslandShape.TEMPLE_SOUTH; lz++) {
                    assertTrue(is.edge(c[0] + lx, c[1] + lz) < 0.85, "the temple well within the island");
                    assertEquals(IslandShape.TERRACE, is.top(c[0] + lx, c[1] + lz), "level");
                }
            for (int lz = IslandShape.TEMPLE_SOUTH; lz < is.southRim(); lz++)
                for (int lx = -IslandShape.ROAD_HALF; lx <= IslandShape.ROAD_HALF; lx++) {
                    assertTrue(is.inIsland(c[0] + lx, c[1] + lz), "the road on the island to its rim");
                    assertEquals(IslandShape.TERRACE, is.top(c[0] + lx, c[1] + lz), "the road level");
                }
            for (IslandShape.Peak p : IslandShape.PEAKS) assertTrue(is.peaks(p.x(), p.z()) > p.height() * 0.7, "its peaks rise");
        }
    }

    @Test
    void theCloudStairIsAContinuousWalkFromTheGroundToTheTerrace() {
        IslandShape is = IslandShape.of(42L, 5000, 7000);
        int[] a = is.stairAxis();
        // every step of a turn has its columns, each beside the next step's, half a block lower
        for (int k = 0; k < IslandShape.STEPS_PER_TURN; k++) {
            int columns = 0;
            boolean joined = false;
            int next = (k + 1) % IslandShape.STEPS_PER_TURN;
            for (int x = a[0] - 9; x <= a[0] + 9; x++) for (int z = a[1] - 9; z <= a[1] + 9; z++) {
                if (is.stairStep(x, z) != k) continue;
                columns++;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (is.stairStep(x + dx, z + dz) == next) joined = true;
            }
            assertTrue(columns >= 3, "step " + k + " is three wide at least: " + columns);
            assertTrue(joined, "step " + k + " joins step " + next);
        }
        for (int k = 0; k < 400; k++) {
            assertEquals(0.5, IslandShape.stepTop(k) - IslandShape.stepTop(k + 1), 1e-9, "half a block a step");
            double top = IslandShape.stepY(k) + (IslandShape.stepIsSlab(k) ? 0.5 : 1.0);
            assertEquals(IslandShape.stepTop(k), top, 1e-9, "step " + k + "'s block tops out where it should");
        }
        assertEquals(IslandShape.TERRACE + 1, IslandShape.stepTop(0), 1e-9, "the top step is level with the terrace");
        // a turn falls 24 blocks: room overhead, and down to the lowest ground in some 7 turns
        assertEquals(16, IslandShape.stepTop(0) - IslandShape.stepTop(IslandShape.STEPS_PER_TURN), 1e-9, "a turn falls 16 blocks: room overhead");
        // the bridge joins the top step to the island's rim
        boolean toTop = false, toIsland = false;
        for (int x = is.cx - 2; x <= is.cx + 2; x++) for (int z = is.cz + is.southRim() - 4; z <= a[1]; z++) {
            if (!is.onBridge(x, z)) continue;
            if (is.inIsland(x, z)) toIsland = true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (is.stairStep(x + dx, z + dz) == 0) toTop = true;
        }
        assertTrue(toTop && toIsland, "the bridge runs from the island's rim to the stair's top");
        // the stair stands clear of the island
        for (int x = a[0] - 9; x <= a[0] + 9; x++) for (int z = a[1] - 9; z <= a[1] + 9; z++) {
            if (is.stairStep(x, z) >= 0) assertFalse(is.inIsland(x, z), "the stair winds in open air");
        }
    }

    @Test
    void theIslandIsTheSameWhicheverChunkAsks() {
        IslandShape a = IslandShape.of(99L, 3000, -2000), b = IslandShape.of(99L, 3000, -2000);
        for (int x = 2900; x < 3100; x += 7) for (int z = -2100; z < -1900; z += 7) {
            assertEquals(a.top(x, z), b.top(x, z));
            assertEquals(a.bottom(x, z), b.bottom(x, z));
            assertEquals(a.stairStep(x, z), b.stairStep(x, z));
            assertEquals(a.cloud(x, 220, z), b.cloud(x, 220, z));
        }
        assertEquals(a.pinesNear(2900, -2100, 3100, -1900, 4), b.pinesNear(2900, -2100, 3100, -1900, 4));
    }
}
