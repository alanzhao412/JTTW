package com.jttw.engine.world.lion;

import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Lion-Camel Cave (狮驼洞), the demon kings' fortress in the mountain behind Lion-Camel City
 * (chapter 74: "on the ridge there is a cave, the Lion-Camel Cave, and in it three demon chiefs";
 * forty-seven thousand little demons kept its gates). A blueprint, as the city's is: what stands at
 * each local place, from the place alone, so each chunk lays its own share and the fortress comes
 * out whole whichever chunk is made first. Local x and z are the region's (the city's centre at 0, 0,
 * its main gate toward +z); local y 0 is the city's ground. The fortress lies north of the city,
 * centred on local z {@value #FZ}; within it {@code u} is local x and {@code v} is local z less
 * {@value #FZ}.
 * <ul>
 *   <li><b>the great door</b>, in the cliff of the mountain behind the city ({@link #DOOR_LZ}), under
 *       a lintel with the cave's name in gold, between pillars of black stone: two leaves of dark
 *       wood banded with iron and studded with gold, {@code 2 *} {@value #DOOR_HALF}{@code + 1}
 *       wide and {@value #DOOR_H} high; it holds until its wardens in the forecourt have fallen, then
 *       opens to a hand ({@link LionGates});</li>
 *   <li><b>the gate hall</b> (floor 0) behind it, at the city's level, {@value #HALL_AIR} high, red
 *       pillars down its length;</li>
 *   <li><b>eight floors</b> under it, each {@value #PITCH} blocks below the last, each a hall
 *       {@code 2 *} {@value #HALF}{@code + 1} blocks square and {@value #AIR} high ({@link #THEMES}):
 *       the barracks, the drill yard, the kitchens where the pilgrims were to be steamed, the bone
 *       pits, the menagerie, the vault, the Great Peng's halls and the generals' hall. Each has its
 *       way down, a stair against one wall, behind a portcullis its warden holds; the stair comes
 *       down on the next floor at the other side, so every floor is crossed;</li>
 *   <li><b>the throne room</b> at the bottom, {@value #THRONE_AIR} high, the generals' hall's grand
 *       stair coming down into it, red pillars, a dais with the three thrones (the Great Peng's empty,
 *       his treasury beside it: the Yin-Yang Vase), the Azure Lion before his and the White Elephant
 *       before his own.</li>
 * </ul>
 * Every block is told by {@link #cell}, a kind (for what stands and what one walks through, pure:
 * unit-tested) and a look; {@link #at} makes it a block.
 */
public final class LionFortress {

    private LionFortress() {}

    // ── The plan ─────────────────────────────────────────────────────────

    /** The fortress's middle (local z), and the floors' interior half-width, the shell's thickness. */
    public static final int FZ = -110, HALF = 31, SHELL = 3;
    /** The floors: how far each lies under the last, how high a hall is within, how many there are. */
    public static final int PITCH = 13, AIR = 10, FLOORS = 8;
    /** The gate hall's height within (floor 0). */
    public static final int HALL_AIR = 20;
    /** The throne room: its height within, its floor (local y), and its north and south ends (v). */
    public static final int THRONE_AIR = 20, THRONE_Y = -PITCH * FLOORS - 23, THRONE_N = -72, THRONE_S = -6;
    /** The floor index of the throne room. */
    public static final int THRONE = FLOORS + 1;
    /** How far down and up the fortress reaches (local y). */
    public static final int BOTTOM = THRONE_Y - SHELL, TOP = 34;
    /** The great door: its plane (v), half-width, height; where it stands (local z). */
    public static final int DOOR_V = HALF + 2, DOOR_HALF = 7, DOOR_H = 17, DOOR_LZ = FZ + DOOR_V;
    /** The façade before the door reaches this far out (v) and this wide (u). */
    public static final int FACADE_V = DOOR_V + 7, FACADE_HALF = DOOR_HALF + 11;
    /** The forecourt between the city's north wall and the door (local z, its south and north ends; half-width). */
    public static final int FORECOURT_S = -57, FORECOURT_N = FZ + FACADE_V + 1, FORECOURT_HALF = 24;

    /** The floors' names, from the gate hall (0) to the throne room ({@value #THRONE}). */
    public static final String[] THEMES = {"the Gate Hall", "the Barracks", "the Drill Yard", "the Steaming Kitchens", "the Bone Pits",
            "the Menagerie", "the Vault", "the Great Peng's Halls", "the Generals' Hall", "the Throne Room"};

    /** The local y of floor {@code k}'s floor (0 the gate hall, 1 to {@value #FLOORS} the floors under it; {@value #THRONE} the throne room's). */
    public static int floorY(int k) {
        return k == THRONE ? THRONE_Y : -PITCH * k;
    }

    /** How high floor {@code k} is within. */
    public static int airOf(int k) {
        return k == 0 ? HALL_AIR : k == THRONE ? THRONE_AIR : AIR;
    }

    /** Whether local (lx, lz) is under or within the fortress (its footprint, the façade and forecourt aside). */
    public static boolean covers(int lx, int lz) {
        int v = lz - FZ, au = Math.abs(lx);
        if (au <= HALF + SHELL && v >= THRONE_N - SHELL && v <= DOOR_V + 1) return true;
        return au <= FACADE_HALF && v > DOOR_V + 1 && v <= FACADE_V;
    }

    /** Whether local (lx, lz) is on the forecourt before the door. */
    public static boolean onForecourt(int lx, int lz) {
        return Math.abs(lx) <= FORECOURT_HALF && lz <= FORECOURT_S && lz >= FORECOURT_N;
    }

    // ── Kinds ────────────────────────────────────────────────────────────

    /** What a cell is: nothing of the fortress (the ground as it is), air, the shell's rock, ... */
    public static final int OUT = 0, OPEN = 1, ROCK = 2, WALL = 3, FLOOR = 4, STEP = 5, GATE = 6, DOOR = 7, DECOR = 8, LOW = 9, LAVA = 10,
            CHEST = 11, BED = 12;

    /** The kind of a cell (its low bits). */
    public static int kind(int cell) {
        return cell & 15;
    }

    /** Its look (what decoration, what material). */
    public static int look(int cell) {
        return (cell >> 4) & 255;
    }

    /** Its facing, for a step or a bed: 0 north, 1 south, 2 west, 3 east. */
    public static int facing(int cell) {
        return (cell >> 12) & 3;
    }

    private static int c(int kind, int look) {
        return kind | look << 4;
    }

    private static int c(int kind, int look, int facing) {
        return kind | look << 4 | facing << 12;
    }

    /** Whether a cell of this kind stops one walking (a step, a gate, a bed and a chest do; air, a carpet or a lantern do not). */
    public static boolean solid(int kind) {
        return kind != OUT && kind != OPEN && kind != LOW && kind != LAVA;
    }

    /** Whether one may stand on a cell of this kind. */
    public static boolean ground(int kind) {
        return solid(kind) && kind != GATE && kind != DOOR;
    }

    // looks: the walls' and floors' (by the floor's theme), and the decorations
    static final int L_PLAIN = 0, L_BAND = 1, L_TRIM = 2, L_PILLAR = 3, L_PILLAR_CAP = 4, L_GOLD = 5, L_INLAY = 6, L_CARPET = 7, L_MAGMA = 8,
            L_SAND = 9, L_BONES = 10, L_MUD = 11, L_WATER = 12, L_FILL = 13, L_STEP = 14, L_RAIL = 15,
            D_LANTERN = 20, D_CHAIN = 21, D_SKULL = 22, D_BONE = 23, D_CAULDRON = 24, D_FIRE = 25, D_SOUL_FIRE = 26, D_BARREL = 27, D_HAY = 28,
            D_BARS = 29, D_ANVIL = 30, D_GRINDSTONE = 31, D_FENCE = 32, D_TABLE = 33, D_COBWEB = 34, D_BRAZIER = 35, D_HEAP = 36,
            D_THRONE_SEAT = 37, D_THRONE_ARM = 38, D_THRONE_BACK = 39, D_BANNER_RED = 40, D_BANNER_GOLD = 41, D_STACK = 42, D_TARGET = 43,
            D_PEN = 44, D_TROUGH = 45, D_FEATHER = 46, D_CANDLE = 47, D_PLAQUE = 48, D_STUD = 49, D_SEAM = 50, D_IRON = 51, D_KNOCKER = 52,
            D_STATUE = 53, D_STATUE_HEAD = 54, D_SLAB = 55, D_LOW_SOUL_FIRE = 56, D_SLIT = 57, D_BEACON = 58, D_LAMP = 59;

    private static final int NORTH = 0, SOUTH = 1, WEST = 2, EAST = 3;

    /** A hash of a place, 0-99. */
    static int hash(int x, int y, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x * 31 + y, salt, z), 100L);
    }

    // ── The ways down ───────────────────────────────────────────────────

    /**
     * Floor {@code k}'s way down (0 to {@value #FLOORS}): its stair's side (u from {@code u0} to
     * {@code u1}), its landing (v), the way it goes down along v ({@code dir}, -1 or 1), how many steps,
     * and the gate's row before the landing.
     */
    public record Stair(int u0, int u1, int landing, int dir, int steps, int gate) {
        /** The v of step {@code i} (1 to steps). */
        public int stepV(int i) {
            return landing + dir * i;
        }

        /** The v one steps off it onto the floor below. */
        public int foot() {
            return landing + dir * (steps + 1);
        }

        public boolean spans(int u) {
            return u >= u0 && u <= u1;
        }

        /** Which step (1 to steps) lies at v, 0 at the landing, -1 if none. */
        public int stepAt(int v) {
            int i = (v - landing) * dir;
            return i >= 0 && i <= steps ? i : -1;
        }
    }

    /** Floor {@code k}'s stair down: on even floors against the west wall going north, on odd ones the east going south; the generals' hall's, the grand stair into the throne room. */
    public static Stair stair(int k) {
        if (k == FLOORS) return new Stair(-3, 3, -8, -1, floorY(FLOORS) - THRONE_Y, -7);
        if (k % 2 == 0) return new Stair(-HALF, -HALF + 4, -6, -1, PITCH, -5);
        return new Stair(HALF - 4, HALF, 6, 1, PITCH, 5);
    }

    /** Where one comes onto floor {@code k} ({u, v}): the door for the gate hall, else the foot of the stair from above. */
    public static int[] arrival(int k) {
        if (k == 0) return new int[]{0, DOOR_V - 1};
        Stair s = stair(k - 1);
        return new int[]{(s.u0() + s.u1()) / 2, s.foot()};
    }

    /** Where floor {@code k}'s warden stands, before its gate (u, v); the throne room has none. */
    public static int[] gatePost(int k) {
        Stair s = stair(k);
        return new int[]{(s.u0() + s.u1()) / 2, s.gate() - 3 * s.dir()};
    }

    /** Whether (u, v) is in a lane floor {@code k} keeps clear: from where one comes on, by the middle, to its gate. */
    static boolean lane(int k, int u, int v) {
        if (k == THRONE) return Math.abs(u) <= 4;
        // the way to the gate
        Stair dep = stair(k);
        int gv = dep.gate() - dep.dir() * 2; // a little before the gate
        if (k == FLOORS) {
            if (Math.abs(u) <= 4 && v >= -7 && v <= 23) return true;
        } else if (Math.min(gv - 3, gv + 3) <= v && v <= gv + 3 && between(u, (dep.u0() + dep.u1()) / 2, 0, 3)) return true;
        // from where one comes on, to the middle
        if (k == 0) {
            if (Math.abs(u) <= 6 && v >= -6) return true;
        } else {
            int[] a = arrival(k);
            if (Math.abs(v - a[1]) <= 3 && between(u, a[0], 0, 3)) return true;
        }
        // the middle, joining them
        return Math.abs(u) <= 3 && v >= -24 && v <= 24;
    }

    private static boolean between(int u, int a, int b, int pad) {
        return u >= Math.min(a, b) - pad && u <= Math.max(a, b) + pad;
    }

    /** Whether (u, v) on floor {@code k} is the stair's enclosure or its near ground (kept clear). */
    static boolean nearStairs(int k, int u, int v) {
        if (k <= FLOORS) {
            Stair s = stair(k);
            int vA = Math.min(s.gate(), s.stepV(s.steps())), vB = Math.max(s.gate(), s.stepV(s.steps()));
            if (u >= s.u0() - 3 && u <= s.u1() + 3 && v >= vA - 3 && v <= vB + 3) return true;
        }
        if (k >= 1 && k <= THRONE) {
            Stair s = stair(k - 1);
            int vA = Math.min(s.landing(), s.foot()), vB = Math.max(s.landing(), s.foot());
            if (u >= s.u0() - 3 && u <= s.u1() + 3 && v >= vA - 3 && v <= vB + 3) return true;
        }
        return false;
    }

    /** Whether decorations may stand at (u, v) on floor {@code k}: off the lanes and clear of the stairs and the walls. */
    static boolean free(int k, int u, int v) {
        return !lane(k, u, v) && !nearStairs(k, u, v) && Math.abs(u) <= HALF - 2 && Math.abs(v) <= HALF - 2
                && (k != 0 || Math.abs(u) > 6);
    }

    // ── The cells ────────────────────────────────────────────────────────

    /** The floor whose band local y {@code ly} is in: 0 for the gate hall and over it, {@value #THRONE} for the throne room's; the band of floor k runs from its floor up to the floor over it. */
    public static int bandOf(int ly) {
        if (ly > 0) return 0;
        return Math.min(THRONE, -ly / PITCH + 1);
    }

    /** What stands at local (lx, ly, lz), packed (see {@link #kind}, {@link #look}, {@link #facing}); {@link #OUT} where the fortress leaves the ground be. */
    public static int cell(int lx, int ly, int lz) {
        if (ly < BOTTOM || ly > TOP || !covers(lx, lz)) return OUT;
        int u = lx, v = lz - FZ;
        if (v > DOOR_V + 1) return facade(u, ly, v);
        boolean inFloors = Math.abs(u) <= HALF && Math.abs(v) <= HALF;
        boolean inThrone = Math.abs(u) <= HALF && v >= THRONE_N && v <= THRONE_S;
        // the gate hall and over it
        if (ly > 0) {
            if (ly > HALL_AIR + SHELL) {
                int f = v >= DOOR_V - 1 ? facade(u, ly, v) : OUT;
                return f != OUT ? f : battlements(u, ly, v);
            }
            if (v >= HALF + 1) return doorWall(u, ly, v);
            if (!inFloors) return ly <= HALL_AIR && face(u, v, HALF, -HALF, HALF) ? faceOf(u, ly, v, HALL_AIR) : shell(u, ly, v);
            if (ly > HALL_AIR) return ly == HALL_AIR + 1 ? ceiling(u, v) : shell(u, ly, v);
            return room(0, u, ly, v);
        }
        int k = bandOf(ly);
        // the stairs, through the slabs and into the floor below
        int stairCell = stairs(u, ly, v);
        if (stairCell >= 0) return stairCell;
        if (k == THRONE) {
            int h = ly - THRONE_Y;
            if (!inThrone || h <= 0 || h > THRONE_AIR) {
                if (inThrone && h == 0) return throneFloor(u, v);
                if (h > THRONE_AIR && inFloors && ly == floorY(FLOORS)) return floorOf(FLOORS, u, v); // the generals' hall's floor
                if (inThrone && h == THRONE_AIR + 1) return ceiling(u, v);
                if (!inThrone && h >= 1 && h <= THRONE_AIR && face(u, v, HALF, THRONE_N, THRONE_S)) return faceOf(u, h, v, THRONE_AIR);
                return shell(u, ly, v);
            }
            return throne(u, h, v);
        }
        int h = ly - floorY(k);
        if (!inFloors) return h >= 1 && h <= AIR && face(u, v, HALF, -HALF, HALF) ? faceOf(u, h, v, AIR) : shell(u, ly, v);
        if (h > AIR) {
            // the slab over the floor; its top the floor above's, its underside the hall's ceiling
            if (h == PITCH) return floorOf(k - 1, u, v);
            if (h == AIR + 1) return ceiling(u, v);
            return shell(u, ly, v);
        }
        return room(k, u, h, v);
    }

    /** Whether (u, v) is the first layer of the walls round a hall of half-width {@code half}, from {@code v0} to {@code v1}. */
    private static boolean face(int u, int v, int half, int v0, int v1) {
        int au = Math.abs(u);
        return au == half + 1 && v >= v0 - 1 && v <= v1 + 1 || au <= half + 1 && (v == v0 - 1 || v == v1 + 1);
    }

    /** A hall's walls, inside: a plinth, pilasters every eight blocks, a frieze under the roof (the floor's theme). */
    private static int faceOf(int u, int h, int v, int air) {
        int along = Math.abs(u) == HALF + 1 ? v : u;
        if (Math.floorMod(along, 8) == 0) return c(WALL, h == air ? L_PILLAR_CAP : L_PILLAR);
        return c(WALL, h == 1 ? L_BAND : h >= air - 1 ? L_TRIM : L_PLAIN);
    }

    /** A hall's ceiling: beams every eight blocks each way, dark panels between (the floor's theme). */
    private static int ceiling(int u, int v) {
        return Math.floorMod(u, 8) == 0 || Math.floorMod(v, 8) == 0 ? c(WALL, L_PILLAR_CAP) : c(WALL, L_BAND);
    }

    /**
     * The shell's rock: within {@value #SHELL} blocks of the halls (the floors stacked under the
     * gate hall, and the throne room), so no cave or water of the mountain breaks in; past that the
     * mountain is left as it is.
     */
    private static int shell(int u, int ly, int v) {
        if (Math.abs(u) > HALF + SHELL) return OUT;
        boolean floors = Math.abs(v) <= HALF + SHELL && ly >= floorY(FLOORS) - SHELL && ly <= HALL_AIR + SHELL;
        boolean throne = v >= THRONE_N - SHELL && v <= THRONE_S + SHELL && ly >= THRONE_Y - SHELL && ly <= THRONE_Y + THRONE_AIR + SHELL;
        return floors || throne ? c(ROCK, L_PLAIN) : OUT;
    }

    /** The front wall the door is in (v from HALF + 1 to DOOR_V + 1). */
    private static int doorWall(int u, int ly, int v) {
        int au = Math.abs(u);
        if (au <= DOOR_HALF && ly <= DOOR_H) {
            if (v != DOOR_V) return c(OPEN, 0);
            // the leaves: dark wood, iron bands, gold studs, the seam down the middle, knockers
            if (u == 0) return c(DOOR, D_SEAM);
            if (ly == 4 || ly == 9 || ly == DOOR_H) return c(DOOR, D_IRON);
            if (au == 2 && ly == 6) return c(DOOR, D_KNOCKER);
            if (au % 2 == 1 && (ly == 2 || ly == 7 || ly == 11)) return c(DOOR, D_STUD);
            return c(DOOR, L_PLAIN);
        }
        if (ly > HALL_AIR + SHELL) return OUT;
        if (v == HALF + 1 && au <= HALF + 1 && ly <= HALL_AIR) return faceOf(u, ly, v, HALL_AIR);
        return shell(u, ly, v);
    }

    /** Before the door: the jambs, the lintel with the cave's name, the eaves, the braziers, the steps. */
    private static int facade(int u, int ly, int v) {
        int au = Math.abs(u);
        if (au > FACADE_HALF || v > FACADE_V || v < DOOR_V - 1 || ly < 0 || ly > TOP) return OUT;
        // the jambs: great pillars of black stone either side of the door, out from the cliff
        if (au >= DOOR_HALF + 1 && au <= DOOR_HALF + 3 && v <= DOOR_V + 4) {
            if (ly <= DOOR_H + 4) return ly % 5 == 0 ? c(WALL, L_GOLD) : c(WALL, L_PILLAR);
        }
        // the lintel over the door, the plaque in it: 狮驼洞
        if (au <= DOOR_HALF + 3 && ly >= DOOR_H + 1 && ly <= DOOR_H + 5 && v <= DOOR_V + 3) {
            if (v == DOOR_V + 3 && au <= 3 && (ly == DOOR_H + 2 || ly == DOOR_H + 3)) return c(WALL, D_PLAQUE);
            return c(WALL, ly == DOOR_H + 1 ? L_TRIM : L_PLAIN);
        }
        // the eaves over it, a lion's head (a skull) at each end
        if (ly == DOOR_H + 6 && au <= DOOR_HALF + 5 && v <= DOOR_V + 5) {
            if (au == DOOR_HALF + 5 || v == DOOR_V + 5) return c(DECOR, L_STEP, au == DOOR_HALF + 5 ? (u > 0 ? WEST : EAST) : NORTH);
            return c(WALL, L_PLAIN);
        }
        if (ly == DOOR_H + 7 && au <= DOOR_HALF + 3 && v <= DOOR_V + 3) {
            if (au == DOOR_HALF + 3 || v == DOOR_V + 3) return c(DECOR, L_STEP, au == DOOR_HALF + 3 ? (u > 0 ? WEST : EAST) : NORTH);
            return c(WALL, L_PLAIN);
        }
        if (ly == DOOR_H + 8 && au == DOOR_HALF + 5 && v == DOOR_V + 5) return c(LOW, D_SKULL);
        // the braziers that light it, on plinths either side of the way
        if (au == DOOR_HALF + 10 && v == DOOR_V + 5) {
            if (ly <= 3) return c(WALL, ly == 3 ? L_TRIM : L_PILLAR);
            if (ly == 4) return c(DECOR, D_BRAZIER);
            return OUT;
        }
        // the statues of crouching lions either side of it, on plinths: a body, a head over the forepaws
        int sa = au - (DOOR_HALF + 4); // across a statue's plinth, 0 to 3
        if (sa >= 0 && sa <= 3 && v >= DOOR_V + 1 && v <= DOOR_V + 4 && ly >= 1 && ly <= 7) {
            if (ly == 1) return c(WALL, L_TRIM);
            if (ly <= 4 && sa >= 1 && v <= DOOR_V + 3) return c(WALL, D_STATUE);
            if (ly >= 4 && ly <= 6 && sa >= 1 && sa <= 2 && v == DOOR_V + 4 - (ly == 6 ? 1 : 0)) return c(WALL, D_STATUE_HEAD);
            if (ly == 5 && sa == 3 && v == DOOR_V + 2) return c(WALL, D_STATUE);
            return OUT;
        }
        // the threshold and the paving before the door
        if (ly == 0 && au <= DOOR_HALF + 3) return c(FLOOR, au <= DOOR_HALF ? L_INLAY : L_TRIM);
        return OUT;
    }

    /** The tops of the front wall either side of the façade: battlements, and a tower at each end with a beacon of soul fire. */
    private static int battlements(int u, int ly, int v) {
        int au = Math.abs(u), h = ly - (HALL_AIR + SHELL);
        if (v < HALF + 1 || v > DOOR_V + 1 || au > HALF + SHELL) return OUT;
        if (au >= HALF - 3) {
            // the towers at the ends
            boolean rim = au == HALF - 3 || au == HALF + SHELL || v == HALF + 1 || v == DOOR_V + 1;
            if (h <= 8) return h == 8 ? c(WALL, L_TRIM) : rim ? c(WALL, h % 4 == 2 && v == DOOR_V + 1 && au == HALF ? D_SLIT : L_PILLAR) : c(ROCK, L_PLAIN);
            if (h == 9) return rim && (au + v) % 2 == 0 ? c(WALL, L_TRIM) : au == HALF && v == DOOR_V ? c(DECOR, D_BEACON) : OUT;
            return OUT;
        }
        if (h == 1 && v == DOOR_V + 1 && au % 2 == 0) return c(WALL, L_TRIM);
        if (h == 2 && v == DOOR_V + 1 && au % 8 == 4) return c(LOW, D_SKULL);
        return OUT;
    }

    /** The stairs: a cell of floor k's stair down, where its steps and the open space over them are. -1 if none. */
    private static int stairs(int u, int ly, int v) {
        int k = bandOf(ly);
        // the stair down from this band's floor runs through the slab under it, so it may be the band's own or the one over's
        for (int from = Math.max(0, k - 2); from <= Math.min(FLOORS, k); from++) {
            Stair s = stair(from);
            boolean rail = s.steps() > PITCH && (u == s.u0() - 1 || u == s.u1() + 1); // the grand stair's balustrades
            if (!s.spans(u) && !rail) continue;
            int i = s.stepAt(v);
            if (i < 1) continue;
            int top = floorY(from), surface = top - i, below = floorY(from == FLOORS ? THRONE : from + 1);
            if (ly > top + airOf(from) || ly < below) continue;
            if (rail) {
                // the balustrades of the grand stair: the mass's sides, a low wall along the top
                if (ly <= top) return ly <= surface ? c(WALL, L_FILL) : ly == surface + 1 ? c(DECOR, L_RAIL) : -1;
                return -1;
            }
            if (ly < surface) return c(WALL, L_FILL);
            if (ly == surface) return c(STEP, L_STEP, s.dir() < 0 ? SOUTH : NORTH);
            return c(OPEN, 0);
        }
        return -1;
    }

    /** Floor {@code k}'s floor at (u, v) (the top of the slab under the hall). */
    private static int floorOf(int k, int u, int v) {
        // the stair's hole down
        if (k <= FLOORS) {
            Stair s = stair(k);
            if (s.spans(u) && s.stepAt(v) >= 1) return c(OPEN, 0);
        }
        // its hazards: magma by the fires, lava pools in the vault, the moats of the generals' hall, the bone pits
        if (free(k, u, v)) {
            switch (k) {
                case 3 -> {
                    if (hearth(u, v) && hash(u, 0, v, 301) < 55) return c(FLOOR, L_MAGMA);
                }
                case 4 -> {
                    if (pit(u, v)) return c(FLOOR, L_BONES);
                }
                case 6 -> {
                    if (pool(u, v)) return c(LAVA, 0);
                }
                case 8 -> {
                    if (moat(u, v)) return c(LAVA, 0);
                }
                default -> {}
            }
        }
        if (k == 0 && Math.abs(u) <= 2 && v >= -4) return c(FLOOR, L_CARPET);
        if (k == 2 && Math.max(Math.abs(u), Math.abs(v)) <= 14 && !lane(k, u, v)) return c(FLOOR, L_SAND);
        if (k == 5 && pen(u, v) && !lane(k, u, v)) return c(FLOOR, L_MUD);
        if (Math.floorMod(u, 8) == 0 && Math.floorMod(v, 8) == 0) return c(FLOOR, L_INLAY);
        return c(FLOOR, Math.abs(u) <= 3 || Math.abs(v) <= 3 ? L_TRIM : L_PLAIN);
    }

    // hazards and rooms, by place
    private static boolean hearth(int u, int v) {
        int ou = Math.floorMod(u, 10), ov = Math.floorMod(v, 10);
        return ou >= 3 && ou <= 7 && ov >= 3 && ov <= 7;
    }

    private static boolean pit(int u, int v) {
        int ou = Math.floorMod(u + 4, 12), ov = Math.floorMod(v + 4, 12);
        return ou >= 2 && ou <= 7 && ov >= 2 && ov <= 7;
    }

    private static boolean pool(int u, int v) {
        return Math.abs(u) >= 8 && Math.abs(v) >= 8 && Math.abs(u) <= 26 && Math.abs(v) <= 26 && (Math.abs(u) + Math.abs(v)) % 9 != 0;
    }

    private static boolean moat(int u, int v) {
        int au = Math.abs(u);
        return (au == 9 || au == 10 || au == 20 || au == 21) && Math.abs(v) <= 25;
    }

    private static boolean pen(int u, int v) {
        int ou = Math.floorMod(u, 14), ov = Math.floorMod(v, 14);
        return ou >= 2 && ou <= 10 && ov >= 2 && ov <= 10;
    }

    /** Whether a pillar of floor {@code k} stands at (u, v). */
    static boolean pillar(int k, int u, int v) {
        int spacing = switch (k) {
            case 0 -> 0;
            case 1 -> 10;
            case 2 -> 0;
            case 3 -> 12;
            case 4 -> 9;
            case 5 -> 14;
            case 6 -> 8;
            case 7 -> 11;
            default -> 10;
        };
        if (k == 0) {
            // the gate hall: two rows of red pillars down its length
            return (Math.abs(u) == 9 || Math.abs(u) == 10) && Math.floorMod(v, 7) <= 1 && v > -26 && v < 28 && !lane(0, u, v);
        }
        if (k == 2) {
            // the drill yard: a colonnade round its open ground
            int q = Math.max(Math.abs(u), Math.abs(v));
            return (q == 19 || q == 20) && Math.floorMod(u + v, 8) <= 1 && !lane(k, u, v) && !nearStairs(k, u, v);
        }
        int pu = Math.floorMod(u + spacing / 2, spacing), pv = Math.floorMod(v + spacing / 2, spacing);
        if (pu > 2 || pv > 2) return false;
        // the whole pillar must stand clear of the lanes and the stairs
        int cu = u - pu + 1, cv = v - pv + 1;
        for (int du = -2; du <= 2; du++) for (int dv = -2; dv <= 2; dv++) {
            if (lane(k, cu + du, cv + dv) || nearStairs(k, cu + du, cv + dv)) return false;
        }
        return Math.abs(cu) <= HALF - 3 && Math.abs(cv) <= HALF - 3;
    }

    /** A floor's hall, {@code h} over its floor (1 to its height). */
    private static int room(int k, int u, int h, int v) {
        int air = airOf(k);
        // the stair's enclosure and its gate
        if (k <= FLOORS) {
            int e = enclosure(k, u, h, v);
            if (e >= 0) return e;
        }
        if (pillar(k, u, v)) return c(WALL, h == air || h == 1 ? L_PILLAR_CAP : L_PILLAR);
        // dividing walls on some floors: side halls through wide doorways
        if (divides(k, u, v) && !lane(k, u, v) && !nearStairs(k, u, v)) {
            int along = k == 3 || k == 6 ? u : v;
            if (Math.floorMod(along, 16) >= 5 && Math.floorMod(along, 16) <= 10) {
                return h > 6 ? c(WALL, h == 7 ? L_TRIM : L_PLAIN) : c(OPEN, 0); // a doorway under a lintel
            }
            return c(WALL, h == 1 ? L_BAND : h == air ? L_TRIM : L_PLAIN);
        }
        // the inner face of the walls: a band low, a trim high, banners and braziers along them
        int au = Math.abs(u), av = Math.abs(v);
        if (au == HALF || av == HALF) {
            int along = au == HALF ? v : u;
            if (Math.floorMod(along, 8) == 4 && h >= 3 && h <= 6 && !lane(k, u, v) && !nearStairs(k, u, v)) {
                return c(DECOR, k == 7 || k == 0 ? D_BANNER_GOLD : D_BANNER_RED);
            }
        }
        // lanterns on chains from the roof
        if (Math.floorMod(u, 8) == 4 && Math.floorMod(v, 8) == 4 && !pillar(k, u, v)) {
            if (h == air) return c(LOW, D_CHAIN);
            if (h == air - 1) return c(LOW, D_LANTERN);
        }
        if (free(k, u, v)) {
            int d = decor(k, u, h, v);
            if (d >= 0) return d;
        }
        return c(OPEN, 0);
    }

    /** Whether floor {@code k} has a dividing wall at (u, v). */
    private static boolean divides(int k, int u, int v) {
        return switch (k) {
            case 1, 5, 7 -> Math.abs(u) == 13;
            case 3, 6 -> Math.abs(v) == 13;
            default -> false;
        };
    }

    /** The enclosure round floor k's stair down: walls to the roof, the portcullis its warden holds. -1 where none. */
    private static int enclosure(int k, int u, int h, int v) {
        Stair s = stair(k);
        int vEnd = s.stepV(s.steps()) + s.dir(); // beyond the last step's hole, the end wall
        int vA = Math.min(s.gate(), vEnd), vB = Math.max(s.gate(), vEnd);
        boolean side = (u == s.u0() - 1 && s.u0() > -HALF) || (u == s.u1() + 1 && s.u1() < HALF);
        if (v < vA || v > vB) return -1;
        if (!side && !s.spans(u)) return -1;
        int air = airOf(k);
        if (side) return c(WALL, h == 1 ? L_BAND : h == air ? L_TRIM : L_PLAIN);
        if (v == vEnd) return c(WALL, h == 1 ? L_BAND : L_PLAIN);
        if (v == s.gate()) return h <= AIR ? c(GATE, 0) : c(WALL, L_TRIM);
        return c(OPEN, 0); // the landing, and the hole the steps go down
    }

    // ── Each floor's furniture ──────────────────────────────────────────

    private static int decor(int k, int u, int h, int v) {
        int ou = Math.floorMod(u, 8), ov = Math.floorMod(v, 8);
        int room = hash(Math.floorDiv(u, 8), k, Math.floorDiv(v, 8), 400);
        return switch (k) {
            case 0 -> gateHall(u, h, v, ou, ov);
            case 1 -> barracks(u, h, v, ou, ov, room);
            case 2 -> drillYard(u, h, v, ou, ov, room);
            case 3 -> kitchens(u, h, v, room);
            case 4 -> bonePits(u, h, v, ou, ov);
            case 5 -> menagerie(u, h, v);
            case 6 -> vault(u, h, v, ou, ov);
            case 7 -> pengHalls(u, h, v, ou, ov, room);
            case 8 -> generals(u, h, v, ou, ov);
            default -> -1;
        };
    }

    private static int bones(int u, int h, int v, int salt, int many) {
        if (h != 1) return -1;
        int r = hash(u, 1, v, salt);
        if (r < many / 2) return c(LOW, D_SKULL);
        if (r < many) return c(DECOR, D_BONE);
        return -1;
    }

    private static int gateHall(int u, int h, int v, int ou, int ov) {
        // braziers between the pillars, the demons' standards, bones about
        if (Math.abs(u) == 14 && Math.floorMod(v, 7) == 4 && v > -26 && v < 28) return h == 1 ? c(DECOR, D_BRAZIER) : -1;
        return bones(u, h, v, 410, 8);
    }

    private static int barracks(int u, int h, int v, int ou, int ov, int room) {
        if (room < 55) {
            // rows of pallets: straw under red cloth
            if ((ov == 2 || ov == 5) && ou >= 1 && ou <= 6) return h == 1 ? c(DECOR, D_HAY) : h == 2 ? c(LOW, L_CARPET) : -1;
        } else if (room < 80) {
            // a mess: long tables
            if (ov == 3 && ou >= 1 && ou <= 6) return h == 1 ? c(DECOR, D_TABLE) : h == 2 && ou % 3 == 0 ? c(LOW, D_CANDLE) : -1;
        } else {
            // the arms: grindstones and anvils, barrels of spears
            if (ou == 2 && ov >= 1 && ov <= 6) return h == 1 ? c(DECOR, ov % 2 == 0 ? D_ANVIL : D_GRINDSTONE) : -1;
            if (ou == 5 && ov >= 2 && ov <= 5) return h <= 2 ? c(DECOR, D_BARREL) : -1;
        }
        return bones(u, h, v, 411, 5);
    }

    private static int drillYard(int u, int h, int v, int ou, int ov, int room) {
        int q = Math.max(Math.abs(u), Math.abs(v));
        if (q <= 14) {
            // the yard: straw men on posts to strike at
            if (ou == 4 && ov == 4) return h == 1 ? c(DECOR, D_FENCE) : h == 2 ? c(DECOR, D_HAY) : h == 3 ? c(DECOR, D_TARGET) : -1;
            return -1;
        }
        // round it the reviewing stands and the racks
        if (q >= 23 && q <= 26) {
            if (h <= q - 23 + 1 && h <= 3) return c(WALL, L_BAND);
            return -1;
        }
        if (q == 17 && Math.floorMod(u + v, 3) == 0) return h == 1 ? c(DECOR, D_GRINDSTONE) : -1;
        return bones(u, h, v, 412, 4);
    }

    private static int kitchens(int u, int h, int v, int room) {
        int ou = Math.floorMod(u, 10), ov = Math.floorMod(v, 10);
        // the great cauldrons on their fires, in the middle of each hearth
        if (ou == 5 && ov == 5) return h == 1 ? c(DECOR, D_FIRE) : h == 2 ? c(DECOR, D_CAULDRON) : -1;
        if ((ou == 4 || ou == 6) && (ov == 4 || ov == 6)) return h == 1 ? c(DECOR, D_SLAB) : -1;
        // the steamers stacked by them, three high: what the Tang Monk was to be cooked in
        if (ou == 8 && (ov == 2 || ov == 3)) return h <= 3 ? c(DECOR, D_STACK) : -1;
        // the pens where they kept what they meant to eat
        if (ou <= 1 && ov >= 7) {
            if (h <= 3) return (ou == 0 || ov == 7) ? c(DECOR, D_BARS) : (h == 1 && room < 50 ? c(LOW, D_SKULL) : -1);
            return -1;
        }
        return bones(u, h, v, 413, 6);
    }

    private static int bonePits(int u, int h, int v, int ou, int ov) {
        if (pit(u, v)) {
            // the pits: heaped with bones, webs over them
            int ou2 = Math.floorMod(u + 4, 12), ov2 = Math.floorMod(v + 4, 12);
            int heap = 3 - Math.max(Math.abs(ou2 - 4) + Math.abs(ov2 - 5), 0) / 2;
            if (h <= heap && h <= 2) return c(DECOR, D_HEAP);
            if (h == 1 && hash(u, h, v, 414) < 40) return c(LOW, D_COBWEB);
            if (h == heap + 1 && hash(u, h, v, 415) < 30) return c(LOW, D_SKULL);
            return -1;
        }
        if (h >= 8 && hash(u, h, v, 416) < 10) return c(LOW, D_COBWEB);
        return bones(u, h, v, 417, 14);
    }

    private static int menagerie(int u, int h, int v) {
        int ou = Math.floorMod(u, 14), ov = Math.floorMod(v, 14);
        if (pen(u, v)) {
            boolean rim = ou == 2 || ou == 10 || ov == 2 || ov == 10;
            boolean gap = (ou == 6 || ou == 5) && ov == 10;
            if (rim && !gap) return h <= 4 ? c(DECOR, D_BARS) : h == 5 ? c(DECOR, D_SLAB) : -1;
            if (ou == 3 && ov >= 4 && ov <= 8) return h == 1 ? c(DECOR, D_HAY) : -1;
            if (ou == 9 && ov == 6) return h == 1 ? c(DECOR, D_TROUGH) : -1;
            return bones(u, h, v, 418, 6);
        }
        return bones(u, h, v, 419, 3);
    }

    private static int vault(int u, int h, int v, int ou, int ov) {
        if (pool(u, v)) return -1;
        // the hoard heaped in the vault: gold, the kings' tribute chests (sealed), on plinths
        if (ou == 4 && ov == 4) return h == 1 ? c(WALL, L_TRIM) : h == 2 ? c(DECOR, D_LOW_SOUL_FIRE) : -1;
        if ((ou == 2 || ou == 6) && (ov == 2 || ov == 6)) return h == 1 ? c(DECOR, D_STACK) : -1;
        return -1;
    }

    private static int pengHalls(int u, int h, int v, int ou, int ov, int room) {
        // the Great Peng's empty halls: feathers of gold on the floor, perches high
        if (ou == 4 && ov == 4 && room < 60) return h <= 5 ? c(WALL, h == 5 ? L_GOLD : L_PILLAR) : h == 6 ? c(DECOR, D_LOW_SOUL_FIRE) : -1;
        if (h == 1 && hash(u, 1, v, 420) < 6) return c(LOW, D_FEATHER);
        return bones(u, h, v, 421, 3);
    }

    private static int generals(int u, int h, int v, int ou, int ov) {
        if (moat(u, v)) return -1;
        // the generals' standards, braziers along the moats, their arms
        if (ou == 4 && ov == 4) return h == 1 ? c(DECOR, D_BRAZIER) : -1;
        if (ou == 0 && ov == 4) return h <= 2 ? c(WALL, L_PILLAR) : h == 3 ? c(LOW, D_SKULL) : -1;
        return bones(u, h, v, 422, 4);
    }

    // ── The throne room ─────────────────────────────────────────────────

    /** The dais (v from its front to the north wall), the thrones' row, the Lion's, the Elephant's and the Peng's (u). */
    public static final int DAIS = -63, THRONE_ROW = -68, LION_U = 0, ELEPHANT_U = -13, PENG_U = 13;
    /** The two kings' places (u, v) on the floor before the dais. */
    public static final int[] LION_POST = {LION_U, -54}, ELEPHANT_POST = {ELEPHANT_U, -50};
    /** The treasury: the chest beside the Great Peng's empty throne, on the dais (u, local y, v). */
    public static final int[] TREASURY = {PENG_U + 4, THRONE_Y + 3, THRONE_ROW};

    private static int throneFloor(int u, int v) {
        int au = Math.abs(u);
        if (au <= 2 && v > DAIS) return c(FLOOR, L_CARPET);
        if (Math.floorMod(u, 6) == 0 && Math.floorMod(v, 6) == 0) return c(FLOOR, L_GOLD);
        return c(FLOOR, au <= 4 || Math.floorMod(u + v, 12) == 0 ? L_INLAY : L_PLAIN);
    }

    /** The great pillars of the throne room: two rows down its length. */
    static boolean thronePillar(int u, int v) {
        int au = Math.abs(u);
        return au >= 17 && au <= 19 && Math.floorMod(v + 1, 11) <= 2 && v <= -14 && v >= -64;
    }

    private static int throne(int u, int h, int v) {
        int au = Math.abs(u), air = THRONE_AIR;
        if (thronePillar(u, v)) return c(WALL, h == 1 || h >= air - 1 ? L_PILLAR_CAP : h % 6 == 0 ? L_GOLD : L_PILLAR);
        // the dais: two steps up to it, then the thrones
        if (v <= DAIS) {
            if (h == 1) return v == DAIS ? c(STEP, L_STEP, NORTH) : c(WALL, L_TRIM);
            if (h == 2 && v <= DAIS - 1) return v == DAIS - 1 ? c(STEP, L_STEP, NORTH) : c(WALL, L_TRIM);
            int[] seats = {LION_U, ELEPHANT_U, PENG_U};
            for (int x : seats) {
                int d = Math.abs(u - x);
                boolean lion = x == LION_U;
                int back = lion ? 9 : 7, wide = lion ? 2 : 1;
                if (v == THRONE_ROW) {
                    if (h == 3 && d <= wide) return c(DECOR, D_THRONE_SEAT, SOUTH);
                    if (h <= 4 && d == wide + 1) return c(DECOR, D_THRONE_ARM);
                }
                if (v == THRONE_ROW - 1 && d <= wide + 1 && h >= 3 && h <= back && !(h == back && d == wide + 1)) {
                    return c(DECOR, D_THRONE_BACK);
                }
                if (v == THRONE_ROW - 1 && d == 0 && h == back + 1) return c(LOW, D_SKULL);
            }
            // the treasury beside the Peng's empty throne
            if (u == TREASURY[0] && h == 3 && v == TREASURY[2]) return c(CHEST, 0, SOUTH);
            // the backdrop behind the thrones: red lacquer in a frame of gold, a gold lion's face over the Lion's
            if (v == THRONE_N && au <= 24 && h >= 3 && h <= THRONE_AIR - 1) {
                if (au == 24 || h == THRONE_AIR - 1 || h == 3) return c(WALL, L_GOLD);
                if (au <= 7 && h >= 11 && h <= 17 && (au + h) % 3 != 0) return c(WALL, L_GOLD);
                return c(WALL, L_PLAIN);
            }
            if (h == 3 && (au == 22 || au == 26) && v == THRONE_ROW + 2) return c(DECOR, D_FIRE);
            if (h == 3 && hash(u, h, v, 430) < 6) return c(LOW, D_SKULL);
            return c(OPEN, 0);
        }
        // braziers down the nave's sides, bones heaped in the corners, lanterns on chains
        if (au == 9 && Math.floorMod(v, 8) == 2 && v < -12) return h == 1 ? c(DECOR, D_FIRE) : c(OPEN, 0); // fires, not soul fire: the kings sit in red light
        if (au >= 26 && Math.abs(v - (THRONE_N + THRONE_S) / 2) > 20) {
            int heap = (int) Math.round(3.5 - 0.6 * (HALF - au) - (hash(u, 0, v, 431) - 50) / 60.0);
            if (h <= heap) return c(DECOR, D_HEAP);
        }
        if (Math.floorMod(u, 10) == 5 && Math.floorMod(v, 10) == 5 && !lane(THRONE, u, v)) {
            if (h >= air - 3) return c(LOW, h == air - 3 ? D_LAMP : D_CHAIN);
        }
        if (au == HALF && Math.floorMod(v, 8) == 4 && h >= 6 && h <= 12) return c(DECOR, D_BANNER_RED);
        if (!lane(THRONE, u, v) && h == 1 && hash(u, h, v, 432) < 4) return c(LOW, D_SKULL);
        return c(OPEN, 0);
    }

    // ── Who keeps it ────────────────────────────────────────────────────

    /** Who keeps the ridge and the cave: its demons, and its two kings. */
    public enum Who {
        LION_DEMON, ELEPHANT_DEMON, PENG_DEMON, WIND_DRILLER, AZURE_LION, WHITE_ELEPHANT;

        public EntityType<? extends Mob> type() {
            return switch (this) {
                case LION_DEMON -> ModEntities.LION_DEMON.get();
                case ELEPHANT_DEMON -> ModEntities.ELEPHANT_DEMON.get();
                case PENG_DEMON -> ModEntities.PENG_DEMON.get();
                case WIND_DRILLER -> ModEntities.LITTLE_WIND_DRILLER.get();
                case AZURE_LION -> ModEntities.AZURE_LION.get();
                case WHITE_ELEPHANT -> ModEntities.WHITE_ELEPHANT.get();
            };
        }
    }

    /** A post: who stands there, as what (a regular, a captain, a floor's warden, a king), on which floor (-1 the forecourt), where (u, v). */
    public record Post(Who who, int role, int floor, int u, int v) {
        /** The local y it stands at: the first block over its floor. */
        public int ly() {
            return floor < 0 ? 1 : floor == THRONE ? floorY(THRONE) + 1 : floorY(floor) + 1;
        }

        public int lz() {
            return FZ + v;
        }
    }

    public static final int REGULAR = 0, CAPTAIN = 1, WARDEN = 2, KING = 3;

    /** The level a regular of floor {@code k} is (the gate hall and forecourt the zone's least, deeper ever higher), a captain, a warden. */
    public static int levelOf(int k, int role) {
        int depth = Math.max(0, Math.min(FLOORS, k));
        return switch (role) {
            case REGULAR -> 89 + depth;
            case CAPTAIN -> Math.min(100, 93 + depth);
            default -> Math.min(100, 94 + depth);
        };
    }

    /** Who keeps each floor (its regulars, in turn), its warden's kind. */
    private static List<Who> keepers(int k) {
        return switch (k) {
            case 0, 1 -> List.of(Who.LION_DEMON, Who.LION_DEMON, Who.WIND_DRILLER);
            case 2 -> List.of(Who.LION_DEMON, Who.ELEPHANT_DEMON);
            case 3 -> List.of(Who.LION_DEMON, Who.WIND_DRILLER, Who.LION_DEMON);
            case 4 -> List.of(Who.LION_DEMON, Who.PENG_DEMON);
            case 5 -> List.of(Who.ELEPHANT_DEMON, Who.ELEPHANT_DEMON, Who.LION_DEMON);
            case 6 -> List.of(Who.PENG_DEMON, Who.LION_DEMON);
            case 7 -> List.of(Who.PENG_DEMON, Who.PENG_DEMON, Who.LION_DEMON);
            default -> List.of(Who.LION_DEMON, Who.ELEPHANT_DEMON, Who.PENG_DEMON);
        };
    }

    private static Who warden(int k) {
        return switch (k % 4) {
            case 0 -> Who.ELEPHANT_DEMON;
            case 1 -> Who.LION_DEMON;
            case 2 -> Who.ELEPHANT_DEMON;
            default -> Who.PENG_DEMON;
        };
    }

    /** How many keep floor {@code k}: its regulars and captains (its warden aside). */
    public static int regulars(int k) {
        return k == 0 ? 5 : 6 + k;
    }

    public static int captains(int k) {
        return k == 0 ? 1 : 1 + k / 3;
    }

    /** The places tried for a floor's keepers, in turn, about its hall. */
    private static final int[][] SPOTS = {{-20, -12}, {16, 14}, {-12, 18}, {22, -18}, {6, -26}, {-24, 26}, {12, 2}, {-6, -6}, {26, 26},
            {-26, -26}, {0, 12}, {-16, 0}, {18, -4}, {-4, 24}, {8, -14}, {-22, 10}, {24, 8}, {-10, -22}, {14, 24}, {2, -20},
            {-14, -27}, {27, -10}, {-27, 16}, {10, 27}, {-5, 7}, {5, -9}, {20, 20}, {-20, 20}, {20, -24}, {-17, -17}, {11, 11}, {-11, 4}};

    /** Every post of the fortress, the forecourt's wardens first, then the gate hall and the floors in turn, the kings last. */
    public static final List<Post> POSTS = posts();

    private static List<Post> posts() {
        List<Post> out = new ArrayList<>();
        // the door's wardens in the forecourt: two elephant-demon captains, lion-demons and peng-demons
        int[][] door = {{-4, DOOR_V + 9}, {4, DOOR_V + 9}, {-9, DOOR_V + 12}, {9, DOOR_V + 12}, {-15, DOOR_V + 8}, {15, DOOR_V + 8}};
        for (int i = 0; i < door.length; i++) {
            var who = i < 2 ? Who.ELEPHANT_DEMON : i < 4 ? Who.LION_DEMON : Who.PENG_DEMON;
            out.add(new Post(who, i < 2 ? CAPTAIN : REGULAR, -1, door[i][0], door[i][1]));
        }
        for (int k = 0; k <= FLOORS; k++) {
            var keepers = keepers(k);
            int n = regulars(k), caps = captains(k), placed = 0;
            for (int[] s : SPOTS) {
                if (placed >= n) break;
                // mirrored on odd floors, so no two floors are kept alike
                int u = k % 2 == 1 ? -s[0] : s[0], v = k % 3 == 2 ? -s[1] : s[1];
                if (!standable(k, u, v) || nearStairs(k, u, v)) continue;
                out.add(new Post(keepers.get(placed % keepers.size()), placed < caps ? CAPTAIN : REGULAR, k, u, v));
                placed++;
            }
            int[] g = gatePost(k);
            out.add(new Post(warden(k), WARDEN, k, g[0], g[1]));
        }
        out.add(new Post(Who.WHITE_ELEPHANT, KING, THRONE, ELEPHANT_POST[0], ELEPHANT_POST[1]));
        out.add(new Post(Who.AZURE_LION, KING, THRONE, LION_POST[0], LION_POST[1]));
        return List.copyOf(out);
    }

    /** Whether one may stand at (u, v) of floor {@code k}: ground under, room over (two blocks, three for the big ones). */
    public static boolean standable(int k, int u, int v) {
        int ly = floorY(k);
        if (!ground(kind(cell(u, ly, FZ + v))) || kind(cell(u, ly, FZ + v)) == LAVA) return false;
        for (int h = 1; h <= 3; h++) if (solid(kind(cell(u, ly + h, FZ + v)))) return false;
        return true;
    }

    // ── The blocks ──────────────────────────────────────────────────────

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static Direction dir(int facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            default -> Direction.EAST;
        };
    }

    private static BlockState bars() {
        return Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                .setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
    }

    /** The floor whose theme local y {@code ly} takes (a floor's own top of the slab under it takes its). */
    private static int themeAt(int ly, int lz) {
        if (lz - FZ > HALF || ly > 0) return 0;
        int k = bandOf(ly);
        if (k == THRONE) return ly == floorY(FLOORS) ? FLOORS : THRONE;
        return ly - floorY(k) == PITCH ? k - 1 : k;
    }

    /** What stands at local (lx, ly, lz): a block, or null where the fortress leaves the ground be. */
    @Nullable
    public static BlockState at(int lx, int ly, int lz) {
        return state(cell(lx, ly, lz), lx, ly, lz);
    }

    /** The block of a cell ({@link #cell}) at local (lx, ly, lz), or null for none. */
    @Nullable
    public static BlockState state(int cell, int lx, int ly, int lz) {
        int kind = kind(cell);
        if (kind == OUT) return null;
        int look = look(cell), k = themeAt(ly, lz);
        int x = lx, z = lz;
        return switch (kind) {
            case OPEN -> s(Blocks.AIR);
            case ROCK -> rock(x, ly, z);
            case WALL -> wall(k, look, x, ly, z);
            case FLOOR -> floor(k, look, x, ly, z);
            case STEP -> Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dir(facing(cell)));
            case GATE -> bars();
            case DOOR -> switch (look) {
                case D_SEAM -> s(Blocks.CHISELED_POLISHED_BLACKSTONE);
                case D_IRON -> s(Blocks.POLISHED_BLACKSTONE);
                case D_KNOCKER -> s(Blocks.GILDED_BLACKSTONE);
                case D_STUD -> s(Blocks.GOLD_BLOCK);
                default -> Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
            };
            case LAVA -> s(Blocks.LAVA);
            case CHEST -> Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, dir(facing(cell)));
            case BED -> s(Blocks.RED_BED);
            default -> decoration(look, cell, x, ly, z);
        };
    }

    private static BlockState rock(int x, int y, int z) {
        int r = hash(x, y, z, 501);
        return r < 30 ? s(Blocks.DEEPSLATE_BRICKS) : r < 45 ? s(Blocks.CRACKED_DEEPSLATE_BRICKS) : r < 70 ? s(Blocks.DEEPSLATE_TILES)
                : r < 85 ? s(Blocks.COBBLED_DEEPSLATE) : s(Blocks.POLISHED_DEEPSLATE);
    }

    /** A wall's block by its floor's theme and its look. */
    private static BlockState wall(int k, int look, int x, int y, int z) {
        int r = hash(x, y, z, 502);
        switch (look) {
            case L_GOLD:
                return s(Blocks.GILDED_BLACKSTONE);
            case L_FILL:
                return r < 50 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            case D_PLAQUE:
                return s(Blocks.GILDED_BLACKSTONE);
            case D_STATUE:
                return r < 70 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.BLACKSTONE);
            case D_STATUE_HEAD:
                return s(Blocks.CHISELED_POLISHED_BLACKSTONE);
            case D_SLIT:
                return s(Blocks.IRON_BARS);
            default:
                break;
        }
        return switch (k) {
            case 0, 9 -> switch (look) {
                case L_PILLAR -> Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
                case L_PILLAR_CAP -> s(Blocks.CHISELED_POLISHED_BLACKSTONE);
                case L_TRIM, L_BAND -> s(Blocks.POLISHED_BLACKSTONE_BRICKS);
                default -> r < 15 ? s(Blocks.BROWN_TERRACOTTA) : s(Blocks.RED_TERRACOTTA);
            };
            case 1 -> switch (look) {
                case L_PILLAR -> Blocks.DARK_OAK_LOG.defaultBlockState();
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.STRIPPED_DARK_OAK_WOOD);
                case L_BAND -> s(Blocks.DEEPSLATE_TILES);
                default -> r < 60 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS);
            };
            case 2 -> switch (look) {
                case L_PILLAR -> s(Blocks.STONE_BRICKS);
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.CHISELED_STONE_BRICKS);
                case L_BAND -> s(Blocks.POLISHED_ANDESITE);
                default -> r < 30 ? s(Blocks.CRACKED_STONE_BRICKS) : r < 45 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
            };
            case 3 -> switch (look) {
                case L_PILLAR -> s(Blocks.BRICKS);
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.MUD_BRICKS);
                case L_BAND -> s(Blocks.BLACKSTONE);
                default -> r < 40 ? s(Blocks.MUD_BRICKS) : r < 60 ? s(Blocks.PACKED_MUD) : s(Blocks.BRICKS);
            };
            case 4 -> switch (look) {
                case L_PILLAR -> Blocks.BONE_BLOCK.defaultBlockState();
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.CHISELED_DEEPSLATE);
                case L_BAND -> s(Blocks.DEEPSLATE_TILES);
                default -> r < 25 ? Blocks.BONE_BLOCK.defaultBlockState() : r < 60 ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.CRACKED_DEEPSLATE_BRICKS);
            };
            case 5 -> switch (look) {
                case L_PILLAR -> Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.MOSSY_STONE_BRICKS);
                case L_BAND -> s(Blocks.MUD_BRICKS);
                default -> r < 40 ? s(Blocks.MOSSY_COBBLESTONE) : r < 70 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
            };
            case 6 -> switch (look) {
                case L_PILLAR -> s(Blocks.POLISHED_BLACKSTONE_BRICKS);
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.GILDED_BLACKSTONE);
                case L_BAND -> s(Blocks.POLISHED_BLACKSTONE);
                default -> r < 20 ? s(Blocks.GILDED_BLACKSTONE) : r < 60 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            };
            case 7 -> switch (look) {
                case L_PILLAR -> s(Blocks.YELLOW_TERRACOTTA);
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.GOLD_BLOCK);
                case L_BAND -> s(Blocks.ORANGE_TERRACOTTA);
                default -> r < 50 ? s(Blocks.RED_TERRACOTTA) : r < 75 ? s(Blocks.ORANGE_TERRACOTTA) : s(Blocks.TERRACOTTA);
            };
            default -> switch (look) {
                case L_PILLAR -> s(Blocks.RED_NETHER_BRICKS);
                case L_PILLAR_CAP, L_TRIM -> s(Blocks.CHISELED_NETHER_BRICKS);
                case L_BAND -> s(Blocks.POLISHED_BLACKSTONE);
                default -> r < 40 ? s(Blocks.NETHER_BRICKS) : r < 70 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.CRACKED_NETHER_BRICKS);
            };
        };
    }

    private static BlockState floor(int k, int look, int x, int y, int z) {
        int r = hash(x, y, z, 503);
        switch (look) {
            case L_CARPET:
                return s(Blocks.RED_WOOL);
            case L_GOLD:
                return s(Blocks.GILDED_BLACKSTONE);
            case L_MAGMA:
                return s(Blocks.MAGMA_BLOCK);
            case L_SAND:
                return r < 60 ? s(Blocks.SAND) : r < 85 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL);
            case L_BONES:
                return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, r < 50 ? Direction.Axis.X : Direction.Axis.Z);
            case L_MUD:
                return r < 50 ? s(Blocks.MUD) : r < 80 ? s(Blocks.PACKED_MUD) : s(Blocks.HAY_BLOCK);
            default:
                break;
        }
        return switch (k) {
            case 0 -> look == L_INLAY ? s(Blocks.GILDED_BLACKSTONE) : look == L_TRIM ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            case 1 -> look == L_INLAY ? s(Blocks.CHISELED_STONE_BRICKS) : look == L_TRIM ? s(Blocks.STONE_BRICKS) : r < 70 ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.DARK_OAK_PLANKS);
            case 2 -> look == L_TRIM ? s(Blocks.POLISHED_ANDESITE) : r < 50 ? s(Blocks.STONE_BRICKS) : r < 80 ? s(Blocks.ANDESITE) : s(Blocks.CRACKED_STONE_BRICKS);
            case 3 -> look == L_TRIM ? s(Blocks.BRICKS) : r < 50 ? s(Blocks.PACKED_MUD) : r < 80 ? s(Blocks.MUD_BRICKS) : s(Blocks.COARSE_DIRT);
            case 4 -> look == L_TRIM ? s(Blocks.DEEPSLATE_TILES) : r < 40 ? s(Blocks.COBBLED_DEEPSLATE) : r < 70 ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.GRAVEL);
            case 5 -> look == L_TRIM ? s(Blocks.STONE_BRICKS) : r < 50 ? s(Blocks.COARSE_DIRT) : r < 75 ? s(Blocks.ROOTED_DIRT) : s(Blocks.MOSSY_COBBLESTONE);
            case 6 -> look == L_INLAY ? s(Blocks.GOLD_BLOCK) : look == L_TRIM ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            case 7 -> look == L_INLAY ? s(Blocks.GOLD_BLOCK) : look == L_TRIM ? s(Blocks.YELLOW_TERRACOTTA) : r < 60 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.CUT_SANDSTONE);
            case 8 -> look == L_TRIM ? s(Blocks.POLISHED_BLACKSTONE) : r < 50 ? s(Blocks.NETHER_BRICKS) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            default -> look == L_INLAY ? s(Blocks.POLISHED_BLACKSTONE) : r < 50 ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.DEEPSLATE_TILES);
        };
    }

    private static BlockState decoration(int look, int cell, int x, int y, int z) {
        int r = hash(x, y, z, 504);
        return switch (look) {
            case D_LANTERN -> Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case D_LAMP -> Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case D_CHAIN -> s(Blocks.CHAIN);
            case D_SKULL -> Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r % 16);
            case D_BONE, D_HEAP -> Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                    r < 40 ? Direction.Axis.X : r < 80 ? Direction.Axis.Z : Direction.Axis.Y);
            case D_CAULDRON -> s(Blocks.CAULDRON);
            case D_FIRE -> Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false);
            case D_SOUL_FIRE, D_BRAZIER, D_LOW_SOUL_FIRE -> Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false);
            case D_BEACON -> Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true);
            case D_BARREL, D_STACK -> s(Blocks.BARREL);
            case D_HAY, D_TARGET -> look == D_TARGET ? s(Blocks.CARVED_PUMPKIN) : s(Blocks.HAY_BLOCK);
            case D_BARS, D_PEN -> bars();
            case D_ANVIL -> s(Blocks.DAMAGED_ANVIL);
            case D_GRINDSTONE -> s(Blocks.GRINDSTONE);
            case D_FENCE -> s(Blocks.DARK_OAK_FENCE);
            case D_TABLE -> Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            case D_SLAB -> Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case D_COBWEB -> s(Blocks.COBWEB);
            case D_THRONE_SEAT -> Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            case D_THRONE_ARM -> s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            case D_THRONE_BACK -> s(Blocks.GILDED_BLACKSTONE);
            case D_BANNER_RED -> s(Blocks.RED_WOOL);
            case D_BANNER_GOLD -> s(Blocks.YELLOW_WOOL);
            case D_TROUGH -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3);
            case D_FEATHER -> s(Blocks.YELLOW_CARPET);
            case D_CANDLE -> Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true);
            case L_CARPET -> s(Blocks.RED_CARPET);
            case L_STEP -> Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dir(facing(cell)))
                    .setValue(StairBlock.HALF, Half.BOTTOM);
            case L_RAIL -> s(Blocks.POLISHED_BLACKSTONE_WALL);
            default -> s(Blocks.AIR);
        };
    }

    /** Lays one block of the fortress at world {@code p}: the treasury's chest with its loot. */
    public static void lay(LevelAccessor level, BlockPos p, BlockState s, RandomSource random) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
        if (s.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, random, p, LionCamelCity.TREASURY_LOOT);
    }

    /** Lays the whole fortress round {@code ground} (the city's ground's level at the region's centre) at once, or only the floors from {@code from} to {@code to}: the gallery's, and tests'. */
    public static void layWhole(LevelAccessor level, BlockPos ground, int lyFrom, int lyTo) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -HALF - SHELL; x <= HALF + SHELL; x++) for (int z = FZ + THRONE_N - SHELL; z <= FZ + FACADE_V; z++) {
            if (!covers(x, z)) continue;
            for (int y = Math.max(BOTTOM, lyFrom); y <= Math.min(TOP, lyTo); y++) {
                BlockState s = at(x, y, z);
                if (s != null) lay(level, p.set(ground.getX() + x, ground.getY() + y, ground.getZ() + z), s, level.getRandom());
            }
        }
    }
}
