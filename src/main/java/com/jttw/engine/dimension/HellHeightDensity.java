package com.jttw.engine.dimension;

import com.jttw.engine.world.hell.HellTerrain;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Hell's land, for its noise generator: the height of the land's top block at a column
 * ({@link HellTerrain#height}), the same at every height. The noise settings
 * ({@code worldgen/noise_settings/hell.json}) cache it per column and take away the height to
 * make the density (solid below the surface, open above), and fill what is open below the sea
 * level with water: the rivers.
 */
public record HellHeightDensity() implements DensityFunction.SimpleFunction {

    public static final KeyDispatchDataCodec<HellHeightDensity> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(HellHeightDensity::new));

    @Override
    public double compute(FunctionContext ctx) {
        return HellTerrain.height(ctx.blockX(), ctx.blockZ());
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 128;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
