package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.kunlun.Kunlun;
import com.jttw.engine.world.kunlun.KunlunPalace;
import com.jttw.engine.world.kunlun.KunlunShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * Makes Mount Kunlun of the mountain country it takes over: raises its peak ({@link KunlunShape})
 * column by column, cuts the stair up it, and lays the Queen Mother's palace on its summit
 * ({@link KunlunPalace}), so it all comes out whole across the chunks it spans, signs and treasure too.
 * <ul>
 *   <li>its <b>flanks</b>: grass and pines low down, bare rock (banded with white calcite) where they
 *       are steep, and from the snow line up snow; where it meets water it rises out of it;</li>
 *   <li>the <b>stair</b>: three or four blocks wide, stone brick in whole and half steps, cut into the
 *       flank where it is higher and built out on stone where it is lower, a white rail along the drop,
 *       a lantern on a post every so often on the outer side;</li>
 *   <li>the <b>summit</b>: snow round the palace's terrace, the palace and its signs; the treasure
 *       chest given its loot ({@link Kunlun#TREASURE}) as it is laid;</li>
 *   <li><b>clouds</b> (celestial cloud) round its shoulders, never on the stair;</li>
 *   <li>round it, its <b>country</b> keeps the land's shape: its steep places bare rock, its high
 *       places snow, grass and ferns and a pine here and there lower down.</li>
 * </ul>
 */
public class KunlunFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = Zones.KUNLUN;
    /** A step this high or more to a neighbour makes a face, not ground. */
    private static final int STEEP = 3;
    /** A lantern on the rail every this many blocks along the stair. */
    public static final int LAMP_EVERY = 18;
    /** The country's snow line, away from the peak. */
    private static final int COUNTRY_SNOW = 190;

    private static final int FACE = 0, GROUND = 1, WAY = 2, EDGE = 3, TOP = 4;

    public KunlunFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The land's own height by its broad shape, in this level. */
    private static IntBinaryOperator land(WorldGenLevel level) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        return (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY);
    }

    /** The summit's ground (its top block's y) in the region centred at (cx, cz) in this level. */
    public static int summit(WorldGenLevel level, int cx, int cz) {
        int max = level.getMaxBuildHeight() - KunlunShape.UNDER_SKY;
        return KunlunShape.summit(level.getSeed(), cx, cz, land(level), Math.min(KunlunShape.MIN_SUMMIT, max), max);
    }

    /** The stair up the mountain in the region centred at (cx, cz) in this level. */
    public static KunlunShape.Path path(WorldGenLevel level, int cx, int cz) {
        return KunlunShape.path(level.getSeed(), cx, cz, summit(level, cx, cz), land(level));
    }

    /** The land's height round the mountain's foot in the region centred at (cx, cz) in this level. */
    public static int foot(WorldGenLevel level, int cx, int cz) {
        return KunlunShape.foot(level.getSeed(), cx, cz, land(level));
    }

    private static int fine(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 41 + 9, z), 1000L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState rock(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 23 + z * 11) >> 5), 11);
        return band == 0 || band == 1 ? s(Blocks.CALCITE) : band == 5 ? s(Blocks.ANDESITE) : band == 8 ? s(Blocks.DIORITE) : s(Blocks.STONE);
    }

    /** The top block of a column, of its kind, at height y, the snow from {@code snow} up. */
    private static BlockState top(int kind, int x, int y, int z, double snow) {
        return switch (kind) {
            case FACE -> y >= snow && fine(x, z, 9) < Math.min(940, 600 + (y - snow) * 40) ? s(Blocks.SNOW_BLOCK) : rock(y, x, z);
            case WAY -> {
                int q = fine(x, z, 7);
                yield q < 600 ? s(Blocks.STONE_BRICKS) : q < 800 ? s(Blocks.CRACKED_STONE_BRICKS) : y < snow - 20 && q < 920 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE);
            }
            case EDGE -> s(Blocks.STONE_BRICKS);
            case TOP -> s(Blocks.SNOW_BLOCK);
            default -> {
                if (y >= snow) yield s(Blocks.SNOW_BLOCK);
                int q = fine(x, z, 8);
                if (y >= snow - 8) yield q < 450 ? s(Blocks.SNOW_BLOCK) : q < 700 ? s(Blocks.STONE) : q < 850 ? s(Blocks.GRAVEL) : s(Blocks.GRASS_BLOCK);
                yield q < 70 ? s(Blocks.COARSE_DIRT) : q < 100 ? s(Blocks.MOSS_BLOCK) : s(Blocks.GRASS_BLOCK);
            }
        };
    }

    private static BlockState under(int kind, int x, int y, int z, BlockState top) {
        if (kind == FACE) return top.is(Blocks.SNOW_BLOCK) ? top : rock(y, x, z); // a snowy cliff white all down its face
        if (kind == WAY || kind == EDGE) return s(Blocks.STONE);
        if (top.is(Blocks.SNOW_BLOCK)) return s(Blocks.SNOW_BLOCK);
        if (top.is(Blocks.STONE) || top.is(Blocks.GRAVEL)) return rock(y, x, z);
        return s(Blocks.DIRT);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        boolean inside = false;
        for (int i = 0; i < 18 && !inside; i += 17) for (int j = 0; j < 18; j += 17) inside |= ZONE.depth(seed, x0 + i - 1, z0 + j - 1) > 0;
        if (!inside) for (int i = 0; i < 16 && !inside; i += 3) for (int j = 0; j < 16; j += 3) inside |= ZONE.depth(seed, x0 + i, z0 + j) > 0;
        if (!inside) return false;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        int minY = level.getMinBuildHeight() + 1, maxY = level.getMaxBuildHeight() - 2;
        int summit = summit(level, c[0], c[1]), foot = foot(level, c[0], c[1]);
        boolean near = Math.abs(x0 + 8 - c[0]) <= KunlunShape.REACH + 12 && Math.abs(z0 + 8 - c[1]) <= KunlunShape.REACH + 12;
        KunlunShape.Path path = near ? path(level, c[0], c[1]) : null;
        // the height each column (and the ring round the chunk) is brought to, and what it is
        int[][] target = new int[18][18], ground = new int[18][18], surface = new int[18][18];
        boolean[][] mountain = new boolean[18][18], rail = new boolean[18][18];
        KunlunShape.Hit[][] hits = new KunlunShape.Hit[18][18];
        int[][] role = new int[18][18]; // 0 none, WAY, EDGE (a rail where it is set)
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            int x = x0 + i - 1, z = z0 + j - 1, lx = x - c[0], lz = z - c[1];
            int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; // under any water
            int sur = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            ground[i][j] = g;
            surface[i][j] = sur;
            if (!near) {
                target[i][j] = g;
                continue;
            }
            boolean summitTop = KunlunPalace.covers(lx, lz);
            double h = summitTop ? summit : KunlunShape.height(seed, x, z, lx, lz, g, summit);
            mountain[i][j] = summitTop || KunlunShape.down(seed, x, z, lx, lz) < 1;
            KunlunShape.Hit bank = summitTop ? null : path.nearest(lx, lz, KunlunShape.SHOULDER + KunlunShape.BANK);
            KunlunShape.Hit hit = bank != null && bank.d() <= KunlunShape.SHOULDER ? bank : null;
            hits[i][j] = hit;
            int t = (int) Math.round(h);
            if (bank != null && hit == null) {
                // the bank cut back from the stair's edge: gently on the drop side, steeply uphill
                double cap = bank.y() + 1 + (bank.d() - KunlunShape.SHOULDER) * (bank.outer() ? 0.8 : 2.2);
                if (h > cap) {
                    t = (int) Math.round(cap);
                    mountain[i][j] = true;
                }
            }
            if (hit != null) {
                int py = (int) Math.floor(hit.y());
                if (hit.d() <= KunlunShape.PATH_HALF) {
                    t = py;
                    role[i][j] = WAY;
                } else if (h < hit.y() - 0.5) {
                    t = py;
                    role[i][j] = EDGE;
                    rail[i][j] = true;
                } else if (h <= hit.y() + 1.5) {
                    t = py;
                    role[i][j] = EDGE;
                }
            }
            if ((mountain[i][j] || hit != null) && sur > g) t = Math.max(t, sur); // over water it rises out of it
            target[i][j] = Mth.clamp(t, minY, maxY);
        }
        boolean any = false;
        List<BlockPos> trees = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                any = true;
                int i = dx + 1, j = dz + 1;
                int g = ground[i][j], sur = surface[i][j], t = target[i][j];
                int lx = x - c[0], lz = z - c[1];
                int step = Math.max(Math.max(Math.abs(target[i - 1][j] - t), Math.abs(target[i + 1][j] - t)),
                        Math.max(Math.abs(target[i][j - 1] - t), Math.abs(target[i][j + 1] - t)));
                boolean inner = dx >= 3 && dx <= 12 && dz >= 3 && dz <= 12;
                double snow = near ? KunlunShape.snowLine(seed, x, z, summit) : COUNTRY_SNOW;
                if (!mountain[i][j] && role[i][j] == 0) {
                    if (sur <= g) country(context, x, g, z, step, snow, inner && (path == null || path.nearest(lx, lz, KunlunShape.SHOULDER + 4) == null), trees, p);
                    if (near) clouds(level, seed, x, z, lx, lz, t, summit, foot, path, p);
                    continue;
                }
                boolean summitTop = KunlunPalace.covers(lx, lz);
                int kind = role[i][j] == WAY ? WAY : role[i][j] == EDGE ? EDGE : summitTop ? TOP : step >= STEEP ? FACE : GROUND;
                // cut down to it (water over it is built over: see the targets)
                for (int y = Math.max(t + 1, minY); y <= Math.max(sur, g); y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                // built up to it, or its top few blocks made over
                BlockState topState = top(kind, x, t, z, snow);
                int skin = kind == FACE && topState.is(Blocks.SNOW_BLOCK) ? Math.min(step, 12) + 1 : 3; // how deep its top's kind goes: down a snowy cliff's whole face
                for (int y = Math.max(minY, Math.min(g + 1, t - skin)); y <= t; y++) {
                    if (y < t && y <= g && !level.getBlockState(p.set(x, y, z)).blocksMotion()) continue; // a cave just under it left open
                    BlockState st = y == t ? topState : y > t - skin ? under(kind, x, y, z, topState) : rock(y, x, z);
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                KunlunShape.Hit hit = hits[i][j];
                if (kind == WAY && hit.y() - Math.floor(hit.y()) >= 0.5) {
                    level.setBlock(p.set(x, t + 1, z), Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), Block.UPDATE_CLIENTS);
                } else if (rail[i][j]) {
                    boolean lamp = hit.outer() && hit.index() % LAMP_EVERY == 0;
                    level.setBlock(p.set(x, t + 1, z), railState(rail, target, i, j, lamp), Block.UPDATE_CLIENTS);
                    if (lamp) {
                        level.setBlock(p.set(x, t + 2, z), s(Blocks.STONE_BRICK_WALL), Block.UPDATE_CLIENTS);
                        level.setBlock(p.set(x, t + 3, z), s(Blocks.LANTERN), Block.UPDATE_CLIENTS);
                    }
                }
                // the summit: the palace, its signs, the treasure
                if (summitTop) {
                    for (int ly = KunlunPalace.BOTTOM; ly <= KunlunPalace.TOP; ly++) {
                        BlockState st = KunlunPalace.at(lx, ly, lz);
                        int y = summit + ly;
                        if (st != null && y >= minY && y <= maxY) level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                    }
                    for (KunlunPalace.Sign sign : KunlunPalace.SIGNS) {
                        if (sign.x() == lx && sign.z() == lz) KunlunPalace.putSign(level, new BlockPos(x, summit + sign.y(), z), sign);
                    }
                    if (lx == KunlunPalace.CHEST_X && lz == KunlunPalace.CHEST_Z) {
                        RandomizableContainer.setBlockEntityLootTable(level, context.random(), new BlockPos(x, summit + KunlunPalace.CHEST_Y, z), Kunlun.TREASURE);
                    }
                    continue;
                }
                if (kind == GROUND && level.getFluidState(p.set(x, t + 1, z)).isEmpty() && t < snow - 8) {
                    boolean clear = path.nearest(lx, lz, KunlunShape.SHOULDER + 4) == null;
                    if (inner && clear && fine(x, z, 82) < 14 && level.getBlockState(p.set(x, t, z)).is(Blocks.GRASS_BLOCK)) {
                        trees.add(new BlockPos(x, t + 1, z)); // a pine on the flank
                    } else if (fine(x, z, 81) < 260 && level.getBlockState(p.set(x, t, z)).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p.set(x, t + 1, z), s(fine(x, z, 83) < 300 ? Blocks.FERN : Blocks.SHORT_GRASS), Block.UPDATE_CLIENTS);
                    }
                }
                clouds(level, seed, x, z, lx, lz, t, summit, foot, path, p);
            }
        }
        // the pines, once the chunk's ground is all made
        var spruce = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .getHolder(net.minecraft.data.worldgen.features.TreeFeatures.SPRUCE);
        for (BlockPos at : trees) {
            if (!level.getBlockState(at).isAir()) continue;
            if (spruce.isPresent()) spruce.get().value().place(level, context.chunkGenerator(), context.random(), at);
        }
        return any;
    }

    /** A rail post on the stair's edge, joined to the rails beside it at its height; a post, if a lamp stands on it. */
    private static BlockState railState(boolean[][] rail, int[][] target, int i, int j, boolean lamp) {
        int t = target[i][j];
        boolean n = rail[i][j - 1] && target[i][j - 1] == t, so = rail[i][j + 1] && target[i][j + 1] == t;
        boolean e = rail[i + 1][j] && target[i + 1][j] == t, w = rail[i - 1][j] && target[i - 1][j] == t;
        boolean straight = (n && so && !e && !w) || (e && w && !n && !so);
        return Blocks.STONE_BRICK_WALL.defaultBlockState()
                .setValue(WallBlock.NORTH_WALL, n ? WallSide.LOW : WallSide.NONE).setValue(WallBlock.SOUTH_WALL, so ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.EAST_WALL, e ? WallSide.LOW : WallSide.NONE).setValue(WallBlock.WEST_WALL, w ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.UP, lamp || !straight);
    }

    /** The clouds round the mountain's shoulders, in the open over column (x, z), its ground at {@code t}; never close over the stair. */
    private static void clouds(WorldGenLevel level, long seed, int x, int z, int lx, int lz, int t, int summit, int foot,
                               KunlunShape.Path path, BlockPos.MutableBlockPos p) {
        int from = summit - KunlunShape.CLOUD_BELOW - KunlunShape.CLOUD_HALF, to = summit - KunlunShape.CLOUD_BELOW + KunlunShape.CLOUD_HALF;
        if (t + 2 > to) return;
        KunlunShape.Hit near = path.nearest(lx, lz, KunlunShape.SHOULDER + 3);
        for (int y = Math.max(from, t + 2); y <= to; y++) {
            if (near != null && y < near.y() + 12) continue;
            if (KunlunShape.cloud(seed, x, y, z, lx, lz, summit, foot) <= 0) continue;
            if (level.getBlockState(p.set(x, y, z)).isAir()) level.setBlock(p, ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Kunlun's country round its peak, the land's shape kept: steep places bare rock, high places
     * snow, ferns and grass, a pine now and then ({@code trees}, if {@code room}: well inside the chunk, off the stair).
     */
    private static void country(FeaturePlaceContext<NoneFeatureConfiguration> context, int x, int g, int z, int step, double snow, boolean room,
                                List<BlockPos> trees, BlockPos.MutableBlockPos p) {
        WorldGenLevel level = context.level();
        BlockState top = level.getBlockState(p.set(x, g, z));
        if (!top.is(Blocks.GRASS_BLOCK) && !top.is(Blocks.DIRT) && !top.is(Blocks.SNOW_BLOCK)) return;
        if (step >= STEEP + 1) {
            level.setBlock(p, rock(g, x, z), Block.UPDATE_CLIENTS);
            return;
        }
        if (g >= snow) {
            level.setBlock(p, s(Blocks.SNOW_BLOCK), Block.UPDATE_CLIENTS);
            return;
        }
        if (!top.is(Blocks.GRASS_BLOCK) || !level.getBlockState(p.set(x, g + 1, z)).isAir()) return;
        int r = fine(x, z, 72);
        if (r < 10 && room) trees.add(new BlockPos(x, g + 1, z));
        else if (r < 220) level.setBlock(p.set(x, g + 1, z), s(r < 80 ? Blocks.FERN : Blocks.SHORT_GRASS), Block.UPDATE_CLIENTS);
    }
}
