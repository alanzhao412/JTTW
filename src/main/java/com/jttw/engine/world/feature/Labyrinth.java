package com.jttw.engine.world.feature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bottomless Cave's labyrinth under one region, worked out from the seed and the region's
 * centre alone, so every chunk of it agrees on it whichever is made first. Pure: no Minecraft in
 * it, so it can be tested on its own.
 *
 * <p>Three levels of tunnels, one under another ({@link #FLOOR}), each a maze on a grid of
 * {@value #GRID}x{@value #GRID} cells {@value #PITCH} blocks apart (a 3-block tunnel, 4 blocks of
 * rock between), within a circle {@value #REACH} blocks round the centre:
 * <ul>
 *   <li>a spanning tree of the cells (Kruskal's, so it branches often and ends blind often), with a
 *       few loops opened in it, so there is more than one way round;</li>
 *   <li>chambers, a few cells merged into one rounded room: nests, larders, bone heaps, grottoes;</li>
 *   <li>spiral stairs down to the next level, three to each pair of levels, out towards the rim,
 *       and never under one another: each level must be crossed to go on;</li>
 *   <li>on the top level, the hub: a round chamber at the centre, where the shaft down from the
 *       sinkhole comes in;</li>
 *   <li>on the bottom level, the palace at the centre (two gates), and out towards the rim her
 *       shrine (one gate), where she flees.</li>
 * </ul>
 * The shaft from the surface is a void with a spiral stair round it ({@link #shaftCode}).
 */
public final class Labyrinth {

    // ── The plan ────────────────────────────────────────────────────────

    /** Blocks from one cell's centre to the next's; each cell has a 3-block room in its middle. */
    public static final int PITCH = 7, ROOM_LO = 2, ROOM_HI = 4;
    /** Cells to a side of the grid. */
    public static final int GRID = 21, MID = GRID / 2;
    /** From the grid's corner to the centre. */
    public static final int HALF = MID * PITCH + PITCH / 2;
    /** Cells whose centres lie within this of the centre are part of the maze. */
    public static final double REACH = 70;
    /** The height one walks at on each level, top first. */
    public static final int[] FLOOR = {32, 16, 0};
    public static final int LEVELS = FLOOR.length;
    /** Clear height of a tunnel. */
    public static final int TUNNEL = 4;
    /** What of each level's heights the labyrinth makes its own, from its floor: rock but where it is open. */
    public static final int SLAB_BELOW = 2, SLAB_ABOVE = 7;
    /** The heights the labyrinth's columns are worked out over. */
    public static final int Y_MIN = FLOOR[LEVELS - 1] - SLAB_BELOW, Y_MAX = FLOOR[0] + SLAB_ABOVE;
    /** The share of the maze's remaining walls opened, so it has loops. */
    public static final double LOOPS = 0.07;
    /** The share of tunnel cells with a lantern hanging in them, by level. */
    public static final double[] LIGHTS = {0.14, 0.11, 0.09};

    /** The hub at the top level's centre, its radius; the shaft that comes down into it. */
    public static final double HUB_RADIUS = 9.5;
    /** The shaft: its open middle, its stair's outer edge, the rock kept round it, and the mouth at the top. */
    public static final double VOID_RADIUS = 1.9, SHAFT_RADIUS = 4.6, LINING_RADIUS = 6.2, MOUTH_RADIUS = 2.6, DISH_RADIUS = 8.5;
    /** How far the shaft's stair climbs in a turn. */
    public static final int SHAFT_PITCH = 10;
    /** The palace: its cells either side of the centre's, and its height. */
    public static final int PALACE_CELLS = 2, PALACE_HEIGHT = 11;
    public static final int SHRINE_HEIGHT = 5;

    // what a block is
    public static final byte UNTOUCHED = 0, ROCK = 1, AIR = 2, STEP_N = 3, STEP_E = 4, STEP_S = 5, STEP_W = 6,
            PILLAR = 7, CORE = 8, WATER = 9, LANTERN = 10, SOUL_LANTERN = 11, COVER = 12,
            HELIX = 13, HELIX_SLAB = 14, HELIX_LANTERN = 15;

    /** Whether a block of this kind is open (something walks or stands in it), not rock. */
    public static boolean open(byte code) {
        return code != UNTOUCHED && code != ROCK && code != PILLAR && code != CORE && code != COVER;
    }

    /** The directions, as {dx, dz}: north, east, south, west (as the step codes). */
    public static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /** A chamber's kind. */
    public static final int NEST = 0, LARDER = 1, BONES = 2, GROTTO = 3, HUB = 4, PALACE = 5, SHRINE = 6;

    /** A room: cells i0..i1, j0..j1 of a level merged, rounded or square, so tall at its middle. */
    public record Room(int level, int i0, int j0, int i1, int j1, int kind, boolean round, int height) {
        boolean holds(int i, int j) {
            return i >= i0 && i <= i1 && j >= j0 && j <= j1;
        }
    }

    /** A spiral stair in cell (i, j), from level {@code upper} down to the next; entered on the upper level from {@code dir}. */
    public record Stair(int i, int j, int upper, int dir) {}

    /** One level's maze. */
    public static final class Level {
        final boolean[][] in = new boolean[GRID][GRID];
        /** Passages: between (i, j) and (i+1, j); between (i, j) and (i, j+1). */
        final boolean[][] east = new boolean[GRID][GRID], south = new boolean[GRID][GRID];
        final Room[][] room = new Room[GRID][GRID];
        final Stair[][] stair = new Stair[GRID][GRID];
        final List<Room> rooms = new ArrayList<>();
        final boolean[][] lit = new boolean[GRID][GRID], soul = new boolean[GRID][GRID];

        public boolean in(int i, int j) {
            return i >= 0 && j >= 0 && i < GRID && j < GRID && in[i][j];
        }

        /** Whether one can go from (i, j) the way {@code dir} to the next cell. */
        public boolean passage(int i, int j, int dir) {
            int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
            if (!in(i, j) || !in(ni, nj)) return false;
            return switch (dir) {
                case 0 -> south[i][nj];
                case 1 -> east[i][j];
                case 2 -> south[i][j];
                default -> east[ni][j];
            };
        }

        void open(int i, int j, int dir) {
            int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
            switch (dir) {
                case 0 -> south[i][nj] = true;
                case 1 -> east[i][j] = true;
                case 2 -> south[i][j] = true;
                default -> east[ni][j] = true;
            }
        }

        /** Whether a lantern hangs in cell (i, j). */
        public boolean lit(int i, int j) {
            return in(i, j) && lit[i][j];
        }

        public List<Room> rooms() {
            return rooms;
        }

        public Room roomOf(int i, int j) {
            return i >= 0 && j >= 0 && i < GRID && j < GRID ? room[i][j] : null;
        }
    }

    public final long seed;
    public final int cx, cz;
    private final Level[] levels = new Level[LEVELS];
    private final List<Stair> stairs = new ArrayList<>();
    private Room palace, shrine, hub;
    /** The cell outside the shrine its gate opens to, and the gate's way from the shrine. */
    private int shrineGateI, shrineGateJ, shrineGateDir;

    private static final Map<Long, Labyrinth> CACHE = new ConcurrentHashMap<>();

    /** The labyrinth under the region centred at (cx, cz) in a world of this seed. */
    public static Labyrinth of(long seed, int cx, int cz) {
        long key = mix(seed, cx, cz);
        Labyrinth l = CACHE.get(key);
        if (l != null && l.seed == seed && l.cx == cx && l.cz == cz) return l;
        if (CACHE.size() > 64) CACHE.clear();
        l = new Labyrinth(seed, cx, cz);
        CACHE.put(key, l);
        return l;
    }

    private Labyrinth(long seed, int cx, int cz) {
        this.seed = seed;
        this.cx = cx;
        this.cz = cz;
        Random r = new Random(mix(seed, cx, cz));
        for (int k = 0; k < LEVELS; k++) {
            levels[k] = new Level();
            for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
                levels[k].in[i][j] = Math.hypot(i - MID, j - MID) * PITCH <= REACH;
            }
        }
        // the hub, the palace and the shrine
        hub = addRoom(0, MID - 1, MID - 1, MID + 1, MID + 1, HUB, true, 6);
        palace = addRoom(LEVELS - 1, MID - PALACE_CELLS, MID - PALACE_CELLS, MID + PALACE_CELLS, MID + PALACE_CELLS, PALACE, false, PALACE_HEIGHT);
        placeShrine(r);
        // the stairs: three down from each level, out towards the rim, none under another
        for (int k = 0; k + 1 < LEVELS; k++) placeStairs(r, k);
        // a few chambers on each level
        for (int k = 0; k < LEVELS; k++) placeChambers(r, k);
        for (int k = 0; k < LEVELS; k++) carve(r, k);
        // the gates of the hub, the palace and the shrine
        for (int d = 0; d < 4; d++) gate(levels[0], hub, d);
        gate(levels[LEVELS - 1], palace, 0);
        gate(levels[LEVELS - 1], palace, 2);
        levels[LEVELS - 1].open(shrineGateI, shrineGateJ, (shrineGateDir + 2) % 4);
        for (Stair s : stairs) {
            // the way the stair is entered on its upper level: the one passage there
            Level up = levels[s.upper()];
            for (int d = 0; d < 4; d++) {
                if (up.passage(s.i(), s.j(), d)) {
                    Stair oriented = new Stair(s.i(), s.j(), s.upper(), d);
                    stairs.set(stairs.indexOf(s), oriented);
                    up.stair[s.i()][s.j()] = oriented;
                    levels[s.upper() + 1].stair[s.i()][s.j()] = oriented;
                    break;
                }
            }
        }
        // lanterns in the tunnels, here and there
        for (int k = 0; k < LEVELS; k++) {
            Level l = levels[k];
            for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
                if (!l.in[i][j] || l.room[i][j] != null || l.stair[i][j] != null) continue;
                if (r.nextDouble() < LIGHTS[k]) {
                    l.lit[i][j] = true;
                    l.soul[i][j] = r.nextDouble() < (k == 0 ? 0.5 : 0.25);
                }
            }
        }
    }

    private Room addRoom(int k, int i0, int j0, int i1, int j1, int kind, boolean round, int height) {
        Room room = new Room(k, i0, j0, i1, j1, kind, round, height);
        Level l = levels[k];
        l.rooms.add(room);
        for (int i = i0; i <= i1; i++) for (int j = j0; j <= j1; j++) {
            l.room[i][j] = room;
            l.in[i][j] = true;
            if (i < i1) l.east[i][j] = true;
            if (j < j1) l.south[i][j] = true;
        }
        return room;
    }

    /** Opens the passage from the middle of a room's side {@code dir} to the cell outside it. */
    private static void gate(Level l, Room room, int dir) {
        int mi = (room.i0() + room.i1()) / 2, mj = (room.j0() + room.j1()) / 2;
        int i = dir == 1 ? room.i1() : dir == 3 ? room.i0() : mi;
        int j = dir == 0 ? room.j0() : dir == 2 ? room.j1() : mj;
        l.open(i, j, dir);
    }

    private boolean free(int k, int i, int j, int margin) {
        for (int a = i - margin; a <= i + margin; a++) for (int b = j - margin; b <= j + margin; b++) {
            if (a < 0 || b < 0 || a >= GRID || b >= GRID) return false;
            if (!levels[k].in[a][b] || levels[k].room[a][b] != null || levels[k].stair[a][b] != null) return false;
        }
        return true;
    }

    private void placeShrine(Random r) {
        int k = LEVELS - 1;
        for (int attempt = 0; attempt < 400; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, d = 5.5 + r.nextDouble() * 2.2;
            int i0 = (int) Math.round(MID + Math.cos(a) * d - 0.5), j0 = (int) Math.round(MID + Math.sin(a) * d - 0.5);
            if (!free(k, i0, j0, 1) || !free(k, i0 + 1, j0 + 1, 1)) continue;
            shrine = addRoom(k, i0, j0, i0 + 1, j0 + 1, SHRINE, false, SHRINE_HEIGHT);
            // its gate faces the centre: the side whose outward way points most nearly at it
            double best = -2;
            for (int dir = 0; dir < 4; dir++) {
                double toward = -(DIRS[dir][0] * Math.cos(a) + DIRS[dir][1] * Math.sin(a));
                if (toward > best) {
                    best = toward;
                    shrineGateDir = dir;
                }
            }
            int gi = shrineGateDir == 1 ? i0 + 1 : shrineGateDir == 3 ? i0 : i0 + (r.nextBoolean() ? 1 : 0);
            int gj = shrineGateDir == 2 ? j0 + 1 : shrineGateDir == 0 ? j0 : j0 + (r.nextBoolean() ? 1 : 0);
            shrineGateI = gi + DIRS[shrineGateDir][0];
            shrineGateJ = gj + DIRS[shrineGateDir][1];
            return;
        }
        throw new IllegalStateException("no room for the shrine");
    }

    private void placeStairs(Random r, int upper) {
        int placed = 0;
        double start = r.nextDouble() * Math.PI * 2;
        for (int attempt = 0; attempt < 600 && placed < 3; attempt++) {
            double a = start + placed * Math.PI * 2 / 3 + (r.nextDouble() - 0.5) * 1.2, d = 4.5 + r.nextDouble() * 4;
            int i = (int) Math.round(MID + Math.cos(a) * d), j = (int) Math.round(MID + Math.sin(a) * d);
            if (!free(upper, i, j, 1) || !free(upper + 1, i, j, 1)) continue;
            // nor over or under another level's stair, room or shrine
            boolean clear = true;
            for (int k = 0; k < LEVELS; k++) {
                for (int a2 = i - 1; a2 <= i + 1; a2++) for (int b = j - 1; b <= j + 1; b++) {
                    if (levels[k].stair[a2][b] != null || (k != upper && k != upper + 1 && levels[k].room[a2][b] != null
                            && levels[k].room[a2][b].kind() >= HUB)) clear = false;
                }
            }
            if (!clear) continue;
            Stair s = new Stair(i, j, upper, 0);
            stairs.add(s);
            levels[upper].stair[i][j] = s;
            levels[upper + 1].stair[i][j] = s;
            placed++;
        }
        if (placed < 3) throw new IllegalStateException("no room for the stairs down from level " + upper);
    }

    private void placeChambers(Random r, int k) {
        int placed = 0;
        for (int attempt = 0; attempt < 300 && placed < 6; attempt++) {
            int w = 2 + (r.nextDouble() < 0.3 ? 1 : 0), h = 2 + (r.nextDouble() < 0.3 ? 1 : 0);
            int i0 = 1 + r.nextInt(GRID - w - 1), j0 = 1 + r.nextInt(GRID - h - 1);
            boolean ok = true;
            for (int i = i0; i < i0 + w && ok; i++) for (int j = j0; j < j0 + h && ok; j++) ok = free(k, i, j, 1);
            if (!ok) continue;
            addRoom(k, i0, j0, i0 + w - 1, j0 + h - 1, r.nextInt(4), true, 6);
            placed++;
        }
    }

    /** The level's maze: Kruskal's spanning tree (stairs as leaves, rooms as one), then a few loops. */
    private void carve(Random r, int k) {
        Level l = levels[k];
        int[] parent = new int[GRID * GRID];
        for (int n = 0; n < parent.length; n++) parent[n] = n;
        int[] degree = new int[GRID * GRID];
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < GRID; i++) for (int j = 0; j < GRID; j++) {
            if (!l.in[i][j]) continue;
            Room room = l.room[i][j];
            if (room != null) {
                // a room is one place already: its cells are joined
                union(parent, i * GRID + j, room.i0() * GRID + room.j0());
                if (room.kind() >= HUB) continue; // the hub, the palace and the shrine: only by their gates
            }
            for (int dir = 1; dir <= 2; dir++) {
                int ni = i + DIRS[dir][0], nj = j + DIRS[dir][1];
                if (!l.in(ni, nj)) continue;
                Room other = l.room[ni][nj];
                if (other != null && other.kind() >= HUB) continue;
                if (room != null && room == other) continue;
                edges.add(new int[]{i, j, dir});
            }
        }
        Collections.shuffle(edges, r);
        List<int[]> closed = new ArrayList<>();
        for (int[] e : edges) {
            int a = e[0] * GRID + e[1], b = (e[0] + DIRS[e[2]][0]) * GRID + e[1] + DIRS[e[2]][1];
            boolean leafA = l.stair[e[0]][e[1]] != null, leafB = l.stair[b / GRID][b % GRID] != null;
            if ((leafA && degree[a] > 0) || (leafB && degree[b] > 0) || (leafA && leafB) || find(parent, a) == find(parent, b)) {
                if (!leafA && !leafB) closed.add(e);
                continue;
            }
            union(parent, a, b);
            degree[a]++;
            degree[b]++;
            l.open(e[0], e[1], e[2]);
        }
        for (int[] e : closed) if (r.nextDouble() < LOOPS) l.open(e[0], e[1], e[2]);
    }

    private static int find(int[] parent, int n) {
        while (parent[n] != n) n = parent[n] = parent[parent[n]];
        return n;
    }

    private static void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }

    // ── What it is ──────────────────────────────────────────────────────

    public Level level(int k) {
        return levels[k];
    }

    public List<Stair> stairs() {
        return stairs;
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

    /** The world x of cell i's first block (its west wall). */
    public int cellX(int i) {
        return cx - HALF + i * PITCH;
    }

    public int cellZ(int j) {
        return cz - HALF + j * PITCH;
    }

    /** A room's middle, in world blocks, as {x, z} (a half block off for an even span). */
    public double[] middle(Room room) {
        return new double[]{(cellX(room.i0()) + ROOM_LO + cellX(room.i1()) + ROOM_HI + 1) / 2.0,
                (cellZ(room.j0()) + ROOM_LO + cellZ(room.j1()) + ROOM_HI + 1) / 2.0};
    }

    /** The shrine's middle, on its floor: where she flees to. As {x, y, z}. */
    public int[] refuge() {
        double[] m = middle(shrine);
        return new int[]{(int) Math.floor(m[0]), FLOOR[LEVELS - 1], (int) Math.floor(m[1])};
    }

    /** The room the block column (x, z) is in on level k, or null. */
    public Room roomAt(int k, int x, int z) {
        int u = x - (cx - HALF), v = z - (cz - HALF);
        if (u < 0 || v < 0 || u >= GRID * PITCH || v >= GRID * PITCH) return null;
        Room room = levels[k].room[u / PITCH][v / PITCH];
        return room != null && insideRoom(room, x, z) ? room : null;
    }

    /** The level whose heights hold y (its slab), or -1. */
    public static int levelAt(int y) {
        for (int k = 0; k < LEVELS; k++) if (y >= FLOOR[k] - SLAB_BELOW && y <= FLOOR[k] + SLAB_ABOVE) return k;
        return -1;
    }

    private boolean insideRoom(Room room, int x, int z) {
        int x0 = cellX(room.i0()) + ROOM_LO, x1 = cellX(room.i1()) + ROOM_HI;
        int z0 = cellZ(room.j0()) + ROOM_LO, z1 = cellZ(room.j1()) + ROOM_HI;
        if (x < x0 || x > x1 || z < z0 || z > z1) return false;
        if (!room.round()) return true;
        double mx = (x0 + x1 + 1) / 2.0, mz = (z0 + z1 + 1) / 2.0;
        if (room.kind() == HUB) return Math.hypot(x + 0.5 - mx, z + 0.5 - mz) <= HUB_RADIUS;
        double rx = (x1 - x0 + 1) / 2.0, rz = (z1 - z0 + 1) / 2.0;
        double ex = (x + 0.5 - mx) / rx, ez = (z + 0.5 - mz) / rz;
        // a rounded square: only its corners cut, so every tunnel into it meets it
        return ex * ex * ex * ex + ez * ez * ez * ez <= 1.15;
    }

    /** How tall a room is over (x, z): its full height at its middle, a tunnel's at its edge. */
    public int roomHeight(Room room, int x, int z) {
        if (!room.round()) return room.height();
        double[] m = middle(room);
        int x0 = cellX(room.i0()) + ROOM_LO, x1 = cellX(room.i1()) + ROOM_HI;
        int z0 = cellZ(room.j0()) + ROOM_LO, z1 = cellZ(room.j1()) + ROOM_HI;
        double rx = (x1 - x0 + 1) / 2.0 + 1, rz = (z1 - z0 + 1) / 2.0 + 1;
        double ex = (x + 0.5 - m[0]) / rx, ez = (z + 0.5 - m[1]) / rz;
        double t = Math.max(0, 1 - (ex * ex + ez * ez));
        return TUNNEL + (int) Math.round((room.height() - TUNNEL) * Math.sqrt(t));
    }

    /**
     * What each height of the column (x, z) holds, from {@link #Y_MIN} to {@link #Y_MAX}: rock, open
     * air, a stair's parts, a light, water; {@link #UNTOUCHED} where the labyrinth leaves the ground be.
     */
    public void column(int x, int z, byte[] out) {
        java.util.Arrays.fill(out, UNTOUCHED);
        int u = x - (cx - HALF), v = z - (cz - HALF);
        if (u >= 0 && v >= 0 && u < GRID * PITCH && v < GRID * PITCH) {
            int i = u / PITCH, j = v / PITCH, ou = u % PITCH, ov = v % PITCH;
            for (int k = 0; k < LEVELS; k++) levelColumn(k, x, z, i, j, ou, ov, out);
        }
        // the hub's shaft, down to its floor
        double r = Math.hypot(x - cx, z - cz);
        if (r <= LINING_RADIUS) {
            for (int y = FLOOR[0]; y <= Y_MAX; y++) {
                byte code = shaftCode(x - cx, z - cz, y);
                if (code == ROCK && out[y - Y_MIN] != UNTOUCHED) continue; // the hub is open there
                if (code != UNTOUCHED) out[y - Y_MIN] = code;
            }
            // a pool under the open middle, to fall into
            if (r <= VOID_RADIUS) {
                out[FLOOR[0] - 1 - Y_MIN] = WATER;
                out[FLOOR[0] - 2 - Y_MIN] = WATER;
                out[FLOOR[0] - 3 - Y_MIN] = ROCK;
            }
        }
    }

    private void levelColumn(int k, int x, int z, int i, int j, int ou, int ov, byte[] out) {
        Level l = levels[k];
        int f = FLOOR[k];
        Room room = l.room[i][j] != null && insideRoom(l.room[i][j], x, z) ? l.room[i][j] : null;
        boolean ru = ou >= ROOM_LO && ou <= ROOM_HI, rv = ov >= ROOM_LO && ov <= ROOM_HI;
        int height = 0;
        if (room != null) height = roomHeight(room, x, z);
        else if (ru && rv) height = l.in[i][j] && (l.room[i][j] == null) ? TUNNEL : 0;
        else if (rv && !ru) height = l.passage(i, j, ou < ROOM_LO ? 3 : 1) ? TUNNEL : 0;
        else if (ru) height = l.passage(i, j, ov < ROOM_LO ? 0 : 2) ? TUNNEL : 0;
        int top = f + SLAB_ABOVE;
        Room own = l.room[i][j];
        if (own != null && own.kind() == PALACE) top = Math.max(top, f + PALACE_HEIGHT + 1);
        Stair stair = l.stair[i][j];
        if (stair != null && stair.upper() == k - 1) top = Math.max(top, FLOOR[k - 1] + SLAB_ABOVE); // its shaft up
        for (int y = f - SLAB_BELOW; y <= top; y++) {
            if (out[y - Y_MIN] == UNTOUCHED || out[y - Y_MIN] == ROCK) out[y - Y_MIN] = y >= f && y < f + height ? AIR : ROCK;
        }
        if (height > 0 && room == null && ru && rv && l.lit[i][j] && ou == 3 && ov == 3) {
            out[f + TUNNEL - 1 - Y_MIN] = l.soul[i][j] ? SOUL_LANTERN : LANTERN;
        }
        if (stair != null && stair.upper() == k && ru && rv) stairColumn(stair, ou - 3, ov - 3, out);
    }

    /**
     * The spiral stair in its cell's 3x3 room: a pillar in the middle, from the lower level's floor to
     * the upper's ceiling (a light in it at each level), and round it sixteen steps, two turns, each
     * a block over the last, so the top step is the upper level's floor where it is entered. Over the
     * first five steps of the upper level's turn, that level's floor is kept, so only the way down is open.
     */
    private static void stairColumn(Stair s, int dx, int dz, byte[] out) {
        int lower = FLOOR[s.upper() + 1], upper = FLOOR[s.upper()];
        if (dx == 0 && dz == 0) {
            for (int y = lower; y < upper + TUNNEL; y++) out[y - Y_MIN] = (y == lower + 2 || y == upper + 2) ? CORE : PILLAR;
            return;
        }
        for (int y = lower; y < upper + TUNNEL; y++) out[y - Y_MIN] = AIR;
        int pos = ringPosition(s.dir(), dx, dz);
        for (int step = 1; step <= 16; step++) {
            if (step % 8 != pos) continue;
            int from = Math.floorMod(step - 1, 8);
            int[] at = ringOffset(s.dir(), pos), prev = ringOffset(s.dir(), from);
            out[lower - 1 + step - Y_MIN] = stepCode(at[0] - prev[0], at[1] - prev[1]);
        }
        if (pos >= 1 && pos <= 5) out[upper - 1 - Y_MIN] = COVER;
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

    /**
     * The shaft at (dx, dz) from the region's centre, at height y (from the hub's floor up): open in its middle; round that, a stair in a spiral, a turn every {@value #SHAFT_PITCH}
     * blocks, laid in half blocks, so it is walked without a jump; now and then a lantern on it;
     * rock about it. Untouched beyond the lining.
     */
    public static byte shaftCode(int dx, int dz, int y) {
        double r = Math.hypot(dx, dz);
        if (r > LINING_RADIUS) return UNTOUCHED;
        if (r > SHAFT_RADIUS) return ROCK;
        if (r < VOID_RADIUS) return AIR;
        double turn = (Math.atan2(dz, dx) / (Math.PI * 2) + 1) % 1.0;
        // the surfaces of the stair over this column: FLOOR[0] + (turn + n) * pitch, to the half block
        int n0 = (int) Math.floor((y + 1 - FLOOR[0] - turn * SHAFT_PITCH) / SHAFT_PITCH);
        for (int n = n0 - 1; n <= n0 + 1; n++) {
            double q = Math.round((FLOOR[0] + (turn + n) * SHAFT_PITCH) * 2) / 2.0;
            if (q == y + 1) return HELIX;
            if (q == y + 0.5) return HELIX_SLAB;
            // a lantern on the stair's outer edge, once a turn, on alternate sides
            if (q == y && dz == 0 && (dx == 4 && Math.floorMod(n, 2) == 0 || dx == -4 && Math.floorMod(n, 2) == 1)) return HELIX_LANTERN;
        }
        return AIR;
    }

    // ── Ways through it ─────────────────────────────────────────────────

    /** Every cell one can reach from the hub, by level: [k][i][j]. Through the passages and down the stairs. */
    public boolean[][][] reachable() {
        boolean[][][] seen = new boolean[LEVELS][GRID][GRID];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(new int[]{0, MID, MID});
        seen[0][MID][MID] = true;
        while (!open.isEmpty()) {
            int[] c = open.poll();
            Level l = levels[c[0]];
            for (int d = 0; d < 4; d++) {
                if (!l.passage(c[1], c[2], d)) continue;
                int ni = c[1] + DIRS[d][0], nj = c[2] + DIRS[d][1];
                if (!seen[c[0]][ni][nj]) {
                    seen[c[0]][ni][nj] = true;
                    open.add(new int[]{c[0], ni, nj});
                }
            }
            Stair s = l.stair[c[1]][c[2]];
            if (s != null) {
                int other = s.upper() == c[0] ? c[0] + 1 : c[0] - 1;
                if (!seen[other][c[1]][c[2]]) {
                    seen[other][c[1]][c[2]] = true;
                    open.add(new int[]{other, c[1], c[2]});
                }
            }
        }
        return seen;
    }

    /** The cells of the bottom level from the palace's middle to the shrine's, by the shortest way; empty if there is none. */
    public List<int[]> trail() {
        Level l = levels[LEVELS - 1];
        int[][] from = new int[GRID * GRID][];
        boolean[] seen = new boolean[GRID * GRID];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(new int[]{MID, MID});
        seen[MID * GRID + MID] = true;
        int[] end = null;
        while (!open.isEmpty()) {
            int[] c = open.poll();
            if (l.room[c[0]][c[1]] == shrine) {
                end = c;
                break;
            }
            for (int d = 0; d < 4; d++) {
                if (!l.passage(c[0], c[1], d)) continue;
                int ni = c[0] + DIRS[d][0], nj = c[1] + DIRS[d][1];
                if (seen[ni * GRID + nj]) continue;
                seen[ni * GRID + nj] = true;
                from[ni * GRID + nj] = c;
                open.add(new int[]{ni, nj});
            }
        }
        List<int[]> path = new ArrayList<>();
        for (int[] c = end; c != null; c = from[c[0] * GRID + c[1]]) path.add(0, c);
        return path;
    }

    /** A cell's middle, in world blocks, as {x, z}. */
    public int[] cellMiddle(int i, int j) {
        return new int[]{cellX(i) + 3, cellZ(j) + 3};
    }

    /** The grid cell holding (x, z), as {i, j}; null outside the grid. */
    public int[] cellAt(int x, int z) {
        int u = x - (cx - HALF), v = z - (cz - HALF);
        if (u < 0 || v < 0 || u >= GRID * PITCH || v >= GRID * PITCH) return null;
        return new int[]{u / PITCH, v / PITCH};
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
