package com.jttw.engine.character.bulldemon;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationSchool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

/**
 * The Bull Demon King: Bull Rush on V, the 72 Transformations on B (a menu, opened
 * client-side), the Great White Bull on M.
 */
public final class BullDemonCharacter implements PlayableCharacter {

    public static final BullDemonCharacter INSTANCE = new BullDemonCharacter();

    private BullDemonCharacter() {}

    /** True if this player is playing the Bull Demon King. */
    public static boolean is(Player player) {
        return Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.BULL_DEMON_KING.id; }
    @Override public SkillTree skillTree() { return BullDemonSkillTree.TREE; }
    @Override public String treeTitle() { return "BULL DEMON KING"; }
    @Override public List<String> treeColumns() {
        return List.of("Bull Rush", "Transforms", "Great White Bull", "Thick Hide");
    }

    @Override public TransformationSchool transformationSchool() { return TransformationSchool.SEVENTY_TWO; }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "bdk_transform");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "bdk_rush", "Bull Rush", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "bdk_transform", "72 Transformations", AbilityType.MENU),
                AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(tree, unlocked, "bdk_bull", "Great White Bull", AbilityType.TOGGLE));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        switch (slot) {
            case 0 -> {
                if (action == AbilityAction.START) BullDemonAbilityHandler.startRush(player);
                else if (action == AbilityAction.STOP) BullDemonAbilityHandler.endRush(player);
            }
            case 3 -> { if (action == AbilityAction.USE) BullDemonAbilityHandler.toggleGreatBull(player); }
            default -> {}
        }
    }
}
