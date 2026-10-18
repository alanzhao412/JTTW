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
        return List.of("Shell Guard", "Roots", "Bedrock", "Force", "Tectonic", "Gravity", "Tremor", "Transform", "True Form");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "tu_shell", "Shell Guard", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "tu_force", "Unstoppable Force", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "tu_tectonic", "Tectonic Slam", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "tu_transform", "18 Transformations", AbilityType.MENU));
    }

    /** Shift + V, B, N and M (see PlayableCharacter#activate for the layout). */
    @Override
    public List<AbilitySlot> shiftBar(Set<String> unlocked) {
        return PlayableCharacter.shiftPage(AbilitySlot.fromBranch(skillTree(), unlocked, "tu_gravity", "Gravity Well", AbilityType.TOGGLE),
                AbilitySlot.EMPTY, AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(skillTree(), unlocked, "tu_true", "Black Tortoise", AbilityType.TOGGLE));
    }

    @Override
    public void use(ServerPlayer player, String abilityId, AbilityAction action) {
        switch (abilityId) {
            case "tu_shell" -> {
                if (action == AbilityAction.START) TurtleAbilityHandler.startGuard(player);
                else if (action == AbilityAction.STOP) TurtleAbilityHandler.stopGuard(player);
            }
            case "tu_force" -> { if (action == AbilityAction.USE) TurtleAbilityHandler.castUnstoppableForce(player); }
            case "tu_tectonic" -> { if (action == AbilityAction.USE) TurtleAbilityHandler.castTectonicSlam(player); }
            case "tu_gravity" -> { if (action == AbilityAction.USE) TurtleAbilityHandler.toggleGravityWell(player); }
            case "tu_true" -> { if (action == AbilityAction.USE) com.jttw.engine.character.divinebeast.TrueForm.toggle(player,
                    com.jttw.engine.registry.ModEntities.BLACK_TORTOISE.get(), skillTree().highestTier(
                            com.jttw.engine.progression.skilltree.SkillTreeData.getUnlockedNodes(player), "tu_true"), "tu_true"); }
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
}
