package com.jttw.engine.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A dead tree of White Bone Ridge: a bare trunk, bleached white or burnt black, with a few
 * crooked branches and not a leaf on it.
 */
public class DeadTreeFeature extends Feature<NoneFeatureConfiguration> {

    public DeadTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        BlockPos base = context.origin();
        if (!level.isEmptyBlock(base) || !level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) return false;

        Block wood = r.nextInt(3) == 0 ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_BIRCH_LOG;
        int height = 4 + r.nextInt(5);
        Direction lean = r.nextBoolean() ? Direction.Plane.HORIZONTAL.getRandomDirection(r) : null;
        BlockPos.MutableBlockPos p = base.mutable();
        BlockPos[] trunk = new BlockPos[height];
        int built = 0;
        for (int i = 0; i < height; i++) {
            if (!level.isEmptyBlock(p)) break;
            level.setBlock(p, wood.defaultBlockState(), Block.UPDATE_CLIENTS);
            trunk[built++] = p.immutable();
            if (lean != null && i == height * 2 / 3) {
                p.move(lean);
                if (!level.isEmptyBlock(p)) break;
                level.setBlock(p, wood.defaultBlockState().setValue(RotatedPillarBlock.AXIS, lean.getAxis()), Block.UPDATE_CLIENTS);
            }
            p.move(Direction.UP);
        }
        if (built < 3) return built > 0;

        int branches = 2 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            Direction way = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockPos.MutableBlockPos q = trunk[built / 2 + r.nextInt(built - built / 2)].mutable();
            int length = 1 + r.nextInt(3);
            for (int i = 0; i < length; i++) {
                boolean up = i > 0 && r.nextInt(5) < 2;
                q.move(up ? Direction.UP : way);
                if (!level.isEmptyBlock(q)) break;
                level.setBlock(q, wood.defaultBlockState().setValue(RotatedPillarBlock.AXIS, up ? Direction.Axis.Y : way.getAxis()), Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }
}
