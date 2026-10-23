package com.jttw.engine.world.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * True Samadhi Fire (三昧真火) on the ground, where Red Boy's breath fell: it burns whatever walks
 * into it with the Samadhi fire ({@link SamadhiFire#ignite}) and burns out by itself after a few
 * seconds ({@value #LIFE} to {@value #LIFE}+{@value #LIFE_SPREAD} ticks). Nothing else puts it out:
 * water will not flow into it nor be poured on it (in the novel the Dragon Kings' rain only fed
 * it), a splash of water does not douse it (it is not in the fire tag), a hand cannot beat it
 * out, and the Banana-Leaf Fan passes over it.
 */
public class SamadhiFlameBlock extends Block implements LiquidBlockContainer {

    public static final int LIFE = 100, LIFE_SPREAD = 60;
    /** How long walking into it sets one burning, in seconds. */
    public static final int BURN_SECONDS = 4;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public SamadhiFlameBlock(Properties p) {
        super(p);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return canSurvive(state, level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!old.is(this)) level.scheduleTick(pos, this, LIFE + level.random.nextInt(LIFE_SPREAD + 1));
    }

    /** It burns out by itself, and only so. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !SamadhiFire.spared(living)) {
            SamadhiFire.ignite(living, BURN_SECONDS, 0);
        }
    }

    /** Nothing is put in its place: not a block, not water. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    /** Water flowing or poured: it does not take. */
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1f + random.nextFloat(), random.nextFloat() * 0.5f + 0.4f, false);
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + random.nextDouble(), pos.getY() + 0.2 + random.nextDouble() * 0.6,
                    pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1, pos.getZ() + random.nextDouble(), 0, 0.05, 0);
        }
    }
}
