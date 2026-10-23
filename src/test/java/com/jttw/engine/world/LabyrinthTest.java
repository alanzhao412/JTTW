package com.jttw.engine.world;

import com.jttw.engine.world.feature.Labyrinth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Bottomless Cave's labyrinth: a maze one can get through, and the same from every chunk. */
class LabyrinthTest {

    private static final long[] SEEDS = {1L, 42L, 987654321L, -7L, 12345L};

    @Test
    void theSameRegionGivesTheSameLabyrinthAndAnotherDoesNot() {
        Labyrinth a = Labyrinth.of(42L, 1000, -2000), b = Labyrinth.of(42L, 1000, -2000), c = Labyrinth.of(43L, 1000, -2000);
        byte[] ca = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1], cb = ca.clone(), cc = ca.clone();
        int differ = 0;
        for (int x = 1000 - 75; x <= 1000 + 75; x += 3) for (int z = -2000 - 75; z <= -2000 + 75; z += 3) {
            a.column(x, z, ca);
            b.column(x, z, cb);
            c.column(x, z, cc);
            assertArrayEquals(ca, cb);
            if (!java.util.Arrays.equals(ca, cc)) differ++;
        }
        assertTrue(differ > 100, "another seed, another maze: " + differ);
    }

    @Test
    void everyCellCanBeReachedFromTheHubAndThePalaceAndShrineAmongThem() {
        for (long seed : SEEDS) for (int region = 0; region < 6; region++) {
            Labyrinth l = Labyrinth.of(seed, region * 1536 + 500, -region * 1536 - 300);
            boolean[][][] seen = l.reachable();
            int cells = 0;
            for (int k = 0; k < Labyrinth.LEVELS; k++) {
                for (int i = 0; i < Labyrinth.GRID; i++) for (int j = 0; j < Labyrinth.GRID; j++) {
                    if (!l.level(k).in(i, j)) continue;
                    cells++;
                    assertTrue(seen[k][i][j], "seed " + seed + " region " + region + ": level " + k + " cell " + i + "," + j + " cut off");
                }
            }
            assertTrue(cells > 900, "about 300 cells a level: " + cells);
            Labyrinth.Room p = l.palace(), s = l.shrine();
            assertTrue(seen[2][p.i0()][p.j0()] && seen[2][s.i0()][s.j0()], "the palace and the shrine are reached");
            assertFalse(l.trail().isEmpty(), "there is a way from the palace to the shrine");
        }
    }

    @Test
    void itIsALabyrinthNotAnOpenCave() {
        for (long seed : SEEDS) {
            Labyrinth l = Labyrinth.of(seed, 0, 0);
            for (int k = 0; k < Labyrinth.LEVELS; k++) {
                int deadEnds = 0, branches = 0, rooms = 0;
                for (int i = 0; i < Labyrinth.GRID; i++) for (int j = 0; j < Labyrinth.GRID; j++) {
                    var level = l.level(k);
                    if (!level.in(i, j) || level.roomOf(i, j) != null) continue;
                    int ways = 0;
                    for (int d = 0; d < 4; d++) if (level.passage(i, j, d)) ways++;
                    if (ways == 1) deadEnds++;
                    if (ways >= 3) branches++;
                }
                for (var room : l.level(k).rooms()) if (room.kind() < Labyrinth.HUB) rooms++;
                assertTrue(deadEnds >= 25, "seed " + seed + " level " + k + ": dead ends " + deadEnds);
                assertTrue(branches >= 25, "seed " + seed + " level " + k + ": branchings " + branches);
                assertTrue(rooms >= 4, "seed " + seed + " level " + k + ": chambers " + rooms);
            }
            assertEquals(6, l.stairs().size(), "three stairs down from each of the upper two levels");
            // you have to cross a level to go on: no stair under another
            for (var a : l.stairs()) for (var b : l.stairs()) {
                if (a != b) assertTrue(Math.max(Math.abs(a.i() - b.i()), Math.abs(a.j() - b.j())) >= 2, "stairs apart");
            }
        }
    }

    @Test
    void stairsAreEnteredOnceAboveAndOnceBelow() {
        for (long seed : SEEDS) {
            Labyrinth l = Labyrinth.of(seed, 3000, 3000);
            for (var s : l.stairs()) {
                for (int k : new int[]{s.upper(), s.upper() + 1}) {
                    int ways = 0;
                    for (int d = 0; d < 4; d++) if (l.level(k).passage(s.i(), s.j(), d)) ways++;
                    assertEquals(1, ways, "a stair's cell is a dead end on both its levels");
                }
                assertTrue(l.level(s.upper()).passage(s.i(), s.j(), s.dir()), "entered from where it faces");
            }
        }
    }

    @Test
    void theStairClimbsABlockAStepFromTheLowerFloorToTheUpper() {
        Labyrinth l = Labyrinth.of(5L, 0, 0);
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        for (var s : l.stairs()) {
            int lower = Labyrinth.FLOOR[s.upper() + 1], upper = Labyrinth.FLOOR[s.upper()];
            int[] mid = l.cellMiddle(s.i(), s.j());
            java.util.Set<Integer> heights = new java.util.TreeSet<>();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                l.column(mid[0] + dx, mid[1] + dz, col);
                for (int y = lower; y < upper; y++) {
                    byte c = col[y - Labyrinth.Y_MIN];
                    if (c >= Labyrinth.STEP_N && c <= Labyrinth.STEP_W) heights.add(y);
                }
                if (dx == 0 && dz == 0) assertEquals(Labyrinth.CORE, col[lower + 2 - Labyrinth.Y_MIN], "a light in the pillar");
            }
            assertEquals(16, heights.size(), "a step at every height between the floors: " + heights);
            // the top step is on the upper floor, where the stair is entered
            int[] top = {mid[0] + Labyrinth.DIRS[s.dir()][0], mid[1] + Labyrinth.DIRS[s.dir()][1]};
            l.column(top[0], top[1], col);
            byte c = col[upper - 1 - Labyrinth.Y_MIN];
            assertTrue(c >= Labyrinth.STEP_N && c <= Labyrinth.STEP_W, "the top step is the upper floor");
            assertTrue(Labyrinth.open(col[upper - Labyrinth.Y_MIN]), "and open over it");
        }
    }

    @Test
    void theShaftsStairIsWalkedInHalfBlocks() {
        // round the shaft at any height, neighbouring columns of the stair's surface never differ by more than half a block
        for (int y0 : new int[]{40, 77, 133}) {
            double[][] surface = new double[13][13];
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                surface[dx + 6][dz + 6] = Double.NaN;
                for (int y = y0; y < y0 + Labyrinth.SHAFT_PITCH; y++) {
                    byte c = Labyrinth.shaftCode(dx, dz, y);
                    if (c == Labyrinth.HELIX) surface[dx + 6][dz + 6] = y + 1;
                    if (c == Labyrinth.HELIX_SLAB) surface[dx + 6][dz + 6] = y + 0.5;
                }
            }
            int steps = 0;
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                double r = Math.hypot(dx, dz);
                if (r < Labyrinth.VOID_RADIUS + 0.8 || r > Labyrinth.SHAFT_RADIUS - 0.4) continue;
                double s = surface[dx + 6][dz + 6];
                assertFalse(Double.isNaN(s), "a step in every column of the stair: " + dx + "," + dz);
                steps++;
                for (int[] d : Labyrinth.DIRS) {
                    double o = surface[dx + d[0] + 6][dz + d[1] + 6];
                    double ro = Math.hypot(dx + d[0], dz + d[1]);
                    if (Double.isNaN(o) || ro < Labyrinth.VOID_RADIUS + 0.8 || ro > Labyrinth.SHAFT_RADIUS - 0.4) continue;
                    double diff = Math.abs(s - o);
                    diff = Math.min(diff, Math.abs(diff - Labyrinth.SHAFT_PITCH)); // across the turn's seam: the next turn up
                    assertTrue(diff <= 0.5 + 1e-9, "at " + dx + "," + dz + " and its neighbour: " + s + " / " + o);
                }
            }
            assertTrue(steps > 20);
            assertEquals(Labyrinth.AIR, Labyrinth.shaftCode(0, 0, y0), "the middle is open all the way down");
        }
    }

    @Test
    void theTrailRunsFromThePalaceToTheShrine() {
        Labyrinth l = Labyrinth.of(77L, -900, 400);
        List<int[]> trail = l.trail();
        assertEquals(l.palace(), l.level(2).roomOf(trail.get(0)[0], trail.get(0)[1]));
        int[] last = trail.get(trail.size() - 1);
        assertEquals(l.shrine(), l.level(2).roomOf(last[0], last[1]));
        for (int n = 1; n < trail.size(); n++) {
            int[] a = trail.get(n - 1), b = trail.get(n);
            assertEquals(1, Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]), "step by step");
        }
        int[] refuge = l.refuge();
        assertEquals(l.shrine(), l.roomAt(2, refuge[0], refuge[2]), "the refuge is in the shrine");
    }

    /** Draws each level's plan (and a cut through the stairs) into build/labyrinth.txt, to look at. */
    @Test
    void drawsItsPlan() throws java.io.IOException {
        Labyrinth l = Labyrinth.of(42L, 0, 0);
        StringBuilder out = new StringBuilder();
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        for (int k = 0; k < Labyrinth.LEVELS; k++) {
            int y = Labyrinth.FLOOR[k] + 1;
            out.append("level ").append(k).append(" (y ").append(y).append(")\n");
            for (int z = -76; z <= 76; z++) {
                for (int x = -76; x <= 76; x++) {
                    l.column(x, z, col);
                    byte c = col[y - Labyrinth.Y_MIN];
                    var room = l.roomAt(k, x, z);
                    char ch = switch (c) {
                        case Labyrinth.AIR -> room == null ? ' ' : room.kind() == Labyrinth.PALACE ? 'P' : room.kind() == Labyrinth.SHRINE ? 'S' : room.kind() == Labyrinth.HUB ? 'H' : '.';
                        case Labyrinth.ROCK -> '#';
                        case Labyrinth.UNTOUCHED -> '~';
                        case Labyrinth.PILLAR, Labyrinth.CORE -> 'O';
                        case Labyrinth.LANTERN, Labyrinth.SOUL_LANTERN -> '*';
                        default -> c >= Labyrinth.STEP_N && c <= Labyrinth.STEP_W ? '^' : '?';
                    };
                    out.append(ch);
                }
                out.append('\n');
            }
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("build"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("build", "labyrinth.txt"), out);
    }
}
