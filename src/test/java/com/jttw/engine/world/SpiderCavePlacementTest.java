package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.spider.SilkRidgeShape;
import com.jttw.engine.world.spider.SilkRidgeWorks;
import com.jttw.engine.world.spider.SpiderCavePlan;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where Silk-Web Ridge lies (dark country, Mount Wuzhuang's ring, never on another zone's ground),
 * its band; the ridge's shape over the cave; and the cave's plan: one way on foot from the cottage
 * through the hall and the seven bowers, each seal shutting all beyond it.
 */
class SpiderCavePlacementTest {

    private static final Zones.Zone S = Zones.SPIDER_CAVE;

    @Test
    void itsHostsAreDarkOvergrownCountry() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/silk_web_ridge_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertTrue(hosts.contains("minecraft:dark_forest") && hosts.contains("minecraft:old_growth_spruce_taiga") && hosts.contains("minecraft:jungle"), hosts.toString());
            assertFalse(hosts.contains("minecraft:desert"), "not dry country");
        }
    }

    @Test
    void itsBandRingAndHeights() {
        var band = S.levelBand();
        assertEquals(78, band.minLevel());
        assertEquals(88, band.maxLevel());
        assertEquals(145, band.baseGruntXp(), "Mount Wuzhuang's, its band's");
        assertEquals(Zones.Vertical.WHOLE_COLUMN, S.vertical());
        assertEquals(Zones.MOUNT_WUZHUANG.layout().near(), S.layout().near(), "the band's ring");
        assertEquals(Zones.MOUNT_WUZHUANG.layout().far(), S.layout().far());
        assertSame(S, Zones.byBiome(Zones.SPIDER_CAVE_BIOME));
        assertTrue(S.layout().radius() - S.layout().wobble() >= SilkRidgeShape.VALLEY + SilkRidgeShape.VALLEY_BLEND, "room for its valley");
    }

    @Test
    void oneRegionAWorldNeverOnAnotherZonesGround() {
        for (long seed : new long[]{1L, 42L, 12345L, -7L, 99L}) {
            int[][] cs = S.centres(seed);
            assertEquals(1, cs.length);
            int[] c = cs[0];
            double d = Math.hypot(c[0], c[1]);
            assertTrue(d >= 6000 && d <= 7500, "6-7.5 km out: " + d);
            int m = S.layout().margin();
            for (int x = c[0] - m; x <= c[0] + m; x += 5) for (int z = c[1] - m; z <= c[1] + m; z += 5) {
                if (!S.covers(seed, x, z)) continue;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other != S && other.vertical().overlaps(S.vertical())) assertFalse(other.covers(seed, x, z), "seed " + seed + ": " + other.id());
                }
            }
        }
    }

    @Test
    void theRidgeKeepsRockOverTheCaveAndTheWorksStandLevel() {
        for (long seed : new long[]{3L, 777L}) {
            int floor = 70;
            for (int x = SpiderCavePlan.MIN_X; x <= SpiderCavePlan.MAX_X; x++) for (int z = SpiderCavePlan.MIN_Z; z <= SpiderCavePlan.MAX_Z; z++) {
                int roof = SpiderCavePlan.roof(x, z);
                if (roof < 0) continue;
                int g = SilkRidgeShape.groundAt(seed, x, z, x, z, 60, floor);
                assertTrue(g >= floor + roof + SilkRidgeShape.COVER - 1, "rock over the cave at " + x + " " + z);
            }
            // the cottage's yard, the spring's ground and the temple's terrace: the valley floor, level
            int[][] level = {{SilkRidgeWorks.COTTAGE_X, SilkRidgeWorks.COTTAGE_Z + 8}, {SilkRidgeWorks.SPRING_X, SilkRidgeWorks.SPRING_Z},
                    {SilkRidgeWorks.TEMPLE_X, SilkRidgeWorks.TEMPLE_Z}, {SilkRidgeWorks.TEMPLE_X + 20, SilkRidgeWorks.TEMPLE_Z - 25}};
            for (int[] p : level) assertEquals(floor - 1, SilkRidgeShape.groundAt(seed, p[0], p[1], p[0], p[1], 60, floor), "level at " + p[0] + " " + p[1]);
            // and the ridge stands high over the valley at its peaks
            double high = 0;
            for (int x = -100; x <= 100; x += 2) high = Math.max(high, SilkRidgeShape.ridge(seed, x, -14, x, -14));
            assertTrue(high >= 60, "a towering ridge: " + high);
        }
    }

    /** The open floor reachable on foot from the cottage's back door, with these tunnels' seals still standing. */
    private static Set<Long> reachable(Set<Integer> loosened) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> todo = new ArrayDeque<>();
        todo.add(new int[]{0, SpiderCavePlan.PASSAGE_S});
        while (!todo.isEmpty()) {
            int[] q = todo.poll();
            long key = ((long) q[0] << 32) ^ (q[1] & 0xFFFFFFFFL);
            if (!seen.add(key)) continue;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = q[0] + d[0], z = q[1] + d[1];
                if (SpiderCavePlan.roof(x, z) < 2) continue;
                int code = SpiderCavePlan.at(x, 0, z);
                if (code == SpiderCavePlan.NONE || code == SpiderCavePlan.SILK) continue;
                boolean sealed = false;
                for (int t = 1; t < SpiderCavePlan.TUNNELS.size(); t++) if (!loosened.contains(t) && SpiderCavePlan.inSeal(t, x, z)) sealed = true;
                if (sealed) continue;
                todo.add(new int[]{x, z});
            }
        }
        return seen;
    }

    private static boolean reaches(Set<Long> seen, SpiderCavePlan.Chamber c) {
        return seen.contains(((long) c.x() << 32) ^ (c.z() & 0xFFFFFFFFL));
    }

    @Test
    void eachSisterHoldsTheWayOnFromHerBower() {
        assertEquals(7, SpiderCavePlan.BOWERS.size());
        Set<Integer> loosened = new HashSet<>();
        for (int b = 0; b < SpiderCavePlan.BOWER_COUNT; b++) {
            Set<Long> seen = reachable(loosened);
            assertTrue(reaches(seen, SpiderCavePlan.HALL), "the hall from the cottage");
            for (int k = 0; k < SpiderCavePlan.BOWER_COUNT; k++) {
                assertEquals(k <= b, reaches(seen, SpiderCavePlan.BOWERS.get(k)), "with " + b + " sisters fallen, bower " + k);
            }
            int seal = SpiderCavePlan.sealOf(b);
            if (seal > 0) {
                assertFalse(SpiderCavePlan.sealBlocks(seal).isEmpty(), "a seal of silk on from bower " + b);
                loosened.add(seal);
            }
        }
        assertEquals(-1, SpiderCavePlan.sealOf(SpiderCavePlan.BOWER_COUNT - 1), "the last bower is the end");
    }

    @Test
    void theBowersAreGreatRoomsAndTheHallGreater() {
        int hall = 0;
        for (int x = -40; x <= 40; x++) for (int z = -25; z <= 30; z++) if (SpiderCavePlan.HALL.heightAt(x + 0.5, z + 0.5) >= 3) hall++;
        assertTrue(hall >= 1800, "the Silk Hall's floor: " + hall);
        for (SpiderCavePlan.Chamber b : SpiderCavePlan.BOWERS) {
            int n = 0;
            for (int x = b.x() - b.rx(); x <= b.x() + b.rx(); x++) for (int z = b.z() - b.rz(); z <= b.z() + b.rz(); z++) if (b.heightAt(x + 0.5, z + 0.5) >= 3) n++;
            assertTrue(n >= 300, "a bower's floor: " + n);
        }
        // the sisters, youngest first, eldest last; all within the band
        for (int b = 0; b < SpiderCavePlan.BOWER_COUNT; b++) {
            int lv = SpiderCavePlan.sisterLevel(b);
            assertTrue(lv >= 78 && lv <= 88, "sister level " + lv);
        }
        assertEquals(6, SpiderCavePlan.sisterOf(0));
        assertEquals(0, SpiderCavePlan.sisterOf(6));
        for (SpiderCavePlan.Post p : SpiderCavePlan.POSTS) {
            assertTrue(p.level() >= 78 && p.level() <= 88, "a keeper of the band: " + p);
            assertTrue(SpiderCavePlan.roof(p.x(), p.z()) >= 2, "on open floor: " + p);
        }
    }

    @Test
    void theCanopyClosesOverTheTemple() {
        // a shell with no gap: from the court, no way out over the ground without crossing it
        for (int tx = -SilkRidgeWorks.TEMPLE_HALF; tx <= SilkRidgeWorks.TEMPLE_HALF; tx++) {
            for (int tz = SilkRidgeWorks.TEMPLE_N; tz <= SilkRidgeWorks.TEMPLE_S; tz++) {
                int hits = 0;
                for (int ly = 0; ly <= SilkRidgeWorks.CANOPY_H + 1; ly++) if (SilkRidgeWorks.inCanopy(tx, ly, tz)) hits++;
                assertTrue(hits >= 1, "canopy over " + tx + " " + tz);
            }
        }
        for (int tz = -SilkRidgeWorks.CANOPY_RZ - 1; tz <= SilkRidgeWorks.CANOPY_RZ + 1; tz++) {
            boolean crossed = false;
            for (int tx = 0; tx <= SilkRidgeWorks.CANOPY_RX + 1; tx++) crossed |= SilkRidgeWorks.inCanopy(tx, 0, tz) || SilkRidgeWorks.inCanopy(tx, 1, tz);
            if (Math.abs(tz) < SilkRidgeWorks.CANOPY_RZ - 1) assertTrue(crossed, "walled at ground level, row " + tz);
        }
    }
}
