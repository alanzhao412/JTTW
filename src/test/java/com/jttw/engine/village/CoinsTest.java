package com.jttw.engine.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The money: eight copper to the string, eight strings to the tael; prices paid as silver and copper. */
class CoinsTest {

    @Test
    void theRatesAreFixed() {
        assertEquals(8, Coins.COPPER_PER_STRING);
        assertEquals(8, Coins.STRINGS_PER_TAEL);
        assertEquals(64, Coins.COPPER_PER_TAEL, "a tael of silver is sixty-four copper");
    }

    @Test
    void aPriceIsPaidInTaelsAndTheRestInCopper() {
        assertEquals(new Coins.Price(0, 12), Coins.price(12));
        assertEquals(new Coins.Price(1, 0), Coins.price(64));
        assertEquals(new Coins.Price(1, 36), Coins.price(100));
        assertEquals(new Coins.Price(12, 0), Coins.price(768));
        for (int wen = 1; wen <= Coins.MOST; wen++) {
            Coins.Price p = Coins.price(wen);
            assertEquals(wen, p.wen(), "nothing lost in the reckoning");
            assertTrue(p.copper() < 64 && p.silver() <= 64, "each part a stack at most");
        }
        assertThrows(IllegalArgumentException.class, () -> Coins.price(0));
        assertThrows(IllegalArgumentException.class, () -> Coins.price(Coins.MOST + 1));
    }

    @Test
    void aMerchantPaysOutInOneStack() {
        assertTrue(Coins.payableInOneStack(64));
        assertTrue(Coins.payableInOneStack(128), "two taels");
        assertFalse(Coins.payableInOneStack(100), "a tael and 36 copper is two stacks");
        assertEquals("1 tael 36 wen", Coins.describe(100));
        assertEquals("5 wen", Coins.describe(5));
        assertEquals("3 taels", Coins.describe(192));
    }
}
