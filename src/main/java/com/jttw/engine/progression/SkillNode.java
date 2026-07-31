package com.jttw.engine.progression;

import java.util.List;

/**
 * A single node in a subclass skill tree.
 *
 * <p>Nodes are defined statically in subclass tree classes (e.g. {@link WukongSkillTree})
 * and consumed by {@link SkillTreeData} for unlock tracking and by
 * {@link com.jttw.engine.client.gui.SkillTreeScreen} for rendering.
 *
 * @param id              Unique string identifier (e.g. {@code "wu_transform_1"}).
 * @param displayName     Short label shown in the skill tree UI.
 * @param descLines       Multi-line description shown in the tooltip (2-3 lines recommended).
 * @param cost            Skill points required to unlock.
 * @param col             Grid column for the UI renderer (0-based).
 * @param row             Grid row for the UI renderer (0-based).
 * @param prerequisites   IDs of nodes that must be unlocked before this one is available.
 * @param type            How this ability is activated (see {@link AbilityType}).
 * @param tier            Tier level of this node (1, 2, 3 …). Used for upgrade logic.
 *                        Passive nodes use tier 0.
 * @param branch          Branch label for column header grouping.
 */
public record SkillNode(
        String id,
        String displayName,
        String[] descLines,
        int cost,
        int col,
        int row,
        List<String> prerequisites,
        AbilityType type,
        int tier,
        String branch
) {
    /** True if this node has any prerequisites. */
    public boolean hasPrerequisites() {
        return prerequisites != null && !prerequisites.isEmpty();
    }

    /** True if this is a tier node for an active ability (MENU, TOGGLE, or HELD). */
    public boolean isActiveAbility() {
        return type == AbilityType.MENU || type == AbilityType.TOGGLE || type == AbilityType.HELD;
    }
}