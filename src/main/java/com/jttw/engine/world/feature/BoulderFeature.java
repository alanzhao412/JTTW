package com.jttw.engine.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A grey boulder fallen from the crags, half sunk in the ground: tuff and andesite, flecked with calcite. */
public class BoulderFeature extends Feature<NoneFeatureConfiguration> {

    public BoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static BlockState stone(RandomSource r) {
        int roll = r.nextInt(10);
        return roll < 5 ? Blocks.TUFF.defaultBlockState() : roll < 8 ? Blocks.ANDESITE.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        BlockPos base = context.origin();
        if (BoneMassifFeature.onLairFloor(level.getSeed(), base.getX(), base.getZ())) return false;
        if (!level.isEmptyBlock(base) || !level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) return false;

        float rx = 1.2f + r.nextFloat() * 1.6f, ry = 1.0f + r.nextFloat() * 1.2f, rz = 1.2f + r.nextFloat() * 1.6f;
        BlockPos centre = base.below(r.nextInt(2));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int ax = (int) Math.ceil(rx), ay = (int) Math.ceil(ry), az = (int) Math.ceil(rz);
        for (int dx = -ax; dx <= ax; dx++) for (int dy = -ay; dy <= ay; dy++) for (int dz = -az; dz <= az; dz++) {
            float d = dx * dx / (rx * rx) + dy * dy / (ry * ry) + dz * dz / (rz * rz);
            if (d > 1 + r.nextFloat() * 0.3f) continue;
            p.setWithOffset(centre, dx, dy, dz);
            level.setBlock(p, stone(r), Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
