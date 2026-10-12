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
    @Override public List<String> treeColumns() { return List.of("Leap", "Scent", "Bleed", "Velocity", "Instinct", "Roar", "Transform", "True Form"); }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "bk_leap", "Predator's Leap", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "bk_roar", "Territorial Roar", AbilityType.TOGGLE),
                AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(tree, unlocked, "bk_transform", "18 Transformations", AbilityType.MENU));
    }

    /** Shift + V, B, N and M (see PlayableCharacter#activate for the layout). */
    @Override
    public List<AbilitySlot> shiftBar(Set<String> unlocked) {
        return PlayableCharacter.shiftPage(AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(skillTree(), unlocked, "bk_true", "White Tiger", AbilityType.TOGGLE));
    }

    @Override
    public void use(ServerPlayer player, String abilityId, AbilityAction action) {
        switch (abilityId) {
            case "bk_leap" -> { if (action == AbilityAction.USE) ByakkoAbilityHandler.castLeap(player); }
            case "bk_roar" -> { if (action == AbilityAction.USE) ByakkoAbilityHandler.castRoar(player); }
            case "bk_true" -> { if (action == AbilityAction.USE) com.jttw.engine.character.divinebeast.TrueForm.toggle(player,
                    com.jttw.engine.registry.ModEntities.WHITE_TIGER.get(), skillTree().highestTier(
                            com.jttw.engine.progression.skilltree.SkillTreeData.getUnlockedNodes(player), "bk_true"), "bk_true"); }
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
