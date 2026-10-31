package com.jttw.engine.world.blackwind;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.storage.loot.LootTable;

import javax.annotation.Nullable;

import static com.jttw.engine.world.blackwind.BlackWindPlan.*;

/**
 * What each of the Black Wind Cave's codes ({@link BlackWindPlan}, {@link TemplePlan}) is in the world,
 * and the laying of it: the peak's dark rock in bands of deepslate, tuff and blackstone; deepslate
 * brick and tile; spruce and dark oak; soul fire; the door's black lacquer, iron and gold; the grate.
 */
public final class BlackWindBlocks {

    private BlackWindBlocks() {}

    /** The Black Bear's treasury, by his throne. */
    public static final ResourceKey<LootTable> TREASURY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/black_wind_treasury"));

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static int hash(int x, int y, int z) {
        return (int) Math.floorMod(Mth.getSeed(x, y, z), 100L);
    }

    /** The black peak's rock inside, in uneven bands of deepslate, tuff and blackstone, a seam of coal now and then. */
    public static BlockState rock(int y, int x, int z) {
        int band = Math.floorMod(y + ((x * 29 + z * 13) >> 5), 13);
        return switch (band) {
            case 0, 1 -> s(Blocks.TUFF);
            case 4 -> s(Blocks.BLACKSTONE);
            case 7 -> hash(x >> 1, y, z >> 1) < 12 ? s(Blocks.COAL_ORE) : s(Blocks.DEEPSLATE);
            case 9 -> s(Blocks.COBBLED_DEEPSLATE);
            case 11 -> s(Blocks.ANDESITE);
            default -> s(Blocks.DEEPSLATE);
        };
    }

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static boolean joins(byte c) {
        return c == FENCE || c == SPRUCE_FENCE || c == BARS || c == SEAL || c == WALL || c == BRICK || c == TILE || c == LOG || c == PLANKS;
    }

    @Nullable
    public static BlockState state(BlackWindPlan.Built plan, int x, int y, int z, int wx, int wy, int wz) {
        return state(plan.code(x, y, z), wx, wy, wz, plan::code, x, y, z);
    }

    /** The plan's neighbours, for fences and bars to join to. */
    @FunctionalInterface
    public interface Codes {
        byte code(int x, int y, int z);
    }

    @Nullable
    public static BlockState state(byte c, int wx, int wy, int wz, Codes around, int x, int y, int z) {
        return switch (c) {
            case UNTOUCHED -> null;
            case AIR -> s(Blocks.AIR);
            case ROCK -> rock(wy, wx, wz);
            case BRICK -> hash(wx, wy, wz) < 14 ? s(Blocks.CRACKED_DEEPSLATE_BRICKS) : s(Blocks.DEEPSLATE_BRICKS);
            case CRACKED -> s(Blocks.CRACKED_DEEPSLATE_TILES);
            case TILE -> s(Blocks.DEEPSLATE_TILES);
            case POLISHED -> s(Blocks.POLISHED_DEEPSLATE);
            case CHISELED -> s(Blocks.CHISELED_DEEPSLATE);
            case BLACKSTONE -> s(Blocks.BLACKSTONE);
            case GILDED -> s(Blocks.GILDED_BLACKSTONE);
            case GOLD, DOOR_STUD -> s(Blocks.GOLD_BLOCK);
            case LOG -> s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case LOG_X -> s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case LOG_Z -> s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            case SPRUCE_LOG -> s(Blocks.SPRUCE_LOG);
            case PLANKS -> s(Blocks.SPRUCE_PLANKS);
            case DARK_PLANKS -> s(Blocks.DARK_OAK_PLANKS);
            case ROOF_N -> stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH);
            case ROOF_E -> stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST);
            case ROOF_S -> stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH);
            case ROOF_W -> stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST);
            case STAIR_N -> stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH);
            case STAIR_E -> stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST);
            case STAIR_S -> stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH);
            case STAIR_W -> stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST);
            case SLAB -> s(Blocks.DEEPSLATE_TILE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case TILE_SLAB_TOP -> s(Blocks.DEEPSLATE_TILE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
            case DARK_SLAB -> s(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case TABLE -> s(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
            case FENCE -> join(s(Blocks.DARK_OAK_FENCE), around, x, y, z);
            case SPRUCE_FENCE -> join(s(Blocks.SPRUCE_FENCE), around, x, y, z);
            case BARS -> join(s(Blocks.IRON_BARS), around, x, y, z);
            case SEAL -> s(Blocks.IRON_BARS).setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
            case DOOR_LEAF -> s(Blocks.STRIPPED_DARK_OAK_WOOD);
            case DOOR_BAND -> s(Blocks.NETHERITE_BLOCK);
            case SOUL_LANTERN_HUNG -> s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true);
            case SOUL_LANTERN -> s(Blocks.SOUL_LANTERN);
            case LANTERN_HUNG -> s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            case SOUL_FIRE -> s(Blocks.SOUL_CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, false);
            case CAMPFIRE -> s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true);
            case CHAIN -> s(Blocks.CHAIN).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case CARPET_BLACK -> s(Blocks.BLACK_CARPET);
            case CARPET_RED -> s(Blocks.RED_CARPET);
            case BASALT -> s(Blocks.BASALT).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case HAY -> s(Blocks.HAY_BLOCK);
            case BARREL -> s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP);
            case CHEST -> s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH);
            case WOOL_BLACK -> s(Blocks.BLACK_WOOL);
            case WOOL_RED -> s(Blocks.RED_WOOL);
            case TERRACOTTA -> s(Blocks.RED_TERRACOTTA);
            case YELLOW -> s(Blocks.YELLOW_TERRACOTTA);
            case COBWEB -> s(Blocks.COBWEB);
            case FLOOR -> switch (hash(wx >> 1, 0, wz >> 1) / 20) {
                case 0 -> s(Blocks.TUFF);
                case 1 -> s(Blocks.GRAVEL);
                case 2 -> s(Blocks.COBBLED_DEEPSLATE);
                default -> s(Blocks.DEEPSLATE);
            };
            case PATH -> hash(wx, wy, wz) < 20 ? s(Blocks.CRACKED_DEEPSLATE_TILES) : s(Blocks.POLISHED_DEEPSLATE);
            case WALL -> s(Blocks.DEEPSLATE_BRICK_WALL).setValue(WallBlock.UP, true)
                    .setValue(WallBlock.NORTH_WALL, joins(around.code(x, y, z - 1)) ? WallSide.LOW : WallSide.NONE)
                    .setValue(WallBlock.SOUTH_WALL, joins(around.code(x, y, z + 1)) ? WallSide.LOW : WallSide.NONE)
                    .setValue(WallBlock.EAST_WALL, joins(around.code(x + 1, y, z)) ? WallSide.LOW : WallSide.NONE)
                    .setValue(WallBlock.WEST_WALL, joins(around.code(x - 1, y, z)) ? WallSide.LOW : WallSide.NONE);
            case BONE -> s(Blocks.BONE_BLOCK);
            case COAL -> s(Blocks.COAL_BLOCK);
            case CALCITE -> s(Blocks.CALCITE);
            case CRYING -> s(Blocks.CRYING_OBSIDIAN);
            case SOUL_SOIL -> s(Blocks.SOUL_SOIL);
            case POLISHED_BLACKSTONE -> s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            case MOSS -> s(Blocks.MOSSY_COBBLESTONE);
            case CANDLE -> s(Blocks.RED_CANDLE).setValue(BlockStateProperties.LIT, true);
            default -> TemplePlan.state(c, wx, wy, wz, around, x, y, z);
        };
    }

    private static BlockState join(BlockState st, Codes around, int x, int y, int z) {
        return st.setValue(CrossCollisionBlock.NORTH, joins(around.code(x, y, z - 1)))
                .setValue(CrossCollisionBlock.SOUTH, joins(around.code(x, y, z + 1)))
                .setValue(CrossCollisionBlock.EAST, joins(around.code(x + 1, y, z)))
                .setValue(CrossCollisionBlock.WEST, joins(around.code(x - 1, y, z)));
    }

    /** Whether a block is of the kinds the great door is made of (its black lacquer, its black iron, its gold). */
    public static boolean doorish(BlockState st) {
        return st.is(Blocks.STRIPPED_DARK_OAK_WOOD) || st.is(Blocks.NETHERITE_BLOCK) || st.is(Blocks.GOLD_BLOCK);
    }

    /** Lays one block at world {@code p}: the treasury's chest with its loot. */
    public static void lay(LevelAccessor level, BlockPos p, BlockState state, RandomSource random) {
        level.setBlock(p, state, Block.UPDATE_CLIENTS);
        if (state.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, random, p, TREASURY_LOOT);
    }

    /** Lays the cave plan's local box round {@code origin} (its local (0, 0, 0)); returns how many blocks it set. The tests' and the gallery's. */
    public static int layBox(LevelAccessor level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1) {
        BlackWindPlan.Built plan = BlackWindPlan.get();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int n = 0;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) {
            BlockState st = state(plan, x, y, z, origin.getX() + x, origin.getY() + y, origin.getZ() + z);
            if (st == null) continue;
            lay(level, p.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z), st, level.getRandom());
            n++;
        }
        return n;
    }

    /** Lays the temple's ruin round {@code origin} (its terrace's middle, at its floor); returns how many blocks it set. */
    public static int layTemple(LevelAccessor level, BlockPos origin) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int n = 0;
        for (int x = TemplePlan.X_MIN; x <= TemplePlan.X_MAX; x++) for (int z = TemplePlan.Z_MIN; z <= TemplePlan.Z_MAX; z++) {
            for (int y = TemplePlan.Y_MIN; y <= TemplePlan.Y_MAX; y++) {
                BlockState st = TemplePlan.blockAt(x, y, z, origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                if (st == null) continue;
                level.setBlock(p.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z), st, Block.UPDATE_CLIENTS);
                n++;
            }
        }
        return n;
    }
}
