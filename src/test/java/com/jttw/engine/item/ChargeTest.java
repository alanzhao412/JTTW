package com.jttw.engine.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A charge reads 0 at the least that strikes, 1 at full, and is not ready before the least. */
class ChargeTest {

    @Test
    void beforeTheLeastItIsNotReady() {
        Charged.Charge c = Charged.Charge.between(5, 10, 60);
        assertFalse(c.ready());
        assertEquals(0f, c.fraction());
    }

    @Test
    void itGrowsFromTheLeastToFull() {
        assertEquals(0f, Charged.Charge.between(10, 10, 60).fraction());
        assertTrue(Charged.Charge.between(10, 10, 60).ready());
        assertEquals(0.5f, Charged.Charge.between(35, 10, 60).fraction(), 1e-6);
        assertEquals(1f, Charged.Charge.between(60, 10, 60).fraction());
        assertEquals(1f, Charged.Charge.between(600, 10, 60).fraction(), "held past full, still full");
    }
}
