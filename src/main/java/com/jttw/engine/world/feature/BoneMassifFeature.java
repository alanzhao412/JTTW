package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Raises White Bone Ridge out of the land it stands on and lays its ground: a massif with sheer
 * cliffs at its edge, so each region stands alone over the country around it. Within, three kinds
 * of ground wander into one another a couple of hundred blocks across:
 * <ul>
 *   <li>{@link #CRAG crags}: ridged spires and knife-edges, the highest of it;</li>
 *   <li>{@link #MESA mesas}: broad plateaus stepped in terraces;</li>
 *   <li>{@link #BASIN basins}: sunken flats of ash and grit, walled in by the rest.</li>
 * </ul>
 * Steep faces show the banded grey stone inside; flat ground is bleached calcite and bone, or ash
 * in the basins. It rises from the floor of any water in its way and fills it: there is no water
 * in the ridge. The ground keeps its own rise and fall beneath; the massif rides on it: on the
 * land's broad shape, as the terrain noise lays it out before any cave, canyon or cave mouth opens
 * in it, so none under the massif leaves a pit in it.
 *
 * <p>At the heart of each region it lays a round, level floor for Lady White Bone's lair, at a
 * height worked out from the centre alone, so every chunk under it agrees whichever is made first
 * ({@link LairFeature} raises its pillars on it, and puts her there).
 *
 * <p>Runs first among the zone's features, so ores, the remains and the brush all come after.
 * Later features must find the ground with the live heightmaps ({@code WORLD_SURFACE}): the
 * worldgen ones ({@code _WG}) still hold the ground from before the massif rose.
 */
public class BoneMassifFeature extends Feature<NoneFeatureConfiguration> {

    /** How far the plateau stands over the land it rose from. */
    public static final int RISE = 70;
    /** How much higher the crags reach. */
    public static final int CREST = 34;
    /** The height of a mesa's terraces. */
    private static final int TERRACE = 7;
    /** How much of the plateau's height a basin keeps. */
    private static final double BASIN_SHARE = 0.35;
    /** The share of the way in over which the cliffs climb. */
    private static final double CLIFF = 0.15;
    /** A step this high or more to a neighbour makes a face, not ground. */
    private static final int STEEP = 3;

    public static final int CRAG = 0, MESA = 1, BASIN = 2;

    /** The lair floor's radius. */
    public static final int LAIR_RADIUS = 8;
    /** The lair floor's height for each region, by its centre (worked out once). */
    private static final java.util.Map<Long, Integer> FLOORS = new java.util.concurrent.ConcurrentHashMap<>();

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    public BoneMassifFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0xB0E5B0E5L));
            noiseSeed = seed;
        }
        return noise;
    }

    private static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** The massif at (x, z): how many blocks it adds (0 outside it), times 4, plus the kind of ground there. */
    private static int shape(long seed, int x, int z) {
        double t = Zones.WHITE_BONE.layout().depth(seed, x, z);
        if (t <= 0) return MESA;
        double s = Math.min(1, t / CLIFF);
        double cliff = s * s * (3 - 2 * s);
        SimplexNoise n = noise(seed);
        double k = n.getValue(x / 180.0 + 311.7, z / 180.0 - 93.1);
        double crags = smooth(0.08, 0.32, k), basins = smooth(0.08, 0.32, -k), mesas = 1 - crags - basins;
        double base = RISE * (0.8 + 0.2 * t);
        double h = 0;
        if (crags > 0) h += crags * (base + CREST * Math.pow(1 - Math.abs(n.getValue(x / 44.0, z / 44.0)), 3));
        if (mesas > 0) h += mesas * Math.floor((base + 6 + 10 * n.getValue(x / 90.0 + 50, z / 90.0 + 50)) / TERRACE) * TERRACE;
        if (basins > 0) h += basins * (base * BASIN_SHARE + 3 * n.getValue(x / 26.0 - 70, z / 26.0 + 70));
        int kind = crags >= mesas && crags >= basins ? CRAG : basins >= mesas ? BASIN : MESA;
        return (int) Math.round(cliff * h) << 2 | kind;
    }

    /** How many blocks the massif adds at (x, z), 0 outside it. */
    public static int riseAt(long seed, int x, int z) {
        return shape(seed, x, z) >> 2;
    }

    /** The kind of ground at (x, z): {@link #CRAG}, {@link #MESA} or {@link #BASIN}. */
    public static int kindAt(long seed, int x, int z) {
        return shape(seed, x, z) & 3;
    }

    /** Where the terrain noise counts as solid: the threshold vanilla's own estimate of the surface uses. */
    private static final double SOLID = 0.390625;

    /**
     * The land's surface at (x, z) by its broad shape alone, as vanilla estimates it before making the
     * terrain (NoiseChunk's preliminary surface level): no caves, cave mouths, canyons or crags in it.
     * Searched down in steps of 16, then of 4.
     */
    private static int landAt(DensityFunction shape, int x, int z, int minY, int maxY) {
        int y = maxY;
        while (y > minY && shape.compute(new DensityFunction.SinglePointContext(x, y, z)) <= SOLID) y -= 16;
        for (int fine = Math.min(maxY, y + 12); fine > y; fine -= 4) {
            if (shape.compute(new DensityFunction.SinglePointContext(x, fine, z)) > SOLID) return fine;
        }
        return Math.max(y, minY);
    }

    /**
     * How far (x, z) is from the centre of its region, where the lair stands; MAX_VALUE where
     * there is no region.
     */
    public static double lairDistance(long seed, int x, int z) {
        int[] c = Zones.WHITE_BONE.layout().centre(seed, x, z);
        return c == null ? Double.MAX_VALUE : Math.hypot(x - c[0], z - c[1]);
    }

    /** Whether (x, z) is on an lair floor, where nothing else is put. */
    public static boolean onLairFloor(long seed, int x, int z) {
        return lairDistance(seed, x, z) <= LAIR_RADIUS + 1.5;
    }

    /** The top of the lair floor of the region centred at (cx, cz): the massif's own height there. */
    private static int lairFloor(long seed, DensityFunction shape, int cx, int cz, int minY, int maxY) {
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)),
                k -> Math.min(maxY - HEADROOM, landAt(shape, cx, cz, minY, maxY) + riseAt(seed, cx, cz)));
    }

    /** The clear air kept over the lair floor. */
    private static final int HEADROOM = 16;

    /** The stone inside, in uneven bands of tuff and andesite. */
    private static BlockState inner(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 31 + z * 17) >> 5), 9);
        return band == 0 ? com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get().defaultBlockState() : band == 5 ? com.jttw.engine.registry.ModBlocks.BONE_ANDESITE.get().defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    private static int patch(int x, int z, int size, int salt) {
        return (int) Math.floorMod(Mth.getSeed(Math.floorDiv(x, size), salt, Math.floorDiv(z, size)), 100L);
    }

    /** The top of flat ground of each kind: crags bleached with bone and tuff, mesas bare calcite, basins ash. */
    private static BlockState topOf(int kind, int x, int z) {
        int roll;
        switch (kind) {
            case CRAG -> {
                roll = patch(x, z, 4, 1);
                if (roll < 12) return com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState();
                if (roll < 25) return com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get().defaultBlockState();
            }
            case BASIN -> {
                roll = patch(x, z, 3, 2);
                if (roll < 55) return com.jttw.engine.registry.ModBlocks.BONE_GRAVEL.get().defaultBlockState();
                if (roll < 75) return com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get().defaultBlockState();
                if (roll < 85) return com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState();
            }
            default -> {
                roll = patch(x, z, 6, 3);
                if (roll < 8) return com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState();
            }
        }
        return com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        // the chunk and a ring around it, to tell the faces from the flats
        int[][] shapes = new int[18][18];
        boolean inside = false;
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            shapes[i][j] = shape(seed, x0 + i - 1, z0 + j - 1);
            inside |= shapes[i][j] >> 2 > 0;
        }
        if (!inside) return false;
        // the land's broad shape every 4 blocks, between them in between
        DensityFunction shape = level.getLevel().getChunkSource().randomState().router().initialDensityWithoutJaggedness();
        int[][] made = new int[5][5];
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            made[i][j] = landAt(shape, x0 + i * 4, z0 + j * 4, level.getMinBuildHeight(), level.getMaxBuildHeight());
        }
        int[] centre = Zones.WHITE_BONE.layout().centre(seed, x0 + 8, z0 + 8); // a chunk lies in one cell
        int floor = centre != null && Math.hypot(Math.max(Math.abs(centre[0] - x0 - 8) - 8, 0), Math.max(Math.abs(centre[1] - z0 - 8) - 8, 0)) <= LAIR_RADIUS + 1
                ? lairFloor(seed, shape, centre[0], centre[1], level.getMinBuildHeight(), level.getMaxBuildHeight()) : Integer.MIN_VALUE;
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; // under any water, or down a canyon
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int i = dx >> 2, j = dz >> 2;
                double fx = (dx & 3) / 4.0, fz = (dz & 3) / 4.0;
                int land = (int) Math.round(Mth.lerp2(fx, fz, made[i][j], made[i + 1][j], made[i][j + 1], made[i + 1][j + 1]));
                p.set(x, surface, z);
                if (!level.getBiome(p).is(Zones.WHITE_BONE_RIDGE)) continue;
                int here = shapes[dx + 1][dz + 1], rise = here >> 2, kind = here & 3;
                int step = Math.max(Math.max(Math.abs((shapes[dx][dz + 1] >> 2) - rise), Math.abs((shapes[dx + 2][dz + 1] >> 2) - rise)),
                        Math.max(Math.abs((shapes[dx + 1][dz] >> 2) - rise), Math.abs((shapes[dx + 1][dz + 2] >> 2) - rise)));
                boolean face = step >= STEEP;
                // at least as high as any water stood, so none is left on it
                int top = Math.min(level.getMaxBuildHeight() - 2, Math.max(Math.max(land, ground) + rise, surface));
                double fromLair = floor == Integer.MIN_VALUE ? Double.MAX_VALUE : Math.hypot(x - centre[0], z - centre[1]);
                boolean lair = fromLair <= LAIR_RADIUS + 0.5;
                if (lair) {
                    // the lair floor: level, in rings of bone and calcite, open to the sky
                    top = floor;
                    face = false;
                    for (int y = top + 1; y <= Math.max(surface, top + HEADROOM); y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (!s.isAir() && !s.is(com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get()) && !s.is(Blocks.SKELETON_SKULL)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                for (int y = Math.min(ground, top); y <= top; y++) {
                    p.set(x, y, z);
                    BlockState s;
                    if (lair && y == top) s = ((int) fromLair) % 3 == 0 ? com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState() : com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
                    else if (face || y < top - 3) s = inner(y, x, z);
                    else if (y == top) s = topOf(kind, x, z);
                    else s = kind == BASIN && y == top - 1 ? com.jttw.engine.registry.ModBlocks.BONE_GRAVEL.get().defaultBlockState() : com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
                    level.setBlock(p, s, Block.UPDATE_CLIENTS);
                }
                any = true;
            }
        }
        return any;
    }
}
