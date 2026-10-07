package com.jttw.engine.progression.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class StatBlockTest {

    private static final double EPS = 1e-9;

    @Test
    void heavenlyAtLevelOneMatchesTheFormulaByHand() {
        // base HP 170: raw = 2*170*1/100 + 1 + 10 = 14.4 -> 20 + 14.4 * 0.145
        StatBlock s = StatBlock.compute(ProgressionClass.HEAVENLY, 1);
        assertEquals(20.0 + 14.4 * 0.145, s.maxHealth(), EPS);
        // base STR 200: raw = 4 + 5 = 9 -> 1 + 9 * 0.072
        assertEquals(1.0 + 9 * 0.072, s.attackDamage(), EPS);
        // base DEF 130: raw = 2.6 + 5 = 7.6 -> 7.6 * 0.045
        assertEquals(7.6 * 0.045, s.armor(), EPS);
        // base QI 270: raw = 5.4 + 5 = 10.4 -> 10.4 * 18.35
        assertEquals(10.4 * 18.35, s.qi(), EPS);
        // base WILL 230: raw = 4.6 + 5 = 9.6 -> 9.6 * 0.10
        assertEquals(9.6 * 0.10, s.will(), EPS);
    }

    @Test
    void yaoguaiAtLevelHundredMatchesTheFormulaByHand() {
        // No class bonus for Yaoguai. base HP 230: raw = 460 + 100 + 10 = 570
        StatBlock s = StatBlock.compute(ProgressionClass.YAOGUAI, 100);
        assertEquals(20.0 + 570 * 0.145, s.maxHealth(), EPS);
        // base STR 220: raw = 440 + 5
        assertEquals(1.0 + 445 * 0.072, s.attackDamage(), EPS);
    }

    @Test
    void primordialGainsOnePointFivePercentPerLevelAboveFifty() {
        StatBlock at50 = StatBlock.compute(ProgressionClass.PRIMORDIAL, 50);
        StatBlock at100 = StatBlock.compute(ProgressionClass.PRIMORDIAL, 100);
        // armor has no base term, so it scales exactly with the bonus: raw DEF 220
        double rawDef50 = 2.0 * 220 * 50 / 100 + 5;
        double rawDef100 = 2.0 * 220 * 100 / 100 + 5;
        assertEquals(rawDef50 * 0.045 * 1.0, at50.armor(), EPS);
        assertEquals(rawDef100 * 0.045 * 1.75, at100.armor(), EPS);
    }

    @Test
    void divineBeastGainsSixteenPercentPerSpikeReached() {
        double rawDef = 2.0 * 230 * 19 / 100 + 5;
        assertEquals(rawDef * 0.045 * 1.0, StatBlock.compute(ProgressionClass.DIVINE_BEAST, 19).armor(), EPS);
        rawDef = 2.0 * 230 * 20 / 100 + 5;
        assertEquals(rawDef * 0.045 * 1.16, StatBlock.compute(ProgressionClass.DIVINE_BEAST, 20).armor(), EPS);
        rawDef = 2.0 * 230 * 100 / 100 + 5;
        assertEquals(rawDef * 0.045 * 1.80, StatBlock.compute(ProgressionClass.DIVINE_BEAST, 100).armor(), EPS);
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void levelsAreClampedToOneThroughHundred(ProgressionClass cls) {
        assertEquals(StatBlock.compute(cls, 1), StatBlock.compute(cls, 0));
        assertEquals(StatBlock.compute(cls, 1), StatBlock.compute(cls, -5));
        assertEquals(StatBlock.compute(cls, 100), StatBlock.compute(cls, 250));
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void everyStatGrowsWithLevel(ProgressionClass cls) {
        for (int level = 1; level < 100; level++) {
            StatBlock a = StatBlock.compute(cls, level);
            StatBlock b = StatBlock.compute(cls, level + 1);
            assertTrue(b.maxHealth() > a.maxHealth(), cls + " hp at " + level);
            assertTrue(b.attackDamage() > a.attackDamage(), cls + " str at " + level);
            assertTrue(b.armor() > a.armor(), cls + " def at " + level);
            assertTrue(b.qi() > a.qi(), cls + " qi at " + level);
            assertTrue(b.will() > a.will(), cls + " will at " + level);
        }
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void neverBelowVanillaBaselines(ProgressionClass cls) {
        StatBlock s = StatBlock.compute(cls, 1);
        assertTrue(s.maxHealth() > 20.0);
        assertTrue(s.attackDamage() > 1.0);
        assertTrue(s.armor() > 0.0);
    }

    @Test
    void primordialOutscalesEveryClassAtLevelHundred() {
        StatBlock primordial = StatBlock.compute(ProgressionClass.PRIMORDIAL, 100);
        for (ProgressionClass other : ProgressionClass.values()) {
            if (other == ProgressionClass.PRIMORDIAL) continue;
            StatBlock s = StatBlock.compute(other, 100);
            assertTrue(primordial.maxHealth() > s.maxHealth(), "hp vs " + other);
            assertTrue(primordial.attackDamage() > s.attackDamage(), "str vs " + other);
        }
    }
}
