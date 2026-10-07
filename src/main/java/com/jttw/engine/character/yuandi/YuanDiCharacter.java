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
                // Tier 1 transformation is automatic; it becomes a key at tier 2
                AbilitySlot.fromBranch(tree, unlocked, "yd_transform", "Primordial Form", AbilityType.TOGGLE, 2),
                AbilitySlot.fromBranch(tree, unlocked, "yd_breath", "Demon's Breath", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "yd_smite", "Primordial Smite", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "yd_gravity", "Abyssal Gravity", AbilityType.TOGGLE));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        switch (slot) {
            case 0 -> {
                // Sovereign's Command fires innately on activation
                if (action == AbilityAction.USE) YuanDiAbilityHandler.toggleTransform(player);
            }
            case 1 -> {
                if (action == AbilityAction.START) YuanDiAbilityHandler.startBreath(player);
                else if (action == AbilityAction.STOP) YuanDiAbilityHandler.stopBreath(player);
            }
            case 2 -> {
                if (action == AbilityAction.USE) YuanDiAbilityHandler.usePrimordialSmite(player);
            }
            case 3 -> {
                if (action == AbilityAction.USE) YuanDiAbilityHandler.useAbyssalGravity(player);
            }
            default -> {}
        }
    }
}
