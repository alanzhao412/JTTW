package com.jttw.engine.progression.stats;

import com.jttw.engine.progression.level.GrowthRate;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/** ProgressionClass, DivineBeastLine and FormStage: the enums that classify a player. */
class ClassesAndFormsTest {

    @Test
    void classesUseTheirDesignedGrowthCurves() {
        assertEquals(GrowthRate.ERRATIC, ProgressionClass.PRIMORDIAL.growthRate);
        assertEquals(GrowthRate.MEDIUM_FAST, ProgressionClass.DIVINE_BEAST.growthRate);
        assertEquals(GrowthRate.MEDIUM, ProgressionClass.HEAVENLY.growthRate);
        assertEquals(GrowthRate.FLUCTUATING, ProgressionClass.YAOGUAI.growthRate);
    }

    @ParameterizedTest
    @CsvSource({
            "CELESTIAL_PRIMATE, PRIMORDIAL", "YUAN_DI, PRIMORDIAL", "GOLD_WINGED_PENG, PRIMORDIAL",
            "EYE_OF_HEAVEN, HEAVENLY", "THIRD_LOTUS_PRINCE, HEAVENLY", "MARSHAL_HEAVENLY_CANOPY, HEAVENLY",
            "PHOENIX, DIVINE_BEAST", "DRAGON, DIVINE_BEAST", "WHITE_TIGER, DIVINE_BEAST", "TURTLE, DIVINE_BEAST",
            "BULL_DEMON_KING, YAOGUAI", "SAURIAN_DEMON_KING, YAOGUAI",
    })
    void everyOutcomeBelongsToItsClass(ReincarnationOutcome outcome, ProgressionClass cls) {
        assertEquals(cls, ProgressionClass.fromOutcome(outcome));
    }

    @Test
    void classParsingIsCaseAndSpaceInsensitive() {
        assertEquals(ProgressionClass.HEAVENLY, ProgressionClass.parse("HEAVENLY"));
        assertEquals(ProgressionClass.DIVINE_BEAST, ProgressionClass.parse("  divine_beast "));
        assertNull(ProgressionClass.parse(""));
        assertNull(ProgressionClass.parse("   "));
        assertNull(ProgressionClass.parse(null));
        assertNull(ProgressionClass.parse("wizard"));
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void classNamesRoundTripThroughParse(ProgressionClass cls) {
        assertEquals(cls, ProgressionClass.parse(cls.name()));
    }

    @ParameterizedTest
    @CsvSource({"DRAGON, WATER", "PHOENIX, FIRE", "WHITE_TIGER, BEAST", "TURTLE, EARTH"})
    void beastOutcomesMapToTheirLine(ReincarnationOutcome outcome, DivineBeastLine line) {
        assertEquals(line, DivineBeastLine.fromOutcome(outcome));
    }

    @ParameterizedTest
    @EnumSource(value = ReincarnationOutcome.class,
            names = {"DRAGON", "PHOENIX", "WHITE_TIGER", "TURTLE"}, mode = EnumSource.Mode.EXCLUDE)
    void otherOutcomesHaveNoBeastLine(ReincarnationOutcome outcome) {
        assertNull(DivineBeastLine.fromOutcome(outcome));
    }

    @Test
    void beastLinesParseAndNameTheirApex() {
        assertEquals(DivineBeastLine.FIRE, DivineBeastLine.parse(" fire"));
        assertNull(DivineBeastLine.parse("lava"));
        assertNull(DivineBeastLine.parse(null));
        assertEquals("Dragon", DivineBeastLine.WATER.apexName);
        assertEquals("World Turtle", DivineBeastLine.EARTH.apexName);
        assertEquals("Byakko", DivineBeastLine.BEAST.apexName);
        assertEquals("Solar Sovereign", DivineBeastLine.FIRE.apexName);
    }

    @ParameterizedTest
    @CsvSource({"0, SPIRIT", "1, SPIRIT", "19, SPIRIT", "20, GUARDIAN", "39, GUARDIAN", "40, TRUE_FORM",
            "60, CELESTIAL", "79, CELESTIAL", "80, SOVEREIGN", "100, SOVEREIGN"})
    void formStageFollowsTheLevelMilestones(int level, FormStage stage) {
        assertEquals(stage, FormStage.forLevel(level));
    }

    @Test
    void formStagesAreOrderedAndHarvestNeverDrops() {
        FormStage[] stages = FormStage.values();
        for (int i = 0; i < stages.length; i++) {
            assertEquals(i, stages[i].stageIndex);
            assertEquals(stages[i], FormStage.fromIndex(i));
            if (i > 0) {
                assertTrue(stages[i].requiredLevel > stages[i - 1].requiredLevel);
                assertTrue(stages[i].harvestLevel >= stages[i - 1].harvestLevel);
            }
        }
    }

    @Test
    void unknownFormIndexFallsBackToSpirit() {
        assertEquals(FormStage.SPIRIT, FormStage.fromIndex(-1));
        assertEquals(FormStage.SPIRIT, FormStage.fromIndex(99));
    }
}
