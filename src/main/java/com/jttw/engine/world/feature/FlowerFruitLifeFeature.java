package com.jttw.engine.world.feature;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Flower-Fruit Mountain's life, once {@link FlowerFruitIslandFeature} has raised it:
 * <ul>
 *   <li><b>the peach orchards</b> (花果, the flowers and fruit it is named for): peach trees in pink
 *       blossom on the terraces, fallen petals under them;</li>
 *   <li>broad-leafed trees and flowering azalea on the lower slopes, <b>pines</b> on the heights and on
 *       the heads of the rock pillars, bamboo about the pool and the stream, ferns, grass and flowers;</li>
 *   <li><b>the monkeys' camps</b> ({@link IslandShape#camps}): a ring of stones, a fire, stools, a
 *       lean-to and a pole flying the Great Sage's yellow banner; and each one's band, put there once
 *       and kept (about one in three roams);</li>
 *   <li><b>the cave's garrison</b> ({@link CavePlan#POSTS}): the monkeys at their posts in the Water
 *       Curtain Cave, the three fake pilgrims (minibosses) before the old throne, and the stolen
 *       luggage beside them ({@link #LUGGAGE}); the signs, the tablet over the stone gate among them.</li>
 * </ul>
 */
public class FlowerFruitLifeFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = IslandShape.ZONE;
    /** The stolen luggage's loot. */
    public static final ResourceKey<LootTable> LUGGAGE = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/stolen_luggage"));

    public FlowerFruitLifeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 31 + 5, z), 1000L);
    }

    private static boolean soil(BlockState b) {
        return b.is(Blocks.GRASS_BLOCK) || b.is(Blocks.MOSS_BLOCK) || b.is(Blocks.PODZOL) || b.is(Blocks.COARSE_DIRT) || b.is(Blocks.DIRT);
    }

    private static void put(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, BlockState st, boolean replace) {
        p.set(x, y, z);
        if (!level.ensureCanWrite(p)) return;
        BlockState here = level.getBlockState(p);
        if (!replace && !here.isAir() && !here.canBeReplaced()) return;
        level.setBlock(p, st, Block.UPDATE_CLIENTS);
    }

    private static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A peach tree in blossom: a short leaning trunk, a broad crown of pink, petals fallen round it. */
    static void peach(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z) {
        int high = 3 + hash(x, z, 31) % 3;
        int lean = hash(x, z, 32) % 4;
        Direction d = Direction.from2DDataValue(lean);
        BlockState log = s(Blocks.CHERRY_LOG);
        int tx = x, tz = z;
        for (int up = 1; up <= high; up++) {
            if (up == high - 1 && hash(x, z, 33) < 600) {
                tx += d.getStepX();
                tz += d.getStepZ();
            }
            put(level, p, tx, y + up, tz, log, true);
        }
        int cy = y + high + 1;
        double r = 2.6 + (hash(x, z, 34) % 10) / 10.0;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -1; dy <= 2; dy++) {
            double e = (dx * dx + dz * dz) / (r * r) + (dy - 0.3) * (dy - 0.3) / 2.2;
            if (e > 1 || (e > 0.75 && hash(tx + dx, tz + dz + dy * 7, 35) < 400)) continue;
            put(level, p, tx + dx, cy + dy, tz + dz, leaves(Blocks.CHERRY_LEAVES), false);
        }
        // the fallen petals
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (hash(x + dx, z + dz, 36) > 330) continue;
            int gy = level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz);
            p.set(x + dx, gy - 1, z + dz);
            if (!level.ensureCanWrite(p) || !soil(level.getBlockState(p))) continue;
            put(level, p, x + dx, gy, z + dz, s(Blocks.PINK_PETALS).setValue(PinkPetalsBlock.AMOUNT, 1 + hash(x + dx, z + dz, 37) % 4)
                    .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue(hash(x + dx, z + dz, 38) % 4)), false);
        }
    }

    /** A pine: a straight trunk, tiers of dark needles narrowing to its head. */
    static void pine(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z) {
        int high = 7 + hash(x, z, 41) % 6;
        for (int up = 1; up <= high; up++) put(level, p, x, y + up, z, s(Blocks.SPRUCE_LOG), true);
        for (int up = 3; up <= high + 1; up++) {
            int tier = (high + 1 - up);
            int r = up == high + 1 ? 0 : 1 + (tier % 3 == 0 ? Math.min(3, tier / 3 + 1) : Math.min(2, tier / 4 + 1));
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > r + (r > 1 ? 1 : 0)) continue;
                if (dx == 0 && dz == 0 && up <= high) continue;
                put(level, p, x + dx, y + up, z + dz, leaves(Blocks.SPRUCE_LEAVES), false);
            }
        }
    }

    /** A broad-leafed tree: a trunk, boughs, a wide crown, flowering azalea in it now and then. */
    static void broadleaf(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z) {
        int high = 5 + hash(x, z, 51) % 3;
        BlockState log = hash(x, z, 52) < 500 ? s(Blocks.JUNGLE_LOG) : s(Blocks.OAK_LOG);
        Block leaf = hash(x, z, 52) < 500 ? Blocks.JUNGLE_LEAVES : Blocks.OAK_LEAVES;
        for (int up = 1; up <= high; up++) put(level, p, x, y + up, z, log, true);
        for (int b = 0; b < 2; b++) {
            Direction d = Direction.from2DDataValue((hash(x, z, 53) + b * 2) % 4);
            put(level, p, x + d.getStepX(), y + high - 1, z + d.getStepZ(), log.setValue(RotatedPillarBlock.AXIS, d.getAxis()), true);
        }
        double r = 3.2;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = -1; dy <= 2; dy++) {
            double e = (dx * dx + dz * dz) / (r * r) + dy * dy / 2.5;
            if (e > 1 || (e > 0.8 && hash(x + dx, z + dz + dy, 54) < 450)) continue;
            Block lf = hash(x + dx * 3, z + dz + dy * 5, 55) < 120 ? Blocks.FLOWERING_AZALEA_LEAVES : leaf;
            put(level, p, x + dx, y + high + dy, z + dz, leaves(lf), false);
        }
    }

    /** A clump of bamboo. */
    static void bamboo(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z) {
        for (int k = 0; k < 5; k++) {
            int bx = x + (hash(x, z, 60 + k) % 5) - 2, bz = z + (hash(x, z, 70 + k) % 5) - 2;
            int gy = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
            p.set(bx, gy - 1, bz);
            if (!level.ensureCanWrite(p) || !soil(level.getBlockState(p))) continue;
            int high = 6 + hash(bx, bz, 61) % 7;
            for (int up = 0; up < high; up++) {
                BlockState st = s(Blocks.BAMBOO).setValue(net.minecraft.world.level.block.BambooStalkBlock.LEAVES,
                        up >= high - 2 ? net.minecraft.world.level.block.state.properties.BambooLeaves.LARGE
                                : up >= high - 4 ? net.minecraft.world.level.block.state.properties.BambooLeaves.SMALL
                                : net.minecraft.world.level.block.state.properties.BambooLeaves.NONE)
                        .setValue(net.minecraft.world.level.block.BambooStalkBlock.AGE, 1);
                put(level, p, bx, gy + up, bz, st, false);
            }
        }
    }

    private static void tall(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, Block b) {
        p.set(x, y + 2, z);
        if (!level.isEmptyBlock(p)) return;
        put(level, p, x, y + 1, z, b.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
        put(level, p, x, y + 2, z, b.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
    }

    // ── The camps ───────────────────────────────────────────────────────

    /** What a camp holds at (dx, dy, dz) from its middle (dy 0 its ground); null where it leaves the ground. */
    public static BlockState campAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r > IslandShape.CAMP_REACH + 0.5) return null;
        if (dy == 0) return r < 1.6 ? s(Blocks.COBBLESTONE) : r < 4.6 ? (((dx * 7 + dz * 3) & 3) == 0 ? s(Blocks.GRAVEL) : s(Blocks.COARSE_DIRT)) : null;
        if (dy == 1 && dx == 0 && dz == 0) return s(Blocks.CAMPFIRE);
        // stools round the fire
        if (dy == 1 && ((ax == 3 && dz == 0) || (az == 3 && dx == 0))) return s(Blocks.STONE_SLAB);
        // a ring of mossy stones, broken
        if (dy == 1 && r > 5.2 && r <= 6.2 && ((dx + dz * 3) & 3) != 0) return ((dx ^ dz) & 1) == 0 ? s(Blocks.MOSSY_COBBLESTONE_WALL) : s(Blocks.MOSSY_COBBLESTONE);
        // the banner pole and the Great Sage's yellow banner on it
        if (dx == 4 && dz == 4 && dy >= 1 && dy <= 7) return s(Blocks.SPRUCE_FENCE);
        if (dx == 4 && dz == 4 && dy == 8) return s(Blocks.YELLOW_BANNER);
        // a lean-to of spruce, a barrel of peaches under it
        if (dx >= -4 && dx <= -2 && dz >= -4 && dz <= -2) {
            if (dy <= 2 && (dx == -4 || dx == -2) && (dz == -4 || dz == -2)) return s(Blocks.SPRUCE_LOG);
            if (dy == 3) return s(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            if (dy == 1 && dx == -3 && dz == -3) return s(Blocks.BARREL);
            if (dy == 1 && dx == -3 && dz == -2) return s(Blocks.HAY_BLOCK);
            return dy <= 2 ? s(Blocks.AIR) : null;
        }
        return dy <= 3 && r <= 5.2 ? s(Blocks.AIR) : null;
    }

    private static boolean camps(WorldGenLevel level, long seed, int[] c, int x0, int z0, RandomSource random) {
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = IslandShape.CAMP_REACH;
        for (IslandShape.Camp camp : IslandShape.camps(seed)) {
            int mx = c[0] + camp.x(), mz = c[1] + camp.z();
            if (mx + r < x0 || mx - r > x0 + 15 || mz + r < z0 || mz - r > z0 + 15) continue;
            int base = IslandShape.campBase(seed, camp);
            for (int x = Math.max(x0, mx - r); x <= Math.min(x0 + 15, mx + r); x++) {
                for (int z = Math.max(z0, mz - r); z <= Math.min(z0 + 15, mz + r); z++) {
                    int dx = x - mx, dz = z - mz;
                    if (dx * dx + dz * dz > (r + 0.5) * (r + 0.5)) continue;
                    int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                    // the ground levelled to the camp's
                    for (int y = Math.min(ground, base - 1); y < base; y++) level.setBlock(p.set(x, y, z), s(Blocks.DIRT), Block.UPDATE_CLIENTS);
                    if (ground < base) level.setBlock(p.set(x, base, z), s(Blocks.GRASS_BLOCK), Block.UPDATE_CLIENTS);
                    for (int y = base + 1; y <= ground; y++) level.setBlock(p.set(x, y, z), s(Blocks.AIR), Block.UPDATE_CLIENTS);
                    for (int dy = 0; dy <= 8; dy++) {
                        BlockState st = campAt(dx, dy, dz);
                        if (st != null) level.setBlock(p.set(x, base + dy, z), st, Block.UPDATE_CLIENTS);
                    }
                }
            }
            any = true;
            if ((mx >> 4) == (x0 >> 4) && (mz >> 4) == (z0 >> 4)) band(level, camp, mx, base + 1, mz, random);
        }
        return any;
    }

    /** A camp's band: a captain and three or four, of the three kinds; kept; a patrol roams. */
    private static void band(WorldGenLevel level, IslandShape.Camp camp, int x, int y, int z, RandomSource random) {
        long h = Mth.getSeed(x, 43, z);
        int n = 4 + (int) Math.floorMod(h >> 8, 2L);
        for (int i = 0; i < n; i++) {
            boolean captain = i == 0;
            int roll = (int) Math.floorMod(h >> (4 + i * 3), 7L);
            EntityType<? extends FlowerFruitMonster> type = roll < 3 ? FlowerFruitRegistry.STONE_GIBBON.get()
                    : roll < 6 ? FlowerFruitRegistry.SPEAR_APE.get() : FlowerFruitRegistry.GREAT_BABOON.get();
            double a = i * Math.PI * 2 / n + 0.4;
            put(level, type, x + (int) Math.round(Math.cos(a) * 3.5), y, z + (int) Math.round(Math.sin(a) * 3.5), captain,
                    captain ? 26 + random.nextInt(3) : 20 + random.nextInt(3), new BlockPos(x, y, z), camp.patrol() ? 40 : 12, random);
        }
    }

    private static <T extends FlowerFruitMonster> T put(WorldGenLevel level, EntityType<T> type, int x, int y, int z, boolean captain, int lv,
                                                        BlockPos post, int reach, RandomSource random) {
        T m = type.create(level.getLevel());
        if (m == null) return null;
        m.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.STRUCTURE, null);
        m.become(lv, captain);
        m.setPersistenceRequired();
        m.restrictTo(post, reach);
        level.addFreshEntityWithPassengers(m);
        return m;
    }

    /** The cave's garrison at its posts in this chunk, the fake pilgrims among them, and the luggage. */
    private static boolean garrison(WorldGenLevel level, int[] c, int x0, int z0, RandomSource random) {
        boolean any = false;
        int floor = IslandShape.BASIN_Y + 1;
        for (CavePlan.Post post : CavePlan.POSTS) {
            int x = c[0] + post.x(), z = c[1] + post.z();
            if ((x >> 4) != (x0 >> 4) || (z >> 4) != (z0 >> 4)) continue;
            BlockPos at = new BlockPos(x, floor + CavePlan.floor(post.x(), post.z()), z);
            switch (post.kind()) {
                case CavePlan.PILGRIM -> {
                    FakePilgrimEntity f = FlowerFruitRegistry.FAKE_PILGRIM.get().create(level.getLevel());
                    if (f == null) break;
                    f.moveTo(x + 0.5, at.getY(), z + 0.5, 0f, 0f);
                    f.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
                    f.setVariant(post.variant());
                    f.becomeMiniboss(post.level(), CavePlan.PILGRIM_HEALTH);
                    f.setPersistenceRequired();
                    f.restrictTo(at, FakePilgrimEntity.POST);
                    level.addFreshEntityWithPassengers(f);
                }
                case CavePlan.GIBBON -> put(level, FlowerFruitRegistry.STONE_GIBBON.get(), x, at.getY(), z, post.captain(), post.level(), at, 10, random);
                case CavePlan.APE -> put(level, FlowerFruitRegistry.SPEAR_APE.get(), x, at.getY(), z, post.captain(), post.level(), at, 10, random);
                default -> put(level, FlowerFruitRegistry.GREAT_BABOON.get(), x, at.getY(), z, post.captain(), post.level(), at, 10, random);
            }
            any = true;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int[] l : CavePlan.LUGGAGE) {
            int x = c[0] + l[0], z = c[1] + l[1];
            if ((x >> 4) != (x0 >> 4) || (z >> 4) != (z0 >> 4)) continue;
            p.set(x, floor, z);
            level.setBlock(p, s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
            RandomizableContainer.setBlockEntityLootTable(level, random, p.immutable(), LUGGAGE);
            any = true;
        }
        for (CavePlan.Sign sign : CavePlan.SIGNS) {
            int x = c[0] + sign.x(), z = c[1] + sign.z();
            if ((x >> 4) != (x0 >> 4) || (z >> 4) != (z0 >> 4)) continue;
            BlockPos at = new BlockPos(x, IslandShape.BASIN_Y + sign.y(), z);
            new com.jttw.engine.world.hell.Site(level, x, z, x, z, 0, 0, 0).sign(x, at.getY(), z,
                    s(Blocks.DARK_OAK_WALL_SIGN).setValue(net.minecraft.world.level.block.WallSignBlock.FACING, sign.facing()), sign.lines());
            any = true;
        }
        return any;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        boolean any = camps(level, seed, c, x0, z0, context.random());
        if (CavePlan.near(x0 + 8 - c[0], z0 + 8 - c[1], 16)) any |= garrison(level, c, x0, z0, context.random());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // a pine on the head of each pillar whose middle is here
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz, lx = x - c[0], lz = z - c[1];
            if (ZONE.depth(seed, x, z) <= 0) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            p.set(x, top, z);
            if (!ZONE.isAt(level, p)) continue;
            BlockState here = level.getBlockState(p);
            if (!soil(here) || !level.isEmptyBlock(p.set(x, top + 1, z))) continue;
            if (IslandShape.campAt(seed, lx, lz) != null || CavePlan.covers(lx, lz) && top < IslandShape.BASIN_Y + CavePlan.Y_MAX) continue;
            if (IslandShape.ledge(lx, lz)) continue;
            IslandShape.Pillar pillar = IslandShape.pillarAt(seed, lx, lz);
            if (pillar != null) {
                if (lx == pillar.x() && lz == pillar.z()) pine(level, p, x, top, z);
                else if (hash(x, z, 3) < 90) put(level, p, x, top + 1, z, s(Blocks.FERN), false);
                any = true;
                continue;
            }
            double u = IslandShape.out(seed, lx, lz);
            int roll = hash(x, z, 1);
            double orchard = 0.5 + 0.5 * Math.sin(lx / 37.0 + 1.3) * Math.cos(lz / 29.0 - 0.7);
            boolean wet = IslandShape.stream(seed, lx + 3, lz) || IslandShape.stream(seed, lx - 3, lz)
                    || (lz > IslandShape.CLIFF_Z && Math.abs(lx) < IslandShape.BASIN_HALF && lz < IslandShape.BASIN_END);
            boolean high = top > IslandShape.SEA + 95;
            if (wet && roll < 14) bamboo(level, p, x, top, z);
            else if (!high && u > 0.18 && u < 0.88 && orchard > 0.45 && roll < 22) peach(level, p, x, top, z);
            else if (high && roll < 12) pine(level, p, x, top, z);
            else if (!high && u < 0.92 && roll < 8) broadleaf(level, p, x, top, z);
            else if (roll < 330) put(level, p, x, top + 1, z, s(Blocks.SHORT_GRASS), false);
            else if (roll < 400) put(level, p, x, top + 1, z, s(Blocks.FERN), false);
            else if (roll < 420) tall(level, p, x, top, z, high ? Blocks.LARGE_FERN : Blocks.PEONY);
            else if (roll < 432) tall(level, p, x, top, z, Blocks.ROSE_BUSH);
            else if (roll < 444) tall(level, p, x, top, z, Blocks.LILAC);
            else if (roll < 470) put(level, p, x, top + 1, z, s(hash(x, z, 9) < 500 ? Blocks.AZURE_BLUET : Blocks.OXEYE_DAISY), false);
            else if (roll < 485 && !high) put(level, p, x, top + 1, z, s(Blocks.SWEET_BERRY_BUSH), false);
            else if (roll < 520) put(level, p, x, top + 1, z, s(Blocks.MOSS_CARPET), false);
            else if (roll < 540 && u > 0.2 && !high) put(level, p, x, top + 1, z, s(Blocks.PINK_PETALS).setValue(PinkPetalsBlock.AMOUNT, 2), false);
            any = true;
        }
        return any;
    }
}
