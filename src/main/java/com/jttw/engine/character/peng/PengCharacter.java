package com.jttw.engine.character.peng;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

/** The Gold-Winged Peng: flies from the start; V soars, B throws feathers, N eclipses. */
public final class PengCharacter implements PlayableCharacter {

    public static final PengCharacter INSTANCE = new PengCharacter();

    private PengCharacter() {}

    /** True if this player is playing the Gold-Winged Peng. */
    public static boolean is(Player player) {
        return Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.GOLD_WINGED_PENG.id; }
    @Override public SkillTree skillTree() { return PengSkillTree.TREE; }
    @Override public String treeTitle() { return "GOLD-WINGED PENG"; }
    @Override public List<String> treeColumns() { return List.of("Flight", "Wingstrike", "Feather Storm", "Eclipse"); }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "gp_soar", "Ninety-Thousand-Li Flight", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "gp_feathers", "Feather Storm", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "gp_eclipse", "Eclipse Wings", AbilityType.TOGGLE));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        switch (slot) {
            case 0 -> {
                if (action == AbilityAction.START) PengAbilityHandler.startSoaring(player);
                else if (action == AbilityAction.STOP) PengAbilityHandler.stopSoaring(player);
            }
            case 1 -> { if (action == AbilityAction.USE) PengAbilityHandler.featherStorm(player); }
            case 2 -> { if (action == AbilityAction.USE) PengAbilityHandler.eclipse(player); }
            default -> {}
        }
    }
}
