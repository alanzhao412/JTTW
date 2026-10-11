package com.jttw.engine.world;

import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.stats.ProgressionClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The mod's own ground: much harder to blast or carve, unless its breaker is very strong. */
class HardenedGroundTest {

    private static final float STONE = 6, OBSIDIAN = 1200, LEAVES = 0.2f, TNT = 4;

    private static double strengthAt(int level) {
        return Math.max(1, Math.sqrt(LevelPower.of(ProgressionClass.HEAVENLY, level)));
    }

    @Test
    void ordinaryGroundIsTougherThanObsidianAgainstTheWeak() {
        assertTrue(HardenedGround.resistance(STONE, 1) > OBSIDIAN, "stone, to a creeper");
        assertFalse(HardenedGround.breaks(TNT, 0, HardenedGround.resistance(STONE, 1)), "TNT does not touch it");
        assertFalse(HardenedGround.breaks(56, 0, HardenedGround.resistance(STONE, strengthAt(20))), "nor a hundredfold staff at level 20");
    }

    @Test
    void theVeryStrongBreakItStill() {
        float atSixty = HardenedGround.resistance(STONE, strengthAt(60)), atHundred = HardenedGround.resistance(STONE, strengthAt(100));
        assertTrue(atSixty < HardenedGround.resistance(STONE, strengthAt(20)) && atHundred < atSixty, "stronger, softer");
        assertFalse(HardenedGround.breaks(TNT, 0, atSixty), "at 60, a small blast still does nothing");
        assertTrue(HardenedGround.breaks(40, 0, atSixty), "but a great one does");
        assertTrue(HardenedGround.breaks(12, 0, atHundred), "at 100, a modest blast breaks it");
    }

    @Test
    void softThingsBreakAsAnywhere() {
        assertEquals(LEAVES, HardenedGround.resistance(LEAVES, 1), 1e-6);
    }
}
