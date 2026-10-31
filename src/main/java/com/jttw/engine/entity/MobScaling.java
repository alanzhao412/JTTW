package com.jttw.engine.entity;

import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;

import java.util.Arrays;

/**
 * How strong the mod's mobs are at a level, measured against a typical character of that level
 * (the median of the four classes): its health, and its ordinary hit (a weapon or a basic
 * ability, about 1.5x its attack). A mob is then set in those terms: "takes eight hits", "a blow
 * of half a character's health". So mobs follow the campaign's power curve wherever it is tuned.
 */
public final class MobScaling {

    private MobScaling() {}

    /** An ordinary hit, as a share of attack: a weapon or a basic ability. */
    public static final double ORDINARY_HIT = 1.5;

    /** A typical character's health at this level. */
    public static double typicalHealth(int level) {
        return median(level, StatBlock::maxHealth);
    }

    /** A typical character's ordinary hit at this level. */
    public static double typicalHit(int level) {
        return ORDINARY_HIT * median(level, StatBlock::attackDamage);
    }

    /**
     * Past the players' cap (level {@value com.jttw.engine.progression.level.LevelConstants#MAX_LEVEL})
     * no character's stats grow; a creature set above it (the Six-Eared Macaque, a level-120 superboss)
     * is measured against a character extrapolated past the cap, each level worth this much more than
     * the one before: gentler than the campaign's backbone ({@code LevelPower.GROWTH}, 1.10), so that
     * twenty levels over the cap are about 1.8x a level-100 character, not 6.7x (which no group of
     * capped players could ever outlast).
     */
    public static final double PAST_CAP_GROWTH = 1.03;

    private static double median(int level, java.util.function.ToDoubleFunction<StatBlock> stat) {
        int cap = com.jttw.engine.progression.level.LevelConstants.MAX_LEVEL;
        if (level > cap) return median(cap, stat) * Math.pow(PAST_CAP_GROWTH, level - cap);
        double[] v = Arrays.stream(ProgressionClass.values()).mapToDouble(c -> stat.applyAsDouble(StatBlock.compute(c, level))).sorted().toArray();
        return v.length % 2 == 1 ? v[v.length / 2] : (v[v.length / 2 - 1] + v[v.length / 2]) / 2;
    }
}
