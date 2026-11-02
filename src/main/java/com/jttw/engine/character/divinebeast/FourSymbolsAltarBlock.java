package com.jttw.engine.character.divinebeast;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Altar of the Four Symbols (四象坛): a stepped stone altar with the Four Symbols' pillars at its
 * corners (the Azure Dragon's, the Vermilion Bird's, the White Tiger's, the Black Tortoise's) and a
 * bronze incense cauldron at its heart. A Divine Beast crafts one (a gated recipe) and burns its
 * evolution offerings on it ({@link BeastEvolution}); empty-handed, it says what the next ritual wants.
 */
public class FourSymbolsAltarBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<FourSymbolsAltarBlock> CODEC = simpleCodec(FourSymbolsAltarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(2, 4, 2, 14, 8, 14),
            Block.box(4, 8, 4, 12, 14, 12));

    public FourSymbolsAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof OfferingItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) BeastEvolution.offer(sp, pos, stack);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) BeastEvolution.explain(sp);
        return InteractionResult.CONSUME;
    }

    /** Incense: a thread of smoke from the cauldron, and now and then a mote of light. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.15, y, z + (random.nextDouble() - 0.5) * 0.15, 0, 0.02, 0);
        if (random.nextInt(8) == 0) level.addParticle(ParticleTypes.END_ROD, x + (random.nextDouble() - 0.5) * 0.8, y + 0.2, z + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
    }
}
