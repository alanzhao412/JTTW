package com.jttw.engine.character;

import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * One playable character (a reincarnation outcome): its skill tree, its skill bar, and what its
 * ability keys do.
 *
 * <p>Everything that varies by character lives behind this interface, so the network,
 * HUD and skill-tree screen look the character up once through {@link Characters} instead
 * of each keeping its own chain of outcome checks.
 */
public interface PlayableCharacter {

    /** Skill-bar slots, bound to V, B, N, M. */
    int SLOT_COUNT = 4;

    /** The reincarnation outcome id this character is, e.g. {@code "celestial_primate"}. */
    String outcomeId();

    SkillTree skillTree();

    /** Heading for the skill-tree screen. */
    String treeTitle();

    /** Column headings for the skill-tree screen, left to right. */
    List<String> treeColumns();

    /**
     * The four skill-bar slots for a player with these nodes unlocked. Always exactly
     * {@link #SLOT_COUNT} entries, empty slots as {@link AbilitySlot#EMPTY}, so slot indices
     * mean the same thing on client and server.
     */
    default List<AbilitySlot> skillBar(Set<String> unlockedNodes) {
        return emptyBar();
    }

    /**
     * This player's skill bar. Usually just {@link #skillBar(Set)} over their unlocked nodes;
     * characters with slots earned through the story rather than the tree override this.
     */
    default List<AbilitySlot> skillBar(Player player) {
        return skillBar(SkillTreeData.getUnlockedNodes(player));
    }

    /** Runs the ability in {@code slot} for a key event. Unused slots do nothing. */
    default void activate(ServerPlayer player, int slot, AbilityAction action) {}

    static List<AbilitySlot> emptyBar() {
        AbilitySlot[] slots = new AbilitySlot[SLOT_COUNT];
        Arrays.fill(slots, AbilitySlot.EMPTY);
        return List.of(slots);
    }

    /** A bar from up to four slots, padded with empties. */
    static List<AbilitySlot> bar(AbilitySlot... slots) {
        if (slots.length > SLOT_COUNT) {
            throw new IllegalArgumentException("A skill bar has " + SLOT_COUNT + " slots");
        }
        AbilitySlot[] all = new AbilitySlot[SLOT_COUNT];
        Arrays.fill(all, AbilitySlot.EMPTY);
        System.arraycopy(slots, 0, all, 0, slots.length);
        return List.of(all);
    }
}
