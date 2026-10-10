package com.jttw.engine.world.furnace;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The great furnace, to be set down: it needs three by three by five clear, its core where it is placed. */
public class TrigramsFurnaceItem extends BlockItem {

    public TrigramsFurnaceItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) return false;
        boolean room = TrigramsFurnaceBlock.hasRoom(context.getLevel(), context.getClickedPos(),
                p -> context.getLevel().getBlockState(p).canBeReplaced(context));
        if (!room && context.getPlayer() != null && !context.getLevel().isClientSide) {
            context.getPlayer().displayClientMessage(Component.literal("§cThe furnace needs 3 x 3 x 5 clear."), true);
        }
        return room;
    }
}
