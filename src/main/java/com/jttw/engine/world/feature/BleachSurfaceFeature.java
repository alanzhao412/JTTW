package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Bleaches White Bone Ridge's ground: in each column of the chunk that lies in the ridge, the top
 * few blocks of soil, sand and clay turn to calcite, with patches of bone, coarse dirt (where dead
 * brush can root) and white terracotta. The terrain keeps the shape of the land it took over.
 * Runs before vegetation, so what grows there grows on the bleached ground.
 */
public class BleachSurfaceFeature extends Feature<NoneFeatureConfiguration> {

    private static final int DEPTH = 4;

    public BleachSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean bleachable(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.TERRACOTTA)
                || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.GRAVEL);
    }

    /** What the top block becomes at (x, z): mostly calcite, in patches of bone, coarse dirt and white clay. */
    static BlockState topAt(int x, int z) {
        long patch = Mth.getSeed(x >> 2, 0, z >> 2); // 4x4 patches
        int roll = (int) Math.floorMod(patch, 100L);
        if (roll < 10) return Blocks.BONE_BLOCK.defaultBlockState();
        if (roll < 32) return Blocks.COARSE_DIRT.defaultBlockState();
        if (roll < 44) return Blocks.WHITE_TERRACOTTA.defaultBlockState();
        return Blocks.CALCITE.defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                p.set(x, top, z);
                if (!level.getBiome(p).is(Zones.WHITE_BONE_RIDGE)) continue;
                for (int d = 0; d < DEPTH; d++) {
                    p.set(x, top - d, z);
                    BlockState s = level.getBlockState(p);
                    if (!bleachable(s)) {
                        if (d == 0 && s.isAir()) continue;
                        break;
                    }
                    level.setBlock(p, d == 0 ? topAt(x, z) : Blocks.CALCITE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    any = true;
                }
            }
        }
        return any;
    }
}
