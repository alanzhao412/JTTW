package com.jttw.engine.character;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.Set;

/**
 * What one skill-bar slot shows: the ability, how its key behaves, and its unlocked tier.
 *
 * @param id          ability id, also the skill-tree branch for most abilities ({@code "wu_ring"})
 * @param displayName label shown on the HUD
 * @param type        how the key activates it
 * @param tier        highest unlocked tier
 */
public record AbilitySlot(String id, String displayName, AbilityType type, int tier) {

    /** A slot with nothing unlocked in it. The HUD draws a placeholder; keys ignore it. */
    public static final AbilitySlot EMPTY = new AbilitySlot("", "", AbilityType.PASSIVE, 0);

    public boolean isEmpty() {
        return id.isEmpty();
    }

    /**
     * The slot for a skill-tree branch, or {@link #EMPTY} until the branch reaches {@code minTier}.
     * The slot's id is the branch name.
     */
    public static AbilitySlot fromBranch(SkillTree tree, Set<String> unlocked, String branch,
                                         String displayName, AbilityType type, int minTier) {
        int tier = tree.highestTier(unlocked, branch);
        return tier >= minTier ? new AbilitySlot(branch, displayName, type, tier) : EMPTY;
    }

    /** {@link #fromBranch} with the usual threshold of tier 1. */
    public static AbilitySlot fromBranch(SkillTree tree, Set<String> unlocked, String branch,
                                         String displayName, AbilityType type) {
        return fromBranch(tree, unlocked, branch, displayName, type, 1);
    }
}
