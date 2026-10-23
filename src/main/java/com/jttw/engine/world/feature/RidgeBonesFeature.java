package com.jttw.engine.world.feature;

import com.jttw.engine.world.lion.RidgeShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The great bones that lie about Lion-Camel Ridge, the remains of what its demons ate: a ribcage
 * ({@link RibcageFeature}, White Bone Ridge's), anywhere on the ridge but on Lion-Camel City's ground
 * and the slope round it (the city's own bones are its blueprint's).
 */
public class RidgeBonesFeature extends Feature<NoneFeatureConfiguration> {

    private final RibcageFeature ribcage;

    public RidgeBonesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
        ribcage = new RibcageFeature(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos at = context.origin();
        int[] c = RidgeShape.ZONE.centre(context.level().getSeed(), at.getX(), at.getZ());
        if (c != null && RidgeShape.cityShare(at.getX() - c[0], at.getZ() - c[1]) < 1) return false;
        return ribcage.place(context);
    }
}
