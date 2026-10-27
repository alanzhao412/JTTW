package com.jttw.engine.world.kunlun;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.hell.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.WallSide;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Queen Mother of the West's palace on the summit of Mount Kunlun, by the Jade Pool (瑶池): a
 * blueprint, as Fengdu is, what stands at each local place from the place alone, so each chunk of
 * the summit lays its own share ({@code KunlunFeature}) and it comes out whole whichever is made first.
 * <ul>
 *   <li><b>the terrace</b>: the summit paved in white jade (quartz, diorite, calcite) {@value #TERRACE}
 *       blocks out, a white balustrade round it, open to the south where the stair comes up; round
 *       it the summit's snow, and snow-laden pines;</li>
 *   <li><b>the gate</b> (a 牌坊, three bays) at its south edge, white pillars, a beam trimmed with
 *       gold under a roof of celestial blue tiles, 昆仑 on its face and 瑶池 on its back;</li>
 *   <li><b>the lower court and the Jade Pool</b>: the pool in two great halves either side of a white
 *       causeway with rails and lamps, rimmed with jade, lit from below by sea lanterns (so it never
 *       freezes), lilies on it, and over the causeway's middle the Jade Terrace (瑶台), a pavilion of
 *       white pillars under a blue roof; peach trees of immortality in blossom round it;</li>
 *   <li><b>the upper terrace</b>, {@value #UP} blocks up a broad flight of steps: a court with peach
 *       trees, a bell tower and a drum tower either side under pyramid roofs, and two side halls of
 *       scriptures;</li>
 *   <li><b>the palace</b> (西王母宫) at its head on a white plinth up three more steps, a colonnade
 *       round it under a lower roof, white walls with pillars headed in gold, windows of blue glass,
 *       and over all a great hip roof of blue tiles with a gold-ended ridge; within, white pillars,
 *       lanterns, a blue carpet to a dais, and on it, before the Queen Mother's throne and a screen of
 *       jade and gold, between candles, the treasure chest ({@link #CHEST_X}, {@link #CHEST_Y},
 *       {@link #CHEST_Z}), where the Banana-Leaf Fan lies ({@link Kunlun#TREASURE}).</li>
 * </ul>
 * Nothing lives or fights here. Local y 0 is the summit's ground (the lower court's paving), (0, 0)
 * the region's centre, the gate toward +z (south).
 */
public final class KunlunPalace {

    private KunlunPalace() {}

    /** The paved terrace's reach (from the centre), the summit's, and the way in from the south edge (its half-width). */
    public static final int TERRACE = 44, SUMMIT = KunlunShape.TOP, APPROACH_HALF = 2;
    /** How far down its foundations (the pool's floor) go and how high it reaches (the palace roof's gold). */
    public static final int BOTTOM = -3, TOP = 29;

    /** The gate: its plane (local z), its pillars (x, either side), its beam's height. */
    public static final int GATE_Z = 41, GATE_INNER = 3, GATE_OUTER = 8, GATE_BEAM = 6;
    /** The Jade Pool's halves: their middles (x, either side) and z, their half-sizes; the causeway between them (its rails at this x, either side). */
    public static final double POOL_X = 11.5, POOL_Z = 27, POOL_HALF_X = 7.5, POOL_HALF_Z = 9.5;
    public static final int CAUSEWAY = 3;
    /** The Jade Terrace over the causeway: its middle (z), its pillars (x, z either side of it), its lintels' height and its eaves'. */
    public static final int POOL_HALL_Z = 27, POOL_HALL_POST = 3, POOL_HALL_LINTEL = 5, POOL_HALL_EAVES = 6;

    /** The upper terrace: how high, how far either side, its front (south) edge, no farther from the centre than this. */
    public static final int UP = 3, UPPER_X = 32, UPPER_S = 12;
    public static final double UPPER_REACH = 42.5;
    /** The broad steps up to it (their half-width), from the lower court. */
    public static final int STEPS_HALF = 5;

    /** The palace's plinth (|x| up to this, its north and south faces) and its floor's height; the steps up it (half-width). */
    public static final int PLINTH_X = 17, PLINTH_N = -38, PLINTH_S = -14, FLOOR = 6, PLINTH_STEPS = 4;
    /** The palace's walls (|x| up to this, north and south), their top, its ceiling. */
    public static final int HALL_X = 14, HALL_N = -35, HALL_S = -17, WALL_TOP = 15, CEILING = 16;
    /** The great roof over the walls (its eaves' box, its eaves' height); the lower roof round the colonnade below it runs from the plinth's edge up to the walls. */
    public static final int ROOF_X = 15, ROOF_N = -36, ROOF_S = -16, ROOF_EAVES = 18;
    /** The dais within (north and south), the throne on it, the screen behind; the chest before the throne. */
    public static final int DAIS_N = -33, DAIS_S = -28, THRONE_Z = -32, SCREEN_Z = -34;
    public static final int CHEST_X = 0, CHEST_Y = FLOOR + 2, CHEST_Z = -29;

    /** The bell and drum towers (their middles' x, either side; z 0): plinth, walls and roof half-sizes, wall top, roof's eaves. */
    public static final int TOWER_X = 20, TOWER_HALF = 5, TOWER_WALL = 12;
    /** The side halls of scriptures (their middles' x, either side; z from north to south): plinth half-width, wall top, roof's eaves. */
    public static final int SIDE_X = 25, SIDE_N = -29, SIDE_S = -13, SIDE_HALF = 5, SIDE_WALL = 10;

    /** The peach trees (x, z, the ground they stand on), and the pines round the summit's snow (at this reach, on these bearings, in degrees from east by south). */
    public static final int[][] PEACHES = {{25, 33, 0}, {-25, 33, 0}, {29, 23, 0}, {-29, 23, 0}, {26, 17, 0}, {-26, 17, 0},
            {10, 6, UP}, {-10, 6, UP}, {10, -6, UP}, {-10, -6, UP}};
    public static final int PINE_AT = 46;
    public static final int[] PINE_BEARINGS = {10, 40, 140, 165, 195, 225, 255, 285, 315, 345};

    // ── Small helpers ───────────────────────────────────────────────────

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** A hash of a place, 0-99. */
    static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState pillar() {
        return Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }


    /** A white rail post, joined to the rails beside it. */
    private static BlockState rail(int x, int y, int z) {
        BlockState w = s(Blocks.DIORITE_WALL);
        boolean n = isRail(x, y, z - 1), so = isRail(x, y, z + 1), e = isRail(x + 1, y, z), wst = isRail(x - 1, y, z);
        w = w.setValue(WallBlock.NORTH_WALL, n ? WallSide.LOW : WallSide.NONE).setValue(WallBlock.SOUTH_WALL, so ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.EAST_WALL, e ? WallSide.LOW : WallSide.NONE).setValue(WallBlock.WEST_WALL, wst ? WallSide.LOW : WallSide.NONE);
        boolean straight = (n && so && !e && !wst) || (e && wst && !n && !so);
        return w.setValue(WallBlock.UP, !straight || lampPost(x, y, z));
    }

    /**
     * A hip roof of blue tiles over the box x0..x1, z0..z1 (its eaves), its eaves at y {@code eaves}:
     * its tile at (x, y, z), its ridge (trimmed with gold at its ends and middle), or null where the
     * roof has nothing (under it, or over it, or outside its box). A square box makes a pyramid with
     * a gold finial.
     */
    @Nullable
    static BlockState hip(int x, int y, int z, int x0, int x1, int z0, int z1, int eaves) {
        if (x < x0 || x > x1 || z < z0 || z > z1) return null;
        int west = x - x0, east = x1 - x, north = z - z0, south = z1 - z;
        int d = Math.min(Math.min(west, east), Math.min(north, south));
        int ridge = Math.min((x1 - x0) / 2, (z1 - z0) / 2);
        if (d >= ridge) {
            if (y == eaves + ridge) return s(Blocks.PRISMARINE_BRICKS);
            boolean end = (x1 - x0 >= z1 - z0) ? (west == ridge || east == ridge || x == (x0 + x1) / 2) : (north == ridge || south == ridge || z == (z0 + z1) / 2);
            if (y == eaves + ridge + 1 && end) return s(Blocks.GOLD_BLOCK);
            return null;
        }
        if (y != eaves + d) return null;
        Direction toward = d == west ? Direction.EAST : d == east ? Direction.WEST : d == north ? Direction.SOUTH : Direction.NORTH;
        return stair(Blocks.DARK_PRISMARINE_STAIRS, toward);
    }

    /** The height of a hip roof's surface over (x, z) in its box (as {@link #hip}). */
    static int hipTop(int x, int z, int x0, int x1, int z0, int z1, int eaves) {
        int d = Math.min(Math.min(x - x0, x1 - x), Math.min(z - z0, z1 - z));
        return eaves + Math.min(d, Math.min((x1 - x0) / 2, (z1 - z0) / 2));
    }

    // ── Where things are ────────────────────────────────────────────────

    private static double reach(int x, int z) {
        return Math.sqrt((double) x * x + (double) z * z);
    }

    /** Whether local (x, z) is the way in from the summit's south edge to the terrace. */
    public static boolean onApproach(int x, int z) {
        return Math.abs(x) <= APPROACH_HALF && z > 0 && reach(x, z) > TERRACE - 0.5 && z <= SUMMIT + 1;
    }

    /** Whether the blueprint has anything to say of local (x, z): the summit, and the way in. */
    public static boolean covers(int x, int z) {
        return reach(x, z) <= SUMMIT + 0.5 || onApproach(x, z);
    }

    /** Whether local (x, z) is on the paved terrace. */
    public static boolean onTerrace(int x, int z) {
        return reach(x, z) <= TERRACE + 0.5;
    }

    /** Whether local (x, z) is on the upper terrace (its retaining walls and all). */
    public static boolean inUpper(int x, int z) {
        return Math.abs(x) <= UPPER_X && z <= UPPER_S && reach(x, z) <= UPPER_REACH;
    }

    /** Whether local (x, z) is in the Jade Pool (either half). */
    public static boolean inPool(int x, int z) {
        double u = (Math.abs(x) - POOL_X) / POOL_HALF_X, v = (z - POOL_Z) / POOL_HALF_Z;
        return Math.abs(x) > CAUSEWAY && u * u * u * u + v * v * v * v <= 1;
    }

    /** Whether local (x, z) is on the pool's jade rim. */
    private static boolean onRim(int x, int z) {
        if (inPool(x, z) || Math.abs(x) <= CAUSEWAY) return false;
        double u = (Math.abs(x) - POOL_X) / (POOL_HALF_X + 1.2), v = (z - POOL_Z) / (POOL_HALF_Z + 1.2);
        return u * u * u * u + v * v * v * v <= 1;
    }

    /** Whether z is along the pool, where the causeway has rails. */
    private static boolean alongPool(int z) {
        return Math.abs(z - POOL_Z) <= POOL_HALF_Z - 0.5;
    }

    /** Whether local (x, z) is one of the Jade Terrace's pillars. */
    private static boolean poolHallPost(int x, int z) {
        return Math.abs(x) == POOL_HALL_POST && Math.abs(z - POOL_HALL_Z) == POOL_HALL_POST;
    }

    /** Whether local (x, z) is under the Jade Terrace's roof. */
    private static boolean underPoolHall(int x, int z) {
        return Math.abs(x) <= POOL_HALL_POST + 1 && Math.abs(z - POOL_HALL_Z) <= POOL_HALL_POST + 1;
    }

    /** Whether local (x, y, z) is a rail of a balustrade: round the terrace's edge (but at the way in and the gate), along the causeway, round the upper terrace (but at its steps), and round the palace's plinth (but at its steps). */
    static boolean isRail(int x, int y, int z) {
        if (y == 1) {
            double r = reach(x, z);
            if (r > TERRACE - 0.5 && r <= TERRACE + 0.5) return !(z > 0 && Math.abs(x) <= GATE_OUTER + 2);
            return Math.abs(x) == CAUSEWAY && alongPool(z) && !poolHallPost(x, z);
        }
        if (y == UP + 1) {
            if (!inUpper(x, z) || inPlinth(x, z) || inTower(x, z) || inSideHall(x, z)) return false;
            if (z == UPPER_S && Math.abs(x) <= STEPS_HALF) return false;
            if (inSideSteps(x, z) || inTowerSteps(x, z)) return false;
            return !inUpper(x + 1, z) || !inUpper(x - 1, z) || !inUpper(x, z + 1) || !inUpper(x, z - 1);
        }
        if (y == FLOOR + 1) {
            if (!inPlinth(x, z)) return false;
            if (z == PLINTH_S && Math.abs(x) <= PLINTH_STEPS) return false;
            return Math.abs(x) == PLINTH_X || z == PLINTH_N || z == PLINTH_S;
        }
        return false;
    }

    /** The rails' lamp posts: at the causeway's ends. */
    private static boolean lampPost(int x, int y, int z) {
        return y == 1 && Math.abs(x) == CAUSEWAY && (z == POOL_ENDS[0] || z == POOL_ENDS[1]);
    }

    /** The causeway's first and last z with rails. */
    private static final int[] POOL_ENDS = poolEnds();

    private static int[] poolEnds() {
        int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE;
        for (int k = 0; k <= 60; k++) {
            if (alongPool(k)) {
                first = Math.min(first, k);
                last = Math.max(last, k);
            }
        }
        return new int[]{first, last};
    }

    /** Whether local (x, z) is on the broad steps up to the upper terrace. */
    public static boolean onSteps(int x, int z) {
        return Math.abs(x) <= STEPS_HALF && z > UPPER_S && z <= UPPER_S + 3;
    }

    /** Whether local (x, z) is on the palace's plinth. */
    public static boolean inPlinth(int x, int z) {
        return Math.abs(x) <= PLINTH_X && z >= PLINTH_N && z <= PLINTH_S;
    }

    /** Whether local (x, z) is on the steps up the palace's plinth. */
    public static boolean onPlinthSteps(int x, int z) {
        return Math.abs(x) <= PLINTH_STEPS && z > PLINTH_S && z <= PLINTH_S + 3;
    }

    /** Whether local (x, z) is on a tower's plinth. */
    private static boolean inTower(int x, int z) {
        return Math.abs(Math.abs(x) - TOWER_X) <= TOWER_HALF && Math.abs(z) <= TOWER_HALF;
    }

    private static boolean inTowerSteps(int x, int z) {
        return Math.abs(x) == TOWER_X - TOWER_HALF - 1 && Math.abs(z) <= 1;
    }

    /** Whether local (x, z) is on a side hall's plinth. */
    private static boolean inSideHall(int x, int z) {
        return Math.abs(Math.abs(x) - SIDE_X) <= SIDE_HALF && z >= SIDE_N && z <= SIDE_S;
    }

    private static boolean inSideSteps(int x, int z) {
        return Math.abs(x) == SIDE_X - SIDE_HALF - 1 && Math.abs(z - (SIDE_N + SIDE_S) / 2) <= 1;
    }

    /** Where the pines stand, as {x, z}. */
    public static int[][] pines() {
        int[][] out = new int[PINE_BEARINGS.length][];
        for (int i = 0; i < PINE_BEARINGS.length; i++) {
            double a = Math.toRadians(PINE_BEARINGS[i]);
            out[i] = new int[]{(int) Math.round(Math.cos(a) * PINE_AT), (int) Math.round(Math.sin(a) * PINE_AT)};
        }
        return out;
    }

    private static final int[][] PINES = pines();

    // ── Signs ───────────────────────────────────────────────────────────

    /** A sign on a wall: its place (local), the way it faces, its lines. */
    public record Sign(int x, int y, int z, Direction facing, String... lines) {}

    /** The gate's names, front and back, the Jade Terrace's, and the palace's over its door. */
    public static final List<Sign> SIGNS = List.of(
            new Sign(0, GATE_BEAM, GATE_Z + 1, Direction.SOUTH, "昆仑", "Mount Kunlun", "", "The Jade Pool"),
            new Sign(0, GATE_BEAM, GATE_Z - 1, Direction.NORTH, "瑶池", "The Jade Pool", "of the Queen", "Mother"),
            new Sign(0, POOL_HALL_LINTEL, POOL_HALL_Z + POOL_HALL_POST + 1, Direction.SOUTH, "瑶台", "The Jade", "Terrace", ""),
            new Sign(0, FLOOR + 6, HALL_S + 1, Direction.SOUTH, "西王母宫", "Palace of the", "Queen Mother", "of the West"));

    /** Puts {@code sign} at {@code pos} (the world's). */
    public static void putSign(WorldGenLevel level, BlockPos pos, Sign sign) {
        new Site(level, pos.getX(), pos.getZ(), pos.getX(), pos.getZ(), 0, 0, 0).sign(pos.getX(), pos.getY(), pos.getZ(),
                Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, sign.facing()), sign.lines());
    }

    // ── The blueprint ───────────────────────────────────────────────────

    /** What stands at local (x, y, z): a block, air where it is open, or null where the ground is left as the mountain has it. */
    @Nullable
    public static BlockState at(int x, int y, int z) {
        if (!covers(x, z) || y < BOTTOM || y > TOP) return null;
        BlockState tree = trees(x, y, z);
        if (tree != null) return tree;
        if (!onTerrace(x, z)) {
            if (onApproach(x, z)) return y == 0 ? paving(x, z, true) : y > 0 ? s(Blocks.AIR) : null;
            return y > 0 ? s(Blocks.AIR) : null; // the summit's snow, round the terrace
        }
        if (inPlinth(x, z) || onPlinthSteps(x, z)) return palace(x, y, z);
        if (inTower(x, z) || inTowerSteps(x, z)) return tower(x, y, z);
        if (inSideHall(x, z) || inSideSteps(x, z)) return sideHall(x, y, z);
        if (inUpper(x, z)) return upper(x, y, z);
        if (onSteps(x, z)) return steps(x, y, z);
        // the lower court
        if (isRail(x, y, z)) return rail(x, y, z);
        if (lampPost(x, 1, z) && y == 2) return s(Blocks.LANTERN);
        if (Math.abs(z - GATE_Z) <= 1 && Math.abs(x) <= GATE_OUTER + 1) {
            BlockState g = gate(x, y, z);
            if (g != null) return g;
        }
        if (underPoolHall(x, z) && y >= 1) {
            BlockState h = poolHall(x, y, z);
            if (h != null) return h;
        }
        if (inPool(x, z)) return pool(x, y, z);
        if (y < 0) return y == -1 ? s(Blocks.SMOOTH_QUARTZ) : null;
        if (y == 0) return onRim(x, z) ? s(ModBlocks.JADE_BLOCK.get()) : paving(x, z, false);
        // lanterns on posts either side of the broad steps
        if (Math.abs(x) == STEPS_HALF + 2 && z == UPPER_S + 4) {
            if (y == 1) return s(Blocks.CHISELED_QUARTZ_BLOCK);
            if (y == 2) return s(Blocks.LANTERN);
        }
        return s(Blocks.AIR);
    }

    /** The paving: white jade, a walk of smooth quartz up the middle, bands of quartz brick. */
    private static BlockState paving(int x, int z, boolean approach) {
        if (Math.abs(x) <= 2) return s(Blocks.SMOOTH_QUARTZ);
        if (approach) return s(Blocks.QUARTZ_BRICKS);
        if (Math.floorMod(x, 7) == 0 || Math.floorMod(z, 7) == 0) return s(Blocks.QUARTZ_BRICKS);
        return hash(x, z, 3) < 85 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.CALCITE);
    }

    /** The gate at the terrace's south edge: three bays between four white pillars, a gold-trimmed beam, a roof of blue tiles. */
    @Nullable
    private static BlockState gate(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean post = z == GATE_Z && (ax == GATE_INNER || ax == GATE_OUTER);
        if (y <= 0) return null;
        if (post && y < GATE_BEAM) return y == 1 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
        if (z == GATE_Z) {
            if (y == GATE_BEAM && ax <= GATE_OUTER) {
                return ax < GATE_INNER ? s(Blocks.CHISELED_QUARTZ_BLOCK) : ax == GATE_INNER || ax == GATE_OUTER ? s(Blocks.GOLD_BLOCK) : s(Blocks.SMOOTH_QUARTZ);
            }
            if (y == GATE_BEAM + 1 && ax <= GATE_OUTER + 1) return s(Blocks.SMOOTH_QUARTZ);
            if (y == GATE_BEAM + 2 && ax <= GATE_OUTER + 1) return s(Blocks.PRISMARINE_BRICKS);
            if (y == GATE_BEAM + 3 && (ax == 0 || ax == GATE_OUTER + 1)) return s(Blocks.GOLD_BLOCK);
            if (y == GATE_BEAM - 1 && (ax == (GATE_INNER + GATE_OUTER) / 2 + 1)) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            return null;
        }
        if (y == GATE_BEAM + 2 && ax <= GATE_OUTER + 1) return stair(Blocks.DARK_PRISMARINE_STAIRS, z < GATE_Z ? Direction.SOUTH : Direction.NORTH);
        return null;
    }

    /** The Jade Pool: two deep of water over a floor of prismarine and sea lanterns, lilies on it. */
    private static BlockState pool(int x, int y, int z) {
        if (y == 0 || y == -1) return s(Blocks.WATER);
        if (y == -2) return Math.floorMod(x + 2 * z, 4) == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.PRISMARINE_BRICKS);
        if (y == -3) return s(Blocks.SMOOTH_QUARTZ);
        if (y == 1 && hash(x, z, 17) < 6) return s(Blocks.LILY_PAD);
        return s(Blocks.AIR);
    }

    /** The Jade Terrace over the causeway's middle: four white pillars, lintels, a pyramid roof of blue tiles, lanterns at its corners. */
    @Nullable
    private static BlockState poolHall(int x, int y, int z) {
        int lx = x, lz = z - POOL_HALL_Z, ax = Math.abs(lx), az = Math.abs(lz), h = POOL_HALL_POST + 1;
        BlockState roof = hip(lx, y, lz, -h, h, -h, h, POOL_HALL_EAVES);
        if (roof != null) return roof;
        if (y >= POOL_HALL_EAVES) return null;
        if (poolHallPost(x, z) && y < POOL_HALL_LINTEL) return y == 1 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
        if (Math.max(ax, az) == POOL_HALL_POST && y == POOL_HALL_LINTEL) return s(Blocks.SMOOTH_QUARTZ);
        if (ax == h && az == h && y == POOL_HALL_EAVES - 1) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
        return null;
    }

    /** The broad steps from the lower court to the upper terrace, three up. */
    private static BlockState steps(int x, int y, int z) {
        int k = UPPER_S + 3 - z; // 0 the lowest step's row, 2 the highest
        if (y < 0) return y == -1 ? s(Blocks.SMOOTH_QUARTZ) : null;
        if (y == 0) return s(Blocks.QUARTZ_BRICKS);
        if (y <= k) return s(Blocks.QUARTZ_BRICKS);
        if (y == k + 1) return stair(Blocks.QUARTZ_STAIRS, Direction.NORTH);
        return s(Blocks.AIR);
    }

    /** The upper terrace: its walls and fill, its paving, its rails and lamps, open over it. */
    private static BlockState upper(int x, int y, int z) {
        if (y < 0) return y == -1 ? s(Blocks.SMOOTH_QUARTZ) : null;
        if (y < UP) return s(Blocks.QUARTZ_BRICKS);
        if (y == UP) {
            boolean edge = !inUpper(x + 1, z) || !inUpper(x - 1, z) || !inUpper(x, z + 1) || !inUpper(x, z - 1);
            return edge ? s(Blocks.QUARTZ_BRICKS) : paving(x, z, false);
        }
        if (isRail(x, y, z)) return rail(x, y, z);
        // lanterns on posts before the palace's steps
        if (Math.abs(x) == PLINTH_STEPS + 2 && z == PLINTH_S + 4) {
            if (y == UP + 1) return s(Blocks.CHISELED_QUARTZ_BLOCK);
            if (y == UP + 2) return s(Blocks.LANTERN);
        }
        return s(Blocks.AIR);
    }

    /** The palace, its plinth and its steps, its colonnade and roofs. */
    @Nullable
    private static BlockState palace(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean plinth = inPlinth(x, z);
        if (y <= UP) return y < 0 ? (y == -1 ? s(Blocks.SMOOTH_QUARTZ) : null) : plinth || y < UP ? s(Blocks.QUARTZ_BRICKS) : upper(x, y, z);
        if (!plinth) { // the steps up to it, three up
            int k = PLINTH_S + 3 - z; // 0 the lowest row (south), 2 the highest
            int ly = y - UP;
            if (ly <= k) return s(Blocks.QUARTZ_BRICKS);
            if (ly == k + 1) return stair(Blocks.QUARTZ_STAIRS, Direction.NORTH);
            return s(Blocks.AIR);
        }
        if (y <= FLOOR) {
            if (y == FLOOR && ax < HALL_X && z > HALL_N && z < HALL_S) return ax <= 1 || Math.floorMod(x + z, 2) == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_DIORITE);
            return y == FLOOR ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS);
        }
        // the great roof, and the attic under it within the walls
        BlockState roof = hip(x, y, z, -ROOF_X, ROOF_X, ROOF_N, ROOF_S, ROOF_EAVES);
        if (roof != null) return roof;
        boolean walls = ax <= HALL_X && z >= HALL_N && z <= HALL_S;
        boolean underRoof = ax <= ROOF_X && z >= ROOF_N && z <= ROOF_S;
        if (underRoof && y > hipTop(x, z, -ROOF_X, ROOF_X, ROOF_N, ROOF_S, ROOF_EAVES)) return s(Blocks.AIR);
        if (walls && y >= CEILING) return s(Blocks.SMOOTH_QUARTZ);
        // the lower roof round the colonnade, from the plinth's edge up to the walls
        int d = Math.min(PLINTH_X - ax, Math.min(z - PLINTH_N, PLINTH_S - z)); // in from the plinth's edge
        if (!walls) {
            if (d <= 2 && y == WALL_TOP + d) {
                Direction toward = d == PLINTH_X - ax ? (x > 0 ? Direction.WEST : Direction.EAST) : d == z - PLINTH_N ? Direction.SOUTH : Direction.NORTH;
                return stair(Blocks.DARK_PRISMARINE_STAIRS, toward);
            }
            if (y > WALL_TOP + d) return y < ROOF_EAVES || !underRoof ? s(Blocks.AIR) : null;
            if (isRail(x, y, z)) return rail(x, y, z);
            if (d == 1 && colonnade(x, z) && y <= WALL_TOP) return y == WALL_TOP ? s(Blocks.GOLD_BLOCK) : y == FLOOR + 1 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
            return s(Blocks.AIR);
        }
        boolean inside = ax < HALL_X && z > HALL_N && z < HALL_S;
        if (!inside) return hallWall(x, y, z);
        return interior(x, y, z);
    }

    /** The colonnade's pillars round the walls, under the lower roof (none before the door). */
    private static boolean colonnade(int x, int z) {
        int ax = Math.abs(x);
        if (ax == PLINTH_X - 1) return Math.floorMod(z - PLINTH_S, 4) == 1 || z == PLINTH_N + 1;
        return ax % 4 == 0 && ax > PLINTH_STEPS - 1 || ax == PLINTH_X - 1;
    }

    private static boolean isPillarAt(int x, int z) {
        int ax = Math.abs(x);
        if (z == HALL_S || z == HALL_N) return ax == HALL_X || ax == 3 || ax == 8;
        return ax == HALL_X && (z == -22 || z == -26 || z == -30);
    }

    /** The palace's walls: white, pillars with gold heads, windows of blue glass; its door in the middle of its front. */
    private static BlockState hallWall(int x, int y, int z) {
        int ax = Math.abs(x);
        if (isPillarAt(x, z)) return y == WALL_TOP ? s(Blocks.GOLD_BLOCK) : pillar();
        if (y == WALL_TOP) return s(Blocks.SMOOTH_QUARTZ);
        if (z == HALL_S && ax <= 2) return y <= FLOOR + 4 ? s(Blocks.AIR) : y == FLOOR + 5 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.QUARTZ_BRICKS);
        if (y == FLOOR + 1) return s(Blocks.QUARTZ_BRICKS);
        boolean window = y >= FLOOR + 3 && y <= FLOOR + 7 && !(z == HALL_S && ax <= 3);
        return window ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.QUARTZ_BRICKS);
    }

    /** Within: white pillars, a blue carpet to the dais, the throne and its screen of jade and gold, the chest before it, candles, lanterns. */
    private static BlockState interior(int x, int y, int z) {
        int ax = Math.abs(x), ly = y - FLOOR;
        if (ax == 7 && (z == -21 || z == -26 || z == -31)) return y == WALL_TOP ? s(Blocks.GOLD_BLOCK) : ly == 1 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
        if (z == SCREEN_Z && ax <= 5 && ly >= 1 && ly <= 7) return ly >= 3 && ly <= 5 && ax <= 1 ? s(Blocks.GOLD_BLOCK) : s(ModBlocks.JADE_BLOCK.get());
        if (z >= DAIS_N && z <= DAIS_S && ax <= 4) {
            if (ly == 1) return z == DAIS_S ? stair(Blocks.QUARTZ_STAIRS, Direction.NORTH) : s(Blocks.SMOOTH_QUARTZ);
            if (ly == 2) {
                if (x == CHEST_X && z == CHEST_Z) return s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH);
                if (ax == 2 && z == CHEST_Z) return s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true);
                if (x == 0 && z == THRONE_Z) return stair(Blocks.QUARTZ_STAIRS, Direction.NORTH);
                if (ax == 1 && z == THRONE_Z) return s(Blocks.GOLD_BLOCK);
                if (ax == 3 && z == THRONE_Z) return s(Blocks.POTTED_CHERRY_SAPLING);
            }
            if (ly == 3 && x == 0 && z == THRONE_Z - 1) return s(Blocks.GOLD_BLOCK);
        }
        if (ly == 1 && ax <= 1 && z > DAIS_S && z < HALL_S) return s(Blocks.LIGHT_BLUE_CARPET);
        if (y == CEILING - 1 && ((ax == 10 && (z == -21 || z == -31)) || (x == 0 && z == -22))) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
        if (ly == 1 && ax == HALL_X - 1 && (z == HALL_N + 1 || z == HALL_S - 1)) return s(Blocks.POTTED_CHERRY_SAPLING);
        return s(Blocks.AIR);
    }

    /** The bell tower (east) and the drum tower (west): a plinth one up, white walls with windows, a pyramid roof of blue tiles; the bell or the drum within. */
    private static BlockState tower(int x, int y, int z) {
        int sx = x > 0 ? 1 : -1, lx = x - sx * TOWER_X, ax = Math.abs(lx), az = Math.abs(z);
        if (y <= UP) return upper(x, y, z);
        if (inTowerSteps(x, z)) return y == UP + 1 ? stair(Blocks.QUARTZ_STAIRS, sx > 0 ? Direction.EAST : Direction.WEST) : s(Blocks.AIR);
        if (y == UP + 1) return ax <= TOWER_HALF - 1 && az <= TOWER_HALF - 1 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS);
        int h = TOWER_HALF;
        BlockState roof = hip(lx, y, z, -h, h, -h, h, TOWER_WALL);
        if (roof != null) return roof;
        int wall = h - 1;
        if (y > hipTop(lx, z, -h, h, -h, h, TOWER_WALL)) return s(Blocks.AIR);
        if (ax > wall || az > wall) return s(Blocks.AIR); // round the walls, on the plinth
        if (ax == wall && az == wall) return y == TOWER_WALL ? s(Blocks.GOLD_BLOCK) : pillar();
        if (y >= TOWER_WALL) return s(Blocks.SMOOTH_QUARTZ);
        boolean wallCell = ax == wall || az == wall;
        if (wallCell) {
            boolean door = lx * sx == -wall && az <= 1 && y <= UP + 4;
            if (door) return s(Blocks.AIR);
            if (y >= UP + 5 && y <= UP + 6 && (ax <= 1 || az <= 1)) return s(Blocks.LIGHT_BLUE_STAINED_GLASS);
            return s(Blocks.QUARTZ_BRICKS);
        }
        // within: the bell hung from the roof (east), the drum on its stand (west)
        if (sx > 0 && lx == 0 && z == 0 && y == TOWER_WALL - 1) return s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH);
        if (sx < 0 && ax <= 1 && az <= 1 && (y == UP + 2 || y == UP + 3)) return y == UP + 2 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.NOTE_BLOCK);
        if (ax == wall - 1 && az == wall - 1 && y == UP + 2) return s(Blocks.LANTERN);
        return s(Blocks.AIR);
    }

    /** The side halls of scriptures: a plinth one up, white walls with windows, a hip roof of blue tiles; shelves of scrolls within. */
    private static BlockState sideHall(int x, int y, int z) {
        int sx = x > 0 ? 1 : -1, lx = x - sx * SIDE_X, ax = Math.abs(lx);
        if (y <= UP) return upper(x, y, z);
        if (inSideSteps(x, z)) return y == UP + 1 ? stair(Blocks.QUARTZ_STAIRS, sx > 0 ? Direction.EAST : Direction.WEST) : s(Blocks.AIR);
        if (y == UP + 1) return s(Blocks.QUARTZ_BRICKS);
        BlockState roof = hip(lx, y, z, -SIDE_HALF, SIDE_HALF, SIDE_N, SIDE_S, SIDE_WALL);
        if (roof != null) return roof;
        if (y > hipTop(lx, z, -SIDE_HALF, SIDE_HALF, SIDE_N, SIDE_S, SIDE_WALL)) return s(Blocks.AIR);
        int wall = SIDE_HALF - 1, n = SIDE_N + 1, so = SIDE_S - 1;
        boolean walls = ax <= wall && z >= n && z <= so;
        if (!walls) return s(Blocks.AIR);
        if (y >= SIDE_WALL) return s(Blocks.SMOOTH_QUARTZ);
        boolean inside = ax < wall && z > n && z < so;
        if (!inside) {
            boolean corner = ax == wall && (z == n || z == so || Math.floorMod(z - n, 4) == 0);
            if (corner) return y == SIDE_WALL - 1 ? s(Blocks.GOLD_BLOCK) : pillar();
            int mid = (SIDE_N + SIDE_S) / 2;
            if (lx * sx == -wall && Math.abs(z - mid) <= 1 && y <= UP + 4) return s(Blocks.AIR); // the door, toward the court
            if (y >= UP + 3 && y <= UP + 4) return s(Blocks.LIGHT_BLUE_STAINED_GLASS);
            return s(Blocks.QUARTZ_BRICKS);
        }
        // within: shelves of scrolls along the outer wall, a lantern
        if (lx * sx == wall - 1 && y <= UP + 4) return s(Blocks.BOOKSHELF);
        if (lx == 0 && z == (SIDE_N + SIDE_S) / 2 && y == SIDE_WALL - 1) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
        if (lx * sx == wall - 2 && Math.floorMod(z, 3) == 0 && y == UP + 2) return s(Blocks.LECTERN).setValue(net.minecraft.world.level.block.LecternBlock.FACING, sx > 0 ? Direction.WEST : Direction.EAST);
        return s(Blocks.AIR);
    }

    /** The peach trees of immortality in blossom (each on a bed of grass, petals fallen round it), and the pines in the snow round the terrace. */
    @Nullable
    private static BlockState trees(int x, int y, int z) {
        for (int[] t : PEACHES) {
            int dx = x - t[0], dz = z - t[1], ly = y - t[2];
            if (Math.abs(dx) > 3 || Math.abs(dz) > 3) continue;
            if (ly == 0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return s(Blocks.GRASS_BLOCK);
            if (dx == 0 && dz == 0 && ly >= 1 && ly <= 4) return s(Blocks.CHERRY_LOG);
            if (ly == 1 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                return s(Blocks.PINK_PETALS).setValue(PinkPetalsBlock.AMOUNT, 1 + hash(x, z, 5) % 4)
                        .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue(hash(x, z, 6) % 4));
            }
            double r2 = dx * dx + dz * dz + (ly - 5.5) * (ly - 5.5) * 1.6;
            if (ly >= 4 && ly <= 7 && r2 <= 8.5 && hash(x * 7 + ly, z, 9) < 92) return leaves(Blocks.CHERRY_LEAVES);
        }
        for (int[] t : PINES) {
            int dx = x - t[0], dz = z - t[1];
            if (Math.abs(dx) > 2 || Math.abs(dz) > 2 || y < 1) continue;
            if (dx == 0 && dz == 0 && y <= 7) return s(Blocks.SPRUCE_LOG);
            int m = Math.abs(dx) + Math.abs(dz);
            int width = switch (y) {
                case 3, 5 -> 3;
                case 4, 6, 7 -> 1;
                case 8 -> 0;
                default -> -1;
            };
            if (m <= width && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)) return leaves(Blocks.SPRUCE_LEAVES);
            if (y == 8 && m == 0) return leaves(Blocks.SPRUCE_LEAVES);
        }
        return null;
    }

    /** Lays the whole summit round {@code ground} (its paving's level at the centre) at once, its signs and its treasure too: the gallery's, and tests'. */
    public static void layWhole(WorldGenLevel level, BlockPos ground) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -SUMMIT - 1; x <= SUMMIT + 1; x++) for (int z = -SUMMIT - 1; z <= SUMMIT + 2; z++) for (int y = BOTTOM; y <= TOP; y++) {
            BlockState s = at(x, y, z);
            if (s != null) level.setBlock(p.set(ground.getX() + x, ground.getY() + y, ground.getZ() + z), s, Block.UPDATE_CLIENTS);
        }
        for (Sign sign : SIGNS) putSign(level, ground.offset(sign.x(), sign.y(), sign.z()), sign);
        net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, level.getRandom(), ground.offset(CHEST_X, CHEST_Y, CHEST_Z), Kunlun.TREASURE);
    }
}
