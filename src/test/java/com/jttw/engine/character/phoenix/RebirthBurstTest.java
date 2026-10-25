package com.jttw.engine.character.phoenix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** How hard the Phoenix's revive burst strikes, by Rebirth Mastery. */
class RebirthBurstTest {

    @Test
    void rebirthMasteryMakesTheBurstStronger() {
        assertEquals(0.25f, PhoenixAbilityHandler.reviveBurstShare(0), 1e-6, "a quarter of max HP unlearned");
        assertEquals(0.40f, PhoenixAbilityHandler.reviveBurstShare(1), 1e-6, "Rebirth Mastery I: two fifths");
        assertEquals(0.40f, PhoenixAbilityHandler.reviveBurstShare(2), 1e-6, "II keeps it");
    }
}
