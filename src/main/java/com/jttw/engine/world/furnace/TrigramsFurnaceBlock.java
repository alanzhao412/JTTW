package com.jttw.engine.world.furnace;

import com.jttw.engine.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Eight Trigrams Furnace, Laozi's: the one place armaments are forged and refined. A great
 * eight-sided bronze brazier, three blocks across and five high, each side bearing one of the eight
 * trigrams (Qian, Kan, Gen, Zhen, Sun, Li, Kun, Dui), with a hatch on top into its fire. It burns
 * only True Samadhi Fire, and glows and lights the land round it while it burns.
 *
 * <p>This is its core, at the bottom of its middle column: the furnace's workings are here
 * ({@link TrigramsFurnaceBlockEntity}), and it draws the whole furnace. The rest is
 * {@link FurnacePartBlock}s, placed with it and broken with it.
 */
public class TrigramsFurnaceBlock extends BaseEntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final com.mojang.serialization.MapCodec<TrigramsFurnaceBlock> CODEC = simpleCodec(TrigramsFurnaceBlock::new);

    public TrigramsFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /** Its body and rim, as near to its eight sides as boxes come. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(3, 0, 3, 13, 13, 13), Block.box(1.5, 2, 5.5, 14.5, 12, 10.5), Block.box(5.5, 2, 1.5, 10.5, 12, 14.5));

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                               net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // drawn whole by its renderer, across all its parts
    }

    /** The furnace's parts round its core: offsets in x and z from -1 to 1, and up 0 to 4, all but the core's own. */
    public static List<int[]> partOffsets() {
        List<int[]> out = new ArrayList<>();
        for (int dy = 0; dy <= 4; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 || dy != 0 || dz != 0) out.add(new int[]{dx, dy, dz});
        }
        return out;
    }

    /** Whether the whole furnace has room to stand with its core here. */
    public static boolean hasRoom(Level level, BlockPos core, java.util.function.Predicate<BlockPos> free) {
        if (core.getY() + 4 >= level.getMaxBuildHeight()) return false;
        for (int[] o : partOffsets()) if (!free.test(core.offset(o[0], o[1], o[2]))) return false;
        return true;
    }

    /** Raises the rest of the furnace round its core. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) raiseParts(level, pos);
    }

    private static void raiseParts(Level level, BlockPos core) {
        FurnacePartBlock part = (FurnacePartBlock) com.jttw.engine.registry.ModBlocks.FURNACE_PART.get();
        for (int[] o : partOffsets()) level.setBlock(core.offset(o[0], o[1], o[2]), part.at(o[0], o[1], o[2]), Block.UPDATE_ALL);
    }

    /** Builds a whole furnace with its core here (as placing one does), and returns its workings. */
    public static TrigramsFurnaceBlockEntity build(Level level, BlockPos core) {
        level.setBlock(core, com.jttw.engine.registry.ModBlocks.EIGHT_TRIGRAMS_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
        raiseParts(level, core);
        return (TrigramsFurnaceBlockEntity) level.getBlockEntity(core);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrigramsFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TRIGRAMS_FURNACE.get(), TrigramsFurnaceBlockEntity::serverTick);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TrigramsFurnaceBlockEntity be && player instanceof ServerPlayer sp) {
            sp.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TrigramsFurnaceBlockEntity be) {
            Containers.dropContents(level, pos, be);
            // and the rest of it goes with it
            for (int[] o : partOffsets()) {
                BlockPos p = pos.offset(o[0], o[1], o[2]);
                if (level.getBlockState(p).getBlock() instanceof FurnacePartBlock) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 5.0, z = pos.getZ() + 0.5; // out of its hatch
        level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, 0, 0.04, 0);
    }
}
