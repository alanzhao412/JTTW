package com.jttw.engine.world.spider;

import com.jttw.engine.entity.spider.SilkKin;
import com.jttw.engine.world.Stun;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

/**
 * A binding web (缚丝): the Seven Spider Spirits' silk, strung across the ridge's paths and the
 * cave's floors, or cast under their foes in a fight. Whatever walks into it (but the ridge's own) is
 * <b>pinned</b> ({@link Stun#pin}: held where it stands, though it may still strike what is in its
 * reach) for {@value #HOLD} ticks; then, struggling, it has {@value #GRACE} ticks to tear free
 * before the silk takes it again. Cut it (a sword or shears make short work of it) to be free.
 * As a cobweb, it slows what passes through it besides.
 *
 * <p>The webs the sisters cast in a fight are {@link #FRESH}: they fall apart by themselves after
 * {@value #FRESH_FOR} ticks; the ones strung over the ridge stay.
 */
public class BindingWebBlock extends WebBlock {

    public static final MapCodec<WebBlock> CODEC = simpleCodec(BindingWebBlock::new);
    public static final BooleanProperty FRESH = BooleanProperty.create("fresh");
    /** How long it holds, and how long one held by it then has to tear free before it holds again. */
    public static final int HOLD = 40, GRACE = 30;
    /** How long a web cast in a fight lasts. */
    public static final int FRESH_FOR = 160;
    /** Persistent data: the game time until which one just held may struggle free. */
    private static final String KEY_FREE = "jttw_web_free_until";

    public BindingWebBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FRESH, false));
    }

    @Override
    public MapCodec<WebBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FRESH);
    }

    /** Whether {@code e} walks through it unheld: the ridge's own, and those not in play. */
    public static boolean passes(Entity e) {
        return !(e instanceof LivingEntity) || e instanceof SilkKin || e instanceof Player p && (p.isCreative() || p.isSpectator());
    }

    /**
     * Binds {@code e} if it is free to be bound: pinned for {@value #HOLD} ticks, then free for
     * {@value #GRACE} more. Returns whether it was bound now.
     */
    public static boolean bind(LivingEntity e) {
        if (passes(e) || Stun.held(e)) return false;
        long now = e.level().getGameTime();
        if (now < e.getPersistentData().getLong(KEY_FREE)) return false;
        Stun.pin(e, HOLD, null);
        e.getPersistentData().putLong(KEY_FREE, now + HOLD + GRACE);
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1f, 0.6f);
        if (e.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WHITE_ASH, e.getX(), e.getY(0.5), e.getZ(), 30, 0.4, 0.6, 0.4, 0.01);
        }
        return true;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (passes(entity)) return; // the ridge's own run over their webs as over the ground
        entity.makeStuckInBlock(state, new Vec3(0.25, 0.05, 0.25));
        if (!level.isClientSide && entity instanceof LivingEntity e) bind(e);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (state.getValue(FRESH) && !level.isClientSide) level.scheduleTick(pos, this, FRESH_FOR);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(FRESH)) {
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
        }
    }
}
