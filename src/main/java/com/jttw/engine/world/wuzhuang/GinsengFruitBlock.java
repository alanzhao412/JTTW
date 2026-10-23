package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A ginseng fruit (人参果) hanging on its tree: a blossom (age 0), a green fruit (1), then ripe (2),
 * a fruit shaped like a newborn child. It ripens slowly (the novel's tree fruits once in nine
 * thousand years; here each stage is one random tick in {@value #RIPEN_ONE_IN}, about an hour of
 * play). It "fears the five elements", and so:
 * <ul>
 *   <li>struck with <b>gold</b> (a golden tool, or the temple's Golden Striker, 金击子) a ripe fruit
 *       falls whole: it must be <b>caught</b> in something lined (a Silk-Lined Tray held under it),
 *       or land on something soft and lined (wool, a carpet); touching the bare earth it <b>sinks into
 *       the ground</b> and is gone; in water it <b>dissolves</b> ({@link Wuzhuang#fruitTick});</li>
 *   <li>struck with anything else (iron, wood, a bare hand) it falls and <b>withers</b> at once;</li>
 *   <li>an unripe one only withers.</li>
 * </ul>
 * Bringing a ripe fruit down without Zhenyuan's leave is theft ({@link Wuzhuang#isGuest}).
 */
public class GinsengFruitBlock extends Block {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    public static final int RIPE = 2, RIPEN_ONE_IN = 60;
    private static final VoxelShape SHAPE = Block.box(5, 2, 5, 11, 16, 11);

    /** What became of a fruit struck down. */
    public enum Outcome { UNRIPE_WITHERED, KNOCKED_DOWN, WITHERED }

    public GinsengFruitBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < RIPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(RIPEN_ONE_IN) == 0) level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
    }

    /** Whether a tool is gold: a golden tool or weapon, or the Golden Striker. */
    public static boolean isGold(ItemStack tool) {
        return tool.getItem() instanceof TieredItem t && t.getTier() == Tiers.GOLD;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.isCreative()) knock(server, pos, state, player, player.getMainHandItem());
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Strikes the fruit at {@code pos} with {@code tool}: what becomes of it (the block is gone after). */
    public static Outcome knock(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player by, ItemStack tool) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        boolean ripe = state.hasProperty(AGE) && state.getValue(AGE) >= RIPE;
        if (!ripe) {
            wither(level, pos);
            return Outcome.UNRIPE_WITHERED;
        }
        if (by instanceof ServerPlayer sp && !sp.isCreative() && !Wuzhuang.isGuest(sp)) Wuzhuang.stole(sp);
        if (!isGold(tool)) {
            wither(level, pos);
            if (by != null) by.displayClientMessage(Component.translatable("block.jttw_engine.hanging_ginseng_fruit.withered"), true);
            return Outcome.WITHERED;
        }
        ItemEntity fruit = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, new ItemStack(ModItems.GINSENG_FRUIT.get()));
        fruit.setDeltaMovement(0, -0.05, 0);
        fruit.setNeverPickUp();
        fruit.getPersistentData().putBoolean(Wuzhuang.KEY_KNOCKED, true);
        level.addFreshEntity(fruit);
        level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 1.8f);
        if (by != null) by.displayClientMessage(Component.translatable("block.jttw_engine.hanging_ginseng_fruit.falls"), true);
        return Outcome.KNOCKED_DOWN;
    }

    private static void wither(ServerLevel level, BlockPos pos) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 16, 0.2, 0.3, 0.2, 0.05);
        level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
    }
}
