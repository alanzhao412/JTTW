package com.jttw.engine.world;

import com.jttw.engine.world.hell.HellRules;
import com.jttw.engine.world.hell.HellTerrain;
import com.jttw.engine.world.hell.TenCourts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Hell: its band, its land (plaza, road, the Naihe, the fortress's ground), the plan of the Ten Courts, and the arrival's safety. */
class HellZoneTest {

    private static final int G = HellTerrain.GROUND;

    @Test
    void itIsADimensionsZoneOfLevels57To66() {
        var band = Zones.HELL.levelBand();
        assertEquals(Zones.HELL_BIOME.location(), band.biome());
        assertEquals(57, band.minLevel());
        assertEquals(66, band.maxLevel());
        assertEquals(125, band.baseGruntXp(), "stepped up from the Mystical Waters' 110");
        assertFalse(Zones.HELL.hasFootprint(), "no overworld ground");
        assertEquals(Zones.ALL.size() - 1, Zones.ALL.indexOf(Zones.HELL), "last, after the Mystical Waters");
        assertFalse(Zones.MAP.zones().contains(Zones.HELL));
        assertSame(Zones.HELL, Zones.byBiome(Zones.HELL_BIOME));
    }

    @Test
    void thePlazaTheRoadAndTheFortressAreLevel() {
        for (int x = -30; x <= 30; x += 3) for (int z = -30; z <= 30; z += 3) {
            if (Math.hypot(x, z) <= HellTerrain.PLAZA_FLAT) assertEquals(G, HellTerrain.surface(x, z), "the plaza at " + x + " " + z);
        }
        assertEquals(G, HellTerrain.surface(HellTerrain.ARRIVAL_X, HellTerrain.ARRIVAL_Z), "the arrival");
        int[] bridge = HellTerrain.bridgeEnds();
        for (int z = 0; z >= TenCourts.GATE_Z; z -= 2) {
            if (z >= bridge[0] - 1 && z <= bridge[1] + 1) continue;
            for (int x = -HellTerrain.ROAD_HALF; x <= HellTerrain.ROAD_HALF; x++) assertEquals(G, HellTerrain.surface(x, z), "the road at " + x + " " + z);
        }
        for (int x = -TenCourts.HALF_WIDTH - 2; x <= TenCourts.HALF_WIDTH + 2; x += 2)
            for (int z = TenCourts.NORTH_Z - 2; z <= TenCourts.GATE_Z + 2; z += 2) assertEquals(G, HellTerrain.surface(x, z), "the fortress at " + x + " " + z);
    }

    @Test
    void theNaiheCrossesAllHellAndTheBridgeSpansIt() {
        for (int x = -3000; x <= 3000; x += 37) {
            int z = (int) Math.round(HellTerrain.naiheCentre(x));
            assertTrue(HellTerrain.underWater(x, z), "the Naihe flows at " + x + " " + z);
            assertTrue(HellTerrain.surface(x, z) <= HellTerrain.SEA_LEVEL - 4, "deep enough at " + x);
        }
        assertEquals(HellTerrain.NAIHE_Z, (int) Math.round(HellTerrain.naiheCentre(0)), "where the road crosses");
        int[] b = HellTerrain.bridgeEnds();
        assertTrue(HellTerrain.surface(0, b[0]) >= G && HellTerrain.surface(0, b[1]) >= G, "the bridge ends on the road's level");
        for (int z = b[0] + 1; z < b[1]; z++) assertTrue(HellTerrain.bridgeRise(z) >= 0);
        assertEquals(6, HellTerrain.bridgeRise((b[0] + b[1]) / 2), "an arch, three blocks up at its middle");
        // never the arrival's nor the fortress's
        for (int x = -200; x <= 200; x += 10) {
            assertTrue(HellTerrain.naiheCentre(x) < -HellTerrain.PLAZA_BLEND, "clear of the plaza");
            assertTrue(HellTerrain.naiheCentre(x) - HellTerrain.NAIHE_HALF - 20 > TenCourts.GATE_Z, "clear of the fortress");
        }
    }

    @Test
    void theLandIsNotFlat() {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, rivers = 0;
        for (int x = -4000; x <= 4000; x += 23) for (int z = -4000; z <= 4000; z += 29) {
            int h = HellTerrain.surface(x, z);
            min = Math.min(min, h);
            max = Math.max(max, h);
            if (HellTerrain.underWater(x, z) && Math.abs(z - HellTerrain.naiheCentre(x)) > 40) rivers++;
        }
        assertTrue(max >= G + 15, "black stone hills: " + max);
        assertTrue(min < HellTerrain.SEA_LEVEL - 2, "river beds: " + min);
        assertTrue(rivers > 50, "side rivers too: " + rivers);
        assertEquals(HellTerrain.height(123, -456), HellTerrain.height(123, -456), "the same every time");
    }

    @Test
    void theTenCourtsAreInOrderAndApart() {
        assertEquals(10, TenCourts.COURTS.size());
        assertEquals(11, TenCourts.HALLS.size());
        for (int n = 1; n <= 10; n++) assertEquals(n, TenCourts.court(n).number());
        // up the west side, Yama at the head, back down the east
        for (int n = 1; n <= 4; n++) {
            assertTrue(TenCourts.court(n).x() < 0, "west: " + n);
            if (n > 1) assertTrue(TenCourts.court(n).z() < TenCourts.court(n - 1).z(), "further north: " + n);
        }
        TenCourts.Court yama = TenCourts.court(5);
        assertEquals(0, yama.x());
        assertTrue(yama.isYama());
        for (TenCourts.Court c : TenCourts.COURTS) if (c != yama) assertTrue(yama.z() < c.z(), "Yama's hall at the head of the avenue");
        for (int n = 6; n <= 10; n++) {
            assertTrue(TenCourts.court(n).x() > 0, "east: " + n);
            if (n > 6) assertTrue(TenCourts.court(n).z() > TenCourts.court(n - 1).z(), "further south: " + n);
        }
        List<TenCourts.Court> halls = TenCourts.HALLS;
        for (TenCourts.Court a : halls) {
            int pad = 3;
            assertTrue(Math.abs(a.x()) + a.halfX() + pad < TenCourts.HALF_WIDTH - TenCourts.WALL_THICK, "within the walls: " + a.number());
            assertTrue(a.z() + a.halfZ() + pad < TenCourts.GATE_Z - TenCourts.WALL_THICK && a.z() - a.halfZ() - pad > TenCourts.NORTH_Z + TenCourts.WALL_THICK,
                    "within the walls: " + a.number());
            int[] t = a.throne(), d = a.door();
            assertTrue(a.contains(t[0], t[1]), "its throne inside: " + a.number());
            assertTrue(a.doorZ() != 0 || a.doorX() == -Integer.signum(a.x()), "its door faces the avenue: " + a.number());
            assertTrue(Math.abs(d[0]) < Math.abs(a.x()) || a.isYama(), "its door on the avenue's side");
            for (TenCourts.Court b : halls) {
                if (a == b) continue;
                boolean apart = Math.abs(a.x() - b.x()) > a.halfX() + b.halfX() + 4 || Math.abs(a.z() - b.z()) > a.halfZ() + b.halfZ() + 4;
                assertTrue(apart, "halls " + a.number() + " and " + b.number() + " apart");
            }
            assertTrue(Math.abs(a.x()) - a.halfX() > TenCourts.AVENUE_HALF + 4 || a.isYama(), "off the avenue: " + a.number());
        }
        int[] w = TenCourts.wheel();
        assertTrue(TenCourts.inside(w[0], w[1]));
        assertNull(TenCourts.hallAt(w[0], w[1]), "the Wheel stands in the yard");
        assertTrue(w[0] - TenCourts.WHEEL_RADIUS > TenCourts.AVENUE_HALF + 1, "not on the avenue");
        TenCourts.Court tenth = TenCourts.court(10);
        assertTrue(w[0] + TenCourts.WHEEL_RADIUS < tenth.x() - tenth.halfX() - 2, "before the Tenth Court's door, not in it");
        assertTrue(Math.abs(w[1] - tenth.z()) > 2 + 1, "beside the way in, not across it");
    }

    @Test
    void theArrivalIsSafeAndFarFromTheCourts() {
        for (TenCourts.Court c : TenCourts.HALLS) {
            int[] t = c.throne();
            assertTrue(HellRules.fromArrival(t[0], t[1]) >= HellRules.FORTRESS_AT_LEAST, "court " + c.number() + " is far");
            assertTrue(HellRules.fromArrival(c.door()[0], c.door()[1]) >= HellRules.FORTRESS_AT_LEAST);
        }
        assertTrue(HellRules.fromArrival(0, TenCourts.GATE_Z) >= HellRules.FORTRESS_AT_LEAST, "the gate is far");
        assertTrue(HellRules.SANCTUARY < HellRules.NO_SPAWNS_WITHIN);
        // nothing spawns near the arrival; officials only within the walls; ghosts out on the plains
        for (int x = -150; x <= 150; x += 10) for (int z = -150; z <= 150; z += 10) {
            if (Math.hypot(x, z) >= HellRules.NO_SPAWNS_WITHIN) continue;
            assertFalse(HellRules.maySpawn(false, x, z), "no ghost near the arrival at " + x + " " + z);
            assertFalse(HellRules.maySpawn(true, x, z), "no official near the arrival");
        }
        assertTrue(HellRules.maySpawn(false, 300, 300), "ghosts out on the plains");
        assertFalse(HellRules.maySpawn(true, 300, 300), "but no official");
        assertTrue(HellRules.maySpawn(true, 0, -450), "officials within the fortress");
        assertFalse(HellRules.maySpawn(true, 0, TenCourts.GATE_Z + 20), "not before its gate");
        assertFalse(HellRules.maySpawn(true, TenCourts.HALF_WIDTH, -450), "not on its walls");
    }
}
