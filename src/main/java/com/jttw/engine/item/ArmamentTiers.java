package com.jttw.engine.item;

/**
 * The armaments' tiers, I to VIII, one for each ring of levels, and what a tier is worth; without the
 * items, so the balance ({@link com.jttw.engine.progression.stats.ClassKit}) and the tests can read it.
 */
public final class ArmamentTiers {

    private ArmamentTiers() {}

    public static final int TIERS = 8;
    /** What each tier past I adds to an armament's own bonus, in percent of its tier-I bonus. */
    public static final int PER_TIER_PERCENT = 10;
    /** The first level of each ring, tier I's to tier VIII's: the level an armament of that tier is made for. */
    static final int[] RING_STARTS = {1, 20, 37, 47, 57, 67, 78, 89};

    /** What an armament's own bonus is multiplied by at a tier: x1 at I, x1.7 at VIII. */
    public static double multiplier(int tier) {
        return 1 + (Math.max(1, Math.min(TIERS, tier)) - 1) * PER_TIER_PERCENT / 100.0;
    }

    /** The tier made for this level: its ring's (I below 20, II at 20 to 36, ... VIII from 89). */
    public static int ringTier(int level) {
        int t = 1;
        for (int i = 0; i < RING_STARTS.length; i++) if (level >= RING_STARTS[i]) t = i + 1;
        return t;
    }

    /** The first level of a tier's ring. */
    public static int ringStart(int tier) {
        return RING_STARTS[Math.max(1, Math.min(TIERS, tier)) - 1];
    }

    public static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
            default -> String.valueOf(tier);
        };
    }

    /** "levels 37-46": the ring a tier is made for. */
    public static String ring(int tier) {
        int t = Math.max(1, Math.min(TIERS, tier));
        int from = RING_STARTS[t - 1], to = t == TIERS ? 100 : RING_STARTS[t] - 1;
        return "levels " + from + "-" + to;
    }
}
