package com.jttw.engine.world.tough;

import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The mod's tough blocks: the stone of White Bone Ridge, Heaven and Hell. Mined as the ordinary
 * stone each stands in for, but against destruction they hold as nothing in the world below does:
 * {@value #RESISTANCE} blast resistance (obsidian's is 1200), worn down by the strength of whoever
 * brings the destruction, by the root of their level power. So no blast or carving of a weak
 * character's (or a creeper's) touches them, a level-60 character's barely, a level-100 one's
 * much as it would ordinary stone.
 */
public final class Tough {

    private Tough() {}

    public static final float RESISTANCE = 1500;

    /** How far a breaker's strength wears tough blocks down: the root of their level power, at least 1. */
    public static double strength(@Nullable Entity by) {
        if (!(by instanceof Player p)) return 1;
        return Math.max(1, Math.sqrt(LevelPower.of(p)));
    }

    /** A tough block's blast resistance against a breaker of this strength. */
    public static float resistance(double strength) {
        return (float) (RESISTANCE / Math.max(1, strength));
    }

    /** Whether this block is one of the mod's tough ones. */
    public static boolean is(BlockState state) {
        return state.getBlock() instanceof Stone || state.getBlock() instanceof Pillar || state.getBlock() instanceof Loose;
    }

    private static float againstBlast(@Nullable Explosion explosion) {
        return resistance(strength(explosion == null ? null : explosion.getIndirectSourceEntity()));
    }

    /** Solid tough stone. */
    public static class Stone extends net.minecraft.world.level.block.Block {
        public Stone(Properties p) { super(p); }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, @Nullable Explosion explosion) {
            return againstBlast(explosion);
        }
    }

    /** Tough stone that runs one way: fused bone, veined marble. */
    public static class Pillar extends RotatedPillarBlock {
        public Pillar(Properties p) { super(p); }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, @Nullable Explosion explosion) {
            return againstBlast(explosion);
        }
    }

    /** Tough but loose: it falls as gravel and sand do. */
    public static class Loose extends FallingBlock {
        public Loose(Properties p) { super(p); }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, @Nullable Explosion explosion) {
            return againstBlast(explosion);
        }
    }
}
