package com.jttw.engine.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * White Bone Ridge's remains: a spire of bone standing out of the ground, sometimes leaning,
 * sometimes crowned with a skull; now and then a rib arching over to a second spire.
 */
public class BoneSpireFeature extends Feature<NoneFeatureConfiguration> {

    public BoneSpireFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean ground(BlockState s) {
        return s.is(com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get()) || s.is(com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get()) || s.is(com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get()) || s.is(com.jttw.engine.registry.ModBlocks.BONE_GRAVEL.get())
                || s.is(net.minecraft.tags.BlockTags.DIRT) || s.is(net.minecraft.tags.BlockTags.SAND)
                || s.is(net.minecraft.tags.BlockTags.TERRACOTTA);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        BlockPos base = context.origin();
        if (BoneMassifFeature.onLairFloor(level.getSeed(), base.getX(), base.getZ())) return false;
        if (!level.isEmptyBlock(base) || !ground(level.getBlockState(base.below()))) return false;

        int height = 2 + r.nextInt(5);
        Direction lean = r.nextInt(3) == 0 ? Direction.Plane.HORIZONTAL.getRandomDirection(r) : null;
        BlockState bone = com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState();
        BlockPos.MutableBlockPos p = base.mutable();
        for (int i = 0; i < height; i++) {
            if (!level.isEmptyBlock(p)) break;
            level.setBlock(p, bone, Block.UPDATE_CLIENTS);
            p.move(Direction.UP);
            if (lean != null && i == height / 2) p.move(lean);
        }
        if (r.nextInt(10) < 3 && level.isEmptyBlock(p)) {
            level.setBlock(p, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)), Block.UPDATE_CLIENTS);
        } else if (r.nextInt(10) < 2) {
            // a rib: arching over from the top, lying along its length
            Direction way = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockState rib = bone.setValue(RotatedPillarBlock.AXIS, way.getAxis());
            BlockPos.MutableBlockPos q = p.mutable().move(Direction.DOWN);
            for (int i = 0; i < 3 + r.nextInt(3); i++) {
                q.move(way);
                if (!level.isEmptyBlock(q)) break;
                level.setBlock(q, rib, Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }
}
