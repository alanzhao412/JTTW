package com.jttw.engine.character.saurian;

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
 * The Saurian Demon King, the Flood-Dragon: Riptide Surge on V (hold and let go), Tidal Lash on
 * B, Whirlpool on N, Flood-Dragon's Wrath on M; and, holding Shift, the 18 Transformations on V.
 * See {@link SaurianAbilityHandler}.
 */
public final class SaurianCharacter implements PlayableCharacter {

    public static final SaurianCharacter INSTANCE = new SaurianCharacter();

    private SaurianCharacter() {}

    /** True if this player is playing the Saurian Demon King. */
    public static boolean is(Player player) {
        return Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.SAURIAN_DEMON_KING.id; }
    @Override public SkillTree skillTree() { return SaurianSkillTree.TREE; }
    @Override public String treeTitle() { return "SAURIAN DEMON KING"; }
    @Override public List<String> treeColumns() {
        return List.of("Riptide Surge", "Tidal Lash", "Whirlpool", "Flood-Dragon's Wrath", "Riverborn", "Scaled Hide", "Venom Fangs", "18 Transformations");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "sdk_surge", "Riptide Surge", AbilityType.HELD),
                AbilitySlot.fromBranch(tree, unlocked, "sdk_lash", "Tidal Lash", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "sdk_whirl", "Whirlpool", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "sdk_transform", "18 Transformations", AbilityType.MENU));
    }

    /** Shift + V: the 18 Transformations (a menu, opened client-side). */
    @Override public TransformationSchool transformationSchool() { return TransformationSchool.EIGHTEEN; }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "sdk_transform");
    }

    /** Shift + V, B, N and M (see PlayableCharacter#activate for the layout). */
    @Override
    public List<AbilitySlot> shiftBar(Set<String> unlocked) {
        return PlayableCharacter.shiftPage(AbilitySlot.EMPTY, AbilitySlot.EMPTY, AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(skillTree(), unlocked, "sdk_wrath", "Flood-Dragon's Wrath", AbilityType.TOGGLE));
    }

    @Override
    public void use(ServerPlayer player, String abilityId, AbilityAction action) {
        switch (abilityId) {
            case "sdk_surge" -> {
                if (action == AbilityAction.START) SaurianAbilityHandler.startCharge(player);
                else if (action == AbilityAction.STOP) SaurianAbilityHandler.release(player);
            }
            case "sdk_lash" -> { if (action == AbilityAction.USE) SaurianAbilityHandler.lash(player); }
            case "sdk_whirl" -> { if (action == AbilityAction.USE) SaurianAbilityHandler.whirlpool(player); }
            case "sdk_wrath" -> { if (action == AbilityAction.USE) SaurianAbilityHandler.wrath(player); }
            default -> {}
        }
    }
}
