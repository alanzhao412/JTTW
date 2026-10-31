package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * Raises Flower-Fruit Mountain (花果山) out of the sea it takes over ({@link IslandShape}), column by
 * column, so it comes out whole whichever chunk is made first: the island's terraced flanks and
 * spurs, its peak and the immortal stone egg on it, the sheer rock pillars on its flanks and in the
 * shallows, the beaches and shoals; on its south face the great cliff, the river down its gorge to
 * the lip, the sheet of water falling past the ledge into the pool, the pool's stream to the sea;
 * and behind the falling water, in the cliff, the Water Curtain Cave ({@link CavePlan}): its mouth,
 * the iron bridge, the stone gate and its tablet, the great hall and its rooms, the false king's
 * sanctum.
 * <ul>
 *   <li><b>faces</b> (a step of three or more to a neighbour) show the mountain's rock: grey stone
 *       banded with white calcite, andesite and tuff, moss on the lower faces;</li>
 *   <li><b>ground</b> is grass with moss and podzol, coarse dirt in places; the summit stone;</li>
 *   <li><b>the pillars</b>: banded stone to their heads, grass and moss on top;</li>
 *   <li><b>the shore</b>: sand and gravel; the shoals sand, clay and gravel under the water.</li>
 * </ul>
 * It runs first among the zone's features; later ones find the ground with the live heightmaps.
 */
public class FlowerFruitIslandFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = IslandShape.ZONE;
    private static final int STEEP = 3;

    public FlowerFruitIslandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 1000L);
    }

    /**
     * The mountain's rock: grey stone in long, uneven strata (their line wandering with the ground's
     * grain), a seam of white calcite now and then, andesite and tuff in patches, mossy where it is
     * low and wet.
     */
    public static BlockState rock(int y, int x, int z) {
        int cell = hash(x >> 3, z >> 3, 31) / 40;                  // the grain: strata shift cell by cell
        int band = Math.floorMod(y + ((x * 7 + z * 5) >> 4) + cell, 29);
        int patch = hash(x >> 2, (z >> 2) + y / 5, 37);
        if (band == 0) return s(Blocks.CALCITE);
        if (band == 9 || band == 10) return patch < 500 ? s(Blocks.ANDESITE) : s(Blocks.STONE);
        if (band == 18) return patch < 600 ? s(Blocks.TUFF) : s(Blocks.STONE);
        if (patch < 70) return s(Blocks.ANDESITE);
        if (patch < 110) return s(Blocks.COBBLESTONE);
        if (y < IslandShape.SEA + 30 && patch > 900) return s(Blocks.MOSSY_COBBLESTONE);
        return s(Blocks.STONE);
    }

    /** The water as it falls in the sheet: falling, so it streams down. */
    private static BlockState falling() {
        return Fluids.WATER.getFlowing(8, true).createLegacyBlock();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        boolean inside = false;
        for (int i = 0; i < 18 && !inside; i += 3) for (int j = 0; j < 18; j += 3) inside |= ZONE.depth(seed, x0 + i - 1, z0 + j - 1) > 0;
        if (!inside) return false;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        int maxY = level.getMaxBuildHeight() - 2, minY = level.getMinBuildHeight() + 1;
        // the height each column of the chunk and the ring round it is brought to (the ring's by its own sea floor guessed from the chunk's edge)
        int[][] native_ = new int[16][16], surface = new int[16][16];
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            native_[i][j] = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x0 + i, z0 + j) - 1;
            surface[i][j] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x0 + i, z0 + j) - 1;
        }
        int[][] top = new int[18][18];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            int nf = native_[Mth.clamp(i - 1, 0, 15)][Mth.clamp(j - 1, 0, 15)];
            top[i][j] = Math.min(maxY - 20, IslandShape.top(seed, x0 + i - 1 - c[0], z0 + j - 1 - c[1], nf));
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz, lx = x - c[0], lz = z - c[1];
                if (ZONE.depth(seed, x, z) <= 0) continue;
                if (!ZONE.isAt(level, p.set(x, surface[dx][dz], z))) continue;
                int t = top[dx + 1][dz + 1];
                int step = Math.max(Math.max(Math.abs(top[dx][dz + 1] - t), Math.abs(top[dx + 2][dz + 1] - t)),
                        Math.max(Math.abs(top[dx + 1][dz] - t), Math.abs(top[dx + 1][dz + 2] - t)));
                double u = IslandShape.out(seed, lx, lz);
                IslandShape.Pillar pillar = IslandShape.pillarAt(seed, lx, lz);
                // the water standing on it: the sea, the pool, the river, the stream
                int water = Integer.MIN_VALUE;
                boolean bed = false;
                if (IslandShape.pool(lx, lz)) {
                    t = IslandShape.BASIN_Y - IslandShape.poolDepth(lx, lz);
                    water = IslandShape.BASIN_Y;
                    bed = true;
                } else if (IslandShape.river(lx, lz) && !IslandShape.overhang(lx, lz)) {
                    water = IslandShape.LIP_Y - 1;
                    bed = true;
                } else if (pillar == null && t >= IslandShape.SEA && IslandShape.stream(seed, lx, lz)) {
                    t -= 1;
                    water = t + 1;
                    bed = true;
                }
                if (t < IslandShape.SEA) water = Math.max(water, IslandShape.SEA);
                int ground = native_[dx][dz];
                int from = Math.max(minY, Math.min(ground, t - 4));
                boolean face = step >= STEEP;
                for (int y = from; y <= t; y++) {
                    BlockState st;
                    if (y == t) st = topBlock(seed, x, y, z, lx, lz, u, face, bed, pillar != null, t < IslandShape.SEA);
                    else if (face || y < t - 3) st = rock(y, x, z);
                    else if (t < IslandShape.SEA + 2) st = s(Blocks.SAND);
                    else st = s(Blocks.DIRT);
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                int clearTo = Math.max(surface[dx][dz], water);
                for (int y = t + 1; y <= clearTo && y <= maxY; y++) {
                    level.setBlock(p.set(x, y, z), y <= water ? s(Blocks.WATER) : s(Blocks.AIR), Block.UPDATE_CLIENTS);
                }
                if (bed && water > IslandShape.SEA && hash(x, z, 3) < 60) level.scheduleTick(p.set(x, water, z).immutable(), Fluids.WATER, 0);
                // the lip: the river runs out over the cliff on a shelf of rock, the ledge clear under it
                if (IslandShape.overhang(lx, lz)) {
                    for (int y = IslandShape.LIP_Y - 4; y <= IslandShape.LIP_Y - 2; y++) level.setBlock(p.set(x, y, z), rock(y, x, z), Block.UPDATE_CLIENTS);
                    if (IslandShape.river(lx, lz)) level.setBlock(p.set(x, IslandShape.LIP_Y - 1, z), s(Blocks.WATER), Block.UPDATE_CLIENTS);
                    else level.setBlock(p.set(x, IslandShape.LIP_Y - 1, z), s(Blocks.MOSSY_COBBLESTONE), Block.UPDATE_CLIENTS);
                }
                // the falling sheet
                if (IslandShape.curtain(lx, lz)) {
                    for (int y = IslandShape.BASIN_Y + 1; y <= IslandShape.LIP_Y - 1; y++) level.setBlock(p.set(x, y, z), falling(), Block.UPDATE_CLIENTS);
                    level.scheduleTick(p.set(x, IslandShape.LIP_Y - 1, z).immutable(), Fluids.WATER, 0);
                }
                // the cave behind it
                if (CavePlan.covers(lx, lz)) {
                    for (int ly = CavePlan.Y_MIN; ly <= CavePlan.Y_MAX; ly++) {
                        BlockState st = CavePlan.at(lx, ly, lz);
                        if (st == null) continue;
                        int y = IslandShape.BASIN_Y + ly;
                        // where the cave is open but the mountain is not over it (never: the plan keeps under it), leave the sky
                        if (y > t && st.isAir()) continue;
                        level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                        if (st.getBlock() instanceof LiquidBlock) level.scheduleTick(p.immutable(), Fluids.WATER, 0);
                    }
                }
                // the immortal stone egg on the summit
                int ex = lx - IslandShape.SUMMIT_X, ez = lz - IslandShape.SUMMIT_Z;
                if (Math.abs(ex) <= 7 && Math.abs(ez) <= 7) {
                    for (int dy = 0; dy <= 20; dy++) {
                        int e = IslandShape.egg(ex, dy, ez);
                        if (e == 0) continue;
                        int y = IslandShape.SUMMIT_Y + 1 + dy;
                        int q = hash(x * 7 + dy, z, 11);
                        BlockState st = e == 2 ? (q < 500 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.TUFF))
                                : dy <= 1 && q < 450 ? s(Blocks.MOSSY_COBBLESTONE)
                                : q < 420 ? s(Blocks.CALCITE) : q < 700 ? s(Blocks.SMOOTH_STONE) : q < 880 ? s(Blocks.DIORITE) : s(Blocks.STONE);
                        level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                    }
                }
                any = true;
            }
        }
        return any;
    }

    /** The top block of a column of the island. */
    private static BlockState topBlock(long seed, int x, int y, int z, int lx, int lz, double u, boolean face, boolean bed, boolean pillar, boolean sea) {
        int q = hash(x, z, 7);
        if (sea || bed) return q < 450 ? s(Blocks.SAND) : q < 700 ? s(Blocks.GRAVEL) : q < 850 ? s(Blocks.CLAY) : s(Blocks.MOSSY_COBBLESTONE);
        if (Math.hypot(lx - IslandShape.SUMMIT_X, lz - IslandShape.SUMMIT_Z) <= IslandShape.SUMMIT_FLAT) {
            return q < 500 ? s(Blocks.STONE) : q < 750 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.MOSS_BLOCK);
        }
        if (IslandShape.ledge(lx, lz) || (lz > IslandShape.CLIFF_Z && Math.abs(lx) <= 2)) {
            return q < 600 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
        }
        if (face && !pillar) {
            if (y < IslandShape.SEA + 40 && q < 260) return s(Blocks.MOSSY_COBBLESTONE);
            return rock(y, x, z);
        }
        if (y <= IslandShape.SEA + 2 && u > 0.9) return q < 700 ? s(Blocks.SAND) : s(Blocks.GRAVEL);
        if (q < 70) return s(Blocks.COARSE_DIRT);
        if (q < 150) return s(Blocks.MOSS_BLOCK);
        if (q < 200 && y > IslandShape.SEA + 60) return s(Blocks.PODZOL);
        return s(Blocks.GRASS_BLOCK);
    }
}
