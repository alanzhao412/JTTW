package com.jttw.engine.world;

import com.jttw.engine.world.ridge.WhiteBoneCave;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

import static com.jttw.engine.world.ridge.WhiteBoneCave.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The White Bone Cave's blueprint: eight floors, each a great hall; the way from the great door down
 * through every floor to the throne room, walked block by block (shut seals and grates barring it
 * floor by floor, as they should); the well of the dead; the posts; its heights within the world's;
 * and the ridge's region big enough for the mountain, the road and the outposts round it.
 */
class WhiteBoneCaveTest {

    private static final int R = REACH + 1, W = 2 * R + 1;

    /** The blueprint over the whole inside, for a seed. */
    private static byte[][][] grid(WhiteBoneCave cave) {
        byte[][][] g = new byte[W][W][];
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            byte[] col = new byte[H];
            cave.column(x, z, col);
            g[x + R][z + R] = col;
        }
        return g;
    }

    private static byte at(byte[][][] g, int x, int y, int z) {
        if (Math.abs(x) > R || Math.abs(z) > R || y < Y_MIN || y > Y_MAX) return UNTOUCHED;
        return g[x + R][z + R][y - Y_MIN];
    }

    /** Whether one passes through a code: with {@code opened}, the seals, the grates and the door are open. */
    private static boolean through(byte c, boolean opened) {
        if (c == AIR || c == LADDER) return true;
        return opened && (c == BARS || c == GRATE || c == LEAF);
    }

    private static boolean stands(byte[][][] g, int x, int y, int z, boolean opened) {
        byte feet = at(g, x, y, z), head = at(g, x, y + 1, z), under = at(g, x, y - 1, z);
        if (!through(feet, opened) || !through(head, opened)) return false;
        if (feet == LADDER) return true;
        return under != UNTOUCHED && under != LAVA && under != SOUL_SOIL && !through(under, opened);
    }

    /** Every place one may stand reached on foot from the threshold of the great door (steps up of one, drops of three; ladders). */
    private static boolean[][][] walk(byte[][][] g, boolean opened, boolean doorOpen) {
        boolean[][][] seen = new boolean[W][W][H];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{0, 0, THRESHOLD_Z});
        seen[R][THRESHOLD_Z + R][-Y_MIN] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int x = p[0], y = p[1], z = p[2];
            for (int[] d : dirs) {
                int nx = x + d[0], nz = z + d[1];
                if (Math.abs(nx) > R || Math.abs(nz) > R) continue;
                for (int dy = 1; dy >= -3; dy--) {
                    int ny = y + dy;
                    if (ny < Y_MIN + 1 || ny > Y_MAX - 1) continue;
                    if (dy == 1 && !through(at(g, x, y + 2, z), opened || doorOpen && at(g, x, y + 2, z) == LEAF)) continue; // headroom to step up
                    boolean leafOk = doorOpen && (at(g, nx, ny, nz) == LEAF || at(g, nx, ny + 1, nz) == LEAF);
                    if (!stands(g, nx, ny, nz, opened || leafOk)) continue;
                    if (!seen[nx + R][nz + R][ny - Y_MIN]) {
                        seen[nx + R][nz + R][ny - Y_MIN] = true;
                        q.add(new int[]{nx, ny, nz});
                    }
                    break;
                }
            }
            if (at(g, x, y, z) == LADDER) {
                for (int dy = -1; dy <= 1; dy += 2) {
                    int ny = y + dy;
                    if (ny < Y_MIN + 1 || ny > Y_MAX - 1) continue;
                    if (!through(at(g, x, ny, z), opened) || !through(at(g, x, ny + 1, z), opened) && dy > 0) continue;
                    if (!seen[x + R][z + R][ny - Y_MIN]) {
                        seen[x + R][z + R][ny - Y_MIN] = true;
                        q.add(new int[]{x, ny, z});
                    }
                }
            }
        }
        return seen;
    }

    private static boolean reached(boolean[][][] seen, int x, int y, int z) {
        return seen[x + R][z + R][y - Y_MIN];
    }

    @Test
    void eightFloorsEachAGreatHallThenTheThroneRoom() {
        WhiteBoneCave cave = WhiteBoneCave.of(12345L);
        assertEquals(8, FLOORS);
        for (int k = 0; k <= FLOORS; k++) {
            int open = 0, minX = 99, maxX = -99, minZ = 99, maxZ = -99;
            for (int x = -REACH; x <= REACH; x++) for (int z = -REACH; z <= REACH; z++) {
                if (!WhiteBoneCave.open(cave.plan(k, x, z))) continue;
                open++;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
            assertTrue(open >= (k == FLOORS ? 3500 : 2500), NAMES[k] + " is a great hall: " + open + " blocks of floor");
            assertTrue(maxX - minX >= 60 && maxZ - minZ >= 60, NAMES[k] + " spans " + (maxX - minX) + " by " + (maxZ - minZ));
        }
        for (int k = 1; k < FLOORS; k++) assertEquals(floorY(k - 1) - STEP, floorY(k), "each floor " + STEP + " under the one before");
        assertTrue(THRONE_Y < floorY(FLOORS - 1) - 20, "the throne room under the last");
        assertTrue(THRONE_H >= 20, "room for her");
    }

    @Test
    void theFloorsDiffer() {
        WhiteBoneCave cave = WhiteBoneCave.of(42L);
        java.util.Set<String> plans = new java.util.HashSet<>();
        for (int k = 0; k < FLOORS; k++) {
            StringBuilder b = new StringBuilder();
            for (int x = -46; x <= 46; x += 3) for (int z = -46; z <= 46; z += 3) {
                int c = cave.plan(k, x, z);
                b.append(WhiteBoneCave.open(c) ? (char) ('a' + kind(c) + 8 * Math.max(0, -fo(c))) : '#');
            }
            plans.add(b.toString());
        }
        assertEquals(FLOORS, plans.size(), "no two floors laid out alike");
    }

    @Test
    void theWayFromTheDoorToTheThroneIsWalkedOnFootOnceTheSealsAreBroken() {
        for (long seed : new long[]{12345L, 42L, 7L}) {
            WhiteBoneCave cave = WhiteBoneCave.of(seed);
            byte[][][] g = grid(cave);
            boolean[][][] seen = walk(g, true, true);
            for (int k = 0; k < FLOORS; k++) {
                int s = side(k), f = floorY(k);
                assertTrue(reached(seen, -12, f, s * (HALL - 1)), "seed " + seed + ": " + NAMES[k] + "'s seal reached");
                assertTrue(reached(seen, -12, f, s * CORRIDOR_Z), "seed " + seed + ": through " + NAMES[k] + "'s seal");
                assertTrue(reached(seen, lastStep(k) + 2, floorY(k + 1), s * 53), "seed " + seed + ": down " + NAMES[k] + "'s stair");
                for (WhiteBoneCave.Post post : cave.posts(k)) {
                    assertTrue(reached(seen, post.x(), post.y(), post.z()), "seed " + seed + ": " + NAMES[k] + "'s post at " + post);
                }
            }
            assertTrue(reached(seen, 0, THRONE_Y, -6), "seed " + seed + ": the throne room");
            assertTrue(reached(seen, 6, THRONE_Y + 3, -32), "seed " + seed + ": the dais");
            for (WhiteBoneCave.Post post : cave.posts(FLOORS)) assertTrue(reached(seen, post.x(), post.y(), post.z()), "her court's post " + post);
        }
    }

    @Test
    void theDoorAndEachSealBarTheWayOn() {
        WhiteBoneCave cave = WhiteBoneCave.of(12345L);
        byte[][][] g = grid(cave);
        boolean[][][] shut = walk(g, false, false);
        assertFalse(reached(shut, 0, 0, 40), "the door shut, the first hall is not reached");
        boolean[][][] doorOnly = walk(g, false, true);
        assertTrue(reached(doorOnly, 0, 0, 10), "through the door: the first hall");
        assertTrue(reached(doorOnly, -12, 0, -(HALL - 1)), "the first seal reached");
        assertFalse(reached(doorOnly, -12, 0, -CORRIDOR_Z), "but not passed");
        for (int y = Y_MIN; y < grateY(1); y++) {
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
                assertFalse(reached(doorOnly, x, y, z), "nothing under the first floor reached with every seal and grate shut: " + x + " " + y + " " + z);
            }
        }
    }

    @Test
    void theWellOfTheDeadRunsFromTheFirstFloorToTheLastBehindItsGrates() {
        WhiteBoneCave cave = WhiteBoneCave.of(12345L);
        byte[] col = new byte[H];
        cave.column(SHAFT_X, SHAFT_Z, col);
        for (int k = 0; k < FLOORS; k++) {
            for (int y = floorY(k); y <= floorY(k) + 4; y++) assertEquals(LADDER, col[y - Y_MIN], "the ladder at floor " + k);
            byte[] corridor = new byte[H];
            cave.column(SHAFT_X, CORRIDOR_Z, corridor);
            assertEquals(AIR, corridor[floorY(k) - Y_MIN], "a corridor to it from floor " + k);
            if (k > 0) assertEquals(GRATE, col[grateY(k) - Y_MIN], "a grate over floor " + k + "'s landing");
        }
        assertNotEquals(LADDER, col[floorY(FLOORS - 1) - 1 - Y_MIN], "it ends at the last floor: not down to the throne room");
    }

    @Test
    void postsGrowStrongerFloorByFloorEachWithAGuardianAtItsSeal() {
        WhiteBoneCave cave = WhiteBoneCave.of(42L);
        int before = 0;
        for (int k = 0; k < FLOORS; k++) {
            List<WhiteBoneCave.Post> posts = cave.posts(k);
            WhiteBoneCave.Post guardian = posts.get(0);
            assertEquals(GUARDIAN, guardian.rank());
            assertEquals(side(k), Integer.signum(guardian.z()), "before the seal");
            long captains = posts.stream().filter(p -> p.rank() == CAPTAIN).count();
            assertEquals(1 + k / 2, captains, NAMES[k] + "'s captains");
            assertEquals(8 + k + 1 + k / 2 + 1, posts.size(), NAMES[k] + "'s posts all found ground");
            int level = floorLevel(k);
            assertTrue(level >= before && level >= 20 && guardian.level() <= 36, "levels rise within the band");
            before = level;
            for (WhiteBoneCave.Post p : posts) assertEquals(k, p.floor());
        }
        assertEquals(33, floorLevel(FLOORS - 1));
        assertEquals(6, cave.posts(FLOORS).size(), "her court");
        long wardens = cave.outside().stream().filter(p -> p.rank() == WARDEN).count();
        assertEquals(2, wardens, "two keepers of the door");
        assertTrue(cave.outside().size() >= 30, "the mountain crawls with them: " + cave.outside().size());
    }

    @Test
    void itFitsTheWorldsHeightsAndTheRegion() {
        assertTrue(MIN_Y0 + THRONE_Y - 5 >= -60, "its depths above the bedrock");
        assertTrue(MAX_Y0 + PEAK + 25 <= 300, "its peak under y 300");
        assertEquals(MIN_Y0, y0(0));
        assertEquals(MAX_Y0, y0(400));
        ZoneLayout l = Zones.WHITE_BONE.layout();
        int least = l.radius() - l.wobble();
        assertTrue(least >= MOUNTAIN_R + 150, "the region round the mountain is wide: " + least);
        assertTrue(ROAD_Z0 + ROAD_LEN + 20 < least, "the road within it");
        for (long seed : new long[]{1, 2, 42, 12345}) {
            for (int[] o : WhiteBoneCave.of(seed).outposts()) {
                assertTrue(Math.hypot(o[0], o[1]) + OUTPOST_R + 12 < least, "an outpost within it");
                assertTrue(Math.hypot(o[0], o[1]) > MOUNTAIN_R + 40, "an outpost off the mountain");
                double off = Math.toDegrees(Math.abs(Math.atan2(o[0], o[1])));
                assertTrue(off >= 29, "an outpost off the road: " + off);
            }
        }
    }

    @Test
    void theRegionIsBigAndKeepsClearOfEveryOtherZone() {
        assertTrue(ZoneLayout.WHITE_BONE.radius() >= 400, "about 840 blocks across");
        for (long seed = 0; seed < 40; seed++) {
            int[] c = Zones.WHITE_BONE.centres(seed)[0];
            for (Zones.Zone other : Zones.MAP.zones()) {
                if (other == Zones.WHITE_BONE || !other.vertical().overlaps(Zones.WHITE_BONE.vertical())) continue;
                for (int[] o : other.centres(seed)) {
                    assertTrue(Math.hypot(o[0] - c[0], o[1] - c[1]) >= other.layout().margin() + Zones.WHITE_BONE.layout().margin(),
                            "seed " + seed + ": clear of " + other.id());
                }
            }
            double d = Math.hypot(c[0], c[1]);
            assertTrue(d >= 1000 && d <= 2000, "1-2 km out: " + d);
        }
    }
}
