package com.jttw.engine.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduShape;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where Fengdu, the Ghost City, lies: one a world, in the farthest ring (9-10.5 km, past Lion-Camel
 * Ridge), on river and hill country, never on any zone's ground, the same for the same seed; a
 * place of no band; and its hill, which holds the city, its archway and its road inside its region.
 */
class FengduPlacementTest {

    private static final Zones.Zone F = Zones.FENGDU;
    private static final long[] SEEDS = {1L, 42L, 12345L, 987654321L, -7L};

    /** Host ground in patches 600 blocks square, a third of them for each zone: a stand-in for the biomes (as ZonesTest's). */
    private static final ZoneMap.Hosts PATCHES = (zone, x, z) ->
            ZoneLayout.unit(ZoneLayout.mix(zone.layout().salt(), Math.floorDiv(x, 600), Math.floorDiv(z, 600))) < 0.33;

    @Test
    void itsHostsAreRiverAndHillCountry() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/jttw_engine/tags/worldgen/biome/fengdu_hosts.json")) {
            assertNotNull(in, "its hosts tag");
            JsonObject tag = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> hosts = new HashSet<>();
            tag.getAsJsonArray("values").forEach(v -> hosts.add(v.getAsString()));
            assertTrue(hosts.contains("minecraft:river") && hosts.contains("minecraft:windswept_hills") && hosts.contains("minecraft:forest")
                    && hosts.contains("minecraft:plains"), "rivers, hills, forest and plain: " + hosts);
            assertFalse(hosts.stream().anyMatch(h -> h.contains("ocean") || h.contains("desert") || h.contains("peaks")), "no sea, desert or peak");
        }
        assertEquals("jttw_engine:fengdu_hosts", F.hosts().location().toString());
        assertEquals(256, F.hostY(), "judged over any hill's top");
    }

    @Test
    void itIsAPlaceOfNoBandLastOfAll() {
        assertFalse(F.hasLevelBand(), "no band of its own: Hell's is the band it leads to");
        assertTrue(Zones.HELL.hasLevelBand() && Zones.LION_CAMEL.hasLevelBand());
        assertEquals(Zones.ALL.size() - 1, Zones.ALL.indexOf(F), "last: it keeps clear of every zone, none of them of it");
        assertTrue(Zones.MAP.zones().contains(F));
        assertSame(F, Zones.byBiome(Zones.FENGDU_BIOME));
        assertEquals(Zones.Vertical.WHOLE_COLUMN, F.vertical(), "the whole column: no zone shares its ground at any height");
        for (Zones.Zone zone : Zones.ALL) {
            if (zone != F) assertTrue(zone.hasLevelBand(), zone.id() + " has its band");
        }
    }

    @Test
    void itLiesOnceAWorldInTheFarthestRingPastLionCamelRidge() {
        assertEquals(9000, F.layout().near());
        assertEquals(10500, F.layout().far());
        assertTrue(F.layout().near() >= Zones.LION_CAMEL.layout().far(), "its ring past Lion-Camel Ridge's");
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : new ZoneMap.Placement[]{Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES)}) {
                int[][] cs = p.of(F);
                assertEquals(1, cs.length, "one Fengdu a world");
                double d = Math.hypot(cs[0][0], cs[0][1]);
                assertTrue(d >= 9000 - 1 && d <= 10500 + 1, "seed " + seed + ": 9-10.5 km out: " + d);
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other == F) continue;
                    for (int[] o : p.of(other)) assertTrue(Math.hypot(o[0], o[1]) < d, "seed " + seed + ": " + other.id() + " nearer spawn");
                }
                assertFalse(p.notes().contains(F.id()), "found in its own ring: " + p.notes());
            }
        }
    }

    @Test
    void itStandsOnItsHosts() {
        for (long seed : SEEDS) {
            int[] c = Zones.MAP.place(seed, PATCHES).of(F)[0];
            assertTrue(PATCHES.at(F, c[0], c[1]), "seed " + seed + ": its centre on host ground");
            int ring = 0;
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4, r = F.layout().radius() * ZoneMap.RING_SHARE;
                if (PATCHES.at(F, c[0] + (int) Math.round(Math.cos(a) * r), c[1] + (int) Math.round(Math.sin(a) * r))) ring++;
            }
            assertTrue(ring >= ZoneMap.RING_HOSTS, "host ground round it too: " + ring);
        }
    }

    @Test
    void itNeverSharesGroundWithAnyZone() {
        int ground = 0;
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : new ZoneMap.Placement[]{Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES)}) {
                int[] c = p.of(F)[0];
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other == F) continue;
                    for (int[] o : p.of(other)) {
                        assertTrue(Math.hypot(o[0] - c[0], o[1] - c[1]) >= F.layout().margin() + other.layout().margin() + ZoneMap.GAP,
                                "seed " + seed + ": " + other.id() + " too near");
                    }
                }
            }
            int[] c = F.centres(seed)[0];
            int m = F.layout().margin();
            for (int x = c[0] - m; x <= c[0] + m; x += 4) for (int z = c[1] - m; z <= c[1] + m; z += 4) {
                if (!F.covers(seed, x, z)) continue;
                ground++;
                for (Zones.Zone other : Zones.MAP.zones()) {
                    if (other != F) assertFalse(other.covers(seed, x, z), "seed " + seed + " at " + x + " " + z + " is " + other.id() + "'s too");
                }
            }
        }
        assertTrue(ground > 5000, "it has ground: " + ground);
    }

    @Test
    void theSameSeedPutsItInTheSamePlaceAndAnotherElsewhere() {
        for (long seed : SEEDS) {
            assertArrayEquals(Zones.MAP.place(seed, PATCHES).of(F)[0], Zones.MAP.place(seed, PATCHES).of(F)[0], "seed " + seed);
            assertArrayEquals(Zones.MAP.placement(seed).of(F)[0], F.centres(seed)[0]);
        }
        int differ = 0;
        for (long seed = 1; seed < 20; seed++) {
            int[] a = F.centres(seed)[0], b = F.centres(seed + 1)[0];
            if (a[0] != b[0] || a[1] != b[1]) differ++;
        }
        assertTrue(differ >= 18, "another seed, another place: " + differ);
    }

    @Test
    void theHillAndTheCityFitInsideItsRegion() {
        assertTrue(FengduShape.REACH <= F.layout().radius() - F.layout().wobble(), "the hill's foot within the region's least reach");
        assertTrue(FengduShape.reach(FengduCity.OUTER, FengduCity.OUTER) < FengduShape.TOP, "the towers on the hill's level top");
        assertTrue(FengduShape.reach(FengduCity.ARCH_HALF, FengduCity.ARCH_Z + 1) < FengduShape.TOP, "the archway too");
        for (long seed : SEEDS) {
            int[] c = F.centres(seed)[0];
            for (int a = 0; a < 360; a += 5) {
                double r = FengduShape.REACH;
                int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * r), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
                assertTrue(F.depth(seed, c[0] + dx, c[1] + dz) > 0, "seed " + seed + ": the hill's foot within its region at " + a + " degrees");
            }
        }
    }

    @Test
    void theHillsShape() {
        long seed = 42L;
        // level on its top; the land's own past its foot; a slope between
        for (int dx = -FengduShape.TOP; dx <= FengduShape.TOP; dx += 3) {
            for (int dz = -FengduShape.TOP; dz <= FengduShape.TOP; dz += 3) {
                if (FengduShape.onTop(dx, dz)) assertEquals(0, FengduShape.share(seed, 1000 + dx, 2000 + dz, dx, dz), "level at " + dx + " " + dz);
            }
        }
        assertEquals(1, FengduShape.share(seed, 0, FengduShape.REACH, 0, FengduShape.REACH), 1e-9, "the land's own at its foot");
        double mid = FengduShape.share(seed, 0, FengduShape.TOP + FengduShape.SLOPE / 2, 0, FengduShape.TOP + FengduShape.SLOPE / 2);
        assertTrue(mid > 0.15 && mid < 0.85, "a slope between: " + mid);
        assertEquals(100, FengduShape.groundAt(60, 100, 0));
        assertEquals(60, FengduShape.groundAt(60, 100, 1));
        // the floor: the land at the centre and a rise, worked out once a region, kept over the sea and under the sky
        assertEquals(64 + FengduShape.RISE, FengduShape.cityFloor(7L, 10, 10, (x, z) -> 64, 70, 224));
        assertEquals(70, FengduShape.cityFloor(7L, 20, 20, (x, z) -> 20, 70, 224));
        assertEquals(224, FengduShape.cityFloor(7L, 30, 30, (x, z) -> 300, 70, 224));
        assertEquals(64 + FengduShape.RISE, FengduShape.cityFloor(7L, 10, 10, (x, z) -> 150, 70, 224), "cached");
        assertEquals(100 + FengduShape.RISE, FengduShape.cityFloor(7L, 40, 40, (x, z) -> x == 40 && z == 40 ? 60 : 100, 70, 224),
                "in a bowl: over the country round its foot, not in a hollow of it");
        // the road: from the gate down the south slope, never inside the walls
        assertTrue(FengduShape.onRoad(0, FengduCity.OUTER + 1) && FengduShape.onRoad(2, FengduShape.TOP + 10));
        assertFalse(FengduShape.onRoad(0, FengduCity.WALL_IN - 2) || FengduShape.onRoad(5, FengduShape.TOP + 10) || FengduShape.onRoad(0, -FengduShape.TOP));
    }

    @Test
    void theCitysPlacesAreWhereTheyShouldBe() {
        assertTrue(FengduCity.onTerrace(0, FengduCity.GHOST_Z) && FengduCity.onTerrace(0, FengduCity.THRONE_Z), "the gate and the palace up on the terrace");
        assertFalse(FengduCity.onTerrace(0, 10), "the lower city below it");
        assertTrue(FengduCity.inVeil(0, FengduCity.TERRACE_H + 1, FengduCity.GHOST_Z) && !FengduCity.inVeil(0, FengduCity.TERRACE_H + 1, FengduCity.RETURN_Z),
                "one comes back up out of the veil, before it");
        assertTrue(FengduCity.RETURN_Z > FengduCity.GHOST_Z + 1 && FengduCity.RETURN_Z <= FengduCity.TERRACE_S, "on the terrace, before the gate");
        assertTrue(FengduCity.onSteps(0, 0) && FengduCity.onSteps(FengduCity.SIDE_STEPS_FROM, 1) && !FengduCity.onSteps(10, 0));
        for (int a = -FengduCity.WALL_IN; a <= FengduCity.WALL_IN; a++) {
            if (Math.abs(a) <= FengduCity.AVENUE + 2 || Math.abs(a) >= FengduCity.RING) assertEquals(0, FengduCity.plotIndexX(a), "no plot at " + a);
        }
        assertEquals(0, FengduCity.plotIndexZ(FengduCity.TERRACE_S + 3), "a street at the terrace's foot");
        assertTrue(FengduCity.covers(0, FengduCity.ARCH_Z) && !FengduCity.covers(FengduCity.ARCH_HALF + 2, FengduCity.ARCH_Z));
    }
}
