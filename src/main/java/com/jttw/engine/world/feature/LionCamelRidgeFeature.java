package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelCity;
import com.jttw.engine.world.lion.LionFortress;
import com.jttw.engine.world.lion.RidgeApproach;
import com.jttw.engine.world.lion.RidgeShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Makes Lion-Camel Ridge (狮驼岭) of the mountain country it takes over ({@link RidgeShape}): breaks
 * its slopes into grim crags, strips the green from them, and lays Lion-Camel City at its heart,
 * column by column ({@link LionCamelCity}), so the city comes out whole across the chunks it spans.
 * <ul>
 *   <li><b>faces</b> (a step of three or more to a neighbour) show bare rock: stone banded with
 *       tuff, andesite and deepslate;</li>
 *   <li><b>ground</b> is coarse dirt, gravel, podzol and bare stone, a little grass gone yellow,
 *       snow on the heights over y {@value #SNOWLINE}; skulls on stakes and bones lie about it (47,000
 *       little demons live on the ridge, and they eat what they catch);</li>
 *   <li>the city's <b>apron</b>, level, is trodden earth and gravel; round it the ground climbs (or
 *       falls) back to the ridge's.</li>
 * </ul>
 * It never touches anything under y {@value Zones#LION_CAMEL_FLOOR} (the Bottomless Cave may lie
 * there), nor the Bottomless Cave's shaft where one comes up through the ridge. Runs first among the
 * zone's features; later ones find the ground with the live heightmaps.
 */
public class LionCamelRidgeFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = RidgeShape.ZONE;
    /** A step this high or more to a neighbour makes a face, not ground. */
    private static final int STEEP = 3;
    /** Snow lies on the heights over this. */
    public static final int SNOWLINE = 196;
    /** How near the Bottomless Cave's shaft the ridge leaves the ground alone. */
    private static final double SHAFT_CLEAR = Labyrinth.DISH_RADIUS + 4;

    private static final int FACE = 0, GROUND = 1, SNOW = 2, APRON = 3, ROAD = 4, COURT = 5;

    public LionCamelRidgeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The city's floor (its ground's top) in the region round (cx, cz) in this level: high enough over the world's floor for the fortress under it. */
    public static int cityFloor(WorldGenLevel level, int cx, int cz) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        return RidgeShape.cityFloor(level.getSeed(), cx, cz, (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY),
                Math.max(level.getSeaLevel() + RidgeShape.ABOVE_SEA, minY + RidgeShape.FORTRESS_ROOM),
                Math.min(maxY - RidgeShape.UNDER_SKY, RidgeShape.HIGHEST));
    }

    /** The ridge's own heights under the roads' middles, and the roads' heights, by region, road and how far out: worked out once each. */
    private static final java.util.Map<Long, Integer> ROAD_GROUND = new java.util.concurrent.ConcurrentHashMap<>(),
            ROAD_HEIGHTS = new java.util.concurrent.ConcurrentHashMap<>();
    /** How far along a road its ground is evened out, each way; how far apart along it the ridge's ground is sampled. */
    private static final int ROAD_EVEN = 14, ROAD_SAMPLE = 8;

    private static long roadKey(WorldGenLevel level, int[] c, int road, int along) {
        return ((level.getSeed() * 31 + c[0]) * 31 + c[1]) * 1031 + road * 1000L + along;
    }

    /** The ridge's own ground (crags and all) under a road's middle at {@code along}, from the terrain's noise. */
    private static int roadGround(WorldGenLevel level, int[] c, int road, int along) {
        return ROAD_GROUND.computeIfAbsent(roadKey(level, c, road, along), k -> {
            int[] p = RidgeApproach.point(road, along, 0);
            int x = c[0] + p[0], z = c[1] + p[1];
            var source = level.getLevel().getChunkSource();
            int ground = source.getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, source.randomState()) - 1;
            return Math.max(ground, level.getSeaLevel()) + RidgeShape.cragAt(level.getSeed(), x, z);
        });
    }

    /**
     * The height a road's ground is brought to at {@code along} (its whole width, and an outpost
     * beside it there): the ridge's own ground under its middle, evened out along it (so it climbs
     * and falls with the mountains, cutting through a crag and built up over a gully), eased into the
     * city's floor near the walls as the ridge's ground is.
     */
    public static int roadHeight(WorldGenLevel level, int[] c, int floor, int road, int along) {
        return ROAD_HEIGHTS.computeIfAbsent(roadKey(level, c, road, along), k -> {
            int[] p = RidgeApproach.point(road, along, 0);
            double share = RidgeShape.cityShare(p[0], p[1]);
            if (share <= 0) return floor;
            double sum = 0;
            int n = 0;
            for (int a = along - ROAD_EVEN; a <= along + ROAD_EVEN; a += 7) {
                int at = Math.max(a, RidgeApproach.ROAD_FROM), a0 = Math.floorDiv(at, ROAD_SAMPLE) * ROAD_SAMPLE;
                double f = (at - a0) / (double) ROAD_SAMPLE;
                sum += Mth.lerp(f, roadGround(level, c, road, a0), roadGround(level, c, road, a0 + ROAD_SAMPLE));
                n++;
            }
            int ground = (int) Math.round(sum / n);
            return (int) Math.round(Mth.lerp(share, floor, Mth.clamp(ground, level.getSeaLevel() + 1, level.getMaxBuildHeight() - 40)));
        });
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    private static int fine(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 37 + 3, z), 1000L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** The ridge's rock, in uneven bands. */
    private static BlockState rock(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 29 + z * 13) >> 5), 13);
        return switch (band) {
            case 0, 1 -> s(Blocks.TUFF);
            case 4 -> s(Blocks.ANDESITE);
            case 7, 8 -> s(Blocks.COBBLED_DEEPSLATE);
            case 10 -> hash(x >> 2, z >> 2, y) < 40 ? s(Blocks.DEEPSLATE) : s(Blocks.STONE);
            default -> s(Blocks.STONE);
        };
    }

    private static BlockState top(int kind, int x, int y, int z, RidgeApproach.Spot road) {
        int r = hash(x >> 1, z >> 1, 5);
        if (kind == ROAD) return RidgeApproach.roadTop(road, x, z);
        if (kind == COURT) {
            int q = hash(x, z, 7);
            return q < 50 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : q < 75 ? s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) : q < 90 ? s(Blocks.BLACKSTONE) : s(Blocks.GRAVEL);
        }
        return switch (kind) {
            case FACE -> rock(y, x, z);
            case SNOW -> r < 80 ? s(Blocks.SNOW_BLOCK) : s(Blocks.STONE);
            case APRON -> r < 45 ? s(Blocks.COARSE_DIRT) : r < 70 ? s(Blocks.GRAVEL) : r < 85 ? s(Blocks.PACKED_MUD) : s(Blocks.GRASS_BLOCK);
            default -> r < 28 ? s(Blocks.COARSE_DIRT) : r < 40 ? s(Blocks.GRAVEL) : r < 50 ? s(Blocks.PODZOL)
                    : r < 62 ? s(Blocks.STONE) : r < 68 ? s(Blocks.ROOTED_DIRT) : r < 72 ? s(Blocks.TUFF) : s(Blocks.GRASS_BLOCK);
        };
    }

    private static BlockState under(int kind, int x, int y, int z) {
        if (kind == FACE || kind == SNOW) return rock(y, x, z);
        if (kind == ROAD || kind == COURT) return hash(x, z, 6 + y) < 50 ? s(Blocks.COBBLESTONE) : s(Blocks.STONE);
        return hash(x, z, 6 + y) < 60 ? s(Blocks.COARSE_DIRT) : s(Blocks.STONE);
    }

    /** The time this feature has taken, in nanoseconds, and the chunks of the ridge it has laid (the bench reads them). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong(), CHUNKS = new java.util.concurrent.atomic.AtomicLong();

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        long start = System.nanoTime();
        boolean laid = lay(context);
        if (laid) {
            NANOS.addAndGet(System.nanoTime() - start);
            CHUNKS.incrementAndGet();
        }
        return laid;
    }

    private boolean lay(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        boolean inside = false;
        for (int i = 0; i < 18 && !inside; i += 17) for (int j = 0; j < 18; j += 17) inside |= ZONE.depth(seed, x0 + i - 1, z0 + j - 1) > 0;
        if (!inside) for (int i = 0; i < 16 && !inside; i += 3) for (int j = 0; j < 16; j += 3) inside |= ZONE.depth(seed, x0 + i, z0 + j) > 0;
        if (!inside) return false;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8); // a chunk lies in one cell
        int floor = c == null ? Integer.MIN_VALUE : cityFloor(level, c[0], c[1]);
        int minY = Zones.LION_CAMEL_FLOOR, maxY = level.getMaxBuildHeight() - 2, bottom = level.getMinBuildHeight() + 1;
        // the Bottomless Cave's shaft, if one comes up near
        int[] shaft = null;
        int[] cave = Zones.BOTTOMLESS.centre(seed, x0 + 8, z0 + 8);
        if (cave != null && Math.hypot(cave[0] - (x0 + 8), cave[1] - (z0 + 8)) < 16 + SHAFT_CLEAR) {
            Labyrinth lab = BottomlessCaveFeature.labyrinthFor(level, x0 + 8, z0 + 8);
            if (lab != null) shaft = new int[]{lab.cx, lab.cz};
        }
        // the height each column (and the ring round the chunk) is brought to
        int[][] target = new int[18][18];
        int[][] ground = new int[18][18];
        int[][] surface = new int[18][18];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            int x = x0 + i - 1, z = z0 + j - 1;
            int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; // under any water, or down a canyon
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            ground[i][j] = g;
            surface[i][j] = top;
            int rise = RidgeShape.riseAt(seed, x, z, c);
            int natural = g + rise;
            if (rise > 0) natural = Math.max(natural, top); // a crag rises out of any water, leaving none on it
            int t = c == null ? natural : RidgeShape.groundAt(natural, floor, x - c[0], z - c[1]);
            if (c != null) t = shaped(level, c, floor, x - c[0], z - c[1], t, seed);
            target[i][j] = Mth.clamp(t, minY, maxY);
        }
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                int g = ground[dx + 1][dz + 1], sur = surface[dx + 1][dz + 1];
                if (!ZONE.isAt(level, p.set(x, Math.max(Math.max(g, sur), minY), z))) continue;
                if (shaft != null && Math.hypot(x - shaft[0], z - shaft[1]) < SHAFT_CLEAR) continue;
                int t = target[dx + 1][dz + 1];
                int step = Math.max(Math.max(Math.abs(target[dx][dz + 1] - t), Math.abs(target[dx + 2][dz + 1] - t)),
                        Math.max(Math.abs(target[dx + 1][dz] - t), Math.abs(target[dx + 1][dz + 2] - t)));
                int lx = c == null ? Integer.MAX_VALUE / 2 : x - c[0], lz = c == null ? Integer.MAX_VALUE / 2 : z - c[1];
                boolean apron = c != null && RidgeShape.onApron(lx, lz);
                RidgeApproach.Spot road = c == null ? null : RidgeApproach.road(lx, lz);
                RidgeApproach.Outpost outpost = c == null ? null : RidgeApproach.outpostAt(lx, lz);
                boolean court = c != null && LionFortress.onForecourt(lx, lz);
                boolean made = road != null || outpost != null || (c != null && nearOutpost(lx, lz) != null); // built ground: no lake left on it
                boolean wet = sur > g && t <= sur && !apron && !made && t == g; // a lake or a river left as it was
                int kind = court ? COURT : road != null && !apron ? ROAD : apron ? APRON : step >= STEEP ? FACE : t >= SNOWLINE ? SNOW : GROUND;
                // cut down to it
                for (int y = Math.max(t + 1, minY); y <= Math.max(sur, g); y++) {
                    if (wet && y > g) break;
                    level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                }
                // built up to it, or its top few blocks made over
                if (!wet && t > g) {
                    for (int y = Math.max(minY, g + 1); y <= t; y++) {
                        BlockState st = y == t ? top(kind, x, y, z, road) : y > t - 4 ? under(kind, x, y, z) : rock(y, x, z);
                        level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                    }
                } else if (!wet) {
                    // its top few blocks made over (a cave just under them left open)
                    for (int y = Math.max(minY, t - 3); y <= t; y++) {
                        if (y < t && !level.getBlockState(p.set(x, y, z)).blocksMotion()) continue;
                        level.setBlock(p.set(x, y, z), y == t ? top(kind, x, y, z, road) : under(kind, x, y, z), Block.UPDATE_CLIENTS);
                    }
                }
                any = true;
                if (c == null) {
                    if (!wet && kind == GROUND) decorate(level, x, t, z, p);
                    continue;
                }
                // the ways up: the road's stakes, an outpost
                if (road != null && !apron) {
                    for (int h = 1; h <= 3; h++) {
                        BlockState st = RidgeApproach.roadside(road, h, x, z);
                        if (st != null) level.setBlock(p.set(x, t + h, z), st, Block.UPDATE_CLIENTS);
                    }
                }
                if (outpost != null) {
                    int base = roadHeight(level, c, floor, outpost.road(), outpost.along());
                    for (int h = 0; h <= RidgeApproach.TOWER_H + 2; h++) {
                        BlockState st = RidgeApproach.outpost(outpost, lx - outpost.dx(), h, lz - outpost.dz());
                        if (st != null && base + h <= maxY) level.setBlock(p.set(x, base + h, z), st, Block.UPDATE_CLIENTS);
                    }
                }
                if (court) forecourt(level, lx, lz, floor, x, z, p);
                // Lion-Camel City, its share in this column
                if (LionCamelCity.covers(lx, lz)) {
                    for (int ly = LionCamelCity.BOTTOM; ly <= LionCamelCity.TOP; ly++) {
                        BlockState st = LionCamelCity.at(lx, ly, lz);
                        int y = floor + ly;
                        if (st != null && y >= minY && y <= maxY) LionCamelCity.lay(level, p.set(x, y, z), st, context.random());
                    }
                    // under the walls and towers, footings down to the ground
                    if (!LionCamelCity.inside(lx, lz) && LionCamelCity.at(lx, 1, lz) != null && !LionCamelCity.at(lx, 1, lz).isAir()) {
                        for (int y = floor + LionCamelCity.BOTTOM - 1; y > Math.max(minY, g - 1); y--) {
                            if (level.getBlockState(p.set(x, y, z)).blocksMotion()) break;
                            level.setBlock(p, s(Blocks.COBBLED_DEEPSLATE), Block.UPDATE_CLIENTS);
                        }
                    }
                } else if (!wet && kind == GROUND && !made) {
                    decorate(level, x, t, z, p);
                }
                // the Lion-Camel Cave, the fortress in the mountain behind the city, its share in this column
                // (its outer rock only where the mountain has none: its halls' own walls keep them)
                if (LionFortress.covers(lx, lz)) {
                    for (int ly = LionFortress.BOTTOM; ly <= LionFortress.TOP; ly++) {
                        int y = floor + ly;
                        if (y < bottom || y > maxY) continue;
                        int cell = LionFortress.cell(lx, ly, lz);
                        int part = LionFortress.kind(cell);
                        if (part == LionFortress.OUT) continue;
                        if (part == LionFortress.ROCK) {
                            BlockState there = level.getBlockState(p.set(x, y, z));
                            if (there.blocksMotion() && there.getFluidState().isEmpty()) continue;
                        }
                        BlockState st = LionFortress.state(cell, lx, ly, lz);
                        if (st != null) LionFortress.lay(level, p.set(x, y, z), st, context.random());
                    }
                }
            }
        }
        return any;
    }

    /**
     * The height the ground at local (lx, lz) is brought to, given {@code t}, the ridge's: the
     * mountain over the fortress raised over it, a road's or an outpost's level ground cut or built.
     */
    private static int shaped(WorldGenLevel level, int[] c, int floor, int lx, int lz, int t, long seed) {
        int m = RidgeShape.massifAt(seed, lx, lz);
        if (m > 0) t = Math.max(t, (int) Math.round(Mth.lerp(RidgeShape.massifBlend(lx, lz), t, floor + m)));
        RidgeApproach.Outpost o = nearOutpost(lx, lz);
        if (o != null) return roadHeight(level, c, floor, o.road(), o.along());
        RidgeApproach.Spot road = RidgeApproach.road(lx, lz);
        if (road != null && !RidgeShape.onApron(lx, lz)) return roadHeight(level, c, floor, road.road(), road.along());
        return t;
    }

    /** The outpost whose levelled ground (its ring and a block round it, and the way joining its road) local (lx, lz) is on, or null. */
    @javax.annotation.Nullable
    private static RidgeApproach.Outpost nearOutpost(int lx, int lz) {
        for (RidgeApproach.Outpost o : RidgeApproach.OUTPOST_LIST) {
            int ox = lx - o.dx(), oz = lz - o.dz();
            if (ox * ox + oz * oz <= (RidgeApproach.OUTPOST_R + 1) * (RidgeApproach.OUTPOST_R + 1)) return o;
            int[] t = RidgeApproach.toRoad(o);
            int along = t[0] * ox + t[1] * oz, side = Math.abs(-t[1] * ox + t[0] * oz);
            if (along >= 0 && along <= RidgeApproach.OUTPOST_ACROSS && side <= 3) return o; // the way from the road to its ring
        }
        return null;
    }

    /** The forecourt before the great door: braziers down the way, skulls on stakes at its sides. */
    private static void forecourt(WorldGenLevel level, int lx, int lz, int floor, int x, int z, BlockPos.MutableBlockPos p) {
        int ax = Math.abs(lx);
        if (ax == 7 && Math.floorMod(lz, 6) == 0) {
            level.setBlock(p.set(x, floor + 1, z), s(Blocks.POLISHED_BLACKSTONE), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, floor + 2, z), Blocks.SOUL_CAMPFIRE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, false), Block.UPDATE_CLIENTS);
        } else if (ax == LionFortress.FORECOURT_HALF && Math.floorMod(lz, 3) == 0) {
            level.setBlock(p.set(x, floor + 1, z), s(Blocks.DARK_OAK_FENCE), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, floor + 2, z), s(Blocks.DARK_OAK_FENCE), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, floor + 3, z), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 8), Block.UPDATE_CLIENTS);
        }
    }

    /** Now and then a skull on a stake, a bone lying, on the ridge's open ground. */
    private static void decorate(WorldGenLevel level, int x, int t, int z, BlockPos.MutableBlockPos p) {
        int r = fine(x, z, 81);
        if (r < 3) {
            level.setBlock(p.set(x, t + 1, z), s(Blocks.DARK_OAK_FENCE), Block.UPDATE_CLIENTS);
            if (r < 2) level.setBlock(p.set(x, t + 2, z), s(Blocks.DARK_OAK_FENCE), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, t + (r < 2 ? 3 : 2), z), Blocks.SKELETON_SKULL.defaultBlockState()
                    .setValue(SkullBlock.ROTATION, hash(x, z, 82) % 16), Block.UPDATE_CLIENTS);
        } else if (r < 9) {
            Direction.Axis axis = hash(x, z, 83) < 50 ? Direction.Axis.X : Direction.Axis.Z;
            level.setBlock(p.set(x, t + 1, z), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), Block.UPDATE_CLIENTS);
        } else if (r < 12) {
            level.setBlock(p.set(x, t + 1, z), Blocks.SKELETON_SKULL.defaultBlockState()
                    .setValue(SkullBlock.ROTATION, hash(x, z, 84) % 16), Block.UPDATE_CLIENTS);
        }
    }
}
