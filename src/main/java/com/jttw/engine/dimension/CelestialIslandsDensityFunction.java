package com.jttw.engine.dimension;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Random;

/**
 * Custom density function that generates small floating cloud islands
 * scattered across the dimension with organic, noisy shapes.
 */
public record CelestialIslandsDensityFunction() implements DensityFunction.SimpleFunction {

    public static final KeyDispatchDataCodec<CelestialIslandsDensityFunction> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(CelestialIslandsDensityFunction::new));

    private static final int CELL_SIZE = 300;
    private static final int MIN_RADIUS = 8;
    private static final int MAX_RADIUS = 25;
    private static final double ISLAND_CHANCE = 0.6;
    private static final int BASE_Y = 64;
    private static final int Y_VARIANCE = 20;
    private static final int THICKNESS = 6;

    // Noise parameters for organic shape distortion
    private static final double EDGE_WARP = 0.35;     // how much the edge wobbles (0 = perfect circle)
    private static final double SURFACE_BUMP = 2.5;    // height variation on the surface
    private static final double NOISE_SCALE = 0.15;    // frequency of edge noise

    @Override
    public double compute(FunctionContext ctx) {
        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();

        int cellX = Math.floorDiv(x, CELL_SIZE);
        int cellZ = Math.floorDiv(z, CELL_SIZE);

        double best = -1.0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double d = sampleCell(cellX + dx, cellZ + dz, x, y, z);
                if (d > best) best = d;
            }
        }

        return best;
    }

    private static double sampleCell(int cx, int cz, int x, int y, int z) {
        long seed = cx * 341873128712L + cz * 132897987541L;
        Random rand = new Random(seed);

        if (rand.nextDouble() > ISLAND_CHANCE) return -1.0;

        // Island center within cell
        int centerX = cx * CELL_SIZE + rand.nextInt(CELL_SIZE);
        int centerZ = cz * CELL_SIZE + rand.nextInt(CELL_SIZE);
        int radius = MIN_RADIUS + rand.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int islandY = BASE_Y + rand.nextInt(Y_VARIANCE * 2 + 1) - Y_VARIANCE;
        // Per-island noise seed for shape variation
        long noiseSeed = rand.nextLong();

        double dx = x - centerX;
        double dz = z - centerZ;
        double distSq = dx * dx + dz * dz;
        double radiusSq = (double) radius * radius;

        // Warp the effective radius using noise based on angle
        double angle = Math.atan2(dz, dx);
        double edgeNoise = pseudoNoise(angle * 3.0 + noiseSeed * 0.0001, noiseSeed);
        double warpedRadiusSq = radiusSq * (1.0 + EDGE_WARP * edgeNoise);

        if (distSq > warpedRadiusSq * 1.2) return -1.0;

        // Smooth horizontal falloff
        double hFactor = 1.0 - (distSq / warpedRadiusSq);
        if (hFactor <= 0) return -1.0;
        hFactor = hFactor * hFactor;

        // Surface height bumps — makes the top uneven
        double surfaceNoise = pseudoNoise(x * NOISE_SCALE + noiseSeed * 0.001,
                                          z * NOISE_SCALE + noiseSeed * 0.002);
        double yOffset = surfaceNoise * SURFACE_BUMP * hFactor;

        // Vertical slab with noise-adjusted surface
        double adjustedY = islandY + yOffset;
        double yDist = y - adjustedY;

        // Asymmetric: flatter on top, rounder on bottom (cloud-like)
        double topThickness = (THICKNESS * 0.4) * hFactor + 0.5;
        double botThickness = (THICKNESS * 1.2) * hFactor + 1.0;

        if (yDist > topThickness || yDist < -botThickness) return -1.0;

        double vFactor;
        if (yDist >= 0) {
            vFactor = 1.0 - (yDist / topThickness);
        } else {
            vFactor = 1.0 - (-yDist / botThickness);
        }

        return hFactor * vFactor;
    }

    /**
     * Simple pseudo-noise using sine mixing. Not true perlin,
     * but cheap and good enough for shape variation.
     * Returns values roughly in [-1, 1].
     */
    private static double pseudoNoise(double a, double b) {
        double v = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453;
        return (v - Math.floor(v)) * 2.0 - 1.0;
    }

    @Override
    public double minValue() { return -1.0; }

    @Override
    public double maxValue() { return 1.0; }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
