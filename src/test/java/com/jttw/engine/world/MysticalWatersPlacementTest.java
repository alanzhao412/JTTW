package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.Sea;
import com.jttw.engine.world.waters.TrenchShape;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Where the Mystical Waters lie: one region in each of the Four Seas, never on another zone's ground; its band; its trench's shape. */
class MysticalWatersPlacementTest {

    private static final Zones.Zone W = Zones.MYSTICAL_WATERS;
    private static final int MIN_Y = -64;

    @Test
    void itsHostsAreTheDeepOceansAlone() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/mystical_waters_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertEquals(Set.of("minecraft:deep_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_lukewarm_ocean",
                    "minecraft:deep_frozen_ocean"), hosts);
        }
        assertEquals("jttw_engine:mystical_waters_hosts", W.hosts().location().toString());
    }

    @Test
    void itsBandAndHeights() {
        var band = W.levelBand();
        assertEquals(47, band.minLevel());
        assertEquals(56, band.maxLevel());
        assertEquals(110, band.baseGruntXp());
        assertEquals(Zones.Vertical.WHOLE_COLUMN, W.vertical(), "the sea over its trench is its too");
        assertEquals(60, W.hostY(), "judged just under the waves");
        assertTrue(Zones.MAP.zones().contains(W));
        assertSame(W, Zones.byBiome(Zones.MYSTICAL_WATERS_BIOME));
    }

    @Test
    void itNeverSharesGroundWithAnotherZone() {
        int waters = 0;
        for (long seed : new long[]{1L, 42L, 12345L, -7L}) {
            for (int[] c : W.centres(seed)) {
                for (int x = c[0] - 270; x <= c[0] + 270; x += 5) for (int z = c[1] - 270; z <= c[1] + 270; z += 5) {
                    if (!W.covers(seed, x, z)) continue;
                    waters++;
                    for (Zones.Zone other : Zones.MAP.zones()) {
                        if (other != W && other.vertical().overlaps(W.vertical())) {
                            assertFalse(other.covers(seed, x, z), "seed " + seed + " at " + x + " " + z + " is " + other.id() + "'s too");
                        }
                    }
                }
            }
        }
        assertTrue(waters > 1000, "it has regions: " + waters);
    }

    @Test
    void oneSeaEachWayEachItsOwnKings() {
        for (long seed : new long[]{1L, 42L, 12345L, -7L, 99L}) {
            int[][] cs = W.centres(seed);
            assertEquals(4, cs.length, "four seas");
            for (Sea sea : Sea.values()) {
                int[] c = cs[sea.ordinal()];
                assertEquals(sea, Sea.of(c[0], c[1]), "seed " + seed + ": the " + sea + " sea's palace is " + sea + "'s king's");
                double d = Math.hypot(c[0], c[1]);
                assertTrue(d >= 3000 && d <= 4500, "3-4.5 km out: " + d);
            }
        }
    }

    @Test
    void eachRegionsPalaceIsTheSeaThatLiesThatWay() {
        assertEquals(Sea.EAST, Sea.of(5000, 100));
        assertEquals(Sea.WEST, Sea.of(-5000, 1200));
        assertEquals(Sea.SOUTH, Sea.of(300, 4000));
        assertEquals(Sea.NORTH, Sea.of(-300, -4000));
        for (Sea sea : Sea.values()) {
            // the gate faces its sea
            BlockPos gate = DragonPalace.toWorld(sea, 0, 0, 10);
            assertEquals(sea.facing.getStepX() * 10, gate.getX(), sea + " gate x");
            assertEquals(sea.facing.getStepZ() * 10, gate.getZ(), sea + " gate z");
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                BlockPos w = DragonPalace.toWorld(sea, x, 0, z);
                assertArrayEquals(new int[]{x, z}, DragonPalace.toLocal(sea, w.getX(), w.getZ()), sea + " turns back");
            }
        }
    }

    @Test
    void theTrenchSinksToJustOverTheBedrock() {
        long seed = 12345L;
        int checked = 0;
        for (int[] c : W.centres(seed)) {
            checked++;
            int floor = TrenchShape.floorY(MIN_Y);
            assertEquals(-58, floor, "six over the bottom: just over the bedrock");
            assertEquals(floor, TrenchShape.groundAt(seed, c[0], c[1], 30, MIN_Y), "level at the throne");
            // the palace's floor is level
            for (int dx = -DragonPalace.LEVEL_RADIUS + 2; dx <= DragonPalace.LEVEL_RADIUS - 2; dx += 3) {
                assertEquals(floor, TrenchShape.groundAt(seed, c[0] + dx, c[1], 30, MIN_Y), "level round the palace");
            }
            // out along a line from the centre: deep, then a steep wall, then the sea floor
            int last = floor, steepest = 0;
            for (int d = 0; d < 400; d++) {
                int y = TrenchShape.groundAt(seed, c[0] + d, c[1], 30, MIN_Y);
                assertTrue(y >= floor && y <= 30, "between the floor and the sea floor: " + y);
                steepest = Math.max(steepest, y - last);
                last = y;
            }
            assertEquals(30, last, "the sea floor beyond the region");
            assertTrue(steepest >= 4, "its walls are steep: " + steepest + " blocks in one");
            assertTrue(30 - floor >= 85, "some 90 blocks of trench under a deep ocean's floor");
        }
        assertTrue(checked > 0, "found regions");
    }
}
