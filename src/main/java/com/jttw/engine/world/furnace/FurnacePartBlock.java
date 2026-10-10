package com.jttw.engine.world.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The rest of the great Eight Trigrams Furnace round its core: three by three, five high. Each part
 * knows where it stands in the furnace (and so where the core is); the furnace is drawn whole by the
 * core, so the parts draw nothing themselves.
 *
 * <p>The middle column, above the core, is the furnace's inside: hollow, where the fire is. The part
 * at its top is the hatch: use it to open or close it, and with it open, whatever stands on it drops
 * into the fire. Using any other part is using the furnace.
 */
public class FurnacePartBlock extends Block {

    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 4);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    /** The hatch's thin lid, closed. */
    private static final VoxelShape LID = Block.box(0, 12, 0, 16, 16, 16);

    public FurnacePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 0).setValue(DY, 0).setValue(DZ, 0).setValue(LIT, false).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ, LIT, OPEN);
    }

    /** The part at this offset from the core (dx, dz in -1..1; dy in 0..4). */
    public BlockState at(int dx, int dy, int dz) {
        return defaultBlockState().setValue(DX, dx + 1).setValue(DY, dy).setValue(DZ, dz + 1);
    }

    public static BlockPos corePos(BlockState state, BlockPos pos) {
        return pos.offset(-(state.getValue(DX) - 1), -state.getValue(DY), -(state.getValue(DZ) - 1));
    }

    public static boolean isInside(BlockState state) {
        return state.getValue(DX) == 1 && state.getValue(DZ) == 1 && state.getValue(DY) >= 1 && state.getValue(DY) <= 3;
    }

    public static boolean isHatch(BlockState state) {
        return state.getValue(DX) == 1 && state.getValue(DZ) == 1 && state.getValue(DY) == 4;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isInside(state)) return Shapes.empty();
        if (isHatch(state)) return state.getValue(OPEN) ? Shapes.empty() : LID;
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // the core draws the furnace whole
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos core = corePos(state, pos);
        if (!(level.getBlockEntity(core) instanceof TrigramsFurnaceBlockEntity furnace)) return InteractionResult.PASS;
        if (isHatch(state)) {
            if (furnace.sealedFromInside(player)) {
                player.displayClientMessage(Component.literal("§cThe hatch is sealed by the fire."), true);
                return InteractionResult.CONSUME;
            }
            furnace.setHatch(!state.getValue(OPEN));
            level.playSound(null, pos, state.getValue(OPEN) ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 1f, 0.6f);
            return InteractionResult.CONSUME;
        }
        return level.getBlockState(core).use(level, player, hand, hit.withPosition(core));
    }

    /** Breaking any part breaks the whole furnace. */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos core = corePos(state, pos);
            if (level.getBlockState(core).getBlock() instanceof TrigramsFurnaceBlock) level.destroyBlock(core, true);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(com.jttw.engine.registry.ModItems.EIGHT_TRIGRAMS_FURNACE_ITEM.get());
    }
}
