package com.jttw.engine.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.kunlun.KunlunPalace;
import com.jttw.engine.world.kunlun.KunlunShape;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where Mount Kunlun lies: one a world, 5-8 km out and within 30 degrees of due west, on mountain
 * country, never on any zone's ground nor Fengdu's, the same for the same seed; a place of no band;
 * its peak, its stair and its summit's palace inside its region; the peak's shape and the stair's
 * climb (walked in half steps, never a whole block at once); and the Banana-Leaf Fan found in its
 * treasure's loot, once, and nowhere else.
 */
class KunlunPlacementTest {

    private static final Zones.Zone K = Zones.KUNLUN;
    private static final long[] SEEDS = {1L, 42L, 12345L, 987654321L, -7L};

    /** Host ground in patches 600 blocks square, a third of them for each zone: a stand-in for the biomes (as ZonesTest's). */
    private static final ZoneMap.Hosts PATCHES = (zone, x, z) ->
            ZoneLayout.unit(ZoneLayout.mix(zone.layout().salt(), Math.floorDiv(x, 600), Math.floorDiv(z, 600))) < 0.33;

    @Test
    void itsHostsAreMountainCountry() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/kunlun_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            for (String h : List.of("jagged_peaks", "frozen_peaks", "stony_peaks", "snowy_slopes", "meadow", "grove")) {
                assertTrue(hosts.contains("minecraft:" + h), h + " in " + hosts);
            }
            assertFalse(hosts.stream().anyMatch(h -> h.contains("ocean") || h.contains("desert") || h.contains("river")), "no sea, desert or river");
        }
        assertEquals("jttw_engine:kunlun_hosts", K.hosts().location().toString());
        assertEquals(256, K.hostY(), "judged over any peak's top");
    }

    @Test
    void itIsAPlaceOfNoBandLastOfAll() {
        assertFalse(K.hasLevelBand(), "no band: no one is sent there by level, it pays no XP");
        assertEquals(Zones.ALL.size() - 1, Zones.ALL.indexOf(K), "last: it keeps clear of every zone and of Fengdu");
        assertTrue(Zones.MAP.zones().contains(K));
        assertSame(K, Zones.byBiome(Zones.KUNLUN_BIOME));
        assertEquals(Zones.Vertical.WHOLE_COLUMN, K.vertical(), "the whole column: no zone shares its ground at any height");
        assertEquals(ZoneLayout.WEST, K.layout().quarter(), "kept to the west");
        for (int lv = 1; lv <= 150; lv++) assertNotSame(K, com.jttw.engine.world.EarthGod.zoneFor(lv), "no band sends anyone there: " + lv);
    }

    @Test
    void itLiesOnceAWorldFiveToEightKilometresWest() {
        assertEquals(5000, K.layout().near());
        assertEquals(8000, K.layout().far());
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : new ZoneMap.Placement[]{Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES)}) {
                int[][] cs = p.of(K);
                assertEquals(1, cs.length, "one Kunlun a world");
                double d = Math.hypot(cs[0][0], cs[0][1]);
                assertTrue(d >= 5000 - 1 && d <= 8000 + 1, "seed " + seed + ": 5-8 km out: " + d);
                double bearing = Math.toDegrees(Math.atan2(cs[0][1], cs[0][0]));
                assertTrue(Math.abs(Math.abs(bearing) - 180) <= 30.5, "seed " + seed + ": within 30 degrees of west: " + bearing);
                assertEquals("west", EarthGod.wind(0, 0, cs[0][0], cs[0][1]).replace("north-", "").replace("south-", ""), "the Earth Gods say west");
                assertFalse(p.notes().contains(K.id()), "found in its own ring: " + p.notes());
            }
        }
        // the candidates themselves keep to the west, in every ring, widened or not
        for (int i = 0; i < 2000; i++) {
            int[] c = K.layout().candidate(77L, 0, i, 5000, 12000);
            assertEquals(ZoneLayout.WEST, ZoneLayout.quarterOf(c[0], c[1]), "candidate " + i + " at " + c[0] + " " + c[1]);
        }
        // widened (no host ground in its ring), it is still in the west
        ZoneMap.Placement far = Zones.MAP.place(5L, (zone, x, z) -> Math.hypot(x, z) > 8000);
        int[] w = far.of(K)[0];
        assertTrue(Math.hypot(w[0], w[1]) > 8000 && ZoneLayout.quarterOf(w[0], w[1]) == ZoneLayout.WEST, "widened, west: " + w[0] + " " + w[1]);
        assertTrue(far.notes().contains(K.id() + ": no host ground 5000-8000 blocks out"), far.notes());
    }

    @Test
    void itStandsOnItsHosts() {
        for (long seed : SEEDS) {
            int[] c = Zones.MAP.place(seed, PATCHES).of(K)[0];
            assertTrue(PATCHES.at(K, c[0], c[1]), "seed " + seed + ": its centre on host ground");
            int ring = 0;
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4, r = K.layout().radius() * ZoneMap.RING_SHARE;
                if (PATCHES.at(K, c[0] + (int) Math.round(Math.cos(a) * r), c[1] + (int) Math.round(Math.sin(a) * r))) ring++;
            }
            assertTrue(ring >= ZoneMap.RING_HOSTS, "host ground round it too: " + ring);
        }
    }

    @Test
    void itNeverSharesGroundWithAnyZoneNorFengdu() {
        int ground = 0;
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : new ZoneMap.Placement[]{Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES)}) {
                int[] c = p.of(K)[0];
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other == K) continue;
                    for (int[] o : p.of(other)) {
                        assertTrue(Math.hypot(o[0] - c[0], o[1] - c[1]) >= K.layout().margin() + other.layout().margin() + ZoneMap.GAP,
                                "seed " + seed + ": " + other.id() + " too near");
                    }
                }
            }
            int[] c = K.centres(seed)[0];
            int m = K.layout().margin();
            for (int x = c[0] - m; x <= c[0] + m; x += 4) for (int z = c[1] - m; z <= c[1] + m; z += 4) {
                if (!K.covers(seed, x, z)) continue;
                ground++;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other != K) assertFalse(other.covers(seed, x, z), "seed " + seed + " at " + x + " " + z + " is " + other.id() + "'s too");
                }
            }
        }
        assertTrue(ground > 5 * 5000, "it has ground: " + ground);
    }

    @Test
    void theSameSeedPutsItInTheSamePlaceAndAnotherElsewhere() {
        for (long seed : SEEDS) {
            assertArrayEquals(Zones.MAP.place(seed, PATCHES).of(K)[0], Zones.MAP.place(seed, PATCHES).of(K)[0], "seed " + seed);
            assertArrayEquals(Zones.MAP.placement(seed).of(K)[0], K.centres(seed)[0]);
        }
        int differ = 0;
        for (long seed = 1; seed < 20; seed++) {
            int[] a = K.centres(seed)[0], b = K.centres(seed + 1)[0];
            if (a[0] != b[0] || a[1] != b[1]) differ++;
        }
        assertTrue(differ >= 18, "another seed, another place: " + differ);
    }

    @Test
    void thePeakItsStairAndThePalaceFitInsideItsRegion() {
        assertTrue(KunlunShape.REACH <= K.layout().radius() - K.layout().wobble(), "the peak's foot within the region's least reach");
        assertTrue(KunlunShape.PATH_FROM + KunlunShape.SHOULDER < KunlunShape.REACH, "the stair's foot too");
        for (int x = -40; x <= 40; x++) for (int z = -40; z <= 40; z++) {
            if (KunlunPalace.covers(x, z)) assertTrue(Math.hypot(x, z) <= KunlunShape.TOP + 1.5, "the palace on the summit: " + x + " " + z);
        }
        for (long seed : SEEDS) {
            int[] c = K.centres(seed)[0];
            for (int a = 0; a < 360; a += 5) {
                double r = KunlunShape.REACH;
                int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * r), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
                assertTrue(K.depth(seed, c[0] + dx, c[1] + dz) > 0, "seed " + seed + ": the peak's foot within its region at " + a + " degrees");
            }
        }
    }

    @Test
    void thePeaksShape() {
        long seed = 42L;
        int summit = 250;
        for (int dx = -KunlunShape.TOP; dx <= KunlunShape.TOP; dx += 2) for (int dz = -KunlunShape.TOP; dz <= KunlunShape.TOP; dz += 2) {
            if (KunlunShape.onTop(dx, dz)) assertEquals(summit, KunlunShape.height(seed, 500 + dx, 700 + dz, dx, dz, 100, summit), 1e-9, "level at " + dx + " " + dz);
        }
        // the land's own past the foot, and between, falling the farther out: well over the land halfway, steep under the top
        int rises = 0;
        for (int a = 0; a < 360; a += 15) {
            double ca = Math.cos(Math.toRadians(a)), sa = Math.sin(Math.toRadians(a));
            int fx = (int) Math.round(ca * (KunlunShape.REACH + 2)), fz = (int) Math.round(sa * (KunlunShape.REACH + 2));
            assertEquals(100, KunlunShape.height(seed, fx, fz, fx, fz, 100, summit), 1e-9, "the land's own past the foot at " + a);
            int mx = (int) Math.round(ca * 60), mz = (int) Math.round(sa * 60);
            double mid = KunlunShape.height(seed, mx, mz, mx, mz, 100, summit);
            assertTrue(mid > 120 && mid < summit, "60 out, well over the land and under the summit: " + mid + " at " + a);
            int nx = (int) Math.round(ca * 32), nz = (int) Math.round(sa * 32);
            if (KunlunShape.height(seed, nx, nz, nx, nz, 100, summit) > mid + 20) rises++;
        }
        assertTrue(rises >= 20, "the peak rises steeply over its flanks: " + rises + " of 24");
        // the summit: over the highest land near and over the foot, kept under the sky
        assertEquals(236, KunlunShape.summit(7L, 10, 10, (x, z) -> 60, 236, 288), "at least its least");
        assertEquals(250 + KunlunShape.HEADROOM, KunlunShape.summit(7L, 20, 20, (x, z) -> x == 20 + 160 && z == 20 ? 250 : 100, 200, 300), "over the highest near it");
        assertEquals(288, KunlunShape.summit(7L, 30, 30, (x, z) -> 270, 236, 288), "under the sky");
        assertEquals(236, KunlunShape.summit(7L, 10, 10, (x, z) -> 200, 236, 288), "cached");
        assertEquals(70 + KunlunShape.MIN_RISE, KunlunShape.summit(7L, 40, 40, (x, z) -> 70, 100, 288), "well over its foot");
    }

    @Test
    void theStairClimbsFromTheFootToTheSummitInHalfSteps() {
        for (long seed : SEEDS) {
            int summit = 240 + (int) Math.floorMod(seed, 40);
            java.util.function.IntBinaryOperator land = (x, z) -> 90 + (int) (25 * Math.sin(x / 60.0) * Math.cos(z / 45.0));
            int cx = 3000 + (int) Math.floorMod(seed, 1000), cz = -200;
            KunlunShape.Path path = KunlunShape.path(seed, cx, cz, summit, land);
            int n = path.size();
            assertTrue(n > 300, "a long way up: " + n);
            // from the foot, out by the mountain's edge, to the summit's south edge, the palace's gate before it
            assertTrue(Math.hypot(path.x[0], path.z[0]) >= KunlunShape.PATH_FROM - 30, "it sets out at the foot");
            assertEquals(0, path.x[n - 1], 1e-9);
            assertEquals(KunlunShape.TOP + 1, path.z[n - 1], 1e-9, "it ends at the summit's south edge");
            assertEquals(summit, path.y[n - 1], 1e-9, "at the summit's height");
            assertTrue(KunlunPalace.onApproach(0, KunlunShape.TOP), "where the way in to the terrace begins");
            int fx = cx + (int) Math.floor(path.x[0]), fz = cz + (int) Math.floor(path.z[0]);
            assertEquals(land.applyAsInt(fx, fz), path.y[0], 8.0, "it starts on the land at its foot");
            assertTrue(Math.hypot(path.x[0], path.z[0]) <= KunlunShape.REACH - 3, "inside the mountain's reach");
            for (int i = 1; i < n; i++) {
                assertTrue(Math.hypot(path.x[i] - path.x[i - 1], path.z[i] - path.z[i - 1]) <= 1.01, "a block apart");
                assertTrue(Math.abs(path.y[i] - path.y[i - 1]) <= KunlunShape.MAX_CLIMB + 1e-9, "never steeper than its climb at " + i);
                assertTrue(path.y[i] <= summit);
            }
            // its turns never come close to each other: a turn lower down is well apart
            for (int i = 0; i < n; i += 3) for (int j = i + 80; j < n; j += 3) {
                double d = Math.hypot(path.x[i] - path.x[j], path.z[i] - path.z[j]);
                assertTrue(d > 2 * KunlunShape.SHOULDER + 4 || Math.abs(j - i) < 80, "turns apart: " + i + " and " + j + " " + d);
            }
            // walked block by block: from any block of its way to the next, up or down half a block at most
            List<int[]> way = new ArrayList<>();
            for (int x = -KunlunShape.REACH; x <= KunlunShape.REACH; x++) for (int z = -KunlunShape.REACH; z <= KunlunShape.REACH; z++) {
                if (KunlunPalace.covers(x, z)) continue;
                var hit = path.nearest(x, z, KunlunShape.PATH_HALF);
                if (hit != null) way.add(new int[]{x, z, (int) Math.floor(2 * hit.y())});
            }
            assertTrue(way.size() > n * 2, "three or four wide: " + way.size());
            java.util.Map<Long, Integer> half = new java.util.HashMap<>();
            for (int[] w : way) half.put(((long) w[0] << 32) ^ (w[1] & 0xFFFFFFFFL), w[2]);
            for (int[] w : way) {
                for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                    Integer o = half.get(((long) (w[0] + d[0]) << 32) ^ ((w[1] + d[1]) & 0xFFFFFFFFL));
                    if (o != null) assertTrue(Math.abs(o - w[2]) <= 1, "a half step at most at " + w[0] + " " + w[1] + ": " + w[2] + " to " + o);
                }
            }
        }
    }

    @Test
    void theBananaLeafFanIsInKunlunsTreasureOnceAndNowhereElse() throws Exception {
        Path data = Path.of(getClass().getResource("/data/jttw_engine").toURI()).getParent();
        assertTrue(Files.isDirectory(data), "the resources' data: " + data.toAbsolutePath());
        List<Path> holding = new ArrayList<>();
        try (Stream<Path> files = Files.walk(data)) {
            for (Path f : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                if (Files.readString(f).contains("banana_leaf_fan")) holding.add(f);
            }
        }
        assertEquals(List.of(data.resolve("jttw_engine/loot_table/chests/kunlun_treasure.json")), holding,
                "no loot table, recipe or tag but Kunlun's treasure gives the fan");
        JsonObject table = JsonParser.parseString(Files.readString(holding.get(0))).getAsJsonObject();
        int fans = 0, alone = 0;
        for (JsonElement pool : table.getAsJsonArray("pools")) {
            JsonArray entries = pool.getAsJsonObject().getAsJsonArray("entries");
            for (JsonElement e : entries) {
                if (!e.getAsJsonObject().get("name").getAsString().equals("jttw_engine:banana_leaf_fan")) continue;
                fans++;
                if (entries.size() == 1 && pool.getAsJsonObject().get("rolls").getAsInt() == 1 && !e.getAsJsonObject().has("functions")) alone++;
            }
        }
        assertEquals(1, fans, "named once");
        assertEquals(1, alone, "alone in a pool of one roll: always exactly one");
        String all = Files.readString(holding.get(0));
        for (String weapon : List.of("ruyi_jingu_bang", "three_pointed_spear", "erlang_axe", "fire_tip_spear", "qiankun_ring", "yin_yang_sword",
                "wind_fire_wheels", "huntian_cloth", "bell_of_taiyi", "nine_tooth_rake", "yin_yang_vase", "mixed_iron_cudgel", "red_boy_spear")) {
            assertFalse(all.contains(weapon), "no other signature weapon: " + weapon);
        }
    }
}
