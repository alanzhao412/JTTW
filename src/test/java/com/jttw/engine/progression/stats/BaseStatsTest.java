package com.jttw.engine.progression.stats;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class BaseStatsTest {

    private static int sum(BaseStats b) {
        return b.hp() + b.str() + b.def() + b.qi() + b.will();
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
        assertTrue(b.hp() > 0 && b.str() > 0 && b.def() > 0 && b.qi() > 0 && b.will() > 0);
    }

    @Test
    void tuningGuideShapesHold() {
        // From BaseStats' tuning guide
        assertTrue(BaseStats.PRIMORDIAL.qi() < BaseStats.PRIMORDIAL.hp(), "Primordial: low QI");
        assertTrue(BaseStats.HEAVENLY.qi() >= BaseStats.HEAVENLY.str(), "Heavenly: high QI");
        assertTrue(BaseStats.YAOGUAI.def() < BaseStats.YAOGUAI.str(), "Yaoguai: low DEF");
    }

    /**
     * BaseStats documents {@code hp + str + def + qi + will == ProgressionClass.bst}, and the
     * README's class table uses those bst values. The numbers don't agree today: the sums are
     * 1200 / 1100 / 1000 / 1030 against bst 1000 / 900 / 700 / 750. Stats are computed from the
     * sums; bst is only shown in chat. Which is intended is a balance call, so this stays off
     * until one side is changed.
     */
    @Disabled("BaseStats sums disagree with ProgressionClass.bst; needs a balance decision")
    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void sumEqualsTheClassBaseStatTotal(ProgressionClass cls) {
        assertEquals(cls.bst, sum(BaseStats.forClass(cls)));
    }
}
