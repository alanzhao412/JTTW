package com.jttw.engine.world.feature;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Terrain helpers for zone features that reshape the land: where the land lies by its broad shape,
 * so every chunk agrees on a height worked out from a point alone, whichever is made first (as
 * {@link BoneMassifFeature} raises the ridge and levels its lair floor). With a zone's
 * {@code depth} (how far into the region) and {@code centre}, the pieces of a zone's own massif,
 * basin, trench or island.
 */
public final class ZoneTerrain {

    private ZoneTerrain() {}

    /** Where the terrain noise counts as solid: the threshold vanilla's own estimate of the surface uses. */
    private static final double SOLID = 0.390625;

    /** The land's broad shape (no caves, canyons or crags) in this level, for {@link #landAt}. */
    public static DensityFunction broadShape(WorldGenLevel level) {
        return level.getLevel().getChunkSource().randomState().router().initialDensityWithoutJaggedness();
    }

    /**
     * The land's surface at (x, z) by its broad shape alone, as vanilla estimates it before making the
     * terrain (NoiseChunk's preliminary surface level): no caves, cave mouths, canyons or crags in it.
     * Searched down in steps of 16, then of 4.
     */
    public static int landAt(DensityFunction shape, int x, int z, int minY, int maxY) {
        int y = maxY;
        while (y > minY && shape.compute(new DensityFunction.SinglePointContext(x, y, z)) <= SOLID) y -= 16;
        for (int fine = Math.min(maxY, y + 12); fine > y; fine -= 4) {
            if (shape.compute(new DensityFunction.SinglePointContext(x, fine, z)) > SOLID) return fine;
        }
        return Math.max(y, minY);
    }
}
