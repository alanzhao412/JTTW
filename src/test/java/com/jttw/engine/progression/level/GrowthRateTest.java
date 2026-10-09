package com.jttw.engine.progression.level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class GrowthRateTest {

    /** The reference table in GrowthRate's javadoc, used to design XP drops. */
    @ParameterizedTest
    @CsvSource({
            "FAST, 10,    160", "MEDIUM_FAST, 10,    200", "MEDIUM, 10,    250", "STEEPENING, 10,    330",
            "FAST, 20,   1280", "MEDIUM_FAST, 20,   1600", "MEDIUM, 20,   2000", "STEEPENING, 20,   2880",
            "FAST, 50,  20000", "MEDIUM_FAST, 50,  25000", "MEDIUM, 50,  31250", "STEEPENING, 50,  56250",
            "FAST, 100, 160000", "MEDIUM_FAST, 100, 200000", "MEDIUM, 100, 250000", "STEEPENING, 100, 600000",
    })
    void matchesDocumentedTotals(GrowthRate rate, int level, long totalXp) {
        assertEquals(totalXp, rate.totalXpForLevel(level));
    }

    @ParameterizedTest
    @EnumSource(GrowthRate.class)
    void levelOneNeedsNoXp(GrowthRate rate) {
        assertEquals(0, rate.totalXpForLevel(1));
    }

    @ParameterizedTest
    @EnumSource(GrowthRate.class)
    void totalsNeverDecreaseAndEveryLevelCostsSomething(GrowthRate rate) {
        for (int level = 1; level < LevelConstants.MAX_LEVEL; level++) {
            long next = rate.xpToNextLevel(level);
            assertTrue(next > 0, rate + " level " + level + " -> next costs " + next);
            assertEquals(rate.totalXpForLevel(level + 1) - rate.totalXpForLevel(level), next);
        }
    }

    @ParameterizedTest
    @EnumSource(GrowthRate.class)
    void noFurtherLevelAtTheCap(GrowthRate rate) {
        assertEquals(Long.MAX_VALUE, rate.xpToNextLevel(LevelConstants.MAX_LEVEL));
        assertEquals(Long.MAX_VALUE, rate.xpToNextLevel(LevelConstants.MAX_LEVEL + 5));
    }

    @ParameterizedTest
    @EnumSource(GrowthRate.class)
    void levelsOutsideOneToHundredAreClamped(GrowthRate rate) {
        assertEquals(rate.totalXpForLevel(1), rate.totalXpForLevel(0));
        assertEquals(rate.totalXpForLevel(1), rate.totalXpForLevel(-20));
        assertEquals(rate.totalXpForLevel(100), rate.totalXpForLevel(150));
    }

    @Test
    void fastIsTheQuickestAndSteepeningTheSlowestAtEveryLevel() {
        for (int level = 5; level < 100; level++) {
            long fast = GrowthRate.FAST.xpToNextLevel(level);
            long mf = GrowthRate.MEDIUM_FAST.xpToNextLevel(level);
            long medium = GrowthRate.MEDIUM.xpToNextLevel(level);
            long steep = GrowthRate.STEEPENING.xpToNextLevel(level);
            assertTrue(fast < mf && mf < medium && medium < steep,
                    "level " + level + ": " + fast + " " + mf + " " + medium + " " + steep);
        }
    }

    @Test
    void steepeningGetsHarderRelativeToTheOthersWithEveryLevel() {
        // Primordial's design: no late relief; the gap to MEDIUM_FAST only widens
        // (ten levels apart: whole-number XP makes neighbouring levels wobble by a hair)
        double last = 0;
        for (int level = 10; level < 100; level += 10) {
            double ratio = (double) GrowthRate.STEEPENING.xpToNextLevel(level) / GrowthRate.MEDIUM_FAST.xpToNextLevel(level);
            assertTrue(ratio > last, "level " + level + ": " + ratio + " after " + last);
            last = ratio;
        }
        assertTrue(last > 2.9, "near 3x by the end: " + last);
    }
}
