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
    /** How far past the canopy's foot its anchor cables reach the ground. */
    public static final int ANCHOR_OUT = 7;
    /** How far from the temple's middle anything of the canopy (its cables and their anchors) reaches. */
    public static final int CANOPY_REACH_X = CANOPY_RX + ANCHOR_OUT + 3, CANOPY_REACH_Z = CANOPY_RZ + ANCHOR_OUT + 3;
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
        double temple = outside(lx, lz, TEMPLE_X - CANOPY_REACH_X, TEMPLE_X + CANOPY_REACH_X, TEMPLE_Z - CANOPY_REACH_Z, TEMPLE_Z + CANOPY_REACH_Z);
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
                || Math.abs(lx - TEMPLE_X) <= CANOPY_REACH_X && Math.abs(lz - TEMPLE_Z) <= CANOPY_REACH_Z;
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
        if (Math.hypot(lx - SPRING_X, lz - SPRING_Z) <= SPRING_LEVEL && !inCottage(lx, lz)) return spring(lx - SPRING_X, ly, lz - SPRING_Z);
        if (ly < -1) return null; // under the cottage's and the temple's floors: the ground the land was brought to (only the spring's pool goes deeper)
        if (inCottage(lx, lz)) return cottage(lx - COTTAGE_X, ly, lz - COTTAGE_Z);
        int tx = lx - TEMPLE_X, tz = lz - TEMPLE_Z;
        if (Math.abs(tx) <= CANOPY_REACH_X && Math.abs(tz) <= CANOPY_REACH_Z) {
            BlockState silk = canopy ? canopyAt(tx, ly, tz) : null;
            if (silk != null) return silk;
            if (Math.abs(tx) <= CANOPY_RX + 1 && Math.abs(tz) <= CANOPY_RZ + 1) return temple(tx, ly, tz);
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
            return r < 40 ? s(Blocks.COARSE_DIRT) : r < 70 ? s(Blocks.DIRT_PATH) : r < 85 ? s(Blocks.PACKED_MUD) : s(Blocks.MUD_BRICKS);
        }
        if (!inside) {
            // the yard: a well-sweep's trough, a woodpile, a fence of withies along its sides
            if (y == 0 && (ax == YARD_HALF - 1) && z > COTTAGE_FRONT + 2 && z < COTTAGE_Z + YARD_S - 1) return s(Blocks.SPRUCE_FENCE);
            if (cx == 10 && z >= 60 && z <= 62 && y <= 1) return log(Blocks.SPRUCE_LOG, Direction.Axis.Z);
            if (cx == 11 && z >= 60 && z <= 61 && y == 0) return log(Blocks.SPRUCE_LOG, Direction.Axis.Z);
            if (cx == -10 && z == 62 && y == 0) return s(Blocks.WATER_CAULDRON);
            if (cx == -11 && z == 62 && y == 0) return s(Blocks.BARREL);
            if (cx == -10 && z == 63 && y == 0) return s(Blocks.COMPOSTER);
            if (y >= 0 && y <= 8 && hip(cx, y, z, -COTTAGE_HALF - 2, COTTAGE_HALF + 2, COTTAGE_BACK - 1, COTTAGE_FRONT + 2, 4, Blocks.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_MOSAIC) != null) {
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
        BlockState roof = hip(cx, y, z, -COTTAGE_HALF - 2, COTTAGE_HALF + 2, COTTAGE_BACK - 1, COTTAGE_FRONT + 2, 4, Blocks.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_MOSAIC);
        if (roof != null) return thatch(cx, y, z);
        if (y == 4 && ax <= COTTAGE_HALF && z >= COTTAGE_BACK && z <= COTTAGE_FRONT) {
            if (ax == COTTAGE_HALF || z == COTTAGE_BACK || z == COTTAGE_FRONT) return log(Blocks.DARK_OAK_LOG, ax == COTTAGE_HALF ? Direction.Axis.Z : Direction.Axis.X);
            if (cx == 0) return log(Blocks.DARK_OAK_LOG, Direction.Axis.Z); // the ridge beam
            return s(Blocks.AIR);
        }
        return y <= 11 ? s(Blocks.AIR) : null;
    }

    /** Thatch: woven bamboo-straw in courses, closing at its ridge. */
    private static BlockState thatch(int x, int y, int z) {
        BlockState roof = hip(x, y, z, -COTTAGE_HALF - 2, COTTAGE_HALF + 2, COTTAGE_BACK - 1, COTTAGE_FRONT + 2, 4, Blocks.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_MOSAIC);
        return roof == null ? s(Blocks.AIR) : roof;
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

    /** The canopy's radii, the spokes of its web from the knot at its crown; every third is held down by a cable. */
    static final int SPOKES = 24, ANCHOR_EVERY = 3;
    /** The polar angle (from the crown) of the canopy's hub, of the clear ring round it, and of its rim at an anchor. */
    static final double HUB = 0.085, FREE = 0.2, RIM = 1.03;
    /** How far the rim sags between two anchors, and the swags under it. */
    static final double RIM_SAG = 0.10, SWAG = 0.17, SWAG_SAG = 0.16;
    /** How far apart the rings of the web are, over the shell (blocks). */
    static final double RING_GAP = 3.4;

    /**
     * What the sisters' canopy puts at temple-local (tx, ly, tz), or null where it puts nothing. Its
     * shell ({@link #inCanopy}) is whole, gauze in every cell of it (or, at the knot of its crown,
     * sealing silk), so none passes it; but it is a web: through the gauze run cords of silk
     * ({@link SilkVeilBlock}), radii from the knot at its crown, rings round them, a heavy rim
     * sagging between the cables that hold it, swags and drapes hanging from that. Past it, from
     * every third radius, a cable runs down and splays into three tethers, each pinned to the ground
     * by a peg of sealing silk, cobwebs tufted round.
     */
    @Nullable
    public static BlockState canopyAt(int tx, int ly, int tz) {
        if (ly < 0 || ly > CANOPY_H) return null;
        int c = Web.cell(tx, ly, tz);
        if (c == 0) return inCanopy(tx, ly, tz) ? SpiderCaveRegistry.SILK_VEIL.get().defaultBlockState() : null;
        if ((c & Web.PEG) != 0) return SpiderCaveRegistry.SILK_SEAL.get().defaultBlockState();
        boolean thick = (c & Web.THICK) != 0;
        if ((c & Web.BARE) == 0) return SpiderCaveRegistry.SILK_VEIL.get().with(c & Web.FACES, c >> 6 & 0xFFF, thick);
        // out in the air: a cable in by one way and on by another; where it splits, a knot of web
        SilkCableBlock.Way from = SilkCableBlock.Way.NONE, to = SilkCableBlock.Way.NONE;
        int ways = 0;
        for (int i = 0; i < Web.WAYS.length; i++) {
            if ((c & 1 << i) == 0) continue;
            ways++;
            if (from == SilkCableBlock.Way.NONE) from = Web.WAYS[i];
            else to = Web.WAYS[i];
        }
        if (ways > 2 || (c & Web.TUFT) != 0) return s(Blocks.COBWEB);
        return SpiderCaveRegistry.SILK_CABLE.get().with(from, to, thick);
    }

    /** Whether the shell's cell at (tx, ly, tz) carries one of the web's strands (else it is bare gauze). */
    public static boolean strand(int tx, int ly, int tz) {
        int c = Web.cell(tx, ly, tz);
        return (c & (Web.FACES | Web.PEG)) != 0 && inCanopy(tx, ly, tz);
    }

    /** The canopy's half-width (blocks) out along azimuth {@code theta} at its foot. */
    private static double footRadius(double theta) {
        double c = Math.cos(theta) / CANOPY_RX, s = Math.sin(theta) / CANOPY_RZ;
        return 1 / Math.sqrt(c * c + s * s);
    }

    /** Where between two neighbouring radii {@code theta} lies, 0 at one to 1 at the next, every {@code step}. */
    private static double between(double theta, double step) {
        double r = theta / step;
        return r - Math.floor(r);
    }

    /** The rim's polar angle along azimuth {@code theta}: highest at the anchors, sagging between. */
    static double rimAt(double theta) {
        return RIM + RIM_SAG * Math.sin(Math.PI * between(theta, ANCHOR_EVERY * 2 * Math.PI / SPOKES));
    }

    /** The swag's polar angle along azimuth {@code theta}: looping down under the rim from anchor to anchor. */
    static double swagAt(double theta) {
        return rimAt(theta) + SWAG + SWAG_SAG * Math.sin(Math.PI * between(theta, ANCHOR_EVERY * 2 * Math.PI / SPOKES));
    }

    /**
     * The canopy's web, worked out once: each cell it touches, temple-local, and what is there: the
     * ways a cord runs out ({@link #WAYS}: through its faces, or for a cable across its edges), whether that
     * cord is heavy, whether it is out past the gauze; or a peg of sealing silk, or a tuft of cobweb.
     * Every strand is traced as a curve over the shell and laid cell to cell through their faces, so
     * it runs on unbroken, a line and not a stair of blocks.
     */
    static final class Web {
        /** The ways a cord leaves a cell, by bit: through its faces (the first six, by {@link Direction#get3DDataValue}), then across its edges. */
        static final SilkCableBlock.Way[] WAYS = java.util.Arrays.copyOfRange(SilkCableBlock.Way.values(), 1, 19);
        static final int FACES = 63, WAYS_MASK = (1 << 18) - 1, THICK = 1 << 18, BARE = 1 << 19, PEG = 1 << 20, TUFT = 1 << 21, SET = 1 << 22;
        private static final int[][] ORDERS = {{1, 0, 2}, {1, 2, 0}, {0, 1, 2}, {2, 1, 0}, {0, 2, 1}, {2, 0, 1}};
        private static final java.util.Map<Long, Integer> CELLS = new java.util.HashMap<>();

        static {
            build();
        }

        private Web() {}

        static int cell(int x, int y, int z) {
            Integer c = CELLS.get(key(x, y, z));
            return c == null ? 0 : c;
        }

        private static long key(int x, int y, int z) {
            return ((long) (x & 0xFFFFF) << 40) | ((long) (y & 0xFFFFF) << 20) | (z & 0xFFFFF);
        }

        private static void put(int x, int y, int z, int bits) {
            if (y < 0 || y > CANOPY_H) return;
            if (!inCanopy(x, y, z)) {
                if (insideCanopy(x, y, z)) return; // nothing of the web inside it
                bits |= BARE;
            }
            CELLS.merge(key(x, y, z), bits | SET, (a, b) -> a | b);
        }

        /** A point on the shell's surface: polar angle {@code phi} from the crown, azimuth {@code theta}. */
        private static double[] surface(double phi, double theta) {
            return new double[]{CANOPY_RX * Math.sin(phi) * Math.cos(theta), CANOPY_H * Math.cos(phi), CANOPY_RZ * Math.sin(phi) * Math.sin(theta)};
        }

        private static double polar(int x, int y, int z) {
            return Math.atan2(Math.hypot(x / (double) CANOPY_RX, z / (double) CANOPY_RZ), y / (double) CANOPY_H);
        }

        /** The shell's cell nearest point p, or null if none is near. */
        private static int[] shellCell(double[] p) {
            int bx = (int) Math.round(p[0]), by = (int) Math.round(p[1]), bz = (int) Math.round(p[2]);
            int[] best = null;
            double bd = Double.MAX_VALUE;
            for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
                int x = bx + i, y = by + j, z = bz + k;
                if (y < 0 || !inCanopy(x, y, z)) continue;
                double d = (x - p[0]) * (x - p[0]) + (y - p[1]) * (y - p[1]) + (z - p[2]) * (z - p[2]);
                if (d < bd) {
                    bd = d;
                    best = new int[]{x, y, z};
                }
            }
            return best;
        }

        /** Lays a strand through the cells {@code curve} passes (over the shell, or free in the air), at {@code samples} points along it. */
        private static void trace(java.util.function.DoubleFunction<double[]> curve, int samples, boolean onShell, int bits) {
            java.util.List<int[]> run = new java.util.ArrayList<>();
            for (int i = 0; i <= samples; i++) {
                double[] p = curve.apply(i / (double) samples);
                if (p == null) {
                    lay(run, onShell, bits);
                    run.clear();
                    continue;
                }
                int[] c;
                if (onShell) c = shellCell(p);
                else {
                    c = new int[]{(int) Math.round(p[0]), (int) Math.round(p[1]), (int) Math.round(p[2])};
                    if (c[1] < 0 || insideCanopy(c[0], c[1], c[2]) && !inCanopy(c[0], c[1], c[2])) c = null;
                }
                if (c == null) continue;
                if (!run.isEmpty()) {
                    int[] last = run.get(run.size() - 1);
                    if (last[0] == c[0] && last[1] == c[1] && last[2] == c[2]) continue;
                    run.addAll(between(last, c, onShell));
                }
                run.add(c);
            }
            lay(run, onShell, bits);
        }

        /** The cells between a and b (neither), stepped face to face, over the shell where it can. */
        private static java.util.List<int[]> between(int[] a, int[] b, boolean onShell) {
            int[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            int[] order = ORDERS[0];
            for (int[] o : ORDERS) {
                if (fits(a, d, o, onShell)) {
                    order = o;
                    break;
                }
            }
            java.util.List<int[]> out = new java.util.ArrayList<>();
            int[] at = a.clone();
            for (int axis : order) {
                while (at[axis] != b[axis]) {
                    at[axis] += Integer.signum(b[axis] - at[axis]);
                    if (at[0] != b[0] || at[1] != b[1] || at[2] != b[2]) out.add(at.clone());
                }
            }
            return out;
        }

        /**
         * Lays a run of cells, each a face from the next, as one cord: where it steps one way and then
         * another (a stair), it runs across the edge between instead, the corner cell left out, so a
         * slant line runs on as a slant line.
         */
        private static void lay(java.util.List<int[]> run, boolean onShell, int bits) {
            if (run.isEmpty()) return;
            put(run.get(0)[0], run.get(0)[1], run.get(0)[2], bits);
            int i = 0;
            while (i + 1 < run.size()) {
                int[] a = run.get(i);
                if (i + 2 < run.size()) {
                    int[] c = run.get(i + 2);
                    int dx = c[0] - a[0], dy = c[1] - a[1], dz = c[2] - a[2];
                    int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                    if (axes == 2 && Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1 && join(a, c, onShell, bits)) {
                        i += 2;
                        continue;
                    }
                }
                join(a, run.get(i + 1), onShell, bits);
                i++;
            }
        }

        /** Joins neighbouring cells a and b (across a face, or an edge if both can take it): whether it could. */
        private static boolean join(int[] a, int[] b, boolean onShell, int bits) {
            int dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            int w = SilkCableBlock.Way.toward(dx, dy, dz).ordinal() - 1, back = SilkCableBlock.Way.toward(-dx, -dy, -dz).ordinal() - 1;
            if (w < 0) return false;
            if (w >= 6) {
                // across an edge: in the gauze, only where the cell has no other slant through it but the one this goes on (a cable takes any two ways)
                if (!slantFits(a, w) || !slantFits(b, back)) return false;
            }
            put(a[0], a[1], a[2], bits | 1 << w);
            put(b[0], b[1], b[2], bits | 1 << back);
            return true;
        }

        /** Whether cell a can take a slant cord out across edge {@code w}: free of slants, or holding only the one straight on from it. */
        private static boolean slantFits(int[] a, int w) {
            int edges = cell(a[0], a[1], a[2]) >> 6 & 0xFFF;
            if (!inCanopy(a[0], a[1], a[2])) return true; // a cable takes any two ways
            if (edges == 0 || edges == 1 << (w - 6)) return true;
            SilkCableBlock.Way way = WAYS[w];
            int opp = SilkCableBlock.Way.toward(-way.dx, -way.dy, -way.dz).ordinal() - 1;
            return edges == 1 << (opp - 6);
        }

        /** Whether the cells between a and a + d, stepped axis by axis in {@code order}, all lie where the strand may run. */
        private static boolean fits(int[] a, int[] d, int[] order, boolean onShell) {
            int[] at = a.clone();
            for (int axis : order) {
                for (int n = 0; n < Math.abs(d[axis]); n++) {
                    at[axis] += Integer.signum(d[axis]);
                    boolean ok = onShell ? inCanopy(at[0], at[1], at[2]) : !insideCanopy(at[0], at[1], at[2]) || inCanopy(at[0], at[1], at[2]);
                    if (!ok) return false;
                }
            }
            return true;
        }

        private static void build() {
            double step = 2 * Math.PI / SPOKES;
            // the knot at the crown
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) for (int y = CANOPY_H - 3; y <= CANOPY_H; y++)
                if (inCanopy(x, y, z) && polar(x, y, z) < HUB) put(x, y, z, PEG);
            // the radii: those the cables hold heavy, and on down to the ground; the rest on down under the rim as drapes
            for (int k = 0; k < SPOKES; k++) {
                double theta = k * step;
                boolean anchor = k % ANCHOR_EVERY == 0;
                int h = hash(k, 0, 0, 40);
                double end = anchor || h < 30 ? Math.PI / 2 : Math.min(Math.PI / 2, swagAt(theta) + 0.04 + (h % 9) * 0.04);
                double from = HUB * 0.7;
                trace(u -> surface(from + (end - from) * u, theta), 500, true, anchor ? THICK : 0);
            }
            // the rim, heavy; the swags under it
            trace(u -> surface(rimAt(u * 2 * Math.PI), u * 2 * Math.PI), 4000, true, THICK);
            trace(u -> {
                double theta = u * 2 * Math.PI, phi = swagAt(theta);
                return phi < Math.PI / 2 ? surface(phi, theta) : null;
            }, 4000, true, 0);
            // the rings round the knot, level all round (so each runs on as one straight line, seen from the side), out to the rim
            double arc = 0, avg = (CANOPY_RX + CANOPY_RZ) / 2.0;
            int lastY = Integer.MAX_VALUE;
            for (double phi = FREE; phi < RIM - 0.04; phi += 0.002) {
                arc += Math.hypot(avg * Math.cos(phi), CANOPY_H * Math.sin(phi)) * 0.002;
                if (arc < RING_GAP) continue;
                int y = (int) Math.round(CANOPY_H * Math.cos(phi));
                if (y >= lastY) continue;
                arc = 0;
                lastY = y;
                double ring = Math.acos(y / (double) CANOPY_H);
                trace(u -> surface(ring, u * 2 * Math.PI), 4000, true, 0);
            }
            // the cables: from the rim at each anchor out and down, splaying near the ground into three tethers, each pinned by a peg
            for (int k = 0; k < SPOKES; k += ANCHOR_EVERY) {
                double theta = k * step, c = Math.cos(theta), sn = Math.sin(theta);
                double[] top = surface(rimAt(theta), theta);
                double reach = footRadius(theta) + ANCHOR_OUT;
                double[] split = {c * (reach - 2.0), 3.0, sn * (reach - 2.0)};
                trace(u -> new double[]{top[0] + (split[0] - top[0]) * u, top[1] + (split[1] - top[1]) * u - 1.4 * 4 * u * (1 - u),
                        top[2] + (split[2] - top[2]) * u}, 600, false, THICK);
                double[][] pins = {{reach + 0.3, -2.6}, {reach + 1.6, 0}, {reach + 0.3, 2.6}};
                for (double[] pin : pins) {
                    int px = (int) Math.round(c * pin[0] - sn * pin[1]), pz = (int) Math.round(sn * pin[0] + c * pin[1]);
                    put(px, 0, pz, PEG);
                    // the knot of web where the cable splits
                    put((int) Math.round(split[0]), (int) Math.round(split[1]), (int) Math.round(split[2]), TUFT);
                    double[] end = {px, 0.3, pz};
                    trace(u -> new double[]{split[0] + (end[0] - split[0]) * u, split[1] + (end[1] - split[1]) * u, split[2] + (end[2] - split[2]) * u},
                            200, false, 0);
                    // a tuft or two of cobweb round the peg
                    for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int qx = px + n[0], qz = pz + n[1];
                        if (hash(qx, 0, qz, 43) < 35 && !CELLS.containsKey(key(qx, 0, qz)) && !insideCanopy(qx, 0, qz)) put(qx, 0, qz, TUFT);
                    }
                }
            }
        }
    }

    /** The main hall: its half-width, its back and front (from the temple's middle). */
    static final int HALL_HALF = 13, HALL_N = -27, HALL_S = -9;

    @Nullable
    static BlockState temple(int tx, int y, int tz) {
        int ax = Math.abs(tx);
        boolean walled = ax <= TEMPLE_HALF && tz >= TEMPLE_N && tz <= TEMPLE_S;
        if (y == -1) {
            if (!walled) {
                // the way up to the gate; trodden gravel round the walls; then the valley's grass, worn
                double off = outside(tx, tz, -TEMPLE_HALF, TEMPLE_HALF, TEMPLE_N, TEMPLE_S);
                int r = hash(tx, y, tz, 10);
                if (Math.abs(tx) <= 2 && tz > TEMPLE_S) return r < 75 ? s(Blocks.DIRT_PATH) : s(Blocks.COARSE_DIRT);
                if (off <= 2 + r % 2) return r < 55 ? s(Blocks.GRAVEL) : r < 80 ? s(Blocks.COBBLESTONE) : s(Blocks.MOSSY_COBBLESTONE);
                return r < 55 ? s(Blocks.GRASS_BLOCK) : r < 72 ? s(Blocks.COARSE_DIRT) : r < 87 ? s(Blocks.PODZOL) : s(Blocks.MOSS_BLOCK);
            }
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
