package com.jttw.engine.world;

import com.jttw.engine.world.feature.Labyrinth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Bottomless Cave's blueprint: eight floors of maze, one under another, each wider and harder;
 * walked on foot from the great door to her throne and back; the same from every chunk.
 */
class LabyrinthTest {

    private static final long[] SEEDS = {1L, 42L, 987654321L, -7L, 12345L};

    private static Labyrinth cave(long seed, int region) {
        return Labyrinth.of(seed, region * 1536 + 500, -region * 1536 - 300, Labyrinth.Land.flat(70 + region * 23));
    }

    @Test
    void theSameRegionGivesTheSameCaveAndAnotherDoesNot() {
        Labyrinth a = Labyrinth.of(42L, 1000, -2000), b = Labyrinth.of(42L, 1000, -2000), c = Labyrinth.of(43L, 1000, -2000);
        assertSame(a, b, "kept, once worked out");
        byte[] ca = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1], cb = ca.clone(), cc = ca.clone();
        int differ = 0;
        for (int x = 1000 - 100; x <= 1000 + 100; x += 3) for (int z = -2000 - 100; z <= -2000 + 100; z += 3) {
            a.column(x, z, ca);
            b.column(x, z, cb);
            c.column(x, z, cc);
            assertArrayEquals(ca, cb);
            if (!java.util.Arrays.equals(ca, cc)) differ++;
        }
        assertTrue(differ > 1000, "another seed, another cave: " + differ);
    }

    @Test
    void eightFloorsOneUnderAnotherEachWiderAndTheThroneAtTheBottom() {
        assertEquals(8, Labyrinth.FLOORS);
        assertEquals(Labyrinth.FLOORS, Labyrinth.FLOOR.length);
        for (int k = 1; k < Labyrinth.FLOORS; k++) {
            assertTrue(Labyrinth.FLOOR[k - 1] - Labyrinth.FLOOR[k] >= 15, "each floor well under the last");
            assertTrue(Labyrinth.bottom(k - 1) > Labyrinth.top(k), "no floor's heights in another's");
            assertTrue(Labyrinth.REACH[k] > Labyrinth.REACH[k - 1], "each wider than the last");
        }
        assertTrue(Labyrinth.top(Labyrinth.FLOORS) < Labyrinth.bottom(Labyrinth.FLOORS - 1), "the throne level under the last floor");
        assertTrue(Labyrinth.Y_MIN >= -60, "over the bedrock");
        assertTrue(Labyrinth.THRONE <= -40, "near the bottom of the world");
        assertTrue(Labyrinth.Y_MAX <= 300, "under the build limit's last few");
        assertTrue(Labyrinth.FLOOR[0] - Labyrinth.THRONE >= 140, "super deep: " + (Labyrinth.FLOOR[0] - Labyrinth.THRONE));
        for (long seed : SEEDS) {
            Labyrinth l = cave(seed, 1);
            assertTrue(l.doorY >= Labyrinth.DOOR_MIN && l.doorY >= Labyrinth.FLOOR[0] + Labyrinth.SLAB_ABOVE + 4, "the door over the first floor");
            int last = 0;
            for (int k = 0; k < Labyrinth.FLOORS; k++) {
                int cells = l.level(k).cells(0) + l.level(k).cells(1);
                assertTrue(cells > last, "floor " + (k + 1) + " larger than the one over it: " + cells);
                last = cells;
            }
            assertTrue(last > 500, "the deepest a great maze: " + last);
        }
    }

    @Test
    void fromTheDoorEveryFloorsWayDownAndHerThroneAreReachedOnFoot() {
        for (long seed : SEEDS) for (int region = 0; region < 2; region++) {
            Labyrinth l = cave(seed, region);
            int[] in = l.beforeDoor(-2);
            Labyrinth.Walk w = l.walk(in[0], in[1], in[2], 6, true);
            String where = "seed " + seed + " region " + region + ": ";
            for (int k = 0; k < Labyrinth.LEVELS; k++) {
                Labyrinth.Level lv = l.level(k);
                int[] a = lv.arrival();
                assertTrue(w.reachedCell(k, a[0], a[1], 0), where + "where one comes into level " + k);
                int[] x = lv.exit();
                if (x != null) assertTrue(w.reachedCell(k, x[0], x[1], 0), where + "floor " + (k + 1) + "'s way down");
            }
            int[] d = l.dais(), r = l.refuge();
            assertTrue(w.reached(d[0], d[1], d[2] + 6) || w.reached(d[0] + 6, d[1], d[2]), where + "her palace");
            assertTrue(w.reached(r[0], r[1], r[2]), where + "her shrine");
            assertTrue(w.size() > 30_000, where + "a great deal walked: " + w.size());
        }
    }

    @Test
    void andFromHerThroneTheWayBackUpToTheDoor() {
        for (long seed : new long[]{42L, 12345L}) {
            Labyrinth l = cave(seed, 0);
            int[] d = l.dais();
            int[] a = l.antechamber(), m = l.cellMiddle(a[0], a[1]);
            Labyrinth.Walk w = l.walk(m[0], Labyrinth.THRONE, m[1], 6, true);
            int[] in = l.beforeDoor(-2);
            assertTrue(w.reached(in[0], in[1], in[2]), "seed " + seed + ": no one is trapped below");
            assertTrue(w.reached(d[0], d[1], d[2] + 6) || w.reached(d[0] + 6, d[1], d[2]), "the palace from its antechamber");
        }
    }

    @Test
    void theDoorIsTheOnlyWayInAndShutItHolds() {
        for (long seed : SEEDS) {
            Labyrinth l = cave(seed, 0);
            int[] dir = Labyrinth.DIRS[l.doorDir];
            byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
            int leaves = 0;
            for (int c = -Labyrinth.HALL_HALF - 2; c <= Labyrinth.HALL_HALF + 2; c++) {
                int x = l.cx + dir[0] * Labyrinth.DOOR_AT + dir[1] * c, z = l.cz + dir[1] * Labyrinth.DOOR_AT - dir[0] * c;
                l.column(x, z, col);
                for (int y = l.doorY - 1; y <= l.doorY + Labyrinth.HALL_HIGH; y++) {
                    byte b = col[y - Labyrinth.Y_MIN];
                    assertFalse(Labyrinth.passable(b, false), "seed " + seed + ": the door's plane is shut at " + c + "," + y);
                    if (b == Labyrinth.GATE) leaves++;
                }
            }
            assertEquals((2 * Labyrinth.DOOR_HALF + 1) * Labyrinth.DOOR_HIGH, leaves, "a giant door: nine wide, eleven high");
            // shut, the walk from inside never gets out past it
            int[] in = l.beforeDoor(-2), out = l.beforeDoor(1);
            assertFalse(l.walk(in[0], in[1], in[2], 6, false).reached(out[0], out[1], out[2]), "shut");
            assertTrue(l.walk(in[0], in[1], in[2], 6, true).reached(out[0], out[1], out[2]), "open");
        }
    }

    @Test
    void eachFloorIsAHardMazeOfDeadEndsLoopsAndSeveralHeights() {
        for (long seed : SEEDS) {
            Labyrinth l = cave(seed, 0);
            int lastDepth = 0;
            for (int k = 0; k < Labyrinth.FLOORS; k++) {
                Labyrinth.Level lv = l.level(k);
                int deadEnds = 0, branches = 0, tunnels = 0, ramps = 0, drops = 0;
                for (int i = 0; i < Labyrinth.SIZE; i++) for (int j = 0; j < Labyrinth.SIZE; j++) {
                    if (lv.ramp(i, j) >= 0) ramps++;
                    if (lv.drop(i, j)) drops++;
                    if (!lv.low(i, j) || lv.roomOf(i, j) != null) continue;
                    tunnels++;
                    int ways = 0;
                    for (int d = 0; d < 4; d++) if (lv.passage(i, j, d)) ways++;
                    if (ways == 1) deadEnds++;
                    if (ways >= 3) branches++;
                }
                String where = "seed " + seed + " floor " + (k + 1) + ": ";
                assertTrue(deadEnds >= tunnels / 6, where + "dead ends " + deadEnds + " of " + tunnels);
                assertTrue(branches >= tunnels / 10, where + "branchings " + branches + " of " + tunnels);
                assertTrue(lv.cells(1) >= 10, where + "galleries over the tunnels: " + lv.cells(1));
                assertTrue(ramps >= 1, where + "ramps up to them");
                assertTrue(drops >= 1, where + "holes to drop through");
                assertTrue(lv.rooms().size() >= 3, where + "halls");
                // the way down lies deep in it, far by the walk from where one comes in
                int[] a = lv.arrival(), x = lv.exit();
                assertTrue(lv.depth() >= 12, where + "the way down " + lv.depth() + " cells' walk in");
                assertTrue(Math.hypot(a[0] - x[0], a[1] - x[1]) >= 4, where + "not beside where one came in");
                lastDepth = Math.max(lastDepth, lv.depth());
            }
            assertTrue(lastDepth >= 25, "seed " + seed + ": the deeper floors' ways down are long: " + lastDepth);
        }
    }

    @Test
    void eachWayDownIsHeldAndTheDeeperFloorsHoldMore() {
        for (long seed : SEEDS) {
            Labyrinth l = cave(seed, 0);
            int[] held = new int[Labyrinth.FLOORS], guardians = new int[Labyrinth.FLOORS];
            for (Labyrinth.Post p : l.posts()) {
                if (p.kind() == Labyrinth.GUARDIAN) {
                    guardians[p.level()]++;
                    int[] g = l.level(p.level()).guard(), x = l.level(p.level()).exit();
                    assertArrayEquals(g, new int[]{p.i(), p.j()}, "the guardian before the way down");
                    assertEquals(1, Math.abs(g[0] - x[0]) + Math.abs(g[1] - x[1]), "beside it");
                } else held[p.level()] += p.count();
            }
            for (int k = 0; k < Labyrinth.FLOORS; k++) {
                assertEquals(1, guardians[k], "one guardian on floor " + (k + 1));
                assertTrue(held[k] >= 6, "floor " + (k + 1) + " holds " + held[k]);
            }
            assertTrue(held[7] > held[0], "the deepest holds more than the first: " + held[7] + " to " + held[0]);
        }
    }

    @Test
    void theStairsBetweenFloorsSpiralDownAStepABlock() {
        Labyrinth l = Labyrinth.of(5L, 0, 0);
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        assertEquals(Labyrinth.FLOORS, l.stairs().size(), "one down from each floor, the last to the throne level");
        for (Labyrinth.Stair s : l.stairs()) {
            int lower = s.lowerY(), upper = s.upperY();
            assertEquals(Labyrinth.floorY(s.upper() + 1), lower);
            int[] mid = l.cellMiddle(s.i(), s.j());
            java.util.List<Integer> heights = new java.util.ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                l.column(mid[0] + dx, mid[1] + dz, col);
                for (int y = lower; y < upper; y++) if (Labyrinth.step(col[y - Labyrinth.Y_MIN])) heights.add(y);
                if (dx == 0 && dz == 0) assertEquals(Labyrinth.CORE, col[lower + 2 - Labyrinth.Y_MIN], "a light in the pillar");
            }
            java.util.Collections.sort(heights);
            assertEquals(upper - lower, heights.size(), "a step a block of the fall");
            for (int n = 0; n < heights.size(); n++) assertEquals(lower + n, (int) heights.get(n));
            // the top step is the upper floor, on the side it is entered from, and open over it
            int[] top = {mid[0] + Labyrinth.DIRS[s.dir()][0], mid[1] + Labyrinth.DIRS[s.dir()][1]};
            l.column(top[0], top[1], col);
            assertTrue(Labyrinth.step(col[upper - 1 - Labyrinth.Y_MIN]), "the top step is the upper floor");
            assertTrue(Labyrinth.open(col[upper - Labyrinth.Y_MIN]), "and open over it");
        }
        assertEquals(Labyrinth.floorY(Labyrinth.FLOORS - 1) - Labyrinth.THRONE, l.stairs().get(Labyrinth.FLOORS - 1).upperY() - l.stairs().get(Labyrinth.FLOORS - 1).lowerY(),
                "the last descent, the longest");
    }

    @Test
    void theGreatShaftsStairIsWalkedWithoutAJumpFromTheHallDown() {
        Labyrinth l = Labyrinth.of(3L, 0, 0);
        int top = l.doorY;
        for (int y0 = Labyrinth.FLOOR[0] + Labyrinth.HALL_HEIGHT; y0 + Labyrinth.SHAFT_PITCH <= top; y0 += Labyrinth.SHAFT_PITCH) {
            double[][] surface = new double[19][19];
            for (double[] row : surface) java.util.Arrays.fill(row, Double.NaN);
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                for (int y = y0; y < y0 + Labyrinth.SHAFT_PITCH; y++) {
                    byte c = l.shaftCode(dx, dz, y);
                    if (c == Labyrinth.HELIX) surface[dx + 9][dz + 9] = y + 1;
                    if (c == Labyrinth.HELIX_SLAB) surface[dx + 9][dz + 9] = y + 0.5;
                }
            }
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                double s = surface[dx + 9][dz + 9];
                double r = Math.hypot(dx, dz);
                if (Double.isNaN(s) || r < Labyrinth.VOID_RADIUS + 0.8 || r > Labyrinth.SHAFT_RADIUS - 0.4) continue;
                double best = 99;
                for (int[] d : Labyrinth.DIRS) {
                    double o = surface[dx + d[0] + 9][dz + d[1] + 9], ro = Math.hypot(dx + d[0], dz + d[1]);
                    if (Double.isNaN(o) || ro < Labyrinth.VOID_RADIUS + 0.8 || ro > Labyrinth.SHAFT_RADIUS - 0.4) continue;
                    double diff = Math.abs(o - s);
                    diff = Math.min(diff, Math.abs(diff - Labyrinth.SHAFT_PITCH));
                    best = Math.min(best, diff);
                }
                assertTrue(best <= 1.0, "a neighbour within a step at " + dx + "," + dz + " y " + y0);
            }
        }
        // its top step is the hall's floor, on the door's side
        int[] d = Labyrinth.DIRS[l.doorDir];
        assertEquals(Labyrinth.HELIX, l.shaftCode(d[0] * 5, d[1] * 5, top - 1), "the stair's head at the hall");
    }

    @Test
    void theTrailRunsFromThePalaceToTheShrineBehindIt() {
        for (long seed : SEEDS) {
            Labyrinth l = cave(seed, 1);
            List<int[]> trail = l.trail();
            Labyrinth.Level t = l.level(Labyrinth.FLOORS);
            assertEquals(l.palace(), t.roomOf(trail.get(0)[0], trail.get(0)[1]));
            int[] last = trail.get(trail.size() - 1);
            assertEquals(l.shrine(), t.roomOf(last[0], last[1]));
            for (int n = 1; n < trail.size(); n++) {
                int[] a = trail.get(n - 1), b = trail.get(n);
                assertEquals(1, Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]), "step by step");
            }
            int[] refuge = l.refuge();
            assertEquals(l.shrine(), l.roomAt(Labyrinth.FLOORS, refuge[0], refuge[2]), "the refuge is in the shrine");
            int[] a = l.antechamber();
            assertEquals(Labyrinth.PALACE_CELLS + 1, Math.max(Math.abs(a[0] - Labyrinth.N), Math.abs(a[1] - Labyrinth.N)), "the antechamber before a gate");
            double fled = Math.hypot(refuge[0] - l.cx, refuge[2] - l.cz);
            assertTrue(fled > 20 && fled < 52, "her shrine behind the palace, near: " + fled);
        }
    }

    @Test
    void itIsWorkedOutQuickly() {
        long start = System.nanoTime();
        for (int n = 0; n < 6; n++) Labyrinth.of(1000L + n, 0, 0, Labyrinth.Land.flat(64));
        double each = (System.nanoTime() - start) / 6e6;
        assertTrue(each < 500, "a cave's plan in " + each + " ms");
        Labyrinth l = Labyrinth.of(1000L, 0, 0);
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        start = System.nanoTime();
        for (int x = 0; x < 18; x++) for (int z = 0; z < 18; z++) l.column(x + 20, z + 20, col);
        double chunk = (System.nanoTime() - start) / 1e6;
        assertTrue(chunk < 200, "a chunk's columns in " + chunk + " ms");
    }

    /** Draws each floor's plan into build/labyrinth.txt, to look at. */
    @Test
    void drawsItsPlan() throws java.io.IOException {
        Labyrinth l = Labyrinth.of(42L, 0, 0);
        StringBuilder out = new StringBuilder();
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        int reach = (Labyrinth.N + 1) * Labyrinth.PITCH;
        for (int k = 0; k < Labyrinth.LEVELS; k++) {
            for (int storey = 0; storey < (k < Labyrinth.FLOORS ? 2 : 1); storey++) {
                int y = Labyrinth.floorY(k) + storey * Labyrinth.STOREY;
                out.append("level ").append(k).append(storey == 1 ? " galleries" : "").append(" (y ").append(y).append(")\n");
                for (int z = -reach; z <= reach; z++) {
                    for (int x = -reach; x <= reach; x++) {
                        l.column(x, z, col);
                        byte c = col[y - Labyrinth.Y_MIN];
                        var room = l.roomAt(k, x, z);
                        char ch = switch (c) {
                            case Labyrinth.AIR -> col[y - 1 - Labyrinth.Y_MIN] == Labyrinth.AIR ? 'v' : room == null ? ' '
                                    : room.kind() == Labyrinth.PALACE ? 'P' : room.kind() == Labyrinth.SHRINE ? 'S' : room.kind() == Labyrinth.HUB ? 'H' : '.';
                            case Labyrinth.ROCK -> '#';
                            case Labyrinth.UNTOUCHED -> '~';
                            case Labyrinth.WATER -> '=';
                            case Labyrinth.PILLAR, Labyrinth.CORE -> 'O';
                            case Labyrinth.LANTERN, Labyrinth.SOUL_LANTERN -> '*';
                            default -> Labyrinth.step(c) ? '^' : '?';
                        };
                        out.append(ch);
                    }
                    out.append('\n');
                }
            }
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("build"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("build", "labyrinth.txt"), out);
    }
}
