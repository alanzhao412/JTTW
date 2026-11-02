package com.jttw.engine.progression.stats;

import com.jttw.engine.item.ArmamentTiers;
import com.jttw.engine.item.Armory;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import org.junit.jupiter.api.Test;

import static com.jttw.engine.reincarnation.ReincarnationOutcome.*;
import static org.junit.jupiter.api.Assertions.*;

/** The class identities' balance: each class's kit on top of its stats, and where that leaves them overall. */
class ClassKitTest {

    private static final ReincarnationOutcome[] PRIMORDIALS = {CELESTIAL_PRIMATE, YUAN_DI, GOLD_WINGED_PENG};

    @Test
    void yaoguaiAbilitiesHitFarHarderThanTheirWeapons() {
        assertTrue(AbilityDamage.YAOGUAI_WEAPON < 1, "a weapon hit modest");
        assertTrue(AbilityDamage.YAOGUAI_ABILITY > 1, "an ability hit harder");
        assertTrue(AbilityDamage.YAOGUAI_ABILITY / AbilityDamage.YAOGUAI_WEAPON >= 2.4, "an ability hit at least 2.4 weapon hits");
        assertEquals(AbilityDamage.YAOGUAI_ABILITY, AbilityDamage.abilityMultiplier(ProgressionClass.YAOGUAI, BULL_DEMON_KING));
        assertEquals(AbilityDamage.YAOGUAI_WEAPON, AbilityDamage.weaponMultiplier(ProgressionClass.YAOGUAI, SAURIAN_DEMON_KING));
        assertEquals(1, AbilityDamage.abilityMultiplier(ProgressionClass.PRIMORDIAL, CELESTIAL_PRIMATE));
        assertEquals(1, AbilityDamage.weaponMultiplier(ProgressionClass.HEAVENLY, THIRD_LOTUS_PRINCE));
    }

    @Test
    void yaoguaiOverallPowerStaysAboutWhereItWasWithMoreOfItInAbilities() {
        for (ReincarnationOutcome y : new ReincarnationOutcome[]{BULL_DEMON_KING, SAURIAN_DEMON_KING, GRAND_SAINT_NINE_SPIRITS}) {
            for (int lv = 1; lv <= 100; lv++) {
                double kit = ClassKit.kit(y, lv);
                assertTrue(kit >= 1.0 && kit <= 1.15, y + " at " + lv + ": casting, within one and a half power levels of before: " + kit);
            }
        }
        // only weapons: they fall well behind; the right play is to cast
        assertTrue(AbilityDamage.YAOGUAI_WEAPON < 0.7);
    }

    @Test
    void thePhoenixAbilitiesHitHarderToo() {
        assertEquals(AbilityDamage.PHOENIX_ABILITY, AbilityDamage.abilityMultiplier(ProgressionClass.DIVINE_BEAST, PHOENIX));
        assertEquals(1, AbilityDamage.weaponMultiplier(ProgressionClass.DIVINE_BEAST, PHOENIX), "its weapon hits as they were");
        assertEquals(1, AbilityDamage.abilityMultiplier(ProgressionClass.DIVINE_BEAST, DRAGON));
        assertTrue(ClassKit.kit(PHOENIX, 50) > 1 && ClassKit.kit(PHOENIX, 50) < 1.2);
    }

    @Test
    void primordialsAndBeastsAreTheirStats() {
        for (ReincarnationOutcome o : new ReincarnationOutcome[]{CELESTIAL_PRIMATE, YUAN_DI, GOLD_WINGED_PENG, DRAGON, WHITE_TIGER, TURTLE}) {
            assertEquals(1, ClassKit.kit(o, 60), 1e-9, o + ": normal gameplay");
        }
    }

    @Test
    void nezhasKitIsTheStrongestOfTheHeavenlyAndErlangsStrongerThanTheMarshals() {
        for (int lv = 1; lv <= 100; lv++) {
            double nezha = ClassKit.kit(THIRD_LOTUS_PRINCE, lv), erlang = ClassKit.kit(EYE_OF_HEAVEN, lv), marshal = ClassKit.kit(MARSHAL_HEAVENLY_CANOPY, lv);
            assertTrue(nezha > erlang, "Nezha's kit over Erlang's at " + lv);
            assertTrue(erlang > 1 && marshal > 1, "every Heavenly's armaments add to them at " + lv);
        }
    }

    @Test
    void erlangOverallIsAtTheLowerEndOfThePrimordialsAtTheTop() {
        double erlang = ClassKit.overallLevels(EYE_OF_HEAVEN, 100);
        double peng = ClassKit.overallLevels(GOLD_WINGED_PENG, 100), yuanDi = ClassKit.overallLevels(YUAN_DI, 100),
                wukong = ClassKit.overallLevels(CELESTIAL_PRIMATE, 100);
        assertTrue(erlang >= peng, "Erlang at least the Peng: " + erlang + " vs " + peng);
        assertTrue(erlang <= yuanDi + 1.5, "about Yuan Di, no more: " + erlang + " vs " + yuanDi);
        assertTrue(erlang < wukong - 5, "far under Wukong: " + erlang + " vs " + wukong);
    }

    @Test
    void gearedHeavenlyLandAmongTheOtherClassesOverall() {
        for (int lv = 20; lv <= 100; lv += 5) {
            double least = Double.MAX_VALUE, most = -Double.MAX_VALUE;
            for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
                if (ProgressionClass.fromOutcome(o) == ProgressionClass.HEAVENLY) continue;
                least = Math.min(least, ClassKit.overallLevels(o, lv));
                most = Math.max(most, ClassKit.overallLevels(o, lv));
            }
            for (ReincarnationOutcome h : new ReincarnationOutcome[]{THIRD_LOTUS_PRINCE, EYE_OF_HEAVEN, MARSHAL_HEAVENLY_CANOPY}) {
                double at = ClassKit.overallLevels(h, lv);
                // the Marshal is the least of anyone at the top by design (his measure, 0.7): near the others, a little under
                double floor = h == MARSHAL_HEAVENLY_CANOPY ? least - 2 : least;
                assertTrue(at >= floor && at <= most, h + " at " + lv + " among the others (" + least + " to " + most + "): " + at);
            }
            // and the Marshal stays the least of the Heavenly
            assertTrue(ClassKit.overallLevels(MARSHAL_HEAVENLY_CANOPY, lv) < ClassKit.overallLevels(THIRD_LOTUS_PRINCE, lv));
        }
    }

    @Test
    void eachTierBehindCostsAHeavenlyAStep() {
        assertEquals(1, Armory.attunement(0), 1e-9);
        assertEquals(1 / 1.3, Armory.attunement(1), 1e-9);
        assertEquals(Math.pow(1.3, -3), Armory.attunement(3), 1e-9);
        assertEquals(Math.pow(1.3, -3), Armory.attunement(8), 1e-9, "no worse than three behind");
        // a same-level fight takes three or four hits; a tier behind, a hit more to win it
        assertTrue(Armory.attunement(1) * Armory.attunement(1) < 0.6, "health and attack both fall: a fight's worth of power ~ x0.59");
    }

    @Test
    void tiersLineUpWithTheRings() {
        int[] starts = {1, 20, 37, 47, 57, 67, 78, 89};
        for (int t = 1; t <= ArmamentTiers.TIERS; t++) {
            assertEquals(starts[t - 1], ArmamentTiers.ringStart(t));
            assertEquals(t, ArmamentTiers.ringTier(starts[t - 1]));
            if (t > 1) assertEquals(t - 1, ArmamentTiers.ringTier(starts[t - 1] - 1));
            assertTrue(ArmamentTiers.multiplier(t) > ArmamentTiers.multiplier(t - 1) || t == 1);
        }
        assertEquals(8, ArmamentTiers.ringTier(100));
        assertEquals(1.7, ArmamentTiers.multiplier(8), 1e-9);
    }

    @Test
    void theHeavenlyStatCurvesAreUntouched() {
        for (int lv = 1; lv <= 100; lv++) {
            assertEquals(LevelPower.of(ProgressionClass.HEAVENLY, null, lv), LevelPower.of(lv), 1e-9 * LevelPower.of(lv));
        }
    }

    @Test
    void primordialsHeadsStayPutOverall() {
        for (ReincarnationOutcome p : PRIMORDIALS) assertEquals(ClassKit.statLevels(p, 70), ClassKit.overallLevels(p, 70), 1e-9);
    }
}
