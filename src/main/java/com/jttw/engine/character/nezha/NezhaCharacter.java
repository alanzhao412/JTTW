package com.jttw.engine.character.nezha;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** Nezha, the Third Lotus Prince. */
public final class NezhaCharacter implements PlayableCharacter {

    public static final NezhaCharacter INSTANCE = new NezhaCharacter();

    private NezhaCharacter() {}

    /** True if this player is playing Nezha (his armaments' special abilities are his alone). */
    public static boolean is(net.minecraft.world.entity.player.Player player) {
        return com.jttw.engine.character.Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.THIRD_LOTUS_PRINCE.id; }
    @Override public SkillTree skillTree() { return NezhaSkillTree.TREE; }
    @Override public String treeTitle() { return "THIRD LOTUS PRINCE"; }

    @Override
    public List<String> treeColumns() {
        return List.of("Armaments", "Samadhi", "Detonation", "Three Heads", "Flight", "Fortitude", "Qi", "Combat");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        // The armaments branch drives the weapon-cycle key, which has its own id
        int armTier = tree.highestTier(unlocked, "nz_arm");
        AbilitySlot cycle = armTier >= 1
                ? new AbilitySlot("nz_cycle", "Weapon Cycle", AbilityType.TOGGLE, armTier)
                : AbilitySlot.EMPTY;
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "nz_fire", "Samadhi Fire", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "nz_det", "Lotus Detonation", AbilityType.HELD),
                cycle,
                AbilitySlot.fromBranch(tree, unlocked, "nz_form", "Three Heads Six Arms", AbilityType.TOGGLE));
    }

    @Override
    public void use(ServerPlayer player, String abilityId, AbilityAction action) {
        switch (abilityId) {
            case "nz_cycle" -> { if (action == AbilityAction.USE) NezhaAbilityHandler.cycleWeapon(player); }
            case "nz_fire" -> {
                if (action == AbilityAction.START) NezhaAbilityHandler.startSamadhiFire(player);
                if (action == AbilityAction.STOP) NezhaAbilityHandler.stopSamadhiFire(player);
            }
            case "nz_det" -> {
                if (action == AbilityAction.START) NezhaAbilityHandler.startDetonationCharge(player);
                if (action == AbilityAction.STOP) NezhaAbilityHandler.stopDetonationCharge(player);
            }
            case "nz_form" -> { if (action == AbilityAction.USE) NezhaAbilityHandler.toggleThreeHeadsSixArms(player); }
            default -> {}
        }
    }
}
