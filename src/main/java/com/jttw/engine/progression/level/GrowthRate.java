package com.jttw.engine.progression.level;

/**
 * Defines the XP-to-level curve for each class archetype.
 *
 * <p>Each value defines {@link #totalXpForLevel(int)}: the total accumulated XP
 * required to REACH that level from level 1.
 *
 * <p>Approximate totals at key milestones (for reference when designing XP drops):
 * <pre>
 * Level   FAST (Heavenly)  MEDIUM_FAST (Divine Beast)  MEDIUM (Yaoguai)  STEEPENING (Primordial)
 *  10            160                 200                    250                    330
 *  20          1,280               1,600                  2,000                  2,880
 *  50         20,000              25,000                 31,250                 56,250
 * 100        160,000             200,000                250,000                600,000
 * </pre>
 */
public enum GrowthRate {

    /**
     * Heavenly — the quickest to level: 80% of MEDIUM_FAST at every level.
     * Formula: {@code ⌊0.8 × n³ / 5⌋}.
     */
    FAST {
        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            return (long) (0.8 * c * c * c / 5.0);
        }
    },

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
     * Yaoguai — a little slower than MEDIUM_FAST: 1.25× at every level.
     * Formula: {@code ⌊1.25 × n³ / 5⌋}.
     */
    MEDIUM {
        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            return (long) (1.25 * c * c * c / 5.0);
        }
    },

    /**
     * Primordial — ever harder: each level costs more than MEDIUM_FAST's by a margin that keeps
     * widening, from 1.5× at the start to 3× at the end. No late-game relief: the long-term
     * investment is paid all the way, and repaid by the steepest stat scaling in {@link
     * com.jttw.engine.progression.stats.StatBlock}.
     * Formula: {@code ⌊1.5 × n³ / 5 × (1 + n / 100)⌋}.
     */
    STEEPENING {
        @Override
        public long totalXpForLevel(int n) {
            long c = cap(n);
            return (long) (1.5 * c * c * c / 5.0 * (1.0 + c / 100.0));
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
