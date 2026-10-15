package com.jttw.engine.world.heaven;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction.Island;
import com.jttw.engine.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


/**
 * What stands on Heaven's islands: now and then a pavilion of the Immortals with a chest in it (and
 * in the chests, now and then, Laozi's elixir pills); rarely, instead, a garden of the Queen Mother's
 * peach trees, Immortal Peaches hanging in them; and once in all Heaven, the Jade Palace, with the
 * Heavenly Wine of its banquet and more of the pills.
 */
public final class HeavenFeatures {

    private HeavenFeatures() {}

    /** The share of islands with a building, and of those, the share that are peach gardens. */
    public static final float BUILDING_CHANCE = 0.25f, GARDEN_CHANCE = 0.2f;

    public static final ResourceLocation PAVILION_CHEST = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "chests/heaven_pavilion");
    public static final ResourceLocation PALACE_CHEST = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "chests/jade_palace");

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    private static void chest(WorldGenLevel level, BlockPos p, Direction facing, ResourceLocation loot, RandomSource r) {
        set(level, p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity c) c.setLootTable(loot, r.nextLong());
    }

    /** Whether this chunk holds the point. */
    private static boolean inChunk(BlockPos origin, int x, int z) {
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        return x >= x0 && x < x0 + 16 && z >= z0 && z < z0 + 16;
    }

    // ── The Jade Palace: one in all Heaven ───────────────────────────────

    /** The cell the Jade Palace stands in, by the world's seed: one of the 24 round Heaven's middle. */
    public static int[] palaceCell(long seed) {
        RandomSource r = RandomSource.create(seed ^ 0x1ADE9A1ACEL);
        int cx, cz;
        do {
            cx = r.nextInt(5) - 2;
            cz = r.nextInt(5) - 2;
        } while (cx == 0 && cz == 0);
        return new int[]{cx, cz};
    }

    /** Where the Jade Palace stands: its island's heart, or the middle of its cell in the open sky, on a foundation of its own. */
    public static BlockPos palace(long seed) {
        int[] c = palaceCell(seed);
        Island island = CelestialIslandsDensityFunction.island(c[0], c[1]);
        if (island != null) return new BlockPos(island.x(), island.y() + 2, island.z());
        return new BlockPos(c[0] * CelestialIslandsDensityFunction.CELL + CelestialIslandsDensityFunction.CELL / 2, 66,
                c[1] * CelestialIslandsDensityFunction.CELL + CelestialIslandsDensityFunction.CELL / 2);
    }

    public static class JadePalace extends Feature<NoneFeatureConfiguration> {
        public JadePalace(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            BlockPos at = palace(ctx.level().getSeed());
            if (!inChunk(ctx.origin(), at.getX(), at.getZ())) return false;
            build(ctx.level(), at, ctx.random());
            return true;
        }

        /** The palace, its floor at {@code at}'s height, facing south. */
        public static void build(WorldGenLevel level, BlockPos at, RandomSource r) {
            BlockState jade = ModBlocks.JADE_BLOCK.get().defaultBlockState(), quartz = ModBlocks.CELESTIAL_MARBLE.get().defaultBlockState(),
                    pillar = ModBlocks.CELESTIAL_MARBLE.get().defaultBlockState(), gold = Blocks.GOLD_BLOCK.defaultBlockState(),
                    cloud = ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState(), air = Blocks.AIR.defaultBlockState(),
                    red = Blocks.RED_CONCRETE.defaultBlockState();
            int x = at.getX(), y = at.getY(), z = at.getZ();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            // its foundation: a round terrace of jade on a bank of cloud
            for (int dx = -11; dx <= 11; dx++) for (int dz = -11; dz <= 11; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 11.5) continue;
                set(level, p.set(x + dx, y - 1, z + dz), d > 10 ? gold : (dx + dz) % 4 == 0 ? quartz : jade);
                for (int down = 2; down <= 2 + (int) (4 * (1 - d / 12)); down++) set(level, p.set(x + dx, y - down, z + dz), cloud);
                for (int up = 0; up < 12; up++) set(level, p.set(x + dx, y + up, z + dz), air);
            }
            // the hall: jade walls, quartz pillars, a gate to the south
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 7;
                set(level, p.set(x + dx, y - 1, z + dz), (Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? gold : quartz);
                if (!edge) continue;
                boolean corner = Math.abs(dx) == 7 && Math.abs(dz) == 7, post = edge && (dx % 4 == 3 || dz % 4 == 3 || corner);
                for (int up = 0; up < 6; up++) {
                    boolean gate = dz == 7 && Math.abs(dx) <= 1 && up < 4;
                    boolean window = !post && up >= 2 && up <= 3 && !gate;
                    Direction out = Math.abs(dz) == 7 ? (dz > 0 ? Direction.SOUTH : Direction.NORTH) : (dx > 0 ? Direction.EAST : Direction.WEST);
                    set(level, p.set(x + dx, y + up, z + dz), gate ? air : post ? (up == 5 ? gold : red) : window ? lattice(out) : jade);
                }
            }
            // three tiers of golden roof
            int[][] tiers = {{8, 6}, {6, 7}, {4, 8}, {2, 9}, {0, 10}};
            for (int[] t : tiers) for (int dx = -t[0]; dx <= t[0]; dx++) for (int dz = -t[0]; dz <= t[0]; dz++) {
                set(level, p.set(x + dx, y + t[1], z + dz), Math.abs(dx) == t[0] || Math.abs(dz) == t[0] ? gold : jade);
            }
            hall(level, x, y, z, r);
        }

        /** A red lattice window, a mangrove trapdoor standing open in the wall. */
        private static BlockState lattice(Direction out) {
            return Blocks.MANGROVE_TRAPDOOR.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true)
                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, out.getOpposite());
        }

        /**
         * The hall within, in the manner of the palaces below Heaven, but in jade and red: a red
         * carpet from the gate to the throne between two rows of red-lacquered columns, red beams
         * across a jade ceiling, the throne on a jade dais before a gold screen hung with red
         * banners, red lanterns, the banquet laid on red-lacquered tables, the treasury beside the
         * throne.
         */
        private static void hall(WorldGenLevel level, int x, int y, int z, RandomSource r) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            BlockState jade = ModBlocks.JADE_BLOCK.get().defaultBlockState(), gold = Blocks.GOLD_BLOCK.defaultBlockState(),
                    red = Blocks.RED_CONCRETE.defaultBlockState(), carpet = Blocks.RED_CARPET.defaultBlockState(),
                    air = Blocks.AIR.defaultBlockState();
            // the floor: jade, a gold-edged red aisle from the gate to the dais
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                set(level, p.set(x + dx, y - 1, z + dz), Math.abs(dx) == 2 ? gold : jade);
                for (int up = 0; up < 6; up++) set(level, p.set(x + dx, y + up, z + dz), air);
                if (Math.abs(dx) <= 1 && dz >= -3) set(level, p.set(x + dx, y, z + dz), carpet);
            }
            // two rows of red-lacquered columns, gold at foot and head
            for (int side : new int[]{-4, 4}) for (int dz : new int[]{-3, 0, 3, 6}) {
                if (dz == 6) continue;
                for (int up = 0; up < 6; up++) set(level, p.set(x + side, y + up, z + dz), up == 0 || up == 5 ? gold : red);
            }
            // red beams across a jade ceiling
            for (int dz = -6; dz <= 6; dz += 3) for (int dx = -6; dx <= 6; dx++) set(level, p.set(x + dx, y + 5, z + dz), red);
            // the throne: a two-step jade dais, gold-edged, the throne gold with a jade back
            for (int dx = -3; dx <= 3; dx++) for (int dz = -6; dz <= -4; dz++) {
                set(level, p.set(x + dx, y, z + dz), Math.abs(dx) == 3 || dz == -4 ? gold : jade);
            }
            for (int dx = -2; dx <= 2; dx++) for (int dz = -6; dz <= -5; dz++) set(level, p.set(x + dx, y + 1, z + dz), jade);
            set(level, p.set(x, y + 2, z - 5), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            set(level, p.set(x, y + 2, z - 6), jade);
            set(level, p.set(x, y + 3, z - 6), gold);
            for (int dx : new int[]{-1, 1}) {
                set(level, p.set(x + dx, y + 2, z - 6), gold);
                set(level, p.set(x + dx, y + 2, z - 5), Blocks.GOLD_BLOCK.defaultBlockState());
            }
            // a gold screen behind it, hung with red banners
            for (int dx = -3; dx <= 3; dx++) for (int up = 1; up < 5; up++) set(level, p.set(x + dx, y + up, z - 7), gold);
            for (int dx : new int[]{-3, -2, 2, 3}) {
                set(level, p.set(x + dx, y + 3, z - 6), Blocks.RED_WALL_BANNER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, Direction.SOUTH));
            }
            // the banquet, on red-lacquered tables, cushions either side; wine jars and candles on them
            for (int side : new int[]{-6, 6}) for (int dz = -2; dz <= 4; dz++) {
                set(level, p.set(x + side, y, z + dz), Blocks.MANGROVE_SLAB.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
                set(level, p.set(x + side + (side < 0 ? 1 : -1), y, z + dz), dz % 2 == 0 ? Blocks.RED_CARPET.defaultBlockState() : air);
                if (dz % 3 == 0) set(level, p.set(x + side, y + 1, z + dz), ModBlocks.WINE_JAR.get().defaultBlockState());
                else if (dz % 3 == 1) set(level, p.set(x + side, y + 1, z + dz), Blocks.YELLOW_CANDLE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
            }
            // the treasury, either side of the throne
            chest(level, p.set(x - 5, y, z - 6).immutable(), Direction.SOUTH, PALACE_CHEST, r);
            chest(level, p.set(x + 5, y, z - 6).immutable(), Direction.SOUTH, PALACE_CHEST, r);
            // red lanterns hung on chains, down the hall and at the gate
            for (int[] l : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}, {-2, 5}, {2, 5}}) {
                set(level, p.set(x + l[0], y + 4, z + l[1]), Blocks.CHAIN.defaultBlockState());
                set(level, p.set(x + l[0], y + 3, z + l[1]), Blocks.SHROOMLIGHT.defaultBlockState());
            }
            for (int dx : new int[]{-2, 2}) set(level, p.set(x + dx, y, z + 8), Blocks.LANTERN.defaultBlockState());
            // bamboo in the corners
            for (int[] c : new int[][]{{-6, 6}, {6, 6}, {-6, -6}, {6, -6}}) {
                set(level, p.set(x + c[0], y, z + c[1]), Blocks.POTTED_BAMBOO.defaultBlockState());
            }
        }
    }

    // ── Pavilions and peach gardens, on the islands ──────────────────────

    /** What stands on this island, if anything: 0 nothing, 1 a pavilion, 2 a peach garden. */
    public static int buildingOn(long seed, Island island, int cx, int cz) {
        int[] palace = palaceCell(seed);
        if (palace[0] == cx && palace[1] == cz) return 0; // the palace's
        RandomSource r = RandomSource.create(seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL));
        if (r.nextFloat() >= BUILDING_CHANCE || island.radius() < 9) return 0;
        return r.nextFloat() < GARDEN_CHANCE ? 2 : 1;
    }

    public static class IslandBuilding extends Feature<NoneFeatureConfiguration> {
        public IslandBuilding(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos origin = ctx.origin();
            int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15, cell = CelestialIslandsDensityFunction.CELL;
            // the island whose heart is in this chunk, from whichever cell the chunk touches
            Island island = null;
            int cx = 0, cz = 0;
            for (int ax : new int[]{x0, x0 + 15}) for (int az : new int[]{z0, z0 + 15}) {
                Island i = CelestialIslandsDensityFunction.island(Math.floorDiv(ax, cell), Math.floorDiv(az, cell));
                if (i != null && inChunk(origin, i.x(), i.z())) {
                    island = i;
                    cx = Math.floorDiv(ax, cell);
                    cz = Math.floorDiv(az, cell);
                }
            }
            if (island == null) return false;
            int kind = buildingOn(level.getSeed(), island, cx, cz);
            if (kind == 0) return false;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, island.x(), island.z());
            if (y <= level.getMinBuildHeight() + 1) return false;
            BlockPos at = new BlockPos(island.x(), y, island.z());
            if (kind == 1) pavilion(level, at, ctx.random());
            else garden(level, at, ctx.random());
            return true;
        }

        /** A pavilion of the Immortals: a quartz floor, four pillars, a golden roof, and a chest. */
        public static void pavilion(WorldGenLevel level, BlockPos at, RandomSource r) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int x = at.getX(), y = at.getY(), z = at.getZ();
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                set(level, p.set(x + dx, y - 1, z + dz), Math.abs(dx) == 3 || Math.abs(dz) == 3 ? Blocks.GOLD_BLOCK.defaultBlockState() : ModBlocks.CELESTIAL_MARBLE.get().defaultBlockState());
                for (int up = 0; up < 6; up++) set(level, p.set(x + dx, y + up, z + dz), Blocks.AIR.defaultBlockState());
            }
            for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                for (int up = 0; up < 4; up++) set(level, p.set(x + c[0], y + up, z + c[1]), ModBlocks.CELESTIAL_MARBLE.get().defaultBlockState());
            }
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                set(level, p.set(x + dx, y + 4, z + dz), rim ? Blocks.GOLD_BLOCK.defaultBlockState() : ModBlocks.JADE_BLOCK.get().defaultBlockState());
            }
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(level, p.set(x + dx, y + 5, z + dz), Blocks.GOLD_BLOCK.defaultBlockState());
            set(level, p.set(x, y + 3, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            chest(level, at, Direction.SOUTH, PAVILION_CHEST, r);
        }

        /** The Queen Mother's peach garden: a mossy lawn, four peach trees, Immortal Peaches hanging in them. */
        public static void garden(WorldGenLevel level, BlockPos at, RandomSource r) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int x = at.getX(), y = at.getY(), z = at.getZ();
            for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz > 30) continue;
                set(level, p.set(x + dx, y - 1, z + dz), r.nextInt(4) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
            }
            BlockState log = Blocks.CHERRY_LOG.defaultBlockState(), leaves = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            for (int[] t : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                int tx = x + t[0], tz = z + t[1];
                for (int up = 0; up < 4; up++) set(level, p.set(tx, y + up, tz), log);
                for (int dx = -2; dx <= 2; dx++) for (int dy = 3; dy <= 5; dy++) for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + (dy - 4) * (dy - 4) + dz * dz > 5) continue;
                    if (level.getBlockState(p.set(tx + dx, y + dy, tz + dz)).isAir()) set(level, p, leaves);
                }
                // the peaches: hanging under the leaves
                int peaches = 2 + r.nextInt(3);
                for (int i = 0; i < 12 && peaches > 0; i++) {
                    p.set(tx + r.nextInt(5) - 2, y + 2, tz + r.nextInt(5) - 2);
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).is(Blocks.CHERRY_LEAVES)) {
                        set(level, p, ModBlocks.PEACH_FRUIT.get().defaultBlockState());
                        peaches--;
                    }
                }
            }
            set(level, p.set(x, y, z), Blocks.LANTERN.defaultBlockState());
        }
    }

    // ── Hell's Hall of Judgement ─────────────────────────────────────────

    /** Sets the Book of Life and Death on a dais near where travellers arrive in Hell, if it is not there already. */
    public static void hallOfJudgement(net.minecraft.server.level.ServerLevel hell, BlockPos arrival) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -24; dx <= 24; dx++) for (int dy = -12; dy <= 12; dy++) for (int dz = -24; dz <= 24; dz++) {
            if (hell.getBlockState(p.setWithOffset(arrival, dx, dy, dz)).is(ModBlocks.BOOK_OF_LIFE_AND_DEATH.get())) return;
        }
        BlockPos book = arrival.offset(0, 0, 4);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            hell.setBlockAndUpdate(p.setWithOffset(book, dx, -1, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            for (int up = 0; up < 3; up++) hell.setBlockAndUpdate(p.setWithOffset(book, dx, up, dz), Blocks.AIR.defaultBlockState());
        }
        hell.setBlockAndUpdate(book, ModBlocks.BOOK_OF_LIFE_AND_DEATH.get().defaultBlockState());
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) hell.setBlockAndUpdate(book.offset(c[0], 0, c[1]), Blocks.SOUL_LANTERN.defaultBlockState());
    }
}
