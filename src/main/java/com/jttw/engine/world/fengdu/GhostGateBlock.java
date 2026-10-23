package com.jttw.engine.world.fengdu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
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

import javax.annotation.Nullable;

/**
 * The ghost veil (鬼门), the cold soul-fire mist that fills a Gate of Ghosts' way: Fengdu's, which
 * leads down to Diyu once it burns; the side doors of the Gate of Ghosts on Hell's arrival plaza,
 * which lead back up to Fengdu; the frame at the heart of an Ancient City, once soul fire is lit in
 * it, which leads to Fengdu; and the side doors of Fengdu's gate, which lead back to that city. It
 * is no wall (anything walks into it) and cannot be broken; a player who stands in it for a moment
 * is taken through ({@link Fengdu#tick}). Laid in a plane, as a nether portal is: {@link #AXIS} the
 * way it runs; {@link #WAY} where it leads in the world of the living (in Diyu every veil leads up
 * to Fengdu). No water washes it out (an Ancient City's frame may stand in a flooded hall).
 */
public class GhostGateBlock extends Block implements LiquidBlockContainer {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<Way> WAY = EnumProperty.create("way", Way.class);
    private static final VoxelShape ALONG_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ALONG_Z = Block.box(6, 0, 0, 10, 16, 16);

    /**
     * Where a veil leads. {@code HELL}: Fengdu's own Gate of Ghosts, down to Diyu, for those it lets
     * pass (the first, so a veil laid before there were ways, the old open gate of Fengdu among
     * them, asks the gate's leave too). {@code FENGDU}: an Ancient City's frame, to Fengdu's terrace.
     * {@code CITY}: Fengdu's side doors, back to the Ancient City one came from.
     */
    public enum Way implements StringRepresentable {
        HELL("hell"), FENGDU("fengdu"), CITY("city");

        private final String name;

        Way(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public GhostGateBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(WAY, Way.HELL));
    }

    /** A veil leading {@code way}, its plane along {@code axis}. */
    public static BlockState veil(Direction.Axis axis, Way way) {
        return com.jttw.engine.registry.ModBlocks.GHOST_GATE.get().defaultBlockState().setValue(AXIS, axis).setValue(WAY, way);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, WAY);
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

    /** Water never flows into it. */
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return false;
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
