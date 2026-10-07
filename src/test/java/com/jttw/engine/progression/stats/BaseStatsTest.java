package com.jttw.engine.progression.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class BaseStatsTest {

    private static int sum(BaseStats b) {
        return b.hp() + b.str() + b.pres() + b.mres() + b.qi() + b.will();
    }

    @Test
    void eachClassHasItsOwnDistribution() {
        assertSame(BaseStats.PRIMORDIAL, BaseStats.forClass(ProgressionClass.PRIMORDIAL));
        assertSame(BaseStats.DIVINE_BEAST, BaseStats.forClass(ProgressionClass.DIVINE_BEAST));
        assertSame(BaseStats.HEAVENLY, BaseStats.forClass(ProgressionClass.HEAVENLY));
        assertSame(BaseStats.YAOGUAI, BaseStats.forClass(ProgressionClass.YAOGUAI));
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void allStatsArePositive(ProgressionClass cls) {
        BaseStats b = BaseStats.forClass(cls);
        assertTrue(b.hp() > 0 && b.str() > 0 && b.pres() > 0 && b.mres() > 0 && b.qi() > 0 && b.will() > 0);
    }

    @Test
    void tuningGuideShapesHold() {
        // From BaseStats' tuning guide
        assertTrue(BaseStats.PRIMORDIAL.qi() < BaseStats.PRIMORDIAL.hp(), "Primordial: low QI");
        assertTrue(BaseStats.HEAVENLY.qi() >= BaseStats.HEAVENLY.str(), "Heavenly: high QI");
        assertTrue(BaseStats.YAOGUAI.pres() + BaseStats.YAOGUAI.mres() < BaseStats.YAOGUAI.str(), "Yaoguai: thin defences");
        assertTrue(BaseStats.PRIMORDIAL.pres() > BaseStats.PRIMORDIAL.mres(), "Primordial: built for blows");
        assertTrue(BaseStats.HEAVENLY.mres() > BaseStats.HEAVENLY.pres(), "Heavenly: wards against magic");
    }

    /** BaseStats' invariant: the six base stats add up to the class's advertised total. */
    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void sumEqualsTheClassBaseStatTotal(ProgressionClass cls) {
        assertEquals(cls.bst, sum(BaseStats.forClass(cls)));
    }
}
