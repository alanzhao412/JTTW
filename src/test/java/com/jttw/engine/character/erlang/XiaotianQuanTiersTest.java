package com.jttw.engine.character.erlang;

import com.jttw.engine.entity.XiaotianQuanEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What the Xiaotian Quan, Hound Training and Zhàn Shén tiers give the hound. */
class XiaotianQuanTiersTest {

    @Test
    void biteSlownessIsTheStrongestSourceNeverStacked() {
        assertNull(XiaotianQuanManager.biteSlowness(1, 2, false), "no slowing below either III");
        assertArrayEquals(new int[]{0, 60}, XiaotianQuanManager.biteSlowness(3, 0, false), "Xiaotian Quan III: I, 3 s");
        assertArrayEquals(new int[]{1, 60}, XiaotianQuanManager.biteSlowness(0, 3, false), "Hound Training III: II, 3 s");
        assertArrayEquals(new int[]{1, 60}, XiaotianQuanManager.biteSlowness(3, 3, false), "both: still II, 3 s");
        assertArrayEquals(new int[]{1, 100}, XiaotianQuanManager.biteSlowness(1, 0, true), "Zhàn Shén III in Wrath: II, 5 s");
        assertArrayEquals(new int[]{1, 100}, XiaotianQuanManager.biteSlowness(3, 3, true), "and on top of both: longer");
    }

    @Test
    void trackingReachesOneThenThree() {
        assertEquals(0, XiaotianQuanManager.trackCount(1, 1));
        assertEquals(1, XiaotianQuanManager.trackCount(2, 1), "Xiaotian Quan II: tracking sense");
        assertEquals(3, XiaotianQuanManager.trackCount(1, 2), "Hound Training II: three");
        assertEquals(3, XiaotianQuanManager.trackCount(3, 3));
    }

    @Test
    void xiaotianQuanTiersRaiseHealthAndAttack() {
        assertEquals(1.0, XiaotianQuanManager.xtqStatMultiplier(1));
        assertEquals(1.25, XiaotianQuanManager.xtqStatMultiplier(2));
        assertEquals(1.5, XiaotianQuanManager.xtqStatMultiplier(3));
    }

    @Test
    void howlRechargesTwiceAsFastAtThree() {
        assertEquals(XiaotianQuanEntity.HOWL_COOLDOWN, XiaotianQuanManager.howlRecharge(2));
        assertEquals(XiaotianQuanEntity.HOWL_COOLDOWN / 2, XiaotianQuanManager.howlRecharge(3));
    }
}
