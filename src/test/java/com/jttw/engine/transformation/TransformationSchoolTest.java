package com.jttw.engine.transformation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.transformation.FormCategory.*;
import static com.jttw.engine.transformation.TransformationSchool.*;
import static org.junit.jupiter.api.Assertions.*;

/** The forms each transformation art offers at each level, exactly as specified. */
class TransformationSchoolTest {

    private static Set<FormCategory> set(FormCategory... c) {
        return c.length == 0 ? EnumSet.noneOf(FormCategory.class) : EnumSet.of(c[0], c);
    }

    // 18: neutral land; + neutral water; + neutral air

    @Test
    void eighteenLevelOneIsNeutralLandOnly() {
        assertEquals(set(NEUTRAL_LAND), EIGHTEEN.allowed(1));
    }

    @Test
    void eighteenLevelTwoAddsNeutralWater() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER), EIGHTEEN.allowed(2));
    }

    @Test
    void eighteenLevelThreeAddsNeutralAir() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR), EIGHTEEN.allowed(3));
    }

    // 36: neutral land and water; + neutral air; + hostile land

    @Test
    void thirtySixLevelOneIsNeutralLandAndWater() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER), THIRTY_SIX.allowed(1));
    }

    @Test
    void thirtySixLevelTwoAddsNeutralAir() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR), THIRTY_SIX.allowed(2));
    }

    @Test
    void thirtySixLevelThreeAddsHostileLand() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR, HOSTILE_LAND), THIRTY_SIX.allowed(3));
    }

    // 72: all neutral; + hostile land (no bosses); + bosses, players, objects, hostile sea and sky

    @Test
    void seventyTwoLevelOneIsEveryNeutralForm() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR), SEVENTY_TWO.allowed(1));
    }

    @Test
    void seventyTwoLevelTwoAddsHostileLandButNoBosses() {
        assertEquals(set(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR, HOSTILE_LAND), SEVENTY_TWO.allowed(2));
        assertFalse(SEVENTY_TWO.allows(BOSS, 2));
    }

    @Test
    void seventyTwoLevelThreeOpensEverything() {
        assertEquals(EnumSet.allOf(FormCategory.class), SEVENTY_TWO.allowed(3));
    }

    // shared behaviour

    @ParameterizedTest
    @EnumSource(TransformationSchool.class)
    void levelZeroOrLessAllowsNothing(TransformationSchool school) {
        assertTrue(school.allowed(0).isEmpty());
        assertTrue(school.allowed(-1).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(TransformationSchool.class)
    void levelsAboveThreeAreLevelThree(TransformationSchool school) {
        assertEquals(school.allowed(3), school.allowed(7));
    }

    @ParameterizedTest
    @EnumSource(TransformationSchool.class)
    void eachLevelKeepsEverythingBelowIt(TransformationSchool school) {
        for (int l = 1; l < MAX_LEVEL; l++) assertTrue(school.allowed(l + 1).containsAll(school.allowed(l)));
    }

    @ParameterizedTest
    @EnumSource(TransformationSchool.class)
    void levelForIsTheFirstLevelThatAllows(TransformationSchool school) {
        for (FormCategory c : FormCategory.values()) {
            int at = school.levelFor(c);
            if (at == 0) {
                assertFalse(school.allows(c, MAX_LEVEL), c + " never opens");
            } else {
                assertTrue(school.allows(c, at), c + " at " + at);
                assertFalse(school.allows(c, at - 1), c + " before " + at);
            }
        }
    }

    @Test
    void schoolsAreNestedEighteenInsideThirtySixInsideSeventyTwo() {
        for (int l = 1; l <= MAX_LEVEL; l++) {
            assertTrue(THIRTY_SIX.allowed(l).containsAll(EIGHTEEN.allowed(l)), "36 >= 18 at " + l);
            assertTrue(SEVENTY_TWO.allowed(l).containsAll(THIRTY_SIX.allowed(l)), "72 >= 36 at " + l);
        }
    }

    @Test
    void onlySeventyTwoEverOffersBossesPlayersObjectsOrHostileSeaAndSky() {
        for (FormCategory c : List.of(BOSS, PLAYER, OBJECT, HOSTILE_WATER, HOSTILE_AIR)) {
            assertEquals(0, EIGHTEEN.levelFor(c), "18 " + c);
            assertEquals(0, THIRTY_SIX.levelFor(c), "36 " + c);
            assertEquals(3, SEVENTY_TWO.levelFor(c), "72 " + c);
        }
    }

    @Test
    void titlesNameTheCount() {
        assertEquals("18 Transformations", EIGHTEEN.title());
        assertEquals("36 Transformations", THIRTY_SIX.title());
        assertEquals("72 Transformations", SEVENTY_TWO.title());
    }

    @Test
    void menuTabsAreTheCategoriesTheSchoolEverOffers() {
        assertEquals(List.of(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR), EIGHTEEN.categories());
        assertEquals(List.of(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR, HOSTILE_LAND), THIRTY_SIX.categories());
        assertEquals(List.of(FormCategory.values()), SEVENTY_TWO.categories());
    }
}
