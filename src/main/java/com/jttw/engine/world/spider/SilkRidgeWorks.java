package com.jttw.engine.world.spider;

import com.jttw.engine.registry.SpiderCaveRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import javax.annotation.Nullable;

/**
 * What men (and demons passing for them) built on Silk-Web Ridge, each a blueprint laid column by
 * column ({@link #at}) so it comes out whole across the chunks it spans. Local coordinates are
 * offsets from the region's centre (+z south), heights from the valley's floor {@code F} (y 0 the
 * first block over the ground, -1 the ground).
 * <ul>
 *   <li><b>the thatched cottage</b> (茅屋, chapter 72: the Tang Monk saw a cottage under the ridge
 *       and went begging there, and found four women at their embroidery): walls of mud brick in a
 *       frame of dark timber, a deep thatch, a yard of trodden earth before it; its back room opens
 *       on the passage into the cave, the stone face of the ridge rising behind it;</li>
 *   <li><b>the Purifying Spring</b> (濯垢泉), where the sisters bathe: a steaming pool in a rim of
 *       mossy stone, a bath pavilion on its north bank over the water, steps down into it;</li>
 *   <li><b>the Yellow Flower Temple</b> (黄花观): the Hundred-Eyed Demon Lord's, a walled Daoist
 *       temple in ochre with a gatehouse, a court of yellow flowers and a censer, side halls, and the
 *       double-eaved main hall with its altar; and over all of it, while any sister lives, their
 *       <b>canopy of silk</b> ({@link #canopy}), as they threw it over the temple in chapter 73.</li>
 * </ul>
 */
public final class SilkRidgeWorks {

    private SilkRidgeWorks() {}

    // ── Where they stand ────────────────────────────────────────────────

    /** The cottage: its middle, its half-width, its back and front (local z). */
    public static final int COTTAGE_X = 0, COTTAGE_Z = 52, COTTAGE_HALF = 7, COTTAGE_BACK = 46, COTTAGE_FRONT = 58;
    /** The cottage's yard reaches this far south of the cottage's middle, this far either side. */
    public static final int YARD_S = 17, YARD_HALF = 17;
    /** The spring's middle, its pool's half-widths. */
    public static final int SPRING_X = 100, SPRING_Z = 82, POOL_RX = 11, POOL_RZ = 8, SPRING_LEVEL = 24;
    /** The temple's middle, its walls' half-width and its north and south walls (from its middle). */
    public static final int TEMPLE_X = -96, TEMPLE_Z = 74, TEMPLE_HALF = 24, TEMPLE_N = -30, TEMPLE_S = 30;
    /** The canopy: an ellipsoid over the temple, these half-widths and this high. */
    public static final int CANOPY_RX = 36, CANOPY_RZ = 44, CANOPY_H = 27;
    /** Where the Demon Lord stands, from the temple's middle: in its court. */
    public static final int COURT_X = 0, COURT_Z = 12;

    /** The stone face of the ridge behind the cottage, at the cave's mouth: how far either side, from where to where. */
    public static final int FACE_HALF = 20, FACE_N = 30, FACE_HIGH = 17;

    private static double smooth(double from, double to, double v) {
        double s = Math.max(0, Math.min(1, (v - from) / (to - from)));
        return s * s * (3 - 2 * s);
    }

    /** How far outside a rectangle (lx, lz) is (0 inside). */
    private static double outside(int lx, int lz, int x0, int x1, int z0, int z1) {
        double dx = Math.max(0, Math.max(x0 - lx, lx - x1)), dz = Math.max(0, Math.max(z0 - lz, lz - z1));
        return Math.hypot(dx, dz);
    }

    /** How level the ground at (lx, lz) is kept for the works: 1 on their grounds, falling off over a few blocks round them. */
    public static double levelShare(int lx, int lz) {
        double yard = outside(lx, lz, COTTAGE_X - YARD_HALF, COTTAGE_X + YARD_HALF, COTTAGE_BACK, COTTAGE_Z + YARD_S);
        double spring = Math.max(0, Math.hypot(lx - SPRING_X, lz - SPRING_Z) - SPRING_LEVEL);
        double temple = outside(lx, lz, TEMPLE_X - CANOPY_RX - 6, TEMPLE_X + CANOPY_RX + 6, TEMPLE_Z - CANOPY_RZ - 4, TEMPLE_Z + CANOPY_RZ + 6);
        return 1 - smooth(0, 9, Math.min(yard, Math.min(spring, temple)));
    }

    /** The least height of the ground's top at (lx, lz), over the floor: the ridge's stone face behind the cottage. */
    public static int faceAt(int lx, int lz) {
        if (Math.abs(lx) > FACE_HALF || lz < FACE_N || lz >= COTTAGE_BACK) return Integer.MIN_VALUE;
        int drop = Math.max(0, Math.abs(lx) - 13) * 2;
        return FACE_HIGH - drop;
    }

    /** Whether (lx, lz) is any of the works' (where {@link #at} may say something). */
    public static boolean covers(int lx, int lz) {
        return inCottage(lx, lz) || Math.hypot(lx - SPRING_X, lz - SPRING_Z) <= SPRING_LEVEL
                || Math.abs(lx - TEMPLE_X) <= CANOPY_RX + 1 && Math.abs(lz - TEMPLE_Z) <= CANOPY_RZ + 1;
    }

    private static boolean inCottage(int lx, int lz) {
        return Math.abs(lx - COTTAGE_X) <= YARD_HALF && lz >= COTTAGE_BACK - 1 && lz <= COTTAGE_Z + YARD_S;
    }

    /** The lowest and highest local y any work may reach. */
    public static final int BOTTOM = -4, TOP = CANOPY_H + 1;

    /**
     * What stands at local (lx, ly, lz) of the works, or null where they say nothing (the ground
     * or the air as the land has it). {@code canopy}: whether the sisters' canopy is over the temple.
     */
    @Nullable
    public static BlockState at(int lx, int ly, int lz, boolean canopy) {
        if (inCottage(lx, lz)) return cottage(lx - COTTAGE_X, ly, lz - COTTAGE_Z);
        if (Math.hypot(lx - SPRING_X, lz - SPRING_Z) <= SPRING_LEVEL) return spring(lx - SPRING_X, ly, lz - SPRING_Z);
        int tx = lx - TEMPLE_X, tz = lz - TEMPLE_Z;
        if (Math.abs(tx) <= CANOPY_RX + 1 && Math.abs(tz) <= CANOPY_RZ + 1) {
            if (canopy && inCanopy(tx, ly, tz)) return SpiderCaveRegistry.SILK_SEAL.get().defaultBlockState();
            return temple(tx, ly, tz);
        }
        return null;
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static int hash(int x, int y, int z, int salt) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L ^ salt * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return (int) Math.floorMod(h >>> 20, 100L);
    }

    private static BlockState stair(Block b, Direction facing, StairsShape shape, boolean upsideDown) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.SHAPE, shape)
                .setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState slab(Block b, boolean top) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /**
     * A hip roof over x [x0, x1] and z [z0, z1] from height {@code base} up, each course a block in
     * from the one under it: stairs round its edge (corners turned), a full block where it closes.
     * Its eaves turn up at the corners. Null where it is not.
     */
    @Nullable
    static BlockState hip(int x, int y, int z, int x0, int x1, int z0, int z1, int base, Block stairs, Block full) {
        int k = y - base;
        if (k < 0) return null;
        int a0 = x0 + k, a1 = x1 - k, b0 = z0 + k, b1 = z1 - k;
        if (a0 > a1 || b0 > b1 || x < a0 || x > a1 || z < b0 || z > b1) return null;
        boolean w = x == a0, e = x == a1, n = z == b0, so = z == b1;
        if (a1 - a0 <= 1 || b1 - b0 <= 1) return s(full); // the ridge, closing it
        if (!(w || e || n || so)) return null;
        if (so) return stair(stairs, Direction.NORTH, w ? StairsShape.OUTER_RIGHT : e ? StairsShape.OUTER_LEFT : StairsShape.STRAIGHT, false);
        if (n) return stair(stairs, Direction.SOUTH, w ? StairsShape.OUTER_LEFT : e ? StairsShape.OUTER_RIGHT : StairsShape.STRAIGHT, false);
        return stair(stairs, w ? Direction.EAST : Direction.WEST, StairsShape.STRAIGHT, false);
    }

    /** A hip roof's turned-up eave corners: a stair standing on each corner of its lowest course. */
    @Nullable
    static BlockState eaveTips(int x, int y, int z, int x0, int x1, int z0, int z1, int base, Block stairs) {
        if (y != base + 1) return null;
        boolean cx = x == x0 || x == x1, cz = z == z0 || z == z1;
        if (!(cx && cz)) return null;
        return stair(stairs, z == z0 ? Direction.NORTH : Direction.SOUTH, StairsShape.STRAIGHT, true);
    }

    // ── The thatched cottage ────────────────────────────────────────────

    /** The cottage and its yard, local to the cottage's middle (cz: from its middle, +z its front). */
    @Nullable
    static BlockState cottage(int cx, int y, int cz) {
        int ax = Math.abs(cx);
        int z = cz + COTTAGE_Z; // back in region terms for the walls
        boolean inside = ax <= COTTAGE_HALF && z >= COTTAGE_BACK && z <= COTTAGE_FRONT;
        if (y == -1) {
            if (inside) return s(Blocks.PACKED_MUD);
            if (ax <= 1 && z > COTTAGE_FRONT) return hash(cx, y, cz, 1) < 70 ? s(Blocks.DIRT_PATH) : s(Blocks.COARSE_DIRT);
            int r = hash(cx, y, cz, 2);
            return r < 40 ? s(Blocks.COARSE_DIRT) : r < 70 ? s(Blocks.DIRT_PATH) : r < 85 ? s(Blocks.GRAVEL) : s(Blocks.ROOTED_DIRT);
        }
        if (!inside) {
            // the yard: a well-sweep's trough, a woodpile, a fence of withies along its sides
            if (y == 0 && (ax == YARD_HALF - 1) && z > COTTAGE_FRONT + 2 && z < COTTAGE_Z + YARD_S - 1) return s(Blocks.SPRUCE_FENCE);
            if (cx == 10 && z >= 60 && z <= 62 && y <= 1) return log(Blocks.SPRUCE_LOG, Direction.Axis.Z);
            if (cx == 11 && z >= 60 && z <= 61 && y == 0) return log(Blocks.SPRUCE_LOG, Direction.Axis.Z);
            if (cx == -10 && z == 62 && y == 0) return s(Blocks.WATER_CAULDRON);
            if (cx == -11 && z == 62 && y == 0) return s(Blocks.BARREL);
            if (cx == -10 && z == 63 && y == 0) return s(Blocks.COMPOSTER);
            if (y >= 0 && y <= 8 && hip(cx, y, z, -COTTAGE_HALF - 2, COTTAGE_HALF + 2, COTTAGE_BACK - 1, COTTAGE_FRONT + 2, 4, Blocks.SPRUCE_STAIRS, Blocks.HAY_BLOCK) != null) {
                return thatch(cx, y, z);
            }
            return y <= 9 ? s(Blocks.AIR) : null;
        }
        boolean wallX = ax == COTTAGE_HALF, wallZ = z == COTTAGE_BACK || z == COTTAGE_FRONT;
        boolean corner = wallX && wallZ;
        if (y <= 3) {
            if (corner || (wallZ && ax == 3) || (wallX && (z == COTTAGE_Z))) return log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
            if (wallZ || wallX) {
                // the door at the front, the door into the cave at the back; windows
                if (z == COTTAGE_FRONT && ax <= 1 && y <= 2) return s(Blocks.AIR);
                if (z == COTTAGE_BACK && ax <= 1 && y <= 2) return s(Blocks.AIR);
                if (y == 2 && (wallX ? Math.abs(z - COTTAGE_Z) == 3 : ax == 5)) return s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true)
                        .setValue(TrapDoorBlock.FACING, wallX ? (cx > 0 ? Direction.EAST : Direction.WEST) : (z == COTTAGE_FRONT ? Direction.SOUTH : Direction.NORTH));
                if (y == 3) return log(Blocks.DARK_OAK_LOG, wallX ? Direction.Axis.Z : Direction.Axis.X);
                return hash(cx, y, z, 3) < 15 ? s(Blocks.MUD_BRICKS) : s(Blocks.PACKED_MUD);
            }
            // within: a loom and an embroidery frame, a table, a bench; a chest; candles
            if (y == 0 && cx == -5 && z == 49) return s(Blocks.LOOM).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
            if (y == 0 && cx == -3 && z == 49) return s(Blocks.LOOM).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
            if (y == 0 && cx == 5 && z == 49) return s(Blocks.CARTOGRAPHY_TABLE);
            if (y == 0 && cx >= 3 && cx <= 5 && z == 54) return s(Blocks.SPRUCE_FENCE);
            if (y == 1 && cx >= 3 && cx <= 5 && z == 54) return slab(Blocks.SPRUCE_SLAB, false);
            if (y == 2 && cx == 4 && z == 54) return s(Blocks.CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3)
                    .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true);
            if (y == 0 && cx >= 3 && cx <= 5 && z == 56) return stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, StairsShape.STRAIGHT, false);
            if (y == 0 && cx == -5 && z == 56) return s(Blocks.CHEST);
            if (y == 0 && cx == -6 && z == 53) return s(Blocks.WHITE_CARPET);
            if (y == 0 && cx == -5 && z == 53) return s(Blocks.WHITE_CARPET);
            if (y == 3 && cx == 0 && z == 52) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            return s(Blocks.AIR);
        }
        // the thatch: a deep hip roof of straw over it, its eaves well out
        BlockState roof = hip(cx, y, z, -COTTAGE_HALF - 2, COTTAGE_HALF + 2, COTTAGE_BACK - 1, COTTAGE_FRONT + 2, 4, Blocks.SPRUCE_STAIRS, Blocks.HAY_BLOCK);
        if (roof != null) return thatch(cx, y, z);
        if (y == 4 && ax <= COTTAGE_HALF && z >= COTTAGE_BACK && z <= COTTAGE_FRONT) {
            if (ax == COTTAGE_HALF || z == COTTAGE_BACK || z == COTTAGE_FRONT) return log(Blocks.DARK_OAK_LOG, ax == COTTAGE_HALF ? Direction.Axis.Z : Direction.Axis.X);
            if (cx == 0) return log(Blocks.DARK_OAK_LOG, Direction.Axis.Z); // the ridge beam
            return s(Blocks.AIR);
        }
        return y <= 11 ? s(Blocks.AIR) : null;
    }

    /** Thatch: straw, with darker, weathered patches. */
    private static BlockState thatch(int x, int y, int z) {
        BlockState roof = hip(x, y, z, -COTTAGE_HALF - 2, COTTAGE_HALF + 2, COTTAGE_BACK - 1, COTTAGE_FRONT + 2, 4, Blocks.SPRUCE_STAIRS, Blocks.HAY_BLOCK);
        if (roof == null) return s(Blocks.AIR);
        if (roof.is(Blocks.HAY_BLOCK)) return roof;
        // the straw itself is the hay; its eaves a course of weathered stair
        int k = y - 4;
        boolean edge = x == -COTTAGE_HALF - 2 + k || x == COTTAGE_HALF + 2 - k || z == COTTAGE_BACK - 1 + k || z == COTTAGE_FRONT + 2 - k;
        if (k == 0) return roof;
        return edge && hash(x, y, z, 4) < 35 ? roof : s(Blocks.HAY_BLOCK);
    }

    // ── The Purifying Spring ────────────────────────────────────────────

    /** How far into the pool (sx, sz) is: under 1 in it. */
    public static double pool(int sx, int sz) {
        double dx = sx / (double) POOL_RX, dz = sz / (double) POOL_RZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Whether local (lx, lz) is in the spring's pool. */
    public static boolean inPool(int lx, int lz) {
        return pool(lx - SPRING_X, lz - SPRING_Z) < 1;
    }

    /** The bath pavilion over the pool's north bank: its middle (from the spring's), its half-width. */
    static final int PAVILION_Z = -8, PAVILION_HALF = 4;

    @Nullable
    static BlockState spring(int sx, int y, int sz) {
        double r = pool(sx, sz);
        int px = sx, pz = sz - PAVILION_Z;
        boolean pavilion = Math.abs(px) <= PAVILION_HALF && Math.abs(pz) <= PAVILION_HALF;
        if (pavilion) {
            int ax = Math.abs(px), az = Math.abs(pz);
            if (y == -1) return s(Blocks.SPRUCE_PLANKS);
            if (y == -2 || y == -3) return r < 1 ? s(Blocks.STONE) : null;
            if (y >= 0 && y <= 3 && ax == PAVILION_HALF - 1 && az == PAVILION_HALF - 1) return log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y);
            if (y == 0 && (ax == PAVILION_HALF || az == PAVILION_HALF) && !(pz == PAVILION_HALF && ax <= 1)) return s(Blocks.SPRUCE_FENCE);
            if (y == 0 && px == 0 && pz == 0) return s(Blocks.SPRUCE_SLAB);
            if (y == 3 && px == 0 && pz == 0) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            BlockState roof = hip(px, y, pz, -PAVILION_HALF - 1, PAVILION_HALF + 1, -PAVILION_HALF - 1, PAVILION_HALF + 1, 4, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
            if (roof != null) return roof;
            BlockState tip = eaveTips(px, y, pz, -PAVILION_HALF - 1, PAVILION_HALF + 1, -PAVILION_HALF - 1, PAVILION_HALF + 1, 4, Blocks.DEEPSLATE_TILE_STAIRS);
            if (tip != null) return tip;
            if (y >= 0 && y <= 9) return s(Blocks.AIR);
            return null;
        }
        if (r < 1) {
            // the water, warm and clear over a floor of smooth stone and sand; steps down at its south
            if (y == -4) return s(Blocks.STONE);
            if (y == -3) return hash(sx, y, sz, 5) < 70 ? s(Blocks.SMOOTH_STONE) : s(Blocks.SAND);
            if (y == -2 || y == -1) {
                if (sz >= POOL_RZ - 2 && Math.abs(sx) <= 1) return y == -2 ? stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, StairsShape.STRAIGHT, false)
                        .setValue(StairBlock.WATERLOGGED, true) : s(Blocks.WATER);
                return s(Blocks.WATER);
            }
            if (y >= 0 && y <= 4) return s(Blocks.AIR);
            return null;
        }
        // its rim of mossy stone, boulders on it; then the ground, stone and moss
        if (r < 1.18) {
            if (y == -1) return hash(sx, y, sz, 6) < 50 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.MOSSY_COBBLESTONE);
            if (y == 0 && hash(sx, 0, sz, 7) < 18 && !(sz > 0 && Math.abs(sx) <= 2)) return s(Blocks.MOSSY_COBBLESTONE);
            if (y == 1 && hash(sx, 0, sz, 7) < 5 && !(sz > 0 && Math.abs(sx) <= 2)) return s(Blocks.MOSSY_COBBLESTONE_WALL);
            if (y >= 0 && y <= 4) return s(Blocks.AIR);
            return null;
        }
        if (y == -1) {
            int h = hash(sx, y, sz, 8);
            return h < 45 ? s(Blocks.MOSS_BLOCK) : h < 70 ? s(Blocks.STONE) : h < 85 ? s(Blocks.GRAVEL) : s(Blocks.COARSE_DIRT);
        }
        // stone lanterns at the steps, ferns about
        if ((sx == 3 || sx == -3) && sz == POOL_RZ + 2) {
            if (y == 0) return s(Blocks.STONE_BRICK_WALL);
            if (y == 1) return s(Blocks.LANTERN);
        }
        if (y == 0 && hash(sx, 0, sz, 9) < 9) return s(Blocks.FERN);
        if (y >= 0 && y <= 4) return s(Blocks.AIR);
        return null;
    }

    // ── The Yellow Flower Temple ────────────────────────────────────────

    /** Whether temple-local (tx, ly, tz) is in the canopy's shell: inside its ellipsoid with a face to the outside. */
    public static boolean inCanopy(int tx, int ly, int tz) {
        if (ly < 0 || !insideCanopy(tx, ly, tz)) return false;
        return !insideCanopy(tx + 1, ly, tz) || !insideCanopy(tx - 1, ly, tz) || !insideCanopy(tx, ly + 1, tz)
                || !insideCanopy(tx, ly, tz + 1) || !insideCanopy(tx, ly, tz - 1);
    }

    private static boolean insideCanopy(int tx, int ly, int tz) {
        double x = tx / (double) CANOPY_RX, y = ly / (double) CANOPY_H, z = tz / (double) CANOPY_RZ;
        return x * x + y * y + z * z < 1;
    }

    /** The main hall: its half-width, its back and front (from the temple's middle). */
    static final int HALL_HALF = 13, HALL_N = -27, HALL_S = -9;

    @Nullable
    static BlockState temple(int tx, int y, int tz) {
        int ax = Math.abs(tx);
        boolean walled = ax <= TEMPLE_HALF && tz >= TEMPLE_N && tz <= TEMPLE_S;
        if (y == -1) {
            if (!walled) return hash(tx, y, tz, 10) < 60 ? s(Blocks.GRAVEL) : s(Blocks.COBBLESTONE);
            if (ax <= 2 || (tz >= HALL_S && tz <= HALL_S + 2 && ax <= 8)) return s(Blocks.STONE_BRICKS);
            if (flowerbed(tx, tz)) return s(Blocks.GRASS_BLOCK);
            return hash(tx, y, tz, 11) < 75 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.ANDESITE);
        }
        if (!walled) return y <= CANOPY_H + 1 ? s(Blocks.AIR) : null;
        // the outer wall, ochre under dark tiles, the gate in its south side
        boolean wall = ax == TEMPLE_HALF || tz == TEMPLE_N || tz == TEMPLE_S;
        if (wall) {
            if (tz == TEMPLE_S && ax <= 1 && y <= 3) return s(Blocks.AIR);
            if (y <= 3) return s(Blocks.YELLOW_TERRACOTTA);
            if (y == 4) return s(Blocks.DEEPSLATE_TILES);
            if (y == 5) return slab(Blocks.DEEPSLATE_TILE_SLAB, false);
        }
        // the gatehouse over the gate
        if (tz >= TEMPLE_S - 2 && tz <= TEMPLE_S + 2 && ax <= 5) {
            BlockState g = gatehouse(tx, y, tz - TEMPLE_S);
            if (g != null) return g;
        }
        // the main hall
        if (tz >= HALL_N - 3 && tz <= HALL_S + 3 && ax <= HALL_HALF + 3) {
            BlockState h = hall(tx, y, tz);
            if (h != null) return h;
        }
        // the side halls along the east and west walls
        if (ax >= 15 && ax <= TEMPLE_HALF - 1 && tz >= -4 && tz <= 22) {
            BlockState w = wing(ax, y, tz, tx > 0);
            if (w != null) return w;
        }
        // the court: its censer, its stone lanterns, its beds of yellow flowers
        if (tx == 0 && tz == 6) {
            if (y == 0) return s(Blocks.STONE_BRICKS);
            if (y == 1) return s(Blocks.CAMPFIRE);
            if (y == 2) return s(Blocks.IRON_BARS);
        }
        if ((tx == 1 || tx == -1) && tz == 6 && y <= 1) return s(Blocks.STONE_BRICK_WALL);
        if (tx == 0 && (tz == 5 || tz == 7) && y <= 1) return s(Blocks.STONE_BRICK_WALL);
        if (ax == 4 && (tz == 24 || tz == 0) && y <= 2) return y == 2 ? s(Blocks.LANTERN) : s(Blocks.STONE_BRICK_WALL);
        if (flowerbed(tx, tz) && y <= 1) {
            int h = hash(tx, 0, tz, 12);
            if (h < 30) return s(Blocks.SUNFLOWER).setValue(DoublePlantBlock.HALF, y == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            if (y == 0) return h < 75 ? s(Blocks.DANDELION) : h < 85 ? s(Blocks.SHORT_GRASS) : s(Blocks.AIR);
        }
        return y <= CANOPY_H + 1 ? s(Blocks.AIR) : null;
    }

    /** The court's beds of yellow flowers, either side of the way up to the hall. */
    static boolean flowerbed(int tx, int tz) {
        int ax = Math.abs(tx);
        return ax >= 6 && ax <= 11 && tz >= 2 && tz <= 24 && !(tz >= 11 && tz <= 13);
    }

    @Nullable
    private static BlockState gatehouse(int tx, int y, int gz) {
        int ax = Math.abs(tx);
        if (y <= 4 && ax == 2 && Math.abs(gz) == 2) return s(Blocks.RED_TERRACOTTA);
        if (y <= 4 && ax == 5 && Math.abs(gz) <= 2) return s(Blocks.YELLOW_TERRACOTTA);
        if (y == 5 && ax <= 5 && Math.abs(gz) <= 2) return ax <= 1 && gz == 2 ? s(Blocks.GOLD_BLOCK) : s(Blocks.SPRUCE_PLANKS); // the plaque, gilded
        BlockState roof = hip(tx, y, gz, -7, 7, -3, 3, 6, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        if (roof != null) return roof;
        return eaveTips(tx, y, gz, -7, 7, -3, 3, 6, Blocks.DEEPSLATE_TILE_STAIRS);
    }

    @Nullable
    private static BlockState hall(int tx, int y, int tz) {
        int ax = Math.abs(tx);
        boolean onPlatform = ax <= HALL_HALF + 2 && tz >= HALL_N - 2 && tz <= HALL_S + 2;
        if (y == 0 && onPlatform) {
            if (tz == HALL_S + 2 && ax <= 3) return stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, StairsShape.STRAIGHT, false);
            return s(Blocks.STONE_BRICKS);
        }
        boolean inHall = ax <= HALL_HALF && tz >= HALL_N && tz <= HALL_S;
        if (y >= 1 && y <= 6 && inHall) {
            boolean wallX = ax == HALL_HALF, back = tz == HALL_N, front = tz == HALL_S;
            boolean column = (wallX || back || front) && (ax % 4 == 1 || ax == HALL_HALF) && ((front || back) || (tz - HALL_N) % 4 == 0);
            if (column) return log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y);
            if (front) {
                if (ax <= 4 && y <= 4) return s(Blocks.AIR); // the hall's doors stand open
                if (y == 3 || y == 4) return s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, Direction.SOUTH);
                return y >= 5 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS);
            }
            if (wallX || back) {
                if (y == 3 && !back && (tz - HALL_N) % 4 == 2) return s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true)
                        .setValue(TrapDoorBlock.FACING, tx > 0 ? Direction.EAST : Direction.WEST);
                return y >= 5 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.SPRUCE_PLANKS);
            }
            // within: the altar to the Three Pure Ones, candles, incense, the tea table
            if (tz >= HALL_N + 1 && tz <= HALL_N + 2 && ax <= 4) {
                if (y == 1) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
                if (y == 2) return ax == 0 ? s(Blocks.GOLD_BLOCK) : ax % 2 == 0 ? s(Blocks.CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 4)
                        .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true) : s(Blocks.AIR);
                if (y == 3 && ax == 0 && tz == HALL_N + 1) return s(Blocks.GOLD_BLOCK);
            }
            if (tz == HALL_N + 6 && ax <= 1) {
                if (y == 1) return s(Blocks.SPRUCE_FENCE);
                if (y == 2) return slab(Blocks.SPRUCE_SLAB, false);
            }
            if (tz == HALL_N + 6 && ax == 0 && y == 3) return s(Blocks.DECORATED_POT);
            if ((ax == 6) && (tz - HALL_N) % 6 == 3 && y == 1) return s(Blocks.RED_CARPET);
            if (y == 5 && ax == 6 && (tz - HALL_N) % 6 == 3) return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            return s(Blocks.AIR);
        }
        // the double eaves: a lower roof round the hall, an upper one over it
        BlockState lower = hip(tx, y, tz, -HALL_HALF - 3, HALL_HALF + 3, HALL_N - 3, HALL_S + 3, 7, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        if (lower != null && y <= 8) return lower;
        BlockState tip = eaveTips(tx, y, tz, -HALL_HALF - 3, HALL_HALF + 3, HALL_N - 3, HALL_S + 3, 7, Blocks.DEEPSLATE_TILE_STAIRS);
        if (tip != null) return tip;
        if (y >= 7 && y <= 10 && ax <= HALL_HALF - 2 && tz >= HALL_N + 2 && tz <= HALL_S - 2) {
            if (ax == HALL_HALF - 2 || tz == HALL_N + 2 || tz == HALL_S - 2) return s(Blocks.DARK_OAK_PLANKS);
            if (y == 7) return s(Blocks.SPRUCE_PLANKS);
        }
        BlockState upper = hip(tx, y, tz, -HALL_HALF, HALL_HALF, HALL_N, HALL_S, 11, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        if (upper != null) return upper;
        BlockState upperTip = eaveTips(tx, y, tz, -HALL_HALF, HALL_HALF, HALL_N, HALL_S, 11, Blocks.DEEPSLATE_TILE_STAIRS);
        if (upperTip != null) return upperTip;
        // gilded finials at the ridge's ends
        int ridgeY = 11 + (HALL_S - HALL_N) / 2;
        if (y == ridgeY + 1 && tz == (HALL_N + HALL_S) / 2 && (tx == -HALL_HALF + (HALL_S - HALL_N) / 2 || tx == HALL_HALF - (HALL_S - HALL_N) / 2)) return s(Blocks.GOLD_BLOCK);
        return null;
    }

    @Nullable
    private static BlockState wing(int ax, int y, int tz, boolean east) {
        int x0 = 16, x1 = TEMPLE_HALF - 1;
        boolean inWing = ax >= x0 && ax <= x1 && tz >= -2 && tz <= 20;
        if (y >= 0 && y <= 3 && inWing) {
            boolean edge = ax == x0 || ax == x1 || tz == -2 || tz == 20;
            if (!edge) return y == 0 && ax == x1 - 1 && tz % 5 == 0 ? s(Blocks.RED_CARPET) : s(Blocks.AIR);
            if ((ax == x0) && (tz - 1) % 5 == 0 && y <= 2) return s(Blocks.AIR); // cell doors on the court
            if ((ax == x0 || ax == x1) && (tz + 2) % 5 == 0 || (tz == -2 || tz == 20) && (ax == x0 || ax == x1)) return log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y);
            return y == 2 && ax == x0 ? s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, east ? Direction.WEST : Direction.EAST)
                    : s(Blocks.SPRUCE_PLANKS);
        }
        BlockState roof = hip(ax, y, tz, x0 - 1, x1 + 1, -3, 21, 4, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        if (roof != null && east) return roof;
        if (roof != null) return mirrorX(roof);
        return null;
    }

    /** A block turned to face the other way along x (for the west wing, built as the east's mirror). */
    private static BlockState mirrorX(BlockState st) {
        return st.mirror(net.minecraft.world.level.block.Mirror.FRONT_BACK);
    }
}
