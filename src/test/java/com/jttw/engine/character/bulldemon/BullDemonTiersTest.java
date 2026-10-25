package com.jttw.engine.character.bulldemon;

import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.transformation.FormCategory;
import com.jttw.engine.transformation.TransformationSchool;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What each tier of the Bull Demon King's tree gives, held against what its tooltips say. */
class BullDemonTiersTest {

    private static String text(String id) {
        SkillNode n = BullDemonSkillTree.TREE.get(id);
        return String.join(" ", n.descLines());
    }

    @Test
    void eachRushTierIsFasterLongerHarderAndStunsLonger() {
        for (int t = 1; t < 3; t++) {
            assertTrue(BullDemonAbilityHandler.rushSpeed(t + 1) > BullDemonAbilityHandler.rushSpeed(t), "faster at " + (t + 1));
            assertTrue(BullDemonAbilityHandler.rushTicks(t + 1) > BullDemonAbilityHandler.rushTicks(t), "longer at " + (t + 1));
            assertTrue(BullDemonAbilityHandler.rushDamage(10f, t + 1) > BullDemonAbilityHandler.rushDamage(10f, t), "harder at " + (t + 1));
        }
        assertEquals(40, BullDemonAbilityHandler.stunTicks(1), "I: a 2 s stun");
        assertEquals(60, BullDemonAbilityHandler.stunTicks(2), "II: 3 s");
        assertEquals(80, BullDemonAbilityHandler.stunTicks(3), "III: 4 s");
        assertTrue(text(BullDemonSkillTree.RUSH_1).contains("2 s"));
        assertTrue(text(BullDemonSkillTree.RUSH_2).contains("3 s"));
        assertTrue(text(BullDemonSkillTree.RUSH_3).contains("4 s"));
    }

    @Test
    void theGreatBullGrowsWithEachTier() {
        assertEquals(20, BullDemonAbilityHandler.greatBullHealth(1));
        assertEquals(30, BullDemonAbilityHandler.greatBullHealth(2));
        assertEquals(40, BullDemonAbilityHandler.greatBullHealth(3));
        assertTrue(BullDemonAbilityHandler.greatBullAttack(2) > BullDemonAbilityHandler.greatBullAttack(1));
        assertTrue(BullDemonAbilityHandler.greatBullAttack(3) > BullDemonAbilityHandler.greatBullAttack(2));
    }

    @Test
    void theTransformTooltipsNameTheFormsTheSeventyTwoOpens() {
        TransformationSchool school = BullDemonCharacter.INSTANCE.transformationSchool();
        assertEquals(TransformationSchool.SEVENTY_TWO, school);
        assertEquals(1, BullDemonCharacter.INSTANCE.transformationLevel(Set.of(BullDemonSkillTree.TRANSFORM_1)));
        assertTrue(school.allowed(1).containsAll(Set.of(FormCategory.NEUTRAL_LAND, FormCategory.NEUTRAL_WATER, FormCategory.NEUTRAL_AIR)));
        assertTrue(text(BullDemonSkillTree.TRANSFORM_1).contains("land, water and sky"));
        assertEquals(2, school.levelFor(FormCategory.HOSTILE_LAND));
        assertTrue(text(BullDemonSkillTree.TRANSFORM_2).contains("hostile land"));
        assertEquals(3, school.levelFor(FormCategory.BOSS));
        assertTrue(text(BullDemonSkillTree.TRANSFORM_3).contains("bosses"));
    }
}
