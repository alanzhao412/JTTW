package com.jttw.engine.character.yuandi;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** Yuan Di. */
public final class YuanDiCharacter implements PlayableCharacter {

    public static final YuanDiCharacter INSTANCE = new YuanDiCharacter();

    private YuanDiCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.YUAN_DI.id; }
    @Override public SkillTree skillTree() { return YuanDiSkillTree.TREE; }
    @Override public String treeTitle() { return "YUAN DI"; }

    @Override
    public List<String> treeColumns() {
        return List.of("Transform", "Sovereign", "Breath", "Smite", "Night", "Body", "Qi",
                "Gravity", "Step", "Dominion");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "yd_breath", "Demon's Breath", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "yd_smite", "Primordial Smite", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "yd_gravity", "Abyssal Gravity", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "yd_transform", "Primordial Form", AbilityType.TOGGLE, 2));
    }

    /** Shift + V: Primordial Step, his fourth (see PlayableCharacter#activate for the layout). */
    @Override
    public List<AbilitySlot> shiftBar(Set<String> unlocked) {
        return PlayableCharacter.shiftPage(AbilitySlot.fromBranch(skillTree(), unlocked, "yd_step", "Primordial Step", AbilityType.TOGGLE),
                AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY);
    }

    @Override
    public void use(ServerPlayer player, String abilityId, AbilityAction action) {
        switch (abilityId) {
            case "yd_transform" -> {
                // Sovereign's Command fires innately on activation
                if (action == AbilityAction.USE) YuanDiAbilityHandler.toggleTransform(player);
            }
            case "yd_breath" -> {
                if (action == AbilityAction.START) YuanDiAbilityHandler.startBreath(player);
                else if (action == AbilityAction.STOP) YuanDiAbilityHandler.stopBreath(player);
            }
            case "yd_smite" -> { if (action == AbilityAction.USE) YuanDiAbilityHandler.usePrimordialSmite(player); }
            case "yd_gravity" -> { if (action == AbilityAction.USE) YuanDiAbilityHandler.useAbyssalGravity(player); }
            case "yd_step" -> { if (action == AbilityAction.USE) YuanDiAbilityHandler.usePrimordialStep(player); }
            default -> {}
        }
    }
}
