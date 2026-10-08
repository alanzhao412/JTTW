package com.jttw.engine.progression.skilltree;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The content of every character's skill tree: the rules the data must keep to. */
class SkillTreesTest {

    private static final Pattern NODE_ID = Pattern.compile("([a-z]+_[a-z_]+)_(\\d+)");

    private static Stream<DynamicTest> perTree(Consumer<SkillTree> check) {
        return SkillTrees.all().stream().map(t -> DynamicTest.dynamicTest(t.idPrefix(), () -> check.accept(t)));
    }

    @Test
    void elevenTreesWithDistinctPrefixes() {
        List<SkillTree> all = SkillTrees.all();
        assertEquals(11, all.size());
        assertEquals(11, all.stream().map(SkillTree::idPrefix).distinct().count());
    }

    @Test
    void lookupFindsTheOwningTree() {
        assertSame(SkillTrees.all().get(0), SkillTrees.treeOf("wu_ring_1"));
        assertEquals("wu_ring_1", SkillTrees.resolveNode("wu_ring_1").id());
        assertEquals("es_eye_2", SkillTrees.resolveNode("es_eye_2").id());
        assertNull(SkillTrees.treeOf("zz_unknown_1"));
        assertNull(SkillTrees.resolveNode("zz_unknown_1"));
        assertNull(SkillTrees.resolveNode("wu_not_a_node_1"), "right tree, no such node");
    }

    @TestFactory
    Stream<DynamicTest> everyNodeResolvesBackToItself() {
        return perTree(t -> t.nodes().forEach((id, node) -> {
            assertSame(t, SkillTrees.treeOf(id));
            assertSame(node, SkillTrees.resolveNode(id));
        }));
    }

    @TestFactory
    Stream<DynamicTest> idsAreBranchAndNumber() {
        return perTree(t -> t.nodes().keySet().forEach(id ->
                assertTrue(NODE_ID.matcher(id).matches(), id)));
    }

    @TestFactory
    Stream<DynamicTest> tieredNodesAreNumberedByTier() {
        // highestTier relies on <branch>_<tier>; passive growth nodes use tier 0
        return perTree(t -> t.nodes().values().stream().filter(n -> n.tier() > 0).forEach(n -> {
            Matcher m = NODE_ID.matcher(n.id());
            assertTrue(m.matches(), n.id());
            assertEquals(n.tier(), Integer.parseInt(m.group(2)), n.id());
        }));
    }

    @TestFactory
    Stream<DynamicTest> nodesAreComplete() {
        return perTree(t -> t.nodes().values().forEach(n -> {
            assertFalse(n.displayName().isBlank(), n.id());
            assertTrue(n.descLines() != null && n.descLines().length > 0, n.id() + " has no description");
            assertTrue(n.cost() > 0, n.id() + " costs " + n.cost());
            assertTrue(n.col() >= 0 && n.row() >= 0, n.id());
            assertNotNull(n.type(), n.id());
            assertFalse(n.branch().isBlank(), n.id());
            assertTrue(n.tier() >= 0, n.id());
        }));
    }

    @TestFactory
    Stream<DynamicTest> noTwoNodesShareAGridCell() {
        return perTree(t -> {
            Set<String> cells = new HashSet<>();
            for (SkillNode n : t.nodes().values()) {
                assertTrue(cells.add(n.col() + "," + n.row()), n.id() + " overlaps at " + n.col() + "," + n.row());
            }
        });
    }

    @TestFactory
    Stream<DynamicTest> eachColumnIsOneBranch() {
        return perTree(t -> {
            Map<Integer, String> branchOfColumn = new HashMap<>();
            for (SkillNode n : t.nodes().values()) {
                String existing = branchOfColumn.putIfAbsent(n.col(), n.branch());
                if (existing != null) assertEquals(existing, n.branch(), n.id() + " in column " + n.col());
            }
        });
    }

    @TestFactory
    Stream<DynamicTest> prerequisitesAreInTheSameTree() {
        return perTree(t -> t.nodes().values().forEach(n -> {
            for (String pre : n.prerequisites()) assertNotNull(t.get(pre), n.id() + " needs missing " + pre);
        }));
    }

    @TestFactory
    Stream<DynamicTest> prerequisitesHaveNoCycles() {
        return perTree(t -> {
            for (SkillNode start : t.nodes().values()) {
                List<String> stack = new ArrayList<>(start.prerequisites());
                Set<String> seen = new HashSet<>();
                while (!stack.isEmpty()) {
                    String id = stack.remove(stack.size() - 1);
                    assertNotEquals(start.id(), id, start.id() + " requires itself");
                    if (seen.add(id)) stack.addAll(t.get(id).prerequisites());
                }
            }
        });
    }

    @TestFactory
    Stream<DynamicTest> everyNodeIsReachableFromARoot() {
        // with enough points, unlocking in order must reach every node
        return perTree(t -> {
            Set<String> unlocked = new HashSet<>();
            boolean progress = true;
            while (progress) {
                progress = false;
                for (SkillNode n : t.nodes().values()) {
                    if (!unlocked.contains(n.id()) && unlocked.containsAll(n.prerequisites())) {
                        unlocked.add(n.id());
                        progress = true;
                    }
                }
            }
            assertEquals(t.nodes().keySet(), unlocked);
        });
    }

    @TestFactory
    Stream<DynamicTest> activeAbilityFlagMatchesType() {
        return perTree(t -> t.nodes().values().forEach(n -> assertEquals(
                n.type() != AbilityType.PASSIVE, n.isActiveAbility(), n.id())));
    }

    @Test
    void onlyPengHasAnEmptyTree() {
        for (SkillTree t : SkillTrees.all()) {
            if (t.idPrefix().equals("gp_")) assertTrue(t.nodes().isEmpty());
            else assertFalse(t.nodes().isEmpty(), t.idPrefix());
        }
    }

    @Test
    void wukongTreeCostsMoreThanAPlayerCanEarn() {
        // WukongSkillTree: 1 point per 5 levels (20 at 100) against a total cost that forces choices
        int total = SkillTrees.treeOf("wu_x").nodes().values().stream().mapToInt(SkillNode::cost).sum();
        assertTrue(total > 20, "total cost " + total);
    }
}
