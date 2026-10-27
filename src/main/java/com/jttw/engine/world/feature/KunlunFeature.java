package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.kunlun.Kunlun;
import com.jttw.engine.world.kunlun.KunlunPalace;
import com.jttw.engine.world.kunlun.KunlunShape;
import com.jttw.engine.world.kunlun.KunlunWay;
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
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * Makes Mount Kunlun of the mountain country it takes over: raises its massif ({@link KunlunShape})
 * column by column, cuts the pilgrim's way up it with its bridges, gate, pavilions and shrines
 * ({@link KunlunWay}), and lays the Queen Mother's palace on its summit ({@link KunlunPalace}), so it
 * all comes out whole across the chunks it spans, signs and treasure too.
 * <ul>
 *   <li>its <b>flanks</b>: grass, ferns and pines low down, alpine stone, gravel and moss over the
 *       trees, bare rock (banded with white calcite, andesite and tuff) where they are steep, and
 *       over the snow line snow, more and more of the cliffs white the higher they stand; in the
 *       hollows high up, glaciers of packed ice, falling over the steeps in ice falls of blue ice;
 *       a spring high in each gorge, its stream falling down it; where it meets water it rises out
 *       of it;</li>
 *   <li>the <b>way</b>: three or four blocks wide, stone brick in whole and half steps, cut into the
 *       flank where it is higher and built out on stone where it is lower, crossing deep drops and
 *       the gorges on bridges of arches (the gorge cut down under them for its stream), a white rail
 *       along any drop, a lantern on a post every so often on the outer side;</li>
 *   <li>the <b>summit</b>: snow round the palace's terrace, the palace and its signs; the treasure
 *       chest given its loot ({@link Kunlun#TREASURE}) as it is laid;</li>
 *   <li><b>clouds</b> (celestial cloud) in belts round it at three heights, never on the way;</li>
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
    /** The country's snow line, away from the massif. */
    private static final int COUNTRY_SNOW = 190;
    /** A hollow this deep (blocks under the ground round it) over the snow line, less this, holds a glacier. */
    private static final double GLACIER_HOLLOW = 3.5;
    private static final int GLACIER_UNDER_SNOW = 25;

    private static final int FACE = 0, GROUND = 1, WAY = 2, EDGE = 3, TOP = 4, ICE = 5;

    public KunlunFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The land's own height by its broad shape, in this level. */
    public static IntBinaryOperator land(WorldGenLevel level) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = ZoneTerrain.landTop(level);
        return (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY);
    }

    /** The summit's ground (its top block's y) in the region centred at (cx, cz) in this level: in a lower world, as high as its sky allows. */
    public static int summit(WorldGenLevel level, int cx, int cz) {
        return summit(level.getSeed(), cx, cz, land(level), level.getMaxBuildHeight());
    }

    /**
     * The summit's ground in the region centred at (cx, cz) of a world whose sky is at {@code sky} (its
     * build height), the land {@code land}: y {@value KunlunShape#SUMMIT_Y} under the taller sky; under
     * an older world's (y 320), {@value KunlunShape#UNDER_SKY} under it, the whole massif pressed down
     * to fit, its plan the same.
     */
    public static int summit(long seed, int cx, int cz, IntBinaryOperator land, int sky) {
        int max = sky - KunlunShape.UNDER_SKY;
        return KunlunShape.summit(seed, cx, cz, land, Math.min(KunlunShape.MIN_SUMMIT, max), max);
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

    /** The mountain's rock at height y: stone in bands of white calcite, andesite, tuff and diorite, tilted a little. */
    private static BlockState rock(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 23 + z * 11) >> 5), 13);
        return switch (band) {
            case 0, 1 -> s(Blocks.CALCITE);
            case 5 -> s(Blocks.ANDESITE);
            case 8 -> s(Blocks.TUFF);
            case 10 -> s(Blocks.DIORITE);
            default -> s(Blocks.STONE);
        };
    }

    /** The top block of a column, of its kind, at height y, the snow from {@code snow} up. */
    private static BlockState top(int kind, int x, int y, int z, double snow) {
        return switch (kind) {
            case FACE -> y >= snow && fine(x, z, 9) < Math.min(940, 560 + (y - snow) * 12) ? s(Blocks.SNOW_BLOCK) : rock(y, x, z);
            case ICE -> fine(x, z, 11) < 380 ? s(Blocks.BLUE_ICE) : fine(x, z, 12) < 120 ? s(Blocks.SNOW_BLOCK) : s(Blocks.PACKED_ICE);
            case WAY -> {
                int q = fine(x, z, 7);
                yield q < 600 ? s(Blocks.STONE_BRICKS) : q < 800 ? s(Blocks.CRACKED_STONE_BRICKS) : y < snow - 20 && q < 920 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE);
            }
            case EDGE -> s(Blocks.STONE_BRICKS);
            case TOP -> s(Blocks.SNOW_BLOCK);
            default -> {
                if (y >= snow) yield s(Blocks.SNOW_BLOCK);
                int q = fine(x, z, 8);
                if (y >= snow - 10) yield q < 450 ? s(Blocks.SNOW_BLOCK) : q < 700 ? s(Blocks.STONE) : q < 850 ? s(Blocks.GRAVEL) : s(Blocks.COARSE_DIRT);
                if (y >= snow - KunlunShape.TREE_LINE) yield q < 300 ? s(Blocks.STONE) : q < 450 ? s(Blocks.GRAVEL) : q < 560 ? s(Blocks.MOSS_BLOCK)
                        : q < 640 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK);
                yield q < 70 ? s(Blocks.COARSE_DIRT) : q < 100 ? s(Blocks.MOSS_BLOCK) : q < 140 ? s(Blocks.PODZOL) : s(Blocks.GRASS_BLOCK);
            }
        };
    }

    private static BlockState under(int kind, int x, int y, int z, BlockState top) {
        if (kind == ICE) return s(Blocks.PACKED_ICE);
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
        // the land's own height in the chunk, before it is touched (the ring round it judged by the chunk's own edge)
        int[][] ownGround = new int[16][16], ownSurface = new int[16][16];
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            ownGround[i][j] = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x0 + i, z0 + j) - 1; // under any water
            ownSurface[i][j] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x0 + i, z0 + j) - 1;
        }
        // the land's broad shape round the chunk, every 4 blocks (the massif is raised from it, as its stair is laid out on it)
        int[][] broad = new int[7][7];
        if (near) {
            IntBinaryOperator land = land(level);
            for (int i = 0; i < 7; i++) for (int j = 0; j < 7; j++) broad[i][j] = land.applyAsInt(x0 - 4 + 4 * i, z0 - 4 + 4 * j);
        }
        // the height each column (and the ring round the chunk) is brought to, and what it is
        int[][] target = new int[18][18], below = new int[18][18];
        boolean[][] mountain = new boolean[18][18], rail = new boolean[18][18], bridge = new boolean[18][18], siteFloor = new boolean[18][18];
        KunlunShape.Hit[][] hits = new KunlunShape.Hit[18][18];
        KunlunShape.Site[][] sites = new KunlunShape.Site[18][18];
        int[][] role = new int[18][18]; // 0 none, WAY, EDGE (a rail where it is set)
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            int x = x0 + i - 1, z = z0 + j - 1, lx = x - c[0], lz = z - c[1];
            int g = ownGround[Mth.clamp(i - 1, 0, 15)][Mth.clamp(j - 1, 0, 15)];
            int sur = ownSurface[Mth.clamp(i - 1, 0, 15)][Mth.clamp(j - 1, 0, 15)];
            if (!near) {
                target[i][j] = below[i][j] = g;
                continue;
            }
            boolean summitTop = KunlunPalace.covers(lx, lz);
            double e = summitTop ? 1 : KunlunShape.field(seed, x, z, lx, lz);
            // the land the massif rises from: its broad shape, but at the very foot its own ground, crags and all
            double fx = (x - x0 + 4) / 4.0, fz = (z - z0 + 4) / 4.0;
            int gi = Mth.clamp((int) Math.floor(fx), 0, 5), gj = Mth.clamp((int) Math.floor(fz), 0, 5);
            double wide = Mth.lerp2(fx - gi, fz - gj, broad[gi][gj], broad[gi + 1][gj], broad[gi][gj + 1], broad[gi + 1][gj + 1]);
            int natural = (int) Math.round(Mth.lerp(smoothstep(0, 0.04, e), g, wide));
            double h = summitTop ? summit : KunlunShape.height(seed, x, z, lx, lz, natural, summit);
            mountain[i][j] = summitTop || e > 0;
            KunlunShape.Hit bank = summitTop ? null : path.nearest(lx, lz, KunlunShape.SHOULDER + KunlunShape.BANK);
            KunlunShape.Hit hit = bank != null && bank.d() <= KunlunShape.SHOULDER ? bank : null;
            hits[i][j] = hit;
            boolean gorge = bank != null && KunlunShape.gorge(seed, x, z, lx, lz) > 3;
            int t = (int) Math.round(h);
            if (bank != null && hit == null) {
                // the bank cut back from the stair's edge: gently on the drop side, steeply uphill; in a gorge, down under the bridge
                double cap = bank.y() + 1 + (bank.d() - KunlunShape.SHOULDER) * (bank.outer() ? KunlunShape.BANK_OUTER : KunlunShape.BANK_INNER);
                if (gorge) cap = Math.min(cap, bank.y() - KunlunShape.CULVERT);
                if (h > cap) {
                    t = (int) Math.round(cap);
                    mountain[i][j] = true;
                }
            }
            int ground = t; // the ground's own top, under any bridge
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
                if (role[i][j] != 0 && (h < hit.y() - KunlunShape.BRIDGE || gorge)) {
                    bridge[i][j] = true;
                    ground = gorge ? Math.min((int) Math.round(h), py - KunlunShape.CULVERT) : (int) Math.round(h);
                    rail[i][j] = hit.d() > KunlunShape.PATH_HALF;
                } else {
                    ground = t;
                }
            }
            KunlunShape.Site site = summitTop ? null : path.siteAt(lx, lz);
            if (site != null) {
                sites[i][j] = site;
                int u = site.along(lx, lz), v = site.across(lx, lz);
                if (KunlunWay.floor(site, u, v) && role[i][j] != WAY) {
                    t = ground = site.kind() == KunlunShape.SiteKind.SHRINE || hit == null ? site.y() : (int) Math.floor(hit.y());
                    role[i][j] = EDGE;
                    rail[i][j] = bridge[i][j] = false;
                    siteFloor[i][j] = true;
                    mountain[i][j] = true;
                }
            }
            if ((mountain[i][j] || hit != null) && sur > g && !bridge[i][j]) t = ground = Math.max(t, sur); // over water it rises out of it
            target[i][j] = Mth.clamp(t, minY, maxY);
            below[i][j] = Mth.clamp(ground, minY, maxY);
        }
        boolean any = false;
        List<BlockPos> trees = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[][] springs = near ? KunlunShape.springs(seed) : new int[0][];
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                any = true;
                int i = dx + 1, j = dz + 1;
                int g = ownGround[dx][dz], sur = ownSurface[dx][dz], t = target[i][j], base = below[i][j];
                int lx = x - c[0], lz = z - c[1];
                int step = Math.max(Math.max(Math.abs(below[i - 1][j] - base), Math.abs(below[i + 1][j] - base)),
                        Math.max(Math.abs(below[i][j - 1] - base), Math.abs(below[i][j + 1] - base)));
                boolean inner = dx >= 3 && dx <= 12 && dz >= 3 && dz <= 12;
                double snow = near ? KunlunShape.snowLine(seed, x, z, summit, foot) : COUNTRY_SNOW;
                KunlunShape.Site site = sites[i][j];
                if (!mountain[i][j] && role[i][j] == 0 && site == null) {
                    if (sur <= g) country(context, x, g, z, step, snow, inner && (path == null || path.nearest(lx, lz, KunlunShape.SHOULDER + 4) == null), trees, p);
                    if (near) clouds(level, seed, x, z, lx, lz, t, summit, foot, path, p);
                    continue;
                }
                boolean summitTop = KunlunPalace.covers(lx, lz);
                int land = step >= STEEP ? FACE : GROUND;
                if (!summitTop && base >= snow - GLACIER_UNDER_SNOW && role[i][j] == 0 && !siteFloor[i][j]
                        && KunlunShape.hollow(seed, x, z, lx, lz, g, summit) >= GLACIER_HOLLOW) land = ICE;
                int kind = bridge[i][j] ? land : role[i][j] == WAY ? WAY : role[i][j] == EDGE ? EDGE : summitTop ? TOP : land;
                // cut down to it (water over it is built over: see the targets)
                for (int y = Math.max(base + 1, minY); y <= Math.max(sur, g); y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                // built up to it, or its top few blocks made over
                BlockState topState = top(kind, x, base, z, snow);
                int skin = kind == FACE && topState.is(Blocks.SNOW_BLOCK) ? Math.min(step, 12) + 1 : kind == ICE ? Math.min(step, 16) + 2 : 3; // a snowy cliff white, an ice fall blue, all down its face
                for (int y = Math.max(minY, Math.min(g + 1, base - skin)); y <= base; y++) {
                    if (y < base && y <= g && !level.getBlockState(p.set(x, y, z)).blocksMotion()) continue; // a cave just under it left open
                    BlockState st = y == base ? topState : y > base - skin ? under(kind, x, y, z, topState) : rock(y, x, z);
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                KunlunShape.Hit hit = hits[i][j];
                if (bridge[i][j]) {
                    // the bridge's deck and arches over the ground: a pier every span, down to the ground
                    int phase = Math.floorMod(hit.index(), KunlunShape.SPAN);
                    int thick = phase == 0 ? t - base : Math.min(t - base, 1 + (int) Math.round(3 * (1 - Math.sin(Math.PI * phase / KunlunShape.SPAN))));
                    for (int y = Math.max(minY, t - thick + 1); y <= t; y++) {
                        level.setBlock(p.set(x, y, z), y == t ? top(role[i][j] == WAY ? WAY : EDGE, x, y, z, snow) : s(Blocks.STONE_BRICKS), Block.UPDATE_CLIENTS);
                    }
                    for (int y = base + 1; y <= t - thick; y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                }
                if (role[i][j] == WAY && hit != null && hit.y() - Math.floor(hit.y()) >= 0.5) {
                    level.setBlock(p.set(x, t + 1, z), Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), Block.UPDATE_CLIENTS);
                } else if (rail[i][j] && site == null) {
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
                // a gate, pavilion or shrine on the way
                if (site != null) {
                    lay(level, site, c, x, z, lx, lz, t, role[i][j] == WAY, minY, maxY, p);
                    continue;
                }
                // a spring high in a gorge, its stream to fall down it
                for (int[] sp : springs) {
                    if (sp[0] == lx && sp[1] == lz && kind != WAY && kind != EDGE && path.nearest(lx, lz, 16) == null) {
                        level.setBlock(p.set(x, base, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                        level.scheduleTick(p.immutable(), Fluids.WATER, 0);
                    }
                }
                if (kind == GROUND && level.getFluidState(p.set(x, base + 1, z)).isEmpty() && base < snow - 8) {
                    boolean clear = path.nearest(lx, lz, KunlunShape.SHOULDER + 4) == null && path.siteAt(lx, lz) == null;
                    boolean wood = base < snow - KunlunShape.TREE_LINE;
                    if (inner && clear && wood && fine(x, z, 82) < 16 && level.getBlockState(p.set(x, base, z)).is(Blocks.GRASS_BLOCK)) {
                        trees.add(new BlockPos(x, base + 1, z)); // a pine on the flank
                    } else if (fine(x, z, 81) < (wood ? 260 : 90) && level.getBlockState(p.set(x, base, z)).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p.set(x, base + 1, z), s(fine(x, z, 83) < 300 ? Blocks.FERN : Blocks.SHORT_GRASS), Block.UPDATE_CLIENTS);
                    }
                }
                clouds(level, seed, x, z, lx, lz, Math.max(t, base), summit, foot, path, p);
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

    private static double smoothstep(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** Lays a site's share in column (x, z) (local lx, lz), its ground at {@code ground} ({@code way}: the way itself, kept clear to walk): a pillar's footing up from the ground, its blueprint, its boards. */
    private static void lay(WorldGenLevel level, KunlunShape.Site site, int[] c, int x, int z, int lx, int lz, int ground, boolean way, int minY, int maxY,
                            BlockPos.MutableBlockPos p) {
        int u = site.along(lx, lz), v = site.across(lx, lz);
        if (KunlunWay.footing(site, u, v) && !way) {
            for (int y = Math.max(minY, ground + 1); y <= site.y(); y++) level.setBlock(p.set(x, y, z), s(Blocks.STONE_BRICKS), Block.UPDATE_CLIENTS);
        }
        for (int ly = -1; ly <= 12; ly++) {
            int y = site.y() + ly;
            if (y < minY || y > maxY) continue;
            BlockState st = KunlunWay.at(site, u, v, ly, ground);
            if (st != null && way && !st.isAir() && y <= ground + 4) continue; // nothing set on the way itself, where one walks (the way bends a little under a site)
            if (st != null) level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
        }
        for (KunlunWay.Board board : KunlunWay.boards(site)) {
            BlockPos at = KunlunWay.world(site, c[0], c[1], board.u(), board.v(), board.ly());
            if (at.getX() == x && at.getZ() == z) KunlunWay.putBoard(level, at, board);
        }
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

    /** The cloud belts round the massif, in the open over column (x, z), its ground at {@code t}; never close over the stair, nor on the summit. */
    private static void clouds(WorldGenLevel level, long seed, int x, int z, int lx, int lz, int t, int summit, int foot,
                               KunlunShape.Path path, BlockPos.MutableBlockPos p) {
        if (KunlunPalace.covers(lx, lz)) return;
        KunlunShape.Hit near = null;
        boolean asked = false;
        for (int belt = 0; belt < KunlunShape.BELTS; belt++) {
            int mid = KunlunShape.beltY(belt, summit, foot);
            if (t >= mid + 3 || t < mid - KunlunShape.BELT_DROP[belt] - 5) continue;
            if (!asked) {
                near = path.nearest(lx, lz, KunlunShape.SHOULDER + 3);
                asked = true;
            }
            for (int y = Math.max(mid - 8, t + 2); y <= mid + KunlunShape.CLOUD_PUFF + 5; y++) {
                if (near != null && y < near.y() + 12) continue;
                if (KunlunShape.cloud(seed, x, y, z, belt, t, summit, foot) <= 0) continue;
                if (level.getBlockState(p.set(x, y, z)).isAir()) level.setBlock(p, ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Kunlun's country round its massif, the land's shape kept: steep places bare rock, high places
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
