package com.jttw.engine.world;

import com.jttw.engine.world.waters.Sea;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where the mod's zones lie: one region a world each (four for the Mystical Waters, one in each sea),
 * in its ring round spawn, farther out the higher its levels; on its hosts; the same for the same
 * seed; never two at the same heights on the same ground.
 */
class ZonesTest {

    private static final long[] SEEDS = {1L, 42L, 12345L, 987654321L, -7L};

    /** Host ground in patches 600 blocks square, a different third of them for each zone: a stand-in for the biomes. */
    private static final ZoneMap.Hosts PATCHES = (zone, x, z) ->
            ZoneLayout.unit(ZoneLayout.mix(zone.layout().salt(), Math.floorDiv(x, 600), Math.floorDiv(z, 600))) < 0.33;

    private static int[][] centres(ZoneMap.Placement p, Zones.Zone zone) {
        return p.of(zone);
    }

    @Test
    void oneRegionOfEachZoneAndOneInEachSea() {
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : List.of(Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES))) {
                for (Zones.Zone zone : Zones.MAP.zones()) {
                    int[][] cs = centres(p, zone);
                    assertEquals(zone == Zones.MYSTICAL_WATERS ? 4 : 1, cs.length, zone.id());
                    for (int[] c : cs) assertNotNull(c, zone.id());
                }
                int[][] seas = centres(p, Zones.MYSTICAL_WATERS);
                for (Sea sea : Sea.values()) {
                    assertEquals(sea, Sea.of(seas[sea.ordinal()][0], seas[sea.ordinal()][1]), "seed " + seed + ": the " + sea + " sea's region lies " + sea);
                    double off = Math.toDegrees(Math.atan2(seas[sea.ordinal()][1], seas[sea.ordinal()][0])) - sea.ordinal() * 90;
                    assertTrue(Math.abs(((off % 360) + 540) % 360 - 180) <= 30.5, "within 30 degrees of its compass point: " + off);
                }
            }
            assertEquals(0, Zones.HELL.centres(seed).length, "Hell has no ground here");
        }
    }

    @Test
    void eachLiesInItsRingFartherOutTheHigherItsLevels() {
        int prevFar = 0;
        for (Zones.Zone zone : Zones.MAP.zones()) {
            ZoneLayout l = zone.layout();
            assertTrue(l.near() >= prevFar, zone.id() + ": its ring beyond the last zone's");
            assertTrue(l.near() > l.margin(), zone.id() + ": clear of spawn");
            prevFar = l.far();
        }
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : List.of(Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES))) {
                for (Zones.Zone zone : Zones.MAP.zones()) {
                    for (int[] c : centres(p, zone)) {
                        double d = Math.hypot(c[0], c[1]);
                        assertTrue(d >= zone.layout().near() - 1 && d <= zone.layout().far() + 1,
                                "seed " + seed + ": " + zone.id() + " " + Math.round(d) + " out, not " + zone.layout().near() + "-" + zone.layout().far());
                    }
                }
                assertEquals("", p.notes(), "nothing widened");
            }
        }
    }

    @Test
    void eachStandsOnItsHosts() {
        for (long seed : SEEDS) {
            ZoneMap.Placement p = Zones.MAP.place(seed, PATCHES);
            for (Zones.Zone zone : Zones.MAP.zones()) {
                for (int[] c : centres(p, zone)) {
                    assertTrue(PATCHES.at(zone, c[0], c[1]), zone.id() + ": its centre on host ground");
                    int ring = 0;
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4, r = zone.layout().radius() * ZoneMap.RING_SHARE;
                        if (PATCHES.at(zone, c[0] + (int) Math.round(Math.cos(a) * r), c[1] + (int) Math.round(Math.sin(a) * r))) ring++;
                    }
                    assertTrue(ring >= ZoneMap.RING_HOSTS, zone.id() + ": host ground round it too, " + ring + " of 8");
                }
            }
        }
    }

    @Test
    void theSameSeedLaysTheSameZonesAndAnotherDoesNot() {
        ZoneMap.Placement a = Zones.MAP.place(42L, PATCHES), b = Zones.MAP.place(42L, PATCHES), c = Zones.MAP.place(43L, PATCHES);
        int differ = 0;
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int i = 0; i < a.of(zone).length; i++) {
                assertArrayEquals(a.of(zone)[i], b.of(zone)[i], zone.id());
                if (a.of(zone)[i][0] != c.of(zone)[i][0] || a.of(zone)[i][1] != c.of(zone)[i][1]) differ++;
            }
        }
        assertTrue(differ >= 8, "another seed, other places: " + differ);
        // and in any direction round spawn: no lopsided half of the world
        int east = 0, west = 0;
        for (long seed = 1; seed <= 200; seed++) {
            if (Zones.WHITE_BONE.centres(seed)[0][0] >= 0) east++;
            else west++;
        }
        assertEquals(1.0, west / (double) east, 0.4, "east " + east + ", west " + west);
    }

    @Test
    void zonesAtTheSameHeightsNeverShareGround() {
        for (long seed : SEEDS) {
            for (ZoneMap.Placement p : List.of(Zones.MAP.placement(seed), Zones.MAP.place(seed, PATCHES))) {
                List<Zones.Zone> zones = Zones.MAP.zones();
                for (int i = 0; i < zones.size(); i++) for (int j = i; j < zones.size(); j++) {
                    Zones.Zone a = zones.get(i), b = zones.get(j);
                    if (!a.vertical().overlaps(b.vertical())) continue;
                    for (int[] ca : p.of(a)) for (int[] cb : p.of(b)) {
                        if (ca == cb) continue;
                        assertTrue(Math.hypot(ca[0] - cb[0], ca[1] - cb[1]) >= a.layout().margin() + b.layout().margin() + ZoneMap.GAP,
                                "seed " + seed + ": " + a.id() + " and " + b.id() + " too near");
                    }
                }
            }
            // and so no block column is two zones' at a height they share
            for (Zones.Zone zone : Zones.MAP.zones()) {
                for (int[] c : zone.centres(seed)) {
                    int m = zone.layout().margin();
                    for (int x = c[0] - m; x <= c[0] + m; x += 7) for (int z = c[1] - m; z <= c[1] + m; z += 7) {
                        if (!zone.covers(seed, x, z)) continue;
                        for (Zones.Zone other : Zones.MAP.zones()) {
                            if (other != zone && other.vertical().overlaps(zone.vertical())) assertFalse(other.covers(seed, x, z), zone.id() + " and " + other.id());
                        }
                    }
                }
            }
        }
    }

    @Test
    void whereNoRingHostsTheSearchWidensOutward() {
        // host ground only past 8 km: every zone's region is found out there, those of nearer rings by widening, and that noted
        ZoneMap.Hosts far = (zone, x, z) -> Math.hypot(x, z) > 8000;
        ZoneMap.Placement p = Zones.MAP.place(5L, far);
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int[] c : p.of(zone)) assertTrue(Math.hypot(c[0], c[1]) > 8000, zone.id() + " widened out");
        }
        assertTrue(p.notes().contains(Zones.WHITE_BONE.id() + ": no host ground 1000-2000 blocks out"), p.notes());
        assertFalse(p.notes().contains(Zones.LION_CAMEL.id()), "the last ring reaches past 8 km already");
        // none anywhere: each is put down in its own ring all the same, and that noted
        ZoneMap.Placement none = Zones.MAP.place(5L, (zone, x, z) -> false);
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int[] c : none.of(zone)) {
                double d = Math.hypot(c[0], c[1]);
                assertTrue(d >= zone.layout().near() - 1 && d <= zone.layout().far() + 1, zone.id());
            }
        }
        assertTrue(none.notes().contains("off its hosts"), none.notes());
    }

    @Test
    void aRegionIsARoughCircleRoundItsCentre() {
        long seed = 77L;
        for (Zones.Zone zone : Zones.MAP.zones()) {
            ZoneLayout l = zone.layout();
            for (int[] c : zone.centres(seed)) {
                assertEquals(1.0, zone.depth(seed, c[0], c[1]), 1e-9, "1 at its centre");
                assertArrayEquals(c, zone.centre(seed, c[0] + 5, c[1] - 5), "its centre, near it");
                assertEquals(Math.hypot(30, 40), zone.distanceToCentre(seed, c[0] + 30, c[1] + 40), 1e-9);
                for (int k = 0; k < 36; k++) {
                    double a = k * Math.PI / 18;
                    int ix = (int) Math.round(Math.cos(a) * (l.radius() - l.wobble() - 2)), iz = (int) Math.round(Math.sin(a) * (l.radius() - l.wobble() - 2));
                    assertTrue(zone.covers(seed, c[0] + ix, c[1] + iz), zone.id() + ": inside its least reach");
                    int ox = (int) Math.round(Math.cos(a) * (l.margin() + 1)), oz = (int) Math.round(Math.sin(a) * (l.margin() + 1));
                    assertFalse(zone.covers(seed, c[0] + ox, c[1] + oz), zone.id() + ": outside its greatest");
                }
            }
            // far from its regions: nothing, yet its centre is still found
            assertFalse(zone.covers(seed, 0, 0), zone.id() + " at spawn");
            assertNotNull(zone.centre(seed, 100_000, -100_000));
        }
        // the seas: the centre nearest
        int[][] seas = Zones.MYSTICAL_WATERS.centres(seed);
        for (int[] c : seas) assertArrayEquals(c, Zones.MYSTICAL_WATERS.centre(seed, c[0] * 3, c[1] * 3), "the nearest sea");
    }

    @Test
    void theWorldsPlacementIsKeptOnceFound() {
        ZoneMap map = new ZoneMap(Zones.ALL);
        int[] calls = new int[1];
        ZoneMap.Hosts counted = (zone, x, z) -> {
            calls[0]++;
            return PATCHES.at(zone, x, z);
        };
        assertFalse(map.settled(9L));
        ZoneMap.Placement p = map.settle(9L, counted, false);
        int once = calls[0];
        assertTrue(map.settled(9L));
        assertSame(p, map.settle(9L, counted, false), "found once");
        assertEquals(once, calls[0], "not searched again");
        assertSame(p, map.placement(9L));
        assertNotSame(p, map.placement(10L), "another seed's is its own");
        assertFalse(map.settled(10L));
    }

    @Test
    void placingIsQuick() {
        long start = System.nanoTime();
        for (long seed = 100; seed < 120; seed++) Zones.MAP.place(seed, PATCHES);
        double ms = (System.nanoTime() - start) / 1e6 / 20;
        assertTrue(ms < 200, "a world's placement in " + ms + " ms (before the biome source's own cost)");
    }
}
