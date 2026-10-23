package com.jttw.engine.world.hell;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * What stands on Hell's plains, here and there: patches of soul fire burning on soul soil, spires
 * of black stone and basalt, heaps of bones. Never on the works ({@link HellWorks#occupied}) nor
 * near where travellers arrive.
 */
public class HellDecorFeature extends Feature<NoneFeatureConfiguration> {

    /** No decoration this near the arrival. */
    public static final double CLEAR_OF_ARRIVAL = 80;

    public HellDecorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        boolean any = false;
        for (int k = 0; k < 3; k++) {
            int x = x0 + 2 + r.nextInt(12), z = z0 + 2 + r.nextInt(12);
            if (HellWorks.occupied(x, z) || Math.hypot(x - HellTerrain.ARRIVAL_X, z - HellTerrain.ARRIVAL_Z) < CLEAR_OF_ARRIVAL) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            if (top < HellTerrain.SEA_LEVEL || !level.getFluidState(new BlockPos(x, top, z)).isEmpty()) continue;
            float roll = r.nextFloat();
            if (k == 0 && roll < 0.45f) any |= soulFire(level, x, top, z, x0, z0, r);
            else if (k == 1 && roll < 0.14f) any |= spire(level, x, top, z, r);
            else if (k == 2 && roll < 0.12f) any |= bones(level, x, top, z, r);
        }
        return any;
    }

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    /** A patch of soul fire, burning on soul soil. */
    private static boolean soulFire(WorldGenLevel level, int x, int top, int z, int x0, int z0, RandomSource r) {
        int n = 3 + r.nextInt(5);
        for (int i = 0; i < n; i++) {
            int px = Math.max(x0, Math.min(x0 + 15, x + r.nextInt(5) - 2)), pz = Math.max(z0, Math.min(z0 + 15, z + r.nextInt(5) - 2));
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, px, pz) - 1;
            if (Math.abs(y - top) > 2 || y < HellTerrain.SEA_LEVEL) continue;
            BlockPos p = new BlockPos(px, y, pz);
            if (!level.getBlockState(p).isSolid()) continue;
            set(level, p, Blocks.SOUL_SOIL.defaultBlockState());
            set(level, p.above(), Blocks.SOUL_FIRE.defaultBlockState());
        }
        return true;
    }

    /** A spire of black stone, basalt banded, leaning a little. */
    private static boolean spire(WorldGenLevel level, int x, int top, int z, RandomSource r) {
        int h = 7 + r.nextInt(10);
        double lean = (r.nextDouble() - 0.5) * 0.15, leanZ = (r.nextDouble() - 0.5) * 0.15;
        for (int y = 0; y < h; y++) {
            double rad = 2.2 * (1 - (double) y / h) + 0.4;
            int cx = x + (int) Math.round(lean * y), cz = z + (int) Math.round(leanZ * y);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > rad * rad) continue;
                // keep within the chunk's reach (the spire's foot is at least two blocks inside)
                BlockState s = (y + dx + dz) % 5 == 0 ? Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                        : r.nextFloat() < 0.1f ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
                set(level, new BlockPos(cx + dx, top + 1 + y, cz + dz), s);
            }
        }
        return true;
    }

    /** A heap of bones, half sunk in the ash. */
    private static boolean bones(WorldGenLevel level, int x, int top, int z, RandomSource r) {
        for (int i = 0; i < 6; i++) {
            BlockPos p = new BlockPos(x + r.nextInt(3) - 1, top + (i < 3 ? 0 : 1), z + r.nextInt(3) - 1);
            set(level, p, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.values()[r.nextInt(3)]));
        }
        return true;
    }
}
