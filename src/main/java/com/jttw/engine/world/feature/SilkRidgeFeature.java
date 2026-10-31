package com.jttw.engine.world.feature;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.spider.HundredEyedDemonEntity;
import com.jttw.engine.entity.spider.SilkMonster;
import com.jttw.engine.entity.spider.SpiderSpiritEntity;
import com.jttw.engine.entity.spider.StingerSonEntity;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.spider.BindingWebBlock;
import com.jttw.engine.world.spider.SilkRidgeShape;
import com.jttw.engine.world.spider.SilkRidgeWorks;
import com.jttw.engine.world.spider.SpiderCave;
import com.jttw.engine.world.spider.SpiderCavePlan;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Makes Silk-Web Ridge's country of the land it takes over ({@link SilkRidgeShape}): brings the
 * valley to its floor and raises the ridge across it, lays the Spider Cave inside the ridge
 * ({@link SpiderCavePlan}) and the works on the valley floor ({@link SilkRidgeWorks}: the cottage,
 * the spring, the temple under its canopy), column by column, so each comes out whole across the
 * chunks it spans; and puts their keepers at their posts, each by the chunk its post lies in: the
 * cave's garrison, a sister in each bower, the yard's, the spring's and the temple gate's bands, and
 * the Hundred-Eyed Demon Lord in his temple's court, once, when the land is made.
 * <ul>
 *   <li>the valley's ground is dark: podzol and moss in the grass, coarse earth, here and there an
 *       old bone; the ridge's faces bare stone, tuff and andesite streaked with calcite;</li>
 *   <li>the paths are worn earth and gravel.</li>
 * </ul>
 * Runs first among the zone's features; the forest (the biome's own trees) and the webs strung in
 * it ({@link SilkWebsFeature}) come after.
 */
public class SilkRidgeFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = Zones.SPIDER_CAVE;
    public static final ResourceKey<LootTable> TREASURY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/spider_cave_treasury"));
    public static final ResourceKey<LootTable> COTTAGE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/silk_cottage"));
    /** A step this high or more to a neighbour makes a face, not ground. */
    private static final int STEEP = 3;
    private static final int FACE = 0, GROUND = 1, PATH = 2;

    public SilkRidgeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int fine(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 41 + 9, z), 1000L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** The ridge's rock: stone, tuff and andesite in tilted bands, streaked with calcite. */
    public static BlockState rock(int x, int y, int z) {
        int band = Math.floorMod(y + ((x * 13 + z * 7) >> 4), 11);
        if (band == 0) return s(Blocks.CALCITE);
        if (band == 3 || band == 4) return s(Blocks.TUFF);
        if (band == 7) return s(Blocks.ANDESITE);
        return s(Blocks.STONE);
    }

    private static BlockState top(int kind, int x, int y, int z, double n, boolean high) {
        return switch (kind) {
            case FACE -> fine(x, z, 3) < 260 ? s(Blocks.MOSSY_COBBLESTONE) : rock(x, y, z);
            case PATH -> {
                int q = fine(x, z, 7);
                yield q < 550 ? s(Blocks.DIRT_PATH) : q < 800 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL);
            }
            default -> high && fine(x, z, 4) < 200 ? s(Blocks.MOSS_BLOCK)
                    : n > 0.45 ? s(Blocks.PODZOL) : n < -0.55 ? s(Blocks.MOSS_BLOCK) : n < -0.4 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK);
        };
    }

    /** What the cave's code {@code code} is, as a block, at (x, y, z) of bower {@code bower} (-1 elsewhere). */
    public static BlockState caveBlock(int code, int x, int y, int z, int bower) {
        return switch (code) {
            case SpiderCavePlan.AIR -> s(Blocks.AIR);
            case SpiderCavePlan.FLOOR -> {
                int q = fine(x, z, 21);
                yield q < 350 ? s(Blocks.PACKED_MUD) : q < 600 ? s(Blocks.TUFF) : q < 800 ? s(Blocks.ROOTED_DIRT) : q < 950 ? s(Blocks.MUD) : s(Blocks.BONE_BLOCK);
            }
            case SpiderCavePlan.SEAL -> SpiderCaveRegistry.SILK_SEAL.get().defaultBlockState();
            case SpiderCavePlan.COBWEB -> s(Blocks.COBWEB);
            case SpiderCavePlan.SILK -> SpiderCaveRegistry.WOVEN_SILK.get().defaultBlockState();
            case SpiderCavePlan.GLOW -> s(Blocks.VERDANT_FROGLIGHT);
            case SpiderCavePlan.CARPET -> s(CARPETS[Math.max(0, bower) % CARPETS.length]);
            case SpiderCavePlan.LOOM -> s(Blocks.LOOM);
            case SpiderCavePlan.CHEST -> s(Blocks.CHEST);
            case SpiderCavePlan.BINDING_WEB -> SpiderCaveRegistry.BINDING_WEB.get().defaultBlockState();
            case SpiderCavePlan.CHAIN -> s(Blocks.CHAIN).setValue(ChainBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
            case SpiderCavePlan.LANTERN -> s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true);
            case SpiderCavePlan.BONES -> fine(x, z, 22) < 500 ? s(Blocks.BONE_BLOCK) : s(Blocks.SKELETON_SKULL);
            case SpiderCavePlan.PLANKS -> s(Blocks.DARK_OAK_PLANKS);
            case SpiderCavePlan.MUD -> fine(x, z, 23) < 750 ? s(Blocks.MUD) : s(Blocks.PACKED_MUD);
            case SpiderCavePlan.MOSS -> fine(x, z, 24) < 850 ? s(Blocks.MOSS_BLOCK) : s(Blocks.ROOTED_DIRT);
            case SpiderCavePlan.MOSS_CARPET -> s(Blocks.MOSS_CARPET);
            case SpiderCavePlan.COARSE -> fine(x, z, 25) < 600 ? s(Blocks.COARSE_DIRT) : fine(x, z, 25) < 850 ? s(Blocks.ROOTED_DIRT) : s(Blocks.GRAVEL);
            case SpiderCavePlan.WATER -> s(Blocks.WATER);
            case SpiderCavePlan.BONE_FLOOR -> fine(x, z, 26) < 700 ? s(Blocks.BONE_BLOCK) : s(Blocks.PACKED_MUD);
            case SpiderCavePlan.SKULL -> s(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, fine(x, z, 27) % 16);
            case SpiderCavePlan.BONE_LYING -> s(Blocks.BONE_BLOCK).setValue(RotatedPillarBlock.AXIS,
                    fine(x, z, 28) < 500 ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z);
            case SpiderCavePlan.SILK_FLOOR -> fine(x, z, 29) < 800 ? SpiderCaveRegistry.WOVEN_SILK.get().defaultBlockState() : s(Blocks.WHITE_WOOL);
            case SpiderCavePlan.STRAND -> s(Blocks.WHITE_STAINED_GLASS_PANE);
            case SpiderCavePlan.MEMBRANE -> s(Blocks.WHITE_STAINED_GLASS);
            case SpiderCavePlan.EGG -> s(Blocks.PEARLESCENT_FROGLIGHT);
            case SpiderCavePlan.EGG_GOLD -> s(Blocks.OCHRE_FROGLIGHT);
            case SpiderCavePlan.WOOL -> s(Blocks.WHITE_WOOL);
            case SpiderCavePlan.LICHEN_UP -> s(Blocks.GLOW_LICHEN).setValue(MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.UP), true);
            case SpiderCavePlan.LICHEN_DOWN -> s(Blocks.GLOW_LICHEN).setValue(MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.DOWN), true);
            case SpiderCavePlan.CRACK -> s(Blocks.PEARLESCENT_FROGLIGHT);
            case SpiderCavePlan.BEAM -> s(Blocks.END_ROD);
            case SpiderCavePlan.LIGHT -> s(Blocks.LIGHT);
            case SpiderCavePlan.MUSHROOM -> fine(x, z, 30) < 700 ? s(Blocks.BROWN_MUSHROOM) : s(Blocks.RED_MUSHROOM);
            case SpiderCavePlan.ROOTS -> s(Blocks.HANGING_ROOTS);
            case SpiderCavePlan.GRASS -> fine(x, z, 31) < 600 ? s(Blocks.SHORT_GRASS) : s(Blocks.FERN);
            default -> null;
        };
    }

    /** Each bower's carpet, in the order the bowers are reached: the seventh sister's (lilac) first, the eldest's (crimson) last. */
    static final Block[] CARPETS = {Blocks.PURPLE_CARPET, Blocks.LIGHT_BLUE_CARPET, Blocks.LIME_CARPET, Blocks.YELLOW_CARPET,
            Blocks.ORANGE_CARPET, Blocks.PINK_CARPET, Blocks.RED_CARPET};

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
        int floor = SpiderCave.floor(level, c[0], c[1]);
        SpiderCave.Region region = new SpiderCave.Region(c[0], c[1], floor);
        int minY = level.getMinBuildHeight() + 1, maxY = level.getMaxBuildHeight() - 2;
        // the height each column (and the ring round the chunk) is brought to
        int[][] target = new int[18][18];
        int[][] ground = new int[18][18];
        int[][] surface = new int[18][18];
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) {
            int x = x0 + i - 1, z = z0 + j - 1, lx = x - c[0], lz = z - c[1];
            int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            ground[i][j] = g;
            surface[i][j] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            int t = SilkRidgeShape.groundAt(seed, x, z, lx, lz, g, floor);
            int face = SilkRidgeWorks.faceAt(lx, lz);
            if (face != Integer.MIN_VALUE) t = Math.max(t, floor - 1 + face);
            target[i][j] = Mth.clamp(t, minY, maxY);
        }
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                int lx = x - c[0], lz = z - c[1];
                int g = ground[dx + 1][dz + 1], sur = surface[dx + 1][dz + 1], t = target[dx + 1][dz + 1];
                any = true;
                double valley = SilkRidgeShape.valley(lx, lz);
                if (valley <= 0) continue; // the land's own, out past the valley
                int step = Math.max(Math.max(Math.abs(target[dx][dz + 1] - t), Math.abs(target[dx + 2][dz + 1] - t)),
                        Math.max(Math.abs(target[dx + 1][dz] - t), Math.abs(target[dx + 1][dz + 2] - t)));
                boolean path = SilkRidgeShape.onPath(lx, lz) && t <= floor + 3;
                int kind = path ? PATH : step >= STEEP ? FACE : GROUND;
                double n = SilkRidgeShape.patch(seed, x, z);
                // cut down to it, water and all; built up to it, or its top few blocks made over
                for (int y = Math.max(t + 1, minY); y <= Math.max(sur, g); y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                for (int y = Math.max(minY, Math.min(g + 1, t - 3)); y <= t; y++) {
                    BlockState st = y == t ? top(kind, x, y, z, n, t > floor + 20) : y > t - 3 && kind != FACE ? s(Blocks.DIRT) : rock(x, y, z);
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                // the cave, inside the ridge
                if (SpiderCavePlan.covers(lx, lz)) {
                    int bower = SpiderCavePlan.bowerAt(lx, lz);
                    int roof = SpiderCavePlan.roof(lx, lz);
                    for (int ly = -1; ly <= roof; ly++) {
                        int code = SpiderCavePlan.at(lx, ly, lz);
                        if (code == SpiderCavePlan.NONE) continue;
                        BlockState st = caveBlock(code, x, floor + ly, z, bower);
                        if (st == null) continue;
                        level.setBlock(p.set(x, floor + ly, z), st, Block.UPDATE_CLIENTS);
                        if (code == SpiderCavePlan.CHEST) RandomizableContainer.setBlockEntityLootTable(level, context.random(), p.immutable(), TREASURY_LOOT);
                    }
                    // the rock just round the cave's walls: dark, mossy, cobwebbed
                    BlockPos under = p.set(x, floor - 2, z);
                    if (level.getBlockState(under).isAir()) level.setBlock(under, rock(x, floor - 2, z), Block.UPDATE_CLIENTS);
                }
                // the works on the valley floor
                if (SilkRidgeWorks.covers(lx, lz)) {
                    for (int ly = SilkRidgeWorks.BOTTOM; ly <= SilkRidgeWorks.TOP; ly++) {
                        BlockState st = SilkRidgeWorks.at(lx, ly, lz, true);
                        if (st == null) continue;
                        level.setBlock(p.set(x, floor + ly, z), st, Block.UPDATE_CLIENTS);
                        if (st.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, context.random(), p.immutable(), COTTAGE_LOOT);
                    }
                } else if (kind == GROUND && fine(x, z, 31) < 6 && valley >= 1 && !SpiderCavePlan.covers(lx, lz)) {
                    // an old bone in the grass, or a web strung low over it
                    level.setBlock(p.set(x, t + 1, z), fine(x, z, 32) < 500 ? SpiderCaveRegistry.BINDING_WEB.get().defaultBlockState()
                            : s(Blocks.SKELETON_SKULL), Block.UPDATE_CLIENTS);
                }
            }
        }
        garrison(level, region, x0, z0, context.random());
        return any;
    }

    // ── Who keeps it ────────────────────────────────────────────────────

    /** The bands outside the cave: the yard's, the spring's, the temple gate's (kind, x, z, captain). */
    static final int[][] OUTSIDE = {
            {SpiderCavePlan.SPIDER, -6, 64, 1}, {SpiderCavePlan.SPIDER, -9, 66, 0}, {SpiderCavePlan.SPIDER, 7, 66, 0}, {SpiderCavePlan.MANTIS, 4, 63, 0},
            {SpiderCavePlan.STINGER, SilkRidgeWorks.SPRING_X - 6, SilkRidgeWorks.SPRING_Z + 12, 1}, {SpiderCavePlan.STINGER, SilkRidgeWorks.SPRING_X + 6, SilkRidgeWorks.SPRING_Z + 12, 0},
            {SpiderCavePlan.SPIDER, SilkRidgeWorks.SPRING_X + 13, SilkRidgeWorks.SPRING_Z, 0},
            {SpiderCavePlan.COCKROACH, SilkRidgeWorks.TEMPLE_X - 5, SilkRidgeWorks.TEMPLE_Z + 42, 1},
            {SpiderCavePlan.COCKROACH, SilkRidgeWorks.TEMPLE_X + 5, SilkRidgeWorks.TEMPLE_Z + 42, 0},
            {SpiderCavePlan.MANTIS, SilkRidgeWorks.TEMPLE_X, SilkRidgeWorks.TEMPLE_Z + 46, 0}};

    static EntityType<? extends SilkMonster> kind(int kind) {
        return switch (kind) {
            case SpiderCavePlan.STINGER -> SpiderCaveRegistry.STINGER_SON.get();
            case SpiderCavePlan.MANTIS -> SpiderCaveRegistry.MANTIS_SON.get();
            case SpiderCavePlan.COCKROACH -> SpiderCaveRegistry.COCKROACH_SON.get();
            case SpiderCavePlan.SISTER -> SpiderCaveRegistry.SPIDER_SPIRIT.get();
            default -> SpiderCaveRegistry.SPIDER_DEMON.get();
        };
    }

    private static boolean inChunk(int x, int z, int x0, int z0) {
        return x >= x0 && x < x0 + 16 && z >= z0 && z < z0 + 16;
    }

    /** Puts each keeper whose post lies in the chunk at (x0, z0) at it. */
    public static void garrison(WorldGenLevel level, SpiderCave.Region region, int x0, int z0, RandomSource random) {
        for (SpiderCavePlan.Post post : SpiderCavePlan.POSTS) {
            BlockPos at = region.at(post.x(), 0, post.z());
            if (inChunk(at.getX(), at.getZ(), x0, z0)) keeper(level, region, post, at, random);
        }
        for (int[] o : OUTSIDE) {
            BlockPos at = region.at(o[1], 0, o[2]);
            if (!inChunk(at.getX(), at.getZ(), x0, z0)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
            boolean captain = o[3] == 1;
            keeper(level, region, new SpiderCavePlan.Post(o[0], o[1], o[2], captain, captain ? 85 : 78, -1), at.atY(y), random);
        }
        BlockPos court = region.at(SilkRidgeWorks.TEMPLE_X + SilkRidgeWorks.COURT_X, 0, SilkRidgeWorks.TEMPLE_Z + SilkRidgeWorks.COURT_Z);
        if (inChunk(court.getX(), court.getZ(), x0, z0)) {
            HundredEyedDemonEntity lord = SpiderCaveRegistry.HUNDRED_EYED_DEMON.get().create(level.getLevel());
            if (lord != null) {
                lord.moveTo(court.getX() + 0.5, court.getY(), court.getZ() + 0.5, 180f, 0f);
                lord.finalizeSpawn(level, level.getCurrentDifficultyAt(court), MobSpawnType.STRUCTURE, null);
                lord.keepTemple(region);
                level.addFreshEntityWithPassengers(lord);
            }
        }
    }

    private static void keeper(WorldGenLevel level, SpiderCave.Region region, SpiderCavePlan.Post post, BlockPos at, RandomSource random) {
        SilkMonster m = kind(post.kind()).create(level.getLevel());
        if (m == null) return;
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        if (m instanceof SpiderSpiritEntity sister) {
            sister.setSister(SpiderCavePlan.sisterOf(post.bower()));
            sister.becomeMiniboss(post.level(), SpiderSpiritEntity.BOWER_HEALTH);
            sister.keepBower(new SpiderCave.Bower(region, post.bower()));
            sister.keepTo(at, SpiderSpiritEntity.BOWER);
        } else {
            m.become(post.level(), post.captain());
            if (m instanceof StingerSonEntity s) s.setKind(Math.floorMod(post.x() * 7 + post.z(), StingerSonEntity.KINDS.length));
            m.keepTo(at, post.bower() >= 0 ? 12 : 18);
        }
        level.addFreshEntityWithPassengers(m);
    }

    /** Whether a binding web is a fresh one (for the webs feature's sake: those it strings are not). */
    static BlockState web() {
        return SpiderCaveRegistry.BINDING_WEB.get().defaultBlockState().setValue(BindingWebBlock.FRESH, false);
    }
}
