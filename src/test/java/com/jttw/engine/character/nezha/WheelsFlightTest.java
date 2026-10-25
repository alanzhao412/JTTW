package com.jttw.engine.character.nezha;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The speed Nezha's Wind Fire Wheels fly at, by his flight nodes. */
class WheelsFlightTest {

    @Test
    void wheelStepCruisesAndBlazingTrailIsTheFastest() {
        assertEquals(0, NezhaAbilityHandler.wheelsSpeedTier(0), "no node, no flight tier");
        assertEquals(2, NezhaAbilityHandler.wheelsSpeedTier(1), "Wheel Step: cruising, never a glide");
        assertEquals(3, NezhaAbilityHandler.wheelsSpeedTier(2), "Blazing Trail: the fastest");
        assertEquals(3, NezhaAbilityHandler.wheelsSpeedTier(5), "never past the fastest");
    }
}
