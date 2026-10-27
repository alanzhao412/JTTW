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
 * its massif, its stair and its summit's palace inside its region; the massif's shape (a level top,
 * a steep summit pyramid, a base more than 680 blocks across, its lesser peaks over their saddles,
 * a gorge between each two spurs, the snow over its upper half, the cloud belts under the summit);
 * the summit's height under the taller sky and an older world's; the stair's climb (walked in half
 * steps, never a whole block at once, from the foot to the summit's edge, its turns apart) and the
 * gate, pavilions and shrines along it; and the Banana-Leaf Fan found in its treasure's loot, once,
 * and nowhere else.
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
    void theMassifItsStairAndThePalaceFitInsideItsRegion() {
        assertTrue(KunlunShape.REACH <= K.layout().radius() - K.layout().wobble(), "the massif's foot within the region's least reach");
        for (int x = -60; x <= 60; x++) for (int z = -60; z <= 60; z++) {
            if (KunlunPalace.covers(x, z)) assertTrue(Math.hypot(x, z) <= KunlunShape.TOP + 1.5, "the palace on the summit: " + x + " " + z);
        }
        for (long seed : SEEDS) {
            int[] c = K.centres(seed)[0];
            for (int a = 0; a < 360; a += 5) {
                double r = KunlunShape.REACH;
                int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * r), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
                assertTrue(K.depth(seed, c[0] + dx, c[1] + dz) > 0, "seed " + seed + ": the massif's foot within its region at " + a + " degrees");
            }
            KunlunShape.Path path = KunlunShape.path(seed, c[0], c[1], KunlunShape.SUMMIT_Y, LAND);
            for (int i = 0; i < path.size(); i += 7) {
                assertTrue(Math.hypot(path.x[i], path.z[i]) + KunlunShape.SHOULDER + KunlunShape.BANK < KunlunShape.REACH + 10,
                        "seed " + seed + ": the stair inside the massif's reach at " + i);
            }
        }
    }

    /** A rolling land round the massif's foot, about y 90. */
    private static final java.util.function.IntBinaryOperator LAND = (x, z) -> 90 + (int) (25 * Math.sin(x / 60.0) * Math.cos(z / 45.0));

    /** The massif's height at local (dx, dz) of the region at (cx, cz) over the rolling land, its summit {@code summit}. */
    private static double at(long seed, int cx, int cz, int dx, int dz, int summit) {
        return KunlunShape.height(seed, cx + dx, cz + dz, dx, dz, LAND.applyAsInt(cx + dx, cz + dz), summit);
    }

    @Test
    void theMassifsShape() {
        int summit = KunlunShape.SUMMIT_Y, cx = 4000, cz = -900;
        for (long seed : SEEDS) {
            // the summit's level top
            for (int dx = -KunlunShape.TOP; dx <= KunlunShape.TOP; dx += 4) for (int dz = -KunlunShape.TOP; dz <= KunlunShape.TOP; dz += 4) {
                if (KunlunShape.onTop(dx, dz)) assertEquals(summit, at(seed, cx, cz, dx, dz, summit), 1e-9, "level at " + dx + " " + dz);
            }
            int steep = 0, wide = 0;
            for (int a = 0; a < 360; a += 10) {
                double ca = Math.cos(Math.toRadians(a)), sa = Math.sin(Math.toRadians(a));
                // the land's own past the foot
                int fx = (int) Math.round(ca * (KunlunShape.REACH + 2)), fz = (int) Math.round(sa * (KunlunShape.REACH + 2));
                assertEquals(LAND.applyAsInt(cx + fx, cz + fz), at(seed, cx, cz, fx, fz, summit), 1e-9, "the land's own past the foot at " + a);
                assertEquals(0, KunlunShape.field(seed, cx + fx, cz + fz, fx, fz), 1e-12, "nothing of it past its reach at " + a);
                // the summit pyramid steep under the top: a fall of at least 120 in the first 80 blocks out
                int px = (int) Math.round(ca * (KunlunShape.TOP + 80)), pz = (int) Math.round(sa * (KunlunShape.TOP + 80));
                if (summit - at(seed, cx, cz, px, pz, summit) >= 120) steep++;
                // and the massif broad: still well over the land 340 blocks out
                int wx = (int) Math.round(ca * 340), wz = (int) Math.round(sa * 340);
                if (KunlunShape.field(seed, cx + wx, cz + wz, wx, wz) > 0.02) wide++;
                // falling all the way, though not everywhere evenly: higher 150 out than 300 out
                int mx = (int) Math.round(ca * 150), mz = (int) Math.round(sa * 150), ox = (int) Math.round(ca * 300), oz = (int) Math.round(sa * 300);
                assertTrue(at(seed, cx, cz, mx, mz, summit) > at(seed, cx, cz, ox, oz, summit) + 40, "seed " + seed + ": falling from the summit at " + a);
                assertTrue(at(seed, cx, cz, mx, mz, summit) < summit - 60, "seed " + seed + ": well under the summit 150 out at " + a);
            }
            assertTrue(steep >= 30, "seed " + seed + ": the summit pyramid falls steeply on most sides: " + steep + " of 36");
            assertTrue(wide >= 33, "seed " + seed + ": its base 680 blocks across and more: " + wide + " of 36");
            // its lesser peaks: one on each spur but one, each standing well over the saddle between it and the summit
            KunlunShape.Plan plan = KunlunShape.plan(seed);
            assertEquals(KunlunShape.PEAKS, plan.peaks().length, "seed " + seed + ": its lesser peaks");
            for (KunlunShape.Peak pk : plan.peaks()) {
                double rho = Math.hypot(pk.x(), pk.z());
                double top = at(seed, cx, cz, (int) Math.round(pk.x()), (int) Math.round(pk.z()), summit);
                double saddle = Double.MAX_VALUE;
                for (double r = KunlunShape.TOP + 60; r < rho - 10; r += 2) {
                    int sx = (int) Math.round(pk.x() / rho * r), sz = (int) Math.round(pk.z() / rho * r);
                    saddle = Math.min(saddle, KunlunShape.field(seed, cx + sx, cz + sz, sx, sz));
                }
                double topField = KunlunShape.field(seed, cx + (int) Math.round(pk.x()), cz + (int) Math.round(pk.z()), (int) Math.round(pk.x()), (int) Math.round(pk.z()));
                assertTrue(topField > saddle + 0.05, "seed " + seed + ": a lesser peak " + rho + " out stands over its saddle: " + topField + " over " + saddle);
                assertTrue(top < summit - 100, "seed " + seed + ": and well under the summit: " + top);
            }
            // gorges between the spurs, none high up
            int gorges = 0;
            for (double g : plan.gorgeAt()) {
                double deepest = 0;
                for (int r = KunlunShape.GORGE_FROM; r < KunlunShape.REACH; r += 5) {
                    int gx = (int) Math.round(Math.cos(g) * r), gz = (int) Math.round(Math.sin(g) * r);
                    deepest = Math.max(deepest, KunlunShape.gorge(seed, cx + gx, cz + gz, gx, gz));
                }
                if (deepest > 20) gorges++;
            }
            assertEquals(KunlunShape.SPURS, gorges, "seed " + seed + ": a deep gorge between each two spurs");
            for (int a = 0; a < 360; a += 5) {
                int gx = (int) Math.round(Math.cos(Math.toRadians(a)) * (KunlunShape.GORGE_FROM - 20)), gz = (int) Math.round(Math.sin(Math.toRadians(a)) * (KunlunShape.GORGE_FROM - 20));
                assertEquals(0, KunlunShape.gorge(seed, cx + gx, cz + gz, gx, gz), 1e-12, "no gorge high up");
            }
            // the same seed, the same mountain
            for (int i = 0; i < 50; i++) {
                int dx = (i * 37) % 800 - 400, dz = (i * 91) % 800 - 400;
                assertEquals(at(seed, cx, cz, dx, dz, summit), at(seed, cx, cz, dx, dz, summit), 0, "the same");
            }
        }
        // the snow over its upper half, the clouds in belts under the summit and never over it
        int foot = 90;
        double snow = KunlunShape.snowLine(42L, 10, 10, summit, foot);
        assertEquals(summit - (summit - foot) / 2.0, snow, 9, "the snow from halfway up");
        for (int b = 0; b < KunlunShape.BELTS; b++) {
            int y = KunlunShape.beltY(b, summit, foot);
            assertTrue(y > foot + 100 && y < summit - 80, "belt " + b + " at " + y + ": over the foot, well under the summit");
            if (b > 0) assertTrue(y > KunlunShape.beltY(b - 1, summit, foot) + 60, "the belts apart");
        }
        int clouds = 0;
        for (int x = -400; x <= 400; x += 3) for (int z = -400; z <= 400; z += 3) {
            int ground = (int) at(42L, cx, cz, x, z, summit);
            for (int b = 0; b < KunlunShape.BELTS; b++) {
                int mid = KunlunShape.beltY(b, summit, foot);
                for (int y = mid - 8; y <= mid + 12; y++) {
                    if (KunlunShape.cloud(42L, cx + x, y, cz + z, b, ground, summit, foot) > 0) {
                        clouds++;
                        assertTrue(y > ground, "a cloud in the open, not in the ground");
                        assertTrue(y < summit - 60, "the summit over the clouds");
                    }
                }
            }
        }
        assertTrue(clouds > 2000, "clouds: " + clouds);
    }

    @Test
    void theSummitStandsAtItsHeightUnderTheTallerSkyAndUnderTheOlderSky() {
        // under the taller sky, y 596, whatever the land
        assertTrue(KunlunShape.SUMMIT_Y >= 560 && KunlunShape.SUMMIT_Y <= 620, "the axis of the world: " + KunlunShape.SUMMIT_Y);
        assertEquals(640 - KunlunShape.UNDER_SKY, KunlunShape.SUMMIT_Y, "as high as the taller sky allows, with room for the palace");
        assertTrue(KunlunShape.SUMMIT_Y + KunlunPalace.TOP < 640, "the palace's roof under the sky");
        assertEquals(596, KunlunShape.summit(7L, 10, 10, (x, z) -> 60, 248, 596), "over low land");
        assertEquals(596, KunlunShape.summit(7L, 20, 20, (x, z) -> x == 20 + 160 && z == 20 ? 300 : 100, 248, 596), "over high land near");
        // under an older world's sky (y 320): pressed down to fit, the palace under it still
        int older = 320 - KunlunShape.UNDER_SKY;
        assertEquals(older, KunlunShape.summit(7L, 10, 10, (x, z) -> 60, 248, older), "in an older world, as high as its sky allows");
        assertTrue(older + KunlunPalace.TOP < 320, "the palace's roof under the older sky");
        assertEquals(596, KunlunShape.summit(7L, 10, 10, (x, z) -> 200, 248, 596), "worked out for each sky, not cached across them");
        // the massif pressed under it: never over its summit, its stair from the foot to it in half steps
        long seed = 42L;
        int cx = -2000, cz = 300;
        for (int a = 0; a < 360; a += 10) for (int r = KunlunShape.TOP + 1; r < KunlunShape.REACH; r += 15) {
            int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * r), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
            double y = at(seed, cx, cz, dx, dz, older);
            assertTrue(y <= older + 1 && y < 320, "under the older sky at " + dx + " " + dz + ": " + y);
        }
        KunlunShape.Path path = KunlunShape.path(seed, cx, cz, older, LAND);
        assertEquals(older, path.y[path.size() - 1], 1e-9, "the stair to the lower summit");
        assertTrue(path.size() > 500, "still a long way up: " + path.size());
        for (int i = 1; i < path.size(); i++) assertTrue(Math.abs(path.y[i] - path.y[i - 1]) <= KunlunShape.MAX_CLIMB + 1e-9, "half steps");
    }

    @Test
    void theStairClimbsFromTheFootToTheSummitInHalfSteps() {
        for (long seed : SEEDS) {
            int summit = KunlunShape.SUMMIT_Y;
            int cx = 3000 + (int) Math.floorMod(seed, 1000), cz = -200;
            KunlunShape.Path path = KunlunShape.path(seed, cx, cz, summit, LAND);
            int n = path.size();
            assertTrue(n > 1500, "a long pilgrim's way: " + n);
            // from the foot, out by the massif's edge, to the summit's south edge, the palace's gate before it
            int fx = cx + (int) Math.floor(path.x[0]), fz = cz + (int) Math.floor(path.z[0]);
            assertTrue(KunlunShape.field(seed, fx, fz, fx - cx, fz - cz) < 0.04, "it sets out at the foot: " + KunlunShape.field(seed, fx, fz, fx - cx, fz - cz));
            assertTrue(Math.hypot(path.x[0], path.z[0]) > KunlunShape.REACH * 0.6, "far out: " + Math.hypot(path.x[0], path.z[0]));
            assertEquals(0, path.x[n - 1], 1e-9);
            assertEquals(KunlunShape.TOP + 1, path.z[n - 1], 1e-9, "it ends at the summit's south edge");
            assertEquals(summit, path.y[n - 1], 1e-9, "at the summit's height");
            assertTrue(KunlunPalace.onApproach(0, KunlunShape.TOP + 1), "where the way in to the terrace begins");
            assertEquals(path.ground[0], path.y[0], 3.0, "it starts on the ground at its foot");
            assertTrue(path.ground[0] < LAND.applyAsInt(fx, fz) + 25, "the land's own, nearly: " + path.ground[0]);
            for (int i = 1; i < n; i++) {
                assertTrue(Math.hypot(path.x[i] - path.x[i - 1], path.z[i] - path.z[i - 1]) <= 1.01, "a block apart");
                assertTrue(Math.abs(path.y[i] - path.y[i - 1]) <= KunlunShape.MAX_CLIMB + 1e-9, "never steeper than its climb at " + i);
                assertTrue(path.y[i] <= summit);
            }
            // its turns never come close to each other: a turn lower down is well apart, or well under it (a cliff between, each way its own)
            for (int i = 0; i < n; i += 3) for (int j = i + 80; j < n; j += 3) {
                double d = Math.hypot(path.x[i] - path.x[j], path.z[i] - path.z[j]);
                assertTrue(d > 2 * KunlunShape.SHOULDER + 4 || d > 2 * KunlunShape.SHOULDER + 1 && Math.abs(path.y[i] - path.y[j]) > 12,
                        "seed " + seed + ": turns apart: " + i + " and " + j + " " + d + " at y " + path.y[i] + " " + path.y[j] + ", " + Math.hypot(path.x[i], path.z[i]) + " out, of " + n);
            }
            // walked block by block: from any block of its way to the next, up or down half a block at most
            java.util.Map<Long, Integer> half = new java.util.HashMap<>();
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                minX = Math.min(minX, (int) Math.floor(path.x[i]) - 3);
                maxX = Math.max(maxX, (int) Math.floor(path.x[i]) + 3);
                minZ = Math.min(minZ, (int) Math.floor(path.z[i]) - 3);
                maxZ = Math.max(maxZ, (int) Math.floor(path.z[i]) + 3);
            }
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                if (KunlunPalace.covers(x, z)) continue;
                var hit = path.nearest(x, z, KunlunShape.PATH_HALF);
                if (hit != null) half.put(((long) x << 32) ^ (z & 0xFFFFFFFFL), (int) Math.floor(2 * hit.y()));
            }
            assertTrue(half.size() > n * 2, "three or four wide: " + half.size());
            for (var w : half.entrySet()) {
                int x = (int) (w.getKey() >> 32), z = (int) (long) w.getKey().intValue();
                for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                    Integer o = half.get(((long) (x + d[0]) << 32) ^ ((z + d[1]) & 0xFFFFFFFFL));
                    if (o != null) assertTrue(Math.abs(o - w.getValue()) <= 1, "a half step at most at " + x + " " + z + ": " + w.getValue() + " to " + o);
                }
            }
            // its gate at the foot, then pavilions every so far, shrines between, none crowding another
            List<KunlunShape.Site> sites = path.sites;
            assertEquals(KunlunShape.SiteKind.GATE, sites.get(0).kind(), "the gate first");
            assertTrue(sites.get(0).index() < 40, "the gate at the foot: " + sites.get(0).index());
            List<Integer> pavilions = sites.stream().filter(s -> s.kind() == KunlunShape.SiteKind.PAVILION).map(KunlunShape.Site::index).sorted().toList();
            assertTrue(pavilions.size() >= n / KunlunShape.PAVILION_EVERY - 2, "pavilions all the way up: " + pavilions);
            for (int k = 1; k < pavilions.size(); k++) {
                int gap = pavilions.get(k) - pavilions.get(k - 1);
                assertTrue(gap > 80 && gap < 2 * KunlunShape.PAVILION_EVERY + 40, "a pavilion every so far: " + pavilions);
            }
            assertTrue(sites.stream().anyMatch(s -> s.kind() == KunlunShape.SiteKind.SHRINE), "shrines");
            for (KunlunShape.Site s : sites) {
                assertEquals((int) Math.floor(path.y[s.index()]), s.y(), "seed " + seed + ": a site's floor the way's height");
                var hit = path.nearest(s.x(), s.z(), KunlunShape.PATH_HALF + 0.8);
                assertNotNull(hit, "seed " + seed + ": a site on the way: " + s);
                assertEquals(1, Math.abs(s.ux()) + Math.abs(s.uz()), "along an axis");
                assertEquals(0, s.along(s.x(), s.z()));
                assertEquals(0, s.across(s.x(), s.z()));
                assertEquals(1, s.along(s.x() + s.ux(), s.z() + s.uz()), "one along, up the way");
                for (KunlunShape.Site o : sites) {
                    if (o != s) assertTrue(Math.abs(o.x() - s.x()) > 19 || Math.abs(o.z() - s.z()) > 19, "sites apart: " + s + " " + o);
                }
                assertSame(s, path.siteAt(s.x(), s.z()), "found where it stands");
            }
            // the same seed, the same stair
            KunlunShape.Path again = KunlunShape.path(seed, cx, cz, summit, LAND);
            assertSame(path, again, "worked out once a region");
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
