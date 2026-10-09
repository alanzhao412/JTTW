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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The carcass of something vast: a spine of bone lying along the ground, its ribs arching up into
 * a cage you can walk through, a skull at one end. Only on ground flat enough to lie along.
 */
public class RibcageFeature extends Feature<NoneFeatureConfiguration> {

    /** The most the ground may rise or fall along the spine. */
    private static final int UNEVEN = 4;

    public RibcageFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        BlockPos origin = context.origin();
        if (BoneMassifFeature.onLairFloor(level.getSeed(), origin.getX(), origin.getZ())) return false;
        Direction along = r.nextBoolean() ? Direction.EAST : Direction.SOUTH;
        Direction side = along.getClockWise();
        int length = 11 + r.nextInt(6), half = length / 2;

        // the ground under each bone of the spine, and whether it is flat enough
        int[] ground = new int[length];
        for (int i = 0; i < length; i++) {
            BlockPos at = origin.relative(along, i - half);
            ground[i] = level.getHeight(Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ());
            if (Math.abs(ground[i] - origin.getY()) > UNEVEN) return false;
        }
        int sunk = r.nextInt(5) < 2 ? 1 : 0;
        BlockState spine = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
        BlockState rib = Blocks.BONE_BLOCK.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < length; i++) {
            BlockPos bone = origin.relative(along, i - half).atY(ground[i] - sunk);
            level.setBlock(bone, spine, Block.UPDATE_CLIENTS);
            // ribs every other bone through the middle, widest at the chest
            if (i < 2 || i > length - 4 || i % 2 != 0) continue;
            double radius = 3 + 2.2 * Math.sin(Math.PI * i / length);
            double reach = Math.PI * (0.72 + 0.1 * r.nextDouble()); // a gap left at the top
            for (int sign = -1; sign <= 1; sign += 2) {
                for (double a = 0.15; a <= reach; a += 0.08) {
                    int out = (int) Math.round(radius * Math.sin(a)) * sign;
                    int up = (int) Math.round(radius - radius * Math.cos(a));
                    p.setWithOffset(bone, side.getStepX() * out, up, side.getStepZ() * out);
                    if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) level.setBlock(p, rib, Block.UPDATE_CLIENTS);
                }
            }
        }
        // the skull, at the front: a lump of bone with two hollow eyes looking on along the spine
        BlockPos head = origin.relative(along, length - half + 1).atY(ground[length - 1] - sunk + 1);
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 3 && r.nextBoolean()) continue;
            level.setBlock(p.setWithOffset(head, dx, dy, dz), rib, Block.UPDATE_CLIENTS);
        }
        BlockPos face = head.relative(along);
        level.setBlock(face.relative(side), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(face.relative(side.getOpposite()), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
}
