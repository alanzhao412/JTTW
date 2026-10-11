package com.jttw.engine.progression.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A Divine Beast's look grows a step at each form, and once more at level 100. */
class BeastFormShownTest {

    @Test
    void aStepAtEachFormAndAtTheTop() {
        assertEquals(0, BeastFormShown.stage(1), "human-shaped to begin");
        assertEquals(0, BeastFormShown.stage(19));
        assertEquals(1, BeastFormShown.stage(20), "eyes and forearm scales");
        assertEquals(2, BeastFormShown.stage(40), "a tail");
        assertEquals(3, BeastFormShown.stage(60), "the face grows forward");
        assertEquals(4, BeastFormShown.stage(80));
        assertEquals(4, BeastFormShown.stage(99));
        assertEquals(5, BeastFormShown.stage(100), "the beast entire");
    }
}
