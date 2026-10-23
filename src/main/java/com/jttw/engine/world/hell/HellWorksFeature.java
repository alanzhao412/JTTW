package com.jttw.engine.world.hell;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Lays this chunk's part of Hell's works ({@link HellWorks}): the plaza, the road, the bridge, the Ten Courts. */
public class HellWorksFeature extends Feature<NoneFeatureConfiguration> {

    public HellWorksFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        HellWorks.build(Site.chunk(context.level(), origin.getX() & ~15, origin.getZ() & ~15), context.level().getSeed());
        return true;
    }
}
