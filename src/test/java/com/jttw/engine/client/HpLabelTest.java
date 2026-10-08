package com.jttw.engine.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HpLabelTest {

    @Test
    void aFullBarReadsTheSameNumberTwiceWhenMaxHealthIsFractional() {
        assertEquals("HP  23 / 23", JTTWHudRenderer.hpLabel(22.4f, 22.4f));
    }

    @Test
    void wholeNumbersShowAsTheyAre() {
        assertEquals("HP  20 / 20", JTTWHudRenderer.hpLabel(20f, 20f));
        assertEquals("HP  7 / 20", JTTWHudRenderer.hpLabel(6.5f, 20f));
    }

    @Test
    void currentNeverReadsAboveMax() {
        assertEquals("HP  22 / 22", JTTWHudRenderer.hpLabel(22f, 21.6f));
    }
}
