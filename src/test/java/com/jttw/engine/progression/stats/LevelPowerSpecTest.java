package com.jttw.engine.progression.stats;

import com.jttw.engine.reincarnation.ReincarnationOutcome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The campaign's scaling, as designed: what one character can do to another across levels, the
 * shape of each class's growth, and who stands where at the top. An "ordinary hit" is a weapon or
 * a basic ability, 1.5x attack, taken through the defender's physical resistance.
 */
class LevelPowerSpecTest {

    private static final ReincarnationOutcome[] ALL = ReincarnationOutcome.values();

    private static StatBlock stats(ReincarnationOutcome who, int level) {
        return StatBlock.compute(ProgressionClass.fromOutcome(who), who, level);
    }

    /** Ordinary hits an attacker at one level needs to kill a defender at another. */
    private static double hits(ReincarnationOutcome attacker, int attackerLevel, ReincarnationOutcome defender, int defenderLevel) {
        StatBlock a = stats(attacker, attackerLevel), d = stats(defender, defenderLevel);
        return d.maxHealth() / ResistanceHandler.damageAfter(1.5 * a.attackDamage(), d.physicalResistance());
    }

    private static double[] everyPairing(int attackerLevel, int defenderLevel) {
        List<Double> all = new ArrayList<>();
        for (ReincarnationOutcome a : ALL) for (ReincarnationOutcome d : ALL) all.add(hits(a, attackerLevel, d, defenderLevel));
        return all.stream().mapToDouble(Double::doubleValue).sorted().toArray();
    }

    private static double median(double[] sorted) {
        return sorted[sorted.length / 2];
    }

    // ── The campaign: twenty levels up one-shots ─────────────────────────

    @Test
    void twentyLevelsUpOneShotsATypicalCharacter() {
        int[][] gaps = {{20, 5}, {40, 20}, {60, 40}, {80, 60}, {100, 80}};
        for (int[] gap : gaps) {
            double[] h = everyPairing(gap[0], gap[1]);
            long oneShots = Arrays.stream(h).filter(x -> x <= 1).count();
            String at = "level " + gap[0] + " against " + gap[1];
            assertTrue(median(h) <= 1, at + ": the typical pairing is one hit: " + median(h));
            assertTrue(oneShots >= h.length * 3 / 4, at + ": three in four pairings or more are one hit: " + oneShots + "/" + h.length);
            assertTrue(h[h.length - 1] <= 2, at + ": and none takes more than two: " + h[h.length - 1]);
        }
    }

    @Test
    void sameLevelFightsTakeThreeToFiveHitsAtEveryLevel() {
        for (int level = 20; level <= 100; level += 10) {
            double m = median(everyPairing(level, level));
            assertTrue(m >= 3 && m <= 5, "level " + level + ": " + m);
        }
    }

    @Test
    void levelOneIsVanillaSized() {
        for (ProgressionClass cls : ProgressionClass.values()) assertEquals(1.0, LevelPower.of(cls, 1), 1e-9, cls.name());
        assertEquals(20.0, StatBlock.compute(ProgressionClass.HEAVENLY, 1).maxHealth(), 1e-6, "the frailest class has a vanilla player's 20");
    }

    // ── Each class's shape ───────────────────────────────────────────────

    @Test
    void yaoguaiAndHeavenlyGrowSteadilyOnTheBackbone() {
        for (int level = 1; level <= 100; level++) {
            assertEquals(LevelPower.of(level), LevelPower.of(ProgressionClass.YAOGUAI, level), LevelPower.of(level) * 1e-9);
            assertEquals(LevelPower.of(level), LevelPower.of(ProgressionClass.HEAVENLY, level), LevelPower.of(level) * 1e-9);
        }
    }

    @Test
    void primordialStartsSlowAndGrowsEverFaster() {
        for (int level : new int[]{10, 20, 30, 40}) {
            assertTrue(LevelPower.of(ProgressionClass.PRIMORDIAL, level) < LevelPower.of(level), "behind the backbone at " + level);
        }
        assertTrue(LevelPower.of(ProgressionClass.PRIMORDIAL, 100) > 2 * LevelPower.of(100), "far past it at 100");
        double before = 0;
        for (int level = 2; level <= 100; level++) {
            double growth = LevelPower.of(ProgressionClass.PRIMORDIAL, level) / LevelPower.of(ProgressionClass.PRIMORDIAL, level - 1);
            assertTrue(growth > before, "each level's growth beats the last's, at " + level);
            before = growth;
        }
    }

    @Test
    void divineBeastsLeapAtEachEvolutionAndSlowBetween() {
        for (int evolution : new int[]{20, 40, 60, 80, 100}) {
            double leap = LevelPower.of(ProgressionClass.DIVINE_BEAST, evolution) / LevelPower.of(ProgressionClass.DIVINE_BEAST, evolution - 1);
            assertTrue(leap >= 2, "a leap at " + evolution + ": " + leap);
            assertEquals(LevelPower.of(evolution), LevelPower.of(ProgressionClass.DIVINE_BEAST, evolution), LevelPower.of(evolution) * 1e-6,
                    "each stage keeps pace with the backbone");
        }
        for (int start : new int[]{21, 41, 61, 81}) {
            double early = LevelPower.of(ProgressionClass.DIVINE_BEAST, start + 9) / LevelPower.of(ProgressionClass.DIVINE_BEAST, start);
            double late = LevelPower.of(ProgressionClass.DIVINE_BEAST, start + 18) / LevelPower.of(ProgressionClass.DIVINE_BEAST, start + 9);
            assertTrue(early > 1.3 * late, "growth slows within the stage from " + start + ": " + early + " then " + late);
        }
    }

    // ── The top ──────────────────────────────────────────────────────────

    /** Stats at the top, all in one: the geometric mean of health and attack. */
    private static double statIndex(ReincarnationOutcome who) {
        StatBlock s = stats(who, 100);
        return Math.sqrt(s.maxHealth() * s.attackDamage());
    }

    @Test
    void atTheTopWukongYuanDiPengAndTheLionStandFarAboveEveryoneByStats() {
        var top = List.of(ReincarnationOutcome.CELESTIAL_PRIMATE, ReincarnationOutcome.YUAN_DI,
                ReincarnationOutcome.GOLD_WINGED_PENG, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        for (int i = 1; i < top.size(); i++) {
            assertTrue(statIndex(top.get(i - 1)) >= statIndex(top.get(i)), top.get(i - 1) + " >= " + top.get(i));
        }
        double rest = Arrays.stream(ALL).filter(o -> !top.contains(o)).mapToDouble(LevelPowerSpecTest::statIndex).max().orElseThrow();
        assertTrue(statIndex(top.get(3)) >= 2 * rest, "the lion is far above everyone else: " + statIndex(top.get(3)) / rest + "x");
    }

    @Test
    void marshalHasTheLeastStatsAtTheTop() {
        double marshal = statIndex(ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        for (ReincarnationOutcome o : ALL) {
            if (o != ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY) assertTrue(statIndex(o) > marshal, o + " over Marshal");
        }
    }

    @Test
    void numbersStayWithinTheRaisedCaps() {
        for (ReincarnationOutcome o : ALL) {
            StatBlock s = stats(o, 100);
            assertTrue(s.maxHealth() < 1e7 && s.attackDamage() < 1e7, o + ": " + s);
        }
        assertTrue(Map.of().isEmpty());
    }
}
