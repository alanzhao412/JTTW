package com.jttw.engine.world.ridge;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The White Bone Cave (白骨洞), Lady White Bone's fortress under the great bone mountain at the heart
 * of White Bone Ridge: a blueprint, as Lion-Camel City's and the Bottomless Cave's are. What stands
 * at each place follows from the place alone (and, for the catacombs' maze, the world seed), so each
 * chunk lays its own share and the cave comes out whole whichever chunk is made first.
 *
 * <p>Local coordinates: (0, 0) the region's centre, the great door to the south (+z); local y 0 the
 * floor of the forecourt and of the first hall, the height {@link Site#y0} works out from the land.
 * <ul>
 *   <li><b>The great door</b>, {@value #DOOR_H} high and {@code 2 *} {@value #DOOR_HALF}{@code + 1}
 *       wide, in the back wall of a forecourt cut into the mountain's southern face: two leaves of
 *       bone and iron in a frame of blackstone; a passage behind it into the first hall.</li>
 *   <li><b>Eight floors</b>, each a hall within {@value #HALL} blocks of the centre each way,
 *       {@value #STEP} blocks under the one before: the Ossuary Gate, the Corpse Halls, the Bone
 *       Forge, the Catacombs, the Hall of Borrowed Faces, the Charnel Pit, the Soul-Fire Crypt and
 *       the Bone Bridge ({@link #plan}). Each hall's way down is a corridor in its wall to a stair
 *       in the rock outside it, behind a <b>seal</b> of iron bars its guardian holds; the stairs
 *       alternate north and south, so every floor is crossed from end to end.</li>
 *   <li><b>The well of the dead</b>: a ladder shaft in the rock by the south-east corner, a short
 *       corridor from every floor to it, and a grate under each floor's landing that opens once
 *       that floor has been reached by its stair (its seal above broken): the way back down to the
 *       deepest floor won, after a death.</li>
 *   <li><b>The throne room</b> under the last floor, {@value #THRONE_H} high under a vault ribbed
 *       like a ribcage, its dais and throne of bone to the north; the last stair comes down to an
 *       antechamber south of it.</li>
 * </ul>
 */
public final class WhiteBoneCave {

    // ── Where things are (local) ────────────────────────────────────────

    /** The floors, and the drop from one to the next. */
    public static final int FLOORS = 8, STEP = 16;
    /** The throne room: its floor, its reach east and west, its north and south walls, its height at the walls and at the ridge of its vault. */
    public static final int THRONE_Y = -150, THRONE_X = 30, THRONE_N = -34, THRONE_S = 34, THRONE_WALL_H = 18, THRONE_H = 28;
    /** The local heights the inside spans (rock and all), and their count. */
    public static final int Y_MIN = THRONE_Y - 5, Y_MAX = 20, H = Y_MAX - Y_MIN + 1;
    /** How far out each way the inside reaches. */
    public static final int REACH = 60;
    /** How far out each way a hall reaches. */
    public static final int HALL = 46;
    /** A seal's corridor (x), the arrival corridor's (x), the plane of the seal, the corridors' far end, the stair's band (z, each its side's way). */
    public static final int SEAL_X0 = -14, SEAL_X1 = -10, ARRIVE_X0 = 10, ARRIVE_X1 = 14, MOUTH_Z = 47, CORRIDOR_Z = 50,
            STAIR_Z0 = 51, STAIR_Z1 = 55;
    /** The seal's opening: as high as this. */
    public static final int SEAL_H = 6;
    /** The well of the dead: its ladder's column, its corridors' x span. */
    public static final int SHAFT_X = 36, SHAFT_Z = 51, SHAFT_X0 = 35, SHAFT_X1 = 37;
    /** The great door: half its width, its height, the plane of its leaves, the threshold's outer edge. */
    public static final int DOOR_HALF = 6, DOOR_H = 16, DOOR_Z = 57, THRESHOLD_Z = 58;
    /** The forecourt cut into the mountain before the door: its half-width and its south end. */
    public static final int COURT_HALF = 16, COURT_Z0 = 59, COURT_Z1 = 80;
    /** The mountain: its reach from the centre and its height over the forecourt at its heart. */
    public static final int MOUNTAIN_R = 130, PEAK = 72;
    /** The road down from the forecourt: where it starts, how long it is, half its paved width. */
    public static final int ROAD_Z0 = COURT_Z1 + 1, ROAD_LEN = 130, ROAD_HALF = 3;
    /** The road's watchtowers (local z), either side of it, and the bone arch over its start. */
    public static final int[] TOWERS = {112, 150, 188};
    public static final int TOWER_OUT = 7, ARCH_Z = ROAD_Z0 + 4;
    /** The outposts round the mountain: how many, their pad's radius. */
    public static final int OUTPOSTS = 5, OUTPOST_R = 12;
    /** Where the fortress's forecourt floor lies: the land at the centre and this much over it, kept within these heights. */
    public static final int RISE = 70, MIN_Y0 = 100, MAX_Y0 = 190;

    /** The seal side of floor {@code k}: -1 north (its stair to the north), +1 south. */
    public static int side(int k) {
        return k % 2 == 0 ? -1 : 1;
    }

    /** The local height of floor {@code k}'s floor (0 to 7; 8 is the throne room's). */
    public static int floorY(int k) {
        return k >= FLOORS ? THRONE_Y : -STEP * k;
    }

    /** The forecourt floor's world height for land at the centre at {@code land}. */
    public static int y0(int land) {
        return Mth.clamp(land + RISE, MIN_Y0, MAX_Y0);
    }

    /** The fortress in one world: its centre and the world height of its local 0. */
    public record Site(int cx, int y0, int cz) {

        public int x(int lx) {
            return cx + lx;
        }

        public int y(int ly) {
            return y0 + ly;
        }

        public int z(int lz) {
            return cz + lz;
        }

        public net.minecraft.core.BlockPos at(int lx, int ly, int lz) {
            return new net.minecraft.core.BlockPos(cx + lx, y0 + ly, cz + lz);
        }

        /** The great door's middle, on the threshold. */
        public net.minecraft.core.BlockPos door() {
            return at(0, 0, THRESHOLD_Z);
        }

        /** Where she keeps to: the middle of the throne room. */
        public net.minecraft.core.BlockPos throne() {
            return at(0, THRONE_Y, -6);
        }
    }

    // ── The world's own ─────────────────────────────────────────────────

    private static final Map<Long, WhiteBoneCave> BY_SEED = new ConcurrentHashMap<>();

    /** The cave for a world of this seed (its maze differs by seed). */
    public static WhiteBoneCave of(long seed) {
        return BY_SEED.computeIfAbsent(seed, WhiteBoneCave::new);
    }

    public final long seed;
    /** The catacombs' maze: the open ways east (i to i + 1) and south (j to j + 1) of each cell. */
    private final boolean[][] east = new boolean[MAZE][MAZE], south = new boolean[MAZE][MAZE];
    private final List<List<Post>> posts = new ArrayList<>();

    private WhiteBoneCave(long seed) {
        this.seed = seed;
        maze();
        for (int k = 0; k <= FLOORS; k++) posts.add(choosePosts(k));
    }

    // ── The halls ───────────────────────────────────────────────────────

    /** What lies on a hall's floor at a place, besides open air over it. */
    public static final int K_NONE = 0, K_PILLAR = 1, K_LAVA = 2, K_SOUL_FIRE = 3, K_MAGMA = 4, K_BIER = 5, K_PILE = 6, K_SOUL_SAND = 7;

    /** A place in a hall: open, its floor this far over or under the hall's own, this much air over it, and what lies there. */
    static int cell(int fo, int h, int kind) {
        return 1 | ((fo + 32) << 1) | (h << 7) | (kind << 13);
    }

    public static boolean open(int cell) {
        return (cell & 1) != 0;
    }

    /** How far the floor lies over (under, if less than 0) the hall's own. */
    public static int fo(int cell) {
        return ((cell >> 1) & 63) - 32;
    }

    /** How much air is over the floor. */
    public static int height(int cell) {
        return (cell >> 7) & 63;
    }

    public static int kind(int cell) {
        return cell >> 13;
    }

    /** A hash of a place, 0-99. */
    public static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    /** The names of the floors, top first; the ninth is the throne room. */
    public static final String[] NAMES = {"the Ossuary Gate", "the Corpse Halls", "the Bone Forge", "the Catacombs",
            "the Hall of Borrowed Faces", "the Charnel Pit", "the Soul-Fire Crypt", "the Bone Bridge", "the Throne of Bones"};

    /**
     * Floor {@code k}'s hall at local (x, z) (k 8, the throne room): 0 where it is not open.
     * <ol start="0">
     *   <li>the <b>Ossuary Gate</b>: a nave 85 blocks long under a vault 16 high, two rows of bone pillars;</li>
     *   <li>the <b>Corpse Halls</b>: three long halls side by side, rows of biers in each; a wall across
     *       the middle one sends the way round by the side halls;</li>
     *   <li>the <b>Bone Forge</b>: a forge chimney in a ring of lava, two channels of lava across the
     *       hall, each bridged only near its ends;</li>
     *   <li>the <b>Catacombs</b>: a maze of burial passages, twelve cells by twelve, the world's own;</li>
     *   <li>the <b>Hall of Borrowed Faces</b>: an outer gallery cut through north and south, an inner
     *       one, and a sanctum: the way crosses by the inner gallery;</li>
     *   <li>the <b>Charnel Pit</b>: a pit of bones seven deep across most of the hall, a causeway over
     *       it, ledges round it, a way up out of it at two corners;</li>
     *   <li>the <b>Soul-Fire Crypt</b>: a low crypt of pillars under the pit, two trenches of soul fire
     *       across it with gaps in them, taller galleries round it;</li>
     *   <li>the <b>Bone Bridge</b>: a chasm across the hall, a bridge of bone over it, a way up out
     *       of it at each end;</li>
     *   <li>the <b>Throne of Bones</b>: her vaulted hall, two rows of pillars, braziers of soul fire, the
     *       dais stepped up three to the throne.</li>
     * </ol>
     */
    public int plan(int k, int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        switch (k) {
            case 0 -> {
                if (ax > 42 || az > HALL) return 0;
                int h = 11 + (int) Math.round(5 * (1 - (x / 42.0) * (x / 42.0)));
                if (Math.abs(ax - 14) <= 1 && az <= 37 && Math.abs(z - Math.round(z / 12.0) * 12) <= 1) return cell(0, h, K_PILLAR);
                if (ax <= 1 && az <= 1) return cell(0, h, K_PILE);
                return cell(0, h, K_NONE);
            }
            case 1 -> {
                if (ax > HALL || az > HALL) return 0;
                if (ax >= 16 && ax <= 18 && !(az >= 30 && az <= 36)) return 0; // the long walls, open near each end
                if (ax <= 15 && az <= 1) return 0; // the wall across the middle hall
                int h = 9;
                boolean bierRow = Math.floorMod(z, 6) <= 2 && az >= 4 && az <= 43;
                if (bierRow && ax >= 19 && (ax == 23 || ax == 24 || ax == 31 || ax == 32 || ax == 39 || ax == 40)) return cell(0, h, K_BIER);
                if (bierRow && (ax == 5 || ax == 6)) return cell(0, h, K_BIER);
                return cell(0, h, K_NONE);
            }
            case 2 -> {
                if (ax > 44 || az > HALL) return 0;
                int h = 12;
                double r = Math.sqrt(x * x + z * z);
                if (ax <= 2 && az <= 2) return cell(0, h, K_PILLAR); // the forge's chimney
                if (r >= 9 && r < 12 && ax > 1 && az > 1) return cell(0, h, K_LAVA); // the moat, bridged four ways
                if ((az == 23 || az == 24) && !(ax >= 17 && ax <= 21)) return cell(0, h, K_LAVA); // the channels
                if (Math.abs(ax - 32) <= 1 && (Math.abs(az - 12) <= 1 || Math.abs(az - 36) <= 1)) return cell(0, h, K_PILLAR);
                if (r >= 12 && r < 15 && hash(x, z, 2) < 30) return cell(0, h, K_MAGMA);
                return cell(0, h, K_NONE);
            }
            case 3 -> {
                return mazeCell(x, z) ? cell(0, 7, hash(x, z, 3) < 4 ? K_PILE : K_NONE) : 0;
            }
            case 4 -> {
                if (ax > HALL || az > HALL) return 0;
                int ring = Math.max(ax, az), h = 11;
                boolean outer = ring >= 40 && !(ax <= 2 && az >= 40);
                boolean inner = ring >= 24 && ring <= 30;
                boolean sanctum = ring <= 14;
                boolean gates = (az <= 2 && ax >= 31 && ax <= 39) || (ax <= 2 && z >= -23 && z <= -15);
                if (!(outer || inner || sanctum || gates)) return 0;
                if (sanctum && ax <= 1 && az <= 1) return cell(0, h, K_PILE);
                return cell(0, h, K_NONE);
            }
            case 5 -> {
                if (ax > 44 || az > HALL) return 0;
                boolean pit = ax <= 38 && az <= 34 && ax > 2 && Math.max(ax, az) > 5;
                if (!pit) return cell(0, 12, K_NONE);
                int fo = -7;
                // the ways up out of it, at the north-west and south-east corners
                if (x >= 31 && z >= 30) fo = Math.min(0, -7 + (x - 31));
                if (x <= -31 && z <= -30) fo = Math.min(0, -7 + (-x - 31));
                int roll = hash(x, z, 5);
                int kind = fo > -7 ? K_NONE : roll < 30 ? K_PILE : roll < 40 ? K_MAGMA : roll < 60 ? K_SOUL_SAND : K_NONE;
                return cell(fo, 12 - fo, kind);
            }
            case 6 -> {
                if (ax > 44 || az > HALL) return 0;
                boolean low = ax <= 40 && az <= 36;
                int h = low ? 6 : 12;
                if (low && Math.floorMod(x, 6) == 0 && Math.floorMod(z, 6) == 3) return cell(0, h, K_PILLAR);
                if (low && (az == 17 || az == 18) && ax <= 40 && Math.floorMod(x + 44, 12) >= 3) return cell(0, h, K_SOUL_FIRE);
                return cell(0, h, K_NONE);
            }
            case 7 -> {
                if (ax > 44 || az > HALL) return 0;
                if (az > 12 || ax <= 2) return cell(0, 12, K_NONE);
                int fo = -7;
                if (ax >= 37) fo = Math.min(0, -7 + (ax - 37)); // the ways up out of the chasm, at either end
                int roll = hash(x, z, 7);
                return cell(fo, 12 - fo, fo > -7 ? K_NONE : roll < 20 ? K_PILE : roll < 25 ? K_SOUL_FIRE : K_NONE);
            }
            default -> {
                if (ax > THRONE_X || z < THRONE_N || z > THRONE_S) return 0;
                int h = THRONE_WALL_H + (int) Math.round((THRONE_H - THRONE_WALL_H) * (1 - (x / (double) THRONE_X) * (x / (double) THRONE_X)));
                int fo = 0;
                if (ax <= 12) fo = z <= -31 ? 3 : z <= -28 ? 2 : z <= -25 ? 1 : 0;
                int kind = K_NONE;
                if (Math.abs(ax - 18) <= 1 && z >= -21 && z <= 21 && Math.abs(z - Math.round(z / 10.0) * 10) <= 1) kind = K_PILLAR;
                else if (ax == 24 && z >= -16 && z <= 16 && Math.floorMod(z, 10) == 5) kind = K_SOUL_FIRE;
                return cell(fo, h - fo, kind);
            }
        }
    }

    // ── The catacombs' maze ─────────────────────────────────────────────

    /** The maze's cells each way, their pitch, and the first's middle. */
    public static final int MAZE = 12, PITCH = 8, FIRST = -44;

    /** A cell's middle (either axis). */
    public static int mazeMiddle(int i) {
        return FIRST + i * PITCH;
    }

    /** A spanning tree of the cells, walked at random from the middle, and one way in ten more besides. */
    private void maze() {
        SplittableRandom r = new SplittableRandom(seed ^ 0xB0E5CA7AC0L);
        boolean[][] seen = new boolean[MAZE][MAZE];
        int[] stack = new int[MAZE * MAZE];
        int top = 0;
        stack[top++] = (MAZE / 2) * MAZE + MAZE / 2;
        seen[MAZE / 2][MAZE / 2] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (top > 0) {
            int cur = stack[top - 1], i = cur / MAZE, j = cur % MAZE;
            int[] order = {0, 1, 2, 3};
            for (int a = 3; a > 0; a--) {
                int b = r.nextInt(a + 1), t = order[a];
                order[a] = order[b];
                order[b] = t;
            }
            boolean moved = false;
            for (int o : order) {
                int ni = i + dirs[o][0], nj = j + dirs[o][1];
                if (ni < 0 || nj < 0 || ni >= MAZE || nj >= MAZE || seen[ni][nj]) continue;
                link(i, j, ni, nj);
                seen[ni][nj] = true;
                stack[top++] = ni * MAZE + nj;
                moved = true;
                break;
            }
            if (!moved) top--;
        }
        for (int i = 0; i < MAZE; i++) for (int j = 0; j < MAZE; j++) {
            if (i + 1 < MAZE && r.nextInt(10) == 0) east[i][j] = true;
            if (j + 1 < MAZE && r.nextInt(10) == 0) south[i][j] = true;
        }
    }

    private void link(int i, int j, int ni, int nj) {
        if (ni == i + 1) east[i][j] = true;
        else if (ni == i - 1) east[ni][j] = true;
        else if (nj == j + 1) south[i][j] = true;
        else south[i][nj] = true;
    }

    /** Whether (x, z) is open in the catacombs: in a cell's passage, or the way between two linked cells. */
    public boolean mazeCell(int x, int z) {
        int fx = x - FIRST + 2, fz = z - FIRST + 2; // 0 at a first cell's western/northern edge
        if (fx < 0 || fz < 0) return false;
        int i = fx / PITCH, j = fz / PITCH, ox = fx % PITCH, oz = fz % PITCH;
        if (i >= MAZE || j >= MAZE) return false;
        boolean inX = ox <= 4, inZ = oz <= 4;
        if (inX && inZ) return true;
        if (inZ) return i + 1 < MAZE && east[i][j];
        if (inX) return j + 1 < MAZE && south[i][j];
        return false;
    }

    /** Whether cells (i, j) and (i + 1, j) are linked (east), or (i, j) and (i, j + 1) (south). */
    public boolean linked(int i, int j, boolean eastward) {
        return eastward ? east[i][j] : south[i][j];
    }

    // ── The column ──────────────────────────────────────────────────────

    /** What stands at a local place (bytes, by local height from {@link #Y_MIN}). */
    public static final byte UNTOUCHED = 0, ROCK = 1, AIR = 2, PILLAR = 3, STEP_W = 4, LAVA = 5, SOUL_FIRE = 6, SOUL_SOIL = 7,
            BARS = 8, GRATE = 9, LADDER = 10, LEAF = 11, FRAME = 12, BONE = 13, MAGMA = 14, PILE = 15, SOUL_SAND = 16;

    /** Whether one standing there passes through it (seals and the door as they are shut). */
    public static boolean passable(byte code) {
        return code == AIR || code == LADDER || code == SOUL_FIRE;
    }

    /**
     * Fills {@code out} (length {@link #H}) with what stands in the column at local (x, z): the halls,
     * the corridors and stairs, the well, the door and its passage, the throne room and its approach,
     * and rock round them all; {@link #UNTOUCHED} outside the inside's reach, and over the forecourt.
     */
    public void column(int x, int z, byte[] out) {
        java.util.Arrays.fill(out, UNTOUCHED);
        if (Math.abs(x) > REACH || Math.abs(z) > REACH) return;
        boolean court = z >= COURT_Z0;
        for (int y = Y_MIN; y <= Y_MAX; y++) if (!(court && y >= 0)) out[y - Y_MIN] = ROCK;
        // the halls
        for (int k = 0; k <= FLOORS; k++) {
            int c = plan(k, x, z);
            if (!open(c)) continue;
            int f = floorY(k) + fo(c), h = height(c);
            fill(out, f, f + h - 1, AIR);
            switch (kind(c)) {
                case K_PILLAR -> fill(out, f, f + h - 1, PILLAR);
                case K_LAVA -> set(out, f - 1, LAVA);
                case K_MAGMA -> set(out, f - 1, MAGMA);
                case K_SOUL_FIRE -> {
                    set(out, f - 1, SOUL_SOIL);
                    set(out, f, SOUL_FIRE);
                }
                case K_SOUL_SAND -> set(out, f - 1, SOUL_SAND);
                case K_BIER -> set(out, f, BONE);
                case K_PILE -> {
                    set(out, f, PILE);
                    if (hash(x, z, 17 + k) < 50) set(out, f + 1, PILE);
                }
                default -> {}
            }
            if (k == 5 && fo(c) == 0 && Math.abs(x) <= 2 && Math.abs(z) <= 34 && Math.max(Math.abs(x), Math.abs(z)) > 5) {
                // the causeway: a spine of bone laid along it, ribs arching over it every four blocks
                if (Math.floorMod(z, 4) == 0 && Math.abs(x) == 2) fill(out, f, f + 3, BONE);
                if (Math.floorMod(z, 4) == 0 && Math.abs(x) <= 2) set(out, f + 4, BONE);
            }
            if (k == 7 && fo(c) == 0 && Math.abs(x) <= 2 && Math.abs(z) <= 12) {
                // the bridge: a rib railing, bowed ribs over it
                if (Math.abs(x) == 2 && Math.floorMod(z, 3) != 0) set(out, f, BONE);
                if (Math.floorMod(z, 6) == 0) {
                    if (Math.abs(x) == 2) fill(out, f, f + 4, BONE);
                    set(out, f + 5, BONE);
                }
            }
        }
        throne(x, z, out);
        for (int k = 0; k < FLOORS; k++) passages(k, x, z, out);
        approach(x, z, out);
        well(x, z, out);
        door(x, z, out);
    }

    private static void set(byte[] out, int y, byte code) {
        if (y >= Y_MIN && y <= Y_MAX) out[y - Y_MIN] = code;
    }

    private static void fill(byte[] out, int y0, int y1, byte code) {
        for (int y = Math.max(Y_MIN, y0); y <= Math.min(Y_MAX, y1); y++) out[y - Y_MIN] = code;
    }

    /** Whether z lies between {@code from} and {@code to} blocks out on the given side (-1 north, +1 south). */
    private static boolean onSide(int z, int side, int from, int to) {
        int d = z * side;
        return d >= from && d <= to;
    }

    /** The last step of floor k's stair (local x): 16 down for a floor, 39 for the throne room's. */
    public static int lastStep(int k) {
        return k < FLOORS - 1 ? 6 : 28;
    }

    /** Floor k's way down: its seal corridor and seal, the stair, its foot, and the corridor into the floor below. */
    private static void passages(int k, int x, int z, byte[] out) {
        int s = side(k), f = floorY(k);
        if (x >= SEAL_X0 && x <= SEAL_X1 && onSide(z, s, MOUTH_Z, CORRIDOR_Z)) {
            fill(out, f, f + SEAL_H - 1, AIR);
            if (z * s == MOUTH_Z) fill(out, f, f + SEAL_H - 1, BARS);
        }
        if (z * s == MOUTH_Z && x >= SEAL_X0 - 1 && x <= SEAL_X1 + 1) {
            if (x == SEAL_X0 - 1 || x == SEAL_X1 + 1) fill(out, f, f + SEAL_H, FRAME);
            else set(out, f + SEAL_H, FRAME);
        }
        if (onSide(z, s, STAIR_Z0, STAIR_Z1)) {
            if (x >= SEAL_X0 && x < SEAL_X1) fill(out, f, f + 5, AIR);
            int last = lastStep(k);
            if (x >= SEAL_X1 && x <= last) {
                int st = f - 1 - (x - SEAL_X1);
                set(out, st, STEP_W);
                fill(out, st + 1, st + 7, AIR);
            }
            int foot = floorY(k + 1);
            if (k < FLOORS - 1 ? x > last && x <= ARRIVE_X1 : x > last && x <= last + 5) fill(out, foot, foot + 5, AIR);
        }
        if (k < FLOORS - 1 && x >= ARRIVE_X0 && x <= ARRIVE_X1 && onSide(z, s, MOUTH_Z, CORRIDOR_Z)) {
            fill(out, floorY(k + 1), floorY(k + 1) + 5, AIR);
        }
        if (x >= SHAFT_X0 && x <= SHAFT_X1 && z >= MOUTH_Z && z <= CORRIDOR_Z) fill(out, f, f + 4, AIR);
    }

    /** From the foot of the last stair to the throne room: a corridor north, an antechamber, the throne room's gate. */
    private static void approach(int x, int z, byte[] out) {
        int f = THRONE_Y, last = lastStep(FLOORS - 1);
        if (x > last && x <= last + 5 && z >= 45 && z <= CORRIDOR_Z) fill(out, f, f + 6, AIR);
        if (x >= -8 && x <= last + 5 && z >= 38 && z <= 44) fill(out, f, f + 8, AIR);
        if (Math.abs(x) <= 5 && z > THRONE_S && z < 38) fill(out, f, f + 11, AIR);
    }

    /** The throne itself, on the dais's top: a seat of bone, arms, a high back. */
    private static void throne(int x, int z, byte[] out) {
        int f = THRONE_Y + 3, ax = Math.abs(x);
        if (ax <= 2 && (z == -32 || z == -31)) set(out, f, BONE);
        if (ax == 3 && z >= -33 && z <= -31) fill(out, f, f + 1, BONE);
        if (ax <= 2 && z >= -34 && z <= -33) fill(out, f, f + 10 - ax, BONE);
    }

    /** The well of the dead: a ladder from the last floor up to the first, a grate under each floor's landing but the first's. */
    private static void well(int x, int z, byte[] out) {
        if (x != SHAFT_X || z != SHAFT_Z) return;
        fill(out, floorY(FLOORS - 1), floorY(0) + 4, LADDER);
        for (int k = 1; k < FLOORS; k++) set(out, grateY(k), GRATE);
    }

    /** The height of the well's grate over floor k's landing (k 1 to 7). */
    public static int grateY(int k) {
        return floorY(k) + 6;
    }

    /** The great door, shut, and the passage behind it; the frame round it. */
    private static void door(int x, int z, byte[] out) {
        int ax = Math.abs(x);
        if (ax <= DOOR_HALF && z >= MOUTH_Z && z <= THRESHOLD_Z) {
            fill(out, 0, DOOR_H - 1, z == DOOR_Z ? LEAF : AIR);
        }
        if (z >= DOOR_Z && z <= THRESHOLD_Z) {
            if (ax == DOOR_HALF + 1 || ax == DOOR_HALF + 2) fill(out, 0, DOOR_H + 1, FRAME);
            else if (ax <= DOOR_HALF) fill(out, DOOR_H, DOOR_H + 1, FRAME);
        }
    }

    /** Whether the door's leaf stands at local (x, y, z) while it is shut. */
    public static boolean leaf(int x, int y, int z) {
        return Math.abs(x) <= DOOR_HALF && z == DOOR_Z && y >= 0 && y < DOOR_H;
    }

    /** Where a leaf stands folded back against the passage's wall once the door is open: local x ±7, z 51 to 56. */
    public static boolean foldedLeaf(int x, int y, int z) {
        return Math.abs(x) == DOOR_HALF + 1 && z >= DOOR_Z - 6 && z < DOOR_Z && y >= 0 && y < DOOR_H;
    }

    // ── Where a place lies ──────────────────────────────────────────────

    /** The floor a local place is on (0 to 7; 8 the throne room and its approach), or -1 outside. */
    public static int floorOf(int x, int y, int z) {
        if (Math.abs(x) > REACH || Math.abs(z) > REACH || y > Y_MAX || y < Y_MIN) return -1;
        if (z >= COURT_Z0 - 1 && y >= -2) return -1; // the forecourt and the threshold
        if (y < floorY(FLOORS - 1) - 8) return FLOORS;
        int k = Math.round(-y / (float) STEP);
        return Mth.clamp(k, 0, FLOORS - 1);
    }

    /** Whether a local place is in the throne room itself (not its approach). */
    public static boolean inThroneRoom(int x, int y, int z) {
        return Math.abs(x) <= THRONE_X && z >= THRONE_N && z <= THRONE_S && y >= THRONE_Y - 1 && y <= THRONE_Y + THRONE_H;
    }

    // ── The mountain, the forecourt, the road ───────────────────────────

    /** The road's middle (local x) at local z: it winds a little as it comes down. */
    public static int roadX(int z) {
        if (z < ROAD_Z0) return 0;
        return (int) Math.round(10 * Math.sin((z - ROAD_Z0) / 35.0));
    }

    /** Whether local z is along the road. */
    public static boolean onRoadLength(int z) {
        return z >= ROAD_Z0 && z <= ROAD_Z0 + ROAD_LEN;
    }

    /**
     * The road's walking height (local, in halves of a block) at local z: level with the forecourt at
     * its head, easing to {@code end} (the land's at its foot) along its length.
     */
    public static int roadHalves(int z, int end) {
        double s = Mth.clamp((z - ROAD_Z0) / (double) ROAD_LEN, 0, 1);
        s = s * s * (3 - 2 * s);
        return (int) Math.round(2 * (end * s));
    }

    /** The mountain's height over the forecourt at local (x, z), before its crags: a broad mountain, a spire at its heart; 0 past its reach. */
    public static double mountain(double x, double z) {
        double r = Math.sqrt(x * x + z * z);
        if (r >= MOUNTAIN_R) return 0;
        return PEAK * (1 - Math.pow(r / MOUNTAIN_R, 1.6)) + SPIRE * Math.pow(Math.max(0, 1 - r / 48.0), 1.5);
    }

    /** The highest the mountain is raised (world height), under the sky's limit. */
    public static final int TOP_LIMIT = 296;
    /** The spire over the mountain's heart. */
    public static final int SPIRE = 34;

    /** The outposts round the mountain: their middles (local x, z). Off the road, 190-260 blocks out. */
    public int[][] outposts() {
        int[][] out = new int[OUTPOSTS][];
        SplittableRandom r = new SplittableRandom(seed ^ 0x0B05705L);
        double start = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < OUTPOSTS; i++) {
            // spread round, but never within 30 degrees of the road (due south, +z)
            double a = start + i * Math.PI * 2 / OUTPOSTS;
            double off = Math.atan2(Math.sin(a - Math.PI / 2), Math.cos(a - Math.PI / 2));
            if (Math.abs(off) < Math.toRadians(30)) a += Math.copySign(Math.toRadians(30) - Math.abs(off), off == 0 ? 1 : off);
            double d = 190 + r.nextDouble() * 70;
            out[i] = new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)};
        }
        return out;
    }

    // ── Who stands where ────────────────────────────────────────────────

    /** Who is posted: a Bone Servant or a Wraith; a regular, a captain, a floor's guardian, the door's warden. */
    public static final int SERVANT = 0, WRAITH = 1;
    public static final int REGULAR = 0, CAPTAIN = 1, GUARDIAN = 2, WARDEN = 3;
    /** The floor of a post outside: the forecourt, the road, the outposts. */
    public static final int OUTSIDE = -1;

    /**
     * A post: where (local; y the floor's height, or for one outside {@link Integer#MIN_VALUE}: on the
     * ground there), on which floor (8 the throne room, -1 outside), who, of what rank, and at what level.
     */
    public record Post(int x, int y, int z, int floor, int kind, int rank, int level) {}

    /**
     * A floor's regulars' level: the ridge's road goes on down the cave from the mountain's (28,
     * {@link com.jttw.engine.entity.ridge.RidgeMonster#LAND} of the band), from 29 on the first to the
     * band's top, 36, on the last.
     */
    public static int floorLevel(int k) {
        return LAND_TOP + 1 + (k * (ZONE.maxLevel() - LAND_TOP - 1)) / (FLOORS - 1);
    }

    /** The level the ridge's land climbs to at the mountain, the cave's door: half the band. */
    public static final int LAND_TOP = Zones.WHITE_BONE.levelAlong(com.jttw.engine.entity.ridge.RidgeMonster.LAND);
    /** The most any post of the ridge is: a captain's or a miniboss's, one over the band's top. */
    public static final int POST_TOP = Zones.WHITE_BONE.maxLevel() + com.jttw.engine.entity.zone.ZoneMonster.CAPTAIN_ABOVE_BAND;

    /** A captain's level among regulars of {@code lv}: four over them, never past {@link #POST_TOP}. */
    public static int captainOver(int lv) {
        return Math.min(POST_TOP, lv + com.jttw.engine.entity.zone.ZoneMonster.CAPTAIN_OVER);
    }

    /** The posts of floor k (8: the throne room's, her court). */
    public List<Post> posts(int k) {
        return posts.get(k);
    }

    /** A floor's share of Wraiths among its regulars and captains, in tenths. */
    private static final int[] WRAITHS = {3, 4, 2, 6, 5, 6, 4, 4};

    private List<Post> choosePosts(int k) {
        List<Post> out = new ArrayList<>();
        if (k == FLOORS) {
            // her court, about the middle of the throne room
            int lv = floorLevel(FLOORS - 1);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                int x = (int) Math.round(Math.cos(a) * 9), z = -6 + (int) Math.round(Math.sin(a) * 9);
                boolean captain = i == 0 || i == 3;
                out.add(new Post(x, THRONE_Y, z, k, i % 2 == 0 ? SERVANT : WRAITH, captain ? CAPTAIN : REGULAR,
                        captain ? captainOver(lv) : lv));
            }
            return List.copyOf(out);
        }
        int regulars = 8 + k, captains = 1 + k / 2, lv = floorLevel(k);
        // the guardian, before the seal
        int s = side(k);
        out.add(new Post(-12, floorY(k), s * 43, k, SERVANT, GUARDIAN, Math.min(POST_TOP, lv + 5)));
        List<int[]> spots = new ArrayList<>();
        int from = k == 3 ? FIRST : -42, pitch = k == 3 ? PITCH : 6; // in the catacombs, the cells' middles
        for (int x1 = from; x1 <= 44; x1 += pitch) for (int z1 = from; z1 <= 44; z1 += pitch) {
            if (Math.abs(x1 + 12) <= 4 && z1 * s >= 36) continue; // not on the guardian's ground
            if (!standing(k, x1, z1)) continue;
            spots.add(new int[]{x1, z1, hash(x1, z1, 40 + k)});
        }
        spots.sort((a, b) -> a[2] != b[2] ? Integer.compare(a[2], b[2]) : a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        for (int i = 0; i < Math.min(spots.size(), regulars + captains); i++) {
            int[] p = spots.get(i);
            boolean captain = i < captains;
            int kind = hash(p[0], p[1], 60 + k) < WRAITHS[k] * 10 ? WRAITH : SERVANT;
            int fy = floorY(k) + fo(plan(k, p[0], p[1]));
            out.add(new Post(p[0], fy, p[1], k, kind, captain ? CAPTAIN : REGULAR, captain ? captainOver(lv) : lv));
        }
        return List.copyOf(out);
    }

    /** Whether a servant may stand at (x, z) on floor k: open ground, nothing on it, room round it, a man's height and more over it. */
    private boolean standing(int k, int x, int z) {
        int c = plan(k, x, z);
        if (!open(c) || kind(c) != K_NONE || height(c) < 6) return false;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int n = plan(k, x + dx, z + dz);
            if (!open(n) || fo(n) != fo(c) || kind(n) != K_NONE) return false;
        }
        return true;
    }

    /**
     * The posts outside: the door's two wardens and their escort in the forecourt, a sentry at the
     * foot of each watchtower and one on its top, patrols along the road, each outpost's garrison
     * (a captain and four), and packs roaming the mountain's flanks and the ridge round it. Their y is {@link Integer#MIN_VALUE}: they stand on what is highest
     * there (a sentry on its tower's platform).
     */
    public List<Post> outside() {
        List<Post> out = new ArrayList<>();
        // the road's levels climb as it nears the door, from the outposts' to the forecourt's (the mountain's: LAND_TOP)
        int door = LAND_TOP, lv = LAND_TOP - 2;
        for (int s = -1; s <= 1; s += 2) {
            out.add(new Post(s * 9, 0, 67, OUTSIDE, SERVANT, WARDEN, Math.min(POST_TOP, door + 6)));
            out.add(new Post(s * 12, 0, 72, OUTSIDE, SERVANT, REGULAR, door));
            out.add(new Post(s * 5, 0, 75, OUTSIDE, WRAITH, REGULAR, door));
        }
        for (int t : TOWERS) {
            int rx = roadX(t);
            out.add(new Post(rx - TOWER_OUT + 1, Integer.MIN_VALUE, t + 1, OUTSIDE, WRAITH, REGULAR, door - 1)); // on its top
            out.add(new Post(rx + TOWER_OUT - 3, Integer.MIN_VALUE, t + 3, OUTSIDE, SERVANT, REGULAR, door - 1));
        }
        for (int z : new int[]{100, 135, 170, 205}) {
            int rx = roadX(z), along = Math.max(ZONE.minLevel(), door - 1 - (z - 100) / 35); // farther down the road, lower
            out.add(new Post(rx, Integer.MIN_VALUE, z, OUTSIDE, z % 2 == 0 ? SERVANT : WRAITH, CAPTAIN, captainOver(along)));
            out.add(new Post(rx - 2, Integer.MIN_VALUE, z + 2, OUTSIDE, SERVANT, REGULAR, along));
            out.add(new Post(rx + 2, Integer.MIN_VALUE, z + 2, OUTSIDE, WRAITH, REGULAR, along));
        }
        for (int[] o : outposts()) {
            out.add(new Post(o[0] + 3, Integer.MIN_VALUE, o[1], OUTSIDE, SERVANT, CAPTAIN, captainOver(lv)));
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + 0.6;
                out.add(new Post(o[0] + (int) Math.round(Math.cos(a) * 7), Integer.MIN_VALUE, o[1] + (int) Math.round(Math.sin(a) * 7),
                        OUTSIDE, i % 2 == 0 ? SERVANT : WRAITH, REGULAR, lv - 1 + (i % 2)));
            }
        }
        // packs roaming the mountain's flanks and the ridge round it: a captain and three
        SplittableRandom r = new SplittableRandom(seed ^ 0x9AC5L);
        int packs = 0;
        for (int tries = 0; packs < PACKS && tries < 400; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 50 + r.nextDouble() * 250;
            int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
            if (z > COURT_Z0 - 10 && Math.abs(x - roadX(z)) < 16 || Math.abs(x) <= COURT_HALF + 4 && z > 40 && z < COURT_Z1 + 6) continue;
            boolean clear = true;
            for (int[] o : outposts()) clear &= Math.hypot(x - o[0], z - o[1]) > OUTPOST_R + 8;
            if (!clear) continue;
            int kind = r.nextInt(2);
            // a pack's level is the land's where it roams: the nearer the mountain, the higher (the ridge's region some 420 across the middle)
            int pack = Math.max(ZONE.minLevel(), Math.min(LAND_TOP, (int) Math.round(LAND_TOP - (LAND_TOP - ZONE.minLevel()) * d / 420.0)));
            out.add(new Post(x, Integer.MIN_VALUE, z, OUTSIDE, kind, CAPTAIN, captainOver(pack) - 1 + r.nextInt(3)));
            for (int i = 0; i < 3; i++) {
                double b = i * Math.PI * 2 / 3 + a;
                out.add(new Post(x + (int) Math.round(Math.cos(b) * 3), Integer.MIN_VALUE, z + (int) Math.round(Math.sin(b) * 3), OUTSIDE,
                        r.nextInt(2), REGULAR, Math.max(ZONE.minLevel(), Math.min(ZONE.maxLevel(), pack - 1 + r.nextInt(3)))));
            }
            packs++;
        }
        return out;
    }

    /** How many packs roam outside. */
    public static final int PACKS = 16;

    /** A watchtower's height over the road. */
    public static final int TOWER_H = 13;

    /** The zone the cave is in. */
    public static final Zones.Zone ZONE = Zones.WHITE_BONE;
}
