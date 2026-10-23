package com.jttw.engine.world.waters;

import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.item.WieldRules;
import com.jttw.engine.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The pillar in the Eastern Sea's treasury that once fixed the depth of the Milky Way (定海神针),
 * a column of dark iron banded in gold that glows: the Dragon King Ao Guang showed it to the Monkey
 * King, who lifted it, bade it shrink, and carried it off as the Ruyi Jingu Bang. Nothing can break
 * it; it moves for one who could wield it (Sun Wukong, or anyone of {@value WieldRules#RUYI_STR}
 * STR): taken in hand, the whole pillar shrinks into the staff, and is gone from the treasury. So
 * each Eastern palace gives one staff, once.
 */
public class SeaCalmingPillarBlock extends Block {

    public SeaCalmingPillarBlock(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} can lift it. */
    public static boolean canLift(Player player) {
        return WukongCharacter.is(player) || WieldRules.strength(player) >= WieldRules.RUYI_STR;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        take(level, pos, player);
        return InteractionResult.CONSUME;
    }

    /**
     * {@code player} lays hands on the pillar at {@code pos}: if they can lift it, the whole of it
     * shrinks into the staff, which is theirs. Returns whether it did.
     */
    public static boolean take(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof SeaCalmingPillarBlock)) return false;
        if (!canLift(player)) {
            player.displayClientMessage(Component.translatable("block.jttw_engine.sea_calming_pillar.immovable"), true);
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.5f);
            return false;
        }
        Block self = level.getBlockState(pos).getBlock();
        BlockPos.MutableBlockPos p = pos.mutable();
        while (level.getBlockState(p.below()).is(self)) p.move(0, -1, 0);
        int removed = 0;
        for (; level.getBlockState(p).is(self); p.move(0, 1, 0)) {
            boolean wet = isWet(level, p);
            level.setBlock(p, (wet ? Blocks.WATER : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL);
            if (level instanceof ServerLevel server) server.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.2, 0.4, 0.2, 0.02);
            removed++;
        }
        if (removed == 0) return false;
        ItemStack staff = new ItemStack(ModItems.RUYI_JINGU_BANG.get());
        if (!player.getInventory().add(staff)) player.drop(staff, false);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 0.6f);
        player.displayClientMessage(Component.translatable("block.jttw_engine.sea_calming_pillar.taken"), false);
        return true;
    }

    private static boolean isWet(Level level, BlockPos pos) {
        for (var d : net.minecraft.core.Direction.values()) {
            if (level.getFluidState(pos.relative(d)).is(net.minecraft.tags.FluidTags.WATER)) return true;
        }
        return false;
    }
}
