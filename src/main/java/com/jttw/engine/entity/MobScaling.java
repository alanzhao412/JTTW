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

    private static double median(int level, java.util.function.ToDoubleFunction<StatBlock> stat) {
        double[] v = Arrays.stream(ProgressionClass.values()).mapToDouble(c -> stat.applyAsDouble(StatBlock.compute(c, level))).sorted().toArray();
        return v.length % 2 == 1 ? v[v.length / 2] : (v[v.length / 2 - 1] + v[v.length / 2]) / 2;
    }
}
