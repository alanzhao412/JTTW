package com.jttw.engine.character;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AbilitySlotTest {

    private static SkillTree ringTree() {
        SkillTree t = new SkillTree("xx_");
        for (int tier = 1; tier <= 3; tier++) {
            t.add(new SkillNode("xx_ring_" + tier, "Ring " + tier, new String[]{"d"}, 1, 0, tier - 1,
                    List.of(), AbilityType.HELD, tier, "Ring"));
        }
        return t;
    }

    @Test
    void emptySlotIsEmpty() {
        assertTrue(AbilitySlot.EMPTY.isEmpty());
        assertFalse(new AbilitySlot("x", "X", AbilityType.TOGGLE, 1).isEmpty());
    }

    @Test
    void fromBranchIsEmptyUntilTheBranchIsLearned() {
        assertSame(AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(ringTree(), Set.of(), "xx_ring", "Ring", AbilityType.HELD));
    }

    @Test
    void fromBranchCarriesTheHighestTierAndUsesTheBranchAsId() {
        AbilitySlot s = AbilitySlot.fromBranch(ringTree(), Set.of("xx_ring_1", "xx_ring_2"),
                "xx_ring", "Ring of Fire", AbilityType.HELD);
        assertEquals(new AbilitySlot("xx_ring", "Ring of Fire", AbilityType.HELD, 2), s);
    }

    @Test
    void fromBranchRespectsAMinimumTier() {
        SkillTree t = ringTree();
        assertSame(AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(t, Set.of("xx_ring_1"), "xx_ring", "R", AbilityType.TOGGLE, 2));
        assertEquals(2, AbilitySlot.fromBranch(t, Set.of("xx_ring_2"), "xx_ring", "R", AbilityType.TOGGLE, 2).tier());
    }

    @Test
    void barPadsToFourSlots() {
        AbilitySlot a = new AbilitySlot("a", "A", AbilityType.TOGGLE, 1);
        List<AbilitySlot> bar = PlayableCharacter.bar(a);
        assertEquals(List.of(a, AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY), bar);
        assertEquals(PlayableCharacter.bar(), PlayableCharacter.emptyBar());
    }

    @Test
    void barRejectsMoreThanFourSlots() {
        AbilitySlot a = AbilitySlot.EMPTY;
        assertThrows(IllegalArgumentException.class, () -> PlayableCharacter.bar(a, a, a, a, a));
    }

    @Test
    void barIsReadOnly() {
        assertThrows(UnsupportedOperationException.class,
                () -> PlayableCharacter.emptyBar().set(0, AbilitySlot.EMPTY));
    }
}
