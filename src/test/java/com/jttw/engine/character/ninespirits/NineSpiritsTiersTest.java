package com.jttw.engine.character.ninespirits;

import com.jttw.engine.progression.skilltree.SkillNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What each tier of the Grand Saint's tree gives, held against what its tooltips say. */
class NineSpiritsTiersTest {

    private static String text(String id) {
        SkillNode n = NineSpiritsSkillTree.TREE.get(id);
        return String.join(" ", n.descLines());
    }

    @Test
    void theHeadsHitHarderAndThunderLeapsFurtherWithEachHead() {
        assertEquals(10f, NineSpiritsAbilityHandler.headDamage(10f, 1), 1e-6f);
        assertEquals(13f, NineSpiritsAbilityHandler.headDamage(10f, 2), 1e-5f);
        assertEquals(16f, NineSpiritsAbilityHandler.headDamage(10f, 3), 1e-5f);
        assertTrue(text(NineSpiritsSkillTree.HEADS_2).contains("x1.3"));
        assertEquals(2, NineSpiritsAbilityHandler.thunderJumps(2));
        assertEquals(4, NineSpiritsAbilityHandler.thunderJumps(3));
        assertTrue(text(NineSpiritsSkillTree.HEADS_3).contains("4 more foes, not 2"));
    }

    @Test
    void theRoarReachesFurtherAndCowsForTwoSeconds() {
        assertTrue(NineSpiritsAbilityHandler.roarRange(2) > NineSpiritsAbilityHandler.roarRange(1));
        assertTrue(NineSpiritsAbilityHandler.roarRange(3) > NineSpiritsAbilityHandler.roarRange(2));
        assertEquals(40, NineSpiritsAbilityHandler.COWED_TICKS);
        assertTrue(text(NineSpiritsSkillTree.ROAR_2).contains("2 seconds"));
    }

    @Test
    void theManeFlaresFurtherAndLongerAtTwo() {
        assertEquals(12, NineSpiritsAbilityHandler.flareRange(1));
        assertEquals(20, NineSpiritsAbilityHandler.flareRange(2));
        assertEquals(60, NineSpiritsAbilityHandler.flareBlindTicks(1));
        assertEquals(100, NineSpiritsAbilityHandler.flareBlindTicks(2));
        assertTrue(text(NineSpiritsSkillTree.FLARE_1).contains("12 blocks"));
        assertTrue(text(NineSpiritsSkillTree.FLARE_2).contains("20 blocks") && text(NineSpiritsSkillTree.FLARE_2).contains("5 s"));
    }

    @Test
    void theTrueFormsHeadsAndToughnessAreTheTooltips() {
        String[] ids = {NineSpiritsSkillTree.FORM_1, NineSpiritsSkillTree.FORM_2, NineSpiritsSkillTree.FORM_3};
        String[] mult = {"x1.5", "x2", "x2.5"};
        for (int t = 1; t <= 3; t++) {
            assertEquals(t + 1, NineSpiritsAbilityHandler.formHeads(t));
            assertEquals(1f + 0.5f * t, NineSpiritsAbilityHandler.formHitMultiplier(t), 1e-6f);
            assertEquals(10.0 + 10 * t, NineSpiritsAbilityHandler.formResistance(t));
            String s = text(ids[t - 1]);
            assertTrue(s.contains((t + 1) + " heads") && s.contains(mult[t - 1]) && s.contains("+" + (10 + 10 * t)), s);
        }
    }
}
