package com.jttw.engine.world.heaven;

import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Heaven's and Hell's blocks for Sun Wukong's road to immortality. */
public final class HeavenBlocks {

    private HeavenBlocks() {}

    /** An Immortal Peach hanging in the Peach Garden's trees: break it to pick it. */
    public static class PeachFruit extends Block {
        private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 14, 12);

        public PeachFruit(Properties p) {
            super(p);
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /** A jar of Heavenly Wine from the Jade Palace's banquet: three cups in it, poured for whoever uses it. */
    public static class WineJar extends Block {
        public static final IntegerProperty CUPS = IntegerProperty.create("cups", 0, 3);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

        public WineJar(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(CUPS, 3));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(CUPS);
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        @SuppressWarnings("deprecation")
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            int cups = state.getValue(CUPS);
            if (cups == 0) {
                player.displayClientMessage(Component.literal("§7The jar is dry."), true);
                return InteractionResult.CONSUME;
            }
            level.setBlock(pos, state.setValue(CUPS, cups - 1), Block.UPDATE_ALL);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp && !sp.isCreative()) {
                com.jttw.engine.world.morality.Morality.deed(sp, com.jttw.engine.world.morality.Morality.STEAL_WINE, "you stole the Jade Emperor's wine");
            }
            ItemStack wine = new ItemStack(ModItems.HEAVENLY_WINE.get());
            if (!player.getInventory().add(wine)) player.drop(wine, false);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 0.8f);
            return InteractionResult.CONSUME;
        }
    }

    /**
     * The Book of Life and Death, in Hell's Hall of Judgement: every mortal's name and the day they
     * die. Sun Wukong strikes his out, and the underworld's claim on him with it.
     */
    public static class BookOfLifeAndDeath extends Block {
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

        public BookOfLifeAndDeath(Properties p) {
            super(p);
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        @SuppressWarnings("deprecation")
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!WukongCharacter.is(player)) {
                player.displayClientMessage(Component.literal("§7Your name is written here, and the day you will die."), true);
                return InteractionResult.CONSUME;
            }
            if (WukongCheckpoints.isComplete(player, WukongCheckpoints.Checkpoint.HELLS_BOOK)) {
                player.displayClientMessage(Component.literal("§7Your name is already struck out."), true);
                return InteractionResult.CONSUME;
            }
            WukongCheckpoints.complete(player, WukongCheckpoints.Checkpoint.HELLS_BOOK);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.jttw.engine.world.morality.Morality.deed(sp, com.jttw.engine.world.morality.Morality.STRIKE_THE_BOOK, "you defied the judges of the dead");
            }
            player.sendSystemMessage(Component.literal("§6You strike your name from the Book of Life and Death, and the names of all your monkeys. Death has no more claim on you."));
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.5f, 0.6f);
            return InteractionResult.CONSUME;
        }
    }
}
