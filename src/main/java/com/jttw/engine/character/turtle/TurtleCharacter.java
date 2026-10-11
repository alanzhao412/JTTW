package com.jttw.engine.character.turtle;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** The World Turtle, Earth line of the Divine Beasts. */
public final class TurtleCharacter implements PlayableCharacter {

    public static final TurtleCharacter INSTANCE = new TurtleCharacter();

    private TurtleCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.TURTLE.id; }
    @Override public SkillTree skillTree() { return TurtleSkillTree.TREE; }
    @Override public String treeTitle() { return "WORLD TURTLE"; }

    @Override
    public List<String> treeColumns() {
        return List.of("Shell Guard", "Roots", "Bedrock", "Force", "Tectonic", "Gravity", "Patience", "Transform");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "tu_shell", "Shell Guard", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "tu_force", "Unstoppable Force", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "tu_tectonic", "Tectonic Slam", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "tu_gravity", "Gravity Well", AbilityType.TOGGLE));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        switch (slot) {
            case 0 -> {
                if (action == AbilityAction.START) TurtleAbilityHandler.startGuard(player);
                else if (action == AbilityAction.STOP) TurtleAbilityHandler.stopGuard(player);
            }
            case 1 -> {
                if (action == AbilityAction.USE) TurtleAbilityHandler.castUnstoppableForce(player);
            }
            case 2 -> {
                if (action == AbilityAction.USE) TurtleAbilityHandler.castTectonicSlam(player);
            }
            case 3 -> {
                if (action == AbilityAction.USE) TurtleAbilityHandler.toggleGravityWell(player);
            }
            default -> {}
        }
    }

    @Override public com.jttw.engine.transformation.TransformationSchool transformationSchool() {
        return com.jttw.engine.transformation.TransformationSchool.EIGHTEEN;
    }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "tu_transform");
    }

    /** Shift + V: the 18 Transformations (a menu, opened client-side). */
    @Override
    public List<AbilitySlot> shiftBar(Set<String> unlocked) {
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(skillTree(), unlocked, "tu_transform", "18 Transformations", AbilityType.MENU),
                AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY);
    }
}
