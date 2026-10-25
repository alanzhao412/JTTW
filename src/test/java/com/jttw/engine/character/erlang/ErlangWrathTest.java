package com.jttw.engine.character.erlang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Zhàn Shén's rules: when Wrath comes, and what each tier gives in it. */
class ErlangWrathTest {

    @Test
    void wrathIsBelowFortyPercent() {
        assertTrue(ErlangWrath.wrathHealth(7.9f, 20f));
        assertFalse(ErlangWrath.wrathHealth(8f, 20f), "at 40% exactly he is out of it");
        assertFalse(ErlangWrath.wrathHealth(20f, 20f));
        assertTrue(ErlangWrath.wrathHealth(1f, 100f));
        assertFalse(ErlangWrath.wrathHealth(0f, 0f), "no max health: no Wrath");
    }

    @Test
    void damageAndSpeedBonusByTier() {
        assertEquals(0f, ErlangWrath.bonus(0));
        assertEquals(0.15f, ErlangWrath.bonus(1));
        assertEquals(0.25f, ErlangWrath.bonus(2));
        assertEquals(0.35f, ErlangWrath.bonus(3));
    }

    @Test
    void knockbackImmunityAndTheBerserkHoundFromTierTwo() {
        assertFalse(ErlangWrath.knockbackImmune(1));
        assertTrue(ErlangWrath.knockbackImmune(2));
        assertTrue(ErlangWrath.knockbackImmune(3));
        assertFalse(ErlangWrath.houndBerserks(0));
        assertFalse(ErlangWrath.houndBerserks(1));
        assertTrue(ErlangWrath.houndBerserks(2));
    }

    @Test
    void lifestealAtTierThree() {
        assertEquals(0f, ErlangWrath.lifesteal(2));
        assertEquals(0.10f, ErlangWrath.lifesteal(3));
    }
}
