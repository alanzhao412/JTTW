package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.ridge.CaveSites;
import com.jttw.engine.world.ridge.WhiteBoneCave;
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

    /** The zone it raises. */
    public static final Zones.Zone ZONE = Zones.WHITE_BONE;

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
        double t = ZONE.depth(seed, x, z);
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

    /**
     * How far (x, z) is from the centre of its region, where the lair stands; MAX_VALUE where
     * there is no region.
     */
    public static double lairDistance(long seed, int x, int z) {
        return ZONE.distanceToCentre(seed, x, z);
    }

    /** Whether (x, z) is on the lair's ground, where nothing else is put (see {@link #keepClear}). */
    public static boolean onLairFloor(long seed, int x, int z) {
        return keepClear(seed, x, z);
    }

    /** The top of the lair floor of the region centred at (cx, cz): the massif's own height there. */
    private static int lairFloor(long seed, DensityFunction shape, int cx, int cz, int minY, int maxY) {
        return FLOORS.computeIfAbsent(seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL)),
                k -> Math.min(maxY - HEADROOM, ZoneTerrain.landAt(shape, cx, cz, minY, maxY) + riseAt(seed, cx, cz)));
    }

    /** The clear air kept over the lair floor. */
    private static final int HEADROOM = 16;

    /** The ridge's stone inside, as the massif lays it (the White Bone Cave fills its rock with it): deepslate deep down. */
    public static BlockState innerStone(int y, int x, int z) {
        return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : inner(y, x, z);
    }

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

    /** What raising it has cost, all told: nanoseconds, and the chunks it raised (the worldgen bench reads them). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong(),
            CHUNKS = new java.util.concurrent.atomic.AtomicLong();

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        long start = System.nanoTime();
        boolean any = raise(context);
        NANOS.addAndGet(System.nanoTime() - start);
        if (any) CHUNKS.incrementAndGet();
        return any;
    }

    private boolean raise(FeaturePlaceContext<NoneFeatureConfiguration> context) {
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
        // the White Bone Cave's mountain, forecourt, road and outposts, where the world has it
        WhiteBoneCave.Site site = CaveSites.site(level);
        Works works = site == null ? null : new Works(level, site, x0, z0);
        if (works != null && !works.any) works = null;
        // the land's broad shape every 4 blocks, between them in between
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int[][] made = new int[5][5];
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            made[i][j] = ZoneTerrain.landAt(shape, x0 + i * 4, z0 + j * 4, level.getMinBuildHeight(), ZoneTerrain.landTop(level));
        }
        int[] centre = ZONE.centre(seed, x0 + 8, z0 + 8); // a chunk lies in one cell
        // the old lair's floor: only where the world has no cave
        int floor = site == null ? lairFloorNear(seed, shape, centre, x0, z0, level) : Integer.MIN_VALUE;
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
                boolean cave = works != null && works.at(dx, dz);
                if (!ZONE.isAt(level, p) && !cave) continue;
                int here = shapes[dx + 1][dz + 1], rise = here >> 2, kind = here & 3;
                int step = Math.max(Math.max(Math.abs((shapes[dx][dz + 1] >> 2) - rise), Math.abs((shapes[dx + 2][dz + 1] >> 2) - rise)),
                        Math.max(Math.abs((shapes[dx + 1][dz] >> 2) - rise), Math.abs((shapes[dx + 1][dz + 2] >> 2) - rise)));
                boolean face = step >= STEEP;
                // at least as high as any water stood, so none is left on it
                int top = Math.min(level.getMaxBuildHeight() - 2, Math.max(Math.max(land, ground) + rise, surface));
                int clearTo = top;
                BlockState cover = null, slab = null;
                if (cave) {
                    int[] w = works.shape(dx, dz, top);
                    if (w[1] != 0) {
                        clearTo = Math.max(top, surface);
                        top = Math.min(level.getMaxBuildHeight() - 2, w[0]);
                        face = w[1] == 1;
                    }
                    cover = works.cover(dx, dz);
                    slab = works.slab(dx, dz);
                }
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
                for (int y = top + 1; y <= clearTo; y++) {
                    if (!level.getBlockState(p.set(x, y, z)).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                for (int y = Math.min(ground, top); y <= top; y++) {
                    p.set(x, y, z);
                    BlockState s;
                    if (lair && y == top) s = ((int) fromLair) % 3 == 0 ? com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState() : com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
                    else if (cover != null && y == top) s = cover;
                    else if (face || y < top - 3) s = inner(y, x, z);
                    else if (y == top) s = topOf(kind, x, z);
                    else s = kind == BASIN && y == top - 1 ? com.jttw.engine.registry.ModBlocks.BONE_GRAVEL.get().defaultBlockState() : com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
                    level.setBlock(p, s, Block.UPDATE_CLIENTS);
                }
                if (slab != null) level.setBlock(p.set(x, top + 1, z), slab, Block.UPDATE_CLIENTS);
                any = true;
            }
        }
        return any;
    }

    /** The old lair's floor height, if the chunk at (x0, z0) is over it; else MIN_VALUE. */
    private static int lairFloorNear(long seed, DensityFunction shape, int[] centre, int x0, int z0, WorldGenLevel level) {
        if (centre == null) return Integer.MIN_VALUE;
        if (Math.hypot(Math.max(Math.abs(centre[0] - x0 - 8) - 8, 0), Math.max(Math.abs(centre[1] - z0 - 8) - 8, 0)) > LAIR_RADIUS + 1) return Integer.MIN_VALUE;
        return lairFloor(seed, shape, centre[0], centre[1], level.getMinBuildHeight(), ZoneTerrain.landTop(level));
    }

    // ── The White Bone Cave's ground ────────────────────────────────────

    /**
     * Whether (x, z) is ground kept clear of the ridge's bones, boulders and dead trees: the old
     * lair's floor; or, where the world has the White Bone Cave, its forecourt, its road (and its
     * towers) and its outposts' pads.
     */
    public static boolean keepClear(long seed, int x, int z) {
        WhiteBoneCave.Site site = CaveSites.cached(seed);
        if (site == null) return lairDistance(seed, x, z) <= LAIR_RADIUS + 1.5;
        int lx = x - site.cx(), lz = z - site.cz();
        if (Math.abs(lx) <= WhiteBoneCave.COURT_HALF + 2 && lz >= WhiteBoneCave.COURT_Z0 - 1 && lz <= WhiteBoneCave.ROAD_Z0) return true;
        if (WhiteBoneCave.onRoadLength(lz) && Math.abs(lx - WhiteBoneCave.roadX(lz)) <= WhiteBoneCave.TOWER_OUT + 3) return true;
        for (int[] o : WhiteBoneCave.of(seed).outposts()) {
            if (Math.hypot(lx - o[0], lz - o[1]) <= WhiteBoneCave.OUTPOST_R + 3) return true;
        }
        return false;
    }

    /**
     * The cave's ground in one chunk: the mountain over the fortress, crags on its flanks; the
     * forecourt cut into its southern face, level with the first hall; the road down from it, cut
     * into the slope or banked up over the low ground, in half steps; the outposts' level pads.
     */
    private static final class Works {
        final WhiteBoneCave.Site site;
        final int x0, z0, roadEnd;
        final int[][] outposts;
        final int[] pads;
        final long seed;
        final boolean any;
        /** The mountain's height (world) over the chunk and a ring round it; MIN_VALUE where there is none. */
        final int[][] mount = new int[18][18];

        Works(WorldGenLevel level, WhiteBoneCave.Site site, int x0, int z0) {
            this.site = site;
            this.x0 = x0;
            this.z0 = z0;
            this.seed = level.getSeed();
            int lx = x0 + 8 - site.cx(), lz = z0 + 8 - site.cz();
            outposts = WhiteBoneCave.of(seed).outposts();
            boolean near = Math.hypot(lx, lz) <= WhiteBoneCave.MOUNTAIN_R + 24
                    || Math.abs(lx - WhiteBoneCave.roadX(lz)) <= 40 && (WhiteBoneCave.onRoadLength(lz + 8) || WhiteBoneCave.onRoadLength(lz - 8));
            for (int[] o : outposts) near |= Math.hypot(lx - o[0], lz - o[1]) <= WhiteBoneCave.OUTPOST_R + 24;
            any = near;
            roadEnd = near ? CaveSites.roadEnd(level, site) : 0;
            pads = near ? CaveSites.pads(level, site) : new int[0];
            SimplexNoise n = noise(seed);
            for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
                int x = x0 + i - 1, z = z0 + j - 1;
                double m = WhiteBoneCave.mountain(x - site.cx(), z - site.cz());
                if (m <= 0) {
                    mount[i][j] = Integer.MIN_VALUE;
                    continue;
                }
                // crags: ridged noise, more of it toward the foot, none at the top
                double ridged = 1 - Math.abs(n.getValue(x / 31.0 + 17.3, z / 31.0 - 41.9));
                double crag = ridged * ridged * ridged * 16 * Math.max(0.45, Math.min(1, Math.max(0, WhiteBoneCave.PEAK - m) / 40.0));
                mount[i][j] = Math.min(WhiteBoneCave.TOP_LIMIT, site.y0() + (int) Math.round(m * 0.9 + crag));
            }
        }

        /** Whether the chunk's column (dx, dz) is the cave's ground. */
        boolean at(int dx, int dz) {
            int lx = x0 + dx - site.cx(), lz = z0 + dz - site.cz();
            if (mount[dx + 1][dz + 1] != Integer.MIN_VALUE) return true;
            if (WhiteBoneCave.onRoadLength(lz) && Math.abs(lx - WhiteBoneCave.roadX(lz)) <= WhiteBoneCave.ROAD_HALF + 14) return true;
            for (int[] o : outposts) if (Math.hypot(lx - o[0], lz - o[1]) <= WhiteBoneCave.OUTPOST_R + 11) return true;
            return false;
        }

        /** The road's walking height in halves of a block (world) at local z. */
        int roadHalves(int lz) {
            return 2 * site.y0() + WhiteBoneCave.roadHalves(lz, roadEnd);
        }

        /**
         * The top of the ground at the chunk's column (dx, dz), the massif's being {@code top}; and
         * whether it is a face (1), level ground (2), or as the massif has it (0).
         */
        int[] shape(int dx, int dz, int top) {
            int lx = x0 + dx - site.cx(), lz = z0 + dz - site.cz();
            int t = top, faced = 0;
            int m = mount[dx + 1][dz + 1];
            if (m != Integer.MIN_VALUE && m > t) {
                t = m;
                int step = 0;
                for (int[] d : new int[][]{{0, 1}, {2, 1}, {1, 0}, {1, 2}}) {
                    int o = mount[dx + d[0]][dz + d[1]];
                    if (o != Integer.MIN_VALUE) step = Math.max(step, Math.abs(o - m));
                }
                faced = step >= STEEP ? 1 : 2;
            } else if (m != Integer.MIN_VALUE) {
                // the ridge's own crags standing over the mountain's flanks are worn down toward
                // it, wholly about its heart, less and less out to its foot: one mountain, not a
                // mountain lost among needles
                double r = Math.hypot(lx, lz);
                double s = Mth.clamp((r - WhiteBoneCave.MOUNTAIN_R * 0.5) / (WhiteBoneCave.MOUNTAIN_R * 0.45), 0, 1);
                int worn = m + (int) Math.round((t - m) * s * s * (3 - 2 * s));
                if (worn < t) {
                    t = worn;
                    faced = 2;
                }
            }
            if (Math.abs(lx) <= WhiteBoneCave.COURT_HALF && lz >= WhiteBoneCave.COURT_Z0 && lz <= WhiteBoneCave.COURT_Z1) {
                return new int[]{site.y0() - 1, 2};
            }
            if (WhiteBoneCave.onRoadLength(lz)) {
                int ad = Math.abs(lx - WhiteBoneCave.roadX(lz));
                int feet = Math.floorDiv(roadHalves(lz), 2);
                if (ad <= WhiteBoneCave.ROAD_HALF + 1) return new int[]{feet - 1, 2};
                int e = ad - WhiteBoneCave.ROAD_HALF - 1;
                if (e <= 14) {
                    int lo = feet - 1 - 2 * e, hi = feet - 1 + 2 * e;
                    if (t < lo || t > hi) return new int[]{Mth.clamp(t, lo, hi), 1};
                }
            }
            for (int i = 0; i < outposts.length; i++) {
                double d = Math.hypot(lx - outposts[i][0], lz - outposts[i][1]);
                int pad = pads[i] - 1;
                if (d <= WhiteBoneCave.OUTPOST_R) return new int[]{pad, 2};
                int e = (int) Math.ceil(d - WhiteBoneCave.OUTPOST_R);
                if (e <= 11) {
                    int lo = pad - 2 * e, hi = pad + 2 * e;
                    if (t < lo || t > hi) return new int[]{Mth.clamp(t, lo, hi), 1};
                }
            }
            return new int[]{t, faced};
        }

        /** The top block of the forecourt, the road and the pads; null elsewhere. */
        BlockState cover(int dx, int dz) {
            int x = x0 + dx, z = z0 + dz, lx = x - site.cx(), lz = z - site.cz();
            int roll = Math.floorMod((int) Mth.getSeed(x, 77, z), 100);
            if (Math.abs(lx) <= WhiteBoneCave.COURT_HALF && lz >= WhiteBoneCave.COURT_Z0 && lz <= WhiteBoneCave.COURT_Z1) {
                // the forecourt: flagstones of bleached stone, a way of grave tuff to the door, a ring of bone round its middle
                double r = Math.hypot(lx, lz - 70);
                if (Math.abs(lx) <= 2) return com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get().defaultBlockState();
                if (r >= 7 && r < 8) return com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState();
                return roll < 12 ? Blocks.POLISHED_DIORITE.defaultBlockState() : com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
            }
            if (WhiteBoneCave.onRoadLength(lz)) {
                int ad = Math.abs(lx - WhiteBoneCave.roadX(lz));
                if (ad == WhiteBoneCave.ROAD_HALF + 1) return com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState();
                if (ad <= WhiteBoneCave.ROAD_HALF) {
                    return roll < 45 ? com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get().defaultBlockState()
                            : roll < 60 ? Blocks.POLISHED_TUFF.defaultBlockState() : com.jttw.engine.registry.ModBlocks.BLEACHED_STONE.get().defaultBlockState();
                }
            }
            for (int[] o : outposts) {
                if (Math.hypot(lx - o[0], lz - o[1]) <= WhiteBoneCave.OUTPOST_R) {
                    return roll < 50 ? com.jttw.engine.registry.ModBlocks.GRAVE_TUFF.get().defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                }
            }
            return null;
        }

        /** A slab over the road where its walking height is a half step. */
        BlockState slab(int dx, int dz) {
            int lx = x0 + dx - site.cx(), lz = z0 + dz - site.cz();
            if (!WhiteBoneCave.onRoadLength(lz)) return null;
            if (Math.abs(lx - WhiteBoneCave.roadX(lz)) > WhiteBoneCave.ROAD_HALF) return null;
            return (roadHalves(lz) & 1) == 0 ? null : Blocks.POLISHED_TUFF_SLAB.defaultBlockState();
        }
    }
}
