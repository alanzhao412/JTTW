package com.jttw.engine.entity;

import com.jttw.engine.progression.level.LevelConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The typical character's curve, and its going on past the players' cap, where only bosses stand. */
class MobScalingTest {

    @Test
    void theCurveGoesOnGrowingPastTheCapAsItGrewToIt() {
        int cap = LevelConstants.MAX_LEVEL;
        for (int lv = cap + 1; lv <= cap + 20; lv++) {
            assertTrue(MobScaling.typicalHealth(lv) > MobScaling.typicalHealth(lv - 1), "health grows at " + lv);
            assertTrue(MobScaling.typicalHit(lv) > MobScaling.typicalHit(lv - 1), "hits grow at " + lv);
        }
        double tenBefore = MobScaling.typicalHealth(cap) / MobScaling.typicalHealth(cap - MobScaling.GROWTH_SPAN);
        double tenAfter = MobScaling.typicalHealth(cap + MobScaling.GROWTH_SPAN) / MobScaling.typicalHealth(cap);
        assertEquals(tenBefore, tenAfter, 1e-9, "as fast past the cap as over the ten levels to it");
        // unchanged up to the cap
        assertTrue(MobScaling.typicalHealth(cap) > MobScaling.typicalHealth(cap - 1));
    }
}
