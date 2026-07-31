package com.jttw.engine.progression;

/**
 * Defines the XP-to-level curve for each class archetype.
 *
 * <p>Each value defines {@link #totalXpForLevel(int)}: the total accumulated XP
 * required to REACH that level from level 1.
 *
 * <p>Approximate totals at key milestones (for reference when designing XP drops):
 * <pre>
 * Level   MEDIUM_FAST  MEDIUM       ERRATIC      FLUCTUATING
 *  10         200        240          400           220
 *  20       1,600      1,920        3,200         1,760
 *  50      25,000     30,000       50,000        27,500
 * 100     200,000    240,000      190,000       220,000
 * </pre>
 *
 * <p>Note that ERRATIC (Primordial) actually ends up costing LESS total XP by level 100
 * than MEDIUM_FAST — the price is paid on the front half.  The higher stat multipliers
 * in {@link StatBlock} are the true late-game payoff.
 */
public enum GrowthRate {

    /**
     * Divine Beast — stable, reliable, moderately fast.
     * Mirrors Pokémon's "Medium-Fast" curve: {@code n³ / 5}.
     */
    MEDIUM_FAST {
        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            return c * c * c / 5L;
        }
    },

    /**
     * Heavenly — ~20% slower than MEDIUM_FAST at every level.
     * Compensated by the widest ability toolkit in the game.
     * Formula: {@code ⌊1.2 × n³ / 5⌋}.
     */
    MEDIUM {
        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            return (long)(1.2 * c * c * c / 5.0);
        }
    },

    /**
     * Primordial — brutally expensive to level 50, then cheaper than MEDIUM_FAST.
     *
     * <p>Levels 1–50: 2× the cost of MEDIUM_FAST per level.
     * Levels 51–100: each new level costs ~80% of what MEDIUM_FAST would — the grind
     * eases once you've proven commitment.
     *
     * <p>This is the "high-risk, long-term investment" curve.  Players who push through
     * the painful early phase get dramatically accelerated late-game progression combined
     * with the steepest stat scaling in {@link StatBlock}.
     */
    ERRATIC {
        // Total XP accumulated at the crossover point (level 50)
        private static final long BASE_AT_50 = (long)(2.0 * 50L * 50L * 50L / 5.0); // 50_000

        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            if (c <= 50) {
                return (long)(2.0 * c * c * c / 5.0);
            } else {
                // After 50: delta costs 80% of MEDIUM_FAST's equivalent delta
                long mfDelta = c * c * c / 5L - 50L * 50L * 50L / 5L;
                return BASE_AT_50 + (long)(0.8 * mfDelta);
            }
        }
    },

    /**
     * Yaoguai — slightly above MEDIUM_FAST overall ({@code ⌊1.1 × n³ / 5⌋}).
     *
     * <p>The "spiky" feel comes from {@link StatBlock}'s +20% bonus per power spike
     * milestone (levels 30, 50, 70, 90), not from the XP curve itself.  The XP curve
     * is intentionally unremarkable so the spikes feel earned and explosive rather than
     * gated by an artificial grind wall.
     */
    FLUCTUATING {
        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            return (long)(1.1 * c * c * c / 5.0);
        }
    };

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Total XP that must have been accumulated to reach level {@code n}.
     * Level 1 = 0 XP.
     */
    public abstract long totalXpForLevel(int n);

    /**
     * XP required to advance from level {@code current} to {@code current + 1}.
     * Returns {@code Long.MAX_VALUE} at the level cap.
     */
    public long xpToNextLevel(int current) {
        if (current >= LevelConstants.MAX_LEVEL) return Long.MAX_VALUE;
        return totalXpForLevel(current + 1) - totalXpForLevel(current);
    }

    private static long cap(long n) {
        return Math.max(1L, Math.min(n, LevelConstants.MAX_LEVEL));
    }
}