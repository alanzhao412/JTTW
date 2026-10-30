package com.jttw.engine.entity;

import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;

import java.util.Arrays;

/**
 * How strong the mod's mobs are at a level, measured against a typical character of that level
 * (the median of the four classes): its health, and its ordinary hit (a weapon or a basic
 * ability, about 1.5x its attack). A mob is then set in those terms: "takes eight hits", "a blow
 * of half a character's health". So mobs follow the campaign's power curve wherever it is tuned;
 * past the players' level cap (bosses only), the curve goes on growing as it grew to the cap.
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

    /** Over how many levels below the cap the growth is measured that carries on past it. */
    public static final int GROWTH_SPAN = 10;

    /**
     * A typical character's stat at {@code level}: up to the players' cap, the median of the four
     * classes; past it (where only bosses stand), the cap's, growing on at the rate it grew over the
     * last {@value #GROWTH_SPAN} levels to it.
     */
    private static double median(int level, java.util.function.ToDoubleFunction<StatBlock> stat) {
        int cap = com.jttw.engine.progression.level.LevelConstants.MAX_LEVEL;
        if (level <= cap) return medianWithin(level, stat);
        double top = medianWithin(cap, stat), growth = top / medianWithin(cap - GROWTH_SPAN, stat);
        return top * Math.pow(growth, (level - cap) / (double) GROWTH_SPAN);
    }

    private static double medianWithin(int level, java.util.function.ToDoubleFunction<StatBlock> stat) {
        double[] v = Arrays.stream(ProgressionClass.values()).mapToDouble(c -> stat.applyAsDouble(StatBlock.compute(c, level))).sorted().toArray();
        return v.length % 2 == 1 ? v[v.length / 2] : (v[v.length / 2 - 1] + v[v.length / 2]) / 2;
    }
}
