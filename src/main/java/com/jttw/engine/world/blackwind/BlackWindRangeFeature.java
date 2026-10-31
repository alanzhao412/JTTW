package com.jttw.engine.world.blackwind;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.ZoneTerrain;
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
 * Raises Black Wind Mountain (黑风山) out of the cold pine country it takes over ({@link BlackWindShape}):
 * ridges of dark mountain, the black peak at the heart with a cliff on its south face, and the valley
 * road cut from the cliff to the region's edge, its forecourt before the door and the temple's terrace
 * level; and lays the Black Wind Cave itself, its facade and door, its passage, the court, the grand
 * stair and the throne cavern, column by column ({@link BlackWindPlan}), so it comes out whole across
 * the chunks it spans.
 * <ul>
 *   <li><b>faces</b> (a step of three or more to a neighbour) show the peak's dark rock in bands;</li>
 *   <li><b>the slopes</b> are grass and podzol, darker in the hollows, for the pines that grow over them
 *       (the biome's own trees, the old pine taiga's);</li>
 *   <li><b>the summit</b> is bare: blackstone, basalt and gravel;</li>
 *   <li><b>the valley's floor</b> is grass, gravel and coarse earth, the road up its middle trodden earth
 *       edged with cobbles, the forecourt paved in dark stone.</li>
 * </ul>
 * Like the Flaming Mountains' range it rides on the land's broad shape, fills any water in its way, and
 * runs first among the zone's features.
 */
public class BlackWindRangeFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = BlackWindShape.ZONE;
    private static final int STEEP = 3;
    private static final int FACE = 0, SLOPE = 1, HOLLOW = 2, SUMMIT = 3, VALLEY = 4, COURT = 5;
    /** How high over the forecourt the bare summit begins. */
    public static final int SUMMIT_OVER = 120;

    public BlackWindRangeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The valley's floors of the region round (cx, cz) in this level (the cave's floor is {@code court}). */
    public static BlackWindShape.Floors floors(WorldGenLevel level, int cx, int cz) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = ZoneTerrain.landTop(level);
        return BlackWindShape.floors(level.getSeed(), cx, cz, (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY), level.getSeaLevel() + 2);
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState topOf(int kind, int x, int z, int lx, int lz) {
        int roll = hash(x >> 1, z >> 1, 7);
        switch (kind) {
            case COURT -> {
                if (Math.abs(lx) <= 3) return Math.abs(lx) == 3 ? s(Blocks.POLISHED_DEEPSLATE) : hash(x, z, 9) < 20 ? s(Blocks.CRACKED_DEEPSLATE_TILES) : s(Blocks.DEEPSLATE_TILES);
                return roll < 40 ? s(Blocks.GRAVEL) : roll < 70 ? s(Blocks.TUFF) : roll < 85 ? s(Blocks.COBBLED_DEEPSLATE) : s(Blocks.COARSE_DIRT);
            }
            case VALLEY -> {
                if (BlackWindShape.onRoad(lx, lz)) return s(Blocks.DIRT_PATH);
                if (Math.abs(lx) == 3) return hash(x, z, 8) < 60 ? s(Blocks.COBBLESTONE) : s(Blocks.MOSSY_COBBLESTONE);
                return roll < 25 ? s(Blocks.COARSE_DIRT) : roll < 40 ? s(Blocks.GRAVEL) : roll < 48 ? s(Blocks.MOSS_BLOCK) : s(Blocks.GRASS_BLOCK);
            }
            case SUMMIT -> {
                return roll < 35 ? s(Blocks.BLACKSTONE) : roll < 55 ? s(Blocks.BASALT) : roll < 75 ? s(Blocks.GRAVEL) : s(Blocks.DEEPSLATE);
            }
            case HOLLOW -> {
                return roll < 55 ? s(Blocks.PODZOL) : roll < 70 ? s(Blocks.COARSE_DIRT) : roll < 80 ? s(Blocks.MOSS_BLOCK) : s(Blocks.GRASS_BLOCK);
            }
            case SLOPE -> {
                return roll < 18 ? s(Blocks.PODZOL) : roll < 26 ? s(Blocks.COARSE_DIRT) : roll < 30 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.GRASS_BLOCK);
            }
            default -> {
                return BlackWindBlocks.rock(0, x, z);
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
            rises[i][j] = BlackWindShape.riseAt(seed, x0 + i - 1, z0 + j - 1);
            inside |= ZONE.depth(seed, x0 + i - 1, z0 + j - 1) > 0;
        }
        if (!inside) return false;
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), landTop = ZoneTerrain.landTop(level), ceiling = level.getMaxBuildHeight() - 24;
        int[][] made = new int[5][5];
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) made[i][j] = ZoneTerrain.landAt(shape, x0 + i * 4, z0 + j * 4, minY, landTop);
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        BlackWindShape.Floors f = c == null ? null : floors(level, c[0], c[1]);
        BlackWindPlan.Built plan = BlackWindPlan.get();
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (!ZONE.isAt(level, p.set(x, surface, z))) continue;
                int i = dx >> 2, j = dz >> 2;
                int land = (int) Math.round(Mth.lerp2((dx & 3) / 4.0, (dz & 3) / 4.0, made[i][j], made[i + 1][j], made[i][j + 1], made[i + 1][j + 1]));
                int rise = rises[dx + 1][dz + 1];
                int step = Math.max(Math.max(Math.abs(rises[dx][dz + 1] - rise), Math.abs(rises[dx + 2][dz + 1] - rise)),
                        Math.max(Math.abs(rises[dx + 1][dz] - rise), Math.abs(rises[dx + 1][dz + 2] - rise)));
                int lx = c == null ? Integer.MAX_VALUE / 2 : x - c[0], lz = c == null ? Integer.MAX_VALUE / 2 : z - c[1];
                int natural = Math.max(land, ground);
                double share = c == null ? 1 : BlackWindShape.valleyShare(lx, lz);
                boolean valley = share < 1;
                int top;
                if (valley) top = (int) Math.round(Mth.lerp(share, BlackWindShape.floorAt(f, lz), Math.max(natural + rise, BlackWindShape.floorAt(f, lz))));
                else top = Math.max(natural + rise, surface);
                if (c != null && BlackWindPlan.underMountain(lx, lz)) {
                    // the peak always stands over the cave, its cliff over the door
                    top = Math.max(top, f.court() + BlackWindPlan.Y_MAX + 3 + hash(x, z, 4) % 4);
                }
                top = Math.min(top, ceiling);
                boolean floorHere = c != null && BlackWindShape.onValleyFloor(lx, lz) && share < 0.05;
                int kind;
                if (floorHere) kind = lz <= BlackWindShape.COURT_END ? COURT : VALLEY;
                else if (step >= STEEP) kind = FACE;
                else if (c != null && top > f.court() + SUMMIT_OVER) kind = SUMMIT;
                else kind = BlackWindShape.hollow(seed, x, z) ? HOLLOW : SLOPE;
                for (int y = top + 1; y <= surface; y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                for (int y = Math.min(ground, valley && top < ground ? top - 12 : top); y <= top; y++) {
                    BlockState st;
                    if (y == top) st = kind == FACE ? BlackWindBlocks.rock(y, x, z) : topOf(kind, x, z, lx, lz);
                    else if (kind == FACE || kind == SUMMIT || y < top - 3) st = BlackWindBlocks.rock(y, x, z);
                    else st = kind == COURT ? s(Blocks.TUFF) : s(Blocks.DIRT);
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                any = true;
                // the Black Wind Cave, its share in this column
                if (c != null && BlackWindPlan.covers(lx, lz)) {
                    int floor = f.court();
                    int lo = Math.max(BlackWindPlan.Y_MIN, minY + 1 - floor), hi = Math.min(BlackWindPlan.Y_MAX, ceiling - floor);
                    for (int ly = lo; ly <= hi; ly++) {
                        BlockState st = BlackWindBlocks.state(plan, lx, ly, lz, x, floor + ly, z);
                        if (st != null) BlackWindBlocks.lay(level, p.set(x, floor + ly, z), st, context.random());
                    }
                }
            }
        }
        return any;
    }
}
