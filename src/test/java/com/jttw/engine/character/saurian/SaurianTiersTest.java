package com.jttw.engine.character.saurian;

import com.jttw.engine.progression.skilltree.SkillNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What each tier of the Saurian Demon King's tree gives, held against what its tooltips say. */
class SaurianTiersTest {

    private static String text(String id) {
        SkillNode n = SaurianSkillTree.TREE.get(id);
        return String.join(" ", n.descLines());
    }

    @Test
    void eachSurgeTierIsFasterFurtherHarder() {
        for (int t = 1; t < 3; t++) {
            assertTrue(SaurianAbilityHandler.surgeSpeed(t + 1, 1f) > SaurianAbilityHandler.surgeSpeed(t, 1f));
            assertTrue(SaurianAbilityHandler.surgeTicks(t + 1, 1f) > SaurianAbilityHandler.surgeTicks(t, 1f));
            assertTrue(SaurianAbilityHandler.surgeDamage(10f, t + 1, 1f) > SaurianAbilityHandler.surgeDamage(10f, t, 1f));
        }
        assertTrue(SaurianAbilityHandler.surgeTicks(1, 1f) > SaurianAbilityHandler.surgeTicks(1, 0.25f), "a full charge goes further");
    }

    @Test
    void eachLashAndWhirlpoolTierReachesFurtherAndHitsHarder() {
        for (int t = 1; t < 3; t++) {
            assertTrue(SaurianAbilityHandler.lashReach(t + 1) > SaurianAbilityHandler.lashReach(t));
            assertTrue(SaurianAbilityHandler.lashDamage(10f, t + 1) > SaurianAbilityHandler.lashDamage(10f, t));
            assertTrue(SaurianAbilityHandler.whirlRadius(t + 1) > SaurianAbilityHandler.whirlRadius(t), "wider");
            assertTrue(SaurianAbilityHandler.whirlTicks(t + 1) > SaurianAbilityHandler.whirlTicks(t), "longer");
            assertTrue(SaurianAbilityHandler.whirlDamage(10f, t + 1) > SaurianAbilityHandler.whirlDamage(10f, t), "deeper");
        }
    }

    @Test
    void theWrathLasts12Then15Then20SecondsAndHitsHarder() {
        assertEquals(240, SaurianAbilityHandler.wrathTicks(1));
        assertEquals(300, SaurianAbilityHandler.wrathTicks(2));
        assertEquals(400, SaurianAbilityHandler.wrathTicks(3));
        assertTrue(SaurianAbilityHandler.wrathBurst(2) > SaurianAbilityHandler.wrathBurst(1));
        assertTrue(SaurianAbilityHandler.wrathBurst(3) > SaurianAbilityHandler.wrathBurst(2));
    }

    @Test
    void riverbornAndVenomNumbersAreTheTooltips() {
        assertEquals(0f, SaurianAbilityHandler.wetBonus(1));
        assertEquals(0.15f, SaurianAbilityHandler.wetBonus(2));
        assertEquals(0.30f, SaurianAbilityHandler.wetBonus(3));
        assertTrue(text(SaurianSkillTree.RIVER_2).contains("+15%") && text(SaurianSkillTree.RIVER_3).contains("+30%"));
        int[] stacks = {3, 5, 7};
        String[] pct = {"4%", "5%", "6%"};
        String[] ids = {SaurianSkillTree.VENOM_1, SaurianSkillTree.VENOM_2, SaurianSkillTree.VENOM_3};
        for (int t = 1; t <= 3; t++) {
            assertEquals(stacks[t - 1], SaurianAbilityHandler.venomMaxStacks(t));
            assertEquals(Integer.parseInt(pct[t - 1].replace("%", "")) / 100f, SaurianAbilityHandler.venomPerStack(t), 1e-6f);
            assertTrue(text(ids[t - 1]).contains(String.valueOf(stacks[t - 1])) && text(ids[t - 1]).contains(pct[t - 1]), ids[t - 1]);
        }
    }
}
