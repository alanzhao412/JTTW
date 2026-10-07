package com.jttw.engine.progression.skilltree;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillTreeTest {

    private static SkillNode node(String id, int col, int row, int tier, String... prereqs) {
        return new SkillNode(id, id, new String[]{"desc"}, 1, col, row, List.of(prereqs),
                AbilityType.TOGGLE, tier, "Branch");
    }

    private static SkillTree tree() {
        SkillTree t = new SkillTree("xx_");
        t.add(node("xx_ring_1", 1, 0, 1));
        t.add(node("xx_ring_2", 1, 1, 2, "xx_ring_1"));
        t.add(node("xx_ring_3", 1, 2, 3, "xx_ring_2"));
        t.add(node("xx_ring_growth_1", 0, 0, 7)); // a different branch that starts with "xx_ring"
        return t;
    }

    @Test
    void prefixMustEndWithUnderscore() {
        assertThrows(IllegalArgumentException.class, () -> new SkillTree("xx"));
    }

    @Test
    void rejectsNodeFromAnotherTree() {
        SkillTree t = new SkillTree("xx_");
        assertThrows(IllegalArgumentException.class, () -> t.add(node("yy_ring_1", 0, 0, 1)));
    }

    @Test
    void rejectsDuplicateNode() {
        SkillTree t = new SkillTree("xx_");
        t.add(node("xx_ring_1", 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> t.add(node("xx_ring_1", 0, 1, 1)));
    }

    @Test
    void ownsIdsWithItsPrefixOnly() {
        SkillTree t = tree();
        assertTrue(t.owns("xx_anything_1"));
        assertFalse(t.owns("yy_ring_1"));
        assertFalse(t.owns("xxring_1"));
    }

    @Test
    void getReturnsNodeOrNull() {
        SkillTree t = tree();
        assertEquals("xx_ring_2", t.get("xx_ring_2").id());
        assertNull(t.get("xx_missing_1"));
    }

    @Test
    void nodesViewIsReadOnlyAndKeepsInsertionOrder() {
        SkillTree t = tree();
        assertEquals(List.of("xx_ring_1", "xx_ring_2", "xx_ring_3", "xx_ring_growth_1"),
                List.copyOf(t.nodes().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> t.nodes().clear());
    }

    @Test
    void allOrderedSortsByColumnThenRow() {
        List<String> ids = tree().allOrdered().stream().map(SkillNode::id).toList();
        assertEquals(List.of("xx_ring_growth_1", "xx_ring_1", "xx_ring_2", "xx_ring_3"), ids);
    }

    @Test
    void highestTierIsZeroWhenNothingUnlocked() {
        assertEquals(0, tree().highestTier(Set.of(), "xx_ring"));
    }

    @Test
    void highestTierTakesTheBestUnlockedTier() {
        assertEquals(2, tree().highestTier(Set.of("xx_ring_1", "xx_ring_2"), "xx_ring"));
        // tiers count even if an earlier one is somehow missing
        assertEquals(3, tree().highestTier(Set.of("xx_ring_3"), "xx_ring"));
    }

    @Test
    void highestTierIgnoresBranchesThatOnlyShareAPrefix() {
        // xx_ring_growth_1 has tier 7 but belongs to branch xx_ring_growth
        assertEquals(1, tree().highestTier(Set.of("xx_ring_1", "xx_ring_growth_1"), "xx_ring"));
        assertEquals(7, tree().highestTier(Set.of("xx_ring_growth_1"), "xx_ring_growth"));
    }

    @Test
    void highestTierIgnoresUnlockedIdsNotInTheTree() {
        assertEquals(0, tree().highestTier(Set.of("xx_ring_9"), "xx_ring"));
    }
}
