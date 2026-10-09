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

    @Test
    void noResistanceLeavesTheHitAlone() {
        assertEquals(10.0, ResistanceHandler.damageAfter(10, 0), EPS);
    }

    /** First a flat tenth of the resistance comes off the hit, then the percentage. */
    @Test
    void flatBlockThenPercentage() {
        // 30 - 10 = 20, x 100/200
        assertEquals(10.0, ResistanceHandler.damageAfter(30, 100), EPS);
        // 10 - 5 = 5, x 100/150
        assertEquals(10.0 / 3.0, ResistanceHandler.damageAfter(10, 50), EPS);
    }

    /** A strong character shrugs off a zombie, but never fully: 5% of the hit always lands. */
    @Test
    void smallHitsKeepOnlyTheFloor() {
        assertEquals(0.2, ResistanceHandler.damageAfter(4, 100), EPS);
        assertEquals(0.05, ResistanceHandler.damageAfter(1, 1000), EPS);
    }

    @Test
    void moreResistanceNeverHurtsMore() {
        for (double hit : new double[]{1, 4, 10, 30, 100}) {
            double prev = Double.MAX_VALUE;
            for (int res = 0; res <= 1000; res += 10) {
                double now = ResistanceHandler.damageAfter(hit, res);
                assertTrue(now <= prev + EPS, "hit " + hit + " at " + res);
                prev = now;
            }
        }
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
    void primordialAtLevelHundredBlocksAboutThreeQuartersOfBlows() {
        StatBlock s = StatBlock.compute(ProgressionClass.PRIMORDIAL, 100);
        double taken = ResistanceHandler.damageMultiplier(s.physicalResistance());
        assertTrue(taken > 0.2 && taken < 0.3, "takes " + taken);
    }
}
