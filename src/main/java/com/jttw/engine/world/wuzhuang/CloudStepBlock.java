package com.jttw.engine.world.wuzhuang;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A step of the cloud stair (云梯) up to Mount Wuzhuang: a half or a whole block of auspicious
 * cloud. The cloud bears up whoever walks it: stepping on it, a player is given Slow Falling for
 * {@value #BORNE} ticks, so one who slips off the stair drifts down to the ground unhurt.
 */
public class CloudStepBlock extends SlabBlock {

    /** How long the cloud bears a player up after they step on it (enough to drift from the island to the ground). */
    public static final int BORNE = 400;

    public CloudStepBlock(Properties p) {
        super(p);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof Player p) bear(p);
        super.stepOn(level, pos, state, entity);
    }

    /** The cloud bears {@code p} up: Slow Falling, renewed while they walk it. */
    public static void bear(Player p) {
        MobEffectInstance now = p.getEffect(MobEffects.SLOW_FALLING);
        if (now == null || now.getDuration() < BORNE - 60) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, BORNE, 0, false, false, true));
    }
}
