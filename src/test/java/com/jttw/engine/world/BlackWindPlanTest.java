package com.jttw.engine.world;

import com.jttw.engine.world.blackwind.BlackWindPlan;
import com.jttw.engine.world.blackwind.BlackWindShape;
import com.jttw.engine.world.blackwind.TemplePlan;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Black Wind Cave's plan and the burnt temple's, and Black Wind Mountain's shape: pure, so tested
 * here. The door, the grate and the throne cavern's arena where the plan says; a way on foot from the
 * door to the Black Bear's post, which the grate shuts; the garrisons on open floor; the temple's ruin;
 * the peak over the cave, the valley's floor before the door and across the terrace.
 */
class BlackWindPlanTest {

    private static final BlackWindPlan.Built PLAN = BlackWindPlan.get();

    @Test
    void theDoorAndTheGrateAreWhereThePlanSays() {
        int[] d = BlackWindPlan.DOOR;
        for (int x = d[0]; x <= d[3]; x++) for (int y = d[1]; y <= d[4]; y++) for (int z = d[2]; z <= d[5]; z++) {
            assertTrue(BlackWindPlan.door(PLAN.code(x, y, z)), "door at " + x + " " + y + " " + z);
        }
        assertEquals((d[3] - d[0] + 1), 11, "eleven wide");
        assertEquals((d[4] - d[1] + 1), 15, "fifteen high");
        int[] s = BlackWindPlan.SEAL_BOX;
        for (int x = s[0]; x <= s[3]; x++) for (int y = s[1]; y <= s[4]; y++) assertTrue(BlackWindPlan.seal(PLAN.code(x, y, s[2])), "grate at " + x + " " + y);
        // the cliff over the door is the peak's rock, the forecourt before it the valley's
        assertNotEquals(BlackWindPlan.AIR, PLAN.code(0, 30, BlackWindPlan.FACE_Z - 4));
        assertEquals(BlackWindPlan.UNTOUCHED, PLAN.code(0, 0, BlackWindPlan.FACE_Z + 20));
    }

    @Test
    void theThroneCavernsArenaIsOpenRoundTheKingsPost() {
        int y = BlackWindPlan.THRONE_Y, cz = BlackWindPlan.THRONE_Z;
        int open = 0;
        for (int x = -BlackWindPlan.ARENA; x <= BlackWindPlan.ARENA; x++) for (int z = cz - BlackWindPlan.ARENA; z <= cz + BlackWindPlan.ARENA; z++) {
            if (x * x + (z - cz) * (z - cz) > BlackWindPlan.ARENA * BlackWindPlan.ARENA) continue;
            assertTrue(PLAN.standable(x, y, z), "the arena open at " + x + " " + z + ": " + PLAN.code(x, y, z) + " over " + PLAN.code(x, y - 1, z));
            for (int h = 0; h < 6; h++) assertTrue(BlackWindPlan.open(PLAN.code(x, y + h, z)), "room overhead at " + x + " " + (y + h) + " " + z);
            open++;
        }
        assertTrue(open > 300, "a broad arena: " + open);
        assertEquals(2, BlackWindPlan.floorAt(0, y, cz), "the throne cavern");
        assertEquals(1, BlackWindPlan.floorAt(0, 1, 0), "the court");
        assertEquals(0, BlackWindPlan.floorAt(0, 1, 70), "the forecourt");
    }

    /** Whether one walks from {@code from} to {@code to} through the plan's open cells, a step up or down at a time; {@code throughSeal} treats the grate as open. */
    private static boolean walks(int[] from, int[] to, boolean throughSeal) {
        ArrayDeque<int[]> q = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        q.add(from);
        seen.add(key(from));
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] == to[0] && c[1] == to[1] && c[2] == to[2]) return true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) for (int dy = -1; dy <= 1; dy++) {
                int[] n = {c[0] + d[0], c[1] + dy, c[2] + d[1]};
                if (n[2] > BlackWindPlan.FACE_Z + 2) continue;
                if (!stand(n, throughSeal) || !seen.add(key(n))) continue;
                q.add(n);
            }
        }
        return false;
    }

    private static boolean stand(int[] n, boolean throughSeal) {
        byte at = PLAN.code(n[0], n[1], n[2]), up = PLAN.code(n[0], n[1] + 1, n[2]), under = PLAN.code(n[0], n[1] - 1, n[2]);
        boolean openAt = BlackWindPlan.open(at) || BlackWindPlan.door(at) || (throughSeal && BlackWindPlan.seal(at)) || isStair(at);
        boolean openUp = BlackWindPlan.open(up) || BlackWindPlan.door(up) || (throughSeal && BlackWindPlan.seal(up));
        return openAt && openUp && (BlackWindPlan.holds(under) || under == BlackWindPlan.UNTOUCHED);
    }

    private static boolean isStair(byte c) {
        return c >= BlackWindPlan.STAIR_N && c <= BlackWindPlan.STAIR_W;
    }

    private static long key(int[] c) {
        return ((long) c[0] & 0xFFFF) << 32 | ((long) c[1] & 0xFFFF) << 16 | ((long) c[2] & 0xFFFF);
    }

    @Test
    void aWayOnFootLeadsFromTheDoorToTheKingsPostAndTheGrateShutsIt() {
        int[] door = {0, 1, BlackWindPlan.FACE_Z + 1}, post = {0, BlackWindPlan.THRONE_Y, BlackWindPlan.THRONE_Z};
        assertTrue(walks(door, new int[]{0, 1, BlackWindPlan.WARDEN_POST[2]}, false), "from the door across the court to the warden");
        assertTrue(walks(door, post, true), "the grate open, down the grand stair to the king");
        assertFalse(walks(door, post, false), "the grate shut, no way down");
    }

    @Test
    void theGarrisonsStandOnOpenFloor() {
        for (BlackWindPlan.Floor f : PLAN.floors()) {
            for (var list : java.util.List.of(f.garrison(), f.guards())) for (BlackWindPlan.Spot s : list) {
                if (f.number() == 0) continue; // the forecourt's floor is the valley's
                assertTrue(PLAN.standable(s.x(), s.y(), s.z()), f.name() + ": a post on open floor at " + s);
            }
        }
        assertEquals(2, PLAN.floor(0).guards().size(), "two keepers before the door");
        assertEquals(1, PLAN.floor(1).guards().size(), "a warden at the stair");
        assertTrue(PLAN.floor(1).garrison().size() >= 24, "a garrison in the court: " + PLAN.floor(1).garrison().size());
    }

    @Test
    void theTemplesRuinStands() {
        int walls = 0, charred = 0, statue = 0;
        for (int x = TemplePlan.X_MIN; x <= TemplePlan.X_MAX; x++) for (int z = TemplePlan.Z_MIN; z <= TemplePlan.Z_MAX; z++) {
            for (int y = 1; y <= TemplePlan.Y_MAX; y++) {
                byte c = TemplePlan.code(x, y, z);
                if (c == BlackWindPlan.TERRACOTTA) walls++;
                if (c == TemplePlan.CHARRED) charred++;
                if (c == BlackWindPlan.CALCITE) statue++;
            }
            assertNotEquals(BlackWindPlan.AIR, TemplePlan.code(x, 0, z), "the terrace paved");
        }
        assertTrue(walls > 100 && charred > 60 && statue > 10, "walls " + walls + ", charred posts " + charred + ", her statue " + statue);
        assertEquals(TemplePlan.BELL, TemplePlan.code(-21, 2, 19), "the great bell where it fell");
    }

    @Test
    void thePeakStandsOverTheCaveAndTheValleyRunsLevelBeforeIt() {
        long seed = 12345;
        int[] c = Zones.BLACK_WIND.centres(seed)[0];
        int peak = BlackWindShape.riseAt(seed, c[0], c[1] + BlackWindShape.PEAK_Z);
        assertTrue(peak > 150, "the black peak: " + peak);
        assertTrue(BlackWindShape.riseAt(seed, c[0], c[1] + 300) < peak / 2, "falling toward the edge");
        assertEquals(0, BlackWindShape.valleyShare(0, BlackWindPlan.FACE_Z + 10), 1e-9, "the forecourt cut to its floor");
        assertEquals(1, BlackWindShape.valleyShare(0, BlackWindPlan.FACE_Z - 1), 1e-9, "the cliff over the door");
        assertEquals(0, BlackWindShape.valleyShare(30, TemplePlan.TEMPLE_Z), 1e-9, "the terrace wide enough for the temple");
        BlackWindShape.Floors f = new BlackWindShape.Floors(90, 80, 74);
        assertEquals(90, BlackWindShape.floorAt(f, 70), 1e-9);
        assertEquals(80, BlackWindShape.floorAt(f, TemplePlan.TEMPLE_Z), 1e-9);
        assertEquals(74, BlackWindShape.floorAt(f, BlackWindShape.MOUTH + 5), 1e-9);
        for (BlackWindShape.Camp camp : BlackWindShape.camps(seed, c[0], c[1])) {
            assertFalse(BlackWindShape.onValleyFloor(camp.x() - c[0], camp.z() - c[1]), "no camp in the valley's way");
        }
    }
}
