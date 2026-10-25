package com.jttw.engine.character.yuandi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** How far Sovereign's Command reaches, by its tier and the form Yuan Di takes. */
class SovereignsCommandTest {

    @Test
    void reachGrowsByTierAndToTwentyFourOnlyIntoTheMasteredForm() {
        assertEquals(8.0, YuanDiAbilityHandler.commandRadius(1, 3), "I: 8 in any form");
        assertEquals(12.0, YuanDiAbilityHandler.commandRadius(2, 3), "II: 12 in any form");
        assertEquals(16.0, YuanDiAbilityHandler.commandRadius(3, 1), "III, awakening: 16");
        assertEquals(16.0, YuanDiAbilityHandler.commandRadius(3, 2), "III, Controlled Shift: 16");
        assertEquals(24.0, YuanDiAbilityHandler.commandRadius(3, 3), "III, into the Mastered Form: 24");
    }
}
