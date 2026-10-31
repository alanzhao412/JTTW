package com.jttw.engine.character.wukong;

import com.jttw.engine.world.tough.Tough;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The Fighting Buddha's destruction: whatever his abilities break (the Ruyi Jingu Bang's blast,
 * its growing and its fall, the Qi Wave, the Fiery Golden Eyes' laser) ignores hardness and every
 * protection. Bedrock, obsidian, reinforced deepslate, the mod's tough stone and its unbreakable
 * lair and boss blocks (the Bottomless Cave's rock, Lion-Camel's fabric, the gates) all break; so
 * do the technical blocks (command, structure and jigsaw blocks, barriers, light blocks, end
 * portals and gateways) and the world's floor of bedrock: there are no exceptions.
 *
 * <p>The carving budgets stay, but no block costs him more than stone does
 * ({@link com.jttw.engine.world.tough.Carving#cost}, given {@link #STRENGTH}); no block resists a
 * blast of his more than stone ({@link #CALCULATOR}); and the terrain breaker, given his
 * work, breaks what would hold against anyone else ({@link com.jttw.engine.world.TerrainBreaker}).
 */
public final class BuddhaBreaking {

    private BuddhaBreaking() {}

    /** His strength against tough blocks, and the mark that a carving is his: without bound. */
    public static final double STRENGTH = Double.POSITIVE_INFINITY;

    /** Whether destruction brought by {@code by} breaks anything: his. */
    public static boolean unstoppable(@Nullable Entity by) {
        return FightingBuddha.is(by);
    }

    /** The strength {@code by} carves with: {@link #STRENGTH} for him, else as {@link Tough#strength}. */
    public static double strength(@Nullable Entity by) {
        return unstoppable(by) ? STRENGTH : Tough.strength(by);
    }

    /** Whether a carving of this strength is his. */
    public static boolean unstoppable(double strength) {
        return Double.isInfinite(strength);
    }

    /** Stone's blast resistance: the most any block resists a blast of his. */
    public static final float STONE_RESISTANCE = 6.0f;

    /**
     * A blast of his: no block resists it more than stone does (bedrock, obsidian, the unbreakable
     * alike), so it carves the crater it would in stone; and every block in its reach may go.
     */
    public static final ExplosionDamageCalculator CALCULATOR = new ExplosionDamageCalculator() {
        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos,
                                                           BlockState state, FluidState fluid) {
            if (state.isAir() && fluid.isEmpty()) return Optional.empty();
            float own = Math.max(state.getExplosionResistance(level, pos, explosion), fluid.getExplosionResistance(level, pos, explosion));
            return Optional.of(Math.min(own, STONE_RESISTANCE));
        }

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
            return true;
        }
    };
}
