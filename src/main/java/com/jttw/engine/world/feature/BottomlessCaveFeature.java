package com.jttw.engine.world.feature;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.cave.BottomlessCave;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Lays the Bottomless Cave and Pit-Hole Mountain over it, chunk by chunk, from their plans
 * ({@link Labyrinth}, {@link PitHole}): first the mountain (its massif raised over the land, the
 * forecourt cut level before the door, the road shelved into its flank, the outposts about it), then
 * the cave (the great door, the gate hall, the great shaft, eight floors of maze, the throne level),
 * then what stands and hangs in it. Everything it puts is worked out from the block's place alone,
 * so the chunks of one region agree whichever is made first, and a chunk made later never undoes
 * another's work.
 * <ul>
 *   <li>The cave's rock is its own ({@code bottomless_earth}, {@code _stone}, {@code _deep}: none
 *       digs, blasts, pushes or phases through it). Where a face of it is backed by more of it, the
 *       face is dressed in the floor's own stone or earth, which may be dug, but only to find the
 *       cave's rock behind it;</li>
 *   <li>each floor has a look of its own: the burrows' earth and roots, the granaries' mud brick and
 *       planking, the bone warrens' tuff and cobwebs, the fungus caverns' moss and giant mushrooms,
 *       the river's stone and flooded tunnels, the dark warren's blackstone and soul soil (unlit),
 *       the storerooms' deepslate and chests, the bridal halls' red and gold;</li>
 *   <li>at each landing, the Earth God's niche: his tablet and a candle (a checkpoint).</li>
 * </ul>
 * Runs first among the zone's features (raw generation).
 */
public class BottomlessCaveFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = Zones.BOTTOMLESS;
    private static final int H = Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1;
    /** How far out from the centre anything of the mountain is laid: its road's far end and its outposts. */
    public static final int REACH = PitHole.OUTPOST_TO + 12 + PitHole.ROAD_ON;
    /** Time spent laying the cave and the mountain, and the chunks they were laid in (for the worldgen bench). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong(),
            CHUNKS = new java.util.concurrent.atomic.AtomicLong();
    /** What the storerooms' chests hold. */
    public static final ResourceKey<LootTable> STOREROOM_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/bottomless_storeroom"));

    public BottomlessCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The cave under the region this chunk lies in, if the region is there (its host was found); else null. */
    public static Labyrinth labyrinthFor(WorldGenLevel level, int x, int z) {
        return BottomlessCave.near(level.getLevel(), x, z);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = ZONE.centre(level.getSeed(), x0 + 8, z0 + 8);
        if (c == null) return false;
        if (Math.hypot(x0 + 8 - c[0], z0 + 8 - c[1]) > REACH + 12) return false;
        long start = System.nanoTime();
        Labyrinth lab = labyrinthFor(level, x0 + 8, z0 + 8);
        if (lab == null) return false;
        build(level, lab, BottomlessCave.mountain(level.getLevel(), lab), x0, z0);
        NANOS.addAndGet(System.nanoTime() - start);
        CHUNKS.incrementAndGet();
        return true;
    }

    /** Lays the mountain's and the cave's part in the chunk starting at (x0, z0) (the gallery and the tests lay some in a flat world). */
    public static void build(WorldGenLevel level, Labyrinth lab, PitHole pit, int x0, int z0) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        long seed = level.getSeed();
        if (pit != null) mountain(level, lab, pit, x0, z0, p);
        boolean near = Math.abs(x0 + 8 - lab.cx) <= BottomlessCave.FORTRESS_REACH + 16 && Math.abs(z0 + 8 - lab.cz) <= BottomlessCave.FORTRESS_REACH + 16;
        if (!near) return;
        // what the cave holds in this chunk's columns and the ring round it (for its walls' faces)
        byte[][][] codes = new byte[18][18][H];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) lab.column(x0 + i - 1, z0 + j - 1, codes[i][j]);
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            byte[] col = codes[dx + 1][dz + 1];
            for (int y = Labyrinth.Y_MIN; y <= Labyrinth.Y_MAX; y++) {
                byte code = col[y - Labyrinth.Y_MIN];
                if (code == Labyrinth.UNTOUCHED) continue;
                p.set(x, y, z);
                if (code == Labyrinth.ROCK) {
                    level.setBlock(p, rockAt(lab, codes, dx + 1, dz + 1, seed, x, y, z), Block.UPDATE_CLIENTS);
                    continue;
                }
                level.setBlock(p, openBlock(code, y), Block.UPDATE_CLIENTS);
            }
        }
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            decorate(level, lab, seed, x0 + dx, z0 + dz, codes[dx + 1][dz + 1], p);
        }
    }

    // ── The cave's rock ─────────────────────────────────────────────────

    private static byte code(byte[][][] codes, int i, int j, int y) {
        if (y < Labyrinth.Y_MIN || y > Labyrinth.Y_MAX) return Labyrinth.UNTOUCHED;
        return codes[i][j][y - Labyrinth.Y_MIN];
    }

    /**
     * A block of the cave's rock: dressed in the floor's own stone where it faces something open and
     * has more of the cave's rock behind it (so digging the dressing finds only that); else the
     * cave's own, unbreakable.
     */
    private static BlockState rockAt(Labyrinth lab, byte[][][] codes, int i, int j, long seed, int x, int y, int z) {
        boolean up = Labyrinth.open(code(codes, i, j, y + 1)), down = Labyrinth.open(code(codes, i, j, y - 1));
        boolean backed;
        int face;
        if (up) {
            face = 1;
            backed = !down && code(codes, i, j, y - 1) == Labyrinth.ROCK;
        } else if (down) {
            face = 2;
            backed = code(codes, i, j, y + 1) == Labyrinth.ROCK;
        } else {
            face = 0;
            backed = true;
            for (int[] d : Labyrinth.DIRS) {
                if (!Labyrinth.open(code(codes, i + d[0], j + d[1], y))) continue;
                face = 3;
                if (code(codes, i - d[0], j - d[1], y) != Labyrinth.ROCK) backed = false;
            }
        }
        if (face == 0 || !backed) return core(y);
        int k = Labyrinth.levelAt(y);
        double n = Labyrinth.noise(seed, x, y, z);
        if (k < 0) return hallSkin(lab, face, n, x, y, z);
        if (k == Labyrinth.FLOORS) return throneSkin(lab, face, n, x, y, z);
        return face == 1 ? floorSkin(k, n) : face == 2 ? core(y) : wallSkin(k, n, y - Labyrinth.floorY(k));
    }

    /** The cave's own rock at a height: its earth over y 60, stone to y 0, deeprock below. */
    static BlockState core(int y) {
        return (y >= 60 ? ModBlocks.BOTTOMLESS_EARTH : y >= 0 ? ModBlocks.BOTTOMLESS_STONE : ModBlocks.BOTTOMLESS_DEEP).get().defaultBlockState();
    }

    private static BlockState pick(double n, Block... blocks) {
        return blocks[Math.min(blocks.length - 1, (int) (n * blocks.length))].defaultBlockState();
    }

    /**
     * A floor's walls, {@code row} blocks over its floor: the burrows' and the caverns' earth and rock
     * as it comes; the built floors (the granaries, the storerooms, the bridal halls) in courses.
     */
    static BlockState wallSkin(int k, double n, int row) {
        int r = Math.floorMod(row, Labyrinth.STOREY);
        return switch (k) {
            case 0 -> pick(n, Blocks.PACKED_MUD, Blocks.PACKED_MUD, Blocks.ROOTED_DIRT, Blocks.DIRT, Blocks.COARSE_DIRT);
            case 1 -> r == 0 ? Blocks.MUD_BRICKS.defaultBlockState() : r == 2 ? (n < 0.8 ? Blocks.SPRUCE_PLANKS : Blocks.MUD_BRICKS).defaultBlockState()
                    : pick(n, Blocks.PACKED_MUD, Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.ROOTED_DIRT);
            case 2 -> pick(n, Blocks.TUFF, Blocks.TUFF, Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.BONE_BLOCK);
            case 3 -> pick(n, Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.STONE, Blocks.MOSSY_COBBLESTONE, Blocks.TUFF);
            case 4 -> pick(n, Blocks.STONE, Blocks.MOSSY_STONE_BRICKS, Blocks.DRIPSTONE_BLOCK, Blocks.CALCITE, Blocks.STONE);
            case 5 -> pick(n, Blocks.BLACKSTONE, Blocks.BLACKSTONE, Blocks.SMOOTH_BASALT, Blocks.TUFF, Blocks.SOUL_SOIL);
            case 6 -> r == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : r == 3 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                    : (n < 0.15 ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_BRICKS).defaultBlockState();
            default -> r == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : r <= 2 ? Blocks.RED_NETHER_BRICKS.defaultBlockState()
                    : r == 3 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        };
    }

    /** A floor's floors (all of them earth on the upper floors, so petals and flowers take). */
    static BlockState floorSkin(int k, double n) {
        return switch (k) {
            case 0 -> pick(n, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.DIRT);
            case 1 -> pick(n, Blocks.ROOTED_DIRT, Blocks.COARSE_DIRT, Blocks.PACKED_MUD);
            case 2 -> pick(n, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.TUFF);
            case 3 -> pick(n, Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PODZOL, Blocks.ROOTED_DIRT);
            case 4 -> pick(n, Blocks.GRAVEL, Blocks.CLAY, Blocks.MUD);
            case 5 -> pick(n, Blocks.SOUL_SOIL, Blocks.SOUL_SOIL, Blocks.SOUL_SAND, Blocks.BLACKSTONE);
            case 6 -> pick(n, Blocks.DEEPSLATE_TILES, Blocks.SPRUCE_PLANKS, Blocks.POLISHED_DEEPSLATE);
            default -> pick(n, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
        };
    }

    /** The gate hall: blackstone and deepslate brick, a lamp set in its walls every few blocks. */
    private static BlockState hallSkin(Labyrinth lab, int face, double n, int x, int y, int z) {
        if (face == 1) return pick(n, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        if (face == 2) return Blocks.DEEPSLATE_TILES.defaultBlockState();
        if (y == lab.doorY + 4 && Math.floorMod(x + z, 6) == 0) return Blocks.SHROOMLIGHT.defaultBlockState();
        return n < 0.1 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : pick(n, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
    }

    /** The throne level: her palace's gilded blackstone and lamps; elsewhere red-hung deepslate. */
    private static BlockState throneSkin(Labyrinth lab, int face, double n, int x, int y, int z) {
        Labyrinth.Room room = nearRoom(lab, x, z);
        if (room != null && room.kind() == Labyrinth.PALACE) {
            if (face == 1) return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            if (face == 2) return Blocks.DEEPSLATE_TILES.defaultBlockState();
            if ((y == Labyrinth.THRONE + 5 || y == Labyrinth.THRONE + 13) && Math.floorMod(x - lab.cx + z - lab.cz, 4) == 0) {
                return Blocks.SHROOMLIGHT.defaultBlockState();
            }
            return n < 0.12 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (room != null && room.kind() == Labyrinth.SHRINE) {
            if (face == 1) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
            return Blocks.MUD_BRICKS.defaultBlockState();
        }
        if (face == 1) return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        if (face == 2) return Blocks.DEEPSLATE_TILES.defaultBlockState();
        return n < 0.25 ? Blocks.RED_NETHER_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    /** The throne level's room at (x, z), or one beside it (for its walls). */
    private static Labyrinth.Room nearRoom(Labyrinth lab, int x, int z) {
        Labyrinth.Room r = lab.roomAt(Labyrinth.FLOORS, x, z);
        if (r != null) return r;
        for (int[] d : Labyrinth.DIRS) {
            r = lab.roomAt(Labyrinth.FLOORS, x + d[0], z + d[1]);
            if (r != null) return r;
        }
        return null;
    }

    /** Which floor's look a height takes (the stairs between floors take the upper's; the throne level 8). */
    static int themeAt(int y) {
        for (int k = 0; k < Labyrinth.FLOORS; k++) if (y >= Labyrinth.FLOOR[k] - 6) return k;
        return Labyrinth.FLOORS;
    }

    private static BlockState openBlock(byte code, int y) {
        int k = themeAt(y);
        return switch (code) {
            case Labyrinth.STEP_N, Labyrinth.STEP_E, Labyrinth.STEP_S, Labyrinth.STEP_W -> stairs(k).defaultBlockState()
                    .setValue(StairBlock.FACING, facing(code - Labyrinth.STEP_N));
            case Labyrinth.PILLAR, Labyrinth.COVER -> bricks(k).defaultBlockState();
            case Labyrinth.CORE -> Blocks.SHROOMLIGHT.defaultBlockState();
            case Labyrinth.WATER -> Blocks.WATER.defaultBlockState();
            case Labyrinth.LANTERN -> Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case Labyrinth.SOUL_LANTERN -> Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case Labyrinth.HELIX -> Blocks.MUD_BRICKS.defaultBlockState();
            case Labyrinth.HELIX_SLAB -> Blocks.MUD_BRICK_SLAB.defaultBlockState();
            case Labyrinth.HELIX_LANTERN -> Blocks.LANTERN.defaultBlockState();
            case Labyrinth.GATE -> ModBlocks.BOTTOMLESS_GATE.get().defaultBlockState();
            case Labyrinth.FRAME, Labyrinth.GILT -> ModBlocks.BOTTOMLESS_GATE_FRAME.get().defaultBlockState();
            default -> Blocks.CAVE_AIR.defaultBlockState();
        };
    }

    private static Block stairs(int k) {
        return switch (k) {
            case 0, 1 -> Blocks.MUD_BRICK_STAIRS;
            case 2 -> Blocks.TUFF_BRICK_STAIRS;
            case 3 -> Blocks.MOSSY_COBBLESTONE_STAIRS;
            case 4 -> Blocks.MOSSY_STONE_BRICK_STAIRS;
            case 5 -> Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS;
            default -> Blocks.DEEPSLATE_BRICK_STAIRS;
        };
    }

    private static Block bricks(int k) {
        return switch (k) {
            case 0, 1 -> Blocks.MUD_BRICKS;
            case 2 -> Blocks.TUFF_BRICKS;
            case 3 -> Blocks.MOSSY_COBBLESTONE;
            case 4 -> Blocks.MOSSY_STONE_BRICKS;
            case 5 -> Blocks.POLISHED_BLACKSTONE_BRICKS;
            default -> Blocks.DEEPSLATE_BRICKS;
        };
    }

    /** North, east, south, west, as the step codes count them. */
    static Direction facing(int dir) {
        return switch (dir) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    // ── Pit-Hole Mountain ───────────────────────────────────────────────

    private static boolean solid(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && s.blocksMotion();
    }

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    private static int ground(WorldGenLevel level, int x, int z) {
        return level.getHeight(level instanceof net.minecraft.server.level.ServerLevel ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
    }

    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(level instanceof net.minecraft.server.level.ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    /** The mountain's part in this chunk: the massif, the forecourt, the road, the outposts. */
    private static void mountain(WorldGenLevel level, Labyrinth lab, PitHole pit, int x0, int z0, BlockPos.MutableBlockPos p) {
        long seed = level.getSeed();
        int top = level.getMaxBuildHeight() - 2;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            if (Math.hypot(x - lab.cx, z - lab.cz) > 130) continue;
            int g = ground(level, x, z);
            if (pit.forecourt(x, z)) {
                forecourt(level, lab, pit, x, z, g, p);
                continue;
            }
            int m = Math.min(top, pit.massif(x, z));
            if (m <= g) continue;
            // how steep it stands here: a face of rock, or ground that bears earth
            int steep = 0;
            for (int[] d : Labyrinth.DIRS) steep = Math.max(steep, Math.abs(pit.massif(x + d[0], z + d[1]) - m));
            for (int y = g + 1; y <= m; y++) {
                double n = Labyrinth.noise(seed, x, y, z);
                BlockState s;
                if (y == m) s = steep >= 3 ? pick(n, Blocks.STONE, Blocks.ANDESITE, Blocks.STONE, Blocks.TUFF, Blocks.MOSSY_COBBLESTONE)
                        : y > 168 ? pick(n, Blocks.STONE, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.SNOW_BLOCK)
                        : pick(n, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.ROOTED_DIRT,
                        Blocks.MOSS_BLOCK, Blocks.GRAVEL, Blocks.PODZOL);
                else if (y >= m - 2 && steep < 3) s = Blocks.DIRT.defaultBlockState();
                else if (Math.floorMod(y + (int) (Labyrinth.noise(seed, x, 0, z) * 3), 9) < 2) s = Blocks.TUFF.defaultBlockState();
                else s = pick(n, Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.COBBLESTONE,
                        Blocks.MOSSY_COBBLESTONE, Blocks.DEEPSLATE);
                set(level, p.set(x, y, z), s);
            }
        }
        road(level, lab, pit, x0, z0, p);
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            PitHole.Outpost o = pit.outpostAt(x0 + dx, z0 + dz);
            if (o != null) outpost(level, o, x0 + dx, z0 + dz, p);
        }
    }

    /**
     * The forecourt: cut level into the mountainside at the door's threshold, open to the sky; a way
     * of mud brick to the door between beaten earth and bones; a pillar either side of the way with a
     * soul lantern on it; skulls on stakes at its outer corners.
     */
    private static void forecourt(WorldGenLevel level, Labyrinth lab, PitHole pit, int x, int z, int g, BlockPos.MutableBlockPos p) {
        int floor = lab.doorY - 1;
        long seed = level.getSeed();
        int[] ac = pit.doorFrame(x, z);
        int a = ac[0] - (Labyrinth.DOOR_AT + 2), c = Math.abs(ac[1]);
        int cut = Math.max(g, pit.massif(x, z)) + 2;
        for (int y = floor + 1; y <= Math.min(cut, level.getMaxBuildHeight() - 2); y++) set(level, p.set(x, y, z), Blocks.AIR.defaultBlockState());
        for (int y = Math.max(g + 1, floor - 110); y < floor; y++) set(level, p.set(x, y, z), Blocks.STONE.defaultBlockState());
        double n = Labyrinth.noise(seed, x, floor, z);
        // none of it ground a tree takes root in
        set(level, p.set(x, floor, z), c <= Labyrinth.DOOR_HALF ? pick(n, Blocks.MUD_BRICKS, Blocks.MUD_BRICKS, Blocks.PACKED_MUD)
                : pick(n, Blocks.PACKED_MUD, Blocks.GRAVEL, Blocks.TUFF, Blocks.PACKED_MUD, Blocks.COBBLESTONE));
        if (c > 2 && n > 0.93) set(level, p.set(x, floor + 1, z), Blocks.SKELETON_SKULL.defaultBlockState());
        gateFront(level, lab, a, ac[1], floor, x, z, p);
        // skulls on stakes at the outer corners
        if (a == PitHole.FORE_OUT - 2 && c == PitHole.FORE_HALF - 2) {
            for (int y = floor + 1; y <= floor + 3; y++) set(level, p.set(x, y, z), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, p.set(x, floor + 4, z), Blocks.SKELETON_SKULL.defaultBlockState());
        }
    }

    /**
     * The gate's front, before the door's frame, as a demon king's gate is built: two great pillars of
     * blackstone banded in gold either side of the door, a red beam across over it with the plaque
     * on it, a roof of dark tiles over all with lanterns hung from its eaves, and soul fire on the
     * pillars' heads; before them, a pillar either side of the way in, a soul lantern on it.
     * {@code a}: blocks out from the frame; {@code c}: across.
     */
    private static void gateFront(WorldGenLevel level, Labyrinth lab, int a, int c, int floor, int x, int z, BlockPos.MutableBlockPos p) {
        int d = lab.doorY, half = Labyrinth.DOOR_HALF, high = Labyrinth.DOOR_HIGH;
        int ac = Math.abs(c);
        int beam = d + high + 1, roof = beam + 3;
        Direction out = facing(lab.doorDir);
        // the great pillars
        if (a <= 1 && (ac == half + 1 || ac == half + 2)) {
            for (int y = d; y < beam + 2; y++) {
                set(level, p.set(x, y, z), (Math.floorMod(y - d, 5) == 4 ? Blocks.GOLD_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            }
            if (a == 1 && ac == half + 2) set(level, p.set(x, beam + 2, z), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        }
        // the beam across, red, gold-edged
        if (a == 0 && ac <= half + 3) {
            set(level, p.set(x, beam, z), Blocks.STRIPPED_CRIMSON_HYPHAE.defaultBlockState());
            set(level, p.set(x, beam + 1, z), (ac % 2 == 0 ? Blocks.GOLD_BLOCK : Blocks.STRIPPED_CRIMSON_HYPHAE).defaultBlockState());
        }
        // the roof over it: tiles running out over the forecourt, the eaves turned up at the ends
        if (a <= 3 && ac <= half + 4) {
            int y = roof + (ac >= half + 4 ? 1 : 0) - (a >= 2 ? 1 : 0);
            BlockState tile = a == 3 ? Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, out.getOpposite())
                    : ac == half + 4 ? Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,
                    c > 0 ? facing((lab.doorDir + 3) % 4) : facing((lab.doorDir + 1) % 4)) : Blocks.DEEPSLATE_TILES.defaultBlockState();
            set(level, p.set(x, y, z), tile);
            if (a <= 1 && ac <= half + 3) set(level, p.set(x, roof - 1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            // lanterns from the eaves
            if (a == 3 && (ac == half + 3 || ac == 1)) {
                set(level, p.set(x, y - 1, z), Blocks.CHAIN.defaultBlockState());
                set(level, p.set(x, y - 2, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
        // the plaque on the beam
        if (a == 1 && c == 0) {
            BlockPos at = new BlockPos(x, beam, z);
            set(level, at, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, out));
            sign(level, at, "\u65e0\u5e95\u6d1e", "THE BOTTOMLESS", "CAVE", "\u9677\u7a7a\u5c71");
        }
        // a pillar either side of the way in, a soul lantern on it
        if (a == 7 && ac == half + 3) {
            for (int y = floor + 1; y <= floor + 5; y++) set(level, p.set(x, y, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            set(level, p.set(x, floor + 6, z), Blocks.GOLD_BLOCK.defaultBlockState());
            set(level, p.set(x, floor + 7, z), Blocks.SOUL_LANTERN.defaultBlockState());
        }
    }

    /**
     * The road through this chunk: up on the mountain a shelf cut into its flank (open over it, built
     * up under it), of beaten earth and gravel, a lantern on a post along its outer edge; below, a
     * track of beaten earth over the land.
     */
    private static void road(WorldGenLevel level, Labyrinth lab, PitHole pit, int x0, int z0, BlockPos.MutableBlockPos p) {
        long seed = level.getSeed();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            int[] r = pit.roadAt(x, z);
            if (r == null || pit.forecourt(x, z)) continue;
            double n = Labyrinth.noise(seed, x, 7, z);
            // beaten earth and gravel, none of it ground a tree takes root in
            BlockState bed = r[1] <= 10 ? pick(n, Blocks.DIRT_PATH, Blocks.DIRT_PATH, Blocks.PACKED_MUD, Blocks.GRAVEL)
                    : pick(n, Blocks.PACKED_MUD, Blocks.GRAVEL, Blocks.COBBLESTONE);
            if (pit.onMountain(r[2])) {
                int y = r[0];
                for (int up = 1; up <= 5; up++) set(level, p.set(x, y + up, z), Blocks.AIR.defaultBlockState());
                set(level, p.set(x, y, z), bed);
                for (int down = 1; down <= 40; down++) {
                    p.set(x, y - down, z);
                    if (solid(level.getBlockState(p))) break;
                    set(level, p, (down == 1 ? Blocks.COBBLESTONE : Blocks.STONE).defaultBlockState());
                }
            } else {
                int y = surface(level, x, z);
                p.set(x, y, z);
                BlockState s = level.getBlockState(p);
                if (solid(s) && !s.is(BlockTags.LOGS)) set(level, p, bed);
                if (!level.getBlockState(p.set(x, y + 1, z)).isAir() && level.getBlockState(p).canBeReplaced()) set(level, p, Blocks.AIR.defaultBlockState());
            }
        }
        // the lantern posts whose foot is in this chunk
        for (int n = 0; n < pit.roadLength(); n++) {
            if (!pit.lanternPost(n)) continue;
            int[] at = pit.lanternAt(n);
            if (at[0] < x0 || at[0] > x0 + 15 || at[1] < z0 || at[1] > z0 + 15) continue;
            int[] rp = pit.road(n);
            int y = pit.onMountain(n) ? rp[1] : surface(level, at[0], at[1]);
            p.set(at[0], y, at[1]);
            if (!solid(level.getBlockState(p))) set(level, p, Blocks.COBBLESTONE.defaultBlockState());
            set(level, p.set(at[0], y + 1, at[1]), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, p.set(at[0], y + 2, at[1]), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, p.set(at[0], y + 3, at[1]), Blocks.LANTERN.defaultBlockState());
        }
    }

    /** One column of an outpost. */
    private static void outpost(WorldGenLevel level, PitHole.Outpost o, int x, int z, BlockPos.MutableBlockPos p) {
        int dx = x - o.x(), dz = z - o.z();
        long seed = level.getSeed();
        int g = ground(level, x, z);
        double n = Labyrinth.noise(seed, x, 3, z);
        switch (o.kind()) {
            case PitHole.TOWER -> {
                // a watchtower of spruce on stilts: a platform railed round, a roof, a lantern; a ladder up its north side
                int deck = o.y() + 7;
                int ax = Math.abs(dx), az = Math.abs(dz);
                if (ax > 2 || az > 3) return;
                if (az == 3) {
                    if (dx == 0 && dz == -3) for (int y = Math.max(g + 1, deck - 30); y < deck; y++) {
                        set(level, p.set(x, y, z), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
                    }
                    return;
                }
                boolean corner = ax == 2 && az == 2;
                if (corner || (dx == 0 && dz == -2)) {
                    for (int y = Math.max(g + 1, deck - 30); y < deck; y++) set(level, p.set(x, y, z), Blocks.SPRUCE_LOG.defaultBlockState());
                }
                set(level, p.set(x, deck, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
                if (corner) {
                    for (int y = deck + 1; y <= deck + 3; y++) set(level, p.set(x, y, z), Blocks.SPRUCE_LOG.defaultBlockState());
                } else if ((ax == 2 || az == 2) && !(dx == 0 && dz == -2)) {
                    set(level, p.set(x, deck + 1, z), Blocks.SPRUCE_FENCE.defaultBlockState());
                }
                set(level, p.set(x, deck + 4, z), Blocks.SPRUCE_SLAB.defaultBlockState());
                if (dx == 0 && dz == 0) set(level, p.set(x, deck + 3, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                if (dx == 1 && dz == 1) set(level, p.set(x, deck + 1, z), Blocks.PINK_BANNER.defaultBlockState());
            }
            case PitHole.MOUND -> {
                // a burrow mound of earth, a mouse hole into it, hay and bones before it
                double d = Math.hypot(dx, dz);
                if (d > 5.5) return;
                int h = (int) Math.round(3.6 * (1 - (d / 5.5) * (d / 5.5)));
                int[] f = Labyrinth.DIRS[o.facing()];
                int along = dx * f[0] + dz * f[1], across = dx * f[1] - dz * f[0];
                for (int y = g + 1; y <= g + h; y++) {
                    boolean hole = across == 0 && along >= 1 && y <= g + 2 && y >= g + 1;
                    set(level, p.set(x, y, z), hole ? Blocks.AIR.defaultBlockState() : pick(Labyrinth.noise(seed, x, y, z),
                            Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.PACKED_MUD, Blocks.DIRT));
                }
                if (h <= 0 && n < 0.25) set(level, p.set(x, g + 1, z), (n < 0.12 ? Blocks.HAY_BLOCK : Blocks.BONE_BLOCK).defaultBlockState());
            }
            case PitHole.CAMP -> {
                // the mice's camp: a fire, logs to sit on round it, a pot, hay, a lantern on a post
                if (Math.abs(dx) > 3 || Math.abs(dz) > 3) return;
                int y = surface(level, x, z) + 1;
                p.set(x, y, z);
                if (dx == 0 && dz == 0) set(level, p, Blocks.CAMPFIRE.defaultBlockState());
                else if ((Math.abs(dx) == 2 && dz == 0) || (dx == 0 && Math.abs(dz) == 2)) {
                    set(level, p, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, dx == 0 ? Direction.Axis.X : Direction.Axis.Z));
                } else if (dx == 1 && dz == 1) set(level, p, Blocks.CAULDRON.defaultBlockState());
                else if (dx == -2 && dz <= -1) {
                    set(level, p, Blocks.HAY_BLOCK.defaultBlockState());
                    if (dz == -2) set(level, p.set(x, y + 1, z), Blocks.HAY_BLOCK.defaultBlockState());
                } else if (dx == 2 && dz == -2) {
                    set(level, p, Blocks.SPRUCE_FENCE.defaultBlockState());
                    set(level, p.set(x, y + 1, z), Blocks.SPRUCE_FENCE.defaultBlockState());
                    set(level, p.set(x, y + 2, z), Blocks.LANTERN.defaultBlockState());
                } else if (n < 0.08) set(level, p, Blocks.BONE_BLOCK.defaultBlockState());
            }
            case PitHole.NOOK -> {
                // a guard nook by the mountain road: a fire on the stone, hay to sit on, a skull on a stake
                if (Math.abs(dx) > 1 || Math.abs(dz) > 1) return;
                int y = o.y();
                p.set(x, y, z);
                if (!solid(level.getBlockState(p))) set(level, p, Blocks.COBBLESTONE.defaultBlockState());
                for (int up = 1; up <= 3; up++) set(level, p.set(x, y + up, z), Blocks.AIR.defaultBlockState());
                if (dx == 0 && dz == 0) set(level, p.set(x, y + 1, z), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
                else if (dx == 1 && dz == 1) {
                    set(level, p.set(x, y + 1, z), Blocks.SPRUCE_FENCE.defaultBlockState());
                    set(level, p.set(x, y + 2, z), Blocks.SKELETON_SKULL.defaultBlockState());
                } else if (dx == -1 && dz == -1) set(level, p.set(x, y + 1, z), Blocks.HAY_BLOCK.defaultBlockState());
            }
            default -> {
                // the stele where the road leaves the mountain: a tablet of stone brick, its name on it
                if (Math.abs(dx) > 1 || Math.abs(dz) > 1) return;
                int y = surface(level, x, z);
                int[] f = Labyrinth.DIRS[o.facing()];
                int along = dx * f[0] + dz * f[1], across = dx * f[1] - dz * f[0];
                if (along == 0) {
                    for (int up = 1; up <= 3; up++) set(level, p.set(x, y + up, z), (up == 3 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
                    if (across == 0) set(level, p.set(x, y + 4, z), Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                } else if (along == -1 && across == 0) {
                    BlockPos at = new BlockPos(x, y + 2, z);
                    set(level, at, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing((o.facing() + 2) % 4)));
                    sign(level, at, "陷空山", "Pit-Hole Mountain", "无底洞 ahead", "The Bottomless Cave");
                }
            }
        }
    }

    // ── What stands in the cave ─────────────────────────────────────────

    private static void decorate(WorldGenLevel level, Labyrinth lab, long seed, int x, int z, byte[] col, BlockPos.MutableBlockPos p) {
        gateHall(level, lab, x, z, col, p);
        int[] cell = lab.cellAt(x, z);
        if (cell == null) return;
        for (int k = 0; k < Labyrinth.LEVELS; k++) {
            Labyrinth.Level l = lab.level(k);
            int f = l.y;
            Labyrinth.Room room = lab.roomAt(k, x, z);
            if (room != null) {
                switch (room.kind()) {
                    case Labyrinth.PALACE -> palace(level, lab, seed, x, z, p);
                    case Labyrinth.SHRINE -> shrine(level, lab, seed, x, z, p);
                    case Labyrinth.HUB -> {}
                    default -> hall(level, lab, room, seed, x, z, col, p);
                }
                continue;
            }
            if (col[f - Labyrinth.Y_MIN] != Labyrinth.AIR) continue;
            if (col[f - 1 - Labyrinth.Y_MIN] != Labyrinth.ROCK) continue;
            double n = Labyrinth.noise(seed, x, f, z), n2 = Labyrinth.noise(seed, z, f + 9, x);
            int top = f + Labyrinth.TUNNEL - 1;
            boolean ceiling = col[top - Labyrinth.Y_MIN] == Labyrinth.AIR && col[top + 1 - Labyrinth.Y_MIN] == Labyrinth.ROCK;
            switch (k) {
                case 0, 1 -> {
                    if (ceiling && n < 0.06) set(level, p.set(x, top, z), Blocks.HANGING_ROOTS.defaultBlockState());
                    if (k == 1 && n2 < 0.03) set(level, p.set(x, f, z), Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7));
                }
                case 2 -> {
                    if (ceiling && n < 0.07) set(level, p.set(x, top, z), Blocks.COBWEB.defaultBlockState());
                    else if (n2 < 0.03) set(level, p.set(x, f, z), Blocks.SKELETON_SKULL.defaultBlockState());
                    else if (n2 < 0.05) set(level, p.set(x, f, z), Blocks.COBWEB.defaultBlockState());
                }
                case 3 -> {
                    if (n < 0.18) set(level, p.set(x, f, z), Blocks.MOSS_CARPET.defaultBlockState());
                    else if (n < 0.24) set(level, p.set(x, f, z), (n2 < 0.5 ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState());
                    if (ceiling && n2 < 0.012) set(level, p.set(x, top, z), Blocks.SPORE_BLOSSOM.defaultBlockState());
                    else if (ceiling && n2 < 0.05) set(level, p.set(x, top, z), Blocks.GLOW_LICHEN.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.UP), true));
                }
                case 4 -> {
                    if (ceiling && n < 0.04) set(level, p.set(x, top, z), Blocks.POINTED_DRIPSTONE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN));
                }
                case 6 -> {
                    if (ceiling && n < 0.03) set(level, p.set(x, top, z), Blocks.COBWEB.defaultBlockState());
                }
                case 7 -> {
                    if (n < 0.10) set(level, p.set(x, f, z), petals(n2));
                }
                case 8 -> {
                    // the processional way: a red carpet down its middle
                    int ou = x - lab.cellX(cell[0]), ov = z - lab.cellZ(cell[1]);
                    if (Math.abs(ou) <= 1 && Math.abs(ov) <= 1 || Math.abs(ou) <= 0 || Math.abs(ov) <= 0) set(level, p.set(x, f, z), Blocks.RED_CARPET.defaultBlockState());
                }
                default -> {}
            }
        }
        landing(level, lab, x, z, p);
    }

    /** The gate hall: a red carpet up its middle from the door to the shaft, pillars of blackstone along its walls, capped in gold. */
    private static void gateHall(WorldGenLevel level, Labyrinth lab, int x, int z, byte[] col, BlockPos.MutableBlockPos p) {
        int[] d = Labyrinth.DIRS[lab.doorDir];
        int u = x - lab.cx, v = z - lab.cz, a = u * d[0] + v * d[1], c = Math.abs(u * d[1] - v * d[0]);
        int y = lab.doorY;
        if (a < 8 || a > Labyrinth.DOOR_AT - 3 || c > Labyrinth.HALL_HALF || col[y - Labyrinth.Y_MIN] != Labyrinth.AIR) return;
        if (c <= 1) set(level, p.set(x, y, z), Blocks.RED_CARPET.defaultBlockState());
        else if (c == Labyrinth.HALL_HALF && Math.floorMod(a, 6) == 3) {
            for (int h = 0; h < Labyrinth.HALL_HIGH; h++) {
                set(level, p.set(x, y + h, z), (h == Labyrinth.HALL_HIGH - 1 || h == 0 ? Blocks.GOLD_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            }
        }
    }

    /**
     * The Earth God's niche at a landing (a checkpoint): his tablet, a standing sign at a corner of
     * the cell, and three candles burning in the corner across from it.
     */
    private static void landing(WorldGenLevel level, Labyrinth lab, int x, int z, BlockPos.MutableBlockPos p) {
        for (BottomlessCave.Landing landing : BottomlessCave.landings(lab)) {
            BlockPos at = landing.at();
            int ox = x - at.getX(), oz = z - at.getZ();
            if (landing.level() < 0) {
                if (ox * ox + oz * oz != 0) continue;
                int[] d = Labyrinth.DIRS[(lab.doorDir + 1) % 4];
                BlockPos s = at.offset(d[0] * 3, 0, d[1] * 3);
                set(level, s, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, signFacing((lab.doorDir + 3) % 4)));
                sign(level, s, "土地", "The Earth God", "keeps this way", "");
                set(level, s.offset(-d[0] * 6, 0, -d[1] * 6), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
                continue;
            }
            if (ox == -1 && oz == -1) {
                p.set(x, at.getY(), z);
                if (!level.getBlockState(p).isAir()) continue;
                set(level, p, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 14));
                sign(level, p.immutable(), "土地", "The Earth God", "keeps this way", landing.level() < Labyrinth.FLOORS ? "Floor " + (landing.level() + 1) : "Her palace");
            } else if (ox == 1 && oz == 1) {
                p.set(x, at.getY(), z);
                if (!level.getBlockState(p).isAir()) continue;
                set(level, p, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            }
        }
    }

    /**
     * A hall of a floor: lanterns on chains from its roof, and what its kind keeps: a nest, a larder,
     * a heap of bones, a grotto, a granary, giant mushrooms, a river through it, a storeroom's
     * chests and gold, a bridal chamber's red.
     */
    private static void hall(WorldGenLevel level, Labyrinth lab, Labyrinth.Room room, long seed, int x, int z, byte[] col, BlockPos.MutableBlockPos p) {
        int f = Labyrinth.floorY(room.level()), top = f + room.height() - 1;
        if (col[f - Labyrinth.Y_MIN] != Labyrinth.AIR) return;
        int x0 = lab.cellX(room.i0()) - 1, x1 = lab.cellX(room.i1()) + 1, z0 = lab.cellZ(room.j0()) - 1, z1 = lab.cellZ(room.j1()) + 1;
        boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
        int[] cell = lab.cellAt(x, z);
        boolean middle = cell != null && x == lab.cellX(cell[0]) && z == lab.cellZ(cell[1]);
        double n = Labyrinth.noise(seed, x, f, z), n2 = Labyrinth.noise(seed, z, f + 7, x);
        if (middle) {
            // a lantern on a chain over each cell's middle
            set(level, p.set(x, top, z), Blocks.CHAIN.defaultBlockState());
            set(level, p.set(x, top - 1, z), Blocks.CHAIN.defaultBlockState());
            set(level, p.set(x, top - 2, z), (room.kind() == Labyrinth.BONES ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState()
                    .setValue(LanternBlock.HANGING, true));
        }
        p.set(x, f, z);
        switch (room.kind()) {
            case Labyrinth.NEST -> {
                if (n < 0.35) set(level, p, (n2 < 0.5 ? Blocks.BROWN_CARPET : n2 < 0.8 ? Blocks.WHITE_CARPET : Blocks.LIGHT_GRAY_CARPET).defaultBlockState());
                else if (edge && n < 0.55) set(level, p, Blocks.HAY_BLOCK.defaultBlockState());
            }
            case Labyrinth.LARDER -> {
                if (edge && n < 0.4) set(level, p, (n2 < 0.4 ? Blocks.BARREL : n2 < 0.7 ? Blocks.HAY_BLOCK : n2 < 0.85 ? Blocks.PUMPKIN : Blocks.MELON).defaultBlockState());
            }
            case Labyrinth.BONES -> {
                if (n < 0.10) set(level, p, Blocks.BONE_BLOCK.defaultBlockState());
                else if (n < 0.14) set(level, p, Blocks.SKELETON_SKULL.defaultBlockState());
                if (n2 < 0.08) set(level, p.set(x, top, z), Blocks.COBWEB.defaultBlockState());
            }
            case Labyrinth.GRANARY -> {
                // heaps of the grain they carried down: bales stacked against the walls, sacks, barrels
                if (edge && n < 0.6) {
                    set(level, p, (n2 < 0.55 ? Blocks.HAY_BLOCK : n2 < 0.8 ? Blocks.BARREL : Blocks.BROWN_WOOL).defaultBlockState());
                    if (n < 0.3) set(level, p.set(x, f + 1, z), Blocks.HAY_BLOCK.defaultBlockState());
                } else if (n < 0.08) set(level, p, Blocks.COMPOSTER.defaultBlockState());
            }
            case Labyrinth.FUNGUS -> {
                set(level, p.set(x, f - 1, z), Blocks.MOSS_BLOCK.defaultBlockState());
                p.set(x, f, z);
                if (middle && n2 < 0.7) mushroom(level, x, f, z, n2 < 0.35, p);
                else if (n < 0.25) set(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                else if (n < 0.33) set(level, p, (n2 < 0.5 ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState());
            }
            case Labyrinth.RIVER -> {
                // the river runs through it, a channel down its middle
                boolean wide = (x1 - x0) >= (z1 - z0);
                int mid = wide ? (z0 + z1) / 2 : (x0 + x1) / 2, at = wide ? z : x;
                if (Math.abs(at - mid) <= 1) {
                    set(level, p.set(x, f - 1, z), Blocks.WATER.defaultBlockState());
                    if (n < 0.15) set(level, p.set(x, f, z), Blocks.LILY_PAD.defaultBlockState());
                } else if (n < 0.12) set(level, p, Blocks.CLAY.defaultBlockState());
            }
            case Labyrinth.VAULT -> {
                // her storerooms: chests and barrels along the walls, a heap of gold here and there, candles
                if (edge && !middle && n < 0.16) {
                    Direction in = x == x0 ? Direction.EAST : x == x1 ? Direction.WEST : z == z0 ? Direction.SOUTH : Direction.NORTH;
                    set(level, p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, in));
                    RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create((long) (n * 1e9) ^ seed), p.immutable(), STOREROOM_LOOT);
                } else if (edge && n < 0.4) set(level, p, Blocks.BARREL.defaultBlockState());
                else if (edge && n < 0.46) set(level, p, Blocks.GOLD_BLOCK.defaultBlockState());
                else if (n < 0.04) set(level, p, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
            }
            case Labyrinth.BRIDAL -> {
                // her bridal chamber, made ready for the Tang monk: red underfoot, red candles, flowers
                set(level, p, (edge ? Blocks.PINK_CARPET : Blocks.RED_CARPET).defaultBlockState());
                if (edge && n < 0.12) set(level, p, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
                else if (edge && n < 0.2) set(level, p, (n2 < 0.5 ? Blocks.POTTED_PINK_TULIP : Blocks.POTTED_ALLIUM).defaultBlockState());
            }
            default -> {
                // a grotto: moss and flowers, as in her palace, but wild
                set(level, p.set(x, f - 1, z), Blocks.MOSS_BLOCK.defaultBlockState());
                p.set(x, f, z);
                if (n < 0.3) set(level, p, Blocks.MOSS_CARPET.defaultBlockState());
                else if (n < 0.42) set(level, p, smallFlower(n2));
                else if (n < 0.47) set(level, p, Blocks.AZALEA.defaultBlockState());
                if (n2 < 0.05) set(level, p.set(x, top, z), Blocks.SPORE_BLOSSOM.defaultBlockState());
            }
        }
    }

    /** A giant mushroom in a fungus cavern: a stem five high, its cap two out round it, a lamp under the cap. */
    private static void mushroom(WorldGenLevel level, int x, int f, int z, boolean red, BlockPos.MutableBlockPos p) {
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        BlockState cap = (red ? Blocks.RED_MUSHROOM_BLOCK : Blocks.BROWN_MUSHROOM_BLOCK).defaultBlockState().setValue(HugeMushroomBlock.DOWN, false);
        for (int y = f; y < f + 5; y++) set(level, p.set(x, y, z), stem);
        for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++) {
            if (Math.abs(a) == 2 && Math.abs(b) == 2) continue;
            set(level, p.set(x + a, f + 6, z + b), cap);
            if (Math.abs(a) == 2 || Math.abs(b) == 2) set(level, p.set(x + a, f + 5, z + b), cap);
        }
        set(level, p.set(x, f + 5, z), Blocks.SHROOMLIGHT.defaultBlockState());
    }

    private static BlockState smallFlower(double n) {
        Block[] flowers = {Blocks.ALLIUM, Blocks.PINK_TULIP, Blocks.WHITE_TULIP, Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY,
                Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.RED_TULIP};
        return flowers[(int) (n * flowers.length) % flowers.length].defaultBlockState();
    }

    private static void tall(WorldGenLevel level, BlockPos.MutableBlockPos p, Block flower) {
        int y = p.getY();
        set(level, p, flower.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, p.setY(y + 1), flower.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        p.setY(y);
    }

    /** Pillars of the palace, as {across, along} from its middle (along: toward its front gate). */
    public static final int[][] PALACE_PILLARS = {{-9, -11}, {-9, -4}, {-9, 4}, {-9, 11}, {9, -11}, {9, -4}, {9, 4}, {9, 11}};
    /** Its censers (a fire on a bale, its smoke climbing), and its hanging lanterns. */
    public static final int[][] CENSERS = {{-4, -6}, {4, -6}, {-4, 6}, {4, 6}};
    public static final int[][] CHAINED_LANTERNS = {{0, -8}, {0, 8}, {-6, 0}, {6, 0}, {-6, -10}, {6, -10}, {-6, 10}, {6, 10}};

    private static boolean at(int[][] list, int dx, int dz) {
        for (int[] a : list) if (a[0] == dx && a[1] == dz) return true;
        return false;
    }

    /** (dx, dz) from the palace's middle as {across, along its front gate's way}. */
    public static int[] palaceLocal(Labyrinth lab, int dx, int dz) {
        int[] f = Labyrinth.DIRS[lab.frontDir()];
        return new int[]{dx * f[1] - dz * f[0], dx * f[0] + dz * f[1]};
    }

    /**
     * Her palace: a dark hall, gilded, twenty blocks to its roof, a red carpet from gate to gate up to
     * her dais, red pillars capped with gold, beds of flowers down both sides (peonies, roses, lilacs,
     * azaleas, tulips, petals), censers smoking either side of the carpet, candles on the dais,
     * lanterns hung on long chains, spore blossoms from the roof, and lamps of shroomlight set in the
     * walls.
     */
    private static void palace(WorldGenLevel level, Labyrinth lab, long seed, int x, int z, BlockPos.MutableBlockPos p) {
        int f = Labyrinth.THRONE, top = f + Labyrinth.PALACE_HEIGHT - 1;
        int[] loc = palaceLocal(lab, x - lab.cx, z - lab.cz);
        int dx = loc[0], dz = loc[1];
        int ax = Math.abs(dx), az = Math.abs(dz);
        double n = Labyrinth.noise(seed, x, f, z), n2 = Labyrinth.noise(seed, z, f + 3, x);
        set(level, p.set(x, f - 1, z), (ax >= 14 || az >= 14 ? Blocks.POLISHED_BLACKSTONE : Blocks.POLISHED_DEEPSLATE).defaultBlockState());
        p.set(x, f, z);
        if (at(PALACE_PILLARS, dx, dz)) {
            for (int y = f; y < top; y++) set(level, p.setY(y), Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState());
            set(level, p.setY(top), Blocks.GOLD_BLOCK.defaultBlockState());
            return;
        }
        if (at(CHAINED_LANTERNS, dx, dz)) {
            for (int y = top; y > top - 7; y--) set(level, p.setY(y), Blocks.CHAIN.defaultBlockState());
            set(level, p.setY(top - 7), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            p.setY(f);
        } else if (n2 < 0.03 && ax > 2) {
            set(level, p.setY(top), Blocks.SPORE_BLOSSOM.defaultBlockState());
            p.setY(f);
        }
        if (ax <= 2 && az <= 2) {
            // her dais, gold at its corners, candles burning there; she stands in its middle, her seat behind her
            boolean corner = ax == 2 && az == 2;
            set(level, p, (corner ? Blocks.GOLD_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
            if (corner) set(level, p.setY(f + 1), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
            if (dx == 0 && dz == -2) set(level, p.setY(f + 1), Blocks.DARK_OAK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, facing((lab.frontDir() + 2) % 4)));
            return;
        }
        if (at(CENSERS, dx, dz)) {
            set(level, p.setY(f - 1), Blocks.HAY_BLOCK.defaultBlockState());
            set(level, p.setY(f), Blocks.CAMPFIRE.defaultBlockState());
            return;
        }
        if (ax <= 1) {
            set(level, p, Blocks.RED_CARPET.defaultBlockState());
            return;
        }
        if (ax >= 12 && az <= 13) {
            set(level, p.set(x, f - 1, z), Blocks.MOSS_BLOCK.defaultBlockState());
            p.set(x, f, z);
            if (n < 0.28) tall(level, p, n2 < 0.4 ? Blocks.PEONY : n2 < 0.75 ? Blocks.ROSE_BUSH : Blocks.LILAC);
            else if (n < 0.43) set(level, p, (n2 < 0.5 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).defaultBlockState());
            else if (n < 0.7) set(level, p, smallFlower(n2));
            else set(level, p, petals(n2));
            return;
        }
        if (ax >= 10 && n < 0.25) set(level, p, petals(n2));
    }

    static BlockState petals(double n) {
        return Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (n * 4) % 4)
                .setValue(PinkPetalsBlock.FACING, facing((int) (n * 16) % 4));
    }

    /**
     * Her shrine, where she flees: a small room of mud brick, floored in dark wood, and against its
     * back wall an altar with two tablets on it, to her adoptive father and brother, Li Jing the
     * Pagoda-Bearing Heavenly King and his son Nezha (as Sun Wukong found them), candles between
     * them, incense smoking before it, and flowers in pots in its corners.
     */
    private static void shrine(WorldGenLevel level, Labyrinth lab, long seed, int x, int z, BlockPos.MutableBlockPos p) {
        Labyrinth.Room s = lab.shrine();
        int f = Labyrinth.THRONE;
        int x0 = lab.cellX(s.i0()) - 1, x1 = lab.cellX(s.i1()) + 1;
        int z0 = lab.cellZ(s.j0()) - 1, z1 = lab.cellZ(s.j1()) + 1;
        int g = lab.shrineGateDir();
        int d = switch (g) {
            case 0 -> z - z0;
            case 1 -> x1 - x;
            case 2 -> z1 - z;
            default -> x - x0;
        };
        int t = switch (g) {
            case 0 -> x - x0;
            case 1 -> z - z0;
            case 2 -> x1 - x;
            default -> z1 - z;
        };
        int depth = (g % 2 == 0 ? z1 - z0 : x1 - x0), across = (g % 2 == 0 ? x1 - x0 : z1 - z0);
        p.set(x, f, z);
        if (d == depth && t >= 3 && t <= across - 3) {
            set(level, p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            p.setY(f + 1);
            if (t == 3 || t == across - 3) {
                set(level, p, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, signFacing(g)));
                boolean father = t == 3;
                sign(level, p.immutable(), father ? "Revered Father" : "Revered Brother", father ? "Li Jing, the" : "Nezha, the",
                        father ? "Pagoda-Bearing" : "Third Prince", father ? "Heavenly King" : "");
            } else {
                set(level, p, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            }
            return;
        }
        if (d == depth - 2 && t == across / 2) {
            set(level, p.setY(f - 1), Blocks.HAY_BLOCK.defaultBlockState());
            set(level, p.setY(f), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
            return;
        }
        if ((d == 0 || d == depth) && (t == 0 || t == across)) {
            Block[] pots = {Blocks.POTTED_ALLIUM, Blocks.POTTED_PINK_TULIP, Blocks.POTTED_FLOWERING_AZALEA, Blocks.POTTED_LILY_OF_THE_VALLEY};
            set(level, p, pots[(t == 0 ? 0 : 1) + (d == 0 ? 0 : 2)].defaultBlockState());
            return;
        }
        if ((d == 2 || d == depth - 2) && (t == 2 || t == across - 2)) {
            set(level, p.setY(f + Labyrinth.SHRINE_HEIGHT - 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    /** A standing sign's rotation that faces the way {@code dir} (north, east, south, west). */
    private static int signFacing(int dir) {
        return switch (dir) {
            case 0 -> 8;
            case 1 -> 12;
            case 2 -> 0;
            default -> 4;
        };
    }

    /** Writes a sign's lines, front and back, waxed (as saved data: while the land is made the sign has no level to tell of a change). */
    static void sign(WorldGenLevel level, BlockPos at, String... lines) {
        if (!(level.getBlockEntity(at) instanceof SignBlockEntity sign)) return;
        SignText text = new SignText();
        for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
        var registries = level.registryAccess();
        var tag = sign.saveWithoutMetadata(registries);
        SignText.DIRECT_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), text)
                .result().ifPresent(l -> {
                    tag.put("front_text", l);
                    tag.put("back_text", l.copy());
                });
        tag.putBoolean("is_waxed", true);
        sign.loadWithComponents(tag, registries);
    }

    /**
     * Strews petals on the floor along the way from the palace to her shrine, as she flees down it
     * shedding them: one patch at the middle of each cell on the way, where the floor takes them and
     * nothing stands there. Returns how many were strewn.
     */
    public static int strewTrail(net.minecraft.world.level.Level level, Labyrinth lab) {
        int strewn = 0;
        int f = Labyrinth.THRONE;
        for (int[] c : lab.trail()) {
            int[] m = lab.cellMiddle(c[0], c[1]);
            for (int[] o : new int[][]{{1, 1}, {-1, 0}}) {
                BlockPos at = new BlockPos(m[0] + o[0], f, m[1] + o[1]);
                if (!level.isLoaded(at)) continue;
                BlockState here = level.getBlockState(at);
                if (!(here.isAir() || here.is(Blocks.RED_CARPET)) || !level.getBlockState(at.below()).isSolidRender(level, at.below())) continue;
                level.setBlock(at, petals(Labyrinth.noise(lab.seed, at.getX(), f, at.getZ())), Block.UPDATE_ALL);
                strewn++;
            }
        }
        return strewn;
    }
}
