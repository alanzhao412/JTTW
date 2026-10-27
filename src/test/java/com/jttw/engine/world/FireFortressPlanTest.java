package com.jttw.engine.world;

import com.jttw.engine.world.fire.FortressPlan;
import org.junit.jupiter.api.Test;

import static com.jttw.engine.world.fire.FortressPlan.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Fire Cloud Cave's plan: eight floors and the throne room under them, a way on foot from the door to the throne, sealed between floors. */
class FireFortressPlanTest {

    private static final FortressPlan.Built P = FortressPlan.get();

    private static int[] feet(int[] box) {
        return new int[]{(box[0] + box[3]) / 2, box[1], (box[2] + box[5]) / 2};
    }

    /** Before the door, under its arch (the gorge's floor past it is the range's, not the plan's). */
    private static final int[] OUTSIDE = {0, 1, FACE_Z};
    /** Red Boy's dais. */
    private static final int[] DAIS = {0, THRONE_Y + 1, 0};

    @Test
    void eightFloorsDescendingThenTheThroneRoom() {
        assertEquals(8, FLOORS);
        assertEquals(10, P.floors().size(), "the forecourt, eight floors, the throne room");
        for (int k = 1; k < FLOORS; k++) assertTrue(FLOOR_Y[k] < FLOOR_Y[k - 1] - FLOOR_H[k] - 1, "floor " + (k + 1) + " under floor " + k);
        assertTrue(THRONE_Y + THRONE_H < FLOOR_Y[FLOORS - 1] - 1, "the throne room under the last floor");
        assertTrue(Y_MIN <= THRONE_Y - 3 && Y_MIN >= -125, "deep, but clear of bedrock over the sea's level: " + Y_MIN);
        assertTrue(Y_MAX <= 40, "never more than 40 over the gorge: " + Y_MAX);
        for (int k = 1; k <= FLOORS; k++) {
            FortressPlan.Floor f = P.floor(k);
            assertEquals(k, f.number());
            assertNotNull(f.seal(), "floor " + k + " sealed");
            assertNotNull(f.guardian(), "floor " + k + " guarded");
            assertNotNull(f.landing(), "floor " + k + " has a landing");
        }
        assertEquals(9, P.floor(9).number());
        assertNotNull(P.floor(9).landing());
    }

    @Test
    void eachFloorIsAGreatHall() {
        for (int k = 1; k <= FLOORS; k++) {
            int area = P.area(k);
            assertTrue(area >= 1200, "floor " + k + " (" + P.floor(k).name() + ") is a great hall, not a corridor: " + area);
        }
        assertTrue(P.area(9) >= 1500, "the throne room: " + P.area(9));
    }

    @Test
    void theDoorAndEachSealHoldTheWayUntilOpened() {
        // with the door and every seal open, one walks from the gorge to the throne
        int all = P.steps(OUTSIDE, DAIS, true);
        assertTrue(all > 0, "a way from the door down to the throne");
        // the door shut, no way in
        assertEquals(-1, P.steps(OUTSIDE, feet(P.floor(1).landing()), false), "the door bars the way");
        for (int k = 1; k <= FLOORS; k++) {
            int[] arrive = feet(P.floor(k).landing()), next = feet(P.floor(k + 1).landing());
            assertTrue(P.steps(arrive, next, true) > 0, "floor " + k + ": from where one comes in to the next floor down");
            assertEquals(-1, P.steps(arrive, next, false), "floor " + k + ": its seal holds the way down");
            // and its guardian stands where one can reach him
            FortressPlan.Spot g = P.floor(k).guardian();
            assertTrue(P.steps(arrive, new int[]{g.x(), g.y(), g.z()}, false) > 0, "floor " + k + ": its guardian is reachable");
        }
    }

    @Test
    void theGarrisonsStandOnTheirFloorsAndGrowWithDepth() {
        int last = 0;
        for (int k = 1; k <= FLOORS; k++) {
            FortressPlan.Floor f = P.floor(k);
            assertTrue(f.garrison().size() >= 12, "floor " + k + " garrisoned: " + f.garrison().size());
            for (FortressPlan.Spot s : f.garrison()) {
                assertEquals(f.y() + 1, s.y(), "on its floor");
                assertTrue(P.standable(s.x(), s.y(), s.z(), false), "floor " + k + " spot " + s);
                assertEquals(k, P.floorAt(s.x(), s.y(), s.z()));
            }
            long captains = f.garrison().stream().filter(s -> s.kind() == IMP_CAPTAIN || s.kind() == BRUTE_CAPTAIN).count();
            assertTrue(captains >= (k >= 4 ? 3 : 1), "floor " + k + " captains: " + captains);
            assertTrue(regularLevel(k) > last, "harder each floor down");
            last = regularLevel(k);
            assertTrue(captainLevel(k) <= 77 && guardianLevel(k) <= 77, "within the band");
        }
        assertEquals(68, regularLevel(1));
        assertEquals(75, regularLevel(8));
        assertEquals(77, guardianLevel(8));
    }

    @Test
    void theThroneRoomHasRoomForHisFight() {
        // open, level floor all round his dais, out past his carts' ring
        for (int x = -ARENA + 1; x <= ARENA - 1; x++) for (int z = -ARENA + 1; z <= ARENA - 1; z++) {
            if (x * x + z * z > (ARENA - 1) * (ARENA - 1)) continue;
            assertTrue(P.standable(x, THRONE_Y + 1, z, false), "the arena at " + x + " " + z);
            for (int y = THRONE_Y + 1; y <= THRONE_Y + 6; y++) assertTrue(open(code(x, y, z)), "room over the arena at " + x + " " + y + " " + z);
        }
        assertEquals(GILDED, code(0, THRONE_Y, 0), "his gilded dais");
        assertEquals(STAIR_N, code(0, THRONE_Y + 4, THRONE_Z), "his throne behind him");
        assertEquals(CHEST, code(3, THRONE_Y + 4, THRONE_Z - 2), "his treasury by it");
        // the room is shut but for the way in: nothing of a floor above opens into it
        assertTrue(THRONE_H >= 16, "high");
    }

    @Test
    void theDoorIsGiantAndTheFacadeFacesTheGorge() {
        int w = DOOR[3] - DOOR[0] + 1, h = DOOR[4] - DOOR[1] + 1, door = 0;
        assertTrue(w >= 13 && h >= 14, "a giant door: " + w + " x " + h);
        for (int x = DOOR[0]; x <= DOOR[3]; x++) for (int y = DOOR[1]; y <= DOOR[4]; y++) for (int z = DOOR[2]; z <= DOOR[5]; z++) {
            assertTrue(door(code(x, y, z)), "the door at " + x + " " + y + " " + z);
            door++;
        }
        assertEquals(w * h * 2, door, "two thick");
        assertEquals(GILDED, code(0, 18, FACE_Z + 1), "the plaque");
        assertEquals(BRAZIER, code(BRAZIER_AT[0], BRAZIER_AT[1], BRAZIER_AT[2]), "the return brazier");
    }
}
