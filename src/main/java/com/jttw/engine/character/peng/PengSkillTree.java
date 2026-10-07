package com.jttw.engine.character.peng;

import com.jttw.engine.progression.skilltree.SkillNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skill tree for Gold Winged Peng (PRIMORDIAL — Spirit Beast secondary).
 *
 * <p><b>TODO:</b> Abilities not yet designed. Skeleton tree for routing purposes.
 */
public final class PengSkillTree {

    private PengSkillTree() {}

    // -----------------------------------------------------------------------
    // Registry (empty for now)
    // -----------------------------------------------------------------------

    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public static Map<String, SkillNode> getNodes() {
        return Collections.unmodifiableMap(NODES);
    }

    public static SkillNode get(String id) {
        return NODES.get(id);
    }

    public static List<SkillNode> getAllOrdered() {
        List<SkillNode> list = new ArrayList<>(NODES.values());
        list.sort((a, b) -> a.col() != b.col() ? Integer.compare(a.col(), b.col())
                                                 : Integer.compare(a.row(), b.row()));
        return list;
    }

    public static int getHighestTier(Set<String> unlockedNodeIds, String idPrefix) {
        int tier = 0;
        for (SkillNode node : NODES.values()) {
            if (node.id().startsWith(idPrefix) && unlockedNodeIds.contains(node.id())) {
                tier = Math.max(tier, node.tier());
            }
        }
        return tier;
    }
}
