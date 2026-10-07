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
            "MEDIUM_FAST, 10,    200", "MEDIUM, 10,    240", "ERRATIC, 10,    400", "FLUCTUATING, 10,    220",
            "MEDIUM_FAST, 20,   1600", "MEDIUM, 20,   1920", "ERRATIC, 20,   3200", "FLUCTUATING, 20,   1760",
            "MEDIUM_FAST, 50,  25000", "MEDIUM, 50,  30000", "ERRATIC, 50,  50000", "FLUCTUATING, 50,  27500",
            "MEDIUM_FAST, 100, 200000", "MEDIUM, 100, 240000", "ERRATIC, 100, 190000", "FLUCTUATING, 100, 220000",
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
    void erraticCostsDoubleMediumFastUpToFifty() {
        for (int level = 1; level <= 50; level++) {
            long mf = GrowthRate.MEDIUM_FAST.totalXpForLevel(level);
            long erratic = GrowthRate.ERRATIC.totalXpForLevel(level);
            // 2 * n^3 / 5 vs n^3 / 5, each floored separately
            assertTrue(Math.abs(erratic - 2 * mf) <= 1, "level " + level);
        }
    }

    @Test
    void erraticLevelsAreCheaperThanMediumFastAfterFifty() {
        for (int level = 51; level < 100; level++) {
            assertTrue(GrowthRate.ERRATIC.xpToNextLevel(level) < GrowthRate.MEDIUM_FAST.xpToNextLevel(level),
                    "level " + level);
        }
    }

    @Test
    void erraticEndsCheaperOverallThanMediumFast() {
        // The design note: Primordial pays up front and finishes with less total XP
        assertTrue(GrowthRate.ERRATIC.totalXpForLevel(100) < GrowthRate.MEDIUM_FAST.totalXpForLevel(100));
    }
}
