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
        return pairings(attackerLevel, defenderLevel, false);
    }

    /** Every pairing's hits, sorted; without Wukong attacking if {@code butWukong} (the late bloomer, far behind until his surge). */
    private static double[] pairings(int attackerLevel, int defenderLevel, boolean butWukong) {
        List<Double> all = new ArrayList<>();
        for (ReincarnationOutcome a : ALL) for (ReincarnationOutcome d : ALL) {
            if (butWukong && a == ReincarnationOutcome.CELESTIAL_PRIMATE) continue;
            all.add(hits(a, attackerLevel, d, defenderLevel));
        }
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
            assertTrue(oneShots >= h.length * 2 / 3, at + ": two in three pairings or more are one hit: " + oneShots + "/" + h.length);
            // the classes' shapes part them (a weak Primordial against a Divine Beast fresh from evolving)
            double[] others = pairings(gap[0], gap[1], true);
            assertTrue(others[others.length - 1] <= 4.5, at + ": and none but Wukong takes more than four or five: " + others[others.length - 1]);
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
    void heavenlyGrowsSteadilyOnTheBackbone() {
        for (int level = 1; level <= 100; level++) {
            assertEquals(LevelPower.of(level), LevelPower.of(ProgressionClass.HEAVENLY, level), LevelPower.of(level) * 1e-9);
        }
    }

    /** A class's power at a level, in backbone levels: the backbone level as strong. */
    private static double powerLevel(ProgressionClass cls, int level) {
        return 1 + Math.log(LevelPower.of(cls, level)) / Math.log(LevelPower.GROWTH);
    }

    @Test
    void yaoguaiGrowLogarithmicallyQuickEarlyAndFlatteningLate() {
        for (int level : new int[]{20, 30, 40}) {
            double ahead = powerLevel(ProgressionClass.YAOGUAI, level) - level;
            assertTrue(ahead >= 7 && ahead <= 10, "some eight levels ahead at " + level + ": " + ahead);
        }
        assertEquals(100 - LevelPower.YAOGUAI_BEHIND, powerLevel(ProgressionClass.YAOGUAI, 100), 1e-6, "behind the backbone at the top");
        double before = Double.MAX_VALUE;
        for (int level = 2; level <= 100; level++) {
            double growth = LevelPower.of(ProgressionClass.YAOGUAI, level) / LevelPower.of(ProgressionClass.YAOGUAI, level - 1);
            assertTrue(growth < before && growth > 1, "each level's growth less than the last's, at " + level);
            before = growth;
        }
    }

    @Test
    void pengLeadsTheEarlyClimbYuanDiTheMiddleAndWukongTheEnd() {
        var peng = ReincarnationOutcome.GOLD_WINGED_PENG;
        var yuanDi = ReincarnationOutcome.YUAN_DI;
        var wukong = ReincarnationOutcome.CELESTIAL_PRIMATE;
        java.util.function.BiFunction<ReincarnationOutcome, Integer, Double> p = (who, lv) -> LevelPower.of(ProgressionClass.PRIMORDIAL, who, lv);
        for (int lv = 10; lv <= 40; lv++) {
            assertTrue(p.apply(peng, lv) > p.apply(yuanDi, lv) && p.apply(yuanDi, lv) > p.apply(wukong, lv), "Peng, then Yuan Di, then Wukong at " + lv);
        }
        for (int lv = 46; lv <= 80; lv++) {
            assertTrue(p.apply(yuanDi, lv) > p.apply(peng, lv) && p.apply(peng, lv) > p.apply(wukong, lv), "Yuan Di, then Peng, then Wukong at " + lv);
        }
        for (int lv = 92; lv <= 100; lv++) {
            assertTrue(p.apply(wukong, lv) > p.apply(yuanDi, lv) && p.apply(yuanDi, lv) > p.apply(peng, lv), "Wukong, then Yuan Di, then Peng at " + lv);
        }
        for (var who : ReincarnationOutcome.values()) {
            assertEquals(LevelPower.characterFactor(who), LevelPower.measure(who, 100), 1e-9, who + " reaches its measure at the top");
            assertEquals(1, LevelPower.measure(who, 1), 1e-9, who + " starts at its class");
        }
    }

    /** A character's stats, in one number: the geometric mean of health and attack. */
    private static double statIndex(ReincarnationOutcome who, int level) {
        StatBlock s = stats(who, level);
        return Math.sqrt(s.maxHealth() * s.attackDamage());
    }

    @Test
    void wukongIsTheWeakestOfAllForLongAndFarTheStrongestAtTheTop() {
        var wukong = ReincarnationOutcome.CELESTIAL_PRIMATE;
        for (int lv = 10; lv <= 80; lv += 5) {
            for (ReincarnationOutcome other : ALL) {
                if (other != wukong) assertTrue(statIndex(wukong, lv) < statIndex(other, lv), "weaker than " + other + " at " + lv);
            }
        }
        double next = Arrays.stream(ALL).filter(o -> o != wukong).mapToDouble(o -> statIndex(o, 100)).max().orElseThrow();
        assertTrue(statIndex(wukong, 100) > 2.5 * next, "far past everyone at 100: " + statIndex(wukong, 100) / next + "x");
    }

    @Test
    void erlangGrowsAtTheLowerEndOfThePrimordialsAndOverNezhaByStats() {
        var erlang = ReincarnationOutcome.EYE_OF_HEAVEN;
        for (int lv = 2; lv <= 100; lv++) {
            assertTrue(statIndex(erlang, lv) > statIndex(ReincarnationOutcome.THIRD_LOTUS_PRINCE, lv), "over Nezha at " + lv);
        }
        for (int lv = 80; lv <= 100; lv += 5) { // where the Primordials have pulled ahead of a Heavenly
            double e = statIndex(erlang, lv);
            assertTrue(e < statIndex(ReincarnationOutcome.GOLD_WINGED_PENG, lv) && e < statIndex(ReincarnationOutcome.YUAN_DI, lv), "under Peng and Yuan Di at " + lv);
        }
        double gap = Math.log(statIndex(ReincarnationOutcome.GOLD_WINGED_PENG, 100) / statIndex(erlang, 100)) / Math.log(LevelPower.GROWTH);
        assertTrue(gap < 3, "close under the Primordials at the top: " + gap + " levels");
    }

    @Test
    void atTheTopTheClassesStandPrimordialDivineBeastHeavenlyYaoguai() {
        var order = List.of(ProgressionClass.PRIMORDIAL, ProgressionClass.DIVINE_BEAST, ProgressionClass.HEAVENLY, ProgressionClass.YAOGUAI);
        for (int i = 1; i < order.size(); i++) {
            StatBlock a = StatBlock.compute(order.get(i - 1), 100), b = StatBlock.compute(order.get(i), 100);
            assertTrue(Math.sqrt(a.maxHealth() * a.attackDamage()) > Math.sqrt(b.maxHealth() * b.attackDamage()), order.get(i - 1) + " over " + order.get(i));
        }
    }

    @Test
    void primordialStartsSlowAndGrowsEverFaster() {
        for (int level : new int[]{10, 20, 30, 40, 50, 60, 70, 80}) {
            assertTrue(LevelPower.of(ProgressionClass.PRIMORDIAL, level) < LevelPower.of(level), "behind the backbone at " + level);
        }
        for (int level : new int[]{30, 40, 50, 60}) {
            double behind = level - powerLevel(ProgressionClass.PRIMORDIAL, level);
            assertTrue(behind >= 6, "weak for long: well behind at " + level + ": " + behind);
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
            assertEquals(LevelPower.DIVINE_LEAP, leap, 1e-6, "a leap at " + evolution);
            assertTrue(leap >= 3, "a drastic one: some thirteen levels' worth");
            assertEquals(evolution + (evolution == 100 ? 0 : LevelPower.DIVINE_AHEAD), powerLevel(ProgressionClass.DIVINE_BEAST, evolution), 1e-6,
                    "each evolution lands it ahead of the backbone, the last level with it");
            if (evolution < 100) assertTrue(powerLevel(ProgressionClass.DIVINE_BEAST, evolution + 19) < evolution + 19 - 5,
                    "and by the next it has fallen behind");
        }
        for (int start : new int[]{21, 41, 61, 81}) {
            double early = LevelPower.of(ProgressionClass.DIVINE_BEAST, start + 9) / LevelPower.of(ProgressionClass.DIVINE_BEAST, start);
            double late = LevelPower.of(ProgressionClass.DIVINE_BEAST, start + 18) / LevelPower.of(ProgressionClass.DIVINE_BEAST, start + 9);
            assertTrue(Math.log(early) > 1.5 * Math.log(late), "growth slows within the stage from " + start + ": " + early + " then " + late);
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
                ReincarnationOutcome.GOLD_WINGED_PENG, ReincarnationOutcome.EYE_OF_HEAVEN, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        for (int i = 1; i < top.size(); i++) {
            assertTrue(statIndex(top.get(i - 1)) >= statIndex(top.get(i)), top.get(i - 1) + " >= " + top.get(i));
        }
        double rest = Arrays.stream(ALL).filter(o -> !top.contains(o)).mapToDouble(LevelPowerSpecTest::statIndex).max().orElseThrow();
        assertTrue(statIndex(top.get(4)) >= 2 * rest, "the lion is far above everyone else: " + statIndex(top.get(4)) / rest + "x");
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
