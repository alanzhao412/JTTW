package com.jttw.engine.progression.stats;

import com.jttw.engine.progression.level.GrowthRate;
import com.jttw.engine.reincarnation.ReincarnationOutcome;

import javax.annotation.Nullable;

/**
 * The four mythological class archetypes.
 *
 * <p>Each class defines:
 * <ul>
 *   <li>{@link #bst} — Base Stat Total.  The ceiling of raw power, distributed across HP/ATK/DEF/SPD.</li>
 *   <li>{@link #growthRate} — The XP-to-level curve.  Shapes how hard each level is to reach.</li>
 * </ul>
 *
 * <p>The actual stat values at any level are computed by {@link StatBlock#compute(ProgressionClass, int)}.
 */
public enum ProgressionClass {

    /**
     * Highest BST ceiling (1200).  Punishing early game; dramatically outscales all
     * other classes after level 50.  Form shifts at major milestones.
     * Designed as a long-term investment.
     */
    PRIMORDIAL(1200, GrowthRate.ERRATIC),

    /**
     * Naturally strong (BST 1100).  Stable, consistent scaling curve.
     * +16% stat bonus at each evolutionary form phase (20/40/60/80/100).
     * Four elemental lines: Water→Dragon King, Earth→World Turtle,
     * Beast→Byakko, Fire→Solar Sovereign.
     */
    DIVINE_BEAST(1100, GrowthRate.MEDIUM_FAST),

    /**
     * Widest utility toolkit (BST 1000, variable effective power).
     * Ability-expansion focused.  Some archetypes are combat-oriented;
     * higher-tier forms gain perception and authority advantages.
     */
    HEAVENLY(1000, GrowthRate.MEDIUM),

    /**
     * Ability-focused (BST 1030).  Lower raw stat floor.
     * Strong transformation mechanics.  Power comes from abilities
     * rather than raw stat bonuses.
     */
    YAOGUAI(1030, GrowthRate.FLUCTUATING);

    /** Sum of all base stats.  Feeds {@link BaseStats} distribution. */
    public final int bst;

    /** XP growth curve that determines leveling speed. */
    public final GrowthRate growthRate;

    ProgressionClass(int bst, GrowthRate growthRate) {
        this.bst = bst;
        this.growthRate = growthRate;
    }

    // -----------------------------------------------------------------------
    // Mapping from reincarnation system
    // -----------------------------------------------------------------------

    /** Derive the class from a reincarnation outcome. */
    public static ProgressionClass fromOutcome(ReincarnationOutcome outcome) {
        return switch (outcome) {
            case CELESTIAL_PRIMATE,
                 YUAN_DI,
                 GOLD_WINGED_PENG                         -> PRIMORDIAL;

            case EYE_OF_HEAVEN,
                 THIRD_LOTUS_PRINCE,
                 MARSHAL_HEAVENLY_CANOPY                  -> HEAVENLY;

            case PHOENIX,
                 DRAGON,
                 WHITE_TIGER,
                 TURTLE                                   -> DIVINE_BEAST;

            case BULL_DEMON_KING,
                 SAURIAN_DEMON_KING                       -> YAOGUAI;
        };
    }

    /** Parse from a saved NBT string.  Returns null if unrecognised. */
    @Nullable
    public static ProgressionClass parse(String name) {
        if (name == null || name.isBlank()) return null;
        for (ProgressionClass c : values()) {
            if (c.name().equalsIgnoreCase(name.trim())) return c;
        }
        return null;
    }
}