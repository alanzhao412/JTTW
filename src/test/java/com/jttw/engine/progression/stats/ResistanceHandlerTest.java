package com.jttw.engine.progression.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResistanceHandlerTest {

    private static final double EPS = 1e-9;

    @Test
    void noResistanceTakesFullDamage() {
        assertEquals(1.0, ResistanceHandler.damageMultiplier(0), EPS);
    }

    @Test
    void reductionFollowsHundredOverHundredPlusResistance() {
        assertEquals(2.0 / 3.0, ResistanceHandler.damageMultiplier(50), EPS);
        assertEquals(0.5, ResistanceHandler.damageMultiplier(100), EPS);
        assertEquals(0.25, ResistanceHandler.damageMultiplier(300), EPS);
    }

    @Test
    void negativeResistanceNeverAmplifiesDamage() {
        assertEquals(1.0, ResistanceHandler.damageMultiplier(-40), EPS);
    }

    /** Uncapped, but each point is worth less than the one before. */
    @Test
    void uncappedWithDiminishingReturns() {
        double prevGain = Double.MAX_VALUE;
        for (int res = 0; res < 2000; res += 50) {
            double gain = ResistanceHandler.damageMultiplier(res) - ResistanceHandler.damageMultiplier(res + 50);
            assertTrue(gain > 0, "more resistance always helps at " + res);
            assertTrue(gain < prevGain, "returns diminish at " + res);
            prevGain = gain;
        }
        assertTrue(ResistanceHandler.damageMultiplier(1_000_000) > 0, "damage never reaches zero");
    }

    @Test
    void primordialAtLevelHundredBlocksAboutTwoThirdsOfBlows() {
        StatBlock s = StatBlock.compute(ProgressionClass.PRIMORDIAL, 100);
        double taken = ResistanceHandler.damageMultiplier(s.physicalResistance());
        assertTrue(taken > 0.3 && taken < 0.4, "takes " + taken);
    }
}
