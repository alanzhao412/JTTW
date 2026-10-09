package com.jttw.engine.progression.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class StatBlockTest {

    private static final double EPS = 1e-9;

    /** A class's health shape: its health at level 50 by the Pokémon-derived formula. */
    private static double hpShape(int baseHp) {
        return 20.0 + (2.0 * baseHp * 50 / 100 + 50 + 10) * 0.435;
    }

    /** A class's attack shape: its attack at level 50 by the Pokémon-derived formula. */
    private static double atkShape(int baseStr) {
        return 1.0 + (2.0 * baseStr * 50 / 100 + 5) * 0.216;
    }

    private static final double HP_NORM = 20.0 / hpShape(170); // Heavenly, the frailest, has 20 at level 1

    @Test
    void heavenlyAtLevelOneMatchesTheFormulaByHand() {
        StatBlock s = StatBlock.compute(ProgressionClass.HEAVENLY, 1);
        // health and attack: the class's shape at power 1
        assertEquals(20.0, s.maxHealth(), EPS);
        assertEquals(atkShape(200) * 0.13, s.attackDamage(), EPS);
        // STR, base 200: raw = 4 + 5 = 9 -> 1 + 9 * 0.216
        assertEquals(1.0 + 9 * 0.216, s.strength(), EPS);
        // base PRES 50: raw = 1 + 5 = 6 -> 6 * 0.60; base MRES 80: raw = 1.6 + 5 = 6.6 -> 6.6 * 0.60
        assertEquals(6 * 0.60, s.physicalResistance(), EPS);
        assertEquals(6.6 * 0.60, s.magicResistance(), EPS);
        // base QI 270: raw = 5.4 + 5 = 10.4 -> 10.4 * 18.35
        assertEquals(10.4 * 18.35, s.qi(), EPS);
        // base WILL 230: raw = 4.6 + 5 = 9.6 -> 9.6 * 0.10
        assertEquals(9.6 * 0.10, s.will(), EPS);
    }

    @Test
    void yaoguaiAtLevelHundredMatchesTheFormulaByHand() {
        // Yaoguai rides the backbone, 1.1 a level
        StatBlock s = StatBlock.compute(ProgressionClass.YAOGUAI, 100);
        double power = Math.pow(1.10, 99);
        assertEquals(hpShape(230) * HP_NORM * power, s.maxHealth(), s.maxHealth() * EPS);
        assertEquals(atkShape(220) * 0.13 * power, s.attackDamage(), s.attackDamage() * EPS);
        // STR keeps the formula: base 220, raw = 440 + 5
        assertEquals(1.0 + 445 * 0.216, s.strength(), EPS);
    }

    @Test
    void healthAndAttackFollowTheCharactersOwnCurve() {
        var wukong = com.jttw.engine.reincarnation.ReincarnationOutcome.CELESTIAL_PRIMATE;
        StatBlock his = StatBlock.compute(ProgressionClass.PRIMORDIAL, wukong, 100);
        StatBlock theClass = StatBlock.compute(ProgressionClass.PRIMORDIAL, 100);
        assertEquals(LevelPower.characterFactor(wukong), his.maxHealth() / theClass.maxHealth(), 1e-9);
        assertEquals(LevelPower.characterFactor(wukong), his.attackDamage() / theClass.attackDamage(), 1e-9);
        assertEquals(theClass.physicalResistance(), his.physicalResistance(), EPS, "his measure is power, not resistance");
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void resistanceStopsGrowingAtLevelForty(ProgressionClass cls) {
        StatBlock at40 = StatBlock.compute(cls, 40);
        for (int level = 41; level <= 100; level++) {
            assertEquals(at40.physicalResistance(), StatBlock.compute(cls, level).physicalResistance(), EPS, cls + " pres at " + level);
            assertEquals(at40.magicResistance(), StatBlock.compute(cls, level).magicResistance(), EPS, cls + " mres at " + level);
        }
    }

    @Test
    void divineBeastResistanceGainsSixteenPercentPerEvolutionToForty() {
        // raw MRES from base 90
        double rawMres = 2.0 * 90 * 19 / 100 + 5;
        assertEquals(rawMres * 0.60 * 1.0, StatBlock.compute(ProgressionClass.DIVINE_BEAST, 19).magicResistance(), EPS);
        rawMres = 2.0 * 90 * 20 / 100 + 5;
        assertEquals(rawMres * 0.60 * 1.16, StatBlock.compute(ProgressionClass.DIVINE_BEAST, 20).magicResistance(), EPS);
        rawMres = 2.0 * 90 * 40 / 100 + 5;
        assertEquals(rawMres * 0.60 * 1.32, StatBlock.compute(ProgressionClass.DIVINE_BEAST, 100).magicResistance(), EPS);
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
            if (level < 40) { // resistance grows to 40, then holds
                assertTrue(b.physicalResistance() > a.physicalResistance(), cls + " pres at " + level);
                assertTrue(b.magicResistance() > a.magicResistance(), cls + " mres at " + level);
            }
            assertTrue(b.strength() > a.strength(), cls + " STR at " + level);
            assertTrue(b.qi() > a.qi(), cls + " qi at " + level);
            assertTrue(b.will() > a.will(), cls + " will at " + level);
        }
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void neverBelowVanillaBaselines(ProgressionClass cls) {
        StatBlock s = StatBlock.compute(cls, 1);
        assertTrue(s.maxHealth() >= 20.0);
        assertTrue(s.attackDamage() > 1.0);
        assertTrue(s.physicalResistance() > 0.0);
        assertTrue(s.magicResistance() > 0.0);
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
