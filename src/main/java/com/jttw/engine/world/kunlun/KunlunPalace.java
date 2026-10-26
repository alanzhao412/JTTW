package com.jttw.engine.world.kunlun;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.hell.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
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
import net.minecraft.world.level.block.state.properties.WallSide;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Queen Mother of the West's palace on the summit of Mount Kunlun, by the Jade Pool (瑶池): a
 * blueprint, as Fengdu is, what stands at each local place from the place alone, so each chunk of
 * the summit lays its own share ({@code KunlunFeature}) and it comes out whole whichever is made first.
 * <ul>
 *   <li><b>the terrace</b>: the summit paved in white jade (quartz and diorite) {@value #TERRACE}
 *       blocks out, a white balustrade round it, open to the south where the stair comes up; round
 *       it the summit's snow, and snow-laden pines;</li>
 *   <li><b>the gate</b> (a 牌坊, three bays) at its south edge, white pillars, a beam trimmed with
 *       gold under a roof of celestial blue tiles, 昆仑 on its face and 瑶池 on its back;</li>
 *   <li><b>the Jade Pool</b>: its two halves either side of a white causeway with rails, rimmed with
 *       jade, lit from below by sea lanterns (so it never freezes), lilies on it; two peach trees in
 *       blossom either side of it, and two more by the palace;</li>
 *   <li><b>the palace</b> (西王母宫) on a white plinth up two steps: white walls and pillars, windows
 *       of blue glass, gold at the pillars' heads, a hip roof of blue tiles with a gold-ended ridge;
 *       within, lanterns, a blue carpet to the altar, a screen of jade and gold, and before it on a
 *       dais between candles the treasure chest ({@link #CHEST_X}, {@link #CHEST_Y}, {@link #CHEST_Z}),
 *       where the Banana-Leaf Fan lies ({@link Kunlun#TREASURE}).</li>
 * </ul>
 * Nothing lives or fights here. Local y 0 is the summit's ground (the paving), (0, 0) the region's
 * centre, the gate toward +z (south).
 */
public final class KunlunPalace {

    private KunlunPalace() {}

    /** The paved terrace's reach (from the centre), the summit's, and the way in from the south edge (its half-width). */
    public static final int TERRACE = 21, SUMMIT = KunlunShape.TOP, APPROACH_HALF = 2;
    /** How far down its foundations (the pool's floor) go and how high it reaches. */
    public static final int BOTTOM = -3, TOP = 16;
    /** The gate: its plane (local z), its pillars (x, either side), its beam's height. */
    public static final int GATE_Z = 19, GATE_INNER = 2, GATE_OUTER = 6, GATE_BEAM = 5;
    /** The Jade Pool's halves: their middles (x, either side) and z, their half-sizes; the causeway between them. */
    public static final double POOL_X = 6.5, POOL_Z = 9.5, POOL_HALF_X = 4.6, POOL_HALF_Z = 6.6;
    public static final int CAUSEWAY = 2;
    /** The plinth (|x| up to this, its north and south faces), its height; the steps up it, either side of the middle. */
    public static final int PLINTH_X = 11, PLINTH_N = -17, PLINTH_S = -3, PLINTH_H = 2, STEPS_HALF = 3;
    /** The palace's walls (|x| up to this, north and south), their top; its ceiling. */
    public static final int HALL_X = 8, HALL_N = -15, HALL_S = -5, WALL_TOP = 7, CEILING = 8;
    /** The roof's eaves (|x| up to this, north and south). */
    public static final int EAVES_X = 10, EAVES_N = -17, EAVES_S = -3;
    /** The altar's screen (its plane), the dais before it; the chest on the dais. */
    public static final int SCREEN_Z = -14, DAIS_N = -13, DAIS_S = -12;
    public static final int CHEST_X = 0, CHEST_Y = PLINTH_H + 2, CHEST_Z = DAIS_S;
    /** The peach trees (x either side, z), and the pines round the summit's snow (at this reach, on these bearings, in degrees from east by south). */
    public static final int[][] PEACHES = {{15, 9}, {-15, 9}, {15, -1}, {-15, -1}};
    public static final int PINE_AT = 23;
    public static final int[] PINE_BEARINGS = {10, 45, 140, 170, 205, 240, 275, 305, 335};

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
        return w.setValue(WallBlock.UP, !straight || lampPost(x, z));
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

    /** Whether local (x, z) is in the Jade Pool (either half). */
    public static boolean inPool(int x, int z) {
        double u = (Math.abs(x) - POOL_X) / POOL_HALF_X, v = (z - POOL_Z) / POOL_HALF_Z;
        return Math.abs(x) > CAUSEWAY && u * u * u * u + v * v * v * v <= 1;
    }

    /** Whether local (x, z) is on the pool's jade rim. */
    private static boolean onRim(int x, int z) {
        if (inPool(x, z) || Math.abs(x) <= CAUSEWAY - 1) return false;
        double u = (Math.abs(x) - POOL_X) / (POOL_HALF_X + 1.2), v = (z - POOL_Z) / (POOL_HALF_Z + 1.2);
        return u * u * u * u + v * v * v * v <= 1;
    }

    /** Whether local (x, y, z) is a rail of the balustrade: round the terrace's edge (but at the way in, and the gate), and along the causeway over the pool. */
    static boolean isRail(int x, int y, int z) {
        if (y != 1) return false;
        double r = reach(x, z);
        if (r > TERRACE - 0.5 && r <= TERRACE + 0.5) return !(z > 0 && Math.abs(x) <= GATE_OUTER + 1) && !inPlinth(x, z);
        return Math.abs(x) == CAUSEWAY && (inPool(x + 1, z) || inPool(x - 1, z));
    }

    /** The rails' lamp posts: at the causeway's ends, and the terrace's balustrade where it turns from the gate. */
    private static boolean lampPost(int x, int z) {
        return Math.abs(x) == CAUSEWAY && (z == POOL_ENDS[0] || z == POOL_ENDS[1]);
    }

    /** The pool's first and last z beside the causeway. */
    private static final int[] POOL_ENDS = poolEnds();

    private static int[] poolEnds() {
        int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE;
        for (int k = 0; k <= 20; k++) {
            if (inPool(CAUSEWAY + 1, k)) {
                first = Math.min(first, k);
                last = Math.max(last, k);
            }
        }
        return new int[]{first, last};
    }

    /** Whether local (x, z) is on the steps up to the plinth. */
    public static boolean onSteps(int x, int z) {
        return Math.abs(x) <= STEPS_HALF && z > PLINTH_S && z <= PLINTH_S + 2;
    }

    private static boolean inPlinth(int x, int z) {
        return Math.abs(x) <= PLINTH_X && z >= PLINTH_N && z <= PLINTH_S;
    }

    /** Whether local (x, z) is under the palace's roof. */
    private static boolean underRoof(int x, int z) {
        return Math.abs(x) <= EAVES_X && z >= EAVES_N && z <= EAVES_S;
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

    /** The gate's names, front and back, and the palace's over its door. */
    public static final List<Sign> SIGNS = List.of(
            new Sign(0, GATE_BEAM, GATE_Z + 1, Direction.SOUTH, "昆仑", "Mount Kunlun", "", "The Jade Pool"),
            new Sign(0, GATE_BEAM, GATE_Z - 1, Direction.NORTH, "瑶池", "The Jade Pool", "of the Queen", "Mother"),
            new Sign(0, WALL_TOP, HALL_S + 1, Direction.SOUTH, "西王母宫", "Palace of the", "Queen Mother", "of the West"));

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
        if (isRail(x, y, z)) return rail(x, y, z);
        if (lampPost(x, z) && isRail(x, 1, z) && y == 2) return s(Blocks.LANTERN);
        if (Math.abs(z - GATE_Z) <= 1 && Math.abs(x) <= GATE_OUTER + 1) {
            BlockState g = gate(x, y, z);
            if (g != null) return g;
        }
        if (inPool(x, z)) return pool(x, y, z);
        if (underRoof(x, z) || inPlinth(x, z) || onSteps(x, z)) {
            BlockState p = palace(x, y, z);
            if (p != null) return p;
        }
        if (y < 0) return y == -1 ? s(Blocks.SMOOTH_QUARTZ) : null;
        if (y == 0) return onRim(x, z) ? s(ModBlocks.JADE_BLOCK.get()) : paving(x, z, false);
        // lanterns on posts before the palace's steps
        if (Math.abs(x) == STEPS_HALF + 2 && z == PLINTH_S + 3) {
            if (y == 1) return s(Blocks.CHISELED_QUARTZ_BLOCK);
            if (y == 2) return s(Blocks.LANTERN);
        }
        return s(Blocks.AIR);
    }

    /** The terrace's paving: white jade, a walk of smooth quartz up the middle, bands of quartz brick. */
    private static BlockState paving(int x, int z, boolean approach) {
        if (Math.abs(x) <= 1) return s(Blocks.SMOOTH_QUARTZ);
        if (approach) return s(Blocks.QUARTZ_BRICKS);
        if (Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0) return s(Blocks.QUARTZ_BRICKS);
        return hash(x, z, 3) < 85 ? s(Blocks.POLISHED_DIORITE) : s(Blocks.CALCITE);
    }

    /** The gate at the terrace's south edge: three bays between four white pillars, a gold-trimmed beam, a roof of blue tiles. */
    @Nullable
    private static BlockState gate(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean pillar = z == GATE_Z && (ax == GATE_INNER || ax == GATE_OUTER);
        if (y <= 0) return null;
        if (pillar && y < GATE_BEAM) return y == 1 ? s(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
        if (z == GATE_Z) {
            if (y == GATE_BEAM && ax <= GATE_OUTER) {
                return ax < GATE_INNER ? s(Blocks.CHISELED_QUARTZ_BLOCK) : ax == GATE_INNER || ax == GATE_OUTER ? s(Blocks.GOLD_BLOCK) : s(Blocks.SMOOTH_QUARTZ);
            }
            if (y == GATE_BEAM + 1 && ax <= GATE_OUTER + 1) return s(Blocks.SMOOTH_QUARTZ);
            if (y == GATE_BEAM + 2 && ax <= GATE_OUTER + 1) return s(Blocks.PRISMARINE_BRICKS);
            if (y == GATE_BEAM + 3 && (ax == 0 || ax == GATE_OUTER + 1)) return s(Blocks.GOLD_BLOCK);
            if (y == GATE_BEAM - 1 && (ax == (GATE_INNER + GATE_OUTER) / 2)) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
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
        if (y == 1 && hash(x, z, 17) < 7) return s(Blocks.LILY_PAD);
        return y > 0 ? s(Blocks.AIR) : null;
    }

    /** The palace, its plinth and its roof. */
    @Nullable
    private static BlockState palace(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean plinth = inPlinth(x, z);
        // the plinth and its steps
        if (y <= 0) return y == 0 ? (plinth ? s(Blocks.QUARTZ_BRICKS) : paving(x, z, false)) : y == -1 ? s(Blocks.SMOOTH_QUARTZ) : null;
        if (!plinth && z > PLINTH_S) {
            if (ax <= STEPS_HALF && z == PLINTH_S + 2 && y == 1) return stair(Blocks.QUARTZ_STAIRS, Direction.NORTH);
            if (ax <= STEPS_HALF && z == PLINTH_S + 1 && y == 1) return s(Blocks.QUARTZ_BRICKS);
            if (ax <= STEPS_HALF && z == PLINTH_S + 1 && y == 2) return stair(Blocks.QUARTZ_STAIRS, Direction.NORTH);
        }
        if (plinth && y <= PLINTH_H) {
            if (y == PLINTH_H && Math.abs(x) <= HALL_X - 1 && z > HALL_N && z < HALL_S) {
                return ax <= 1 || Math.floorMod(x + z, 2) == 0 ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_DIORITE);
            }
            return y == PLINTH_H ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.QUARTZ_BRICKS);
        }
        int ly = y - PLINTH_H; // over the plinth
        boolean inside = ax < HALL_X && z > HALL_N && z < HALL_S;
        boolean wall = ax <= HALL_X && z >= HALL_N && z <= HALL_S && !inside;
        // the roof: a hip of blue tiles, its eaves at the walls' top, a ridge trimmed with gold
        int d = Math.min(EAVES_X - ax, Math.min(z - EAVES_N, EAVES_S - z));
        int roofY = WALL_TOP + d;
        if (underRoof(x, z) && y >= WALL_TOP) {
            int ridge = (EAVES_S - EAVES_N) / 2;
            if (d >= ridge) {
                if (y == WALL_TOP + ridge) return s(Blocks.PRISMARINE_BRICKS);
                if (y == WALL_TOP + ridge + 1 && (ax == EAVES_X - ridge || ax == 0)) return s(Blocks.GOLD_BLOCK);
                if (y > WALL_TOP + ridge) return s(Blocks.AIR);
            }
            if (y == roofY && d < ridge) {
                Direction toward = EAVES_X - ax == d ? (x > 0 ? Direction.WEST : Direction.EAST) : z - EAVES_N == d ? Direction.SOUTH : Direction.NORTH;
                return stair(Blocks.DARK_PRISMARINE_STAIRS, toward);
            }
            if (y > roofY) return s(Blocks.AIR);
            if (y == WALL_TOP && d <= 1) {
                if (d == 0) return stair(Blocks.DARK_PRISMARINE_STAIRS, EAVES_X - ax == 0 ? (x > 0 ? Direction.WEST : Direction.EAST)
                        : z == EAVES_N ? Direction.SOUTH : Direction.NORTH);
                return s(Blocks.AIR);
            }
            if (y >= CEILING && (inside || wall)) {
                if (y == CEILING && wall) return isPillarAt(x, z) ? s(Blocks.GOLD_BLOCK) : s(Blocks.CHISELED_QUARTZ_BLOCK);
                return s(Blocks.SMOOTH_QUARTZ);
            }
        }
        if (!plinth) return y >= 1 ? s(Blocks.AIR) : null;
        if (wall && ly >= 1 && y <= WALL_TOP) return hallWall(x, y, z);
        if (inside && y < CEILING) return interior(x, y, z);
        return y >= 1 ? s(Blocks.AIR) : null;
    }

    private static boolean isPillarAt(int x, int z) {
        int ax = Math.abs(x);
        if (z == HALL_S || z == HALL_N) return ax == HALL_X || ax == 5 || ax == 2;
        return ax == HALL_X && (z == HALL_N || z == HALL_S || z == -8 || z == -12);
    }

    /** The palace's walls: white, pillars with gold heads, windows of blue glass; its front open in the middle bay. */
    private static BlockState hallWall(int x, int y, int z) {
        int ax = Math.abs(x), ly = y - PLINTH_H;
        if (isPillarAt(x, z)) return y == WALL_TOP ? s(Blocks.GOLD_BLOCK) : pillar();
        if (y == WALL_TOP) return s(Blocks.SMOOTH_QUARTZ);
        if (z == HALL_S && ax < 2) return s(Blocks.AIR); // the door
        if (ly == 1) return s(Blocks.QUARTZ_BRICKS);
        boolean window = z == HALL_S ? ly <= 4 : ly >= 2 && ly <= 3;
        return window ? s(Blocks.LIGHT_BLUE_STAINED_GLASS) : s(Blocks.QUARTZ_BRICKS);
    }

    /** Within: a blue carpet to the altar, its screen of jade and gold, the dais, the chest, candles, lanterns. */
    private static BlockState interior(int x, int y, int z) {
        int ax = Math.abs(x), ly = y - PLINTH_H;
        if (z == SCREEN_Z && ax <= 3) {
            if (ly >= 1 && ly <= 4) return ly >= 2 && ly <= 3 && ax <= 1 ? s(Blocks.GOLD_BLOCK) : s(ModBlocks.JADE_BLOCK.get());
        }
        if (z >= DAIS_N && z <= DAIS_S && ax <= 2) {
            if (ly == 1) return s(Blocks.SMOOTH_QUARTZ);
            if (ly == 2) {
                if (x == CHEST_X && z == CHEST_Z) return s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH);
                if (ax == 2 && z == DAIS_S) return s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
                if (ax == 1 && z == DAIS_N) return s(Blocks.POTTED_AZURE_BLUET);
            }
        }
        if (ly == 1 && ax <= 1 && z > DAIS_S && z < HALL_S) return s(Blocks.LIGHT_BLUE_CARPET);
        if (y == CEILING - 1 && ax == 4 && (z == -8 || z == -12)) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
        if (ly == 1 && ax == HALL_X - 1 && (z == HALL_N + 1 || z == HALL_S - 1)) return s(Blocks.POTTED_CHERRY_SAPLING);
        return s(Blocks.AIR);
    }

    /** The peach trees in blossom by the pool and the palace (each on a bed of grass, petals fallen round it), and the pines in the snow round the terrace. */
    @Nullable
    private static BlockState trees(int x, int y, int z) {
        for (int[] t : PEACHES) {
            int dx = x - t[0], dz = z - t[1];
            if (Math.abs(dx) > 3 || Math.abs(dz) > 3) continue;
            if (y == 0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return s(Blocks.GRASS_BLOCK);
            if (dx == 0 && dz == 0 && y >= 1 && y <= 4) return s(Blocks.CHERRY_LOG);
            if (y == 1 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                return s(Blocks.PINK_PETALS).setValue(PinkPetalsBlock.AMOUNT, 1 + hash(x, z, 5) % 4)
                        .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue(hash(x, z, 6) % 4));
            }
            double r2 = dx * dx + dz * dz + (y - 5.5) * (y - 5.5) * 1.6;
            if (y >= 4 && y <= 7 && r2 <= 7.5 && hash(x * 7 + y, z, 9) < 92) return leaves(Blocks.CHERRY_LEAVES);
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
