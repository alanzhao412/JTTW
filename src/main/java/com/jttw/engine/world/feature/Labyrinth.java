package com.jttw.engine.world.feature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bottomless Cave (无底洞) under Pit-Hole Mountain, worked out from the seed, the region's
 * centre and the lie of the land at its door alone, so every chunk of it agrees on it whichever is
 * made first. Pure: no Minecraft in it, so it can be tested on its own.
 *
 * <p>From the top down:
 * <ul>
 *   <li><b>the great door</b> in the mountainside, {@value #DOOR_AT} blocks out from the centre on
 *       one of the four sides, eleven high and nine wide in a frame of black stone and gold, at a
 *       height of its own ({@link #doorY}: never under {@value #DOOR_MIN}, higher where the
 *       mountain is);</li>
 *   <li><b>the gate hall</b> behind it, running in to the centre, and there <b>the great shaft</b>,
 *       a void with a spiral stair round it, down to the first floor's hub;</li>
 *   <li><b>eight floors</b>, one under another ({@link #FLOOR}), each a maze that grows wider and
 *       harder the deeper it lies ({@link #REACH}), on a grid of cells {@value #PITCH} blocks apart
 *       (a 3-block tunnel, 4 of rock between). Each has two storeys: tunnels at its floor and
 *       galleries five blocks over them, joined by ramps (both ways) and by holes and balconies
 *       one drops through (one way only). A floor is a spanning tree of its cells (Kruskal's: it
 *       branches and dead-ends often), a few loops opened in it, halls of its own kind merged
 *       from several cells, lit here and there or not at all. The way down is a spiral stair in
 *       the cell farthest, by the walk, from where one came in, and its guardian stands before
 *       it; each floor's stair comes out in the next floor's maze;</li>
 *   <li><b>the throne level</b>, at the bottom ({@value #THRONE}): her palace, a hall
 *       thirty-one blocks square and {@value #PALACE_HEIGHT} high, reached from the last stair by
 *       a processional way, and behind it, by a narrow passage, her shrine, where she flees.</li>
 * </ul>
 * Each floor's heights are its own, rock where nothing is open, and so are the stairs' and the
 * shaft's, so no vanilla cave, water or lava breaks in (and the rock is the cave's own, which no
 * one digs through: see {@code BottomlessCaveFeature}).
 */
public final class Labyrinth {

    // ── The plan ────────────────────────────────────────────────────────

    /** Blocks from one cell's middle to the next's; each cell has a 3-block room in its middle. */
    public static final int PITCH = 7;
    /** Cells either side of the centre's; the grid's side. */
    public static final int N = 14, SIZE = 2 * N + 1;
    /** How many floors of maze. */
    public static final int FLOORS = 8;
    /** The height one walks at on each floor's lower storey, top first. */
    public static final int[] FLOOR = {98, 81, 64, 47, 30, 13, -4, -21};
    /** The cells of each floor lie within this of the centre: each wider than the last. */
    public static final int[] REACH = {42, 49, 56, 63, 70, 77, 84, 91};
    /** The upper storey is walked this far over the lower; a tunnel's clear height; a hall's (both storeys). */
    public static final int STOREY = 5, TUNNEL = 4, HALL_HEIGHT = 9;
    /** What of each floor's heights it makes its own, from its floor: rock but where it is open. */
    public static final int SLAB_BELOW = 2, SLAB_ABOVE = 10;
    /** The throne level: its floor, her palace's height, the palace's cells either side of the centre's, her shrine's height. */
    public static final int THRONE = -50, PALACE_HEIGHT = 20, PALACE_CELLS = 2, SHRINE_HEIGHT = 6;
    /** How far (in cells, either way) the throne level's ground reaches; where the last stair may come down (from, to). */
    public static final int THRONE_REACH = 7, LAST_STAIR_FROM = 4, LAST_STAIR_TO = 5;
    /** The levels (0-7 the floors, 8 the throne level). */
    public static final int LEVELS = FLOORS + 1;
    /** The heights the cave's columns are worked out over. */
    public static final int Y_MIN = THRONE - SLAB_BELOW, Y_MAX = 216;
    /** The great door: how far out, its least and greatest height, its half-width and height (the leaves). */
    public static final int DOOR_AT = 56, DOOR_MIN = 150, DOOR_MAX = 200, DOOR_HALF = 4, DOOR_HIGH = 11;
    /** The gate hall behind it: its half-width and height. */
    public static final int HALL_HALF = 5, HALL_HIGH = 13;
    /** The share of a floor's remaining walls opened, so it has loops: a little more the deeper. */
    public static final double LOOPS = 0.05;
    /** The share of tunnel cells with a lantern in them, by floor: the sixth is the dark warren, unlit. */
    public static final double[] LIGHTS = {0.13, 0.12, 0.08, 0.05, 0.08, 0.0, 0.1, 0.14};
    /** The hub at the first floor's centre, its radius. */
    public static final double HUB_RADIUS = 9.5;
    /** The great shaft: its open middle, its stair's outer edge, the rock kept round it. */
    public static final double VOID_RADIUS = 3.2, SHAFT_RADIUS = 6.6, LINING_RADIUS = 8.2;
    /** How near the shaft's mouth another zone's terrain keeps clear (kept for Lion-Camel Ridge). */
    public static final double MOUTH_RADIUS = 2.6, DISH_RADIUS = 8.5;
    /** How far the great shaft's stair climbs in a turn. */
    public static final int SHAFT_PITCH = 10;

    // what a block is
    public static final byte UNTOUCHED = 0, ROCK = 1, AIR = 2, STEP_N = 3, STEP_E = 4, STEP_S = 5, STEP_W = 6,
            PILLAR = 7, CORE = 8, WATER = 9, LANTERN = 10, SOUL_LANTERN = 11, COVER = 12,
            HELIX = 13, HELIX_SLAB = 14, HELIX_LANTERN = 15, GATE = 16, FRAME = 17, GILT = 18;

    /** Whether a block of this kind is open (something walks or stands in it), not rock: its neighbours face it. */
    public static boolean open(byte code) {
        return code != UNTOUCHED && code != ROCK && code != PILLAR && code != CORE && code != COVER
                && code != GATE && code != FRAME && code != GILT;
    }

    /** Whether one can stand in a block of this kind (it is no obstacle). */
    public static boolean passable(byte code, boolean doorOpen) {
        return code == AIR || code == WATER || code == LANTERN || code == SOUL_LANTERN || code == HELIX_LANTERN
                || (doorOpen && code == GATE);
    }

    /** Whether a block of this kind bears one up. */
    public static boolean bears(byte code) {
        return code != AIR && code != LANTERN && code != SOUL_LANTERN && code != HELIX_LANTERN && code != GATE;
    }

    /** Whether a step code. */
    public static boolean step(byte code) {
        return code >= STEP_N && code <= STEP_W;
    }

    /** The directions, as {dx, dz}: north, east, south, west (as the step codes). */
    public static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /** A hall's kind. */
    public static final int NEST = 0, LARDER = 1, BONES = 2, GROTTO = 3, GRANARY = 4, FUNGUS = 5, RIVER = 6, VAULT = 7, BRIDAL = 8,
            HUB = 9, PALACE = 10, SHRINE = 11;
    /** The kinds of hall each floor has, by floor: burrows, granaries, bones, fungus, the river, the dark warren, her storerooms, her bridal halls. */
    public static final int[][] HALL_KINDS = {{NEST, LARDER}, {GRANARY, LARDER}, {BONES, NEST}, {FUNGUS, GROTTO},
            {RIVER, GROTTO}, {BONES, NEST}, {VAULT, GRANARY}, {BRIDAL, GROTTO}};
    /** The floors' names, as the player is told them. */
    public static final String[] FLOOR_NAMES = {"the Burrows", "the Granaries", "the Bone Warrens", "the Fungus Caverns",
            "the Underground River", "the Dark Warren", "the Storerooms", "the Bridal Halls"};

    /** A hall: cells i0..i1, j0..j1 of a level merged, square or rounded, so tall. */
    public record Room(int level, int i0, int j0, int i1, int j1, int kind, boolean round, int height) {
        public boolean holds(int i, int j) {
            return i >= i0 && i <= i1 && j >= j0 && j <= j1;
        }
    }

    /**
     * A spiral stair in cell (i, j), from level {@code upper}'s floor down to the next level's
     * ({@code lowerY}); entered on its upper level from {@code dir} (its top step on that side).
     */
    public record Stair(int i, int j, int upper, int dir, int upperY, int lowerY) {}

    /** A place where a group of the household is put when the land is made: a level (0-7 a floor, 8 the throne level), a cell, what stands there. */
    public record Post(int level, int i, int j, boolean high, int kind, int count, boolean captain) {}

    /** What stands at a post. */
    public static final int MICE = 0, MAIDS = 1, MIXED = 2, GUARDIAN = 3;

    /** One level's maze. */
    public static final class Level {
        public final int k, y;
        final boolean[][] low = new boolean[SIZE][SIZE], high = new boolean[SIZE][SIZE];
        /** Passages, per storey: between (i, j) and (i+1, j); between (i, j) and (i, j+1). */
        final boolean[][] east0 = new boolean[SIZE][SIZE], south0 = new boolean[SIZE][SIZE];
        final boolean[][] east1 = new boolean[SIZE][SIZE], south1 = new boolean[SIZE][SIZE];
        final Room[][] room = new Room[SIZE][SIZE];
        final List<Room> rooms = new ArrayList<>();
        /** A ramp up out of this (lower) cell to the next cell's upper storey, its way; -1 none. */
        final int[][] ramp = new int[SIZE][SIZE];
        /** An upper cell with a hole in its floor down to the cell under it. */
        final boolean[][] drop = new boolean[SIZE][SIZE];
        final boolean[][] lit = new boolean[SIZE][SIZE], litHigh = new boolean[SIZE][SIZE], soul = new boolean[SIZE][SIZE];
        final boolean[][] flooded = new boolean[SIZE][SIZE];
        /** The cells whose columns its heights are owned in (its cells and a ring of rock round them). */
        final boolean[][] owned = new boolean[SIZE][SIZE];
        /** Where one comes in (a stair's foot, or the hub), and where the way down is, and the cell its guardian holds. */
        int ai, aj, xi = -1, xj = -1, gi = -1, gj = -1;
        /** The farthest any cell is, by the walk, from where one comes in. */
        int depth;

        Level(int k, int y) {
            this.k = k;
            this.y = y;
            for (int[] row : ramp) java.util.Arrays.fill(row, -1);
        }

        static boolean inGrid(int i, int j) {
            return i >= 0 && j >= 0 && i < SIZE && j < SIZE;
        }

        public boolean low(int i, int j) {
            return inGrid(i, j) && low[i][j];
        }

        public boolean high(int i, int j) {
            return inGrid(i, j) && high[i][j];
        }

        /** Whether one can go from (i, j) the way {@code dir} to the next cell on a storey (0 lower, 1 upper). */
        public boolean passage(int i, int j, int dir, int storey) {
            int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
            if (!inGrid(i, j) || !inGrid(ni, nj)) return false;
            boolean[][] e = storey == 0 ? east0 : east1, s = storey == 0 ? south0 : south1;
            return switch (dir) {
                case 0 -> s[i][nj];
                case 1 -> e[i][j];
                case 2 -> s[i][j];
                default -> e[ni][j];
            };
        }

        public boolean passage(int i, int j, int dir) {
            return passage(i, j, dir, 0);
        }

        void open(int i, int j, int dir, int storey) {
            int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
            boolean[][] e = storey == 0 ? east0 : east1, s = storey == 0 ? south0 : south1;
            switch (dir) {
                case 0 -> s[i][nj] = true;
                case 1 -> e[i][j] = true;
                case 2 -> s[i][j] = true;
                default -> e[ni][j] = true;
            }
        }

        /** Whether a lantern hangs in cell (i, j)'s lower tunnel. */
        public boolean lit(int i, int j) {
            return inGrid(i, j) && lit[i][j];
        }

        public List<Room> rooms() {
            return rooms;
        }

        public Room roomOf(int i, int j) {
            return inGrid(i, j) ? room[i][j] : null;
        }

        public int ramp(int i, int j) {
            return inGrid(i, j) ? ramp[i][j] : -1;
        }

        public boolean drop(int i, int j) {
            return inGrid(i, j) && drop[i][j];
        }

        public boolean flooded(int i, int j) {
            return inGrid(i, j) && flooded[i][j];
        }

        /** Where one comes in, as {i, j}. */
        public int[] arrival() {
            return new int[]{ai, aj};
        }

        /** The cell of the way down, as {i, j}; for the throne level, none (null). */
        public int[] exit() {
            return xi < 0 ? null : new int[]{xi, xj};
        }

        /** The cell its guardian holds, before the way down; null for the throne level. */
        public int[] guard() {
            return gi < 0 ? null : new int[]{gi, gj};
        }

        /** How many cells of each storey it has. */
        public int cells(int storey) {
            int n = 0;
            for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) if (storey == 0 ? low[i][j] : high[i][j]) n++;
            return n;
        }

        /** The walk's length from where one comes in to the way down, in cells. */
        public int depth() {
            return depth;
        }
    }

    public final long seed;
    public final int cx, cz;
    /** The great door's way out from the centre (north, east, south, west), and the height one walks in at. */
    public final int doorDir, doorY;
    private final Level[] levels = new Level[LEVELS];
    private final List<Stair> stairs = new ArrayList<>();
    private final List<Post> posts = new ArrayList<>();
    private Room palace, shrine, hub;
    private int shrineGateDir;
    /** The palace's front gate (toward the last stair) and the antechamber cell before it. */
    private int frontDir, anteI, anteJ;
    /** The way along the throne level from the palace's middle to her shrine, as cells. */
    private final List<int[]> trail = new ArrayList<>();

    /** The land's height at (x, z) before the mountain is raised: for the door's height. */
    @FunctionalInterface
    public interface Land {
        int at(int x, int z);

        /** Flat land at a height, for tests and the gallery. */
        static Land flat(int y) {
            return (x, z) -> y;
        }
    }

    private static final Map<Long, Labyrinth> CACHE = new ConcurrentHashMap<>();

    /** The cave under the region centred at (cx, cz) in a world of this seed, its door set by the {@code land} there. */
    public static Labyrinth of(long seed, int cx, int cz, Land land) {
        long key = mix(seed, cx, cz);
        Labyrinth l = CACHE.get(key);
        if (l != null && l.seed == seed && l.cx == cx && l.cz == cz) return l;
        l = new Labyrinth(seed, cx, cz, land);
        if (CACHE.size() > 32) CACHE.clear();
        CACHE.put(key, l);
        return l;
    }

    /** The cave as {@link #of(long, int, int, Land)} would make it, its door at its least height (for tests). */
    public static Labyrinth of(long seed, int cx, int cz) {
        return of(seed, cx, cz, Land.flat(64));
    }

    /** The great door's way out from the centre, by the seed and the centre alone (as the Earth God knows it). */
    public static int doorDirOf(long seed, int cx, int cz) {
        return (int) Math.floorMod(mix(seed ^ 0xD00AL, cx, cz), 4L);
    }

    /** The great door's middle, on the threshold, as {x, z}, by the seed and the centre alone. */
    public static int[] doorOf(long seed, int cx, int cz) {
        int d = doorDirOf(seed, cx, cz);
        return new int[]{cx + DIRS[d][0] * DOOR_AT, cz + DIRS[d][1] * DOOR_AT};
    }

    private Labyrinth(long seed, int cx, int cz, Land land) {
        this.seed = seed;
        this.cx = cx;
        this.cz = cz;
        this.doorDir = doorDirOf(seed, cx, cz);
        int fx = cx + DIRS[doorDir][0] * (DOOR_AT + 6), fz = cz + DIRS[doorDir][1] * (DOOR_AT + 6);
        this.doorY = Math.max(DOOR_MIN, Math.min(DOOR_MAX, land.at(fx, fz) + 1));
        Random r = new Random(mix(seed, cx, cz));
        int ai = N, aj = N;
        for (int k = 0; k < FLOORS; k++) {
            Level l = plan(r, k, ai, aj);
            levels[k] = l;
            ai = l.xi;
            aj = l.xj;
        }
        levels[FLOORS] = throne(r, ai, aj);
        for (int k = 0; k < FLOORS; k++) {
            Level l = levels[k];
            stairs.add(new Stair(l.xi, l.xj, k, wayTo(l.xi, l.xj, l.gi, l.gj), l.y, levels[k + 1].y));
        }
        posts(r);
    }

    private static int wayTo(int i, int j, int ni, int nj) {
        for (int d = 0; d < 4; d++) if (i + DIRS[d][0] == ni && j + DIRS[d][1] == nj) return d;
        return 0;
    }

    private static double out(int i, int j) {
        return Math.hypot(i - N, j - N) * PITCH;
    }

    // ── A floor ─────────────────────────────────────────────────────────

    /** Floor k: its cells, its halls, its galleries and ramps, its maze, its drops, its way down. */
    private Level plan(Random r, int k, int ai, int aj) {
        Level l = new Level(k, FLOOR[k]);
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) l.low[i][j] = out(i, j) <= REACH[k];
        l.ai = ai;
        l.aj = aj;
        boolean[][] reserved = new boolean[SIZE][SIZE];
        if (k == 0) {
            hub = addRoom(l, N - 1, N - 1, N + 1, N + 1, HUB, true, HALL_HEIGHT);
        } else {
            reserved[ai][aj] = true;
        }
        // its halls, of its kinds: more the deeper
        int halls = 3 + k / 2;
        for (int attempt = 0, placed = 0; attempt < 400 && placed < halls; attempt++) {
            int w = 2 + (r.nextDouble() < 0.35 ? 1 : 0), h = 2 + (r.nextDouble() < 0.35 ? 1 : 0);
            int i0 = 1 + r.nextInt(SIZE - w - 1), j0 = 1 + r.nextInt(SIZE - h - 1);
            boolean ok = true;
            for (int i = i0 - 1; i <= i0 + w && ok; i++) for (int j = j0 - 1; j <= j0 + h && ok; j++) {
                ok = l.low(i, j) && l.room[i][j] == null && !reserved[i][j] && !near(reserved, i, j);
            }
            if (!ok) continue;
            int[] kinds = HALL_KINDS[k];
            addRoom(l, i0, j0, i0 + w - 1, j0 + h - 1, kinds[placed % kinds.length], false, HALL_HEIGHT);
            placed++;
        }
        // its galleries: blobs of upper cells over the tunnels
        int blobs = 3 + k / 2, grow = 10 + 2 * k;
        for (int b = 0; b < blobs; b++) {
            for (int attempt = 0; attempt < 60; attempt++) {
                int i = r.nextInt(SIZE), j = r.nextInt(SIZE);
                if (!galleryOk(l, reserved, i, j)) continue;
                List<int[]> blob = new ArrayList<>();
                blob.add(new int[]{i, j});
                l.high[i][j] = true;
                for (int n = 0; n < grow * 4 && blob.size() < grow; n++) {
                    int[] c = blob.get(r.nextInt(blob.size()));
                    int d = r.nextInt(4), ni = c[0] + DIRS[d][0], nj = c[1] + DIRS[d][1];
                    if (!galleryOk(l, reserved, ni, nj) || l.high[ni][nj]) continue;
                    l.high[ni][nj] = true;
                    blob.add(new int[]{ni, nj});
                }
                break;
            }
        }
        // the ramps up into them: a gallery cell beside a tunnel cell becomes the ramp's head, upper only
        boolean[][] seenHigh = new boolean[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            if (!l.high[i][j] || seenHigh[i][j]) continue;
            List<int[]> comp = component(l, i, j, seenHigh);
            Collections.shuffle(comp, r);
            int ramps = 0, want = comp.size() > 12 ? 2 : 1;
            for (int[] c : comp) {
                if (ramps >= want) break;
                if (!l.low[c[0]][c[1]] || l.ramp[c[0]][c[1]] >= 0) continue;
                for (int d0 = 0, d = r.nextInt(4); d0 < 4; d0++, d = (d + 1) % 4) {
                    int ni = c[0] - DIRS[d][0], nj = c[1] - DIRS[d][1]; // the ramp's foot, from which it climbs the way d
                    if (!l.low(ni, nj) || l.high[ni][nj] || l.room[ni][nj] != null || reserved[ni][nj] || l.ramp[ni][nj] >= 0
                            || rampHead(l, ni, nj) || (ni == ai && nj == aj)) continue;
                    l.ramp[ni][nj] = d;
                    l.low[c[0]][c[1]] = false;
                    ramps++;
                    break;
                }
            }
            if (ramps == 0) for (int[] c : comp) l.high[c[0]][c[1]] = false; // no way up: no gallery
        }
        carve(r, l, reserved);
        // drops: holes in a gallery's floor, down into the tunnel under it (one way)
        int drops = 2 + k / 2;
        for (int attempt = 0, made = 0; attempt < 200 && made < drops; attempt++) {
            int i = r.nextInt(SIZE), j = r.nextInt(SIZE);
            if (!l.high[i][j] || !l.low[i][j] || l.drop[i][j] || l.room[i][j] != null) continue;
            l.drop[i][j] = true;
            made++;
        }
        // balconies: a gallery's passage out over a hall, to drop into it (one way)
        for (int attempt = 0, made = 0; attempt < 200 && made < 1 + k / 3; attempt++) {
            int i = r.nextInt(SIZE), j = r.nextInt(SIZE), d = r.nextInt(4);
            int ni = i + DIRS[d][0], nj = j + DIRS[d][1];
            if (!l.high(i, j) || !Level.inGrid(ni, nj) || l.room[ni][nj] == null || l.room[ni][nj].kind() >= HUB) continue;
            if (l.ramp[i][j] >= 0 || rampHead(l, i, j)) continue;
            l.open(i, j, d, 1);
            made++;
        }
        // the river: a run of flooded tunnels across the fifth floor
        if (HALL_KINDS[k][0] == RIVER) river(r, l, reserved);
        // the way down: the cell farthest by the walk from where one comes in, a dead end if it can be
        int[][] dist = walkDistances(l);
        int best = -1;
        List<int[]> ties = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            if (!exitOk(l, k, i, j, reserved)) continue;
            int d = dist[i][j] * 4 + (degree(l, i, j) == 1 ? 3 : 0);
            if (d > best) {
                best = d;
                ties.clear();
            }
            if (d == best) ties.add(new int[]{i, j});
        }
        if (ties.isEmpty()) throw new IllegalStateException("no way down from floor " + k);
        int[] x = ties.get(r.nextInt(ties.size()));
        l.xi = x[0];
        l.xj = x[1];
        l.depth = dist[x[0]][x[1]];
        // its guardian holds the cell before it, the way one comes
        int bestD = Integer.MAX_VALUE;
        for (int d = 0; d < 4; d++) {
            if (!l.passage(x[0], x[1], d, 0)) continue;
            int ni = x[0] + DIRS[d][0], nj = x[1] + DIRS[d][1];
            if (dist[ni][nj] >= 0 && dist[ni][nj] < bestD) {
                bestD = dist[ni][nj];
                l.gi = ni;
                l.gj = nj;
            }
        }
        // lanterns, here and there (none in the dark warren)
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            if (l.low[i][j] && l.room[i][j] == null && r.nextDouble() < LIGHTS[k]) {
                l.lit[i][j] = true;
                l.soul[i][j] = r.nextDouble() < 0.3;
            }
            if (l.high[i][j] && r.nextDouble() < LIGHTS[k] * 0.6) l.litHigh[i][j] = true;
        }
        own(l);
        return l;
    }

    private static boolean near(boolean[][] reserved, int i, int j) {
        for (int a = i - 1; a <= i + 1; a++) for (int b = j - 1; b <= j + 1; b++) if (Level.inGrid(a, b) && reserved[a][b]) return true;
        return false;
    }

    private static boolean galleryOk(Level l, boolean[][] reserved, int i, int j) {
        if (!l.low(i, j) || l.room[i][j] != null || reserved[i][j]) return false;
        if (i == l.ai && j == l.aj) return false;
        // never over a hall's wall-side cells either, so its height is its own
        for (int[] d : DIRS) {
            Room o = l.roomOf(i + d[0], j + d[1]);
            if (o != null && o.kind() >= HUB) return false;
        }
        return true;
    }

    /** Whether (i, j) is the head of a ramp from a neighbour. */
    private static boolean rampHead(Level l, int i, int j) {
        for (int d = 0; d < 4; d++) {
            int fi = i - DIRS[d][0], fj = j - DIRS[d][1];
            if (Level.inGrid(fi, fj) && l.ramp[fi][fj] == d) return true;
        }
        return false;
    }

    private static List<int[]> component(Level l, int i, int j, boolean[][] seen) {
        List<int[]> out = new ArrayList<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{i, j});
        seen[i][j] = true;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            out.add(c);
            for (int[] d : DIRS) {
                int ni = c[0] + d[0], nj = c[1] + d[1];
                if (l.high(ni, nj) && !seen[ni][nj]) {
                    seen[ni][nj] = true;
                    q.add(new int[]{ni, nj});
                }
            }
        }
        return out;
    }

    private Room addRoom(Level l, int i0, int j0, int i1, int j1, int kind, boolean round, int height) {
        Room room = new Room(l.k, i0, j0, i1, j1, kind, round, height);
        l.rooms.add(room);
        for (int i = i0; i <= i1; i++) for (int j = j0; j <= j1; j++) {
            l.room[i][j] = room;
            l.low[i][j] = true;
            l.high[i][j] = false;
            if (i < i1) l.east0[i][j] = true;
            if (j < j1) l.south0[i][j] = true;
        }
        return room;
    }

    /**
     * The floor's maze: Kruskal's spanning tree over both storeys and the ramps between them (a
     * room as one place; where one comes in, a dead end), then a few loops. Every cell is reached
     * both ways by it.
     */
    private void carve(Random r, Level l, boolean[][] reserved) {
        int cells = SIZE * SIZE;
        int[] parent = new int[2 * cells];
        for (int n = 0; n < parent.length; n++) parent[n] = n;
        // rooms are one place already
        for (Room room : l.rooms) for (int i = room.i0(); i <= room.i1(); i++) for (int j = room.j0(); j <= room.j1(); j++) {
            union(parent, i * SIZE + j, room.i0() * SIZE + room.j0());
        }
        // the ramps join their foot's lower storey to their head's upper
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            int d = l.ramp[i][j];
            if (d < 0) continue;
            union(parent, i * SIZE + j, cells + (i + DIRS[d][0]) * SIZE + j + DIRS[d][1]);
        }
        List<int[]> edges = new ArrayList<>();
        for (int s = 0; s < 2; s++) for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            if (s == 0 ? !l.low[i][j] : !l.high[i][j]) continue;
            for (int dir = 1; dir <= 2; dir++) {
                int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
                if (s == 0 ? !l.low(ni, nj) : !l.high(ni, nj)) continue;
                if (s == 0) {
                    Room a = l.room[i][j], b = l.room[ni][nj];
                    if (a != null && a == b) continue;
                    if (a != null && a.kind() == HUB && !hubGate(a, i, j, dir)) continue;
                    if (b != null && b.kind() == HUB && !hubGate(b, ni, nj, (dir + 2) % 4)) continue;
                    // a ramp's way is its own
                    if (l.ramp[i][j] == dir || l.ramp[ni][nj] == (dir + 2) % 4) continue;
                }
                edges.add(new int[]{s, i, j, dir});
            }
        }
        Collections.shuffle(edges, r);
        List<int[]> closed = new ArrayList<>(), leaves = new ArrayList<>();
        for (int[] e : edges) {
            int s = e[0], base = s * cells;
            int a = base + e[1] * SIZE + e[2], b = base + (e[1] + DIRS[e[3]][0]) * SIZE + e[2] + DIRS[e[3]][1];
            boolean leafA = s == 0 && reserved[e[1]][e[2]], leafB = s == 0 && reserved[e[1] + DIRS[e[3]][0]][e[2] + DIRS[e[3]][1]];
            if (leafA || leafB) {
                if (!(leafA && leafB)) leaves.add(e);
                continue;
            }
            if (find(parent, a) == find(parent, b)) {
                closed.add(e);
                continue;
            }
            union(parent, a, b);
            l.open(e[1], e[2], e[3], s);
        }
        // where one comes in is a dead end: its one way leads into the greatest part of the maze, not a pocket
        int[] size = new int[2 * cells];
        for (int s = 0; s < 2; s++) for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            if ((s == 0 ? l.low : l.high)[i][j]) size[find(parent, s * cells + i * SIZE + j)]++;
        }
        int[] bestLeaf = null;
        for (int[] e : leaves) {
            int ni = e[1] + DIRS[e[3]][0], nj = e[2] + DIRS[e[3]][1];
            int other = reserved[e[1]][e[2]] ? ni * SIZE + nj : e[1] * SIZE + e[2];
            if (bestLeaf == null || size[find(parent, other)] > size[find(parent, bestLeaf[4])]) bestLeaf = new int[]{e[0], e[1], e[2], e[3], other};
        }
        if (bestLeaf != null) {
            union(parent, bestLeaf[1] * SIZE + bestLeaf[2], (bestLeaf[1] + DIRS[bestLeaf[3]][0]) * SIZE + bestLeaf[2] + DIRS[bestLeaf[3]][1]);
            l.open(bestLeaf[1], bestLeaf[2], bestLeaf[3], 0);
        }
        double loops = LOOPS + 0.01 * l.k;
        for (int[] e : closed) if (r.nextDouble() < loops) l.open(e[1], e[2], e[3], e[0]);
        // anything left unjoined (cut off by the ramps' heads) is filled in
        int root = find(parent, l.ai * SIZE + l.aj);
        for (int s = 0; s < 2; s++) for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            boolean[][] here = s == 0 ? l.low : l.high;
            if (here[i][j] && find(parent, s * cells + i * SIZE + j) != root) {
                here[i][j] = false;
                if (s == 0) l.ramp[i][j] = -1;
                for (int d = 0; d < 4; d++) {
                    int ni = i + DIRS[d][0], nj = j + DIRS[d][1];
                    if (!Level.inGrid(ni, nj)) continue;
                    if (l.passage(i, j, d, s)) close(l, i, j, d, s);
                }
            }
        }
    }

    private static int find(int[] parent, int n) {
        while (parent[n] != n) n = parent[n] = parent[parent[n]];
        return n;
    }

    private static void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }

    private static void close(Level l, int i, int j, int dir, int storey) {
        int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
        boolean[][] e = storey == 0 ? l.east0 : l.east1, s = storey == 0 ? l.south0 : l.south1;
        switch (dir) {
            case 0 -> s[i][nj] = false;
            case 1 -> e[i][j] = false;
            case 2 -> s[i][j] = false;
            default -> e[ni][j] = false;
        }
    }

    /** The hub opens only at the middle of each side. */
    private static boolean hubGate(Room hub, int i, int j, int dir) {
        int mi = (hub.i0() + hub.i1()) / 2, mj = (hub.j0() + hub.j1()) / 2;
        return switch (dir) {
            case 0 -> j == hub.j0() && i == mi;
            case 2 -> j == hub.j1() && i == mi;
            case 1 -> i == hub.i1() && j == mj;
            default -> i == hub.i0() && j == mj;
        };
    }

    /** The fifth floor's river: a winding run of flooded tunnel cells from rim to rim. */
    private static void river(Random r, Level l, boolean[][] reserved) {
        double a = r.nextDouble() * Math.PI * 2;
        int ci = N + (int) Math.round(Math.cos(a) * 8), cj = N + (int) Math.round(Math.sin(a) * 8);
        for (int step = 0; step < 60; step++) {
            if (l.low(ci, cj) && l.room[ci][cj] == null && !reserved[ci][cj] && l.ramp[ci][cj] < 0) l.flooded[ci][cj] = true;
            // wander on, mostly away from where it began
            int d = r.nextInt(4);
            int ni = ci + DIRS[d][0], nj = cj + DIRS[d][1];
            if (!l.low(ni, nj)) continue;
            if (Math.hypot(ni - N + Math.cos(a) * 8, nj - N + Math.sin(a) * 8) < Math.hypot(ci - N + Math.cos(a) * 8, cj - N + Math.sin(a) * 8)
                    && r.nextDouble() < 0.6) continue;
            ci = ni;
            cj = nj;
        }
    }

    private boolean exitOk(Level l, int k, int i, int j, boolean[][] reserved) {
        if (!l.low[i][j] || l.high[i][j] || l.room[i][j] != null || reserved[i][j] || l.ramp[i][j] >= 0 || rampHead(l, i, j)
                || l.flooded[i][j]) return false;
        if (i == l.ai && j == l.aj) return false;
        if (degree(l, i, j) == 0) return false;
        // nor anywhere near where one comes in: four cells off at the least, as the crow flies
        if (Math.hypot(i - l.ai, j - l.aj) < 4) return false;
        if (k == FLOORS - 1) {
            int cheb = Math.max(Math.abs(i - N), Math.abs(j - N));
            return cheb >= LAST_STAIR_FROM && cheb <= LAST_STAIR_TO;
        }
        // well within the next floor, and clear of its hub-side cells
        return out(i, j) <= REACH[k + 1] - PITCH && Math.max(Math.abs(i - N), Math.abs(j - N)) >= 2;
    }

    private static int degree(Level l, int i, int j) {
        int n = 0;
        for (int d = 0; d < 4; d++) if (l.passage(i, j, d, 0)) n++;
        return n;
    }

    /** How far each lower cell is, by the walk (both storeys, ramps, drops and balconies), from where one comes in; -1 out of reach. */
    static int[][] walkDistances(Level l) {
        int cells = SIZE * SIZE;
        int[] dist = new int[2 * cells];
        java.util.Arrays.fill(dist, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        int start = l.ai * SIZE + l.aj;
        dist[start] = 0;
        q.add(start);
        while (!q.isEmpty()) {
            int n = q.poll(), s = n / cells, i = (n % cells) / SIZE, j = n % SIZE;
            List<Integer> next = new ArrayList<>(6);
            for (int d = 0; d < 4; d++) {
                if (!l.passage(i, j, d, s)) continue;
                int ni = i + DIRS[d][0], nj = j + DIRS[d][1];
                if (s == 1 && !l.high[ni][nj]) next.add(ni * SIZE + nj); // a balcony: down into the hall
                else next.add(s * cells + ni * SIZE + nj);
            }
            if (s == 0 && l.ramp[i][j] >= 0) next.add(cells + (i + DIRS[l.ramp[i][j]][0]) * SIZE + j + DIRS[l.ramp[i][j]][1]);
            if (s == 1) {
                for (int d = 0; d < 4; d++) {
                    int fi = i - DIRS[d][0], fj = j - DIRS[d][1];
                    if (Level.inGrid(fi, fj) && l.ramp[fi][fj] == d) next.add(fi * SIZE + fj);
                }
                if (l.drop[i][j]) next.add(i * SIZE + j);
            }
            for (int m : next) {
                if (dist[m] >= 0) continue;
                dist[m] = dist[n] + 1;
                q.add(m);
            }
        }
        int[][] out = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) out[i][j] = dist[i * SIZE + j];
        return out;
    }

    /** The columns a level's heights are owned in: its cells and every cell beside one (a shell of rock round it). */
    private static void own(Level l) {
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            boolean any = false;
            for (int a = i - 1; a <= i + 1 && !any; a++) for (int b = j - 1; b <= j + 1 && !any; b++) {
                any = l.low(a, b) || l.high(a, b) || (Level.inGrid(a, b) && l.ramp[a][b] >= 0);
            }
            l.owned[i][j] = any;
        }
    }

    // ── The throne level ────────────────────────────────────────────────

    /**
     * The bottom: her palace at the centre, the last stair's foot ({@code ai, aj}) out toward the
     * edge, the processional way from it round to the palace's nearer gate, and behind the palace,
     * by its other gate and a narrow, turning passage, her shrine.
     */
    private Level throne(Random r, int ai, int aj) {
        Level l = new Level(FLOORS, THRONE);
        l.ai = ai;
        l.aj = aj;
        l.low[ai][aj] = true;
        palace = addRoom(l, N - PALACE_CELLS, N - PALACE_CELLS, N + PALACE_CELLS, N + PALACE_CELLS, PALACE, false, PALACE_HEIGHT);
        // the processional way: the shortest way round from the stair's foot to a gate cell
        int g = PALACE_CELLS + 1;
        int[][] gates = {{N, N - g}, {N + g, N}, {N, N + g}, {N - g, N}};
        int[][] from = new int[SIZE * SIZE][];
        boolean[] seen = new boolean[SIZE * SIZE];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ai, aj});
        seen[ai * SIZE + aj] = true;
        int[] end = null;
        while (!q.isEmpty() && end == null) {
            int[] c = q.poll();
            for (int d = 0; d < 4 && end == null; d++) {
                int ni = c[0] + DIRS[d][0], nj = c[1] + DIRS[d][1];
                if (!Level.inGrid(ni, nj) || seen[ni * SIZE + nj] || l.room[ni][nj] != null) continue;
                if (Math.max(Math.abs(ni - N), Math.abs(nj - N)) > LAST_STAIR_TO) continue;
                seen[ni * SIZE + nj] = true;
                from[ni * SIZE + nj] = c;
                for (int[] gc : gates) if (gc[0] == ni && gc[1] == nj) end = new int[]{ni, nj};
                q.add(new int[]{ni, nj});
            }
        }
        if (end == null) throw new IllegalStateException("no way to the palace");
        for (int[] c = end; ; c = from[c[0] * SIZE + c[1]]) {
            int[] prev = from[c[0] * SIZE + c[1]];
            l.low[c[0]][c[1]] = true;
            if (prev == null) break;
            l.open(prev[0], prev[1], wayTo(prev[0], prev[1], c[0], c[1]), 0);
        }
        for (int d = 0; d < 4; d++) if (gates[d][0] == end[0] && gates[d][1] == end[1]) frontDir = d;
        anteI = end[0];
        anteJ = end[1];
        // the front gate, into the palace
        l.open(end[0], end[1], (frontDir + 2) % 4, 0);
        // the back gate, and the way to her shrine, behind
        int back = (frontDir + 2) % 4;
        int[] bg = gates[back];
        l.low[bg[0]][bg[1]] = true;
        l.open(bg[0], bg[1], (back + 2) % 4, 0);
        int[] fwd = DIRS[back], side = DIRS[(back + 1) % 4];
        // one cell on, then aside two or three, then on into the shrine (two by two cells)
        int[] c1 = {bg[0] + fwd[0], bg[1] + fwd[1]};
        int sideways = (r.nextBoolean() ? 1 : -1) * (2 + r.nextInt(2));
        List<int[]> way = new ArrayList<>();
        way.add(bg);
        way.add(c1);
        int[] c = c1;
        for (int s = 0; s < Math.abs(sideways); s++) {
            c = new int[]{c[0] + side[0] * Integer.signum(sideways), c[1] + side[1] * Integer.signum(sideways)};
            way.add(c);
        }
        for (int n = 1; n < way.size(); n++) {
            int[] a = way.get(n - 1), b = way.get(n);
            l.low[b[0]][b[1]] = true;
            l.open(a[0], a[1], wayTo(a[0], a[1], b[0], b[1]), 0);
        }
        // the shrine beyond the last of it: its gate faces back the way one came
        int si = c[0] + fwd[0], sj = c[1] + fwd[1];
        // the two-by-two block whose near side holds (si, sj), reaching on, and back toward the palace's line
        int[] a0 = {si, sj}, a1 = {si + fwd[0], sj + fwd[1]}, a2 = {si - side[0] * Integer.signum(sideways), sj - side[1] * Integer.signum(sideways)};
        int[] a3 = {a2[0] + fwd[0], a2[1] + fwd[1]};
        int i0 = Math.min(Math.min(a0[0], a1[0]), Math.min(a2[0], a3[0]));
        int j0 = Math.min(Math.min(a0[1], a1[1]), Math.min(a2[1], a3[1]));
        shrine = addRoom(l, i0, j0, i0 + 1, j0 + 1, SHRINE, false, SHRINE_HEIGHT);
        shrineGateDir = (back + 2) % 4;
        l.open(c[0], c[1], back, 0);
        // the way she flees, cell by cell
        trail.add(new int[]{N, N});
        for (int s = 1; s <= g; s++) trail.add(new int[]{N + DIRS[back][0] * s, N + DIRS[back][1] * s});
        for (int n = 1; n < way.size(); n++) trail.add(way.get(n));
        trail.add(new int[]{si, sj});
        // lit well: the way is lined with lanterns
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) if (l.low[i][j] && l.room[i][j] == null) l.lit[i][j] = true;
        l.depth = 0;
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) {
            l.owned[i][j] = Math.max(Math.abs(i - N), Math.abs(j - N)) <= THRONE_REACH;
        }
        return l;
    }

    // ── Who is put where ────────────────────────────────────────────────

    /** The household's posts, floor by floor: more and stronger the deeper; a guardian before each way down. */
    private void posts(Random r) {
        for (int k = 0; k < FLOORS; k++) {
            Level l = levels[k];
            // a group in each hall (two in the larger)
            for (Room room : l.rooms) {
                if (room.kind() == HUB) continue;
                int area = (room.i1() - room.i0() + 1) * (room.j1() - room.j0() + 1);
                for (int g = 0; g < (area >= 6 ? 2 : 1); g++) {
                    int kind = room.kind() == GRANARY || room.kind() == VAULT || room.kind() == BRIDAL || room.kind() == GROTTO
                            ? (r.nextDouble() < 0.5 ? MAIDS : MIXED) : MICE;
                    posts.add(new Post(k, room.i0() + r.nextInt(room.i1() - room.i0() + 1), room.j0() + r.nextInt(room.j1() - room.j0() + 1),
                            false, kind, kind == MAIDS ? 2 : 3 + r.nextInt(2) + k / 3, r.nextDouble() < 0.15 + 0.06 * k));
                }
            }
            // and a few at crossings and in the galleries
            int extra = 2 + k / 2;
            for (int attempt = 0, made = 0; attempt < 300 && made < extra; attempt++) {
                int i = r.nextInt(SIZE), j = r.nextInt(SIZE);
                boolean up = r.nextDouble() < 0.3;
                if (up ? !l.high[i][j] : !l.low[i][j] || l.room[i][j] != null || degree(l, i, j) < 3) continue;
                if (Math.abs(i - l.ai) <= 2 && Math.abs(j - l.aj) <= 2) continue; // not on one as they come in
                posts.add(new Post(k, i, j, up, MICE, 3 + r.nextInt(2), r.nextDouble() < 0.1 + 0.05 * k));
                made++;
            }
            posts.add(new Post(k, l.gi, l.gj, false, GUARDIAN, 1, true));
        }
    }

    // ── What it is ──────────────────────────────────────────────────────

    public Level level(int k) {
        return levels[k];
    }

    public List<Stair> stairs() {
        return stairs;
    }

    public List<Post> posts() {
        return posts;
    }

    public Room palace() {
        return palace;
    }

    public Room shrine() {
        return shrine;
    }

    public Room hub() {
        return hub;
    }

    /** The way out of the shrine through its gate. */
    public int shrineGateDir() {
        return shrineGateDir;
    }

    /** The palace's front gate's side (toward the last stair), and the antechamber before it, as {i, j}. */
    public int frontDir() {
        return frontDir;
    }

    public int[] antechamber() {
        return new int[]{anteI, anteJ};
    }

    /** A cell's middle, in world blocks, as {x, z}. */
    public int[] cellMiddle(int i, int j) {
        return new int[]{cx + (i - N) * PITCH, cz + (j - N) * PITCH};
    }

    /** The world x of cell i's middle. */
    public int cellX(int i) {
        return cx + (i - N) * PITCH;
    }

    public int cellZ(int j) {
        return cz + (j - N) * PITCH;
    }

    /** The grid cell holding (x, z), as {i, j}; null outside the grid. */
    public int[] cellAt(int x, int z) {
        int i = Math.floorDiv(x - cx + 3, PITCH) + N, j = Math.floorDiv(z - cz + 3, PITCH) + N;
        return Level.inGrid(i, j) ? new int[]{i, j} : null;
    }

    /** A room's middle, in world blocks, as {x, z} (a half block off for an even span). */
    public double[] middle(Room room) {
        return new double[]{(cellX(room.i0()) - 1 + cellX(room.i1()) + 2) / 2.0, (cellZ(room.j0()) - 1 + cellZ(room.j1()) + 2) / 2.0};
    }

    /** The shrine's middle, on its floor: where she flees to. As {x, y, z}. */
    public int[] refuge() {
        double[] m = middle(shrine);
        return new int[]{(int) Math.floor(m[0]), THRONE, (int) Math.floor(m[1])};
    }

    /** Her dais, the palace's middle, on its floor, as {x, y, z}: where she stands. */
    public int[] dais() {
        return new int[]{cx, THRONE, cz};
    }

    /** The room the column (x, z) is in on level k, or null. */
    public Room roomAt(int k, int x, int z) {
        int[] c = cellAt(x, z);
        if (c == null) return null;
        Room room = levels[k].room[c[0]][c[1]];
        return room != null && insideRoom(room, x, z) ? room : null;
    }

    /** The level whose heights hold y (its slab), or -1. */
    public static int levelAt(int y) {
        for (int k = 0; k < LEVELS; k++) if (y >= bottom(k) && y <= top(k)) return k;
        return -1;
    }

    /** The lowest and highest height level k owns. */
    public static int bottom(int k) {
        return (k < FLOORS ? FLOOR[k] : THRONE) - SLAB_BELOW;
    }

    public static int top(int k) {
        return k < FLOORS ? FLOOR[k] + SLAB_ABOVE : THRONE + PALACE_HEIGHT + 2;
    }

    /** The walking height of level k's lower storey. */
    public static int floorY(int k) {
        return k < FLOORS ? FLOOR[k] : THRONE;
    }

    private boolean insideRoom(Room room, int x, int z) {
        int x0 = cellX(room.i0()) - 1, x1 = cellX(room.i1()) + 1, z0 = cellZ(room.j0()) - 1, z1 = cellZ(room.j1()) + 1;
        if (x < x0 || x > x1 || z < z0 || z > z1) return false;
        if (!room.round()) return true;
        double mx = (x0 + x1 + 1) / 2.0, mz = (z0 + z1 + 1) / 2.0;
        if (room.kind() == HUB) return Math.hypot(x + 0.5 - mx, z + 0.5 - mz) <= HUB_RADIUS;
        double rx = (x1 - x0 + 1) / 2.0, rz = (z1 - z0 + 1) / 2.0;
        double ex = (x + 0.5 - mx) / rx, ez = (z + 0.5 - mz) / rz;
        return ex * ex * ex * ex + ez * ez * ez * ez <= 1.15;
    }

    /** How tall a room is over (x, z). */
    public int roomHeight(Room room, int x, int z) {
        return room.height();
    }

    // ── Its blocks ──────────────────────────────────────────────────────

    /**
     * What each height of the column (x, z) holds, from {@link #Y_MIN} to {@link #Y_MAX}: rock, open
     * air, a stair's parts, a light, water, the door; {@link #UNTOUCHED} where the cave leaves the
     * ground be.
     */
    public void column(int x, int z, byte[] out) {
        java.util.Arrays.fill(out, UNTOUCHED);
        int u = x - cx, v = z - cz;
        int[] c = cellAt(x, z);
        if (c != null) {
            int i = c[0], j = c[1], ou = x - cellX(i), ov = z - cellZ(j);
            for (int k = 0; k < LEVELS; k++) levelColumn(k, x, z, i, j, ou, ov, out);
            for (Stair s : stairs) {
                if (Math.abs(i - s.i()) > 0 || Math.abs(j - s.j()) > 0) continue;
                // the stair's own heights, between its floors, rock round its well
                for (int y = s.lowerY() - SLAB_BELOW; y <= s.upperY() + SLAB_ABOVE; y++) {
                    if (out[y - Y_MIN] == UNTOUCHED) out[y - Y_MIN] = ROCK;
                }
                if (Math.abs(ou) <= 1 && Math.abs(ov) <= 1) stairColumn(s, ou, ov, out);
            }
        }
        // the gate hall, the great shaft, the door
        gateColumn(u, v, out);
    }

    private void levelColumn(int k, int x, int z, int i, int j, int ou, int ov, byte[] out) {
        Level l = levels[k];
        if (!l.owned[i][j]) return;
        int f = l.y;
        for (int y = bottom(k); y <= top(k); y++) out[y - Y_MIN] = ROCK;
        boolean ru = Math.abs(ou) <= 1, rv = Math.abs(ov) <= 1;
        // the lower storey
        Room room = l.room[i][j] != null && insideRoom(l.room[i][j], x, z) ? l.room[i][j] : null;
        int h0 = 0;
        if (room != null) h0 = room.height();
        else if (ru && rv) h0 = l.low[i][j] ? TUNNEL : 0;
        else if (rv) h0 = l.passage(i, j, ou > 0 ? 1 : 3, 0) ? TUNNEL : 0;
        else if (ru) h0 = l.passage(i, j, ov > 0 ? 2 : 0, 0) ? TUNNEL : 0;
        for (int y = f; y < f + h0; y++) out[y - Y_MIN] = AIR;
        if (h0 > 0 && room == null && (ru && rv ? l.flooded[i][j] : l.flooded[i][j] && neighbourFlooded(l, i, j, ou, ov))) {
            out[f - Y_MIN] = WATER;
        }
        // the upper storey
        if (k < FLOORS) {
            int h1 = 0;
            if (ru && rv) h1 = l.high[i][j] ? TUNNEL : 0;
            else if (rv) h1 = l.passage(i, j, ou > 0 ? 1 : 3, 1) ? TUNNEL : 0;
            else if (ru) h1 = l.passage(i, j, ov > 0 ? 2 : 0, 1) ? TUNNEL : 0;
            for (int y = f + STOREY; y < f + STOREY + h1; y++) out[y - Y_MIN] = AIR;
            if (l.drop[i][j] && ou == 0 && ov == 0) out[f + STOREY - 1 - Y_MIN] = AIR;
            if (ou == 0 && ov == 0 && l.high[i][j] && l.litHigh[i][j] && !l.drop[i][j]) out[f + STOREY + TUNNEL - 1 - Y_MIN] = LANTERN;
            rampColumn(l, i, j, ou, ov, out);
        }
        if (h0 > 0 && room == null && ou == 0 && ov == 0 && l.lit[i][j]) {
            out[f + TUNNEL - 1 - Y_MIN] = l.soul[i][j] ? SOUL_LANTERN : LANTERN;
        }
    }

    private static boolean neighbourFlooded(Level l, int i, int j, int ou, int ov) {
        int d = Math.abs(ou) > 1 ? (ou > 0 ? 1 : 3) : (ov > 0 ? 2 : 0);
        int ni = i + DIRS[d][0], nj = j + DIRS[d][1];
        return l.flooded(ni, nj);
    }

    /**
     * A ramp, in the band of its way from its foot's middle to its head's: five steps, a block up
     * each, from the lower storey's floor to the upper's, open over them up to the upper storey's
     * ceiling.
     */
    private static void rampColumn(Level l, int i, int j, int ou, int ov, byte[] out) {
        int f = l.y;
        for (int d = 0; d < 4; d++) {
            // t: how far along the ramp this column is, from its foot's middle (1-5); a: across it
            int t, a;
            boolean foot = l.ramp[i][j] == d;
            boolean head = !foot && Level.inGrid(i - DIRS[d][0], j - DIRS[d][1]) && l.ramp[i - DIRS[d][0]][j - DIRS[d][1]] == d;
            if (!foot && !head) continue;
            int along = ou * DIRS[d][0] + ov * DIRS[d][1], across = ou * DIRS[d][1] - ov * DIRS[d][0];
            t = foot ? along : along + PITCH;
            a = across;
            if (Math.abs(a) > 1 || t < 1 || t > 5) continue;
            int stepY = f + t - 1;
            for (int y = f; y < stepY; y++) out[y - Y_MIN] = ROCK;
            out[stepY - Y_MIN] = (byte) (STEP_N + d);
            for (int y = stepY + 1; y <= f + STOREY + TUNNEL - 1; y++) {
                if (t == 1 && y >= f + TUNNEL) break; // over its foot, the foot's own ceiling
                out[y - Y_MIN] = AIR;
            }
        }
    }

    /**
     * The spiral stair in its cell's 3x3 room: a pillar in the middle, from the lower floor to the
     * upper's ceiling (a light in it at each floor), and round it a step a block for every block of
     * the fall, so the top step is the upper floor where it is entered. Over the steps far below the
     * top, that floor is kept, so only the way down is open.
     */
    private static void stairColumn(Stair s, int dx, int dz, byte[] out) {
        int lower = s.lowerY(), upper = s.upperY(), rise = upper - lower;
        if (dx == 0 && dz == 0) {
            for (int y = lower; y < upper + TUNNEL; y++) out[y - Y_MIN] = (y == lower + 2 || y == upper + 2) ? CORE : PILLAR;
            return;
        }
        for (int y = lower; y < upper + TUNNEL; y++) out[y - Y_MIN] = AIR;
        int pos = ringPosition(s.dir(), dx, dz);
        boolean cover = true;
        for (int step = 1; step <= rise; step++) {
            if (Math.floorMod(step - rise, 8) != pos) continue;
            int from = Math.floorMod(pos - 1, 8);
            int[] at = ringOffset(s.dir(), pos), prev = ringOffset(s.dir(), from);
            int y = lower - 1 + step;
            out[y - Y_MIN] = stepCode(at[0] - prev[0], at[1] - prev[1]);
            if (y > upper - 1 - 4) cover = false; // room over the head of one coming down onto it
        }
        if (cover) out[upper - 1 - Y_MIN] = COVER;
    }

    /** Where ring position p lies from the stair's pillar, for a stair entered from {@code dir}: 0 the middle of that side, then round clockwise. */
    static int[] ringOffset(int dir, int p) {
        int[] d = DIRS[dir], right = DIRS[(dir + 1) % 4];
        return switch (p) {
            case 0 -> new int[]{d[0], d[1]};
            case 1 -> new int[]{d[0] + right[0], d[1] + right[1]};
            case 2 -> new int[]{right[0], right[1]};
            case 3 -> new int[]{right[0] - d[0], right[1] - d[1]};
            case 4 -> new int[]{-d[0], -d[1]};
            case 5 -> new int[]{-d[0] - right[0], -d[1] - right[1]};
            case 6 -> new int[]{-right[0], -right[1]};
            default -> new int[]{d[0] - right[0], d[1] - right[1]};
        };
    }

    static int ringPosition(int dir, int dx, int dz) {
        for (int p = 0; p < 8; p++) {
            int[] o = ringOffset(dir, p);
            if (o[0] == dx && o[1] == dz) return p;
        }
        return -1;
    }

    /** A step climbing the way (dx, dz): a stair block facing it (its high side ahead). */
    private static byte stepCode(int dx, int dz) {
        for (int d = 0; d < 4; d++) if (DIRS[d][0] == dx && DIRS[d][1] == dz) return (byte) (STEP_N + d);
        return STEP_N;
    }

    // ── The door, the gate hall, the great shaft ────────────────────────

    /** (u, v) from the centre as {along the door's way, across it}. */
    private int[] doorFrame(int u, int v) {
        int[] d = DIRS[doorDir];
        return new int[]{u * d[0] + v * d[1], u * d[1] - v * d[0]};
    }

    /** The great shaft's top: the gate hall's floor. Its stair's surfaces run down from there. */
    private void gateColumn(int u, int v, byte[] out) {
        int[] ac = doorFrame(u, v);
        int a = ac[0], c = ac[1];
        int D = doorY;
        double r = Math.hypot(u, v);
        int hubTop = FLOOR[0] + HALL_HEIGHT;
        // the shaft, from the hub's roof to the hall
        if (r <= LINING_RADIUS) {
            for (int y = hubTop; y <= D + HALL_HIGH + 1; y++) {
                byte code = shaftCode(u, v, y);
                if (y >= D && r <= SHAFT_RADIUS + 0.5 && y < D + HALL_HIGH) code = shaftOpen(u, v, y);
                if (y > D + HALL_HIGH - 1 && r <= LINING_RADIUS) code = ROCK;
                if (code != UNTOUCHED) out[y - Y_MIN] = code;
            }
            // in the hub, the stair winds on down to its floor
            for (int y = FLOOR[0]; y < hubTop; y++) {
                byte code = shaftCode(u, v, y);
                if (code == HELIX || code == HELIX_SLAB || code == HELIX_LANTERN) out[y - Y_MIN] = code;
            }
            // a pool under the open middle, to fall into
            if (r <= VOID_RADIUS) {
                out[FLOOR[0] - 1 - Y_MIN] = WATER;
                out[FLOOR[0] - 2 - Y_MIN] = WATER;
                out[FLOOR[0] - 3 - Y_MIN] = ROCK;
            }
        }
        // the gate hall, from the shaft out to the door
        if (a >= 3 && a <= DOOR_AT + 1 && Math.abs(c) <= HALL_HALF + 2) {
            for (int y = D - SLAB_BELOW; y <= D + HALL_HIGH + 1; y++) {
                if (r <= SHAFT_RADIUS + 0.5 && y < D + HALL_HIGH && y >= D) continue; // the shaft's own
                boolean inside = Math.abs(c) <= HALL_HALF && y >= D && y < D + HALL_HIGH && a <= DOOR_AT - 1;
                byte code = inside ? AIR : ROCK;
                if (inside && r <= LINING_RADIUS + 0.5 && y >= D && y < D + HALL_HIGH) code = AIR;
                out[y - Y_MIN] = code;
            }
            // lanterns down the hall, either side
            if (Math.abs(c) == HALL_HALF - 1 && a > 10 && a < DOOR_AT - 3 && Math.floorMod(a, 6) == 0) out[D + HALL_HIGH - 2 - Y_MIN] = LANTERN;
            // the door: its leaves, and outside them the doorway in its frame of blackstone, gilded round
            if (a == DOOR_AT || a == DOOR_AT + 1) {
                for (int y = D - 1; y <= D + HALL_HIGH + 1; y++) {
                    boolean way = Math.abs(c) <= DOOR_HALF && y >= D && y < D + DOOR_HIGH;
                    boolean gilt = Math.abs(c) <= DOOR_HALF + 1 && (y == D + DOOR_HIGH || y == D - 1) || Math.abs(c) == DOOR_HALF + 1;
                    out[y - Y_MIN] = way ? (a == DOOR_AT ? GATE : AIR) : gilt ? GILT : FRAME;
                }
            }
            if (a == DOOR_AT - 1 && Math.abs(c) <= HALL_HALF) {
                // the threshold inside the door is the hall's own
                for (int y = D; y < D + HALL_HIGH; y++) out[y - Y_MIN] = Math.abs(c) <= DOOR_HALF && y < D + DOOR_HIGH ? AIR : FRAME;
            }
        }
    }

    /** Inside the shaft at the hall's heights: open, but the stair's last steps up to the hall's floor. */
    private byte shaftOpen(int dx, int dz, int y) {
        byte code = shaftCode(dx, dz, y);
        return code == HELIX || code == HELIX_SLAB ? code : AIR;
    }

    /**
     * The great shaft at (dx, dz) from the centre, at height y: open in its middle; round that, a
     * stair in a spiral, a turn every {@value #SHAFT_PITCH} blocks, laid in half blocks, so it is
     * walked without a jump, its top at the gate hall's floor on the door's side; now and then a
     * lantern on it; rock about it. Untouched beyond the lining.
     */
    public byte shaftCode(int dx, int dz, int y) {
        double r = Math.hypot(dx, dz);
        if (r > LINING_RADIUS) return UNTOUCHED;
        if (r > SHAFT_RADIUS) return ROCK;
        if (r < VOID_RADIUS) return AIR;
        double turn = (Math.atan2(dz, dx) / (Math.PI * 2) + 1) % 1.0;
        double doorTurn = (Math.atan2(DIRS[doorDir][1], DIRS[doorDir][0]) / (Math.PI * 2) + 1) % 1.0;
        // the surfaces of the stair over this column: doorY + (turn - doorTurn + n) * pitch, to the half block, none over the hall's floor
        double phase = Math.floorMod((int) Math.round((turn - doorTurn) * 1000), 1000) / 1000.0;
        int n0 = (int) Math.floor((y + 1 - doorY - phase * SHAFT_PITCH) / SHAFT_PITCH);
        for (int n = n0 - 1; n <= n0 + 1; n++) {
            double q = Math.round((doorY + (phase + n) * SHAFT_PITCH) * 2) / 2.0;
            if (q > doorY) continue;
            if (q == y + 1) return HELIX;
            if (q == y + 0.5) return HELIX_SLAB;
            if (q == y && dz == 0 && (dx == 4 && Math.floorMod(n, 2) == 0 || dx == -4 && Math.floorMod(n, 2) == 1)) return HELIX_LANTERN;
        }
        return AIR;
    }

    /** The door's leaves' middle at its foot, as {x, y, z}. */
    public int[] door() {
        return new int[]{cx + DIRS[doorDir][0] * DOOR_AT, doorY, cz + DIRS[doorDir][1] * DOOR_AT};
    }

    /** A block in the forecourt before the door, {@code out} blocks out from it, on the threshold's height. */
    public int[] beforeDoor(int out) {
        return new int[]{cx + DIRS[doorDir][0] * (DOOR_AT + out), doorY, cz + DIRS[doorDir][1] * (DOOR_AT + out)};
    }

    // ── Ways through it ─────────────────────────────────────────────────

    /** The cells of the throne level from the palace's middle to the shrine's, by the way she flees. */
    public List<int[]> trail() {
        return trail;
    }

    /**
     * A walker's search through the cave as built from {@link #column}: from one standing place, every
     * place one reaches on foot (walking, stepping or jumping up a block with room over one's head,
     * dropping down no more than {@code maxDrop} blocks, or into water from any height). The door
     * counts as open if {@code doorOpen}. Returns the reached standing places (feet) as a set of keys
     * ({@link #key}).
     */
    public Walk walk(int x, int y, int z, int maxDrop, boolean doorOpen) {
        Walk w = new Walk(this, doorOpen);
        w.run(x, y, z, maxDrop);
        return w;
    }

    /** A walk through the cave, and what it reached. */
    public static final class Walk {
        private final Labyrinth lab;
        private final boolean doorOpen;
        private final Map<Long, byte[]> columns = new HashMap<>();
        private final BitSet seen = new BitSet();
        private static final int SPAN = 2 * (N + 2) * PITCH, H = Y_MAX - Y_MIN + 1;
        private int reached;

        Walk(Labyrinth lab, boolean doorOpen) {
            this.lab = lab;
            this.doorOpen = doorOpen;
        }

        private byte[] col(int x, int z) {
            long k = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
            return columns.computeIfAbsent(k, kk -> {
                byte[] b = new byte[H];
                lab.column(x, z, b);
                return b;
            });
        }

        private byte at(int x, int y, int z) {
            if (y < Y_MIN || y > Y_MAX) return UNTOUCHED;
            return col(x, z)[y - Y_MIN];
        }

        private boolean free(int x, int y, int z) {
            return passable(at(x, y, z), doorOpen);
        }

        /** Whether one can stand with feet at (x, y, z). */
        private boolean stands(int x, int y, int z) {
            if (!free(x, y, z) || !free(x, y + 1, z)) return false;
            byte under = at(x, y - 1, z);
            return bears(under) || at(x, y, z) == WATER;
        }

        private int index(int x, int y, int z) {
            int ix = x - lab.cx + SPAN / 2, iz = z - lab.cz + SPAN / 2;
            if (ix < 0 || iz < 0 || ix >= SPAN || iz >= SPAN) return -1;
            return (ix * SPAN + iz) * H + (y - Y_MIN);
        }

        void run(int x, int y, int z, int maxDrop) {
            ArrayDeque<int[]> q = new ArrayDeque<>();
            int i0 = index(x, y, z);
            if (i0 < 0 || !stands(x, y, z)) return;
            seen.set(i0);
            q.add(new int[]{x, y, z});
            while (!q.isEmpty()) {
                int[] p = q.poll();
                reached++;
                for (int[] d : DIRS) {
                    int nx = p[0] + d[0], nz = p[2] + d[1], y0 = p[1];
                    // on the level, or a step or jump up, or down off an edge
                    if (stands(nx, y0, nz)) visit(q, nx, y0, nz);
                    else if (free(p[0], y0 + 2, p[2]) && stands(nx, y0 + 1, nz)) visit(q, nx, y0 + 1, nz);
                    else if (free(nx, y0, nz) && free(nx, y0 + 1, nz)) {
                        for (int fall = 1; fall <= 64; fall++) {
                            int ny = y0 - fall;
                            if (ny < Y_MIN + 1) break;
                            if (!free(nx, ny, nz)) break;
                            if (stands(nx, ny, nz)) {
                                if (fall <= maxDrop || at(nx, ny, nz) == WATER || at(nx, ny - 1, nz) == WATER) visit(q, nx, ny, nz);
                                break;
                            }
                        }
                    }
                }
                // in water, one swims up
                if (at(p[0], p[1], p[2]) == WATER && stands(p[0], p[1] + 1, p[2])) visit(q, p[0], p[1] + 1, p[2]);
                // and one may let oneself fall straight down a hole (a drop)
                if (free(p[0], p[1] - 1, p[2])) {
                    for (int fall = 1; fall <= 64; fall++) {
                        int ny = p[1] - fall;
                        if (!free(p[0], ny, p[2])) break;
                        if (stands(p[0], ny, p[2])) {
                            if (fall <= maxDrop || at(p[0], ny, p[2]) == WATER || at(p[0], ny - 1, p[2]) == WATER) visit(q, p[0], ny, p[2]);
                            break;
                        }
                    }
                }
            }
        }

        private void visit(ArrayDeque<int[]> q, int x, int y, int z) {
            int i = index(x, y, z);
            if (i < 0 || seen.get(i)) return;
            seen.set(i);
            q.add(new int[]{x, y, z});
        }

        /** Whether the walk reached standing at (x, y, z). */
        public boolean reached(int x, int y, int z) {
            int i = index(x, y, z);
            return i >= 0 && seen.get(i);
        }

        /** Whether it reached anywhere in the cell (i, j) of level k, on a storey (its 3x3 room). */
        public boolean reachedCell(int k, int i, int j, int storey) {
            int y = floorY(k) + storey * STOREY;
            int[] m = lab.cellMiddle(i, j);
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) for (int dy = 0; dy <= 1; dy++) {
                if (reached(m[0] + a, y + dy, m[1] + b)) return true;
            }
            return false;
        }

        /** How many standing places it reached. */
        public int size() {
            return reached;
        }
    }

    static long mix(long base, long a, long b) {
        long z = base + a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + 0x1AB7B1L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A hash of a block, for the choices made block by block (materials, flowers): [0, 1). */
    public static double noise(long seed, int x, int y, int z) {
        return (mix(seed ^ 0x5EED_CA7EL, x * 31L + y, z * 17L - y) >>> 11) * 0x1.0p-53;
    }
}
