package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.ridge.CaveSites;
import com.jttw.engine.world.ridge.WhiteBoneCave;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import static com.jttw.engine.world.ridge.WhiteBoneCave.*;

/**
 * Lays the White Bone Cave ({@link WhiteBoneCave}) chunk by chunk, after the massif has raised its
 * mountain and cut its forecourt and road ({@link BoneMassifFeature}): the halls, stairs, seals,
 * well and throne room inside the mountain, each floor faced in its own stone and lit and strewn
 * with its own dead; the great door and the skull carved over it, the rib arches and braziers of
 * the forecourt; the bone arch and watchtowers along the road; the outposts round the mountain.
 * Everything it puts follows from the place alone, so a chunk made later never undoes another's work.
 *
 * <p>Inside, rock the cave does not open is kept where it is solid and filled where it is not, so
 * no vanilla cave, water or lava breaks into it. Runs right after the massif (raw generation).
 */
public class WhiteBoneCaveFeature extends Feature<NoneFeatureConfiguration> {

    public WhiteBoneCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** What laying it has cost, all told: nanoseconds, and the chunks it laid anything in (the worldgen bench reads them). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong(),
            CHUNKS = new java.util.concurrent.atomic.AtomicLong();

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Site site = CaveSites.site(level);
        if (site == null) return false;
        long start = System.nanoTime();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        boolean any = build(level, WhiteBoneCave.of(level.getSeed()), site, x0, z0);
        NANOS.addAndGet(System.nanoTime() - start);
        if (any) CHUNKS.incrementAndGet();
        return any;
    }

    /** Lays the cave's share of the chunk starting at (x0, z0) (the gallery and the tests lay it where they like). Returns whether any. */
    public static boolean build(WorldGenLevel level, WhiteBoneCave cave, Site site, int x0, int z0) {
        boolean any = inside(level, cave, site, x0, z0);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[][] outposts = cave.outposts();
        int[] pads = null;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz, lx = x - site.cx(), lz = z - site.cz();
            if (Math.abs(lx) <= COURT_HALF + 1 && lz >= DOOR_Z && lz <= COURT_Z1 + 1) any |= forecourt(level, site, lx, lz, p);
            if (onRoadLength(lz) && Math.abs(lx - roadX(lz)) <= TOWER_OUT + 2) any |= road(level, site, lx, lz, p);
            if (Math.hypot(lx, lz) <= MOUNTAIN_R) any |= tusks(level, site, lx, lz, x, z, p);
            for (int i = 0; i < outposts.length; i++) {
                if (Math.hypot(lx - outposts[i][0], lz - outposts[i][1]) > OUTPOST_R + 1) continue;
                if (pads == null) pads = CaveSites.pads(level, site);
                any |= outpost(level, site, lx - outposts[i][0], lz - outposts[i][1], pads[i], x, z, p);
            }
        }
        return any;
    }

    // ── Inside ──────────────────────────────────────────────────────────

    /** Lays only the cave's inside in the chunk at (x0, z0), none of what stands outside it (the gallery lays it in the air). */
    public static boolean buildInside(WorldGenLevel level, WhiteBoneCave cave, Site site, int x0, int z0) {
        return inside(level, cave, site, x0, z0);
    }

    private static boolean inside(WorldGenLevel level, WhiteBoneCave cave, Site site, int x0, int z0) {
        int lx0 = x0 - site.cx(), lz0 = z0 - site.cz();
        if (lx0 + 16 < -REACH || lx0 - 1 > REACH || lz0 + 16 < -REACH || lz0 - 1 > REACH) return false;
        byte[][][] codes = new byte[18][18][H];
        byte[][][] bands = new byte[18][18][];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            cave.column(lx0 + i - 1, lz0 + j - 1, codes[i][j]);
            bands[i][j] = bands(cave, lx0 + i - 1, lz0 + j - 1);
        }
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz, lx = lx0 + dx, lz = lz0 + dz;
            byte[] col = codes[dx + 1][dz + 1];
            if (col[0] == UNTOUCHED && col[H - 1] == UNTOUCHED) continue;
            for (int k = 0; k < H; k++) {
                byte code = col[k];
                if (code == UNTOUCHED) continue;
                int ly = Y_MIN + k, y = site.y(ly);
                if (y < minY + 1 || y > maxY) continue;
                p.set(x, y, z);
                if (code == ROCK) {
                    int face = face(codes, dx + 1, dz + 1, k);
                    if (face != 0) {
                        level.setBlock(p, skin(band(bands, codes, dx + 1, dz + 1, k, face), face, lx, ly, lz, x, y, z), Block.UPDATE_CLIENTS);
                    } else {
                        BlockState here = level.getBlockState(p);
                        if (!solid(here)) level.setBlock(p, BoneMassifFeature.innerStone(y, x, z), Block.UPDATE_CLIENTS);
                    }
                    continue;
                }
                level.setBlock(p, state(code, bands[dx + 1][dz + 1][k], lx, ly, lz), Block.UPDATE_CLIENTS);
            }
            decorate(level, cave, site, codes[dx + 1][dz + 1], codes, dx + 1, dz + 1, lx, lz, x, z, p);
            if (lz == THRONE_N - 1 && Math.abs(lx) <= 8) {
                // over the throne, a skull carved in the wall, looking down the hall
                for (int r = 0; r < SKULL.length; r++) {
                    char ch = SKULL[r].charAt(lx + 8);
                    int y = site.y(THRONE_Y + THRONE_H - 2 - r);
                    if (ch == 'X') put(level, p, x, y, z, pillar(Blocks.BONE_BLOCK, Direction.Axis.Y));
                    else if (ch == 'o') put(level, p, x, y, z, r <= 7 ? s(Blocks.CRYING_OBSIDIAN) : s(Blocks.BLACKSTONE));
                }
            }
        }
        return true;
    }

    /** Which floor's stone each local height of a column is faced with: the hall open there, else the floor nearest. */
    private static byte[] bands(WhiteBoneCave cave, int lx, int lz) {
        byte[] out = new byte[H];
        for (int k = 0; k < H; k++) {
            int ly = Y_MIN + k;
            out[k] = (byte) (ly < floorY(FLOORS - 1) - 9 ? FLOORS : Mth.clamp(Math.round(-ly / (float) STEP), 0, FLOORS - 1));
        }
        if (Math.abs(lx) > REACH || Math.abs(lz) > REACH) return out;
        for (int f = 0; f <= FLOORS; f++) {
            int c = cave.plan(f, lx, lz);
            if (!WhiteBoneCave.open(c)) continue;
            int from = floorY(f) + fo(c) - 1, to = floorY(f) + fo(c) + height(c);
            for (int ly = Math.max(Y_MIN, from); ly <= Math.min(Y_MAX, to); ly++) out[ly - Y_MIN] = (byte) f;
        }
        return out;
    }

    /** The band of the open place a face of rock looks onto (over it, under it, or beside it). */
    private static int band(byte[][][] bands, byte[][][] codes, int i, int j, int k, int face) {
        if (face == 1) return bands[i][j][k + 1];
        if (face == 2) return bands[i][j][k - 1];
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] o : d) if (opening(codes[i + o[0]][j + o[1]][k])) return bands[i + o[0]][j + o[1]][k];
        return bands[i][j][k];
    }

    private static boolean opening(byte code) {
        return code == AIR || code == LADDER || code == SOUL_FIRE || code == BARS || code == GRATE;
    }

    /** Which way a rock block faces something open: 1 a floor (open over it), 2 a ceiling (open under it), 3 a wall; 0 none. */
    private static int face(byte[][][] codes, int i, int j, int k) {
        if (k + 1 < H && opening(codes[i][j][k + 1])) return 1;
        if (k > 0 && opening(codes[i][j][k - 1])) return 2;
        if (opening(codes[i + 1][j][k]) || opening(codes[i - 1][j][k]) || opening(codes[i][j + 1][k]) || opening(codes[i][j - 1][k])) return 3;
        return 0;
    }

    private static boolean solid(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && s.blocksMotion();
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState pillar(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState bone(Direction.Axis axis) {
        return pillar(ModBlocks.OSSIFIED_BONE.get(), axis);
    }

    /** The face of a floor's hall: its floor (1), its ceiling (2), its walls (3), in that floor's own stone. */
    static BlockState skin(int band, int face, int lx, int ly, int lz, int x, int y, int z) {
        int roll = hash(x * 3 + y, z, 11 + band);
        switch (band) {
            case 0 -> { // the Ossuary Gate: walls of bones stacked, skulls set in niches; a floor of bleached flags
                if (face == 1) {
                    int patch = hash(Math.floorDiv(x, 3), Math.floorDiv(z, 3), 21);
                    if (Math.abs(lx) <= 3) return Math.abs(lx) == 3 ? bone(Direction.Axis.Z) : s(ModBlocks.GRAVE_TUFF.get()); // the way down the nave
                    return patch < 30 ? s(ModBlocks.GRAVE_TUFF.get()) : patch < 38 ? bone(Direction.Axis.X) : s(ModBlocks.BLEACHED_STONE.get());
                }
                if (face == 2) return roll < 15 ? bone(Direction.Axis.X) : s(ModBlocks.GRAVE_TUFF.get());
                if (roll < 8 && Math.floorMod(ly, 16) >= 1 && Math.floorMod(ly, 16) <= 4) return skull(x, z);
                return roll < 55 ? pillar(Blocks.BONE_BLOCK, Direction.Axis.Z) : roll < 85 ? bone(Direction.Axis.Y) : s(ModBlocks.BLEACHED_STONE.get());
            }
            case 1 -> { // the Corpse Halls: cold deepslate
                if (face == 1) return roll < 80 ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.DEEPSLATE_TILES);
                if (face == 2) return s(Blocks.DEEPSLATE_TILES);
                return roll < 50 ? s(Blocks.DEEPSLATE_TILES) : roll < 75 ? s(Blocks.CRACKED_DEEPSLATE_TILES) : s(Blocks.DEEPSLATE_BRICKS);
            }
            case 2 -> { // the Bone Forge: blackstone, gilded here and there, soot overhead
                if (face == 1) return roll < 60 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.BLACKSTONE);
                if (face == 2) return s(Blocks.SMOOTH_BASALT);
                return roll < 6 ? s(Blocks.GILDED_BLACKSTONE) : roll < 60 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            }
            case 3 -> { // the Catacombs: tuff, burial niches with their skulls
                if (face == 1) return roll < 55 ? s(Blocks.POLISHED_TUFF) : s(Blocks.TUFF_BRICKS);
                if (face == 2) return s(Blocks.TUFF_BRICKS);
                if (roll < 14 && Math.floorMod(ly, 16) >= 1 && Math.floorMod(ly, 16) <= 3) return skull(x, z);
                return roll < 60 ? s(Blocks.TUFF_BRICKS) : roll < 80 ? s(Blocks.CHISELED_TUFF) : s(Blocks.CHISELED_TUFF_BRICKS);
            }
            case 4 -> { // the Hall of Borrowed Faces: white plaster, dark wood, mirrors of smoked glass
                if (face == 1) return s(Blocks.DARK_OAK_PLANKS);
                if (face == 2) return s(Blocks.DARK_OAK_PLANKS);
                int along = Math.floorMod(x + z, 10), up = Math.floorMod(ly, 16);
                if (along == 0) return pillar(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
                if (along >= 4 && along <= 6 && up >= 2 && up <= 5) return s(Blocks.TINTED_GLASS);
                return up == 7 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.CALCITE);
            }
            case 5 -> { // the Charnel Pit: bone-grey stone over a pit of the dead
                if (face == 1) return ly < floorY(5) - 1 ? (roll < 60 ? s(Blocks.SOUL_SOIL) : s(Blocks.BONE_BLOCK)) : roll < 50 ? s(ModBlocks.BONE_ANDESITE.get()) : s(ModBlocks.GRAVE_TUFF.get());
                if (face == 2) return s(ModBlocks.BONE_ANDESITE.get());
                return roll < 45 ? s(ModBlocks.BONE_ANDESITE.get()) : roll < 75 ? s(ModBlocks.GRAVE_TUFF.get()) : s(Blocks.POLISHED_ANDESITE);
            }
            case 6 -> { // the Soul-Fire Crypt: basalt and soul soil
                if (face == 1) return roll < 50 ? s(Blocks.SOUL_SOIL) : s(Blocks.SMOOTH_BASALT);
                if (face == 2) return s(Blocks.BLACKSTONE);
                return roll < 60 ? pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y) : s(Blocks.POLISHED_BLACKSTONE);
            }
            case 7 -> { // the Bone Bridge: raw deepslate round the chasm
                if (face == 1) return ly < floorY(7) - 1 ? (roll < 50 ? s(ModBlocks.BONE_GRAVEL.get()) : s(Blocks.SOUL_SOIL)) : s(Blocks.DEEPSLATE_TILES);
                if (face == 2) return s(Blocks.DEEPSLATE_TILES);
                return roll < 50 ? s(Blocks.COBBLED_DEEPSLATE) : roll < 85 ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.CRACKED_DEEPSLATE_BRICKS);
            }
            default -> { // the Throne of Bones: black floor, a white way to the dais, walls of bone
                if (face == 1) {
                    if (Math.abs(lx) <= 2 && lz > THRONE_N + 10) return Math.abs(lx) == 2 ? bone(Direction.Axis.Z) : s(Blocks.SMOOTH_QUARTZ);
                    if (Math.abs(lx) <= 12 && lz <= THRONE_N + 9) return roll < 70 ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.CHISELED_QUARTZ_BLOCK);
                    return (Math.floorMod(lx, 4) == 0 || Math.floorMod(lz, 4) == 0) ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.POLISHED_BLACKSTONE);
                }
                if (face == 2) return s(ModBlocks.GRAVE_TUFF.get());
                // walls of bone: ribs standing every four blocks, bones stacked between, bands of blackstone, skulls set in them
                int along = Math.floorMod(lx + lz, 4), up = Math.floorMod(ly - THRONE_Y, 6);
                if (along == 0) return bone(Direction.Axis.Y);
                if (up == 0) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
                if (up == 3 && along == 2) return skull(x, z);
                return pillar(Blocks.BONE_BLOCK, Direction.Axis.X);
            }
        }
    }

    private static BlockState skull(int x, int z) {
        return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, hash(x, z, 91) % 16);
    }

    /** What stands at a code other than rock, in its floor's own way. */
    static BlockState state(byte code, int band, int lx, int ly, int lz) {
        return switch (code) {
            case AIR -> Blocks.CAVE_AIR.defaultBlockState();
            case PILLAR -> switch (band) {
                case 1 -> s(Blocks.DEEPSLATE_BRICKS);
                case 2 -> Math.abs(lx) <= 2 && Math.abs(lz) <= 2
                        ? (Math.abs(lx) == 2 || Math.abs(lz) == 2 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.MAGMA_BLOCK))
                        : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
                case 3 -> s(Blocks.TUFF_BRICKS);
                case 4 -> pillar(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
                case 6 -> pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y);
                default -> Math.floorMod(ly, 4) == 0 ? pillar(Blocks.BONE_BLOCK, Direction.Axis.Y) : bone(Direction.Axis.Y);
            };
            case STEP_W -> stair(band).setValue(StairBlock.FACING, Direction.WEST);
            case LAVA -> s(Blocks.LAVA);
            case SOUL_FIRE -> s(Blocks.SOUL_FIRE);
            case SOUL_SOIL -> s(Blocks.SOUL_SOIL);
            case SOUL_SAND -> s(Blocks.SOUL_SAND);
            case MAGMA -> s(Blocks.MAGMA_BLOCK);
            case BARS -> Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
            case GRATE -> s(Blocks.IRON_BARS);
            case LADDER -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
            case LEAF -> leaf(lx, ly);
            case FRAME -> ly >= 0 && lz >= DOOR_Z ? (Math.floorMod(ly, 4) == 3 ? s(Blocks.GILDED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS))
                    : bone(ly == floorY(Math.round(-ly / (float) STEP)) + SEAL_H ? Direction.Axis.X : Direction.Axis.Y);
            case BONE -> band == 1 ? bone(Direction.Axis.Z) : bone(Direction.Axis.Y);
            case PILE -> pillar(Blocks.BONE_BLOCK, hash(lx, lz, ly) % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z);
            default -> s(Blocks.STONE);
        };
    }

    private static BlockState stair(int band) {
        return switch (band) {
            case 0, 1 -> s(Blocks.POLISHED_DEEPSLATE_STAIRS);
            case 2 -> s(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
            case 3 -> s(Blocks.TUFF_BRICK_STAIRS);
            case 4 -> s(Blocks.DARK_OAK_STAIRS);
            case 5 -> s(Blocks.STONE_BRICK_STAIRS);
            case 6 -> s(Blocks.POLISHED_BLACKSTONE_STAIRS);
            default -> s(Blocks.DEEPSLATE_TILE_STAIRS);
        };
    }

    /**
     * The great door's leaf at local (x, y) across it, shut: bone, banded with blackstone every five
     * blocks, studded with iron, an iron seam down the middle where the two leaves meet.
     */
    public static BlockState leaf(int x, int y) {
        int ax = Math.abs(x);
        if (ax == 0) return s(Blocks.IRON_BLOCK);
        if (Math.floorMod(y, 5) == 0) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        if (ax % 3 == 0 && Math.floorMod(y, 5) == 2) return s(Blocks.IRON_BLOCK);
        if (ax == DOOR_HALF) return bone(Direction.Axis.Y);
        return pillar(Blocks.BONE_BLOCK, Direction.Axis.Y);
    }

    // ── What stands in the halls ────────────────────────────────────────

    private static void put(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, BlockState s) {
        level.setBlock(p.set(x, y, z), s, Block.UPDATE_CLIENTS);
    }

    /** Whether local (x, z) is near a mouth of the hall (the seal's, the arrival's, the well's, the door's), where nothing is put. */
    private static boolean nearMouth(int lx, int lz) {
        if (Math.abs(lz) < HALL - 4) return false;
        return lx >= SEAL_X0 - 3 && lx <= SEAL_X1 + 3 || lx >= ARRIVE_X0 - 3 && lx <= ARRIVE_X1 + 3
                || lx >= SHAFT_X0 - 3 && lx <= SHAFT_X1 + 3 || Math.abs(lx) <= DOOR_HALF + 2;
    }

    private static void decorate(WorldGenLevel level, WhiteBoneCave cave, Site site, byte[] col, byte[][][] codes, int i, int j,
                                 int lx, int lz, int x, int z, BlockPos.MutableBlockPos p) {
        for (int k = 0; k <= FLOORS; k++) {
            int c = cave.plan(k, lx, lz);
            if (!WhiteBoneCave.open(c) || kind(c) == K_PILLAR) continue;
            int f = floorY(k) + fo(c), h = height(c);
            int top = f + h - 1;
            if (col[f - Y_MIN] != AIR || col[top - Y_MIN] != AIR) continue;
            int roll = hash(x, z, 300 + k), roll2 = hash(z, x, 400 + k);
            boolean wall = false;
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] o : d) if (codes[i + o[0]][j + o[1]][f - Y_MIN] == ROCK) wall = true;
            boolean clear = !nearMouth(lx, lz) && kind(c) == K_NONE;
            int fy = site.y(f), ty = site.y(top);
            // lanterns hung on chains from the ceiling, every nine blocks (few in the Hall of Borrowed Faces)
            boolean lamp = Math.floorMod(lx, 9) == 4 && Math.floorMod(lz, 9) == 4 && (k != 4 || Math.floorMod(lx + lz, 18) == 8);
            if (lamp && h >= 6 && col[top - Y_MIN - 2] == AIR) {
                int drop = Math.min(h - 5, k == FLOORS ? 8 : 3);
                for (int y = 0; y < drop; y++) put(level, p, x, ty - y, z, s(Blocks.CHAIN));
                put(level, p, x, ty - drop, z, (k == 2 ? Blocks.LANTERN : Blocks.SOUL_LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            if (!clear) {
                if (kind(c) == K_BIER && roll < 35 && col[f + 1 - Y_MIN] == AIR) {
                    put(level, p, x, fy + 1, z, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + roll % 3).setValue(CandleBlock.LIT, true));
                }
                if (kind(c) == K_PILE && roll < 30 && col[f + 2 - Y_MIN] == AIR && col[f + 1 - Y_MIN] == PILE) put(level, p, x, fy + 2, z, skull(x, z));
                continue;
            }
            switch (k) {
                case 0 -> {
                    if (wall && roll < 18) {
                        put(level, p, x, fy, z, pillar(Blocks.BONE_BLOCK, roll % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z));
                        if (roll < 8) put(level, p, x, fy + 1, z, skull(x, z));
                    } else if (roll < 1) put(level, p, x, fy, z, skull(x, z));
                    if (Math.floorMod(lz, 6) == 0 && h >= 12) put(level, p, x, ty, z, bone(Direction.Axis.X)); // ribs of the vault
                }
                case 1 -> {
                    if (roll2 < 3) put(level, p, x, ty, z, s(Blocks.COBWEB));
                    if (!wall && roll < 2) put(level, p, x, fy, z, skull(x, z));
                }
                case 2 -> {
                    double r = Math.sqrt(lx * lx + lz * lz);
                    if (r >= 4 && r < 8 && roll < 12) put(level, p, x, fy, z, (roll < 4 ? Blocks.ANVIL : roll < 8 ? Blocks.CHIPPED_ANVIL : Blocks.DAMAGED_ANVIL).defaultBlockState());
                    else if (wall && roll < 10) put(level, p, x, fy, z, (roll < 4 ? Blocks.BLAST_FURNACE : roll < 7 ? Blocks.SMITHING_TABLE : Blocks.LAVA_CAULDRON).defaultBlockState());
                    else if (roll2 < 4 && h >= 8) for (int y = 0; y < 4; y++) put(level, p, x, ty - y, z, s(Blocks.CHAIN));
                }
                case 3 -> {
                    if (roll2 < 10) put(level, p, x, ty, z, s(Blocks.COBWEB));
                    else if (roll < 2) put(level, p, x, fy, z, s(Blocks.COBWEB));
                    else if (roll < 6) put(level, p, x, fy, z, skull(x, z));
                    if (lamp(lx, lz, 8)) put(level, p, x, ty, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                }
                case 4 -> {
                    if (wall && roll < 6) put(level, p, x, fy, z, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + roll % 4).setValue(CandleBlock.LIT, true));
                    else if (wall && roll < 14) {
                        // her cast-off faces, as she left them: a pale shape against the wall, a skull for its head
                        put(level, p, x, fy, z, s(Blocks.WHITE_WOOL));
                        put(level, p, x, fy + 1, z, s(Blocks.PINK_WOOL));
                        put(level, p, x, fy + 2, z, skull(x, z));
                    }
                }
                case 5 -> {
                    if (f < floorY(5) && roll < 8) put(level, p, x, fy, z, skull(x, z));
                    else if (f == floorY(5) && wall && roll < 15) put(level, p, x, fy, z, pillar(Blocks.BONE_BLOCK, Direction.Axis.Y));
                }
                case 6 -> {
                    if (roll < 3) put(level, p, x, fy, z, skull(x, z));
                    else if (wall && roll < 8) put(level, p, x, fy, z, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
                }
                case 7 -> {
                    if (f < floorY(7) && roll < 10) put(level, p, x, fy, z, skull(x, z));
                    if (f == floorY(7) && wall && roll < 12) put(level, p, x, fy, z, pillar(Blocks.BONE_BLOCK, Direction.Axis.Y));
                }
                default -> {
                    // the throne room: candles burning on the dais, skulls along its walls, soul lanterns in rows
                    if (fo(c) > 0 && roll < 20) {
                        put(level, p, x, fy, z, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + roll % 4).setValue(CandleBlock.LIT, true));
                    } else if (wall && roll < 30) put(level, p, x, fy, z, roll < 15 ? skull(x, z) : pillar(Blocks.BONE_BLOCK, Direction.Axis.Y));
                    if (Math.abs(lx) == 10 && Math.floorMod(lz, 8) == 4 && lz > THRONE_N + 10) {
                        for (int y = 0; y < 10; y++) put(level, p, x, ty - y, z, s(Blocks.CHAIN));
                        put(level, p, x, ty - 10, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                    }
                    // ribs: the vault arched with bone every eight blocks, a spine along its ridge
                    if (Math.floorMod(lz, 8) == 0 || Math.abs(lx) <= 1) {
                        put(level, p, x, ty, z, bone(Math.abs(lx) <= 1 ? Direction.Axis.Z : Direction.Axis.X));
                        if (Math.floorMod(lz, 8) == 0) put(level, p, x, ty - 1, z, bone(Direction.Axis.X));
                    }
                }
            }
        }
    }

    private static boolean lamp(int lx, int lz, int every) {
        return Math.floorMod(lx, every) == every / 2 && Math.floorMod(lz, every) == every / 2;
    }

    // ── The mountain's tusks ────────────────────────────────────────────

    /** The great bones standing out of the mountain's flanks: how many, how far out, how tall, how far they lean. */
    public static final int TUSKS = 11, TUSK_FROM = 58, TUSK_TO = 112, TUSK_H = 30, TUSK_LEAN = 13;

    /**
     * The mountain's tusks in the column at local (x, z): great curved bones standing out of its
     * flanks all round, leaning outward, tapering to a point (none over the forecourt or the road).
     * Each is rooted in the mountain (its foot at the mountain's height there, sunk a little).
     */
    private static boolean tusks(WorldGenLevel level, Site site, int lx, int lz, int x, int z, BlockPos.MutableBlockPos p) {
        boolean any = false;
        for (int i = 0; i < TUSKS; i++) {
            double a = (i + 0.5) * Math.PI * 2 / TUSKS - Math.PI / 2 + 0.2;
            double d = TUSK_FROM + (TUSK_TO - TUSK_FROM) * ((i * 7) % TUSKS) / (double) TUSKS;
            double bx = Math.cos(a) * d, bz = Math.sin(a) * d;
            if (bz > 30 && Math.abs(bx) < 40) continue; // the forecourt and the road's head
            if (Math.abs(lx - bx) > TUSK_LEAN + 4 || Math.abs(lz - bz) > TUSK_LEAN + 4) continue;
            int foot = site.y0() + (int) Math.round(mountain(bx, bz) * 0.9) - 4;
            int h = TUSK_H + (i % 3) * 5;
            for (int t = 0; t <= h; t++) {
                double f = t / (double) h;
                double cx = bx + Math.cos(a) * TUSK_LEAN * f * f, cz = bz + Math.sin(a) * TUSK_LEAN * f * f;
                double rr = 3.3 * (1 - f) + 0.5;
                if (Math.hypot(lx - cx, lz - cz) > rr) continue;
                int y = Math.min(TOP_LIMIT + 3, foot + t);
                put(level, p, x, y, z, t == h ? s(Blocks.BONE_BLOCK) : bone(Direction.Axis.Y));
                any = true;
            }
        }
        return any;
    }

    // ── The forecourt ───────────────────────────────────────────────────

    /** The skull carved over the door, rows from the top (local y 34 down to 20): X bone, o a hollow, . nothing. */
    private static final String[] SKULL = {
            ".....XXXXXXX.....",
            "...XXXXXXXXXXX...",
            "..XXXXXXXXXXXXX..",
            ".XXXXXXXXXXXXXXX.",
            ".XXXXXXXXXXXXXXX.",
            "XXXooooXXXooooXXX",
            "XXXooooXXXooooXXX",
            "XXXooooXXXooooXXX",
            ".XXXXXXXoXXXXXXX.",
            ".XXXXXXoooXXXXXX.",
            "..XXXXXXXXXXXXX..",
            "...XoXoXoXoXoX...",
            "...XoXoXoXoXoX...",
            "....XXXXXXXXX....",
            "....XXXXXXXXX....",
    };
    /** The top of the skull over the door (local height). */
    public static final int SKULL_TOP = 47;

    /**
     * The forecourt's column at local (x, z): over the door, the skull carved in the cliff; before it
     * the door's pillars, braziers of soul fire either side, skulls on pikes along its walls, and
     * two great ribs arched over it, as if one walked into the carcass of something vast.
     */
    private static boolean forecourt(WorldGenLevel level, Site site, int lx, int lz, BlockPos.MutableBlockPos p) {
        int x = site.x(lx), z = site.z(lz), y0 = site.y0();
        int ax = Math.abs(lx);
        boolean any = false;
        // the skull over the door, twice the size of a man's height in each of its lines: its face on
        // the cliff (z 59), its hollows cut into it (z 58); the door is its mouth
        if ((lz == THRESHOLD_Z || lz == COURT_Z0) && ax <= COURT_HALF) {
            int col = (lx + COURT_HALF + 1) / 2;
            for (int r = 0; r < SKULL.length; r++) {
                char ch = SKULL[r].charAt(Math.min(16, col));
                for (int sub = 0; sub < 2; sub++) {
                    int y = y0 + SKULL_TOP - 2 * r - sub;
                    if (lz == COURT_Z0 && ch == 'X') put(level, p, x, y, z, (r + sub) % 5 == 0 ? bone(Direction.Axis.X) : pillar(Blocks.BONE_BLOCK, Direction.Axis.Y));
                    if (lz == THRESHOLD_Z && ch == 'o') put(level, p, x, y, z, r <= 7 ? s(Blocks.CRYING_OBSIDIAN) : s(Blocks.BLACKSTONE));
                }
            }
            any = true;
        }
        // the door's pillars, standing out of the cliff under the skull's jaw, lanterns hung between them
        if (lz == COURT_Z0 && (ax == DOOR_HALF + 3 || ax == DOOR_HALF + 4)) {
            for (int y = 0; y <= 17; y++) put(level, p, x, y0 + y, z, y % 6 == 5 ? s(Blocks.GILDED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            any = true;
        }
        if (lz == COURT_Z0 && ax <= DOOR_HALF && ax % 3 == 0) {
            put(level, p, x, y0 + 17, z, s(Blocks.CHAIN));
            put(level, p, x, y0 + 16, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            any = true;
        }
        if (lz < COURT_Z0) return any;
        // braziers of soul fire either side of the way
        if (ax == 12 && (lz == 63 || lz == 73)) {
            put(level, p, x, y0, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            put(level, p, x, y0 + 1, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            put(level, p, x, y0 + 2, z, s(Blocks.SOUL_SOIL));
            put(level, p, x, y0 + 3, z, s(Blocks.SOUL_FIRE));
            any = true;
        }
        // skulls on pikes along its walls
        if (ax == COURT_HALF && Math.floorMod(lz, 4) == 1) {
            for (int y = 0; y < 3; y++) put(level, p, x, y0 + y, z, bone(Direction.Axis.Y));
            put(level, p, x, y0 + 3, z, skull(x, z));
            any = true;
        }
        // two ribs arched over the forecourt
        for (int rib : new int[]{65, 77}) {
            if (lz != rib && lz != rib + 1) continue;
            double rr = COURT_HALF - 0.5;
            if (ax > COURT_HALF) continue;
            int lo = (int) Math.floor(Math.sqrt(Math.max(0, rr * rr - (ax + 0.5) * (ax + 0.5))) * 1.25);
            int hi = (int) Math.ceil(Math.sqrt(Math.max(0, rr * rr - Math.max(0, ax - 0.5) * Math.max(0, ax - 0.5))) * 1.25);
            for (int y = Math.max(lo, 0); y <= hi; y++) {
                if (ax >= COURT_HALF - 1 || y >= 6) put(level, p, x, y0 + y, z, ax >= COURT_HALF - 2 ? bone(Direction.Axis.Y) : pillar(Blocks.BONE_BLOCK, Direction.Axis.X));
            }
            any = true;
        }
        return any;
    }

    // ── The road ────────────────────────────────────────────────────────

    /** The road's walking height (world) at local z, and whether it is a half step there. */
    private static int roadFeet(WorldGenLevel level, Site site, int lz) {
        return Math.floorDiv(2 * site.y0() + roadHalves(lz, CaveSites.roadEnd(level, site)), 2);
    }

    /** The bone arch over the road's head, and the watchtowers along it, either side. */
    private static boolean road(WorldGenLevel level, Site site, int lx, int lz, BlockPos.MutableBlockPos p) {
        int x = site.x(lx), z = site.z(lz);
        int dx = lx - roadX(lz);
        boolean any = false;
        if (lz == ARCH_Z && Math.abs(dx) <= ROAD_HALF + 4) {
            int feet = roadFeet(level, site, lz);
            int adx = Math.abs(dx);
            if (adx == ROAD_HALF + 2) {
                for (int y = 0; y < 13; y++) put(level, p, x, feet + y, z, bone(Direction.Axis.Y));
                put(level, p, x, feet + 13, z, bone(Direction.Axis.Y));
                put(level, p, x, feet + 14, z, skull(x, z));
            }
            if (adx <= ROAD_HALF + 4) {
                put(level, p, x, feet + 12, z, pillar(Blocks.BONE_BLOCK, Direction.Axis.X));
                if (adx <= ROAD_HALF + 2) put(level, p, x, feet + 13, z, bone(Direction.Axis.X));
            }
            if (adx == 2) {
                put(level, p, x, feet + 11, z, s(Blocks.CHAIN));
                put(level, p, x, feet + 10, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            any = true;
        }
        for (int t : TOWERS) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int tx = roadX(t) + sgn * TOWER_OUT, ox = lx - tx, oz = lz - t;
                if (Math.abs(ox) > 2 || Math.abs(oz) > 2) continue;
                any |= tower(level, site, ox, oz, roadFeet(level, site, t), x, z, p);
            }
        }
        return any;
    }

    /**
     * A watchtower's column at (ox, oz) from its middle, its foot at {@code feet}: posts of dark wood
     * and bone three wide, down to the ground, up {@value WhiteBoneCave#TOWER_H}; a platform five wide
     * on top, skulls on its corners, a fire of soul fire in its middle.
     */
    private static boolean tower(WorldGenLevel level, Site site, int ox, int oz, int feet, int x, int z, BlockPos.MutableBlockPos p) {
        int ax = Math.abs(ox), az = Math.abs(oz);
        int top = feet + TOWER_H;
        if (ax <= 1 && az <= 1) {
            int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            for (int y = Math.min(ground, feet) - 4; y < top; y++) {
                boolean post = ax == 1 && az == 1;
                BlockState s = post ? pillar(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : y >= feet && (ax == 0 && az == 0) ? Blocks.CAVE_AIR.defaultBlockState()
                        : y < feet ? s(ModBlocks.BONE_ANDESITE.get()) : Math.floorMod(y, 4) == 0 ? bone(Direction.Axis.X) : s(Blocks.DARK_OAK_PLANKS);
                if (y >= feet && ax + az == 1 && Math.floorMod(y - feet, 4) == 2) s = Blocks.CAVE_AIR.defaultBlockState(); // arrow slits
                put(level, p, x, y, z, s);
            }
            if (ax == 0 && az == 0) put(level, p, x, top - 1, z, s(Blocks.DARK_OAK_PLANKS));
        }
        // the platform, its rail, its fire
        put(level, p, x, top, z, ax == 2 || az == 2 ? bone(Direction.Axis.Y) : s(Blocks.DARK_OAK_PLANKS));
        if (ax == 2 && az == 2) {
            put(level, p, x, top + 1, z, bone(Direction.Axis.Y));
            put(level, p, x, top + 2, z, skull(x, z));
        } else if (ax == 2 || az == 2) {
            put(level, p, x, top + 1, z, s(Blocks.DARK_OAK_FENCE));
        } else if (ax == 0 && az == 0) {
            put(level, p, x, top + 1, z, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        return true;
    }

    // ── The outposts ────────────────────────────────────────────────────

    /**
     * An outpost's column at (ox, oz) from its middle, its pad's walking height {@code feet}: a
     * palisade of bone stakes round it, open east and west; a watchtower in its middle; a fire, and
     * skulls on pikes.
     */
    private static boolean outpost(WorldGenLevel level, Site site, int ox, int oz, int feet, int x, int z, BlockPos.MutableBlockPos p) {
        double r = Math.hypot(ox, oz);
        if (r >= OUTPOST_R - 0.5 && r < OUTPOST_R + 0.5 && Math.abs(oz) > 2) {
            if (Math.floorMod(ox + oz, 2) == 0) {
                for (int y = 0; y < 4; y++) put(level, p, x, feet + y, z, bone(Direction.Axis.Y));
                if (hash(x, z, 5) < 20) put(level, p, x, feet + 4, z, skull(x, z));
            } else {
                put(level, p, x, feet, z, bone(Direction.Axis.Y));
                put(level, p, x, feet + 1, z, s(Blocks.DARK_OAK_FENCE));
            }
            return true;
        }
        if (Math.abs(ox) <= 2 && Math.abs(oz) <= 2) return tower(level, site, ox, oz, feet, x, z, p) | true;
        if (ox == 5 && oz == -5) {
            put(level, p, x, feet, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            return true;
        }
        if ((ox == -6 && oz == 6) || (ox == 6 && oz == 6) || (ox == -6 && oz == -6)) {
            for (int y = 0; y < 3; y++) put(level, p, x, feet + y, z, bone(Direction.Axis.Y));
            put(level, p, x, feet + 3, z, skull(x, z));
            return true;
        }
        return false;
    }
}
