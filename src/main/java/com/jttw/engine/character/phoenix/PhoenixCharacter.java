package com.jttw.engine.character.phoenix;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** The Phoenix (Solar Sovereign), Fire line of the Divine Beasts. */
public final class PhoenixCharacter implements PlayableCharacter {

    public static final PhoenixCharacter INSTANCE = new PhoenixCharacter();

    private PhoenixCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.PHOENIX.id; }
    @Override public SkillTree skillTree() { return PhoenixSkillTree.TREE; }
    @Override public String treeTitle() { return "SOLAR SOVEREIGN"; }
    @Override public List<String> treeColumns() { return List.of("Flamethrower", "Life Drain", "Rebirth", "Radiance", "Ember", "Talons", "Transform"); }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "ph_flame", "Flamethrower", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "ph_radiance", "Solar Radiance", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "ph_transform", "18 Transformations", AbilityType.MENU));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        switch (slot) {
            case 0 -> {
                if (action == AbilityAction.START) PhoenixAbilityHandler.startFlame(player);
                else if (action == AbilityAction.STOP) PhoenixAbilityHandler.stopFlame(player);
            }
            case 1 -> {
                if (action == AbilityAction.USE) PhoenixAbilityHandler.toggleRadiance(player);
            }
            default -> {}
        }
    }

    @Override public com.jttw.engine.transformation.TransformationSchool transformationSchool() {
        return com.jttw.engine.transformation.TransformationSchool.EIGHTEEN;
    }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "ph_transform");
    }
}
