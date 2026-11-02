package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flower-Fruit Mountain: where it lies (the sea, never on another zone's ground), its band, and the
 * island's shape: a towering peak, the stone egg on it, its pillars and camps, the falls country on
 * its south face, and the Water Curtain Cave under the mountain behind the falling water.
 */
class FlowerFruitPlacementTest {

    private static final Zones.Zone Z = Zones.FLOWER_FRUIT;
    private static final long[] SEEDS = {1L, 42L, 12345L, -7L};

    @Test
    void itsHostsAreTheSea() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/flower_fruit_mountain_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertTrue(hosts.contains("minecraft:ocean") && hosts.contains("minecraft:warm_ocean") && hosts.contains("minecraft:deep_ocean"), hosts.toString());
            for (String h : hosts) assertTrue(h.endsWith("ocean"), "only the sea: " + h);
        }
    }

    @Test
    void itsBandIsTheFirstAndItsMonkeysSpareTheKing() {
        var band = Z.levelBand();
        assertEquals(20, band.minLevel());
        assertEquals(30, band.maxLevel());
        assertEquals(80, band.baseGruntXp(), "White Bone Ridge's pay");
        assertEquals(Zones.Vertical.WHOLE_COLUMN, Z.vertical());
        assertSame(ZoneAllies.WUKONG, Z.allies());
        assertSame(Z, Zones.byBiome(Zones.FLOWER_FRUIT_BIOME));
        assertTrue(Zones.ALL.indexOf(Z) > Zones.ALL.indexOf(Zones.BLACK_WIND) && Zones.ALL.indexOf(Z) < Zones.ALL.indexOf(Zones.LION_CAMEL),
                "after the ridge's band's zones, before the last band's: it keeps clear of every zone before it");
    }

    @Test
    void oneIslandAWorldInItsRingNeverOnAnotherZonesGround() {
        for (long seed : SEEDS) {
            assertEquals(1, Z.centres(seed).length);
            int[] c = Z.centres(seed)[0];
            double d = Math.hypot(c[0], c[1]);
            assertTrue(d >= Z.layout().near() - 1 && d <= Z.layout().far() + 1, "in its ring: " + d);
            int m = Z.layout().margin();
            for (int x = c[0] - m; x <= c[0] + m; x += 5) for (int z = c[1] - m; z <= c[1] + m; z += 5) {
                if (!Z.covers(seed, x, z)) continue;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other != Z && other.vertical().overlaps(Z.vertical())) assertFalse(other.covers(seed, x, z), other.id() + " at " + x + " " + z);
                }
            }
        }
    }

    @Test
    void theIslandRisesOutOfTheSeaToATowerPeak() {
        for (long seed : SEEDS) {
            int top = IslandShape.top(seed, IslandShape.SUMMIT_X, IslandShape.SUMMIT_Z, 40);
            assertEquals(IslandShape.SUMMIT_Y, top, "the summit's ground");
            assertTrue(top >= IslandShape.SEA + 150, "towering: " + top);
            // the sea all round it, past its coast
            for (int a = 0; a < 360; a += 15) {
                double r = IslandShape.COAST + IslandShape.COAST_WANDER + 30;
                int x = (int) Math.round(Math.cos(Math.toRadians(a)) * r), z = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
                if (IslandShape.pillarAt(seed, x, z) != null) continue;
                assertTrue(IslandShape.top(seed, x, z, 40) < IslandShape.SEA, "the sea past its coast at " + a + " degrees");
            }
            // still within its region: the sea round it is its own
            assertTrue(IslandShape.COAST + IslandShape.COAST_WANDER + 40 < Z.layout().radius() - Z.layout().wobble(), "the coast inside the region");
            assertFalse(IslandShape.pillars(seed).isEmpty(), "rock pillars");
            assertTrue(IslandShape.pillars(seed).stream().anyMatch(IslandShape.Pillar::sea), "sea stacks too");
            assertTrue(IslandShape.camps(seed).size() >= 5, "the monkeys' camps: " + IslandShape.camps(seed).size());
        }
    }

    @Test
    void theFallsCountry() {
        long seed = 42L;
        // the basin at the cliff's foot, the cliff over it at least to the lip
        assertEquals(IslandShape.BASIN_Y, IslandShape.top(seed, 20, IslandShape.CLIFF_Z + 6, 40));
        assertTrue(IslandShape.top(seed, 15, IslandShape.CLIFF_Z, 40) >= IslandShape.LIP_Y, "a sheer cliff");
        // the river's bed down its gorge to the lip, the sheet falling into the pool
        assertEquals(IslandShape.LIP_Y - 2, IslandShape.top(seed, 0, IslandShape.CLIFF_Z - 20, 40));
        assertTrue(IslandShape.river(0, IslandShape.CLIFF_Z + IslandShape.LIP_OUT));
        for (int x = -IslandShape.FALL_HALF; x <= IslandShape.FALL_HALF; x++) {
            assertTrue(IslandShape.curtain(x, IslandShape.FALL_Z));
            assertTrue(IslandShape.pool(x, IslandShape.FALL_Z), "it falls into the pool");
        }
        assertTrue(IslandShape.ledge(0, IslandShape.CLIFF_Z + 1), "the ledge behind it");
    }

    @Test
    void theStoneEggOnTheSummitIsSplitAndHollow() {
        assertTrue(IslandShape.egg(5, 6, 0) > 0, "its shell");
        assertEquals(0, IslandShape.egg(0, 9, 0), "split down its middle");
        assertEquals(0, IslandShape.egg(2, 8, 0), "hollow");
        assertEquals(0, IslandShape.egg(0, 25, 0));
    }

    @Test
    void theCaveRunsFromBehindTheWaterToTheSanctum() {
        // the mouth opens in the cliff, behind the sheet
        assertTrue(CavePlan.ceiling(0, IslandShape.CLIFF_Z) >= 10);
        // over the chasm the bridge; the stone gate's tablet
        int mid = (CavePlan.CHASM_Z0 + CavePlan.CHASM_Z1) / 2;
        assertTrue(CavePlan.inChasm(15, mid) && CavePlan.onBridge(0, mid) && CavePlan.floor(15, mid) < CavePlan.STREAM_TOP);
        assertTrue(CavePlan.SIGNS.stream().anyMatch(s -> s.lines()[0].equals("花果山福地")));
        assertTrue(CavePlan.SIGNS.stream().anyMatch(s -> s.lines()[0].equals("水帘洞洞天")));
        // the great hall, high-vaulted; the sanctum
        assertTrue(CavePlan.ceiling(0, 0) >= 25, "the hall's vault");
        assertTrue(CavePlan.ceiling(CavePlan.BOSS_X, CavePlan.BOSS_Z) >= 25, "the sanctum's vault");
        assertTrue(CavePlan.inSanctum(CavePlan.BOSS_X, CavePlan.BOSS_Z));
        // a way on foot from the mouth to the sanctum: every column along it open over its floor
        int[][] way = {{0, IslandShape.CLIFF_Z}, {0, 60}, {0, 50}, {0, CavePlan.GATE_Z}, {0, 10}, {CavePlan.TUNNEL_X, 0},
                {CavePlan.TUNNEL_X, -30}, {CavePlan.TUNNEL_X, CavePlan.SANCTUM_Z + 4}, {10, CavePlan.SANCTUM_Z + 4}};
        for (int i = 0; i + 1 < way.length; i++) {
            int[] a = way[i], b = way[i + 1];
            int n = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
            for (int k = 0; k <= n; k++) {
                int x = a[0] + (b[0] - a[0]) * k / n, z = a[1] + (b[1] - a[1]) * k / n;
                int fl = CavePlan.inChasm(x, z) ? 0 : CavePlan.floor(x, z);
                assertTrue(CavePlan.ceiling(x, z) >= fl + 3, "room to walk at " + x + " " + z);
            }
        }
        // the whole cave lies under the mountain
        for (long seed : SEEDS) {
            for (int x = CavePlan.X_MIN; x <= CavePlan.X_MAX; x += 3) for (int z = CavePlan.Z_MIN; z < IslandShape.CLIFF_Z - 2; z += 3) {
                int c = CavePlan.ceiling(x, z);
                if (c <= 0) continue;
                assertTrue(IslandShape.top(seed, x, z, 40) > IslandShape.BASIN_Y + c + 3, "rock over the cave at " + x + " " + z);
            }
        }
        // the posts: the three fake pilgrims, none in the sanctum
        assertEquals(3, CavePlan.POSTS.stream().filter(p -> p.kind() == CavePlan.PILGRIM).count());
        for (CavePlan.Post p : CavePlan.POSTS) {
            assertFalse(CavePlan.inSanctum(p.x(), p.z()), "no one in the sanctum but him");
            assertTrue(CavePlan.ceiling(p.x(), p.z()) >= 3, "a post in the open: " + p);
            assertTrue(p.level() >= Z.minLevel() && p.level() <= Z.maxLevel() + (p.captain() ? 1 : 0), "of the band (a captain or a miniboss at most one over): " + p);
        }
    }
}
