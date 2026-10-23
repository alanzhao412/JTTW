package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fengdu.FengduCity;
import com.jttw.engine.world.fengdu.FengduShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * Makes Fengdu's country of the land it takes over: raises its hill ({@link FengduShape}) and lays
 * the Ghost City on its top, column by column ({@link FengduCity}), so the city comes out whole
 * across the chunks it spans, signs and all.
 * <ul>
 *   <li>the hill's <b>top</b>, level, is grass worn to earth and gravel round the walls;</li>
 *   <li>its <b>slopes</b> grass with patches of podzol and coarse earth, bare stone and tuff where
 *       they are steep, a dark spruce here and there; where it meets water it rises out of it;</li>
 *   <li>the <b>Road to the Yellow Springs</b> (黄泉路) climbs them from the south to the city's
 *       archway, paved, a soul lantern on a post every sixth block, by turns either side;</li>
 *   <li>round the hill, its <b>country</b> keeps the land's shape, gone gloomy: patches of podzol
 *       and coarse earth in the grass, dead bushes, a dead tree or a dark spruce here and there, now
 *       and then an old grave and its headstone.</li>
 * </ul>
 * Runs first among the zone's features; later ones find the ground with the live heightmaps.
 */
public class FengduFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = Zones.FENGDU;
    /** A step this high or more to a neighbour makes a face, not ground. */
    private static final int STEEP = 3;

    private static final int FACE = 0, GROUND = 1, TOP = 2, ROAD = 3;

    public FengduFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The city's floor (its ground's top) in the region centred at (cx, cz) in this level. */
    public static int cityFloor(WorldGenLevel level, int cx, int cz) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        return FengduShape.cityFloor(level.getSeed(), cx, cz, (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY),
                level.getSeaLevel() + FengduShape.ABOVE_SEA, maxY - FengduShape.UNDER_SKY);
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    private static int fine(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 41 + 9, z), 1000L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState rock(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 23 + z * 11) >> 5), 9);
        return band == 0 || band == 1 ? s(Blocks.TUFF) : band == 5 ? s(Blocks.ANDESITE) : s(Blocks.STONE);
    }

    /** The top block of a column of the hill, of its kind; {@code n} the ground's patchiness there ({@link FengduShape#patch}). */
    private static BlockState top(int kind, int x, int y, int z, double n) {
        return switch (kind) {
            case FACE -> rock(y, x, z);
            case ROAD -> {
                int q = hash(x, z, 7);
                yield q < 55 ? s(Blocks.STONE_BRICKS) : q < 75 ? s(Blocks.CRACKED_STONE_BRICKS) : q < 90 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.COBBLESTONE);
            }
            case TOP -> n > 0.55 ? s(Blocks.PODZOL) : n > 0.25 ? s(Blocks.COARSE_DIRT) : fine(x, z, 8) < 12 ? s(Blocks.GRAVEL) : s(Blocks.GRASS_BLOCK);
            default -> n > 0.45 ? s(Blocks.PODZOL) : n < -0.6 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK);
        };
    }

    private static BlockState under(int kind, int x, int y, int z) {
        if (kind == FACE) return rock(y, x, z);
        return s(Blocks.DIRT);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        boolean inside = false;
        for (int i = 0; i < 18 && !inside; i += 17) for (int j = 0; j < 18; j += 17) inside |= ZONE.depth(seed, x0 + i - 1, z0 + j - 1) > 0;
        if (!inside) for (int i = 0; i < 16 && !inside; i += 3) for (int j = 0; j < 16; j += 3) inside |= ZONE.depth(seed, x0 + i, z0 + j) > 0;
        if (!inside) return false;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        int minY = level.getMinBuildHeight() + 1, maxY = level.getMaxBuildHeight() - 2;
        boolean hill = Math.abs(x0 + 8 - c[0]) <= FengduShape.REACH + 12 && Math.abs(z0 + 8 - c[1]) <= FengduShape.REACH + 12;
        int floor = hill ? cityFloor(level, c[0], c[1]) : 0;
        // the height each column (and the ring round the chunk) is brought to
        int[][] target = new int[18][18];
        int[][] ground = new int[18][18];
        int[][] surface = new int[18][18];
        double[][] share = new double[18][18];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            int x = x0 + i - 1, z = z0 + j - 1;
            int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; // under any water
            ground[i][j] = g;
            surface[i][j] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            share[i][j] = hill ? FengduShape.share(seed, x, z, x - c[0], z - c[1]) : 1;
            int t = FengduShape.groundAt(g, floor, share[i][j]);
            boolean hillOrRoad = share[i][j] < 1 || hill && FengduShape.onRoad(x - c[0], z - c[1]);
            if (hillOrRoad && surface[i][j] > g) t = Math.max(t, surface[i][j]); // over water the hill (and the road) rise out of it: none is left hanging
            target[i][j] = Mth.clamp(t, minY, maxY);
        }
        boolean any = false;
        java.util.List<BlockPos> trees = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                int g = ground[dx + 1][dz + 1], sur = surface[dx + 1][dz + 1], t = target[dx + 1][dz + 1];
                int lx = x - c[0], lz = z - c[1];
                any = true;
                double n = FengduShape.patch(seed, x, z);
                boolean road = hill && FengduShape.onRoad(lx, lz), onTop = FengduShape.onTop(lx, lz);
                // a tree's crown kept within the chunk, and off the road
                boolean inner = dx >= 3 && dx <= 12 && dz >= 3 && dz <= 12 && !(FengduShape.onRoad(0, lz) && Math.abs(lx) <= FengduShape.ROAD_HALF + 4);
                if (share[dx + 1][dz + 1] >= 1 && !road) {
                    if (sur <= g) country(context, x, g, z, n, inner, trees, p);
                    continue;
                }
                int step = Math.max(Math.max(Math.abs(target[dx][dz + 1] - t), Math.abs(target[dx + 2][dz + 1] - t)),
                        Math.max(Math.abs(target[dx + 1][dz] - t), Math.abs(target[dx + 1][dz + 2] - t)));
                int kind = road ? ROAD : onTop ? TOP : step >= STEEP ? FACE : GROUND;
                // cut down to it (water over it is built over: see the targets)
                for (int y = Math.max(t + 1, minY); y <= Math.max(sur, g); y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                // built up to it, or its top few blocks made over
                for (int y = Math.max(minY, Math.min(g + 1, t - 3)); y <= t; y++) {
                    if (y < t && y <= g && !level.getBlockState(p.set(x, y, z)).blocksMotion()) continue; // a cave just under it left open
                    BlockState st = y == t ? top(kind, x, y, z, n) : y > t - 4 ? under(kind, x, y, z) : rock(y, x, z);
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                if (!level.getFluidState(p.set(x, t + 1, z)).isEmpty()) continue;
                // the Ghost City, its share in this column, and its signs
                if (FengduCity.covers(lx, lz)) {
                    for (int ly = FengduCity.BOTTOM; ly <= FengduCity.TOP; ly++) {
                        BlockState st = FengduCity.at(lx, ly, lz);
                        int y = floor + ly;
                        if (st != null && y >= minY && y <= maxY) level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                    }
                    for (FengduCity.Sign sign : FengduCity.SIGNS) {
                        if (sign.x() == lx && sign.z() == lz) FengduCity.putSign(level, new BlockPos(x, floor + sign.y(), z), sign);
                    }
                } else if (!road && (onTop || kind == GROUND)) {
                    if (fine(x, z, 81) < 40 && level.getBlockState(p.set(x, t, z)).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p.set(x, t + 1, z), s(Blocks.SHORT_GRASS), Block.UPDATE_CLIENTS);
                    } else if (!onTop && inner && fine(x, z, 82) < 7) {
                        trees.add(new BlockPos(x, t + 1, z)); // a dark spruce on the slope, as cypresses stand round a temple hill
                    }
                }
                // soul lanterns on posts along the road, every sixth block, by turns either side
                if (Math.abs(lx) == FengduShape.ROAD_HALF + 1 && FengduShape.onRoad(0, lz) && !FengduCity.covers(lx, lz)
                        && Math.floorMod(lz, 6) == 0 && (Math.floorMod(lz, 12) == 0) == (lx > 0)) {
                    level.setBlock(p.set(x, t + 1, z), s(Blocks.DARK_OAK_FENCE), Block.UPDATE_CLIENTS);
                    level.setBlock(p.set(x, t + 2, z), s(Blocks.DARK_OAK_FENCE), Block.UPDATE_CLIENTS);
                    level.setBlock(p.set(x, t + 3, z), s(Blocks.SOUL_LANTERN), Block.UPDATE_CLIENTS);
                }
            }
        }
        // the trees, once the chunk's ground is all made
        var spruce = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .getHolder(net.minecraft.data.worldgen.features.TreeFeatures.SPRUCE);
        for (BlockPos at : trees) {
            if (!level.getBlockState(at).isAir()) continue;
            if (spruce.isPresent()) spruce.get().value().place(level, context.chunkGenerator(), context.random(), at);
        }
        return any;
    }

    /**
     * Fengdu's country round its hill, the land's shape kept: patches gone dark, dead bushes, a dark
     * spruce or a dead tree now and then ({@code trees}, if {@code inner}: well inside the chunk), and
     * now and then an old grave.
     */
    private static void country(FeaturePlaceContext<NoneFeatureConfiguration> context, int x, int g, int z, double n, boolean inner,
                                java.util.List<BlockPos> trees, BlockPos.MutableBlockPos p) {
        WorldGenLevel level = context.level();
        BlockState top = level.getBlockState(p.set(x, g, z));
        if (!top.is(Blocks.GRASS_BLOCK)) return;
        if (n > 0.5) level.setBlock(p, s(Blocks.PODZOL), Block.UPDATE_CLIENTS);
        else if (n < -0.65) level.setBlock(p, s(Blocks.COARSE_DIRT), Block.UPDATE_CLIENTS);
        if (!level.getBlockState(p.set(x, g + 1, z)).isAir() || !level.getFluidState(p).is(Fluids.EMPTY)) return;
        int r = fine(x, z, 72);
        if (r < 2) {
            // an old grave: a mound, its headstone at its head
            level.setBlock(p.set(x, g, z), s(Blocks.ROOTED_DIRT), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, g + 1, z), s(r == 0 ? Blocks.MOSSY_STONE_BRICK_WALL : Blocks.STONE_BRICK_WALL), Block.UPDATE_CLIENTS);
        } else if (r < 14) {
            level.setBlock(p.set(x, g + 1, z), s(Blocks.DEAD_BUSH), Block.UPDATE_CLIENTS);
        } else if (r < 16 && inner) {
            // a dead tree, bare and black or bleached
            new DeadTreeFeature(NoneFeatureConfiguration.CODEC).place(new FeaturePlaceContext<>(java.util.Optional.empty(), level,
                    context.chunkGenerator(), context.random(), new BlockPos(x, g + 1, z), NoneFeatureConfiguration.INSTANCE));
        } else if (r < 22 && inner) {
            trees.add(new BlockPos(x, g + 1, z));
        }
    }
}
