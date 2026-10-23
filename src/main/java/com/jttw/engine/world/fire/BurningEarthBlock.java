package com.jttw.engine.world.fire;

import com.jttw.engine.world.tough.Tough;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The burning ground of the Flaming Mountains (火焰山): cracked red earth with the fire of the
 * fallen furnace still in it. Lit, it is the zone's heat ({@link FireMountain#scorch}): whatever
 * stands on it without Fire Resistance is scorched each second and set alight; fire on it never
 * burns out (it is a fire source, as netherrack is), and now and then flames creep across it
 * ({@link #randomTick}). Princess Iron Fan's Banana-Leaf Fan puts it out ({@link #quench}) for
 * {@value #QUENCHED_FOR} ticks, then it kindles again by itself.
 */
public class BurningEarthBlock extends Tough.Stone {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** How long fanned earth stays out, in ticks: a minute. */
    public static final int QUENCHED_FOR = 1200;
    /** How many fires there may be on the earth round it (5x5) before it lights another. */
    public static final int CROWD = 5;

    public BurningEarthBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    public static boolean isLit(BlockState state) {
        return state.getBlock() instanceof BurningEarthBlock && state.getValue(LIT);
    }

    /** Lit, it scorches whatever stands on it (once a second, by the world's clock). */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && state.getValue(LIT) && entity instanceof LivingEntity living
                && (level.getGameTime() + entity.getId()) % FireMountain.HEAT_EVERY == 0) {
            FireMountain.scorch(living);
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Lit, the fire on it never dies. */
    @Override
    public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return direction == Direction.UP && state.getValue(LIT);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    /** Flames creep over lit earth: now and then one kindles on it, unless the ground round it burns enough already. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) kindle(level, pos);
    }

    /** Lights a fire on top of it, if there is room and the earth round it is not crowded with fire. Returns whether it did. */
    public static boolean kindle(Level level, BlockPos pos) {
        BlockPos up = pos.above();
        if (!isLit(level.getBlockState(pos)) || !level.isEmptyBlock(up)) return false;
        int fires = 0;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (level.getBlockState(up.offset(dx, 0, dz)).is(net.minecraft.tags.BlockTags.FIRE) && ++fires >= CROWD) return false;
        }
        level.setBlock(up, BaseFireBlock.getState(level, up), Block.UPDATE_ALL);
        return true;
    }

    /** Puts it out (and the fire on it), for {@value #QUENCHED_FOR} ticks. Returns whether it was lit. */
    public static boolean quench(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isLit(state)) return false;
        level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        if (level.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.FIRE)) level.removeBlock(pos.above(), false);
        level.scheduleTick(pos, state.getBlock(), QUENCHED_FOR);
        return true;
    }

    /** The fanned earth kindles again. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) return;
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 0.7f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || !level.getBlockState(pos.above()).isAir()) return;
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(), 0, 0.04, 0);
        }
        if (random.nextInt(120) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0, 0);
        }
    }
}
