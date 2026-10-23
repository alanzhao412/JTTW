package com.jttw.engine.world.feature;

import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.Sea;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * What lives in the trench, once it is sunk ({@link TrenchFeature}): kelp in forests, seagrass, sea
 * pickles glowing on the floor in clusters (the only light down there but the palace's), glow
 * lichen on the walls, now and then a vent of black stone with magma at its foot, or a spire of
 * deepslate standing up off the floor. Nothing on the palace's plinth or its road.
 *
 * <p>The vanilla sea's own plants are not made here: they find the floor with the worldgen
 * heightmaps, which still hold the sea floor from before the trench sank. This finds it with the
 * live one, column by column of its own chunk.
 */
public class TrenchLifeFeature extends Feature<NoneFeatureConfiguration> {

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    public TrenchLifeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0x11FE5EA5L));
            noiseSeed = seed;
        }
        return noise;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int minY = level.getMinBuildHeight();
        BlockPos throne = TrenchFeature.throne(seed, x0 + 8, z0 + 8, minY);
        if (throne == null) return false;
        Sea sea = Sea.of(throne.getX(), throne.getZ());
        int deep = TrenchFeature.floorY(minY) + com.jttw.engine.world.waters.TrenchShape.MOUNDS + 12; // the trench's lower reaches
        int[][] floor = new int[16][16];
        boolean[][] free = new boolean[16][16];
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            floor[dx][dz] = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
            int[] l = DragonPalace.toLocal(sea, x - throne.getX(), z - throne.getZ());
            boolean palace = DragonPalace.covers(l[0], l[1]) || DragonPalace.onRoad(l[0], l[1])
                    || DragonPalace.covers(l[0] + 3, l[1]) || DragonPalace.covers(l[0] - 3, l[1]);
            free[dx][dz] = !palace && TrenchFeature.ZONE.depth(seed, x, z) > 0 && floor[dx][dz] < deep + 60
                    && level.getBlockState(p.set(x, floor[dx][dz] + 1, z)).is(Blocks.WATER)
                    && TrenchFeature.ZONE.isAt(level, p);
            any |= free[dx][dz];
        }
        if (!any) return false;
        SimplexNoise n = noise(seed);
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            if (!free[dx][dz]) continue;
            int x = x0 + dx, z = z0 + dz, y = floor[dx][dz] + 1;
            double forest = n.getValue(x / 40.0, z / 40.0);
            float r = random.nextFloat();
            if (forest > 0.25 && r < 0.3) kelp(level, p, x, y, z, 4 + random.nextInt(18));
            else if (r < 0.12) level.setBlock(p.set(x, y, z), Blocks.SEAGRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
            else if (r < 0.135 && level.getBlockState(p.set(x, y - 1, z)).isFaceSturdy(level, p, Direction.UP)) {
                level.setBlock(p.set(x, y, z), Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + random.nextInt(4)), Block.UPDATE_CLIENTS);
            }
            // glow lichen up the walls: on the faces of higher neighbours
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int nx = dx + d.getStepX(), nz = dz + d.getStepZ();
                if (nx < 0 || nz < 0 || nx > 15 || nz > 15) continue;
                for (int wy = y + 1; wy <= floor[nx][nz]; wy++) {
                    if (random.nextFloat() >= 0.035f) continue;
                    if (!level.getBlockState(p.set(x, wy, z)).is(Blocks.WATER)) continue;
                    level.setBlock(p, Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(d), true)
                            .setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_CLIENTS);
                }
            }
        }
        // now and then a vent, or a spire, kept off the chunk's edges
        int cx = 3 + random.nextInt(10), cz = 3 + random.nextInt(10);
        if (clear(free, cx, cz, 2) && floor[cx][cz] < deep) {
            float r = random.nextFloat();
            if (r < 0.18f) vent(level, p, random, x0 + cx, floor[cx][cz], z0 + cz);
            else if (r < 0.30f) spire(level, p, random, x0 + cx, floor[cx][cz], z0 + cz);
        }
        return true;
    }

    /** Whether every column within {@code r} of (cx, cz) is free: off the palace and its road, in the trench. */
    private static boolean clear(boolean[][] free, int cx, int cz, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (!free[cx + dx][cz + dz]) return false;
        }
        return true;
    }

    private static void kelp(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, int height) {
        for (int k = 0; k < height; k++) {
            if (!level.getBlockState(p.set(x, y + k, z)).is(Blocks.WATER) || !level.getBlockState(p.set(x, y + k + 1, z)).is(Blocks.WATER)) {
                height = k;
                break;
            }
        }
        for (int k = 0; k < height; k++) {
            BlockState s = k == height - 1 ? Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, 25) : Blocks.KELP_PLANT.defaultBlockState();
            level.setBlock(p.set(x, y + k, z), s, Block.UPDATE_CLIENTS);
        }
    }

    /** A vent: a chimney of basalt and blackstone, magma about its foot and smouldering at its mouth. */
    private static void vent(WorldGenLevel level, BlockPos.MutableBlockPos p, RandomSource random, int x, int ground, int z) {
        int h = 3 + random.nextInt(6);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if ((dx != 0 || dz != 0) && random.nextFloat() < 0.7f) level.setBlock(p.set(x + dx, ground, z + dz), Blocks.MAGMA_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int k = 1; k <= h; k++) {
            level.setBlock(p.set(x, ground + k, z), (k == h ? Blocks.MAGMA_BLOCK : random.nextBoolean() ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState(), Block.UPDATE_CLIENTS);
            if (k <= h / 2) for (Direction d : Direction.Plane.HORIZONTAL) {
                if (random.nextFloat() < 0.5f) level.setBlock(p.set(x + d.getStepX(), ground + k, z + d.getStepZ()), Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** A spire of deepslate and tuff, thick at its foot and tapering up off the floor. */
    private static void spire(WorldGenLevel level, BlockPos.MutableBlockPos p, RandomSource random, int x, int ground, int z) {
        int h = 10 + random.nextInt(18);
        for (int k = 1; k <= h; k++) {
            double radius = 2.2 * (1 - (double) k / (h + 1)) + 0.3;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                BlockState s = (ground + k) % 7 == 0 ? Blocks.TUFF.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
                level.setBlock(p.set(x + dx, ground + k, z + dz), s, Block.UPDATE_CLIENTS);
            }
        }
    }
}
