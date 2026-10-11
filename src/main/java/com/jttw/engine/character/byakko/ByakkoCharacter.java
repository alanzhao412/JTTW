package com.jttw.engine.character.byakko;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** Byakko, the White Tiger, Beast line of the Divine Beasts. */
public final class ByakkoCharacter implements PlayableCharacter {

    public static final ByakkoCharacter INSTANCE = new ByakkoCharacter();

    private ByakkoCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.WHITE_TIGER.id; }
    @Override public SkillTree skillTree() { return ByakkoSkillTree.TREE; }
    @Override public String treeTitle() { return "BYAKKO"; }
    @Override public List<String> treeColumns() { return List.of("Leap", "Scent", "Bleed", "Velocity", "Instinct", "Roar", "Transform"); }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "bk_leap", "Predator's Leap", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "bk_roar", "Territorial Roar", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "bk_transform", "18 Transformations", AbilityType.MENU));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        if (action != AbilityAction.USE) return;
        switch (slot) {
            case 0 -> ByakkoAbilityHandler.castLeap(player);
            case 1 -> ByakkoAbilityHandler.castRoar(player);
            default -> {}
        }
    }

    @Override public com.jttw.engine.transformation.TransformationSchool transformationSchool() {
        return com.jttw.engine.transformation.TransformationSchool.EIGHTEEN;
    }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "bk_transform");
    }
}
