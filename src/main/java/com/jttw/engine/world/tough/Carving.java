package com.jttw.engine.world.tough;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What it costs the destruction powers to carve a block, the one measure the staff, the Qi Wave,
 * the thrown spear and the axe all spend their budgets by: the greater of how hard it is to mine
 * and a quarter of how well it stands a blast. Earth and stone cost little (dirt 0.5, stone 1.5),
 * obsidian 300, a tough block 375 to the weak and as little as stone to the mightiest; what cannot
 * be broken (bedrock) can never be paid for.
 */
public final class Carving {

    private Carving() {}

    /** The cost of carving {@code state}, for a breaker of {@code strength} (see Tough#strength); infinite if it cannot be. */
    public static double cost(Level level, BlockPos pos, BlockState state, double strength) {
        if (state.isAir()) return 0;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return Double.POSITIVE_INFINITY; // bedrock, barriers: never
        float resistance = Tough.is(state) ? Tough.resistance(strength) : state.getBlock().getExplosionResistance();
        return Math.max(hardness, resistance / 4.0);
    }
}
