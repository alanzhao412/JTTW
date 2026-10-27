package com.jttw.engine.world.fire;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Fire Cloud Cave (火云洞) as a fortress: the plan of it, worked out once, the same in every
 * world. Pure: no Minecraft in it, so it can be tested on its own; {@link FortressBlocks} says what
 * block each of its codes is, and the range lays it column by column ({@code FlamingRangeFeature}),
 * so it comes out whole whichever of its chunks is made first.
 *
 * <p>Local coordinates are block offsets from the region's centre, y from the floor of the gorge
 * before the door (the hall floor of the old cave; {@code FlamingRangeFeature#hallFloor}): +z is
 * south, toward the door. Into the mountain and down:
 * <ul>
 *   <li><b>the great door</b> on Withered Pine Ravine (枯松涧): a facade of black stone set in the
 *       cliff at the gorge's head, red pillars with braziers on them, two crouching demons, a gilded
 *       plaque, a tiered roof; the door itself two leaves of bronze banded with iron and studded
 *       with gold, thirteen blocks wide and fourteen high ({@link #DOOR});</li>
 *   <li>a gatehouse passage into the mountain, and then <b>eight floors</b>, each a great hall of
 *       its own ({@link #FLOORS}), each reached down a stair cut in the rock round them from the one
 *       above, its way down sealed by a grate ({@link Floor#seal}) that its guardian holds;</li>
 *   <li>at the bottom, Red Boy's <b>throne room</b>: a high pillared hall, a moat of lava down each
 *       side, the open floor round his dais where he fights, his throne on its steps behind.</li>
 * </ul>
 */
public final class FortressPlan {

    private FortressPlan() {}

    // ── Codes: what stands in a cell (FortressBlocks says which block) ──

    public static final byte UNTOUCHED = 0, AIR = 1, ROCK = 2, BRICK = 3, CRACKED = 4, DARK = 5, POLISHED = 6, GILDED = 7,
            CHISELED = 8, MAGMA = 9, LAVA = 10, LAMP = 11, LANTERN_HUNG = 12, LANTERN = 13, CAMPFIRE = 14, CHAIN = 15,
            SEAL_NS = 16, SEAL_EW = 17, DOOR_IRON = 18, DOOR_BAND = 19, DOOR_STUD = 20, CARPET = 21,
            STAIR_N = 22, STAIR_E = 23, STAIR_S = 24, STAIR_W = 25, SLAB = 26, BASALT = 27, NETHER = 28, FENCE = 29,
            ANVIL = 30, BLAST = 31, CAULDRON = 32, COAL = 33, BONE = 34, SMOKER = 35, SMOKE = 36, HAY = 37, WALL = 38,
            BARS_NS = 39, BARS_EW = 40, BAMBOO = 41, WOOL = 42, TERRACOTTA = 43, ORANGE = 44, BRAZIER = 45, CHEST = 46,
            OBSIDIAN = 47, GOLD = 48, BLACKSTONE = 49, ASH = 50, CAULDRON_LAVA = 51, BARS = 52;
    public static final int CODES = 53;

    /** Whether a code is open: one walks or stands in it. */
    public static boolean open(byte c) {
        return c == AIR || c == CARPET;
    }

    /** Whether a code holds one up (a block one can stand on), lava, fire and the like not. */
    public static boolean holds(byte c) {
        return c != UNTOUCHED && c != AIR && c != CARPET && c != LAVA && c != LANTERN_HUNG && c != CHAIN
                && c != CAMPFIRE && c != SMOKE && c != BRAZIER;
    }

    /** Whether a code is the door's: what the door opening takes away. */
    public static boolean door(byte c) {
        return c == DOOR_IRON || c == DOOR_BAND || c == DOOR_STUD;
    }

    /** Whether a code is a seal's grate. */
    public static boolean seal(byte c) {
        return c == SEAL_NS || c == SEAL_EW;
    }

    // ── The frame ───────────────────────────────────────────────────────

    /** The plan's extent, local. */
    public static final int X_MIN = -46, X_MAX = 46, Z_MIN = -46, Z_MAX = 62, Y_MIN = -118, Y_MAX = 36;
    private static final int SX = X_MAX - X_MIN + 1, SZ = Z_MAX - Z_MIN + 1, SY = Y_MAX - Y_MIN + 1;

    /** How far round the centre the rock of the fortress is its own (and its halls' bounds). */
    public static final int SHELL = 44, HALL = 33;
    /** The stairs run through the rock round the halls, this far out (their middle). */
    public static final int STAIR_LINE = 38;

    /** The floors' levels (the floor's top block), top first; their halls' clear height. */
    public static final int[] FLOOR_Y = {0, -13, -26, -39, -52, -65, -78, -91};
    public static final int[] FLOOR_H = {15, 10, 10, 10, 10, 10, 10, 10};
    public static final int FLOORS = FLOOR_Y.length;
    /** The throne room's floor and its clear height. */
    public static final int THRONE_Y = -113, THRONE_H = 18;
    /** The throne room's half-width, and how far it reaches north and south. */
    public static final int THRONE_X = 26, THRONE_N = -30, THRONE_S = 28;
    /** Where the throne stands (local z), and the arena round Red Boy's dais. */
    public static final int THRONE_Z = -25, ARENA = 8;

    /** The door: its leaves' local box, x0, y0, z0, x1, y1, z1 (two thick, at the gatehouse's mouth). */
    public static final int[] DOOR = {-6, 1, 50, 6, 14, 51};
    /** The facade's front face (local z); the gorge begins past it. */
    public static final int FACE_Z = 53;
    /** The return brazier at the door, by which one goes back down to the deepest landing reached. */
    public static final int[] BRAZIER_AT = {-5, 1, 57};
    /** Where the gate's wardens stand before the door (x, z), and how far round the door they are looked for. */
    public static final int[][] WARDEN_POSTS = {{-7, 58}, {7, 58}};

    // ── Kinds of garrison ───────────────────────────────────────────────

    public static final int IMP = 0, BRUTE = 1, IMP_CAPTAIN = 2, BRUTE_CAPTAIN = 3, GENERAL = 4, WARDEN = 5;

    /** One of a floor's garrison, where it stands (local; y its feet), what it is, and for a general which of the six. */
    public record Spot(int x, int y, int z, int kind, int variant) {}

    /**
     * One floor: its number (1 at the top; 0 for the forecourt before the door, 9 for the throne
     * room), its name, its floor's level and clear height; its garrison and its guardian; the grate
     * sealing its way down (a box, local, x0 y0 z0 x1 y1 z1; null for the throne room and the
     * forecourt) and the landing one arrives on from above (where a fall back to it brings one).
     */
    public record Floor(int number, String name, int y, int height, List<Spot> garrison, Spot guardian,
                        int[] seal, int[] landing) {

        /** Whether local (x, y, z) is on this floor: over its floor, under its ceiling, within the fortress. */
        public boolean holds(int x, int yy, int z) {
            return yy > y - 2 && yy <= y + height + 1 && Math.abs(x) <= SHELL && z >= -SHELL && z <= SHELL;
        }
    }

    /** The plan, worked out once. */
    private static volatile FortressPlan.Built built;

    public static Built get() {
        Built b = built;
        if (b == null) {
            synchronized (FortressPlan.class) {
                b = built;
                if (b == null) built = b = new Builder().build();
            }
        }
        return b;
    }

    /** The code at local (x, y, z): {@link #UNTOUCHED} outside the plan. */
    public static byte code(int x, int y, int z) {
        return get().code(x, y, z);
    }

    /** Whether the plan reaches the column at local (x, z). */
    public static boolean covers(int x, int z) {
        return x >= X_MIN && x <= X_MAX && z >= Z_MIN && z <= Z_MAX;
    }

    /** The level of floor {@code n} (1-8; 9 the throne room). */
    public static int levelOf(int n) {
        return n >= 9 ? THRONE_Y : FLOOR_Y[n - 1];
    }

    /** A floor's regulars' level: from 68 on the first to 75 on the eighth (the forecourt's 67). */
    public static int regularLevel(int floor) {
        return 67 + Math.min(8, floor);
    }

    /** Its captains': four over, no higher than the band's top. */
    public static int captainLevel(int floor) {
        return Math.min(77, regularLevel(floor) + 4);
    }

    /** Its guardian's: from 71 on the first floor to the band's top, 77, on the seventh and eighth. */
    public static int guardianLevel(int floor) {
        return Math.min(77, 70 + floor);
    }

    /** The plan, built. */
    public static final class Built {
        private final byte[] cells;
        private final List<Floor> floors;

        Built(byte[] cells, List<Floor> floors) {
            this.cells = cells;
            this.floors = floors;
        }

        public byte code(int x, int y, int z) {
            if (x < X_MIN || x > X_MAX || z < Z_MIN || z > Z_MAX || y < Y_MIN || y > Y_MAX) return UNTOUCHED;
            return cells[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)];
        }

        /** The floors: 0 the forecourt before the door, 1-8 the floors, 9 the throne room. */
        public List<Floor> floors() {
            return floors;
        }

        public Floor floor(int n) {
            return floors.get(n);
        }

        /** The floor local (x, y, z) is on (0 the forecourt, 9 the throne room), or -1 if none. */
        public int floorAt(int x, int y, int z) {
            for (int n = 1; n <= FLOORS + 1; n++) if (floors.get(n).holds(x, y, z)) return n;
            if (y >= -2 && y <= 30 && Math.abs(x) <= 40 && z > SHELL && z <= Z_MAX + 40) return 0;
            return -1;
        }

        /** Whether one can stand with his feet in local (x, y, z): room for him, something under him. Seals and the door count as open if {@code through}. */
        public boolean standable(int x, int y, int z, boolean through) {
            byte under = code(x, y - 1, z);
            return clear(code(x, y, z), through) && clear(code(x, y + 1, z), through) && holds(under) && !tall(under);
        }

        /** Whether a code is a block too tall to step up onto (walls, fences, grates). */
        private static boolean tall(byte c) {
            return c == WALL || c == FENCE || c == BARS || c == BARS_NS || c == BARS_EW || seal(c);
        }

        private static boolean clear(byte c, boolean through) {
            return open(c) || (through && (seal(c) || door(c)));
        }

        /**
         * How many steps on foot from local {@code from} to {@code to} (feet positions), walking,
         * climbing a block at a time and dropping up to three; -1 if there is no way. Seals and the
         * door are passed through if {@code through}.
         */
        public int steps(int[] from, int[] to, boolean through) {
            List<int[]> r = route(from, to, through);
            return r == null ? -1 : r.size() - 1;
        }

        /** The way on foot from local {@code from} to {@code to}, as {@link #steps} finds it, every place stood on in turn; null if there is none. */
        @javax.annotation.Nullable
        public List<int[]> route(int[] from, int[] to, boolean through) {
            if (!standable(from[0], from[1], from[2], through)) return null;
            int n = SX * SY * SZ;
            int[] dist = new int[n];
            java.util.Arrays.fill(dist, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            int start = index(from[0], from[1], from[2]);
            dist[start] = 0;
            queue.add(start);
            int goal = index(to[0], to[1], to[2]);
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            int[] prev = new int[n];
            while (!queue.isEmpty()) {
                int i = queue.poll();
                if (i == goal) {
                    List<int[]> way = new ArrayList<>();
                    for (int k = i; ; k = prev[k]) {
                        way.add(new int[]{k / SY / SZ + X_MIN, k % SY + Y_MIN, k / SY % SZ + Z_MIN});
                        if (k == start) break;
                    }
                    Collections.reverse(way);
                    return way;
                }
                int y = i % SY + Y_MIN, rest = i / SY, z = rest % SZ + Z_MIN, x = rest / SZ + X_MIN;
                for (int[] d : dirs) {
                    int nx = x + d[0], nz = z + d[1];
                    if (nx < X_MIN || nx > X_MAX || nz < Z_MIN || nz > Z_MAX) continue;
                    // a step up (room over his head first), level, or a drop of up to three
                    for (int dy = 1; dy >= -3; dy--) {
                        int ny = y + dy;
                        if (ny < Y_MIN + 1 || ny > Y_MAX - 2) continue;
                        if (dy == 1 && !clear(code(x, y + 2, z), through)) continue;
                        if (dy < 0) {
                            boolean fall = true;
                            for (int k = 0; k > dy; k--) if (!clear(code(nx, y + k, nz), through) || !clear(code(nx, y + k + 1, nz), through)) fall = false;
                            if (!fall) break;
                        }
                        if (!standable(nx, ny, nz, through)) continue;
                        int j = index(nx, ny, nz);
                        if (dist[j] < 0) {
                            dist[j] = dist[i] + 1;
                            prev[j] = i;
                            queue.add(j);
                        }
                        break;
                    }
                }
            }
            return null;
        }

        private static int index(int x, int y, int z) {
            return ((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN);
        }

        /** How many columns of floor {@code n} are open over its floor (its area, lava and all). */
        public int area(int n) {
            Floor f = floors.get(n);
            int a = 0;
            for (int x = -SHELL; x <= SHELL; x++) for (int z = -SHELL; z <= SHELL; z++) {
                if (open(code(x, f.y() + 1, z)) && open(code(x, f.y() + 2, z))) a++;
            }
            return a;
        }
    }

    // ── Building it ─────────────────────────────────────────────────────

    /** One flight of stairs down through the rock: from floor {@code from}'s level down to the next's. */
    private record Stair(int rot, int start, int top, int bottom) {}

    private static final class Builder {
        final byte[] g = new byte[SX * SY * SZ];
        final List<Floor> floors = new ArrayList<>();
        /** Where each stair's doorways came out in its halls: {x, z, dx, dz} of the first open cell, inward. */
        final int[][] topMouth = new int[FLOORS + 1][], bottomMouth = new int[FLOORS + 1][];

        void set(int x, int y, int z, byte c) {
            if (x < X_MIN || x > X_MAX || z < Z_MIN || z > Z_MAX || y < Y_MIN || y > Y_MAX) return;
            g[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)] = c;
        }

        byte get(int x, int y, int z) {
            if (x < X_MIN || x > X_MAX || z < Z_MIN || z > Z_MAX || y < Y_MIN || y > Y_MAX) return UNTOUCHED;
            return g[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)];
        }

        void box(int x0, int y0, int z0, int x1, int y1, int z1, byte c) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) set(x, y, z, c);
        }

        /** A hash of a cell, 0-99, for scattering things the same way every time. */
        static int hash(int x, int z, int salt) {
            long h = x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
            h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
            return (int) Math.floorMod(h ^ (h >>> 29), 100L);
        }

        Built build() {
            // the mountain's root: rock all through the fortress's ground, from under the throne room to over the first floor
            box(-SHELL, Y_MIN, -SHELL, SHELL, FLOOR_Y[0] + FLOOR_H[0] + 4, SHELL, ROCK);
            gate();
            outerCourt(FLOOR_Y[0]);
            forges(FLOOR_Y[1]);
            channels(FLOOR_Y[2]);
            barracks(FLOOR_Y[3]);
            kitchen(FLOOR_Y[4]);
            galleries(FLOOR_Y[5]);
            carts(FLOOR_Y[6]);
            cistern(FLOOR_Y[7]);
            throneRoom();
            // the stairs down, each in the rock round the halls on a different side from the last
            Stair[] stairs = {
                    new Stair(2, -10, FLOOR_Y[0], FLOOR_Y[1]),  // west, from the court's west aisle
                    new Stair(0, -24, FLOOR_Y[1], FLOOR_Y[2]),  // east, from the forges' north-east bay
                    new Stair(3, -30, FLOOR_Y[2], FLOOR_Y[3]),  // north
                    new Stair(1, -30, FLOOR_Y[3], FLOOR_Y[4]),  // south
                    new Stair(2, 10, FLOOR_Y[4], FLOOR_Y[5]),   // west
                    new Stair(0, -2, FLOOR_Y[5], FLOOR_Y[6]),   // east
                    new Stair(3, -8, FLOOR_Y[6], FLOOR_Y[7]),   // north
                    new Stair(1, -26, FLOOR_Y[7], THRONE_Y)};  // south, down to the throne room's doors
            int[][] seals = new int[FLOORS + 1][], landings = new int[FLOORS + 2][];
            for (int k = 0; k < stairs.length; k++) {
                int[][] made = stair(stairs[k], k + 1);
                seals[k + 1] = made[0];
                landings[k + 2] = made[1];
            }
            throneGate();
            lamps();
            // the forecourt: the gate's wardens before the door
            landings[1] = new int[]{-4, 1, 36, 4, 1, 44};
            floors.add(new Floor(0, "the forecourt", 0, 30, spots(0, new int[][]{
                    {-12, 62, IMP}, {12, 62, IMP}, {-3, 70, IMP}, {4, 74, IMP}}), new Spot(WARDEN_POSTS[0][0], 1, WARDEN_POSTS[0][1], WARDEN, 0), null, null));
            String[] names = {"the Outer Court", "the Forges", "the Lava Channels", "the Barracks of the Six Generals",
                    "the Kitchen of the Captives", "the Smoke Galleries", "the Hall of the Five Carts", "the Magma Cistern"};
            int[] generals = {2, 3, 4, 5, 1, 0}; // the Winds, Flame-Head, Eye-Spark, Fog-Diving, Cloud-Soaring
            for (int k = 1; k <= FLOORS; k++) {
                int[] m = topMouth[k];
                // the guardian stands two steps into the hall from his way down
                Spot guard = new Spot(m[0] + m[2] * 2, FLOOR_Y[k - 1] + 1, m[1] + m[3] * 2, k <= 6 ? GENERAL : WARDEN, k <= 6 ? generals[k - 1] : 0);
                floors.add(new Floor(k, names[k - 1], FLOOR_Y[k - 1], FLOOR_H[k - 1], spots(k, GARRISONS[k - 1]), stand(guard, k), seals[k], landings[k]));
            }
            floors.add(new Floor(9, "the throne room", THRONE_Y, THRONE_H, List.of(), null, null, landings[9]));
            return new Built(g, Collections.unmodifiableList(floors));
        }

        /** The garrisons, by floor: {x, z, kind}; each put on the nearest open floor to its place. */
        static final int[][][] GARRISONS = {
                // the Outer Court: imps in the aisles and the nave, brutes by the dais and the door
                {{-22, -12, IMP}, {22, -12, IMP}, {-22, 14, IMP}, {22, 14, IMP}, {-7, 2, IMP}, {7, 2, IMP}, {0, -14, IMP},
                        {-24, -24, IMP}, {24, -24, IMP}, {24, 26, IMP}, {-6, 22, BRUTE}, {6, 22, BRUTE}, {0, -8, BRUTE},
                        {18, 2, BRUTE}, {0, 10, IMP_CAPTAIN}},
                // the Forges: brutes at the hearths, imps between
                {{-22, -18, BRUTE}, {0, -18, BRUTE}, {22, 18, BRUTE}, {-22, 18, BRUTE}, {18, -2, BRUTE}, {-18, 2, BRUTE},
                        {-26, -26, IMP}, {26, 26, IMP}, {-4, 26, IMP}, {4, -26, IMP}, {-26, 4, IMP}, {26, -6, IMP},
                        {0, 24, IMP}, {12, 12, IMP}, {-22, 24, BRUTE_CAPTAIN}, {22, -26, IMP_CAPTAIN}},
                // the Lava Channels: imps on the islands, brutes at the bridges
                {{26, -20, IMP}, {26, 6, IMP}, {10, -24, IMP}, {10, 20, IMP}, {10, -4, IMP}, {-6, 24, IMP}, {-6, -24, IMP},
                        {-6, 4, IMP}, {-24, -16, IMP}, {-24, 20, IMP}, {-26, 2, IMP}, {28, 24, BRUTE}, {10, 26, BRUTE},
                        {-6, -14, BRUTE}, {10, -14, IMP_CAPTAIN}, {-24, -26, BRUTE_CAPTAIN}},
                // the Barracks: squads in the yard and in every barrack
                {{-4, -4, BRUTE}, {4, 4, BRUTE}, {-6, 6, IMP}, {6, -6, IMP}, {0, -24, IMP}, {0, 24, IMP}, {22, -14, BRUTE},
                        {22, 14, IMP}, {-22, 14, BRUTE}, {-22, -14, IMP}, {14, 26, IMP}, {-14, -26, IMP}, {26, 4, BRUTE},
                        {-26, -4, IMP}, {0, 0, BRUTE_CAPTAIN}, {24, 24, IMP_CAPTAIN}, {-24, 22, BRUTE_CAPTAIN}},
                // the Kitchen: cooks at the cauldrons, brutes at the cages
                {{-24, -18, BRUTE}, {-8, -18, BRUTE}, {8, -18, BRUTE}, {24, -18, BRUTE}, {-26, 10, IMP}, {-26, 24, IMP},
                        {26, 10, IMP}, {26, 24, IMP}, {-10, 8, IMP}, {10, 8, IMP}, {0, 18, IMP}, {-14, 26, IMP},
                        {14, -8, BRUTE}, {-14, -8, BRUTE_CAPTAIN}, {0, 26, IMP_CAPTAIN}, {22, 0, BRUTE_CAPTAIN}},
                // the Smoke Galleries: in the cells and at the crossings
                {{-30, -18, IMP}, {-18, -30, IMP}, {-6, -6, BRUTE}, {6, 6, IMP}, {18, -18, IMP}, {30, 18, IMP}, {-18, 18, BRUTE},
                        {6, -30, IMP}, {-30, 30, IMP}, {18, 30, BRUTE}, {30, -30, IMP}, {-6, 18, IMP}, {6, -18, BRUTE},
                        {-18, -6, IMP_CAPTAIN}, {18, 6, BRUTE_CAPTAIN}, {0, 30, IMP_CAPTAIN}},
                // the Hall of the Five Carts: a captain at each cart, brutes and imps round the well
                {{0, -19, BRUTE_CAPTAIN}, {18, -6, IMP_CAPTAIN}, {11, 15, BRUTE_CAPTAIN}, {-11, 15, IMP_CAPTAIN}, {-18, -6, BRUTE_CAPTAIN},
                        {-8, -4, BRUTE}, {8, -4, BRUTE}, {0, 9, BRUTE}, {-24, 10, IMP}, {24, 10, IMP}, {-14, -22, IMP},
                        {14, -22, IMP}, {0, 26, IMP}, {-26, -10, IMP}, {26, -10, IMP}, {6, 24, BRUTE}},
                // the Magma Cistern: imps on every island, the causeway held
                {{-8, -14, IMP}, {-6, -16, IMP}, {12, -2, BRUTE}, {14, 0, IMP}, {-10, 12, BRUTE}, {-12, 10, IMP}, {24, -18, IMP},
                        {-24, 0, IMP}, {24, 14, IMP}, {25, -17, IMP_CAPTAIN}, {-23, 1, IMP_CAPTAIN}, {8, -28, BRUTE},
                        {16, 28, BRUTE}, {2, -6, BRUTE_CAPTAIN}, {-2, 20, BRUTE_CAPTAIN}, {23, 15, IMP}}};

        List<Spot> spots(int floor, int[][] list) {
            int y = floor == 0 ? 1 : FLOOR_Y[floor - 1] + 1;
            List<Spot> out = new ArrayList<>();
            for (int[] s : list) {
                Spot at = floor == 0 ? new Spot(s[0], y, s[1], s[2], 0) : stand(new Spot(s[0], y, s[1], s[2], 0), floor);
                if (at != null) out.add(at);
            }
            return Collections.unmodifiableList(out);
        }

        /**
         * The spot, moved to the nearest place on its floor where the broadest and tallest of them
         * (a brute captain) stands free: solid floor under it, not by fire, open a block round it
         * and four high. Null if there is none near.
         */
        Spot stand(Spot s, int floor) {
            int y = FLOOR_Y[floor - 1] + 1;
            for (int r = 0; r <= 6; r++) {
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = s.x() + dx, z = s.z() + dz;
                    byte under = get(x, y - 1, z);
                    if (!holds(under) || under == MAGMA || under == WALL || under == FENCE) continue;
                    boolean free = true;
                    for (int ax = -1; ax <= 1 && free; ax++) for (int az = -1; az <= 1 && free; az++) for (int t = 0; t <= 3; t++) {
                        if (!open(get(x + ax, y + t, z + az))) { free = false; break; }
                    }
                    if (free) return new Spot(x, y, z, s.kind(), s.variant());
                }
            }
            return null;
        }

        // ── Pieces ──────────────────────────────────────────────────────

        /** Clears a hall's room: floor of {@code floor} at y, open over it to y + h. */
        void room(int x0, int z0, int x1, int z1, int y, int h, byte floor) {
            box(x0, y, z0, x1, y, z1, floor);
            box(x0, y + 1, z0, x1, y + h, z1, AIR);
        }

        /** Faces the hall's walls round a box: where rock meets the open, {@code wall} instead, a block deep. */
        void face(int y0, int y1, byte wall) {
            for (int x = -SHELL + 1; x < SHELL; x++) for (int z = -SHELL + 1; z < SHELL; z++) for (int y = y0; y <= y1; y++) {
                if (get(x, y, z) != ROCK) continue;
                if (open(get(x + 1, y, z)) || open(get(x - 1, y, z)) || open(get(x, y, z + 1)) || open(get(x, y, z - 1))) set(x, y, z, wall);
            }
        }

        /**
         * Faces a hall's ceiling, over floor y with clear height h: where rock hangs over the open,
         * {@code c} instead, in beams of {@code beam} every six blocks each way (none if UNTOUCHED).
         */
        void ceiling(int y, int h, byte c, byte beam) {
            for (int x = -SHELL + 1; x < SHELL; x++) for (int z = -SHELL + 1; z < SHELL; z++) for (int t = y + 3; t <= y + h + 1; t++) {
                if (get(x, t, z) != ROCK || !open(get(x, t - 1, z))) continue;
                boolean onBeam = beam != UNTOUCHED && (Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0);
                set(x, t, z, onBeam ? beam : c);
            }
        }

        /** A pillar w x w from y0 to y1 at (x, z) (its low corner), of {@code c}, a band of {@code band} at {@code bandY}. */
        void pillar(int x, int z, int w, int y0, int y1, byte c, byte band, int bandY) {
            box(x, y0, z, x + w - 1, y1, z + w - 1, c);
            if (band != UNTOUCHED) box(x, bandY, z, x + w - 1, bandY, z + w - 1, band);
        }

        /** Lights every hall: a lamp let into its ceiling every few blocks over open floor. */
        void lamps() {
            for (int k = 0; k < FLOORS; k++) {
                int y = FLOOR_Y[k], top = y + FLOOR_H[k];
                int every = k == 5 ? 9 : 6; // the galleries are dim
                for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                    if (Math.floorMod(x, every) != 3 || Math.floorMod(z, every) != 3) continue;
                    for (int t = top; t > y + 4; t--) {
                        if (open(get(x, t, z)) && get(x, t + 1, z) != AIR && get(x, t + 1, z) != UNTOUCHED && get(x, t + 1, z) != LAMP
                                && !seal(get(x, t + 1, z))) {
                            set(x, t + 1, z, LAMP);
                            break;
                        }
                    }
                }
            }
        }

        // ── The gate ────────────────────────────────────────────────────

        void gate() {
            // the mountain over the gatehouse and behind the facade
            box(-24, -2, SHELL, 24, 30, FACE_Z, ROCK);
            // the gatehouse passage from the door into the court
            for (int z = HALL; z < DOOR[2]; z++) for (int x = -7; x <= 7; x++) {
                set(x, 0, z, DARK);
                int top = Math.abs(x) <= 4 ? 14 : 13;
                for (int y = 1; y <= 14; y++) set(x, y, z, Math.abs(x) == 7 ? (y % 5 == 0 ? CHISELED : DARK) : y <= top ? AIR : DARK);
                if (Math.abs(x) <= 1) set(x, 1, z, CARPET);
                // red pillars along its walls, lamps between
                if (Math.abs(x) == 6 && Math.floorMod(z, 5) == 0) for (int y = 1; y <= 12; y++) set(x, y, z, BRICK);
                if (Math.abs(x) == 7 && Math.floorMod(z, 5) == 2) set(x, 9, z, LAMP);
            }
            for (int z = HALL + 2; z < DOOR[2]; z += 4) {
                set(0, 13, z, CHAIN);
                set(0, 12, z, LANTERN_HUNG);
            }
            // the door: two leaves of bronze, banded in iron, studded with gold, a ring on each
            for (int x = DOOR[0]; x <= DOOR[3]; x++) for (int y = DOOR[1]; y <= DOOR[4]; y++) for (int z = DOOR[2]; z <= DOOR[5]; z++) {
                boolean front = z == DOOR[5];
                byte c = DOOR_IRON;
                if (y == 3 || y == 8 || y == 13 || x == 0) c = DOOR_BAND;
                if (front && (y == 5 || y == 10) && Math.floorMod(x, 2) == 1 && x != 0) c = DOOR_STUD;
                if (front && y == 7 && Math.abs(x) == 2) c = DOOR_STUD; // the rings
                set(x, y, z, c);
            }
            set(-1, 0, DOOR[5], CHISELED);
            set(1, 0, DOOR[5], CHISELED);
            // the facade, the door recessed in its arch
            for (int x = -18; x <= 18; x++) for (int y = 0; y <= 24; y++) for (int z = DOOR[5] + 1; z <= FACE_Z; z++) {
                int ax = Math.abs(x);
                boolean front = z == FACE_Z;
                if (ax <= 6 && y >= 1 && y <= 14) {
                    set(x, y, z, AIR);
                    continue;
                }
                byte c = DARK;
                if (y == 0) c = POLISHED;
                else if (ax <= 8 && y <= 16 && (ax >= 7 || y >= 15)) c = front ? CHISELED : DARK; // the arch
                else if (y == 21 || y == 22) c = NETHER;
                else if (front && (ax == 18 || ax == 13) && y <= 20) c = POLISHED;
                set(x, y, z, c);
            }
            // the plaque: 火云洞, gold in a black frame, out from the face
            for (int x = -6; x <= 6; x++) for (int y = 17; y <= 20; y++) {
                boolean frame = Math.abs(x) == 6 || y == 17 || y == 20;
                set(x, y, FACE_Z + 1, frame ? CHISELED : GILDED);
            }
            // the tiered roof over it, a gold ridge
            box(-21, 25, DOOR[2], 21, 25, FACE_Z + 3, NETHER);
            box(-19, 26, DOOR[2], 19, 26, FACE_Z + 1, NETHER);
            box(-16, 27, DOOR[2], 16, 27, FACE_Z - 1, BRICK);
            box(-14, 28, DOOR[2], 14, 28, DOOR[2] + 1, GILDED);
            for (int sx : new int[]{-21, 21}) {
                set(sx, 26, FACE_Z + 3, NETHER);
                set(sx, 27, FACE_Z + 3, GILDED);
            }
            for (int x = -20; x <= 20; x += 4) set(x, 24, FACE_Z + 3, LANTERN_HUNG);
            // two red pillars before it, each with a brazier on top
            for (int sx : new int[]{-12, 10}) {
                pillar(sx, FACE_Z + 1, 3, 1, 22, BRICK, CHISELED, 1);
                box(sx, 22, FACE_Z + 1, sx + 2, 22, FACE_Z + 3, GILDED);
                set(sx + 1, 23, FACE_Z + 2, MAGMA);
                set(sx + 1, 24, FACE_Z + 2, CAMPFIRE);
                box(sx, 0, FACE_Z + 1, sx + 2, 0, FACE_Z + 3, POLISHED);
            }
            // two demons crouched either side of the door, eyes of fire
            for (int sx : new int[]{-17, 15}) {
                box(sx, 0, FACE_Z + 2, sx + 2, 0, FACE_Z + 5, POLISHED);
                box(sx, 1, FACE_Z + 2, sx + 2, 1, FACE_Z + 5, CHISELED);
                box(sx, 2, FACE_Z + 2, sx + 2, 5, FACE_Z + 4, BLACKSTONE);
                box(sx, 2, FACE_Z + 5, sx + 2, 3, FACE_Z + 5, BLACKSTONE); // forepaws
                box(sx, 6, FACE_Z + 3, sx + 2, 8, FACE_Z + 5, BLACKSTONE); // head
                set(sx, 7, FACE_Z + 5, MAGMA);
                set(sx + 2, 7, FACE_Z + 5, MAGMA);
                set(sx + 1, 6, FACE_Z + 5, GILDED); // teeth
                set(sx, 9, FACE_Z + 3, FENCE);
                set(sx + 2, 9, FACE_Z + 3, FENCE); // horns
            }
            // standing braziers down the forecourt, and the return brazier by the door
            for (int sx : new int[]{-8, 8}) for (int z : new int[]{58, 62}) {
                set(sx, 0, z, POLISHED);
                set(sx, 1, z, WALL);
                set(sx, 2, z, MAGMA);
                set(sx, 3, z, CAMPFIRE);
            }
            set(BRAZIER_AT[0], 0, BRAZIER_AT[2], GILDED);
            set(BRAZIER_AT[0], 1, BRAZIER_AT[2], BRAZIER);
        }

        // ── 1. The Outer Court ──────────────────────────────────────────

        void outerCourt(int y) {
            int h = FLOOR_H[0];
            // the nave, full height, and the aisles either side, lower
            room(-12, -28, 12, 32, y, h, BRICK);
            room(-30, -28, -13, 32, y, 10, BRICK);
            room(13, -28, 30, 32, y, 10, BRICK);
            // the doorway from the gatehouse
            box(-6, y + 1, HALL - 1, 6, y + 14, HALL, AIR);
            face(y, y + h + 1, DARK);
            ceiling(y, h, DARK, NETHER);
            for (int z = -28; z <= 32; z++) {
                set(-12, y, z, DARK);
                set(12, y, z, DARK);
                if (z >= -18) for (int x = -1; x <= 1; x++) set(x, y + 1, z, CARPET);
            }
            // the arcades: red pillars between the nave and the aisles, lamps in them over the aisles
            for (int z = -26; z <= 30; z += 6) for (int sx : new int[]{-14, 13}) {
                pillar(sx, z, 2, y + 1, y + h, BRICK, CHISELED, y + 1);
                box(sx, y + 10, z, sx + 1, y + 10, z + 1, GILDED);
                set(sx == -14 ? -13 : 13, y + 8, z, LAMP);
            }
            // lanterns hung down the nave
            for (int z = -20; z <= 28; z += 8) for (int sx : new int[]{-6, 6}) {
                for (int t = y + 11; t <= y + h; t++) set(sx, t, z, CHAIN);
                set(sx, y + 10, z, LANTERN_HUNG);
            }
            // weapon racks and red hangings along the outer walls
            for (int z = -24; z <= 28; z += 4) for (int sx : new int[]{-30, 30}) {
                set(sx, y + 1, z, FENCE);
                set(sx, y + 2, z, FENCE);
            }
            for (int z = -22; z <= 26; z += 8) for (int sx : new int[]{-31, 31}) for (int t = y + 4; t <= y + 8; t++) set(sx, t, z, WOOL);
            // the dais at the north end: two steps, the flame idol between two great braziers
            box(-10, y + 1, -28, 10, y + 1, -21, POLISHED);
            box(-7, y + 2, -28, 7, y + 2, -24, POLISHED);
            for (int x = -10; x <= 10; x++) set(x, y + 1, -20, STAIR_N);
            for (int x = -7; x <= 7; x++) set(x, y + 2, -23, STAIR_N);
            for (int sx : new int[]{-6, 6}) {
                set(sx, y + 3, -25, MAGMA);
                set(sx, y + 4, -25, CAMPFIRE);
            }
            box(-1, y + 3, -28, 1, y + 10, -26, BLACKSTONE);
            box(-1, y + 11, -28, 1, y + 11, -26, GILDED);
            set(0, y + 12, -27, LAMP);
            set(0, y + 8, -25, MAGMA);
            set(-1, y + 9, -25, GILDED);
            set(1, y + 9, -25, GILDED);
        }

        // ── 2. The Forges ───────────────────────────────────────────────

        void forges(int y) {
            int h = FLOOR_H[1];
            room(-HALL, -HALL, HALL, HALL, y, h, DARK);
            // the walls between the nine bays, arched through in the middle of each side
            for (int w : new int[]{-11, 10}) {
                for (int along = -HALL; along <= HALL; along++) {
                    boolean arch = Math.abs(along + 22) <= 2 || Math.abs(along) <= 2 || Math.abs(along - 22) <= 2;
                    for (int t = y + 1; t <= y + h; t++) {
                        byte c = arch && t <= y + 6 ? AIR : t == y + 7 && arch ? CHISELED : BRICK;
                        set(w, t, along, c);
                        set(w + 1, t, along, c);
                        set(along, t, w, c);
                        set(along, t, w + 1, c);
                    }
                }
            }
            // the crossings of the walls stay whole
            for (int a : new int[]{-11, 10}) for (int b : new int[]{-11, 10}) box(a, y + 1, b, a + 1, y + h, b + 1, BRICK);
            face(y, y + h + 1, BRICK);
            ceiling(y, h, NETHER, UNTOUCHED);
            // floors of the bays scattered with cracked brick and ash
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                if (get(x, y, z) == DARK && hash(x, z, 21) < 12) set(x, y, z, ASH);
            }
            for (int bx : new int[]{-22, 0, 22}) for (int bz : new int[]{-22, 0, 22}) {
                if (bx == 0 && bz == 0) {
                    greatFurnace(y);
                    continue;
                }
                // a hearth of lava, a low wall round it, a hood over it on chains
                box(bx - 2, y, bz - 2, bx + 2, y, bz + 2, POLISHED);
                box(bx - 1, y, bz - 1, bx + 1, y, bz + 1, LAVA);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) set(bx + dx, y + 1, bz + dz, WALL);
                }
                box(bx - 2, y + 7, bz - 2, bx + 2, y + 7, bz + 2, NETHER);
                box(bx - 1, y + 8, bz - 1, bx + 1, y + h, bz + 1, NETHER);
                for (int sx : new int[]{-2, 2}) for (int sz : new int[]{-2, 2}) for (int t = y + 2; t <= y + 6; t++) set(bx + sx, t, bz + sz, CHAIN);
                // anvils, furnaces, coal
                set(bx + 4, y + 1, bz + 4, ANVIL);
                set(bx - 4, y + 1, bz + 4, ANVIL);
                set(bx - 4, y + 1, bz - 4, BLAST);
                set(bx + 4, y + 1, bz - 4, BLAST);
                box(bx + 6, y + 1, bz - 7, bx + 7, y + 2, bz - 6, COAL);
                set(bx - 7, y + 1, bz + 7, COAL);
                set(bx - 6, y + 1, bz + 7, CAULDRON_LAVA);
            }
        }

        /** The great furnace in the middle bay: a tower of brick, lava glowing through its grates, a chimney to the roof. */
        void greatFurnace(int y) {
            box(-3, y + 1, -3, 3, y + 8, 3, BRICK);
            box(-2, y + 1, -2, 2, y + 3, 2, LAVA);
            box(-2, y + 1, -2, 2, y + 1, 2, MAGMA);
            for (int x = -2; x <= 2; x++) for (int t = y + 2; t <= y + 4; t++) {
                set(x, t, 3, BARS_EW);
                set(x, t, -3, BARS_EW);
            }
            box(-2, y + 4, -2, 2, y + 4, 2, MAGMA);
            box(-3, y + 9, -3, 3, y + 9, 3, CHISELED);
            box(-1, y + 9, -1, 1, y + FLOOR_H[1], 1, NETHER);
            box(-3, y + 1, -3, -3, y + 8, -3, GILDED);
            box(3, y + 1, -3, 3, y + 8, -3, GILDED);
            box(-3, y + 1, 3, -3, y + 8, 3, GILDED);
            box(3, y + 1, 3, 3, y + 8, 3, GILDED);
        }

        // ── 3. The Lava Channels ────────────────────────────────────────

        static boolean rounded(int x, int z, double r) {
            double a = Math.abs(x) / r, b = Math.abs(z) / r;
            return a * a * a + b * b * b <= 1;
        }

        void channels(int y) {
            int h = FLOOR_H[2];
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                if (!rounded(x, z, HALL + 0.5)) continue;
                // the cavern's floor, rough, and its roof, uneven
                set(x, y, z, hash(x, z, 31) < 30 ? ASH : BLACKSTONE);
                int roof = y + h - (hash(x >> 2, z >> 2, 33) < 25 ? 1 : 0);
                for (int t = y + 1; t <= roof; t++) set(x, t, z, AIR);
                if (hash(x, z, 35) < 4) for (int t = roof; t >= roof - 1 - hash(x, z, 37) % 3; t--) set(x, t, z, BASALT); // drips
            }
            // the three channels, north to south, a bridge over each, staggered
            int[][] channels = {{16, 18}, {0, -19}, {-17, 15}}; // {west edge, bridge's middle}
            for (int[] ch : channels) {
                for (int z = -HALL; z <= HALL; z++) for (int x = ch[0]; x <= ch[0] + 3; x++) {
                    if (!rounded(x, z, HALL + 0.5)) continue;
                    set(x, y, z, LAVA);
                    set(x, y - 1, z, MAGMA);
                    for (int t = y + 1; t <= y + 3; t++) if (get(x, t, z) == BASALT) set(x, t, z, AIR);
                }
                for (int z = ch[1] - 2; z <= ch[1] + 2; z++) for (int x = ch[0] - 1; x <= ch[0] + 4; x++) {
                    set(x, y, z, DARK);
                    if (Math.abs(z - ch[1]) == 2) set(x, y + 1, z, WALL);
                }
            }
            face(y, y + h + 1, ROCK);
            // pillars of basalt banded with magma, out of the islands
            int[][] pillars = {{26, -10}, {25, 14}, {8, 6}, {9, -26}, {-8, -6}, {-9, 22}, {-26, -8}, {-25, 9}};
            for (int[] p : pillars) {
                pillar(p[0], p[1], 2, y + 1, y + h, BASALT, MAGMA, y + 5);
                set(p[0], y + 8, p[1], LAMP);
            }
            // magma running down the walls
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                if (get(x, y + 3, z) == ROCK && hash(x, z, 39) < 7 && touchesOpen(x, y + 3, z)) for (int t = y + 1; t <= y + h; t++) {
                    if (get(x, t, z) == ROCK) set(x, t, z, MAGMA);
                }
            }
        }

        boolean touchesOpen(int x, int y, int z) {
            return open(get(x + 1, y, z)) || open(get(x - 1, y, z)) || open(get(x, y, z + 1)) || open(get(x, y, z - 1));
        }

        // ── 4. The Barracks of the Six Generals ─────────────────────────

        void barracks(int y) {
            int h = FLOOR_H[3];
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                double r = Math.sqrt(x * x + z * z);
                double a = Math.toDegrees(Math.atan2(z, x));
                double sector = Math.floorMod((int) Math.round(a) + 30, 60); // 0..59, 30 at a barrack's middle
                if (r <= 18) {
                    set(x, y, z, r <= 13 ? DARK : POLISHED);
                    for (int t = y + 1; t <= y + h; t++) set(x, t, z, AIR);
                } else if (r < 20.5) {
                    // the wall round the yard, a doorway into each barrack
                    boolean doorway = Math.abs(sector - 30) * Math.PI / 180 * r <= 1.6;
                    set(x, y, z, POLISHED);
                    for (int t = y + 1; t <= y + h; t++) set(x, t, z, doorway && t <= y + 4 ? AIR : BRICK);
                } else {
                    // the barracks, walled from each other
                    boolean wall = Math.min(sector, 60 - sector) * Math.PI / 180 * r < 1.1;
                    set(x, y, z, BRICK);
                    for (int t = y + 1; t <= y + h; t++) set(x, t, z, wall ? BRICK : AIR);
                    if (!wall && r >= 26 && r <= 30 && Math.floorMod((int) Math.floor(sector), 8) < 3) set(x, y + 1, z, WOOL); // bunks
                    if (!wall && r > 31 && Math.floorMod((int) Math.floor(sector), 6) == 0) {
                        set(x, y + 1, z, FENCE);
                        set(x, y + 2, z, FENCE); // racks of spears
                    }
                }
            }
            face(y, y + h + 1, BRICK);
            ceiling(y, h, NETHER, DARK);
            // the yard: a platform, and the six generals' standards round it
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) if (x * x + z * z <= 10) set(x, y + 1, z, POLISHED);
            set(0, y + 1, 0, GILDED);
            for (int k = 0; k < 6; k++) {
                double a = Math.toRadians(k * 60);
                int x = (int) Math.round(Math.cos(a) * 9), z = (int) Math.round(Math.sin(a) * 9);
                for (int t = y + 1; t <= y + 6; t++) set(x, t, z, FENCE);
                set(x, y + 7, z, GILDED);
                int ox = Math.abs(x) >= Math.abs(z) ? Integer.signum(x) : 0, oz = ox == 0 ? Integer.signum(z) : 0;
                for (int t = y + 3; t <= y + 6; t++) set(x + ox, t, z + oz, WOOL);
            }
            // a lantern in each barrack
            for (int k = 0; k < 6; k++) {
                double a = Math.toRadians(k * 60);
                set((int) Math.round(Math.cos(a) * 25), y + 1, (int) Math.round(Math.sin(a) * 25), LANTERN);
            }
        }

        // ── 5. The Kitchen of the Captives ──────────────────────────────

        void kitchen(int y) {
            int h = FLOOR_H[4];
            room(-HALL, -28, HALL, HALL, y, h, BRICK);
            face(y, y + h + 1, NETHER);
            ceiling(y, h, NETHER, DARK);
            for (int x = -HALL; x <= HALL; x++) for (int z = -28; z <= HALL; z++) if (hash(x, z, 51) < 15) set(x, y, z, CRACKED);
            // the great steamer over its fire pit: four baskets of bamboo, one on another, hung on chains
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > 5.4) continue;
                set(x, y, z, MAGMA);
                if (r <= 4.4 && (x + z) % 2 == 0 && Math.abs(x) % 2 == 1) set(x, y + 1, z, SMOKE);
                if (r > 4.4) set(x, y + 1, z, WALL);
                for (int tier = 0; tier < 3; tier++) {
                    int base = y + 3 + tier * 2;
                    double rr = 4.6 - tier * 0.7;
                    if (r <= rr) set(x, base, z, r > rr - 1 ? BAMBOO : SLAB);
                    if (r <= rr && r > rr - 1) set(x, base + 1, z, BAMBOO);
                }
                if (r <= 2.6) set(x, y + 9, z, BAMBOO);
            }
            set(0, y + 10, 0, CHAIN);
            set(0, y + 9, 0, GILDED);
            // cages of iron along the north wall, the captives' bones in some
            for (int c = 0; c < 6; c++) {
                int x0 = -30 + c * 10, z0 = -28;
                for (int x = x0; x <= x0 + 6; x++) for (int z = z0; z <= z0 + 5; z++) for (int t = y + 1; t <= y + 5; t++) {
                    boolean edge = x == x0 || x == x0 + 6 || z == z0 + 5;
                    if (t == y + 5) set(x, t, z, NETHER);
                    else if (edge) set(x, t, z, (x == x0 || x == x0 + 6) && z != z0 + 5 ? BARS_NS : BARS_EW);
                }
                if (c % 2 == 0) {
                    set(x0 + 2, y + 1, z0 + 1, BONE);
                    set(x0 + 3, y + 1, z0 + 2, BONE);
                }
                set(x0 + 3, y + 4, z0 + 2, LANTERN_HUNG);
            }
            // the cooks' benches: smokers and cauldrons down both side walls, bones by them
            for (int z = -14; z <= 30; z += 4) for (int sx : new int[]{-32, 32}) {
                set(sx, y + 1, z, SMOKER);
                set(sx, y + 1, z + 1, CAULDRON);
                set(sx, y + 1, z + 2, BLAST);
                if (hash(sx, z, 53) < 40) set(sx + (sx < 0 ? 2 : -2), y + 1, z + 1, BONE);
            }
            // hooks hung from the roof
            for (int x = -24; x <= 24; x += 12) for (int z = -12; z <= 24; z += 12) {
                if (Math.abs(x) < 8 && Math.abs(z) < 8) continue;
                for (int t = y + 6; t <= y + h; t++) set(x, t, z, CHAIN);
                set(x, y + 5, z, BARS);
            }
            // two rows of red pillars down the hall, lamps in them
            for (int z = -14; z <= 22; z += 12) for (int sx : new int[]{-21, 19}) {
                pillar(sx, z, 2, y + 1, y + h, BRICK, CHISELED, y + 1);
                set(sx, y + 6, z, LAMP);
                set(sx + 1, y + 6, z + 1, LAMP);
            }
            // great cauldrons of the broth, steaming
            for (int[] p : new int[][]{{-16, 14}, {16, 14}, {-16, -12}, {16, -12}}) {
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    set(p[0] + dx, y, p[1] + dz, MAGMA);
                    set(p[0] + dx, y + 1, p[1] + dz, dx == 0 && dz == 0 ? SMOKE : CAULDRON);
                }
                set(p[0], y, p[1], HAY);
            }
        }

        // ── 6. The Smoke Galleries ──────────────────────────────────────

        void galleries(int y) {
            int h = FLOOR_H[5];
            int n = 11, pitch = 6, first = -30;
            // a maze of galleries, the same every time: a tree of the cells, a few loops opened in it
            boolean[][] east = new boolean[n][n], south = new boolean[n][n];
            boolean[][] seen = new boolean[n][n];
            Random r = new Random(0xF1EC1D0DL);
            ArrayDeque<int[]> stack = new ArrayDeque<>();
            stack.push(new int[]{n / 2, n / 2});
            seen[n / 2][n / 2] = true;
            while (!stack.isEmpty()) {
                int[] c = stack.peek();
                List<int[]> next = new ArrayList<>();
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int i = c[0] + d[0], j = c[1] + d[1];
                    if (i >= 0 && i < n && j >= 0 && j < n && !seen[i][j]) next.add(new int[]{i, j, d[0], d[1]});
                }
                if (next.isEmpty()) {
                    stack.pop();
                    continue;
                }
                int[] go = next.get(r.nextInt(next.size()));
                if (go[2] == 1) east[c[0]][c[1]] = true;
                else if (go[2] == -1) east[go[0]][go[1]] = true;
                else if (go[3] == 1) south[c[0]][c[1]] = true;
                else south[go[0]][go[1]] = true;
                seen[go[0]][go[1]] = true;
                stack.push(new int[]{go[0], go[1]});
            }
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                if (r.nextInt(100) < 12) east[i][j] = true;
                if (r.nextInt(100) < 12) south[i][j] = true;
            }
            box(-HALL, y, -HALL, HALL, y, HALL, DARK);
            box(-HALL, y + 1, -HALL, HALL, y + h, HALL, NETHER);
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                int cx = first + i * pitch, cz = first + j * pitch;
                box(cx - 2, y + 1, cz - 2, cx + 1, y + h - 2, cz + 1, AIR);
                if (east[i][j] && i + 1 < n) box(cx + 2, y + 1, cz - 2, cx + 3, y + h - 3, cz + 1, AIR);
                if (south[i][j] && j + 1 < n) box(cx - 2, y + 1, cz + 2, cx + 1, y + h - 3, cz + 3, AIR);
                int ways = (east[i][j] && i + 1 < n ? 1 : 0) + (south[i][j] && j + 1 < n ? 1 : 0)
                        + (i > 0 && east[i - 1][j] ? 1 : 0) + (j > 0 && south[i][j - 1] ? 1 : 0);
                // smoke in the dead ends, a lantern now and then at a crossing
                if (ways == 1 && hash(i, j, 61) < 70) {
                    set(cx, y, cz, HAY);
                    set(cx, y + 1, cz, SMOKE);
                } else if (ways >= 3 && hash(i, j, 63) < 45) set(cx - 1, y + 1, cz - 1, LANTERN);
                if (hash(i, j, 65) < 20) set(cx, y, cz - 1, MAGMA);
            }
            // the galleries' walls of dark brick, banded
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) for (int t = y + 1; t <= y + h; t++) {
                if (get(x, t, z) == NETHER && (t - y) % 4 == 0 && touchesOpen(x, t, z)) set(x, t, z, CRACKED);
            }
        }

        // ── 7. The Hall of the Five Carts ───────────────────────────────

        void carts(int y) {
            int h = FLOOR_H[6];
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > 31.5) continue;
                set(x, y, z, r > 29 ? POLISHED : DARK);
                int roof = y + h - (r > 24 ? 2 : 0);
                for (int t = y + 1; t <= roof; t++) set(x, t, z, AIR);
            }
            face(y, y + h + 1, BRICK);
            ceiling(y, h, DARK, BRICK);
            int[][] at = new int[5][];
            for (int k = 0; k < 5; k++) {
                double a = Math.toRadians(-90 + 72 * k);
                at[k] = new int[]{(int) Math.round(Math.cos(a) * 19), (int) Math.round(Math.sin(a) * 19)};
            }
            // the five-element pentagon in magma, the star within it in gold
            for (int k = 0; k < 5; k++) {
                line(at[k], at[(k + 1) % 5], y, MAGMA);
                line(at[k], at[(k + 2) % 5], y, GILDED);
            }
            // the well of fire in the middle
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= 3.2) set(x, y, z, LAVA);
                else if (r <= 4.3) set(x, y + 1, z, WALL);
                else if (r <= 5.3) set(x, y, z, CHISELED);
            }
            // the carts, each on its dais: a body of brick, its fire on top, a great wheel either side
            for (int k = 0; k < 5; k++) {
                int cx = at[k][0], cz = at[k][1];
                box(cx - 3, y + 1, cz - 3, cx + 3, y + 1, cz + 3, POLISHED);
                boolean alongX = Math.abs(cx) < Math.abs(cz); // its wheels turn toward the middle
                box(cx - 1, y + 2, cz - 1, cx + 1, y + 3, cz + 1, NETHER);
                set(cx, y + 4, cz, CAMPFIRE);
                set(cx, y + 3, cz, MAGMA);
                for (int s : new int[]{-2, 2}) for (int u = -2; u <= 2; u++) for (int v = 0; v <= 4; v++) {
                    int du = u, dv = v - 2;
                    if (du * du + dv * dv > 5) continue;
                    int x = alongX ? cx + u : cx + s, z = alongX ? cz + s : cz + u;
                    set(x, y + 2 + v, z, du == 0 && dv == 0 ? GILDED : du * du + dv * dv >= 4 ? BLACKSTONE : CHAIN);
                }
                // a lamp over each
                for (int t = y + 8; t <= y + h; t++) set(cx, t, cz, CHAIN);
                set(cx, y + 7, cz, LANTERN_HUNG);
            }
            // a ring of pillars round the hall
            for (int k = 0; k < 12; k++) {
                double a = Math.toRadians(k * 30 + 15);
                int px = (int) Math.round(Math.cos(a) * 27) - 1, pz = (int) Math.round(Math.sin(a) * 27) - 1;
                pillar(px, pz, 2, y + 1, y + h, BRICK, LAMP, y + 6);
            }
        }

        /** A line two blocks wide of {@code c} in the floor at y, from a to b. */
        void line(int[] a, int[] b, int y, byte c) {
            int steps = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
            for (int s = 0; s <= steps; s++) {
                int x = (int) Math.round(a[0] + (b[0] - a[0]) * s / (double) steps), z = (int) Math.round(a[1] + (b[1] - a[1]) * s / (double) steps);
                set(x, y, z, c);
                set(x + 1, y, z, c);
            }
        }

        // ── 8. The Magma Cistern ────────────────────────────────────────

        void cistern(int y) {
            int h = FLOOR_H[7];
            for (int x = -HALL; x <= HALL; x++) for (int z = -HALL; z <= HALL; z++) {
                if (!rounded(x, z, HALL + 0.5)) continue;
                set(x, y, z, LAVA);
                set(x, y - 1, z, MAGMA);
                for (int t = y + 1; t <= y + h; t++) set(x, t, z, AIR);
            }
            // the landings at either end, the islands, and the causeway winding between them
            platform(2, -33, 16, -26, y);
            platform(10, 24, 30, 33, y);
            int[][] islands = {{-8, -14, 4}, {12, -2, 4}, {-10, 12, 4}, {24, -18, 3}, {-24, 0, 3}, {24, 14, 3}};
            for (int[] is : islands) disc(is[0], is[1], is[2], y, DARK);
            int[][] way = {{9, -27}, {-8, -14}, {12, -2}, {-10, 12}, {16, 26}};
            for (int k = 0; k + 1 < way.length; k++) causeway(way[k], way[k + 1], y);
            face(y, y + h + 1, ROCK);
            // pillars rising out of the lava, banded with magma, chains hung between
            int[][] pillars = {{-20, -22}, {20, -6}, {-22, 20}, {2, 8}, {-2, -24}, {26, 2}, {-28, -10}};
            for (int[] p : pillars) {
                pillar(p[0] - 1, p[1] - 1, 3, y, y + h, BASALT, MAGMA, y + 4);
                set(p[0], y + 7, p[1] - 1, LAMP);
                set(p[0], y + 7, p[1] + 1, LAMP);
            }
            for (int[] is : islands) {
                for (int t = y + 6; t <= y + h; t++) set(is[0], t, is[1], CHAIN);
                set(is[0], y + 5, is[1], LANTERN_HUNG);
            }
        }

        void platform(int x0, int z0, int x1, int z1, int y) {
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (get(x, y, z) == LAVA) set(x, y, z, DARK);
        }

        void disc(int cx, int cz, int r, int y, byte c) {
            for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r + 1) set(x, y, z, c);
            }
        }

        /** A causeway three blocks wide over the lava from a to b, edged with polished stone. */
        void causeway(int[] a, int[] b, int y) {
            int steps = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])) * 2;
            for (int s = 0; s <= steps; s++) {
                double x = a[0] + (b[0] - a[0]) * s / (double) steps, z = a[1] + (b[1] - a[1]) * s / (double) steps;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    int px = (int) Math.round(x) + dx, pz = (int) Math.round(z) + dz;
                    if (get(px, y, pz) == LAVA) set(px, y, pz, dx == 0 && dz == 0 ? DARK : POLISHED);
                }
            }
        }

        // ── The throne room ─────────────────────────────────────────────

        void throneRoom() {
            int y = THRONE_Y, h = THRONE_H;
            for (int x = -THRONE_X; x <= THRONE_X; x++) for (int z = THRONE_N; z <= THRONE_S; z++) {
                int ax = Math.abs(x);
                // a high nave over the middle, the vault stepping down to the aisles
                int roof = ax <= 11 ? y + h : ax <= 15 ? y + h - 2 : ax <= 19 ? y + h - 4 : y + h - 6;
                set(x, y, z, Math.floorMod(x + z, 2) == 0 && ax <= 11 ? DARK : BRICK);
                for (int t = y + 1; t <= roof; t++) set(x, t, z, AIR);
            }
            face(y, y + h + 1, DARK);
            ceiling(y, h, DARK, NETHER);
            // the arena round his dais: gold and black, open for his carts and his fire
            for (int x = -ARENA; x <= ARENA; x++) for (int z = -ARENA; z <= ARENA; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > ARENA + 0.4) continue;
                set(x, y, z, r <= 2.5 ? GILDED : r > ARENA - 0.6 ? CHISELED : Math.floorMod(x + z, 2) == 0 ? POLISHED : DARK);
            }
            // lamps let into the floor round it, and further out between the pillars
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8;
                set((int) Math.round(Math.cos(a) * ARENA), y, (int) Math.round(Math.sin(a) * ARENA), LAMP);
            }
            for (int k = 0; k < 12; k++) {
                double a = k * Math.PI / 6 + Math.PI / 12;
                int x = (int) Math.round(Math.cos(a) * 14), z = (int) Math.round(Math.sin(a) * 14);
                if (Math.abs(x) > 1) set(x, y, z, LAMP);
            }
            // the carpet from his doors to the arena
            for (int z = ARENA + 1; z <= THRONE_S; z++) for (int x = -1; x <= 1; x++) set(x, y + 1, z, CARPET);
            // the throne's steps, the throne, braziers either side, his treasury behind
            for (int x = -9; x <= 9; x++) for (int z = THRONE_N; z <= -16; z++) {
                int ax = Math.abs(x);
                set(x, y + 1, z, z == -16 ? STAIR_N : POLISHED);
                if (ax <= 7 && z <= -19) set(x, y + 2, z, z == -19 ? STAIR_N : POLISHED);
                if (ax <= 5 && z <= -22) set(x, y + 3, z, z == -22 ? STAIR_N : GILDED);
            }
            // the throne: its seat, arms of black stone, a high back of gold stepping up to a crest of fire
            set(0, y + 4, THRONE_Z, STAIR_N);
            set(-1, y + 4, THRONE_Z, SLAB);
            set(1, y + 4, THRONE_Z, SLAB);
            for (int sx : new int[]{-2, 2}) {
                set(sx, y + 4, THRONE_Z, CHISELED);
                set(sx, y + 5, THRONE_Z, MAGMA);
            }
            box(-2, y + 4, THRONE_Z - 1, 2, y + 7, THRONE_Z - 1, GILDED);
            box(-1, y + 8, THRONE_Z - 1, 1, y + 9, THRONE_Z - 1, GILDED);
            set(0, y + 10, THRONE_Z - 1, GILDED);
            for (int sx : new int[]{-2, 2}) set(sx, y + 8, THRONE_Z - 1, MAGMA);
            set(0, y + 8, THRONE_Z - 1, LAMP);
            set(0, y + 6, THRONE_Z - 1, MAGMA);
            for (int sx : new int[]{-4, 4}) {
                set(sx, y + 4, THRONE_Z, MAGMA);
                set(sx, y + 5, THRONE_Z, CAMPFIRE);
            }
            set(3, y + 4, THRONE_Z - 2, CHEST);
            // the flame on the wall behind the throne, magma and gold
            for (int x = -12; x <= 12; x++) for (int t = y + 4; t <= y + h; t++) {
                double flame = 12 - Math.abs(x) * 0.7 + 2.5 * Math.sin(x * 0.9);
                int up = t - y - 4;
                if (up > flame) continue;
                set(x, t, THRONE_N - 1, up > flame - 2 ? GILDED : (x + up) % 3 == 0 ? LAMP : MAGMA);
            }
            // two rows of great red pillars, gold at their heads and lamps in them
            for (int z = -22; z <= 22; z += 8) for (int sx : new int[]{-18, 16}) {
                pillar(sx, z - 1, 3, y + 1, y + h - 4, BRICK, CHISELED, y + 1);
                box(sx, y + h - 5, z - 1, sx + 2, y + h - 5, z + 1, GILDED);
                set(sx + 1, y + 8, z - 1, LAMP);
                set(sx + 1, y + 8, z + 1, LAMP);
            }
            // the moats of lava down the side aisles, a rail of black stone along them
            for (int z = THRONE_N + 4; z <= THRONE_S - 4; z++) for (int sx : new int[]{-1, 1}) {
                for (int ax = 21; ax <= 24; ax++) {
                    set(sx * ax, y, z, LAVA);
                    set(sx * ax, y - 1, z, MAGMA);
                }
                set(sx * 20, y + 1, z, WALL);
            }
            // banners of red down the walls, lanterns over the arena on chains
            for (int z = -20; z <= 20; z += 10) for (int sx : new int[]{-THRONE_X, THRONE_X}) {
                for (int t = y + 3; t <= y + 10; t++) set(sx, t, z, WOOL);
            }
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int x = (int) Math.round(Math.cos(a) * 10), z = (int) Math.round(Math.sin(a) * 10);
                for (int t = y + 9; t <= y + h; t++) set(x, t, z, CHAIN);
                set(x, y + 8, z, LANTERN_HUNG);
            }
            // lamps let into the nave's vault
            for (int x = -10; x <= 10; x++) for (int z = THRONE_N; z <= THRONE_S; z++) {
                if (Math.floorMod(x, 5) != 2 || Math.floorMod(z, 5) != 2) continue;
                for (int t = y + h; t > y + 6; t--) {
                    byte over = get(x, t + 1, z);
                    if (open(get(x, t, z)) && (over == DARK || over == NETHER)) {
                        set(x, t + 1, z, LAMP);
                        break;
                    }
                }
            }
        }

        /** His doors: a gilded arch where the stair's passage comes into the throne room. */
        void throneGate() {
            int y = THRONE_Y;
            for (int x = -4; x <= 4; x++) for (int t = y + 1; t <= y + 9; t++) for (int z = THRONE_S + 1; z <= THRONE_S + 2; z++) {
                int ax = Math.abs(x);
                if (ax <= 2 && t <= y + 7) set(x, t, z, AIR);
                else set(x, t, z, ax >= 3 || t >= y + 8 ? GILDED : CHISELED);
            }
            for (int z = THRONE_S + 1; z <= THRONE_S + 2; z++) for (int x = -2; x <= 2; x++) set(x, y, z, POLISHED);
        }

        // ── The stairs ──────────────────────────────────────────────────

        /** Local (x, z) of a stair's own (x, z), it running down the east side southward, turned to its side. */
        static int[] turn(int rot, int x, int z) {
            return switch (rot) {
                case 1 -> new int[]{-z, x};
                case 2 -> new int[]{-x, -z};
                case 3 -> new int[]{z, -x};
                default -> new int[]{x, z};
            };
        }

        /** A step's facing (its high side), north in the stair's own frame, turned. */
        static byte stairFacing(int rot) {
            return switch (rot) {
                case 1 -> STAIR_E;
                case 2 -> STAIR_S;
                case 3 -> STAIR_W;
                default -> STAIR_N;
            };
        }

        void setT(int rot, int x, int y, int z, byte c) {
            int[] p = turn(rot, x, z);
            set(p[0], y, p[1], c);
        }

        byte getT(int rot, int x, int y, int z) {
            int[] p = turn(rot, x, z);
            return get(p[0], y, p[1]);
        }

        /**
         * Cuts stair {@code n} (from floor n down to the next, or the throne room): its landing at the
         * top, a doorway from its hall sealed by a grate, the flight, the landing at the bottom and
         * its doorway into the hall below. Returns the seal's box and the bottom landing's.
         */
        int[][] stair(Stair s, int n) {
            int a = s.start(), top = s.top(), bottom = s.bottom(), drop = top - bottom, r = s.rot();
            int line = STAIR_LINE;
            // the tunnel's walls of dark brick, its floor
            for (int z = a - 1; z <= a + drop + 6; z++) for (int x = line - 2; x <= line + 2; x++) {
                int floorY = z <= a + 2 ? top : z >= a + 3 + drop ? bottom : top - (z - a - 2) + 1;
                for (int y = floorY - 1; y <= floorY + 6; y++) {
                    if (Math.abs(x - line) == 2 || y == floorY + 6 || z == a - 1 || z == a + drop + 6) setT(r, x, y, z, DARK);
                }
            }
            for (int x = line - 1; x <= line + 1; x++) {
                // the top landing, the flight, the bottom landing
                for (int z = a; z <= a + 2; z++) {
                    setT(r, x, top, z, POLISHED);
                    for (int y = top + 1; y <= top + 5; y++) setT(r, x, y, z, AIR);
                }
                for (int i = 1; i <= drop; i++) {
                    int z = a + 2 + i, y = top - i + 1;
                    setT(r, x, y - 1, z, DARK);
                    setT(r, x, y, z, stairFacing(r));
                    for (int t = y + 1; t <= y + 5; t++) setT(r, x, t, z, AIR);
                }
                for (int z = a + 3 + drop; z <= a + 5 + drop; z++) {
                    setT(r, x, bottom, z, POLISHED);
                    for (int y = bottom + 1; y <= bottom + 5; y++) setT(r, x, y, z, AIR);
                }
            }
            for (int i = 2; i <= drop; i += 4) setT(r, line, top - i + 6, a + 2 + i, LANTERN_HUNG);
            setT(r, line, top + 5, a + 1, LANTERN_HUNG);
            setT(r, line, bottom + 5, a + 4 + drop, LANTERN_HUNG);
            // the doorways into the halls, cut in until they come out in one
            topMouth[n] = doorway(r, a, top);
            bottomMouth[n] = doorway(r, a + 3 + drop, bottom);
            // the seal: a grate across the top doorway, at the landing
            int[] p0 = turn(r, line - 2, a), p1 = turn(r, line - 2, a + 2);
            byte grate = r % 2 == 0 ? SEAL_NS : SEAL_EW;
            for (int z = a; z <= a + 2; z++) for (int y = top + 1; y <= top + 4; y++) setT(r, line - 2, y, z, grate);
            int[] b0 = turn(r, line - 1, a + 3 + drop), b1 = turn(r, line + 1, a + 5 + drop);
            return new int[][]{
                    {Math.min(p0[0], p1[0]), top + 1, Math.min(p0[1], p1[1]), Math.max(p0[0], p1[0]), top + 4, Math.max(p0[1], p1[1])},
                    {Math.min(b0[0], b1[0]), bottom + 1, Math.min(b0[1], b1[1]), Math.max(b0[0], b1[0]), bottom + 1, Math.max(b0[1], b1[1])}};
        }

        /**
         * A doorway three wide from a landing (its own frame: from x = line - 2 inward, z from z0) into
         * the hall at floor y: cut a step at a time until it comes out where the hall is open. Returns
         * the first open cell's middle and the way inward, local: {x, z, dx, dz}.
         */
        int[] doorway(int r, int z0, int y) {
            int[] in = turn(r, -1, 0);
            for (int x = STAIR_LINE - 2; x > -HALL; x--) {
                boolean opened = false;
                for (int z = z0; z <= z0 + 2; z++) if (open(getT(r, x, y + 1, z)) && open(getT(r, x, y + 2, z))) opened = true;
                if (opened && x < STAIR_LINE - 2) {
                    int[] at = turn(r, x, z0 + 1);
                    return new int[]{at[0], at[1], in[0], in[1]};
                }
                for (int z = z0; z <= z0 + 2; z++) {
                    if (!holds(getT(r, x, y, z)) || getT(r, x, y, z) == ROCK || getT(r, x, y, z) == UNTOUCHED) setT(r, x, y, z, DARK);
                    for (int t = y + 1; t <= y + 4; t++) if (!open(getT(r, x, t, z))) setT(r, x, t, z, AIR);
                }
                // an arch over it, and a lamp
                for (int z = z0 - 1; z <= z0 + 3; z++) if (getT(r, x, y + 5, z) == ROCK) setT(r, x, y + 5, z, CHISELED);
            }
            throw new IllegalStateException("a doorway that never came out into its hall");
        }
    }
}
