package com.jttw.engine.world;

/**
 * Where a zone's regions lie, from the world seed alone: the world is cut into square cells, a
 * share of which hold one region, a rough circle somewhere inside the cell (never crossing it).
 *
 * @param cellSize blocks per side of the cells the world is cut into
 * @param chance   share of cells that hold a region
 * @param radius   rough radius of a region, in blocks
 * @param wobble   how far its edge strays from the circle, in blocks
 * @param salt     keeps zones of different kinds from landing in the same cells
 */
public record ZoneLayout(int cellSize, double chance, int radius, int wobble, long salt) {

    /** White Bone Ridge: 60% of 2048-block cells hold a region about 260 blocks across. */
    public static final ZoneLayout WHITE_BONE = new ZoneLayout(2048, 0.6, 260, 70, 0x5EEDB0E1L);

    /** Whether a region covers the block column at (x, z) in a world of this seed. */
    public boolean covers(long seed, int x, int z) {
        int cx = Math.floorDiv(x, cellSize), cz = Math.floorDiv(z, cellSize);
        long h = mix(seed ^ salt, cx, cz);
        if (unit(h) >= chance) return false;
        double margin = radius + wobble;
        double centreX = (double) cx * cellSize + margin + unit(mix(h, 1, 0)) * (cellSize - 2 * margin);
        double centreZ = (double) cz * cellSize + margin + unit(mix(h, 2, 0)) * (cellSize - 2 * margin);
        double dx = x - centreX, dz = z - centreZ;
        double d2 = dx * dx + dz * dz;
        if (d2 > margin * margin) return false;
        double a = Math.atan2(dz, dx);
        double edge = radius + wobble * (0.5 * Math.sin(3 * a + 6.283 * unit(mix(h, 3, 0)))
                + 0.3 * Math.sin(5 * a + 6.283 * unit(mix(h, 4, 0)))
                + 0.2 * Math.sin(7 * a + 6.283 * unit(mix(h, 5, 0))));
        return d2 <= edge * edge;
    }

    static long mix(long base, long a, long b) {
        long z = base + a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A hash as a number in [0, 1). */
    static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
