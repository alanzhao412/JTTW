package com.jttw.engine.world.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;

/**
 * Lion-Camel City (狮驼国), at the heart of each region of Lion-Camel Ridge: the kingdom the demons
 * took five hundred years before the pilgrims came, when the Great Peng ate its king and all his
 * court, and the city's people after them (chapter 74: "a city of demons... the streets were full
 * of tigers and wolves, and skeletons lay in heaps"). A blueprint, as the Dragon Kings' palaces and
 * the Fire Cloud Cave are: what stands at each local place, from the place alone, so each chunk of
 * the ridge lays its own share ({@code LionCamelRidgeFeature}) and the city comes out whole
 * whichever chunk is made first.
 * <ul>
 *   <li><b>the walls</b>: a square of grim deepslate brick {@value #WALL_H} high, three thick,
 *       {@value #WALL_OUT} blocks out from the centre each way, crenellated, skulls set on the
 *       battlements, breached here and there; a tower at each corner; a gate in each side, the
 *       main one to the south under a gatehouse with the city's plaque (狮驼国);</li>
 *   <li><b>the streets</b>: two old paved avenues crossing at the palace, a road round inside the
 *       walls, alleys between the blocks; skulls and bones lie in them;</li>
 *   <li><b>the houses</b>, in plots of nine by nine: ruined houses (broken walls, fallen roofs,
 *       cobwebs), the demons' kitchens (a cauldron on a fire, the steamer stacked by it, a pen of
 *       iron bars where they kept what they meant to eat), rubble, and yards heaped with bones;</li>
 *   <li><b>the palace</b> in its red-walled compound: a terrace, and on it the throne hall, its
 *       roof of dark tiles fallen in at one corner, red pillars, soul lanterns, the three demon
 *       kings' thrones (the third, the Great Peng's, empty), braziers in the courtyard; the Azure
 *       Lion holds court on the terrace at the centre;</li>
 *   <li><b>the elephant court</b> behind the palace, walled, ringed with great tusks of ivory, hay
 *       along its walls: the White Elephant's.</li>
 * </ul>
 * It is built for its kings, giants two and a half times a man's height whose true forms are
 * larger still: the hall stands {@value #HALL_H} high within, its thrones are a giant's, the gates
 * of the palace and of the elephant court {@value #KINGS_GATE_HIGH} high and {@code 2 *}
 * {@value #KINGS_GATE_HALF}{@code + 1} wide (a king walks from the one to the other: out of the
 * hall, round the terrace, through the palace's north gate and into his court), and the court
 * has room for the elephant he truly is.
 * Local y 0 is the city's ground (its paving), (0, 0) the region's centre, the main gate toward +z.
 */
public final class LionCamelCity {

    private LionCamelCity() {}

    /** The wall's inner and outer faces (blocks out from the centre each way), its height. */
    public static final int WALL_IN = 54, WALL_OUT = 56, WALL_H = 11;
    /** The corner towers: their centres' distance out, half-width, height. */
    public static final int TOWER_AT = 55, TOWER_HALF = 4, TOWER_H = 16;
    /** The gates: half-width of the way through and its height; the gatehouse over the main (south) gate. */
    public static final int GATE_HALF = 2, GATE_HIGH = 7, GATEHOUSE_HALF = 6, GATEHOUSE_H = 15;
    /** How far out anything of the city reaches. */
    public static final int OUTER = TOWER_AT + TOWER_HALF + 1;
    /** How far down its foundations go and how high it reaches. */
    public static final int BOTTOM = -3, TOP = 19;
    /** The ring road inside the walls starts this far out; the avenues are this wide either side of the middle. */
    public static final int RING = 49, AVENUE = 3;
    /** The plots: their first's near edge, their width, and the pitch between them (an alley between). */
    public static final int PLOT_FROM = 5, PLOT = 9, PITCH = 11, PLOTS = 4;

    /** The palace compound (its wall this far out each way, and so high), its terrace, its hall. */
    public static final int PALACE = 24, PALACE_WALL_H = 7;
    public static final int TERRACE_X = 17, TERRACE_N = -19, TERRACE_S = 11, TERRACE_H = 2;
    public static final int HALL_X = 14, HALL_N = -17, HALL_S = 9, HALL_H = 11;
    /** Where the thrones stand (local z), and the three of them (local x): the Elephant's, the Lion's, the Peng's; each a giant's, its back so high. */
    public static final int THRONE_Z = -15, THRONE_BACK = 5;
    public static final int[] THRONES = {-6, 0, 6};
    /** The elephant court: its extent and its centre; its wall's height. */
    public static final int COURT_X = 16, COURT_N = -50, COURT_S = -27, COURT_Z = -38, COURT_WALL_H = 6;
    /** The gates of the palace and of the elephant court, the kings' ways: half-width of the way through, and its height. */
    public static final int KINGS_GATE_HALF = 3, KINGS_GATE_HIGH = 6;

    // ── Small helpers ───────────────────────────────────────────────────

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** A hash of a place, 0-99. */
    static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    /** A finer hash of a place, 0-999. */
    static int fine(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 31 + 7, z), 1000L);
    }

    private static BlockState skull(int x, int z) {
        return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, hash(x, z, 91) % 16);
    }

    private static BlockState bone(int x, int z) {
        Direction.Axis[] axes = {Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Y};
        return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axes[hash(x, z, 92) % 3]);
    }

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState slab(Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    private static BlockState hanging(Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    private static BlockState wallBrick(int x, int y, int z) {
        int r = hash(x * 3 + y, z, 11);
        return r < 14 ? s(Blocks.CRACKED_DEEPSLATE_BRICKS) : r < 22 ? s(Blocks.DEEPSLATE_TILES) : r < 26 ? s(Blocks.COBBLED_DEEPSLATE) : s(Blocks.DEEPSLATE_BRICKS);
    }

    // ── Where things are ────────────────────────────────────────────────

    public static boolean covers(int x, int z) {
        return Math.max(Math.abs(x), Math.abs(z)) <= OUTER;
    }

    /** Whether local (x, z) is inside the walls. */
    public static boolean inside(int x, int z) {
        return Math.max(Math.abs(x), Math.abs(z)) < WALL_IN;
    }

    public static boolean inPalace(int x, int z) {
        return Math.abs(x) <= PALACE && Math.abs(z) <= PALACE;
    }

    public static boolean inCourt(int x, int z) {
        return Math.abs(x) <= COURT_X && z >= COURT_N && z <= COURT_S;
    }

    /** Whether local (x, z) is in one of the corner towers. */
    static boolean inTower(int x, int z) {
        return Math.abs(Math.abs(x) - TOWER_AT) <= TOWER_HALF && Math.abs(Math.abs(z) - TOWER_AT) <= TOWER_HALF;
    }

    static boolean inGatehouse(int x, int z) {
        return Math.abs(x) <= GATEHOUSE_HALF && z >= WALL_IN - 1 && z <= WALL_OUT + 2;
    }

    /** The plot index (1..PLOTS, signed by side) along one axis at {@code a}, or 0 between plots. */
    public static int plotIndex(int a) {
        int m = Math.abs(a) - PLOT_FROM;
        if (m < 0 || m >= PLOTS * PITCH || m % PITCH >= PLOT) return 0;
        return (m / PITCH + 1) * Integer.signum(a);
    }

    /** How far into its plot (0..PLOT-1) {@code a} lies, counted from the side nearer the city's middle. */
    public static int plotOffset(int a) {
        return (Math.abs(a) - PLOT_FROM) % PITCH;
    }

    /** What a plot is: a ruined house, the demons' kitchen, rubble, or a yard of bones. */
    public static final int HOUSE = 0, KITCHEN = 1, RUBBLE = 2, BONEYARD = 3;

    public static int plotKind(int px, int pz) {
        int r = hash(px * 17, pz * 13, 5);
        return r < 50 ? HOUSE : r < 66 ? KITCHEN : r < 84 ? RUBBLE : BONEYARD;
    }

    // ── The blueprint ───────────────────────────────────────────────────

    /**
     * Where the city's court and garrison stand (local x, y over the city's ground, z), kept clear
     * of the bones that lie everywhere else: the Azure Lion on the terrace, the White Elephant in
     * his court, lion-demon captains at the palace gate, lion- and peng-demons in the hall and on the
     * terrace, elephant-demons at the main gate and in the court, a Little Wind-Driller at each gate.
     * Who stands at each is the city feature's ({@code LionCamelCityFeature}).
     */
    public static final int[][] POSTS = {
            {0, TERRACE_H + 1, 0}, {0, 1, COURT_Z},
            {-4, 1, PALACE + 3}, {4, 1, PALACE + 3},
            {-10, TERRACE_H + 1, -6}, {10, TERRACE_H + 1, -6},
            {-12, TERRACE_H + 1, TERRACE_S - 1}, {12, TERRACE_H + 1, TERRACE_S - 1},
            {-5, 1, WALL_IN - 4}, {5, 1, WALL_IN - 4}, {0, 1, WALL_IN - 3},
            {0, 1, -WALL_IN + 3}, {WALL_IN - 3, 1, 0}, {-WALL_IN + 3, 1, 0},
            {-8, 1, COURT_Z - 8}, {8, 1, COURT_Z - 8}};

    /** Whether local (x, y, z) is where one of the court or the garrison stands, or the block over. */
    public static boolean postAt(int x, int y, int z) {
        for (int[] p : POSTS) if (p[0] == x && p[2] == z && (y == p[1] || y == p[1] + 1)) return true;
        return false;
    }

    /** What stands at local (x, y, z): a block, air where the city is open, or null where the ground is left as it is. */
    @Nullable
    public static BlockState at(int x, int y, int z) {
        BlockState s = laid(x, y, z);
        // where the court and the garrison stand, nothing in the way (a carpet may stay)
        return s != null && s.blocksMotion() && postAt(x, y, z) ? s(Blocks.AIR) : s;
    }

    @Nullable
    private static BlockState laid(int x, int y, int z) {
        if (!covers(x, z) || y < BOTTOM || y > TOP) return null;
        if (inTower(x, z)) return tower(x, y, z);
        if (inGatehouse(x, z)) return gatehouse(x, y, z);
        int q = Math.max(Math.abs(x), Math.abs(z));
        if (q >= WALL_IN && q <= WALL_OUT) return wall(x, y, z);
        if (q > WALL_OUT) return outside(x, y, z);
        if (y < 0) return null;
        if (inPalace(x, z)) return palace(x, y, z);
        if (inCourt(x, z)) return court(x, y, z);
        int px = plotIndex(x), pz = plotIndex(z);
        boolean street = px == 0 || pz == 0 || q >= RING;
        if (street) return street(x, y, z);
        return plot(px, pz, plotOffset(x), plotOffset(z), x, y, z);
    }

    /** Before the walls: skulls on stakes either side of the main gate, else the ground as the ridge has it. */
    private static BlockState outside(int x, int y, int z) {
        if (y < 1) return null;
        if (z == OUTER && Math.abs(x) == GATE_HALF + 3) {
            if (y <= 2) return s(Blocks.DARK_OAK_FENCE);
            if (y == 3) return skull(x, z);
        }
        return null;
    }

    private static BlockState wall(int x, int y, int z) {
        if (y < 0) return wallBrick(x, y, z);
        // which side, and how far along it
        boolean ns = Math.abs(z) >= Math.abs(x);
        int along = ns ? x : z, side = ns ? Integer.signum(z) * 2 : Integer.signum(x) * 3;
        int depth = Math.max(Math.abs(x), Math.abs(z)); // WALL_IN (inner face) to WALL_OUT (outer)
        if (y == 0) return wallBrick(x, y, z);
        // the gates
        if (Math.abs(along) <= GATE_HALF && y < GATE_HIGH + (Math.abs(along) <= GATE_HALF - 1 ? 1 : 0)) return s(Blocks.AIR);
        // breaches: where it has fallen, it stands only so high
        int top = WALL_H;
        int b = Math.floorMod(along * 7 + side * 13, 61);
        if (b < 4 && Math.abs(along) > GATEHOUSE_HALF + 2) top = 4 + hash(along, side, 3) % 3;
        if (y <= top) {
            if (depth == WALL_OUT && y == 8 && Math.floorMod(along, 6) == 3 && top == WALL_H) return s(Blocks.AIR); // arrow slits
            return wallBrick(x, y, z);
        }
        if (top < WALL_H) {
            // rubble atop a breach
            return y == top + 1 && hash(x, z, 4) < 35 ? s(Blocks.COBBLED_DEEPSLATE) : null;
        }
        // battlements on the outer row, a skull on every fourth
        if (depth == WALL_OUT && y == WALL_H + 1 && Math.floorMod(along, 2) == 0) return s(Blocks.DEEPSLATE_BRICK_WALL);
        if (depth == WALL_OUT && y == WALL_H + 2 && Math.floorMod(along, 8) == 0) return skull(x, z);
        return null;
    }

    private static BlockState tower(int x, int y, int z) {
        int dx = Math.abs(x) - TOWER_AT, dz = Math.abs(z) - TOWER_AT;
        boolean rim = Math.abs(dx) == TOWER_HALF || Math.abs(dz) == TOWER_HALF;
        if (y <= TOWER_H) {
            if (y > 0 && y < TOWER_H && rim && y % 4 == 2 && (dx == 0 || dz == 0)) return s(Blocks.AIR); // slits
            return y == TOWER_H ? s(Blocks.POLISHED_DEEPSLATE) : wallBrick(x, y, z);
        }
        if (y == TOWER_H + 1 && rim && Math.floorMod(dx + dz, 2) == 0) return s(Blocks.DEEPSLATE_BRICK_WALL);
        if (y == TOWER_H + 2 && rim && Math.abs(dx) == TOWER_HALF && Math.abs(dz) == TOWER_HALF) return skull(x, z);
        if (y == TOWER_H + 1 && dx == 0 && dz == 0) return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true);
        return null;
    }

    private static BlockState gatehouse(int x, int y, int z) {
        int ax = Math.abs(x);
        if (y < 0) return wallBrick(x, y, z);
        if (ax <= GATE_HALF && y >= 1 && y < GATE_HIGH + (ax <= GATE_HALF - 1 ? 1 : 0)) return s(Blocks.AIR);
        if (y == 0) return ax <= GATE_HALF ? s(Blocks.POLISHED_DEEPSLATE) : wallBrick(x, y, z);
        boolean front = z == WALL_OUT + 2;
        if (y <= GATEHOUSE_H) {
            if (front && y == 10 && ax <= 2) return s(Blocks.GILDED_BLACKSTONE);              // the plaque: 狮驼国
            if (front && y == 10 && ax == 3) return s(Blocks.CHISELED_POLISHED_BLACKSTONE);
            if (front && ax == GATE_HALF + 1 && y <= GATE_HIGH + 1) return s(Blocks.CHISELED_DEEPSLATE); // the jambs
            if (y == GATEHOUSE_H) return s(Blocks.POLISHED_DEEPSLATE);
            return wallBrick(x, y, z);
        }
        boolean rim = ax == GATEHOUSE_HALF || z == WALL_IN - 1 || z == WALL_OUT + 2;
        if (y == GATEHOUSE_H + 1 && rim && Math.floorMod(x + z, 2) == 0) return s(Blocks.DEEPSLATE_BRICK_WALL);
        if (y == GATEHOUSE_H + 2 && front && (ax == GATEHOUSE_HALF || ax == 2)) return skull(x, z);
        return null;
    }

    // ── The streets ─────────────────────────────────────────────────────

    private static BlockState street(int x, int y, int z) {
        boolean avenue = Math.abs(x) <= AVENUE || Math.abs(z) <= AVENUE;
        if (y == 0) {
            int r = hash(x, z, 21);
            if (avenue) return r < 45 ? s(Blocks.STONE_BRICKS) : r < 65 ? s(Blocks.CRACKED_STONE_BRICKS)
                    : r < 80 ? s(Blocks.COBBLESTONE) : r < 90 ? s(Blocks.GRAVEL) : s(Blocks.MOSSY_STONE_BRICKS);
            return r < 35 ? s(Blocks.COARSE_DIRT) : r < 60 ? s(Blocks.GRAVEL) : r < 80 ? s(Blocks.COBBLESTONE) : s(Blocks.PACKED_MUD);
        }
        if (y == 1 && !avenue) { // the avenues kept clear; the alleys and the ring road strewn
            int r = fine(x, z, 22);
            if (r < 9) return skull(x, z);
            if (r < 13) return bone(x, z);
        }
        return null;
    }

    // ── The plots ───────────────────────────────────────────────────────

    private static BlockState plot(int px, int pz, int u, int v, int x, int y, int z) {
        return switch (plotKind(px, pz)) {
            case HOUSE -> house(px, pz, u, v, x, y, z);
            case KITCHEN -> kitchen(u, v, x, y, z);
            case RUBBLE -> rubble(x, y, z);
            default -> boneyard(u, v, x, y, z);
        };
    }

    private static BlockState houseBrick(int px, int pz, int x, int y, int z) {
        int kind = hash(px, pz, 9) % 3, r = hash(x * 5 + y, z, 12);
        return switch (kind) {
            case 0 -> r < 20 ? s(Blocks.CRACKED_STONE_BRICKS) : r < 35 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
            case 1 -> r < 30 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.COBBLESTONE);
            default -> r < 25 ? s(Blocks.PACKED_MUD) : s(Blocks.MUD_BRICKS);
        };
    }

    private static BlockState house(int px, int pz, int u, int v, int x, int y, int z) {
        int last = PLOT - 1;
        boolean edge = u == 0 || u == last || v == 0 || v == last;
        boolean corner = (u == 0 || u == last) && (v == 0 || v == last);
        int high = 4 + hash(px, pz, 6) % 2;
        if (y == 0) return edge ? houseBrick(px, pz, x, y, z) : hash(x, z, 13) < 70 ? s(Blocks.COARSE_DIRT) : s(Blocks.PACKED_MUD);
        if (edge) {
            if (corner) return y <= high ? s(Blocks.DARK_OAK_LOG) : null;
            // its door, toward whichever side it was built facing
            int door = hash(px, pz, 14) % 4;
            boolean doorway = switch (door) {
                case 0 -> v == 0 && u == 4;
                case 1 -> v == last && u == 4;
                case 2 -> u == 0 && v == 4;
                default -> u == last && v == 4;
            };
            if (doorway && y <= 2) return s(Blocks.AIR);
            // its walls stand broken: lower here and there
            int top = high - (hash(x, z, 7) < 40 ? 1 + hash(x, z, 8) % 3 : 0);
            if (y > top) return null;
            boolean along = (v == 0 || v == last) ? u % 3 == 1 : v % 3 == 1;
            if (y == 2 && along && !doorway) return hash(x, z, 15) < 50 ? s(Blocks.AIR) : s(Blocks.DARK_OAK_FENCE); // windows, their lattices broken
            return houseBrick(px, pz, x, y, z);
        }
        // a roof, if it is still there, holed
        boolean roofed = hash(px, pz, 10) < 45;
        if (roofed && y == high + 1 && hash(x, z, 11) >= 30) return slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM);
        if (y == 1) {
            if ((u == 1 || u == last - 1) && (v == 1 || v == last - 1) && hash(x, z, 16) < 55) return s(Blocks.COBWEB);
            int r = fine(x, z, 17);
            if (r < 30) return skull(x, z);
            if (r < 45) return bone(x, z);
        }
        if (y == high && (u == 1 || u == last - 1) && (v == 1 || v == last - 1) && hash(x, z, 18) < 40) return s(Blocks.COBWEB);
        return y <= high ? s(Blocks.AIR) : null;
    }

    /** The demons' kitchen: a great cauldron on a fire, the steamer they meant for the Tang Monk stacked by it, a pen of iron bars. */
    private static BlockState kitchen(int u, int v, int x, int y, int z) {
        if (y == 0) return (u >= 3 && u <= 5 && v >= 3 && v <= 5) ? s(Blocks.STONE_BRICKS) : hash(x, z, 31) < 50 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL);
        if (u == 4 && v == 4) return y == 1 ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false) : y == 2 ? s(Blocks.CAULDRON) : null;
        // the steamer: barrels stacked three high
        if (u == 6 && v == 4) return y <= 3 ? s(Blocks.BARREL) : null;
        if (u == 6 && v == 5) return y <= 2 ? s(Blocks.BARREL) : null;
        if ((u == 3 || u == 5) && (v == 3 || v == 5) && y == 1) return s(Blocks.CHISELED_STONE_BRICKS);
        // the pen
        boolean pen = u <= 2 && v >= 6;
        if (pen) {
            boolean rim = u == 0 || v == 8 || u == 2 || v == 6;
            if (rim && y <= 3) {
                if (u == 2 && v == 7 && y <= 2) return s(Blocks.AIR); // its door, open
                return y == 3 ? s(Blocks.DARK_OAK_SLAB) : Blocks.IRON_BARS.defaultBlockState()
                        .setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                        .setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
            }
            if (!rim && y == 1) return u == 1 && v == 7 ? skull(x, z) : null;
            return null;
        }
        if (y == 1) {
            int r = fine(x, z, 32);
            if (r < 60) return bone(x, z);
            if (r < 90) return skull(x, z);
        }
        return null;
    }

    private static BlockState rubble(int x, int y, int z) {
        if (y == 0) return hash(x, z, 41) < 60 ? s(Blocks.GRAVEL) : s(Blocks.COBBLESTONE);
        int r = hash(x, z, 42);
        if (y == 1 && r < 40) return r < 8 ? bone(x, z) : r < 25 ? s(Blocks.COBBLESTONE) : s(Blocks.MOSSY_COBBLESTONE);
        if (y == 2 && r < 12) return r < 4 ? s(Blocks.DARK_OAK_LOG) : s(Blocks.COBBLESTONE);
        if (y == 2 && r < 16 && fine(x, z, 43) < 400) return skull(x, z);
        return null;
    }

    /** A yard heaped with bones, skulls on the heap. */
    private static BlockState boneyard(int u, int v, int x, int y, int z) {
        if (y == 0) return s(Blocks.COARSE_DIRT);
        double d = Math.hypot(u - 4, v - 4);
        int heap = (int) Math.floor(3.4 - d * 0.85 + (hash(x, z, 51) - 50) / 100.0);
        if (y <= heap) return hash(x, z, 52) < 85 ? bone(x * 3 + y, z) : s(Blocks.BONE_BLOCK);
        if (y == heap + 1 && heap >= 0 && hash(x, z, 53) < 30) return skull(x, z);
        return null;
    }

    // ── The palace ──────────────────────────────────────────────────────

    /** Whether local (x, z) is under the hall's fallen corner: its roof and beams gone, their wreck on the floor. */
    public static boolean fallen(int x, int z) {
        return Math.hypot(x - 8, z + 9) < 4.6 + (hash(x, z, 61) - 50) / 80.0;
    }

    private static BlockState palace(int x, int y, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        boolean rim = Math.max(ax, az) == PALACE;
        if (rim) {
            if (ax <= KINGS_GATE_HALF && az == PALACE && y >= 1 && y <= KINGS_GATE_HIGH) return s(Blocks.AIR); // its gates, south and north
            if (y == 0) return s(Blocks.STONE_BRICKS);
            if (ax == KINGS_GATE_HALF + 1 && az == PALACE && y <= KINGS_GATE_HIGH + 1) return s(Blocks.STRIPPED_MANGROVE_LOG); // their posts
            if (y <= PALACE_WALL_H) return hash(x * 7 + y, z, 62) < 12 ? s(Blocks.BROWN_TERRACOTTA) : s(Blocks.RED_TERRACOTTA);
            if (y == PALACE_WALL_H + 1) return slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM);
            return null;
        }
        boolean terrace = ax <= TERRACE_X && z >= TERRACE_N && z <= TERRACE_S;
        if (terrace) return terrace(x, y, z);
        // the steps up to it
        if (ax <= 4 && (z == TERRACE_S + 1 || z == TERRACE_S + 2)) {
            if (y == 0) return s(Blocks.STONE_BRICKS);
            if (z == TERRACE_S + 2) return y == 1 ? stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH) : null;
            return y == 1 ? s(Blocks.STONE_BRICKS) : y == 2 ? stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH) : null;
        }
        // the courtyard
        if (y == 0) {
            if (ax <= 2 && z > TERRACE_S) return s(Blocks.POLISHED_DEEPSLATE);                 // the way in
            return hash(x, z, 63) < 75 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.ANDESITE);
        }
        // two braziers of soul fire either side of the way
        if (ax == 10 && z == 17) {
            if (y == 1) return s(Blocks.POLISHED_BLACKSTONE);
            if (y == 2) return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false);
            return null;
        }
        if (y == 1 && ax > 3) {
            int r = fine(x, z, 64);
            if (r < 25) return skull(x, z);
            if (r < 40) return bone(x, z);
        }
        return null;
    }

    private static BlockState terrace(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean side = ax == TERRACE_X || z == TERRACE_N || z == TERRACE_S;
        if (y <= 0) return s(Blocks.STONE_BRICKS);
        if (y < TERRACE_H) return side ? s(Blocks.CHISELED_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
        if (y == TERRACE_H) {
            boolean inHall = ax <= HALL_X && z >= HALL_N && z <= HALL_S;
            if (inHall) return Math.floorMod(x + z, 2) == 0 ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.DEEPSLATE_TILES);
            return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        return hall(x, y - TERRACE_H, z);
    }

    /** Whether local (x, z) is one of the hall's red pillars. */
    public static boolean pillar(int x, int z) {
        int ax = Math.abs(x);
        return (ax == HALL_X || ax == 8) && (z - HALL_N) % 6 == 0 && z >= HALL_N && z <= HALL_S - 2
                || (ax == HALL_X || ax == 8) && z == HALL_S;
    }

    /** The hall over the terrace, {@code h} blocks up from its floor. */
    private static BlockState hall(int x, int h, int z) {
        int ax = Math.abs(x);
        boolean inHall = ax <= HALL_X && z >= HALL_N && z <= HALL_S;
        boolean under = ax <= HALL_X + 2 && z >= HALL_N - 2 && z <= HALL_S + 2;
        if (!under) {
            // on the terrace before the hall: nothing but bones
            if (h == 1 && fine(x, z, 65) < 20) return bone(x, z);
            return null;
        }
        boolean broken = fallen(x, z);
        // the roof: tiers of dark tile over the beams
        if (h > HALL_H) return roof(x, h - HALL_H, z, broken);
        if (pillar(x, z)) return Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
        if (!inHall) return null;
        if (h == HALL_H) {
            if (broken) return null;
            return (ax % 6 == 2 || (z - HALL_N) % 6 == 0) ? s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, ax % 6 == 2 ? Direction.Axis.Z : Direction.Axis.X)
                    : s(Blocks.DARK_OAK_PLANKS);
        }
        // the back wall, a mural of gold in it
        if (z == HALL_N) {
            if (h >= 4 && h <= 7 && ax <= 6 && (ax + h) % 3 != 0) return s(Blocks.GILDED_BLACKSTONE);
            return hash(x * 3 + h, z, 66) < 15 ? s(Blocks.BROWN_TERRACOTTA) : s(Blocks.RED_TERRACOTTA);
        }
        // the side walls: low walls, lattice over them, fallen in places
        if (ax == HALL_X) {
            if (hash(x, z * 5, 67) < 25 && h > 2) return null; // fallen
            if (h <= 2) return s(Blocks.RED_TERRACOTTA);
            if (h <= 7) return s(Blocks.DARK_OAK_FENCE);
            return s(Blocks.RED_TERRACOTTA);
        }
        // the front: open between its pillars, a lattice frieze under the beams
        if (z == HALL_S) return h >= HALL_H - 2 ? s(Blocks.DARK_OAK_FENCE) : s(Blocks.AIR);
        // within
        if (h == 1) {
            // the dais and the three thrones
            if (z >= THRONE_Z - 1 && z <= THRONE_Z + 1 && ax <= 9) return s(Blocks.POLISHED_BLACKSTONE);
            if (z == THRONE_Z + 2 && ax <= 9) return slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM);
            if (broken && hash(x, z, 68) < 55) return hash(x, z, 69) < 50 ? s(Blocks.DEEPSLATE_TILES) : s(Blocks.DARK_OAK_PLANKS); // the wreck of the roof
            // a carpet up the nave
            if (ax <= 1 && z > THRONE_Z + 2 && z < HALL_S) return s(Blocks.RED_CARPET);
            int r = fine(x, z, 70);
            if (ax > 2 && r < 25) return skull(x, z);
            if (ax > 2 && r < 37) return bone(x, z);
            return s(Blocks.AIR);
        }
        // the thrones, a giant's each: a seat three wide, arms, a gilded back, a skull atop it
        if (h >= 2 && h <= THRONE_BACK + 2) {
            for (int t : THRONES) {
                int d = Math.abs(x - t);
                if (h == 2 && d <= 1 && z == THRONE_Z) return stair(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.NORTH);
                if (d == 2 && z == THRONE_Z && h <= 3) return h == 2 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM);
                if (z == THRONE_Z - 1 && (d <= 2 && h <= THRONE_BACK || d <= 1 && h == THRONE_BACK + 1)) return s(Blocks.GILDED_BLACKSTONE);
                if (z == THRONE_Z - 1 && d == 0 && h == THRONE_BACK + 2) return skull(x, z);
            }
        }
        if (h == 2 && broken && hash(x, z, 68) < 20) return s(Blocks.DEEPSLATE_TILES);
        // soul lanterns hung from the beams down the nave
        if (h == HALL_H - 1 && !broken && (ax == 4) && Math.floorMod(z - HALL_N, 6) == 3) return hanging(Blocks.SOUL_LANTERN);
        return s(Blocks.AIR);
    }

    /** The roof's {@code tier}th layer over the beams: each smaller, eaves of stairs round its rim, a ridge on top. */
    private static BlockState roof(int x, int tier, int z, boolean broken) {
        if (broken) return null;
        int shrink = (tier - 1) * 3;
        int hx = HALL_X + 2 - shrink, n = HALL_N - 2 + shrink, so = HALL_S + 2 - shrink;
        if (hx < 2 || so - n < 2) return null;
        int ax = Math.abs(x);
        if (ax > hx || z < n || z > so) return null;
        boolean ex = ax == hx, en = z == n, es = z == so;
        if ((ex ? 1 : 0) + (en || es ? 1 : 0) >= 2) return slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM);
        if (en) return stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH);
        if (es) return stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH);
        if (ex) return stair(Blocks.DEEPSLATE_TILE_STAIRS, x > 0 ? Direction.WEST : Direction.EAST);
        return s(Blocks.DEEPSLATE_TILES);
    }

    // ── The elephant court ──────────────────────────────────────────────

    /** The tusks round the court: their feet (local x, z), each curving in toward the court's middle. */
    public static final int[][] TUSKS = {{-11, COURT_Z - 7}, {11, COURT_Z - 7}, {-12, COURT_Z}, {12, COURT_Z}, {-11, COURT_Z + 7}, {11, COURT_Z + 7}};
    public static final int TUSK_H = 8;

    private static BlockState court(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean rim = ax == COURT_X || z == COURT_N || z == COURT_S;
        if (rim) {
            if (ax <= KINGS_GATE_HALF && (z == COURT_N || z == COURT_S) && y >= 1 && y <= KINGS_GATE_HIGH) return s(Blocks.AIR);
            if (y == 0) return s(Blocks.STONE_BRICKS);
            if (y <= COURT_WALL_H) return hash(x * 3 + y, z, 71) < 30 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
            if (y == COURT_WALL_H + 1) return Math.floorMod(x + z, 2) == 0 ? s(Blocks.STONE_BRICK_WALL) : null;
            return null;
        }
        if (y == 0) {
            double d = Math.hypot(x, (z - COURT_Z) * 1.2);
            if (d < 8) return hash(x, z, 72) < 70 ? s(Blocks.COARSE_DIRT) : s(Blocks.ROOTED_DIRT);   // trampled
            return hash(x, z, 73) < 60 ? s(Blocks.MUD_BRICKS) : s(Blocks.PACKED_MUD);
        }
        // the tusks: ivory rising and bending inward
        for (int[] t : TUSKS) {
            int tx = t[0], tz = t[1];
            int lean = y >= TUSK_H - 1 ? 1 : 0, lean2 = y >= TUSK_H ? 1 : 0;
            int inX = -Integer.signum(tx) * (lean + lean2);
            if (y <= TUSK_H && x == tx + inX && z == tz) return y == TUSK_H ? s(Blocks.BONE_BLOCK).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X) : s(Blocks.BONE_BLOCK);
        }
        // hay along the walls, a trough of water
        if (y == 1 && ax == COURT_X - 1 && (z - COURT_N) % 4 != 0 && z < COURT_Z - 2) return s(Blocks.HAY_BLOCK);
        if (y == 1 && ax == COURT_X - 1 && z >= COURT_Z + 2 && z <= COURT_Z + 6) return Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3);
        return null;
    }

    /** Lays the whole city round {@code ground} (its ground's level at the centre) at once: the gallery's, and tests'. */
    public static void layWhole(LevelAccessor level, BlockPos ground) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -OUTER; x <= OUTER; x++) for (int z = -OUTER; z <= OUTER; z++) for (int y = BOTTOM; y <= TOP; y++) {
            BlockState s = at(x, y, z);
            if (s != null) level.setBlock(p.set(ground.getX() + x, ground.getY() + y, ground.getZ() + z), s, Block.UPDATE_CLIENTS);
        }
    }
}
