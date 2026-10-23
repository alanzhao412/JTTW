package com.jttw.engine.world.fengdu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The ghost veil (鬼门), the cold soul-fire mist that fills a Gate of Ghosts' way: Fengdu's, which
 * leads down to Diyu, and the side doors of the Gate of Ghosts on Hell's arrival plaza, which lead
 * back up to Fengdu. It is no wall (anything walks into it) and cannot be broken; a player who
 * stands in it for a moment is taken through ({@link Fengdu#tick}). Laid in a plane, as a nether
 * portal is: {@link #AXIS} the way it runs.
 */
public class GhostGateBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape ALONG_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ALONG_Z = Block.box(6, 0, 0, 10, 16, 16);

    public GhostGateBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? ALONG_Z : ALONG_X;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    /** Souls drift in it and wisps of soul fire rise; now and then a moan. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean alongX = state.getValue(AXIS) == Direction.Axis.X;
        double x = pos.getX() + (alongX ? random.nextDouble() : 0.5 + (random.nextDouble() - 0.5) * 0.3);
        double z = pos.getZ() + (alongX ? 0.5 + (random.nextDouble() - 0.5) * 0.3 : random.nextDouble());
        double y = pos.getY() + random.nextDouble();
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.02 + random.nextDouble() * 0.03, 0);
        if (random.nextInt(5) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
        if (random.nextInt(400) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS,
                    0.8f, 0.6f + random.nextFloat() * 0.3f, false);
        }
    }
}
