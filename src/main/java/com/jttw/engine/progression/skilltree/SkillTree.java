package com.jttw.engine.progression.skilltree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One character's skill tree: an ordered set of {@link SkillNode}s that share an id prefix.
 *
 * <p>Node ids follow {@code <branch>_<tier number>}, e.g. {@code wu_ring_2} is tier 2 of the
 * {@code wu_ring} branch. Every tree owns exactly one prefix ({@code "wu_"}), which is how
 * {@link SkillTrees} finds the tree a node belongs to.
 */
public final class SkillTree {

    private final String idPrefix;
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();

    public SkillTree(String idPrefix) {
        if (!idPrefix.endsWith("_")) {
            throw new IllegalArgumentException("Skill tree prefix must end with '_': " + idPrefix);
        }
        this.idPrefix = idPrefix;
    }

    /** Adds a node. Ids must carry this tree's prefix and be unique within it. */
    public void add(SkillNode node) {
        if (!node.id().startsWith(idPrefix)) {
            throw new IllegalArgumentException(node.id() + " does not belong to tree " + idPrefix);
        }
        if (nodes.putIfAbsent(node.id(), node) != null) {
            throw new IllegalArgumentException("Duplicate skill node " + node.id());
        }
    }

    public String idPrefix() {
        return idPrefix;
    }

    /** True if {@code nodeId} is named with this tree's prefix. */
    public boolean owns(String nodeId) {
        return nodeId.startsWith(idPrefix);
    }

    public Map<String, SkillNode> nodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public SkillNode get(String id) {
        return nodes.get(id);
    }

    /** All nodes, ordered by grid column then row. */
    public List<SkillNode> allOrdered() {
        List<SkillNode> list = new ArrayList<>(nodes.values());
        list.sort(Comparator.comparingInt(SkillNode::col).thenComparingInt(SkillNode::row));
        return list;
    }

    /**
     * Highest unlocked tier in one branch, or 0 if none of it is unlocked.
     *
     * <p>Matches {@code <branch>_<number>} exactly, so {@code "es_xtq"} counts {@code es_xtq_2}
     * but not {@code es_xtq_growth_2}, which belongs to a different branch.
     */
    public int highestTier(Set<String> unlockedNodeIds, String branch) {
        Pattern branchNode = Pattern.compile(Pattern.quote(branch) + "_\\d+");
        int tier = 0;
        for (SkillNode node : nodes.values()) {
            if (unlockedNodeIds.contains(node.id()) && branchNode.matcher(node.id()).matches()) {
                tier = Math.max(tier, node.tier());
            }
        }
        return tier;
    }
}
