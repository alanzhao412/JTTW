package com.jttw.engine.item;

/**
 * Each Heavenly armament's own bonus at tier I, as a share added to its holder's attack (or the
 * worn ones' speed and resistance): raised by {@link Armaments#PER_TIER_PERCENT}% of itself each tier
 * (x1.7 at VIII). Kept apart, without the items, so the balance ({@link com.jttw.engine.progression.stats.ClassKit})
 * reads the very numbers the items use.
 */
public final class ArmamentBonuses {

    private ArmamentBonuses() {}

    /** Nezha's Fire-Tip Spear: +attack in hand; lit (sneak + use, drawing Qi), more. */
    public static final double FIRE_TIP_SPEAR = 0.75, FIRE_TIP_SPEAR_LIT = 1.25;
    /** Each of Nezha's Yin and Yang swords, in either hand (a pair, so the two multiply). */
    public static final double YIN_YANG_SWORD = 0.40;
    /** Nezha's Qiankun Ring, in hand. */
    public static final double QIANKUN_RING = 0.25;
    /** Erlang Shen's Three-Pointed Spear, in hand. */
    public static final double THREE_POINTED_SPEAR = 0.10;
    /** Erlang Shen's axe, in hand; grown large (drawing Qi), far more and slower. */
    public static final double ERLANG_AXE = 0.15, ERLANG_AXE_LARGE = 2.0;
    /** The Marshal's Nine-Tooth Rake, in hand. */
    public static final double NINE_TOOTH_RAKE = 0.60;
}
