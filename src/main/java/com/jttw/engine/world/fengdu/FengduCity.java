package com.jttw.engine.world.fengdu;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.hell.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fengdu, the Ghost City (丰都鬼城), on its hill: in folk belief the city of the dead's officials,
 * where souls pass on their way down to the underworld. A blueprint, as Lion-Camel City is: what
 * stands at each local place, from the place alone, so each chunk of the hill lays its own share
 * ({@code FengduFeature}) and the city comes out whole whichever chunk is made first.
 * <ul>
 *   <li><b>the walls</b>: a square of dark deepslate brick {@value #WALL_H} high, three thick,
 *       {@value #WALL_OUT} blocks out from the centre each way, crenellated, soul lanterns along
 *       their tops; a tower at each corner; one gate, to the south, under a gatehouse with a gate
 *       tower on it (red pillars, a roof of dark tiles) and the city's name over the way in;</li>
 *   <li><b>before it</b>, on the hill's level top, a stone archway (牌坊) over the road: 丰都鬼城;
 *       the road itself, the Road to the Yellow Springs (黄泉路), climbs the hill to it, lit by soul
 *       lanterns ({@code FengduFeature});</li>
 *   <li><b>the lower city</b>: an avenue north from the gate, lit by soul lanterns, a road round
 *       inside the walls, streets between plots of seven by seven: shrines to the officials of the
 *       dead (red walls, a dark tiled roof, a skull-faced image on its altar, candles), the houses
 *       of the dead (dark, shut, red lanterns at their doors), yards where paper offerings burn, and
 *       graveyards;</li>
 *   <li><b>the upper city</b>, a terrace {@value #TERRACE_H} blocks up across its north half, up
 *       three flights of steps: on it the <b>Gate of Ghosts</b> (鬼门关), a great arch of black
 *       stone and red, its way filled with the ghost veil ({@code GhostGateBlock}: step into it and
 *       you are taken down to Diyu), between screen walls with a brazier before each; behind it the
 *       <b>Palace of the Son of Heaven</b> (天子殿), King Yama's, his throne on its dais, an incense
 *       burner before its steps; to its west the Temple of the City God (城隍庙) and to its east the
 *       Hall of the Judges (判官殿).</li>
 * </ul>
 * No one lives here and nothing fights here: it is a place to pass through. Local y 0 is the city's
 * ground (its paving), (0, 0) the region's centre, the main gate toward +z.
 */
public final class FengduCity {

    private FengduCity() {}

    /** The wall's inner and outer faces (blocks out from the centre each way), its height. */
    public static final int WALL_IN = 34, WALL_OUT = 36, WALL_H = 8;
    /** The corner towers: their centres' distance out, half-width, height. */
    public static final int TOWER_AT = 35, TOWER_HALF = 3, TOWER_H = 12;
    /** The main gate: half-width of the way through and its height; the gatehouse over it (its body this high, a gate tower on that). */
    public static final int GATE_HALF = 2, GATE_HIGH = 5, GATEHOUSE_HALF = 6, GATEHOUSE_H = WALL_H + 1;
    /** How far out anything of the city reaches but the archway before it. */
    public static final int OUTER = TOWER_AT + TOWER_HALF + 1;
    /** The archway over the road before the gate: where (local z), its half-width. */
    public static final int ARCH_Z = OUTER + 5, ARCH_HALF = 7;
    /** How far down its foundations go and how high it reaches. */
    public static final int BOTTOM = -3, TOP = 21;
    /** The ring road inside the walls starts this far out; the avenue is this wide either side of the middle. */
    public static final int RING = 30, AVENUE = 2;
    /** The lower city's plots: their first's near edge (from the avenue, and from the terrace's foot), width, pitch, how many. */
    public static final int PLOT_FROM = 5, PLOT_FROM_Z = 4, PLOT = 7, PITCH = 9, PLOTS = 3;

    /** The upper city: the terrace's front (south) edge and its height. */
    public static final int TERRACE_S = -2, TERRACE_H = 3;
    /** The flights of steps up to it: the middle one this wide either side of the avenue, and the side ones between these. */
    public static final int STEPS_HALF = 3, SIDE_STEPS_FROM = 21, SIDE_STEPS_TO = 24;

    /** The Gate of Ghosts: the plane of its veil (local z), the veil's half-width and height over the terrace, the arch's pillars. */
    public static final int GHOST_Z = -7, VEIL_HALF = 2, VEIL_H = 6, PILLAR_IN = 3, PILLAR_OUT = 4;
    /** Its screen walls reach this far either side. */
    public static final int SCREEN_TO = 9;
    /** Where one comes back up out of Diyu: before the Gate of Ghosts, on the terrace (local x, z; standing on the terrace). */
    public static final int RETURN_X = 0, RETURN_Z = GHOST_Z + 4;

    /** The Palace of the Son of Heaven: its plinth, its walls, where the throne stands. */
    public static final int PALACE_X = 12, PALACE_N = -31, PALACE_S = -15;
    public static final int HALL_X = 10, HALL_N = -29, HALL_S = -17, HALL_H = 6, THRONE_Z = -27;
    /** The two temples on the terrace's flanks (either side, between these x), their north and south walls, their walls' height. */
    public static final int TEMPLE_IN = 16, TEMPLE_OUT = 28, TEMPLE_N = -29, TEMPLE_S = -15, TEMPLE_H = 5;

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

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState slab(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static BlockState hanging(Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    private static BlockState candles(int n) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, n).setValue(CandleBlock.LIT, true);
    }

    /** A skull-faced image, looking along {@code facing} (a wither skeleton's skull: the officials of the dead). */
    private static BlockState image(Direction facing) {
        int rot = switch (facing) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            default -> 12;
        };
        return Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rot);
    }

    private static BlockState wallBrick(int x, int y, int z) {
        int r = hash(x * 3 + y, z, 11);
        return r < 12 ? s(Blocks.CRACKED_DEEPSLATE_BRICKS) : r < 26 ? s(Blocks.DEEPSLATE_TILES) : r < 34 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS)
                : s(Blocks.DEEPSLATE_BRICKS);
    }

    /** The ghost veil, its plane along x (the Gate of Ghosts faces south). */
    private static BlockState veil() {
        return ModBlocks.GHOST_GATE.get().defaultBlockState().setValue(GhostGateBlock.AXIS, Direction.Axis.X);
    }

    /**
     * Tier {@code t} (from 0, the eaves) of a hip roof over the box (x0, z0)..(x1, z1): each tier a
     * block in on every side, stairs round its rim (their low side out), tiles within, slabs at its
     * corners and along its ridge; null off it.
     */
    static BlockState hipRoof(int x, int t, int z, int x0, int x1, int z0, int z1, Block stairs, Block slab, Block full) {
        int ax0 = x0 + t, ax1 = x1 - t, az0 = z0 + t, az1 = z1 - t;
        if (ax0 > ax1 || az0 > az1 || x < ax0 || x > ax1 || z < az0 || z > az1) return null;
        if (ax1 - ax0 <= 1 || az1 - az0 <= 1) return slab(slab); // the ridge
        boolean w = x == ax0, e = x == ax1, n = z == az0, so = z == az1;
        if ((w || e) && (n || so)) return slab(slab);
        if (n) return stair(stairs, Direction.SOUTH);
        if (so) return stair(stairs, Direction.NORTH);
        if (w) return stair(stairs, Direction.EAST);
        if (e) return stair(stairs, Direction.WEST);
        return s(full);
    }

    private static BlockState tiles(int x, int t, int z, int x0, int x1, int z0, int z1) {
        return hipRoof(x, t, z, x0, x1, z0, z1, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES);
    }

    // ── Where things are ────────────────────────────────────────────────

    /** Whether local (x, z) is the city's: within its towers' reach, or under the archway before it. */
    public static boolean covers(int x, int z) {
        return Math.max(Math.abs(x), Math.abs(z)) <= OUTER || Math.abs(x) <= ARCH_HALF && Math.abs(z - ARCH_Z) <= 1;
    }

    /** Whether local (x, z) is inside the walls. */
    public static boolean inside(int x, int z) {
        return Math.max(Math.abs(x), Math.abs(z)) < WALL_IN;
    }

    /** Whether local (x, z) is on the upper city's terrace. */
    public static boolean onTerrace(int x, int z) {
        return inside(x, z) && z <= TERRACE_S;
    }

    static boolean inTower(int x, int z) {
        return Math.abs(Math.abs(x) - TOWER_AT) <= TOWER_HALF && Math.abs(Math.abs(z) - TOWER_AT) <= TOWER_HALF;
    }

    static boolean inGatehouse(int x, int z) {
        return Math.abs(x) <= GATEHOUSE_HALF && z >= WALL_IN - 1 && z <= WALL_OUT + 2;
    }

    /** The plot index (1..PLOTS, signed by side) across the avenue at {@code x}, or 0 off the plots. */
    public static int plotIndexX(int x) {
        int m = Math.abs(x) - PLOT_FROM;
        if (m < 0 || m >= PLOTS * PITCH || m % PITCH >= PLOT) return 0;
        return (m / PITCH + 1) * Integer.signum(x);
    }

    /** The plot index (1..PLOTS) from the terrace's foot south at {@code z}, or 0 off the plots. */
    public static int plotIndexZ(int z) {
        int m = z - PLOT_FROM_Z;
        if (m < 0 || m >= PLOTS * PITCH || m % PITCH >= PLOT) return 0;
        return m / PITCH + 1;
    }

    /** What a lower-city plot is: a shrine, a house of the dead, a yard of burning offerings, a graveyard. */
    public static final int SHRINE = 0, HOUSE = 1, OFFERINGS = 2, GRAVES = 3;

    public static int plotKind(int px, int pz) {
        int r = hash(px * 17, pz * 13, 5);
        return r < 36 ? SHRINE : r < 64 ? HOUSE : r < 80 ? OFFERINGS : GRAVES;
    }

    /** Whether local (x, z) is on one of the flights of steps up to the terrace (z -1 to 1). */
    public static boolean onSteps(int x, int z) {
        int ax = Math.abs(x);
        return z >= TERRACE_S + 1 && z <= TERRACE_S + 3 && (ax <= STEPS_HALF || ax >= SIDE_STEPS_FROM && ax <= SIDE_STEPS_TO);
    }

    // ── The signs ───────────────────────────────────────────────────────

    /** A sign on a wall: where (local; y over the city's ground), the way it faces, its lines. */
    public record Sign(int x, int y, int z, Direction facing, String... lines) {}

    /** The city's signs, each on the face of a block of the blueprint. */
    public static final List<Sign> SIGNS = List.of(
            new Sign(0, 7, WALL_OUT + 3, Direction.SOUTH, "丰都", "Fengdu,", "the Ghost City"),
            new Sign(0, 7, ARCH_Z + 1, Direction.SOUTH, "丰都鬼城", "Fengdu", "the Ghost City"),
            new Sign(0, 7, ARCH_Z - 1, Direction.NORTH, "黄泉路", "The Road to", "the Yellow Springs"),
            new Sign(-1, TERRACE_H + VEIL_H + 1, GHOST_Z + 2, Direction.SOUTH, "鬼门关", "The Gate", "of Ghosts"),
            new Sign(1, TERRACE_H + VEIL_H + 1, GHOST_Z + 2, Direction.SOUTH, "Step through:", "Diyu, the", "underworld,", "lies beyond"),
            new Sign(0, TERRACE_H + 1 + HALL_H, HALL_S + 1, Direction.SOUTH, "天子殿", "Palace of the", "Son of Heaven", "(King Yama)"),
            new Sign(-TEMPLE_IN + 1, TERRACE_H + 4, (TEMPLE_N + TEMPLE_S) / 2, Direction.EAST, "城隍庙", "Temple of", "the City God"),
            new Sign(TEMPLE_IN - 1, TERRACE_H + 4, (TEMPLE_N + TEMPLE_S) / 2, Direction.WEST, "判官殿", "Hall of", "the Judges"));

    /** Puts up a sign of the city's at world {@code pos}, its text glowing. */
    public static void putSign(WorldGenLevel level, BlockPos pos, Sign sign) {
        new Site(level, pos.getX(), pos.getZ(), pos.getX(), pos.getZ(), 0, 0, 0).sign(pos.getX(), pos.getY(), pos.getZ(),
                Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, sign.facing()), sign.lines());
    }

    // ── The blueprint ───────────────────────────────────────────────────

    /** What stands at local (x, y, z): a block, air where the city is open, or null where the ground is left as it is. */
    @Nullable
    public static BlockState at(int x, int y, int z) {
        if (!covers(x, z) || y < BOTTOM || y > TOP) return null;
        if (Math.max(Math.abs(x), Math.abs(z)) > OUTER) return arch(x, y, z);
        if (inTower(x, z)) return tower(x, y, z);
        if (inGatehouse(x, z)) return gatehouse(x, y, z);
        int q = Math.max(Math.abs(x), Math.abs(z));
        if (q >= WALL_IN && q <= WALL_OUT) return wall(x, y, z);
        if (q > WALL_OUT) return outside(x, y, z);
        if (y < 0) return null;
        if (z <= TERRACE_S) return terrace(x, y, z);
        if (onSteps(x, z)) return steps(x, y, z);
        int px = plotIndexX(x), pz = plotIndexZ(z);
        if (px == 0 || pz == 0 || q >= RING) return street(x, y, z);
        return plot(px, pz, (Math.abs(x) - PLOT_FROM) % PITCH, (z - PLOT_FROM_Z) % PITCH, x, y, z);
    }

    /** Before the walls: soul lanterns on posts either side of the gate, else the ground as the hill has it. */
    private static BlockState outside(int x, int y, int z) {
        if (z == OUTER && Math.abs(x) == GATEHOUSE_HALF + 2) {
            if (y >= 1 && y <= 2) return s(Blocks.DARK_OAK_FENCE);
            if (y == 3) return s(Blocks.SOUL_LANTERN);
        }
        return null;
    }

    /** The archway over the road (牌坊): three bays on four posts, the middle one high, each roofed; its name over the road. */
    private static BlockState arch(int x, int y, int z) {
        int ax = Math.abs(x), dz = z - ARCH_Z;
        boolean middle = ax <= 4;
        int beam = middle ? 7 : 5;
        if (y <= 0) return y == 0 && dz == 0 && (ax == 3 || ax == ARCH_HALF) ? s(Blocks.POLISHED_BLACKSTONE) : null;
        if (dz == 0 && (ax == 3 || ax == ARCH_HALF) && y <= beam) return y == 1 ? s(Blocks.CHISELED_POLISHED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        if (dz == 0 && (y == beam || y == 5 && ax >= 4)) return ax <= 2 ? s(Blocks.GILDED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        if (dz == 0 && y == 6 && ax == 0) return hanging(Blocks.SOUL_LANTERN);
        if (y == beam + 1 && Math.abs(dz) <= 1) {
            if (dz == -1) return stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH);
            if (dz == 1) return stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH);
            return ax == 4 || ax == ARCH_HALF ? s(Blocks.GILDED_BLACKSTONE) : s(Blocks.DEEPSLATE_TILES);
        }
        if (middle && y == beam + 2 && dz == 0) return slab(Blocks.DEEPSLATE_TILE_SLAB);
        return null;
    }

    private static BlockState wall(int x, int y, int z) {
        if (y <= 0) return wallBrick(x, y, z);
        boolean ns = Math.abs(z) >= Math.abs(x);
        int along = ns ? x : z;
        int depth = Math.max(Math.abs(x), Math.abs(z)); // WALL_IN (inner face) to WALL_OUT (outer)
        if (y <= WALL_H) {
            if (depth == WALL_OUT && y == WALL_H - 2 && Math.floorMod(along, 6) == 3) return s(Blocks.AIR); // slits
            return y == WALL_H ? s(Blocks.POLISHED_DEEPSLATE) : wallBrick(x, y, z);
        }
        // battlements on the outer row, a soul lantern on every eighth merlon; a low parapet on the inner
        if (depth == WALL_OUT && y == WALL_H + 1 && Math.floorMod(along, 2) == 0) return s(Blocks.DEEPSLATE_BRICK_WALL);
        if (depth == WALL_OUT && y == WALL_H + 2 && Math.floorMod(along, 8) == 4) return s(Blocks.SOUL_LANTERN);
        if (depth == WALL_IN && y == WALL_H + 1 && Math.floorMod(along, 4) == 0) return s(Blocks.DEEPSLATE_BRICK_WALL);
        return null;
    }

    private static BlockState tower(int x, int y, int z) {
        int dx = Math.abs(x) - TOWER_AT, dz = Math.abs(z) - TOWER_AT;
        boolean rim = Math.abs(dx) == TOWER_HALF || Math.abs(dz) == TOWER_HALF;
        boolean corner = Math.abs(dx) == TOWER_HALF && Math.abs(dz) == TOWER_HALF;
        if (y <= TOWER_H) {
            if (y > 0 && y < TOWER_H && rim && y % 4 == 2 && (dx == 0 || dz == 0)) return s(Blocks.AIR); // slits
            return y == TOWER_H ? s(Blocks.POLISHED_DEEPSLATE) : wallBrick(x, y, z);
        }
        if (y == TOWER_H + 1 && rim) return corner ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : Math.floorMod(dx + dz, 2) == 0 ? s(Blocks.DEEPSLATE_BRICK_WALL) : null;
        if (y == TOWER_H + 2 && corner) return s(Blocks.SOUL_LANTERN);
        return null;
    }

    private static BlockState gatehouse(int x, int y, int z) {
        int ax = Math.abs(x);
        if (y < 0) return wallBrick(x, y, z);
        if (ax <= GATE_HALF && y >= 1 && y < GATE_HIGH + (ax <= GATE_HALF - 1 ? 1 : 0)) return s(Blocks.AIR);
        if (y == 0) return ax <= GATE_HALF ? s(Blocks.POLISHED_BLACKSTONE) : wallBrick(x, y, z);
        boolean front = z == WALL_OUT + 2;
        if (y <= GATEHOUSE_H) {
            if (front && ax == GATE_HALF + 1 && y <= GATE_HIGH + 1) return s(Blocks.CHISELED_POLISHED_BLACKSTONE); // the jambs
            if (front && y == 7 && ax <= 2) return s(Blocks.POLISHED_BLACKSTONE_BRICKS); // where its name hangs
            if (ax <= GATE_HALF && y == GATE_HIGH + 1 && (front || z == WALL_IN - 1)) return s(Blocks.GILDED_BLACKSTONE); // the lintels
            if (y == GATEHOUSE_H) return s(Blocks.POLISHED_DEEPSLATE);
            return wallBrick(x, y, z);
        }
        // the gate tower on it: red pillars, open between, a lattice frieze, a hip roof of dark tiles; red lanterns at its corners
        int h = y - GATEHOUSE_H; // 1..
        boolean hallRim = ax == GATEHOUSE_HALF - 1 || z == WALL_IN || z == WALL_OUT + 1;
        boolean inHall = ax <= GATEHOUSE_HALF - 1 && z >= WALL_IN && z <= WALL_OUT + 1;
        if (h <= 3) {
            if (!inHall) return h == 1 && (ax == GATEHOUSE_HALF || z == WALL_IN - 1 || z == WALL_OUT + 2) && Math.floorMod(x + z, 2) == 0
                    ? s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL) : null;
            boolean pillar = hallRim && (ax == GATEHOUSE_HALF - 1 || ax == 2) && (z == WALL_IN || z == WALL_OUT + 1);
            if (pillar) return s(Blocks.STRIPPED_MANGROVE_LOG);
            if (hallRim && h == 3) return s(Blocks.DARK_OAK_FENCE);
            if (h == 3 && ax == 3 && (z == WALL_IN + 1 || z == WALL_OUT)) return hanging(Blocks.LANTERN);
            return null;
        }
        return tiles(x, h - 4, z, -GATEHOUSE_HALF, GATEHOUSE_HALF, WALL_IN - 1, WALL_OUT + 2);
    }

    // ── The lower city ──────────────────────────────────────────────────

    private static BlockState street(int x, int y, int z) {
        int ax = Math.abs(x);
        if (y == 0) {
            int r = hash(x, z, 21);
            if (ax <= AVENUE) return ax == 0 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : r < 55 ? s(Blocks.DEEPSLATE_BRICKS) : r < 80 ? s(Blocks.CRACKED_DEEPSLATE_BRICKS)
                    : s(Blocks.DEEPSLATE_TILES);
            return r < 40 ? s(Blocks.COBBLED_DEEPSLATE) : r < 50 ? s(Blocks.GRAVEL) : r < 62 ? s(Blocks.TUFF) : r < 88 ? s(Blocks.DEEPSLATE_TILES)
                    : s(Blocks.MOSSY_COBBLESTONE);
        }
        // soul lanterns on posts down the avenue's sides, every fourth block
        if (ax == AVENUE + 1 && z >= PLOT_FROM_Z && z < RING && Math.floorMod(z, 4) == 0) {
            if (y <= 2) return s(Blocks.DARK_OAK_FENCE);
            if (y == 3) return s(Blocks.SOUL_LANTERN);
        }
        return null;
    }

    /** The steps up to the terrace: three flights (z -1 to 1), each three steps high. */
    private static BlockState steps(int x, int y, int z) {
        int up = TERRACE_S + 4 - z; // 1 at the foot (z 1), 3 at the top (z -1)
        if (y < up) return s(Blocks.DEEPSLATE_BRICKS);
        if (y == up) return stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH);
        return null;
    }

    private static BlockState plot(int px, int pz, int u, int v, int x, int y, int z) {
        // its front, toward the avenue
        Direction front = x > 0 ? Direction.WEST : Direction.EAST;
        return switch (plotKind(px, pz)) {
            case SHRINE -> shrine(u, v, x, y, z, front);
            case HOUSE -> house(px, pz, u, v, x, y, z);
            case OFFERINGS -> offerings(u, v, x, y, z);
            default -> graves(u, v, x, y, z);
        };
    }

    /** A stair laid in a plot's own (u, v) frame, turned for the world: u runs out from the avenue, so westward on the west side. */
    private static BlockState turned(BlockState s, int x) {
        if (x < 0 && s != null && s.hasProperty(StairBlock.FACING) && s.getValue(StairBlock.FACING).getAxis() == Direction.Axis.X) {
            return s.setValue(StairBlock.FACING, s.getValue(StairBlock.FACING).getOpposite());
        }
        return s;
    }

    /** A shrine to an official of the dead: red walls on three sides, open to the street, a dark tiled roof, his skull-faced image on the altar and candles. */
    private static BlockState shrine(int u, int v, int x, int y, int z, Direction front) {
        if (y == 0) return u >= 1 && u <= 5 && v >= 1 && v <= 5 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.COARSE_DIRT);
        boolean in = u >= 1 && u <= 5 && v >= 1 && v <= 5;
        if (y <= 3 && in) {
            boolean post = (u == 1 || u == 5) && (v == 1 || v == 5);
            if (post) return s(Blocks.STRIPPED_MANGROVE_LOG);
            if (u == 5 || v == 1 || v == 5) return y == 3 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.RED_TERRACOTTA);
            if (u == 1) return y == 3 ? s(Blocks.DARK_OAK_FENCE) : null; // the open front, a lattice over it
            // the altar at the back, the image on it, candles either side
            if (u == 4 && v == 3) return y == 1 ? s(Blocks.POLISHED_BLACKSTONE) : y == 2 ? image(front) : null;
            if (u == 4 && (v == 2 || v == 4)) return y == 1 ? s(Blocks.CHISELED_POLISHED_BLACKSTONE) : y == 2 ? candles(2 + hash(x, z, 23) % 3) : null;
            return null;
        }
        if (y >= 4) return turned(tiles(u, y - 4, v, 0, 6, 0, 6), x);
        // candles before it by the street
        if (y == 1 && u == 0 && (v == 2 || v == 4)) return s(Blocks.CHISELED_STONE_BRICKS);
        if (y == 2 && u == 0 && (v == 2 || v == 4)) return candles(1 + hash(x, z, 24) % 4);
        return null;
    }

    private static BlockState houseWall(int px, int pz, int x, int y, int z) {
        int kind = Math.floorMod(px * 3 + pz, 2), r = hash(x * 5 + y, z, 12);
        return kind == 0 ? (r < 25 ? s(Blocks.DEEPSLATE_TILES) : s(Blocks.POLISHED_BLACKSTONE_BRICKS)) : (r < 30 ? s(Blocks.POLISHED_TUFF) : s(Blocks.TUFF_BRICKS));
    }

    /** A house of the dead: dark and shut, a lattice for windows, red lanterns either side of its door, its roof dark. */
    private static BlockState house(int px, int pz, int u, int v, int x, int y, int z) {
        boolean in = u >= 1 && u <= 5 && v >= 1 && v <= 5;
        if (y == 0) return in ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.GRAVEL);
        if (y <= 4 && in) {
            boolean edge = u == 1 || u == 5 || v == 1 || v == 5;
            boolean corner = (u == 1 || u == 5) && (v == 1 || v == 5);
            if (corner) return s(Blocks.DARK_OAK_LOG);
            if (edge) {
                if (u == 1 && v == 3 && y <= 2) return s(Blocks.AIR); // its door
                if (y == 2 && (u == 1 || u == 5) && (v == 2 || v == 4) || y == 2 && (v == 1 || v == 5) && u == 3) return s(Blocks.DARK_OAK_FENCE);
                return houseWall(px, pz, x, y, z);
            }
            // within: a coffin on trestles, a cobweb in a corner
            if (y == 1 && u == 4 && (v >= 2 && v <= 4)) return slab(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
            if (y == 4 && u == 4 && v == 2 && hash(px, pz, 25) < 60) return s(Blocks.COBWEB);
            return s(Blocks.AIR);
        }
        if (y >= 5) return turned(hipRoof(u, y - 5, v, 0, 6, 0, 6, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_PLANKS), x);
        // red lanterns either side of the door, under the eaves
        if (y == 4 && u == 0 && (v == 2 || v == 4)) return hanging(Blocks.LANTERN);
        return null;
    }

    /** A yard where paper offerings burn: a fire walled round (none can step in it), bundles of paper in barrels. */
    private static BlockState offerings(int u, int v, int x, int y, int z) {
        if (y == 0) return u >= 2 && u <= 4 && v >= 2 && v <= 4 ? s(Blocks.POLISHED_BLACKSTONE) : hash(x, z, 31) < 60 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL);
        if (u == 3 && v == 3) return y == 1 ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false) : null;
        if (u >= 2 && u <= 4 && v >= 2 && v <= 4) return y == 1 ? s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL) : null;
        if (u == 6 && (v == 1 || v == 5)) return y <= 2 ? s(Blocks.BARREL) : null;
        if (u == 6 && v == 2 && y == 1) return s(Blocks.BARREL);
        if (u == 5 && v == 0 || u == 5 && v == 6) {
            if (y == 1) return s(Blocks.DARK_OAK_FENCE);
            if (y == 2) return s(Blocks.LANTERN);
        }
        return null;
    }

    /** A graveyard: two rows of graves, a headstone and a mound each, a soul lantern on a post among them. */
    private static BlockState graves(int u, int v, int x, int y, int z) {
        boolean grave = (u == 1 || u == 3 || u == 5) && (v == 1 || v == 4);
        boolean mound = (u == 1 || u == 3 || u == 5) && (v == 2 || v == 5);
        if (y == 0) return mound ? s(Blocks.ROOTED_DIRT) : hash(x, z, 41) < 55 ? s(Blocks.COARSE_DIRT) : s(Blocks.PODZOL);
        if (grave && y == 1) return hash(x, z, 42) < 50 ? s(Blocks.MOSSY_STONE_BRICK_WALL) : s(Blocks.STONE_BRICK_WALL);
        if (grave && y == 2 && hash(x, z, 43) < 30) return candles(1);
        if (u == 6 && v == 3) {
            if (y <= 2) return s(Blocks.DARK_OAK_FENCE);
            if (y == 3) return s(Blocks.SOUL_LANTERN);
        }
        if (y == 1 && !grave && !mound && fine(x, z, 44) < 120) return s(Blocks.DEAD_BUSH);
        return null;
    }

    // ── The upper city ──────────────────────────────────────────────────

    private static BlockState terrace(int x, int y, int z) {
        int ax = Math.abs(x);
        int h = y - TERRACE_H; // 0 is its paving
        if (h < 0) return z == TERRACE_S ? s(Blocks.CHISELED_DEEPSLATE) : s(Blocks.DEEPSLATE_BRICKS);
        if (h == 0) {
            if (ax <= VEIL_HALF) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            if (z == TERRACE_S) return s(Blocks.POLISHED_BLACKSTONE);
            int r = hash(x, z, 51);
            return r < 45 ? s(Blocks.POLISHED_DEEPSLATE) : r < 80 ? s(Blocks.DEEPSLATE_TILES) : r < 92 ? s(Blocks.CRACKED_DEEPSLATE_TILES) : s(Blocks.POLISHED_TUFF);
        }
        // a balustrade along its edge, open at the steps
        if (z == TERRACE_S) {
            boolean open = ax <= STEPS_HALF || ax >= SIDE_STEPS_FROM && ax <= SIDE_STEPS_TO;
            return h == 1 && !open && Math.floorMod(x, 3) == 0 ? s(Blocks.POLISHED_BLACKSTONE_WALL) : null;
        }
        if (z >= GHOST_Z - 2 && z <= GHOST_Z + 3 && ax <= SCREEN_TO + 3) {
            BlockState g = ghostGate(x, h, z);
            if (g != null) return g;
        }
        if (ax <= PALACE_X + 1 && z >= PALACE_N && z <= PALACE_S + 4) return palace(x, h, z);
        if (ax >= TEMPLE_IN - 1 && ax <= TEMPLE_OUT + 1 && z >= TEMPLE_N - 1 && z <= TEMPLE_S + 1) return temple(x, h, z);
        // soul lanterns on posts about the terrace
        if (h <= 3 && (ax == 12 || ax == 30) && Math.floorMod(z, 6) == 0 && z <= -6) return h <= 2 ? s(Blocks.DARK_OAK_FENCE) : s(Blocks.SOUL_LANTERN);
        return null;
    }

    /** Whether local (x, y, z) is in the Gate of Ghosts' veil ({@code y} over the city's ground). */
    public static boolean inVeil(int x, int y, int z) {
        return z == GHOST_Z && Math.abs(x) <= VEIL_HALF && y > TERRACE_H && y <= TERRACE_H + VEIL_H;
    }

    /** The Gate of Ghosts, {@code h} over the terrace: null where it is not. */
    private static BlockState ghostGate(int x, int h, int z) {
        int ax = Math.abs(x), dz = z - GHOST_Z;
        // braziers of soul fire before it
        if (ax == 6 && dz == 3) return h == 1 ? s(Blocks.POLISHED_BLACKSTONE_WALL) : h == 2 ? s(Blocks.SOUL_CAMPFIRE) : null;
        if (Math.abs(dz) > 2) return null;
        if (h >= 1 && h <= VEIL_H && dz == 0 && ax <= VEIL_HALF) return veil();
        // the arch: two pillars a side, two wide and three deep, black stone on gold feet
        boolean pillar = (ax == PILLAR_IN || ax == PILLAR_OUT) && Math.abs(dz) <= 1;
        if (pillar && h <= VEIL_H) return h == 1 ? s(Blocks.GILDED_BLACKSTONE) : ax == PILLAR_IN && dz == 0 ? s(Blocks.RED_NETHER_BRICKS) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        // its lintel and the beam over it
        if (ax <= PILLAR_OUT && Math.abs(dz) <= 1 && (h == VEIL_H + 1 || h == VEIL_H + 2)) {
            if (h == VEIL_H + 1 && dz == 0 && ax <= VEIL_HALF) return s(Blocks.GILDED_BLACKSTONE);
            return h == VEIL_H + 2 ? s(Blocks.RED_NETHER_BRICKS) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        // the roof, a soul lantern hung at each corner of it
        if (h >= VEIL_H + 3) {
            BlockState r = tiles(x, h - VEIL_H - 3, z, -6, 6, GHOST_Z - 2, GHOST_Z + 2);
            if (r != null && h == VEIL_H + 5 && ax == 4 && dz == 0) return s(Blocks.GILDED_BLACKSTONE); // its ridge's ends
            return r;
        }
        if (h == VEIL_H + 2 && ax == 5 && Math.abs(dz) == 2) return hanging(Blocks.SOUL_LANTERN);
        // the screen walls either side: red, a cap of dark tile, black posts at their ends
        if (dz == 0 && ax > PILLAR_OUT && ax <= SCREEN_TO) {
            if (ax == SCREEN_TO) return h <= 5 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : h == 6 ? slab(Blocks.POLISHED_BLACKSTONE_SLAB) : null;
            if (h <= 4) return h == 1 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.RED_TERRACOTTA);
            if (h == 5) return slab(Blocks.DEEPSLATE_TILE_SLAB);
        }
        return null;
    }

    /** The Palace of the Son of Heaven, {@code h} over the terrace. */
    private static BlockState palace(int x, int h, int z) {
        int ax = Math.abs(x);
        boolean plinth = ax <= PALACE_X && z >= PALACE_N && z <= PALACE_S;
        // the incense burner before its steps: a fire walled round
        if (ax <= 1 && z >= PALACE_S + 2 && z <= PALACE_S + 4 && h == 1) {
            return ax == 0 && z == PALACE_S + 3 ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false)
                    : s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        }
        if (!plinth) {
            // the steps up to its plinth
            if (z == PALACE_S + 1 && ax <= 2 && h == 1) return stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH);
            return null;
        }
        if (h == 1) {
            boolean inHall = ax <= HALL_X && z >= HALL_N && z <= HALL_S;
            if (!inHall) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            return Math.floorMod(x + z, 2) == 0 ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.DEEPSLATE_TILES);
        }
        return hall(x, h - 1, z);
    }

    /** The palace's hall, {@code h} over its floor. */
    private static BlockState hall(int x, int h, int z) {
        int ax = Math.abs(x);
        boolean inHall = ax <= HALL_X && z >= HALL_N && z <= HALL_S;
        if (h > HALL_H + 1) return tiles(x, h - HALL_H - 2, z, -HALL_X - 1, HALL_X + 1, HALL_N - 1, HALL_S + 1);
        if (!inHall) return null;
        boolean pillar = (ax == HALL_X || ax == 6 || ax == 2) && (z == HALL_S || z == HALL_N) || ax == HALL_X && Math.floorMod(z - HALL_N, 4) == 0;
        if (h == HALL_H + 1) return s(Blocks.DARK_OAK_PLANKS); // its ceiling
        if (pillar) return s(Blocks.STRIPPED_MANGROVE_LOG);
        if (z == HALL_N || ax == HALL_X) return h == HALL_H ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.RED_TERRACOTTA);
        if (z == HALL_S) {
            if (ax <= 1 && h <= 3) return s(Blocks.AIR); // its door
            if (h == HALL_H) return s(Blocks.DARK_OAK_PLANKS);
            return h >= 3 ? s(Blocks.DARK_OAK_FENCE) : s(Blocks.RED_TERRACOTTA);
        }
        // within: the dais, Yama's throne on it, a gilded screen behind, his image over it; candles; a red carpet to it
        if (h == 1) {
            if (z >= THRONE_Z - 1 && z <= THRONE_Z + 1 && ax <= 4) return s(Blocks.POLISHED_BLACKSTONE);
            if (ax <= 1 && z > THRONE_Z + 1) return s(Blocks.RED_CARPET);
            return s(Blocks.AIR);
        }
        if (h == 2) {
            if (z == THRONE_Z && ax == 0) return stair(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.NORTH);
            if (z == THRONE_Z && ax == 1) return slab(Blocks.POLISHED_BLACKSTONE_SLAB);
            if (z == THRONE_Z + 1 && ax == 4) return candles(4);
        }
        if (z == THRONE_Z - 1 && ax <= 1 && h >= 2 && h <= 4) return s(Blocks.GILDED_BLACKSTONE);
        if (z == THRONE_Z - 1 && ax == 0 && h == 5) return image(Direction.SOUTH);
        if (h == HALL_H && ax == 5 && Math.floorMod(z - HALL_N, 4) == 2) return hanging(Blocks.SOUL_LANTERN);
        return s(Blocks.AIR);
    }

    /** The temples on the terrace's flanks (the City God's to the west, the Judges' to the east), {@code h} over the terrace. */
    private static BlockState temple(int x, int h, int z) {
        int ax = Math.abs(x), sx = Integer.signum(x);
        boolean in = ax >= TEMPLE_IN && ax <= TEMPLE_OUT && z >= TEMPLE_N && z <= TEMPLE_S;
        if (h > TEMPLE_H + 1) return turned(tiles(ax, h - TEMPLE_H - 2, z, TEMPLE_IN - 1, TEMPLE_OUT + 1, TEMPLE_N - 1, TEMPLE_S + 1), x);
        if (!in) return null;
        boolean edge = ax == TEMPLE_IN || ax == TEMPLE_OUT || z == TEMPLE_N || z == TEMPLE_S;
        boolean corner = (ax == TEMPLE_IN || ax == TEMPLE_OUT) && (z == TEMPLE_N || z == TEMPLE_S);
        int door = (TEMPLE_N + TEMPLE_S) / 2;
        if (h == TEMPLE_H + 1) return s(Blocks.DARK_OAK_PLANKS);
        if (corner || edge && ax == TEMPLE_IN && Math.abs(z - door) == 2) return s(Blocks.STRIPPED_MANGROVE_LOG);
        if (edge) {
            if (ax == TEMPLE_IN && Math.abs(z - door) <= 1 && h <= 3) return s(Blocks.AIR); // its door, toward the palace
            if (h == TEMPLE_H) return s(Blocks.DARK_OAK_PLANKS);
            if (h == 3 && (z == TEMPLE_N || z == TEMPLE_S) && Math.floorMod(ax, 3) == 0) return s(Blocks.DARK_OAK_FENCE);
            return s(Blocks.RED_TERRACOTTA);
        }
        // the image of its god at the back, on a pedestal, candles round it; soul lanterns hung
        if (ax == TEMPLE_OUT - 1 && z == door) return h <= 2 ? s(Blocks.POLISHED_BLACKSTONE) : h == 3 ? image(sx > 0 ? Direction.WEST : Direction.EAST) : s(Blocks.AIR);
        if (ax == TEMPLE_OUT - 1 && Math.abs(z - door) == 1) return h == 1 ? s(Blocks.CHISELED_POLISHED_BLACKSTONE) : h == 2 ? candles(3) : s(Blocks.AIR);
        if (h == TEMPLE_H && ax == (TEMPLE_IN + TEMPLE_OUT) / 2 && Math.floorMod(z - TEMPLE_N, 5) == 2) return hanging(Blocks.SOUL_LANTERN);
        if (h == 1 && Math.abs(z - door) == 0 && ax > TEMPLE_IN && ax < TEMPLE_OUT - 1) return s(Blocks.RED_CARPET);
        return s(Blocks.AIR);
    }

    /** Lays the whole city round {@code ground} (its ground's level at the centre) at once, its signs too: the gallery's, and tests'. */
    public static void layWhole(WorldGenLevel level, BlockPos ground) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -OUTER; x <= OUTER; x++) for (int z = -OUTER; z <= ARCH_Z + 1; z++) for (int y = BOTTOM; y <= TOP; y++) {
            BlockState s = at(x, y, z);
            if (s != null) level.setBlock(p.set(ground.getX() + x, ground.getY() + y, ground.getZ() + z), s, Block.UPDATE_CLIENTS);
        }
        for (Sign sign : SIGNS) putSign(level, ground.offset(sign.x(), sign.y(), sign.z()), sign);
    }

}
