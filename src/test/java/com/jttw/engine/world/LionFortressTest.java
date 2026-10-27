package com.jttw.engine.world;

import com.jttw.engine.world.lion.LionCamelCity;
import com.jttw.engine.world.lion.LionFortress;
import com.jttw.engine.world.lion.RidgeApproach;
import com.jttw.engine.world.lion.RidgeShape;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import static com.jttw.engine.world.lion.LionFortress.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Lion-Camel Cave's blueprint: a great door, the gate hall, eight floors one under another, the
 * throne room at the bottom; a way on foot from the door down through every floor to the kings once
 * the gates are open, and none while they are shut; who keeps each floor; and where it lies in the
 * region, under the mountain behind the city.
 */
class LionFortressTest {

    /** The blueprint's kinds over its whole extent, by (u, ly, v) offset. */
    private static final class Grid {
        static final int U0 = -HALF - SHELL - 1, U1 = HALF + SHELL + 1, V0 = THRONE_N - SHELL - 1, V1 = FACADE_V + 8, Y0 = BOTTOM - 1, Y1 = TOP + 1;
        final byte[] kinds = new byte[(U1 - U0 + 1) * (V1 - V0 + 1) * (Y1 - Y0 + 1)];

        Grid() {
            for (int u = U0; u <= U1; u++) for (int v = V0; v <= V1; v++) for (int y = Y0; y <= Y1; y++) {
                int k = LionFortress.kind(cell(u, y, FZ + v));
                // outside the blueprint: the forecourt's level ground at the door, rock beside and under it, air over it
                if (k == OUT) k = y <= 0 ? (v > DOOR_V && Math.abs(u) <= HALF + SHELL ? (y == 0 ? FLOOR : ROCK) : ROCK) : OPEN;
                kinds[i(u, y, v)] = (byte) k;
            }
        }

        int i(int u, int y, int v) {
            return ((u - U0) * (V1 - V0 + 1) + (v - V0)) * (Y1 - Y0 + 1) + (y - Y0);
        }

        boolean in(int u, int y, int v) {
            return u >= U0 && u <= U1 && v >= V0 && v <= V1 && y >= Y0 && y <= Y1;
        }

        int kind(int u, int y, int v) {
            return in(u, y, v) ? kinds[i(u, y, v)] : ROCK;
        }

        boolean blocks(int u, int y, int v, boolean gatesOpen) {
            int k = kind(u, y, v);
            if (gatesOpen && (k == GATE || k == DOOR)) return false;
            return solid(k);
        }

        /** Whether one can stand with his feet at (u, y, v). */
        boolean stand(int u, int y, int v, boolean gatesOpen) {
            int under = kind(u, y - 1, v);
            return ground(under) && !blocks(u, y, v, gatesOpen) && !blocks(u, y + 1, v, gatesOpen) && kind(u, y, v) != LAVA;
        }

        /** Every place one can walk to from (u, y, v): a step up of one, a drop of up to three. */
        Map<Long, Boolean> walk(int u, int y, int v, boolean gatesOpen) {
            Map<Long, Boolean> seen = new HashMap<>();
            ArrayDeque<int[]> todo = new ArrayDeque<>();
            assertTrue(stand(u, y, v, gatesOpen), "one can stand at the start " + u + " " + y + " " + v);
            todo.add(new int[]{u, y, v});
            seen.put(key(u, y, v), true);
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!todo.isEmpty()) {
                int[] p = todo.poll();
                for (int[] d : dirs) {
                    int nu = p[0] + d[0], nv = p[2] + d[1];
                    for (int dy = 1; dy >= -3; dy--) {
                        int ny = p[1] + dy;
                        if (dy == 1 && (blocks(p[0], p[1] + 2, p[2], gatesOpen) || blocks(nu, ny + 1, nv, gatesOpen))) continue;
                        if (dy < 0) {
                            // stepping off: the way over must be clear down to where one lands
                            boolean clear = true;
                            for (int y2 = p[1]; y2 > ny && clear; y2--) clear = !blocks(nu, y2, nv, gatesOpen) && !blocks(nu, y2 + 1, nv, gatesOpen);
                            if (!clear) continue;
                        }
                        if (!stand(nu, ny, nv, gatesOpen)) continue;
                        if (seen.putIfAbsent(key(nu, ny, nv), true) == null) todo.add(new int[]{nu, ny, nv});
                        break;
                    }
                }
            }
            return seen;
        }

        static long key(int u, int y, int v) {
            return ((long) (u + 512) << 40) | ((long) (y + 512) << 20) | (v + 512);
        }
    }

    private static final Grid GRID = new Grid();

    @Test
    void eightFloorsOneUnderAnotherAndTheThroneRoomAtTheBottom() {
        assertEquals(8, FLOORS);
        for (int k = 1; k <= FLOORS; k++) assertEquals(floorY(k - 1) - PITCH, floorY(k), "floor " + k + " a pitch under the last");
        assertTrue(floorY(THRONE) < floorY(FLOORS) - AIR, "the throne room under the last floor");
        assertEquals(THRONE, bandOf(floorY(THRONE) + 1));
        for (int k = 0; k <= FLOORS; k++) {
            assertEquals(k, bandOf(floorY(k) + 1), "floor " + k + "'s hall is its band");
            // each a great hall, not a corridor: most of its 63 by 63 open to walk in
            int open = 0;
            for (int u = -HALF; u <= HALF; u++) for (int v = -HALF; v <= HALF; v++) {
                if (GRID.stand(u, floorY(k) + 1, v, true)) open++;
            }
            assertTrue(open > 2000, "floor " + k + " is a great hall: " + open + " places to stand");
            assertEquals(airOf(k), k == 0 ? HALL_AIR : AIR);
        }
        // the throne room: wide and high enough for the kings' true forms to fight in
        int open = 0;
        for (int u = -HALF; u <= HALF; u++) for (int v = THRONE_N; v <= THRONE_S; v++) if (GRID.stand(u, THRONE_Y + 1, v, true)) open++;
        assertTrue(open > 2500, "the throne room is vast: " + open);
        assertTrue(THRONE_AIR >= 16, "and high");
        // it fits in the world under a city at the lowest floor the city may have
        assertTrue(-64 + RidgeShape.FORTRESS_ROOM + BOTTOM >= -64 + 2, "over the world's floor");
        assertTrue(RidgeShape.HIGHEST + TOP <= 300 && RidgeShape.HIGHEST + Math.ceil(RidgeShape.MASSIF_H * 1.2) <= 300,
                "and under y 300 at the highest, the mountain over it too");
    }

    @Test
    void eachFloorsStairGoesDownOneStepABlockToTheNext() {
        for (int k = 0; k <= FLOORS; k++) {
            Stair s = stair(k);
            int below = floorY(k == FLOORS ? THRONE : k + 1);
            assertEquals(floorY(k) - below, s.steps(), "floor " + k + ": a step for each block down");
            for (int i = 1; i <= s.steps(); i++) {
                int u = (s.u0() + s.u1()) / 2, y = floorY(k) - i;
                assertEquals(STEP, GRID.kind(u, y, s.stepV(i)), "floor " + k + " step " + i);
                assertFalse(solid(GRID.kind(u, y + 1, s.stepV(i))) || solid(GRID.kind(u, y + 2, s.stepV(i))), "headroom on step " + i + " of floor " + k);
            }
            // its portcullis across the way to it
            for (int u = s.u0(); u <= s.u1(); u++) assertEquals(GATE, GRID.kind(u, floorY(k) + 1, s.gate()), "floor " + k + "'s gate at " + u);
        }
    }

    @Test
    void fromTheDoorOneWalksDownThroughEveryFloorToTheKingsOnceTheGatesAreOpen() {
        Map<Long, Boolean> seen = GRID.walk(0, 1, DOOR_V + 4, true);
        for (int k = 0; k <= FLOORS; k++) {
            Stair s = stair(k);
            int mid = (s.u0() + s.u1()) / 2;
            assertTrue(seen.containsKey(Grid.key(mid, floorY(k) + 1, s.landing())), "floor " + k + "'s way down is reached");
            int[] a = arrival(k);
            assertTrue(seen.containsKey(Grid.key(a[0], floorY(k) + 1, a[1] - (k == 0 ? 0 : 0))), "floor " + k + " is come onto at " + a[0] + " " + a[1]);
            int[] g = gatePost(k);
            assertTrue(seen.containsKey(Grid.key(g[0], floorY(k) + 1, g[1])), "floor " + k + "'s warden's place is reached");
        }
        assertTrue(seen.containsKey(Grid.key(LION_POST[0], THRONE_Y + 1, LION_POST[1])), "the Azure Lion's place is reached");
        assertTrue(seen.containsKey(Grid.key(ELEPHANT_POST[0], THRONE_Y + 1, ELEPHANT_POST[1])), "the White Elephant's place is reached");
        // every keeper's post is on the way
        for (Post p : POSTS) {
            if (p.floor() < 0) continue;
            assertTrue(seen.containsKey(Grid.key(p.u(), p.ly(), p.v())), "the post on floor " + p.floor() + " at " + p.u() + " " + p.v() + " is reached");
        }
    }

    @Test
    void whileTheGatesAreShutTheWayIsBarred() {
        // outside, the great door keeps one out of the gate hall
        Map<Long, Boolean> outside = GRID.walk(0, 1, DOOR_V + 4, false);
        assertFalse(outside.containsKey(Grid.key(0, 1, 0)), "the great door bars the gate hall");
        // in the gate hall, the first portcullis keeps one from the floors under it
        Map<Long, Boolean> hall = GRID.walk(0, 1, 0, false);
        for (int k = 1; k <= THRONE; k++) {
            int[] a = arrival(k);
            assertFalse(hall.containsKey(Grid.key(a[0], floorY(k) + 1, a[1])), "floor " + k + " is barred from the gate hall");
        }
        // on each floor, its own portcullis keeps one from the next
        for (int k = 1; k <= FLOORS; k++) {
            int[] a = arrival(k), next = arrival(k + 1);
            Map<Long, Boolean> on = GRID.walk(a[0], floorY(k) + 1, a[1], false);
            assertFalse(on.containsKey(Grid.key(next[0], floorY(k + 1) + 1, next[1])), "floor " + (k + 1) + " is barred from floor " + k);
            assertTrue(on.containsKey(Grid.key(gatePost(k)[0], floorY(k) + 1, gatePost(k)[1])), "but floor " + k + "'s warden is reached");
        }
    }

    @Test
    void eachFloorIsKeptDeeperByMoreAndStrongerAndAWardenHoldsItsWayDown() {
        int last = 0;
        for (int k = 0; k <= FLOORS; k++) {
            final int floor = k;
            long keepers = POSTS.stream().filter(p -> p.floor() == floor && p.role() != WARDEN).count();
            long wardens = POSTS.stream().filter(p -> p.floor() == floor && p.role() == WARDEN).count();
            long caps = POSTS.stream().filter(p -> p.floor() == floor && p.role() == CAPTAIN).count();
            assertEquals(regulars(k), keepers, "floor " + k + "'s keepers");
            assertEquals(1, wardens, "floor " + k + "'s one warden");
            assertEquals(captains(k), caps, "floor " + k + "'s captains");
            assertTrue(keepers >= last, "no fewer deeper");
            last = (int) keepers;
            assertTrue(levelOf(k, REGULAR) >= 89 && levelOf(k, WARDEN) <= 100, "within the band");
            if (k > 0) assertTrue(levelOf(k, REGULAR) > levelOf(k - 1, REGULAR), "stronger deeper");
            assertTrue(levelOf(k, WARDEN) >= levelOf(k, CAPTAIN) && levelOf(k, CAPTAIN) > levelOf(k, REGULAR));
            Post w = POSTS.stream().filter(p -> p.floor() == floor && p.role() == WARDEN).findFirst().orElseThrow();
            assertArrayEquals(gatePost(k), new int[]{w.u(), w.v()}, "floor " + k + "'s warden before its gate");
        }
        assertEquals(6, POSTS.stream().filter(p -> p.floor() < 0).count(), "six wardens of the great door");
        assertEquals(2, POSTS.stream().filter(p -> p.role() == KING).count(), "the two kings, in the throne room");
        POSTS.stream().filter(p -> p.role() == KING).forEach(p -> assertEquals(THRONE, p.floor()));
        for (Post p : POSTS) {
            if (p.floor() < 0 || p.role() == KING) continue;
            assertTrue(standable(p.floor(), p.u(), p.v()), "ground and room at the post on floor " + p.floor() + " at " + p.u() + " " + p.v());
        }
    }

    @Test
    void theKingsHaveRoomForTheirTrueFormsAndTheirTreasuryIsBesideThePengsThrone() {
        // round each king's place, room for a beast four and a half blocks high and wide
        for (int[] post : new int[][]{LION_POST, ELEPHANT_POST}) {
            for (int u = post[0] - 3; u <= post[0] + 3; u++) for (int v = post[1] - 3; v <= post[1] + 3; v++) for (int h = 1; h <= 6; h++) {
                assertFalse(solid(GRID.kind(u, THRONE_Y + h, v)), "room about the king at " + post[0] + " " + post[1] + ": " + u + " " + h + " " + v);
            }
        }
        assertTrue(Math.hypot(LION_POST[0] - ELEPHANT_POST[0], LION_POST[1] - ELEPHANT_POST[1]) > 10, "each king his own ground");
        // the treasury: one chest in all the fortress, on the dais by the Peng's empty throne
        int chests = 0;
        for (int u = Grid.U0; u <= Grid.U1; u++) for (int v = Grid.V0; v <= Grid.V1; v++) for (int y = Grid.Y0; y <= Grid.Y1; y++) {
            if (GRID.kind(u, y, v) == CHEST) {
                chests++;
                assertArrayEquals(new int[]{TREASURY[0], TREASURY[1], TREASURY[2]}, new int[]{u, y, v});
            }
        }
        assertEquals(1, chests, "one chest");
        assertTrue(Math.abs(TREASURY[0] - PENG_U) <= 4, "beside the Peng's throne");
        assertTrue(ground(GRID.kind(TREASURY[0], TREASURY[1] - 1, TREASURY[2])), "on the dais");
    }

    @Test
    void itLiesNorthOfTheCityInsideTheRegionUnderTheMountain() {
        Zones.Zone l = Zones.LION_CAMEL;
        assertTrue(DOOR_LZ < -LionCamelCity.OUTER - 8, "the door is behind the city, past its north wall and a forecourt");
        for (long seed : new long[]{1L, 42L, 12345L, -7L}) {
            int[] c = l.centres(seed)[0];
            for (int u : new int[]{-HALF - SHELL, HALF + SHELL}) for (int v : new int[]{THRONE_N - SHELL, DOOR_V + 1, FACADE_V}) {
                assertTrue(l.depth(seed, c[0] + u, c[1] + FZ + v) > 0, "seed " + seed + ": the fortress inside its region at " + u + " " + v);
            }
            // the mountain stands over the gate hall's roof, its cliff at the door
            for (int u = -HALF - SHELL; u <= HALF + SHELL; u += 2) for (int lz = FZ - HALF; lz <= DOOR_LZ - 2; lz += 2) {
                assertTrue(RidgeShape.massifAt(seed, u, lz) > HALL_AIR + SHELL, "seed " + seed + ": the mountain over the gate hall at " + u + " " + lz
                        + ": " + RidgeShape.massifAt(seed, u, lz));
                assertEquals(1.0, RidgeShape.massifBlend(u, lz), 1e-9, "the mountain wholly itself over the gate hall at " + u + " " + lz);
            }
            for (int u = -FORECOURT_HALF; u <= FORECOURT_HALF; u++) assertEquals(0, RidgeShape.massifAt(seed, u, DOOR_LZ + 1), "the forecourt is open");
            // the outposts and the roads are on the region's ground
            for (RidgeApproach.Outpost o : RidgeApproach.OUTPOST_LIST) {
                assertTrue(l.depth(seed, c[0] + o.dx(), c[1] + o.dz()) > 0, "seed " + seed + ": an outpost inside the region");
            }
        }
        // the forecourt is level ground, of the city's apron
        assertEquals(0, RidgeShape.cityShare(0, DOOR_LZ + 2), 1e-9);
        assertTrue(RidgeShape.onApron(10, -65));
    }

    @Test
    void theRoadsComeInFromTheEdgeToTheGatesAndTheOutpostsKeepThem() {
        assertEquals(3, RidgeApproach.ROADS);
        assertEquals(9, RidgeApproach.OUTPOST_LIST.size());
        for (int r = 0; r < RidgeApproach.ROADS; r++) {
            // at the gate, straight out of it
            int[] p = RidgeApproach.point(r, RidgeApproach.ROAD_FROM, 0);
            assertEquals(0, RidgeApproach.bend(r, RidgeApproach.ROAD_FROM));
            var spot = RidgeApproach.road(p[0], p[1]);
            assertNotNull(spot);
            assertEquals(r, spot.road());
            // continuous out to the edge: every block along it is on it
            for (int a = RidgeApproach.ROAD_FROM; a <= 300; a++) {
                int[] q = RidgeApproach.point(r, a, 0);
                assertNotNull(RidgeApproach.road(q[0], q[1]), "road " + r + " at " + a);
            }
        }
        // the outposts stand off the roads, apart
        for (RidgeApproach.Outpost o : RidgeApproach.OUTPOST_LIST) {
            assertNull(RidgeApproach.road(o.dx(), o.dz()), "an outpost's tower off the road");
            for (RidgeApproach.Outpost other : RidgeApproach.OUTPOST_LIST) {
                if (other != o) assertTrue(Math.hypot(o.dx() - other.dx(), o.dz() - other.dz()) > 2 * RidgeApproach.OUTPOST_R, "outposts apart");
            }
            assertFalse(LionCamelCity.covers(o.dx(), o.dz()) || LionFortress.covers(o.dx(), o.dz()));
        }
        assertTrue(RidgeApproach.POSTS.size() >= 9 * 4 + 3 * 4 * 3, "a horde out on the ridge: " + RidgeApproach.POSTS.size());
    }
}
