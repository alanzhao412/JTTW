package com.jttw.engine.world.fire;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.feature.FlamingRangeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
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

import static com.jttw.engine.world.fire.FortressPlan.*;

/**
 * What each of the {@link FortressPlan}'s codes is in the world, and the laying of it: the rock of
 * the mountain in its bands, red and black brick, gold, magma and lava, the door's bronze and iron,
 * the seals' grates. Fences, walls and grates join to what the plan puts beside them, so the plan's
 * shapes come out as they are meant without a block update.
 */
public final class FortressBlocks {

    private FortressBlocks() {}

    /** Red Boy's treasury, by his throne. */
    public static final ResourceKey<LootTable> TREASURY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/fire_cloud_treasury"));

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState stair(Direction facing) {
        return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    /** Whether a neighbour of this code is one a fence, wall or grate joins to. */
    private static boolean joins(byte c) {
        return c == WALL || c == FENCE || c == BARS || c == BARS_NS || c == BARS_EW || seal(c)
                || (holds(c) && c != STAIR_N && c != STAIR_E && c != STAIR_S && c != STAIR_W && c != SLAB && c != CAULDRON
                && c != CAULDRON_LAVA && c != ANVIL && c != CHEST && c != LANTERN);
    }

    /**
     * The block at local (x, y, z) of the plan; null where the plan leaves the ground as it is.
     * {@code wx, wz} are its world column, for the rock's bands.
     */
    @Nullable
    public static BlockState state(FortressPlan.Built plan, int x, int y, int z, int wx, int wy, int wz) {
        byte c = plan.code(x, y, z);
        return switch (c) {
            case UNTOUCHED -> null;
            case AIR -> s(Blocks.AIR);
            case ROCK -> FlamingRangeFeature.inner(wy, wx, wz);
            case BRICK -> s(Blocks.RED_NETHER_BRICKS);
            case CRACKED -> s(Blocks.CRACKED_NETHER_BRICKS);
            case DARK -> s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            case POLISHED -> s(Blocks.POLISHED_BLACKSTONE);
            case GILDED -> s(Blocks.GILDED_BLACKSTONE);
            case CHISELED -> s(Blocks.CHISELED_POLISHED_BLACKSTONE);
            case MAGMA -> s(Blocks.MAGMA_BLOCK);
            case LAVA -> s(Blocks.LAVA);
            case LAMP -> s(Blocks.SHROOMLIGHT);
            case LANTERN_HUNG -> s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            case LANTERN -> s(Blocks.LANTERN);
            case CAMPFIRE, BRAZIER -> s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, false);
            case SMOKE -> s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true);
            case CHAIN -> s(Blocks.CHAIN).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case SEAL_NS, BARS_NS -> s(Blocks.IRON_BARS).setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
            case SEAL_EW, BARS_EW -> s(Blocks.IRON_BARS).setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
            case BARS -> s(Blocks.IRON_BARS);
            case DOOR_IRON -> s(Blocks.WAXED_CUT_COPPER); // the leaves' bronze, in panels
            case DOOR_BAND -> s(Blocks.NETHERITE_BLOCK); // their bands of black iron
            case DOOR_STUD, GOLD -> s(Blocks.GOLD_BLOCK);
            case CARPET -> s(Blocks.RED_CARPET);
            case STAIR_N -> stair(Direction.NORTH);
            case STAIR_E -> stair(Direction.EAST);
            case STAIR_S -> stair(Direction.SOUTH);
            case STAIR_W -> stair(Direction.WEST);
            case SLAB -> s(Blocks.POLISHED_BLACKSTONE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case BASALT -> s(Blocks.BASALT).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case NETHER -> s(Blocks.NETHER_BRICKS);
            case FENCE -> fence(plan, x, y, z);
            case ANVIL -> s(Blocks.ANVIL);
            case BLAST -> s(Blocks.BLAST_FURNACE).setValue(BlockStateProperties.LIT, true);
            case SMOKER -> s(Blocks.SMOKER).setValue(BlockStateProperties.LIT, true);
            case CAULDRON -> s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3);
            case CAULDRON_LAVA -> s(Blocks.LAVA_CAULDRON);
            case COAL -> s(Blocks.COAL_BLOCK);
            case BONE -> s(Blocks.BONE_BLOCK);
            case HAY -> s(Blocks.HAY_BLOCK);
            case WALL -> wall(plan, x, y, z);
            case BAMBOO -> s(Blocks.BAMBOO_MOSAIC);
            case WOOL -> s(Blocks.RED_WOOL);
            case TERRACOTTA -> s(Blocks.RED_TERRACOTTA);
            case ORANGE -> s(Blocks.ORANGE_TERRACOTTA);
            case CHEST -> s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH);
            case OBSIDIAN -> s(Blocks.OBSIDIAN);
            case BLACKSTONE -> s(Blocks.BLACKSTONE);
            case ASH -> s(ModBlocks.CINDER_ASH.get());
            default -> s(Blocks.AIR);
        };
    }

    /** Whether a block is of the kinds the great door is made of (its bronze, its black iron, its gold). */
    public static boolean doorish(BlockState st) {
        return st.is(Blocks.WAXED_CUT_COPPER) || st.is(Blocks.NETHERITE_BLOCK) || st.is(Blocks.GOLD_BLOCK);
    }

    private static BlockState fence(FortressPlan.Built plan, int x, int y, int z) {
        return s(Blocks.NETHER_BRICK_FENCE)
                .setValue(CrossCollisionBlock.NORTH, joins(plan.code(x, y, z - 1)))
                .setValue(CrossCollisionBlock.SOUTH, joins(plan.code(x, y, z + 1)))
                .setValue(CrossCollisionBlock.EAST, joins(plan.code(x + 1, y, z)))
                .setValue(CrossCollisionBlock.WEST, joins(plan.code(x - 1, y, z)));
    }

    private static BlockState wall(FortressPlan.Built plan, int x, int y, int z) {
        boolean n = joins(plan.code(x, y, z - 1)), so = joins(plan.code(x, y, z + 1)), e = joins(plan.code(x + 1, y, z)), w = joins(plan.code(x - 1, y, z));
        boolean straight = (n && so && !e && !w) || (e && w && !n && !so);
        byte over = plan.code(x, y + 1, z);
        return s(Blocks.POLISHED_BLACKSTONE_WALL)
                .setValue(WallBlock.NORTH_WALL, n ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.SOUTH_WALL, so ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.EAST_WALL, e ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.WEST_WALL, w ? WallSide.LOW : WallSide.NONE)
                .setValue(WallBlock.UP, !straight || holds(over) || over == CAMPFIRE || over == SMOKE);
    }

    /** Lays one block of the plan at world {@code p}: the treasury's chest with its loot. */
    public static void lay(LevelAccessor level, BlockPos p, BlockState state, RandomSource random) {
        level.setBlock(p, state, Block.UPDATE_CLIENTS);
        if (state.is(Blocks.CHEST)) RandomizableContainer.setBlockEntityLootTable(level, random, p, TREASURY_LOOT);
    }

    /**
     * Lays the plan's local box (x0..x1, y0..y1, z0..z1) round {@code origin} (its local (0, 0, 0)),
     * every block it sets; returns how many. The gallery's and the tests' (the range lays its own).
     */
    public static int layBox(LevelAccessor level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1) {
        FortressPlan.Built plan = FortressPlan.get();
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

    /** Lays floor {@code n} (1-8; 9 the throne room) round {@code origin}, from the rock under it to the rock over it, and its stair down. */
    public static int layFloor(LevelAccessor level, BlockPos origin, int n) {
        int y = FortressPlan.levelOf(n), h = n >= 9 ? THRONE_H : FLOOR_H[n - 1];
        int below = n >= 9 ? -4 : n == FLOORS ? THRONE_Y - FLOOR_Y[FLOORS - 1] : FLOOR_Y[n] - FLOOR_Y[n - 1];
        return layBox(level, origin, X_MIN, y + below - 1, Z_MIN, X_MAX, y + h + 2, Z_MAX);
    }
}
