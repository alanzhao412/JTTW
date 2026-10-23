package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.FireCloudCave;
import com.jttw.engine.world.fire.RangeShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Raises the Flaming Mountains (火焰山) out of the hot, dry country they take over ({@link RangeShape}):
 * long chains of red mountains with valleys of burning earth between, Mount Haohao at the heart, and
 * the gorge cut through to the Fire Cloud Cave's stone gate; and lays the cave itself, column by
 * column ({@link FireCloudCave}), so it comes out whole across the chunks it spans.
 * <ul>
 *   <li><b>faces</b> (a step of three or more to a neighbour) show the mountains' red rock in bands of
 *       red, orange and brown terracotta, seams of magma in it;</li>
 *   <li><b>valley floors</b> are {@link com.jttw.engine.world.fire.BurningEarthBlock burning earth},
 *       lit: the heat;</li>
 *   <li><b>high ground</b> is flame rock and cinder ash, specked with magma;</li>
 *   <li>the <b>forecourt</b> before the gate is ash, a road of blackstone up its middle.</li>
 * </ul>
 * Like the ridge's massif it rides on the land's broad shape (no cave or canyon leaves a pit in it),
 * fills any water in its way, and runs first among the zone's features; later ones must find the
 * ground with the live heightmaps.
 */
public class FlamingRangeFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = RangeShape.ZONE;
    /** A step this high or more to a neighbour makes a face, not ground. */
    private static final int STEEP = 3;

    private static final int FACE = 0, VALLEY = 1, HIGH = 2, COURT = 3;

    public FlamingRangeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The hall's floor of the region round (cx, cz) in this level. */
    public static int hallFloor(WorldGenLevel level, int cx, int cz) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        return RangeShape.hallFloor(level.getSeed(), cx, cz, (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY), level.getSeaLevel() + 2);
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    private static int patch(int x, int z, int size, int salt) {
        return hash(Math.floorDiv(x, size), Math.floorDiv(z, size), salt);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** The mountains' rock inside, in uneven bands of red, orange and brown, a seam of magma now and then. */
    private static BlockState inner(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 31 + z * 17) >> 5), 11);
        return switch (band) {
            case 0 -> s(Blocks.RED_TERRACOTTA);
            case 3 -> s(Blocks.ORANGE_TERRACOTTA);
            case 6 -> s(Blocks.TERRACOTTA);
            case 9 -> hash(x >> 2, z >> 2, y) < 30 ? s(Blocks.MAGMA_BLOCK) : s(ModBlocks.FLAME_ROCK.get());
            default -> s(ModBlocks.FLAME_ROCK.get());
        };
    }

    private static BlockState topOf(int kind, int x, int z, int dx) {
        switch (kind) {
            case VALLEY -> {
                return s(ModBlocks.BURNING_EARTH.get());
            }
            case COURT -> {
                return Math.abs(dx) <= 1 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(ModBlocks.CINDER_ASH.get());
            }
            case HIGH -> {
                int roll = patch(x, z, 3, 7);
                if (roll < 45) return s(ModBlocks.CINDER_ASH.get());
                if (roll < 49) return s(Blocks.MAGMA_BLOCK);
                if (roll < 56) return s(Blocks.RED_TERRACOTTA);
                return s(ModBlocks.FLAME_ROCK.get());
            }
            default -> {
                return s(ModBlocks.FLAME_ROCK.get());
            }
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[][] rises = new int[18][18];
        boolean inside = false;
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            rises[i][j] = RangeShape.riseAt(seed, x0 + i - 1, z0 + j - 1);
            inside |= ZONE.depth(seed, x0 + i - 1, z0 + j - 1) > 0;
        }
        if (!inside) return false;
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        int[][] made = new int[5][5];
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) made[i][j] = ZoneTerrain.landAt(shape, x0 + i * 4, z0 + j * 4, minY, maxY);
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8); // a chunk lies in one cell
        int floor = c == null ? Integer.MIN_VALUE : hallFloor(level, c[0], c[1]);
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; // under any water, or down a canyon
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (!ZONE.isAt(level, p.set(x, surface, z))) continue;
                int i = dx >> 2, j = dz >> 2;
                int land = (int) Math.round(Mth.lerp2((dx & 3) / 4.0, (dz & 3) / 4.0, made[i][j], made[i + 1][j], made[i][j + 1], made[i + 1][j + 1]));
                int rise = rises[dx + 1][dz + 1];
                int step = Math.max(Math.max(Math.abs(rises[dx][dz + 1] - rise), Math.abs(rises[dx + 2][dz + 1] - rise)),
                        Math.max(Math.abs(rises[dx + 1][dz] - rise), Math.abs(rises[dx + 1][dz + 2] - rise)));
                int lx = c == null ? Integer.MAX_VALUE / 2 : x - c[0], lz = c == null ? Integer.MAX_VALUE / 2 : z - c[1];
                int natural = Math.max(land, ground);
                double share = c == null ? 1 : RangeShape.floorShare(lx, lz);
                boolean gorge = share < 1;
                int base = gorge ? (int) Math.round(Mth.lerp(share, floor, natural)) : natural;
                int top = base + rise;
                if (!gorge) top = Math.max(top, surface); // at least as high as any water stood, so none is left on it
                top = Math.min(top, maxY - 2);
                int kind = c != null && RangeShape.onForecourt(lx, lz) ? COURT
                        : step >= STEEP ? FACE : RangeShape.valley(seed, x, z) ? VALLEY : HIGH;
                // the gorge is cut down to its floor
                for (int y = top + 1; y <= surface; y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                // (cut down, the rock under its new top is the range's too, where a step shows it)
                for (int y = Math.min(ground, gorge && top < ground ? top - 12 : top); y <= top; y++) {
                    BlockState st;
                    if (y == top) st = topOf(kind, x, z, lx);
                    else if (kind == FACE || y < top - 3) st = inner(y, x, z);
                    else st = kind == HIGH && y == top - 1 && hash(x, z, 3) < 50 ? s(ModBlocks.CINDER_ASH.get()) : s(ModBlocks.FLAME_ROCK.get());
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                any = true;
                // the Fire Cloud Cave, its share in this column
                if (c != null && FireCloudCave.covers(lx, lz)) {
                    for (int ly = FireCloudCave.BOTTOM; ly <= FireCloudCave.TOP; ly++) {
                        BlockState st = FireCloudCave.at(lx, ly, lz);
                        if (st != null) level.setBlock(p.set(x, floor + ly, z), st, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return any;
    }
}
