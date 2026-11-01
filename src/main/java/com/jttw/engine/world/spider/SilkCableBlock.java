package com.jttw.engine.world.spider;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cable of the sisters' silk, out in the air: the stays that hold their canopy down to the
 * ground. A cord through the cell from where it comes in ({@link #FROM}) to where it goes on
 * ({@link #TO}), each through a face or across an edge (so a stay running slant runs on as a slant
 * line, not a stair); {@link #THICK} a heavy one. Like the canopy's silk, no hand breaks it; it
 * falls with the canopy.
 */
public class SilkCableBlock extends Block {

    /** Where a cord leaves a cell: through one of its faces, or across one of its edges; or nowhere. */
    public enum Way implements StringRepresentable {
        NONE(0, 0, 0),
        DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0),
        NORTH_UP(0, 1, -1), EAST_UP(1, 1, 0), SOUTH_UP(0, 1, 1), WEST_UP(-1, 1, 0),
        NORTH_DOWN(0, -1, -1), EAST_DOWN(1, -1, 0), SOUTH_DOWN(0, -1, 1), WEST_DOWN(-1, -1, 0),
        NORTH_EAST(1, 0, -1), SOUTH_EAST(1, 0, 1), SOUTH_WEST(-1, 0, 1), NORTH_WEST(-1, 0, -1);

        public final int dx, dy, dz;

        Way(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        /** The way out toward the cell (dx, dy, dz) off, or NONE if it is not a neighbour across a face or an edge. */
        public static Way toward(int dx, int dy, int dz) {
            for (Way w : values()) if (w != NONE && w.dx == dx && w.dy == dy && w.dz == dz) return w;
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final MapCodec<SilkCableBlock> CODEC = simpleCodec(SilkCableBlock::new);
    public static final EnumProperty<Way> FROM = EnumProperty.create("from", Way.class);
    public static final EnumProperty<Way> TO = EnumProperty.create("to", Way.class);
    public static final BooleanProperty THICK = BooleanProperty.create("thick");
    private static final VoxelShape KNOT = Block.box(5, 5, 5, 11, 11, 11);

    public SilkCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FROM, Way.NONE).setValue(TO, Way.NONE).setValue(THICK, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FROM, TO, THICK);
    }

    /** The cable coming in by {@code from} and going on by {@code to}, heavy or not. */
    public BlockState with(Way from, Way to, boolean thick) {
        return defaultBlockState().setValue(FROM, from).setValue(TO, to).setValue(THICK, thick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return KNOT;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }
}
