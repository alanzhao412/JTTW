package com.jttw.engine.world.spider;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;

/**
 * The sisters' gauze: sealing silk spun so fine it is all but clear, which one sees through and
 * none passes. A cell of it may carry a cord of silk, run from its middle out through any of its
 * faces ({@link PipeBlock#PROPERTY_BY_DIRECTION}; {@link #THICK} a heavy one), so the strands of
 * their canopy run on from cell to cell as thin lines, not blocks; or, where a strand runs slant,
 * out across one of its edges, or straight through it from edge to edge ({@link #SLANT}).
 */
public class SilkVeilBlock extends TransparentBlock {

    public static final MapCodec<SilkVeilBlock> CODEC = simpleCodec(SilkVeilBlock::new);
    public static final BooleanProperty THICK = BooleanProperty.create("thick");

    /** A cord across the cell's edges: none; out across one edge (as {@link SilkCableBlock.Way}); or straight through, edge to edge. */
    public enum Slant implements StringRepresentable {
        NONE, NORTH_UP, EAST_UP, SOUTH_UP, WEST_UP, NORTH_DOWN, EAST_DOWN, SOUTH_DOWN, WEST_DOWN, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST,
        NORTH_UP_SOUTH_DOWN, EAST_UP_WEST_DOWN, SOUTH_UP_NORTH_DOWN, WEST_UP_EAST_DOWN, NORTH_EAST_SOUTH_WEST, SOUTH_EAST_NORTH_WEST;

        /** The slant for the edges in {@code edges} (bit i the i-th edge of {@link SilkCableBlock.Way}, from NORTH_UP): one, or two opposite. */
        public static Slant of(int edges) {
            if (edges == 0) return NONE;
            int first = Integer.numberOfTrailingZeros(edges), rest = edges & ~(1 << first);
            if (rest == 0) return values()[1 + first];
            int second = Integer.numberOfTrailingZeros(rest);
            String a = values()[1 + first].name(), b = values()[1 + second].name();
            for (Slant s : values()) if (s.name().equals(a + "_" + b) || s.name().equals(b + "_" + a)) return s;
            return values()[1 + first];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Slant> SLANT = EnumProperty.create("slant", Slant.class);

    public SilkVeilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any().setValue(THICK, false).setValue(SLANT, Slant.NONE);
        for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    public MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN, THICK, SLANT);
    }

    /** The gauze with a cord out through the faces in {@code arms} (bit {@link Direction#get3DDataValue}) and across the edges in {@code edges}, heavy or not. */
    public BlockState with(int arms, int edges, boolean thick) {
        BlockState s = defaultBlockState().setValue(THICK, thick).setValue(SLANT, Slant.of(edges));
        for (Direction d : Direction.values()) s = s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), (arms >> d.get3DDataValue() & 1) != 0);
        return s;
    }
}
