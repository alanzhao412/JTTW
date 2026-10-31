package com.jttw.engine.world.blackwind;

import java.util.ArrayList;
import java.util.List;

/**
 * The Black Wind Cave (黑风洞) as a fortress: the plan of it, worked out once, the same in every world.
 * Pure: no Minecraft in it, so it can be tested on its own; {@link BlackWindBlocks} says what block each
 * of its codes is, and the range lays it column by column ({@code BlackWindRangeFeature}), so it comes
 * out whole whichever of its chunks is made first.
 *
 * <p>Local coordinates are block offsets from the region's centre, y from the forecourt's floor before
 * the door ({@code BlackWindShape.Floors#court}); +z is south, toward the door. From the valley in:
 * <ul>
 *   <li><b>the forecourt</b> before the cliff: braziers of soul fire up the road, a pavilion where the
 *       Black Bear's two friends sit and talk, a stone stele; and its two keepers;</li>
 *   <li><b>the great door</b> ({@link #DOOR}) in a facade cut into the cliff: black-lacquered leaves banded
 *       in black iron and studded with gold, eleven wide and fifteen high, between pillars of dark tile
 *       crowned with soul fire, under a tiled eave and a gilded plaque, two crouching black bears of
 *       stone with eyes of crying obsidian before it;</li>
 *   <li>a passage thirteen wide into the peak, and <b>the Black Wind Court</b>: a great cavern some
 *       hundred blocks across under a domed roof thirty high, the bear's garrison camped in it: barracks
 *       of spruce and dark oak either side of a paved road, braziers, a training yard; across it a wall
 *       with towers and a gatehouse; behind the wall the inner court, the pavilion where the
 *       <b>Buddha-Robe Feast</b> was laid (佛衣会), its long tables; and at its back the head of the
 *       grand stair, shut by a grate its warden holds;</li>
 *   <li>the grand stair down, eighteen deep, into <b>the throne cavern</b>: two rows of basalt pillars,
 *       soul fire, a black-and-red carpet to the dais, the Black Bear's throne, the stolen kasaya's
 *       likeness hung in gold and red on the rock behind it, his treasury beside it.</li>
 * </ul>
 */
public final class BlackWindPlan {

    private BlackWindPlan() {}

    // ── Codes ───────────────────────────────────────────────────────────

    public static final byte UNTOUCHED = 0, AIR = 1, ROCK = 2, BRICK = 3, CRACKED = 4, TILE = 5, POLISHED = 6, CHISELED = 7,
            BLACKSTONE = 8, GILDED = 9, GOLD = 10, LOG = 11, PLANKS = 12, DARK_PLANKS = 13, ROOF_N = 14, ROOF_E = 15, ROOF_S = 16,
            ROOF_W = 17, SLAB = 18, FENCE = 19, BARS = 20, SEAL = 21, DOOR_LEAF = 22, DOOR_BAND = 23, DOOR_STUD = 24,
            SOUL_LANTERN_HUNG = 25, SOUL_LANTERN = 26, SOUL_FIRE = 27, CAMPFIRE = 28, CHAIN = 29, CARPET_BLACK = 30, CARPET_RED = 31,
            STAIR_N = 32, STAIR_E = 33, STAIR_S = 34, STAIR_W = 35, BASALT = 36, HAY = 37, BARREL = 38, CHEST = 39, WOOL_BLACK = 40,
            WOOL_RED = 41, TERRACOTTA = 42, COBWEB = 43, FLOOR = 44, PATH = 45, WALL = 46, BONE = 47, COAL = 48, LANTERN_HUNG = 49,
            SPRUCE_LOG = 50, CALCITE = 51, TABLE = 52, CRYING = 53, SPRUCE_FENCE = 54, DARK_SLAB = 55, SOUL_SOIL = 56, POLISHED_BLACKSTONE = 57,
            TILE_SLAB_TOP = 58, LOG_X = 59, LOG_Z = 60, YELLOW = 61, MOSS = 62, CANDLE = 63;
    public static final int CODES = 64;

    /** Whether a code is open: one walks or stands in it. */
    public static boolean open(byte c) {
        return c == AIR || c == CARPET_BLACK || c == CARPET_RED || c == COBWEB;
    }

    /** Whether a code holds one up (a block one can stand on). */
    public static boolean holds(byte c) {
        return c != UNTOUCHED && !open(c) && c != SOUL_LANTERN_HUNG && c != LANTERN_HUNG && c != CHAIN && c != CAMPFIRE && c != SOUL_FIRE
                && c != CANDLE;
    }

    /** Whether a code is the door's: what the door opening takes away. */
    public static boolean door(byte c) {
        return c == DOOR_LEAF || c == DOOR_BAND || c == DOOR_STUD;
    }

    /** Whether a code is the grate's over the grand stair. */
    public static boolean seal(byte c) {
        return c == SEAL;
    }

    // ── The frame ───────────────────────────────────────────────────────

    /** The plan's extent, local. */
    public static final int X_MIN = -54, X_MAX = 54, Z_MIN = -112, Z_MAX = 98, Y_MIN = -24, Y_MAX = 44;
    private static final int SX = X_MAX - X_MIN + 1, SZ = Z_MAX - Z_MIN + 1, SY = Y_MAX - Y_MIN + 1;

    /** The cliff's face (local z): the facade's front; the valley's forecourt begins past it. */
    public static final int FACE_Z = 56;
    /** The door: its leaves' local box, x0, y0, z0, x1, y1, z1 (two thick, in the passage's mouth). */
    public static final int[] DOOR = {-5, 1, 52, 5, 15, 53};
    /** The passage: its half-width, and from where to where it runs (local z). */
    public static final int PASSAGE_HALF = 6, PASSAGE_N = 24, PASSAGE_S = 53;

    /** The Black Wind Court, the great cavern: its middle (local z), half-width, half-depth and height. */
    public static final int COURT_Z = -10, COURT_RX = 50, COURT_RZ = 36, COURT_H = 30;
    /** The inner wall across the court, its local z; the feast pavilion behind it. */
    public static final int INNER_WALL_Z = -18;
    /** The grand stair: its head (local z, the court's floor), and its grate across it there. */
    public static final int STAIR_HEAD_Z = -44, STAIR_HALF = 4;
    public static final int[] SEAL_BOX = {-STAIR_HALF, 1, -43, STAIR_HALF, 7, -43};

    /** The throne cavern: its floor (local y), middle (local z), half-width, half-depth and height. */
    public static final int THRONE_Y = -18, THRONE_Z = -82, THRONE_RX = 30, THRONE_RZ = 24, THRONE_H = 26;
    /** Where the throne stands (local z), and the dais's front. */
    public static final int SEAT_Z = -102, DAIS_FRONT = -96;
    /** The open floor round the Black Bear's post, where he fights: clear of pillars this far round. */
    public static final int ARENA = 11;

    // ── The garrison ────────────────────────────────────────────────────

    public static final int IMP = 0, BOAR = 1, BEAR = 2, IMP_CAPTAIN = 3, BOAR_CAPTAIN = 4, BEAR_CAPTAIN = 5, LINGXUZI = 6,
            SCHOLAR = 7, WARDEN = 8;

    /** One of a floor's garrison: where it stands (local; y its feet), what it is. */
    public record Spot(int x, int y, int z, int kind) {

        public boolean captain() {
            return kind >= IMP_CAPTAIN;
        }
    }

    /**
     * A part of the cave the living come to: 0 the forecourt (its two keepers and their escort), 1 the
     * Black Wind Court (its garrison and the stair's warden), 2 the throne cavern (the Black Bear alone).
     */
    public record Floor(int number, String name, List<Spot> garrison, List<Spot> guards) {}

    /** The forecourt's keepers' posts, Lingxuzi's and the White-Robed Scholar's, before the door. */
    public static final int[][] KEEPER_POSTS = {{-7, 66}, {7, 66}};
    /** The warden of the grand stair's post, before its grate. */
    public static final int[] WARDEN_POST = {0, 1, -36};

    /** The level the members of each floor are about: regulars, captains, and its guards (the keepers, the warden). */
    public static int regularLevel(int floor) {
        return floor == 0 ? 22 : 25;
    }

    public static int captainLevel(int floor) {
        return floor == 0 ? 28 : 30;
    }

    public static int guardLevel(int floor) {
        return floor == 0 ? 30 : 33;
    }

    private static volatile Built built;

    public static Built get() {
        Built b = built;
        if (b == null) {
            synchronized (BlackWindPlan.class) {
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

    /** Whether local (x, z) is inside the mountain over the plan (north of the cliff's face), where the peak must stand over it. */
    public static boolean underMountain(int x, int z) {
        return covers(x, z) && z <= FACE_Z;
    }

    /** Whether local (x, y, z) lies in the Black Wind Court's cavern (its open air, its floor's level and over). */
    public static boolean inCourt(int x, int y, int z) {
        double e = sq(x / (double) COURT_RX) + sq((z - COURT_Z) / (double) COURT_RZ);
        return e < 1 && y >= 0 && y <= roof(COURT_H, e, x, z);
    }

    /** Whether local (x, y, z) lies in the throne cavern. */
    public static boolean inThrone(int x, int y, int z) {
        double e = sq(x / (double) THRONE_RX) + sq((z - THRONE_Z) / (double) THRONE_RZ);
        return e < 1 && y >= THRONE_Y && y <= THRONE_Y + roof(THRONE_H, e, x, z);
    }

    /** The floor (0 forecourt, 1 court, 2 throne cavern) local (x, y, z) is on, or -1 if none: the stair is the court's. */
    public static int floorAt(int x, int y, int z) {
        if (z > PASSAGE_S && z <= Z_MAX + 30 && Math.abs(x) <= 44 && y >= -4 && y <= 24) return 0;
        if (inThrone(x, y, z) && z <= STAIR_HEAD_Z - 16) return 2;
        if (Math.abs(x) <= COURT_RX + 2 && z >= STAIR_HEAD_Z - 20 && z <= PASSAGE_S && y >= THRONE_Y - 2 && y <= COURT_H + 6) return 1;
        if (inThrone(x, y, z)) return 2;
        return -1;
    }

    private static double sq(double v) {
        return v * v;
    }

    /** A little roughness, the same everywhere for the same (x, z). */
    static int rough(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        return (int) Math.floorMod(h ^ (h >>> 29), 1000L);
    }

    /** A cavern's roof over a point {@code e} of the way out (0 its middle, 1 its rim): a dome, roughened. */
    static double roof(int height, double e, int x, int z) {
        double coarse = (rough(x >> 2, z >> 2, 5) - 500) / 500.0 * 2.2;
        return 3 + (height - 3) * Math.sqrt(Math.max(0, 1 - e)) + coarse;
    }

    // ── The built plan ──────────────────────────────────────────────────

    public static final class Built {
        private final byte[] cells;
        private final List<Floor> floors;

        Built(byte[] cells, List<Floor> floors) {
            this.cells = cells;
            this.floors = floors;
        }

        public byte code(int x, int y, int z) {
            if (x < X_MIN || x > X_MAX || y < Y_MIN || y > Y_MAX || z < Z_MIN || z > Z_MAX) return UNTOUCHED;
            return cells[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)];
        }

        public List<Floor> floors() {
            return floors;
        }

        public Floor floor(int n) {
            return floors.get(n);
        }

        /** Whether one may stand at local (x, y, z): open there and over it, something under it that holds. */
        public boolean standable(int x, int y, int z) {
            return open(code(x, y, z)) && open(code(x, y + 1, z)) && holds(code(x, y - 1, z));
        }
    }

    private static final class Builder {
        private final byte[] c = new byte[SX * SY * SZ];

        private int at(int x, int y, int z) {
            return ((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN);
        }

        void set(int x, int y, int z, byte code) {
            if (x < X_MIN || x > X_MAX || y < Y_MIN || y > Y_MAX || z < Z_MIN || z > Z_MAX) return;
            c[at(x, y, z)] = code;
        }

        byte get(int x, int y, int z) {
            if (x < X_MIN || x > X_MAX || y < Y_MIN || y > Y_MAX || z < Z_MIN || z > Z_MAX) return UNTOUCHED;
            return c[at(x, y, z)];
        }

        void box(int x0, int y0, int z0, int x1, int y1, int z1, byte code) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, code);
        }

        /** Sets the cell only where it is air (furnishing an open room). */
        void put(int x, int y, int z, byte code) {
            if (get(x, y, z) == AIR) set(x, y, z, code);
        }

        Built build() {
            // the mountain's rock over all the plan north of the cliff; the forecourt is the valley's (untouched)
            for (int x = X_MIN; x <= X_MAX; x++) for (int z = Z_MIN; z <= FACE_Z; z++) for (int y = Y_MIN; y <= Y_MAX; y++) set(x, y, z, ROCK);
            court();
            passage();
            facade();
            forecourt();
            stair();
            throne();
            List<Floor> floors = new ArrayList<>();
            floors.add(new Floor(0, "the forecourt of the Black Wind Cave", forecourtGarrison(),
                    List.of(new Spot(KEEPER_POSTS[0][0], 1, KEEPER_POSTS[0][1], LINGXUZI), new Spot(KEEPER_POSTS[1][0], 1, KEEPER_POSTS[1][1], SCHOLAR))));
            floors.add(new Floor(1, "the Black Wind Court", courtGarrison(), List.of(new Spot(WARDEN_POST[0], WARDEN_POST[1], WARDEN_POST[2], WARDEN))));
            floors.add(new Floor(2, "the throne cavern of the Black Wind King", List.of(), List.of()));
            return new Built(c, floors);
        }

        // ── The Black Wind Court ────────────────────────────────────────

        void court() {
            for (int x = -COURT_RX; x <= COURT_RX; x++) for (int z = COURT_Z - COURT_RZ; z <= COURT_Z + COURT_RZ; z++) {
                double e = sq(x / (double) COURT_RX) + sq((z - COURT_Z) / (double) COURT_RZ);
                if (e >= 1) continue;
                int top = (int) Math.floor(roof(COURT_H, e, x, z));
                set(x, 0, z, FLOOR);
                for (int y = 1; y <= top; y++) set(x, y, z, AIR);
                // stalactites hang from the roof here and there, a few long
                if (rough(x, z, 11) < 25 && top > 8) {
                    int len = 1 + rough(x, z, 12) % (rough(x, z, 13) < 300 ? 5 : 2);
                    for (int y = top; y > top - len; y--) set(x, y, z, BASALT);
                }
            }
            // the road from the passage to the inner gate, and on to the stair's head
            for (int z = STAIR_HEAD_Z + 1; z <= COURT_Z + COURT_RZ; z++) for (int x = -3; x <= 3; x++) set(x, 0, z, Math.abs(x) == 3 ? POLISHED : PATH);
            // great columns of basalt from floor to roof, where the cavern is broad
            int[][] columns = {{-38, -2}, {38, -2}, {-30, 14}, {30, 14}, {-40, -24}, {40, -24}, {-16, 22}, {16, 22}};
            for (int[] col : columns) column(col[0], col[1]);
            // the barracks, two either side of the road
            for (int side = -1; side <= 1; side += 2) {
                barracks(side, -12, -3);
                barracks(side, 4, 13);
            }
            // braziers up the road, and the training yard's dummies and racks
            for (int z = 20; z >= -14; z -= 10) {
                brazier(-6, z);
                brazier(6, z);
            }
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 3; k++) {
                    int x = side * (9 + k * 2), z = 17 + (k % 2);
                    put(x, 1, z, SPRUCE_FENCE);
                    put(x, 2, z, HAY);
                }
                for (int z = -1; z <= 1; z++) put(side * 10, 1, z, SPRUCE_FENCE);
                put(side * 10, 2, 0, SPRUCE_FENCE);
                put(side * 9, 1, -1, BARREL);
                put(side * 9, 1, 1, BARREL);
            }
            // lanterns hung from the roof on chains
            for (int x = -40; x <= 40; x += 16) for (int z = COURT_Z - 26; z <= COURT_Z + 26; z += 13) hang(x + ((z / 13) % 2) * 8, z, 3 + rough(x, z, 21) % 4);
            innerWall();
            feast();
            // the cavern's edges: rubble, bones and webs
            for (int x = -COURT_RX; x <= COURT_RX; x++) for (int z = COURT_Z - COURT_RZ; z <= COURT_Z + COURT_RZ; z++) {
                double e = sq(x / (double) COURT_RX) + sq((z - COURT_Z) / (double) COURT_RZ);
                if (e < 0.82 || e >= 1 || get(x, 1, z) != AIR) continue;
                int r = rough(x, z, 31);
                if (r < 40) put(x, 1, z, BONE);
                else if (r < 70) put(x, 1, z, COBWEB);
                else if (r < 110) set(x, 0, z, SOUL_SOIL);
            }
        }

        void column(int cx, int cz) {
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
                for (int y = 1; y <= Y_MAX; y++) {
                    if (get(x, y, z) != AIR) break;
                    set(x, y, z, BASALT);
                }
            }
            // a footing
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) if (get(x, 1, z) == AIR) set(x, 1, z, BLACKSTONE);
        }

        void hang(int x, int z, int len) {
            int top = -1;
            for (int y = 1; y <= Y_MAX; y++) if (get(x, y, z) == AIR) top = y; else if (top > 0) break;
            if (top < len + 6) return;
            for (int y = top; y > top - len; y--) set(x, y, z, CHAIN);
            set(x, top - len, z, SOUL_LANTERN_HUNG);
        }

        void brazier(int x, int z) {
            put(x, 1, z, BRICK);
            put(x, 2, z, CHISELED);
            put(x, 3, z, SOUL_FIRE);
        }

        /** A barracks hall, its long side toward the road: x from 14 to 32 (by {@code side}), z0..z1. */
        void barracks(int side, int z0, int z1) {
            int xa = side * 14, xb = side * 32;
            int x0 = Math.min(xa, xb), x1 = Math.max(xa, xb);
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                set(x, 0, z, DARK_PLANKS);
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = 1; y <= 4; y++) {
                    if (!wall) set(x, y, z, AIR);
                    else {
                        boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                        boolean post = (x - x0) % 6 == 0 && (z == z0 || z == z1);
                        set(x, y, z, corner || post ? LOG : y == 3 && (x - x0) % 6 == 3 ? SPRUCE_FENCE : PLANKS);
                    }
                }
                // the roof: a slab course, then a ridge of tiles along its length
                set(x, 5, z, z == z0 || z == z1 ? SLAB : TILE);
                if (z > z0 + 1 && z < z1 - 1) set(x, 6, z, z == (z0 + z1) / 2 ? TILE : SLAB);
            }
            // the door, toward the road, and straw and barrels inside
            int door = side > 0 ? x0 : x1;
            int mid = (z0 + z1) / 2;
            for (int z = mid - 1; z <= mid + 1; z++) for (int y = 1; y <= 3; y++) set(door, y, z, AIR);
            for (int x = x0 + 2; x <= x1 - 2; x += 3) {
                set(x, 1, z0 + 1, HAY);
                set(x, 1, z1 - 1, HAY);
            }
            set(side > 0 ? x1 - 1 : x0 + 1, 1, mid, BARREL);
            set(side > 0 ? x1 - 1 : x0 + 1, 2, mid, BARREL);
            set((x0 + x1) / 2, 4, mid, LANTERN_HUNG);
        }

        /** The wall across the court, its towers and its gatehouse. */
        void innerWall() {
            int z0 = INNER_WALL_Z - 1, z1 = INNER_WALL_Z + 1;
            for (int x = -46; x <= 46; x++) {
                if (!inCourt(x, 1, INNER_WALL_Z)) continue;
                for (int z = z0; z <= z1; z++) for (int y = 1; y <= 8; y++) if (inCourt(x, y, z)) set(x, y, z, y == 8 ? CHISELED : BRICK);
                if (x % 2 == 0 && inCourt(x, 9, INNER_WALL_Z)) set(x, 9, z1, BRICK); // crenellations
                if (x % 2 == 0 && inCourt(x, 9, INNER_WALL_Z)) set(x, 9, z0, BRICK);
            }
            for (int tx : new int[]{-30, 30}) tower(tx, INNER_WALL_Z, 12);
            // the gatehouse: taller, its arch seven wide and seven high, a roof of tiles
            for (int x = -7; x <= 7; x++) for (int z = z0 - 1; z <= z1 + 1; z++) for (int y = 1; y <= 11; y++) {
                boolean arch = Math.abs(x) <= 3 && y <= 7 - (Math.abs(x) == 3 ? 1 : 0);
                set(x, y, z, arch ? AIR : y == 11 ? CHISELED : Math.abs(x) == 7 ? TILE : BRICK);
            }
            for (int x = -8; x <= 8; x++) {
                set(x, 12, z0 - 2, ROOF_S);
                set(x, 12, z1 + 2, ROOF_N);
                for (int z = z0 - 1; z <= z1 + 1; z++) set(x, 12, z, TILE);
                set(x, 13, INNER_WALL_Z, SLAB);
            }
            for (int x : new int[]{-5, 5}) {
                set(x, 10, z1 + 2, SOUL_LANTERN_HUNG);
                set(x, 10, z0 - 2, SOUL_LANTERN_HUNG);
            }
            // black hangings either side of the arch
            for (int x : new int[]{-5, -4, 4, 5}) for (int y = 3; y <= 9; y++) set(x, y, z1 + 2, WOOL_BLACK);
            for (int y = 3; y <= 9; y++) {
                set(-5, y, z1 + 2, y % 3 == 0 ? WOOL_RED : WOOL_BLACK);
                set(5, y, z1 + 2, y % 3 == 0 ? WOOL_RED : WOOL_BLACK);
            }
        }

        void tower(int cx, int cz, int high) {
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) for (int y = 1; y <= high; y++) {
                boolean edge = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
                if (!inCourt(x, y, z)) continue;
                set(x, y, z, y == high ? (edge && (x + z) % 2 == 0 ? CHISELED : TILE) : edge ? (y % 4 == 0 ? CHISELED : BRICK) : AIR);
                if (y == high - 1 && !edge) set(x, y, z, AIR);
            }
            if (inCourt(cx, high + 1, cz)) set(cx, high + 1, cz, SOUL_FIRE);
            if (inCourt(cx, high, cz)) set(cx, high, cz, CHISELED);
        }

        /** The inner court: the pavilion where the Buddha-Robe Feast was laid, its long tables, and the stair's gatehouse at its back. */
        void feast() {
            int x0 = -22, x1 = 22, z0 = -40, z1 = -24;
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                set(x, 0, z, POLISHED);
                if (Math.abs(x) <= 1) set(x, 1, z, CARPET_RED);
            }
            // log pillars round it, a great roof of tiles over it on its beams
            for (int x = x0; x <= x1; x += 11) for (int z : new int[]{z0, z1}) for (int y = 1; y <= 9; y++) set(x, y, z, LOG);
            // the middle row: king posts hung from the roof's ridge (to its tiles, so none hangs in the air)
            for (int x = x0; x <= x1; x += 11) {
                int zm = (z0 + z1) / 2 - 4;
                int in = Math.min(Math.min(x - (x0 - 2), (x1 + 2) - x), Math.min(zm - (z0 - 2), (z1 + 2) - zm));
                for (int y = 8; y <= (in < 4 ? 8 + in : 13); y++) set(x, y, zm, LOG); // up to the roof's planks, or its tiles
            }
            for (int x = x0 - 2; x <= x1 + 2; x++) {
                for (int z = z0 - 2; z <= z1 + 2; z++) {
                    int in = Math.min(Math.min(x - (x0 - 2), (x1 + 2) - x), Math.min(z - (z0 - 2), (z1 + 2) - z));
                    int y = 10 + Math.min(in, 4);
                    if (in < 4) {
                        byte stair = in == Math.min(z - (z0 - 2), (z1 + 2) - z) ? (z < (z0 + z1) / 2 ? ROOF_S : ROOF_N)
                                : (x < 0 ? ROOF_E : ROOF_W);
                        set(x, y, z, stair);
                        set(x, y - 1, z, in == 0 ? AIR : DARK_PLANKS);
                    } else set(x, 14, z, TILE);
                }
            }
            for (int x = x0; x <= x1; x++) for (int z : new int[]{z0, z1}) set(x, 9, z, LOG_X);
            // the long tables, a lantern on each; stools
            for (int side = -1; side <= 1; side += 2) {
                for (int z = z0 + 3; z <= z1 - 3; z++) {
                    set(side * 7, 1, z, FENCE);
                    set(side * 7, 2, z, TABLE);
                    set(side * 9, 1, z, (z % 3 == 0) ? DARK_SLAB : AIR);
                    set(side * 5, 1, z, (z % 3 == 0) ? DARK_SLAB : AIR);
                    if (z % 4 == 0) set(side * 7, 3, z, CANDLE);
                }
                for (int z = z0 + 2; z <= z1 - 2; z += 7) set(side * 16, 8, z, LANTERN_HUNG);
            }
            // the stair's gatehouse, its arch shut by the grate
            for (int x = -7; x <= 7; x++) for (int z = -45; z <= -42; z++) for (int y = 1; y <= 11; y++) {
                if (!inCourt(x, y, z) && z > -45) continue;
                boolean arch = Math.abs(x) <= STAIR_HALF && y <= 7;
                set(x, y, z, arch ? AIR : y >= 10 ? CHISELED : Math.abs(x) >= 6 ? TILE : BRICK);
            }
            box(SEAL_BOX[0], SEAL_BOX[1], SEAL_BOX[2], SEAL_BOX[3], SEAL_BOX[4], SEAL_BOX[5], SEAL);
            for (int x : new int[]{-6, 6}) {
                set(x, 1, -41, CHISELED);
                set(x, 2, -41, SOUL_FIRE);
            }
        }

        // ── The passage, the facade, the forecourt ──────────────────────

        static boolean inPassage(int x, int y) {
            int ax = Math.abs(x);
            if (y < 1) return false;
            if (y <= 15) return ax <= PASSAGE_HALF;
            if (y == 16) return ax <= 4;
            if (y == 17) return ax <= 2;
            return false;
        }

        void passage() {
            for (int z = PASSAGE_N; z <= PASSAGE_S; z++) for (int x = -PASSAGE_HALF - 2; x <= PASSAGE_HALF + 2; x++) {
                set(x, 0, z, Math.abs(x) <= 2 ? PATH : BRICK);
                for (int y = 1; y <= 19; y++) {
                    if (inPassage(x, y)) set(x, y, z, AIR);
                    else if (Math.abs(x) <= PASSAGE_HALF + 1 && y <= 18) set(x, y, z, (z % 6 == 0 && y <= 15) ? CHISELED : BRICK);
                }
                if (z % 6 == 3) {
                    set(-4, 14, z, SOUL_LANTERN_HUNG);
                    set(4, 14, z, SOUL_LANTERN_HUNG);
                    set(-4, 15, z, CHAIN);
                    set(4, 15, z, CHAIN);
                }
            }
            // where it opens on the court
            for (int x = -PASSAGE_HALF; x <= PASSAGE_HALF; x++) for (int y = 1; y <= 15; y++) for (int z = PASSAGE_N - 4; z < PASSAGE_N; z++) {
                if (get(x, y, z) == ROCK) set(x, y, z, AIR);
            }
            for (int x = -PASSAGE_HALF; x <= PASSAGE_HALF; x++) for (int z = PASSAGE_N - 4; z < PASSAGE_N; z++) set(x, 0, z, Math.abs(x) <= 2 ? PATH : POLISHED);
            // the door: two leaves of black lacquer, banded in black iron, studded with gold; its frame
            int[] d = DOOR;
            for (int z = d[2]; z <= d[5]; z++) {
                for (int x = -PASSAGE_HALF; x <= PASSAGE_HALF; x++) for (int y = 1; y <= 17; y++) {
                    if (!inPassage(x, y)) continue;
                    if (Math.abs(x) <= d[3] && y <= d[4]) {
                        boolean band = y == 3 || y == 8 || y == 13 || Math.abs(x) == d[3] || x == 0;
                        boolean stud = !band && (y == 5 || y == 6 || y == 10 || y == 11) && Math.abs(x) % 2 == 0 && z == d[5];
                        set(x, y, z, stud ? DOOR_STUD : band ? DOOR_BAND : DOOR_LEAF);
                        if (z == d[5] && (x == 2 || x == -2) && y == 7) set(x, y, z, DOOR_STUD); // the rings
                    } else set(x, y, z, POLISHED);
                }
            }
        }

        void facade() {
            int f = FACE_Z;
            // the dressed face of the cliff, the door's recess through it
            for (int x = -17; x <= 17; x++) for (int y = 0; y <= 28; y++) for (int z = f - 2; z <= f; z++) {
                boolean opening = Math.abs(x) <= PASSAGE_HALF && y >= 1 && y <= 15;
                if (opening) {
                    set(x, y, z, AIR);
                    continue;
                }
                if (Math.abs(x) <= PASSAGE_HALF + 1 && y <= 17 && y >= 1) set(x, y, z, POLISHED); // the frame
                else set(x, y, z, (y % 7 == 0) ? CHISELED : (Math.abs(x) + y) % 9 == 0 ? CRACKED : TILE);
            }
            for (int x = -PASSAGE_HALF; x <= PASSAGE_HALF; x++) set(x, 0, f, POLISHED);
            // the lintel over the door
            for (int x = -8; x <= 8; x++) for (int y = 16; y <= 17; y++) set(x, y, f + 1, y == 17 ? CHISELED : POLISHED);
            // pillars either side, crowned with soul fire
            for (int side = -1; side <= 1; side += 2) {
                for (int x = side * 8; Math.abs(x) <= 10; x += side) for (int z = f + 1; z <= f + 2; z++) for (int y = 0; y <= 21; y++) {
                    set(x, y, z, y == 21 ? BLACKSTONE : y % 5 == 0 ? CHISELED : TILE);
                }
                set(side * 9, 22, f + 1, SOUL_FIRE);
                set(side * 9, 22, f + 2, SOUL_FIRE);
                for (int y = 0; y <= 1; y++) for (int x = side * 7; Math.abs(x) <= 11; x += side) for (int z = f + 1; z <= f + 3; z++) set(x, y, z, POLISHED_BLACKSTONE);
            }
            // the eave: three courses of tiles stepping out over the door, a ridge of black stone, lanterns under it
            for (int x = -17; x <= 17; x++) {
                set(x, 23, f + 5, ROOF_N);
                set(x, 23, f + 4, ROOF_N);
                set(x, 24, f + 3, ROOF_N);
                set(x, 24, f + 2, ROOF_N);
                set(x, 25, f + 1, ROOF_N);
                set(x, 22, f + 4, DARK_PLANKS);
                set(x, 22, f + 3, Math.abs(x) % 4 == 0 ? DARK_PLANKS : AIR);
                set(x, 23, f + 3, DARK_PLANKS);
                set(x, 24, f + 1, DARK_PLANKS);
                set(x, 26, f + 1, x % 2 == 0 ? BLACKSTONE : AIR);
            }
            for (int side = -1; side <= 1; side += 2) {
                set(side * 18, 23, f + 5, ROOF_N);
                set(side * 18, 22, f + 5, BLACKSTONE); // the eave's upturned corners
                set(side * 18, 24, f + 5, BLACKSTONE);
            }
            for (int x : new int[]{-13, -4, 4, 13}) {
                set(x, 22, f + 4, CHAIN);
                set(x, 21, f + 4, SOUL_LANTERN_HUNG);
            }
            // the gilded plaque over the lintel (黑风洞)
            for (int x = -5; x <= 5; x++) for (int y = 18; y <= 21; y++) {
                boolean rim = Math.abs(x) == 5 || y == 18 || y == 21;
                set(x, y, f + 1, rim ? GOLD : BLACKSTONE);
            }
            for (int[] g : new int[][]{{-3, 20}, {-3, 19}, {-2, 20}, {0, 20}, {0, 19}, {1, 19}, {3, 20}, {3, 19}, {2, 19}}) set(g[0], g[1], f + 1, GILDED);
            // the two black bears of stone before it, crouching, their eyes of crying obsidian
            for (int side = -1; side <= 1; side += 2) bear(side * 15, f + 6);
        }

        /** A crouching black bear of stone, its head toward the valley, centred on (cx, cz) (its pedestal's middle). */
        void bear(int cx, int cz) {
            for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 5; z <= cz + 5; z++) set(x, 0, z, Math.abs(x - cx) == 4 || Math.abs(z - cz) == 5 ? CHISELED : POLISHED);
            for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 5; z <= cz + 5; z++) set(x, 1, z, Math.abs(x - cx) == 4 || Math.abs(z - cz) == 5 ? POLISHED_BLACKSTONE : POLISHED);
            // haunches and back
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 4; z <= cz + 1; z++) for (int y = 2; y <= 5; y++) {
                if (y == 5 && (Math.abs(x - cx) == 2 || z == cz - 4)) continue;
                set(x, y, z, BLACKSTONE);
            }
            // the shoulders and forelegs, paws forward
            for (int x : new int[]{cx - 2, cx + 2}) for (int z = cz + 2; z <= cz + 4; z++) set(x, 2, z, BLACKSTONE);
            for (int x = cx - 2; x <= cx + 2; x++) for (int y = 3; y <= 6; y++) set(x, y, cz + 2, BLACKSTONE);
            // the head, its ears and muzzle, its eyes
            for (int x = cx - 2; x <= cx + 2; x++) for (int y = 5; y <= 8; y++) for (int z = cz + 2; z <= cz + 4; z++) set(x, y, z, BLACKSTONE);
            set(cx - 2, 9, cz + 3, BLACKSTONE);
            set(cx + 2, 9, cz + 3, BLACKSTONE);
            for (int x = cx - 1; x <= cx + 1; x++) for (int y = 5; y <= 6; y++) set(x, y, cz + 5, POLISHED_BLACKSTONE);
            set(cx, 6, cz + 6, BLACKSTONE);
            set(cx - 1, 7, cz + 5, CRYING);
            set(cx + 1, 7, cz + 5, CRYING);
            set(cx, 5, cz + 6, COAL);
        }

        void forecourt() {
            // braziers of soul fire up the road
            for (int z = 64; z <= 94; z += 8) for (int x : new int[]{-9, 9}) {
                set(x, 1, z, BRICK);
                set(x, 2, z, CHISELED);
                set(x, 3, z, SOUL_FIRE);
            }
            // the pavilion where the Black Bear's friends sit and talk: four posts, a tiled roof, a stone table and stools
            int px = 17, pz = 80;
            for (int x = px - 4; x <= px + 4; x++) for (int z = pz - 4; z <= pz + 4; z++) set(x, 0, z, POLISHED);
            for (int x : new int[]{px - 3, px + 3}) for (int z : new int[]{pz - 3, pz + 3}) for (int y = 1; y <= 5; y++) set(x, y, z, LOG);
            for (int x = px - 4; x <= px + 4; x++) for (int z = pz - 4; z <= pz + 4; z++) {
                int in = Math.min(Math.min(x - (px - 4), (px + 4) - x), Math.min(z - (pz - 4), (pz + 4) - z));
                if (in > 3) {
                    set(x, 9, z, BLACKSTONE);
                    continue;
                }
                byte s = in == z - (pz - 4) ? ROOF_S : in == (pz + 4) - z ? ROOF_N : in == x - (px - 4) ? ROOF_E : ROOF_W;
                set(x, 6 + in, z, s);
            }
            set(px, 1, pz, POLISHED_BLACKSTONE);
            set(px, 2, pz, TILE_SLAB_TOP);
            for (int[] d : new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) set(px + d[0], 1, pz + d[1], SLAB);
            set(px, 5, pz, LANTERN_HUNG);
            // across the road, a stele on a tortoise of stone (the cave's name and the bear's boast)
            int sx = -17, sz = 80;
            for (int x = sx - 2; x <= sx + 2; x++) for (int z = sz - 3; z <= sz + 3; z++) set(x, 1, z, Math.abs(x - sx) <= 1 && Math.abs(z - sz) <= 2 ? POLISHED_BLACKSTONE : AIR);
            set(sx, 1, sz + 3, POLISHED_BLACKSTONE);
            for (int y = 2; y <= 7; y++) for (int x = sx - 1; x <= sx + 1; x++) set(x, y, sz, y == 7 ? CHISELED : TILE);
            set(sx, 8, sz, TILE);
            for (int y = 3; y <= 6; y++) set(sx, y, sz + 1, y % 2 == 0 ? GILDED : BLACKSTONE);
        }

        // ── The grand stair and the throne cavern ───────────────────────

        void stair() {
            int steps = -THRONE_Y;
            for (int k = 0; k <= steps; k++) {
                int z = STAIR_HEAD_Z - k, y = -k;
                for (int x = -STAIR_HALF - 1; x <= STAIR_HALF + 1; x++) {
                    boolean wall = Math.abs(x) == STAIR_HALF + 1;
                    if (wall) {
                        for (int yy = y - 1; yy <= y + 7; yy++) if (!inThrone(x, yy, z)) set(x, yy, z, yy == y + 7 ? CHISELED : BRICK);
                        continue;
                    }
                    set(x, y - 1, z, BRICK);
                    set(x, y, z, k == steps ? (Math.abs(x) <= 1 ? CARPET_RED : AIR) : STAIR_S);
                    for (int yy = y + 1; yy <= y + 6; yy++) set(x, yy, z, AIR);
                    if (!inThrone(x, y + 7, z)) set(x, y + 7, z, k % 4 == 0 ? CHISELED : TILE);
                }
                if (k % 5 == 2) {
                    put(-STAIR_HALF, y + 5, z, SOUL_LANTERN_HUNG);
                    put(STAIR_HALF, y + 5, z, SOUL_LANTERN_HUNG);
                }
            }
        }

        void throne() {
            for (int x = -THRONE_RX; x <= THRONE_RX; x++) for (int z = THRONE_Z - THRONE_RZ; z <= THRONE_Z + THRONE_RZ; z++) {
                double e = sq(x / (double) THRONE_RX) + sq((z - THRONE_Z) / (double) THRONE_RZ);
                if (e >= 1) continue;
                int top = THRONE_Y + (int) Math.floor(roof(THRONE_H, e, x, z));
                set(x, THRONE_Y - 1, z, e > 0.8 ? FLOOR : (Math.abs(x) + Math.abs(z)) % 2 == 0 ? POLISHED : TILE);
                for (int y = THRONE_Y; y <= top; y++) set(x, y, z, AIR);
            }
            // the stair comes down into it: keep its last steps
            stairFoot();
            // two rows of basalt pillars, soul fire at their feet
            for (int z = THRONE_Z - 16; z <= THRONE_Z + 16; z += 8) for (int x : new int[]{-14, 14}) {
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    for (int y = THRONE_Y; y <= THRONE_Y + THRONE_H + 3; y++) {
                        if (get(x + dx, y, z + dz) != AIR) break;
                        set(x + dx, y, z + dz, (dx == 0 && dz == 0) || y % 6 != 0 ? BASALT : POLISHED_BLACKSTONE);
                    }
                }
                set(x + (x > 0 ? -2 : 2), THRONE_Y, z, SOUL_FIRE);
                set(x + (x > 0 ? -2 : 2), THRONE_Y - 1, z, POLISHED_BLACKSTONE);
            }
            // the carpet from the stair to the dais: red, edged in black
            for (int z = DAIS_FRONT + 1; z <= STAIR_HEAD_Z + THRONE_Y; z++) for (int x = -2; x <= 2; x++) {
                if (get(x, THRONE_Y, z) == AIR) set(x, THRONE_Y, z, Math.abs(x) == 2 ? CARPET_BLACK : CARPET_RED);
            }
            // the dais, its steps, the throne; soul fire at its corners; the treasury beside it
            for (int x = -9; x <= 9; x++) for (int z = SEAT_Z - 4; z <= DAIS_FRONT; z++) {
                int step = z >= DAIS_FRONT - 1 ? 1 : z >= DAIS_FRONT - 3 ? 2 : 3;
                for (int y = THRONE_Y; y < THRONE_Y + step; y++) set(x, y, z, Math.abs(x) == 9 ? CHISELED : POLISHED_BLACKSTONE);
                if (z == DAIS_FRONT || z == DAIS_FRONT - 2) set(x, THRONE_Y + step - 1, z, STAIR_S);
            }
            int seatY = THRONE_Y + 3;
            set(0, seatY, SEAT_Z, STAIR_S);
            set(-1, seatY, SEAT_Z, BLACKSTONE);
            set(1, seatY, SEAT_Z, BLACKSTONE);
            for (int y = seatY; y <= seatY + 5; y++) for (int x = -2; x <= 2; x++) set(x, y, SEAT_Z - 1, Math.abs(x) == 2 || y == seatY + 5 ? GOLD : BLACKSTONE);
            set(-2, seatY + 6, SEAT_Z - 1, GOLD);
            set(2, seatY + 6, SEAT_Z - 1, GOLD);
            set(0, seatY + 6, SEAT_Z - 1, GILDED);
            set(-2, seatY, SEAT_Z, GOLD);
            set(2, seatY, SEAT_Z, GOLD);
            for (int x : new int[]{-8, 8}) for (int z : new int[]{SEAT_Z - 3, DAIS_FRONT - 1}) {
                set(x, THRONE_Y + (z == SEAT_Z - 3 ? 3 : 1), z, SOUL_FIRE);
            }
            set(5, seatY, SEAT_Z - 1, CHEST);
            set(-5, seatY, SEAT_Z - 1, BARREL);
            // on the rock behind the throne, the stolen kasaya's likeness: a patchwork of red framed in gold
            for (int x = -9; x <= 9; x++) for (int y = seatY + 2; y <= seatY + 15; y++) {
                int z = SEAT_Z - 2;
                while (z > Z_MIN && get(x, y, z) == AIR) z--;
                if (get(x, y, z) == AIR || z < SEAT_Z - 10) continue;
                int u = x + 9, v = y - (seatY + 2);
                boolean rim = x == -9 || x == 9 || v == 0 || v == 13;
                boolean seam = u % 4 == 0 || v % 4 == 0;
                set(x, y, z, rim ? GOLD : seam ? YELLOW : TERRACOTTA);
                if (x == -6 && v == 9) set(x, y, z, GILDED); // the jade ring that clasps it
            }
        }

        void stairFoot() {
            int steps = -THRONE_Y;
            for (int k = 0; k <= steps; k++) {
                int z = STAIR_HEAD_Z - k, y = -k;
                for (int x = -STAIR_HALF; x <= STAIR_HALF; x++) {
                    set(x, y - 1, z, BRICK);
                    set(x, y, z, k == steps ? AIR : STAIR_S);
                    if (k < steps) for (int yy = y - 2; yy >= THRONE_Y - 1 && inThrone(x, yy, z); yy--) set(x, yy, z, BRICK);
                }
            }
        }

        // ── Who holds it ────────────────────────────────────────────────

        List<Spot> forecourtGarrison() {
            return List.of(new Spot(-12, 1, 72, IMP), new Spot(12, 1, 72, IMP), new Spot(-4, 1, 78, BOAR), new Spot(4, 1, 78, BOAR),
                    new Spot(14, 1, 86, IMP), new Spot(-14, 1, 88, IMP));
        }

        List<Spot> courtGarrison() {
            List<Spot> out = new ArrayList<>();
            // the yard and the road
            int[][] yard = {{-8, 18}, {8, 18}, {-12, 8}, {12, 8}, {-6, -6}, {6, -6}, {-20, 1}, {20, 1}, {-40, 6}, {40, 6}, {-24, 20}, {24, 20},
                    {-36, -10}, {36, -10}, {0, 4}, {-10, -12}, {10, -12}};
            int[] kinds = {IMP, IMP, BOAR, BOAR, BEAR, BEAR, IMP, IMP, BOAR, BOAR, IMP, IMP, BEAR, BEAR, BOAR_CAPTAIN, IMP_CAPTAIN, BEAR_CAPTAIN};
            for (int i = 0; i < yard.length; i++) out.add(new Spot(yard[i][0], 1, yard[i][1], kinds[i]));
            // in the barracks
            out.add(new Spot(23, 1, -7, BOAR));
            out.add(new Spot(-23, 1, 8, BOAR));
            // the inner court: the bear's guard about the feast
            int[][] inner = {{-12, -28}, {12, -28}, {-12, -36}, {12, -36}, {-3, -30}, {3, -30}, {-30, -26}, {30, -26}};
            int[] innerKinds = {BEAR, BEAR, BEAR, BEAR, IMP, IMP, BOAR, BEAR_CAPTAIN};
            for (int i = 0; i < inner.length; i++) out.add(new Spot(inner[i][0], 1, inner[i][1], innerKinds[i]));
            return out;
        }
    }
}
