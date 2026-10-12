package com.jttw.engine.character;

import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.transformation.TransformationSchool;
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

    /**
     * The second page of four, shown while Shift is held, for a character with more than four
     * abilities; its slots are numbered 4 to 7 in {@link #activate}. Empty for most.
     */
    default List<AbilitySlot> shiftBar(Set<String> unlockedNodes) {
        return emptyBar();
    }

    /** Everything on this player's bar: the first page, and the Shift page if it holds anything. */
    default List<AbilitySlot> fullBar(Player player) {
        Set<String> unlocked = SkillTreeData.getUnlockedNodes(player);
        List<AbilitySlot> first = skillBar(player), second = shiftBar(unlocked);
        if (second.stream().allMatch(AbilitySlot::isEmpty)) return first;
        List<AbilitySlot> all = new java.util.ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }

    /** The transformation art this character practises, or null if it has none. */
    default TransformationSchool transformationSchool() {
        return null;
    }

    /** This character's level (0-3) in its transformation art, from its unlocked nodes. */
    default int transformationLevel(Set<String> unlockedNodes) {
        return 0;
    }

    /**
     * Runs the ability in {@code slot} (0-3 the keys V B N M, 4-7 the same with Shift) for a key
     * event: whatever ability this player's bar holds there, by its id ({@link #use}). Unused
     * slots do nothing.
     *
     * <p>Every character's keys follow one layout: V, B and N hold its own abilities, its
     * signature first, without gaps; M its transformation art (the 18, 36 or 72), or, if it has
     * none, its own form; Shift with V, B or N any further abilities; Shift with M its true form or
     * ultimate, where M holds its art.
     */
    default void activate(ServerPlayer player, int slot, AbilityAction action) {
        // exhausted after a true form, no ability answers
        if (com.jttw.engine.character.divinebeast.TrueForm.exhausted(player)) return;
        List<AbilitySlot> bar = fullBar(player);
        if (slot < 0 || slot >= bar.size() || bar.get(slot).isEmpty()) return;
        use(player, bar.get(slot).id(), action);
    }

    /** Runs the ability {@code abilityId} for a key event (USE, or START and STOP for one held). */
    default void use(ServerPlayer player, String abilityId, AbilityAction action) {}

    /** Shift + V, B, N and M: the second page, of further abilities and the true form. */
    static List<AbilitySlot> shiftPage(AbilitySlot shiftV, AbilitySlot shiftB, AbilitySlot shiftN, AbilitySlot shiftM) {
        return bar(shiftV, shiftB, shiftN, shiftM);
    }

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
