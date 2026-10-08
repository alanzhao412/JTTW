package com.jttw.engine.character.marshal;

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
 * Marshal of the Heavenly Canopy: Belly Slam on V, Truffle Snout on B, and the 36
 * Transformations on M (a menu, opened client-side, so that key never reaches the server).
 */
public final class MarshalCharacter implements PlayableCharacter {

    public static final MarshalCharacter INSTANCE = new MarshalCharacter();

    private MarshalCharacter() {}

    /** True if this player is playing the Marshal of the Heavenly Canopy. */
    public static boolean is(Player player) {
        return Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY.id; }
    @Override public SkillTree skillTree() { return MarshalSkillTree.TREE; }
    @Override public String treeTitle() { return "MARSHAL OF THE HEAVENLY CANOPY"; }
    @Override public List<String> treeColumns() {
        return List.of("Belly Slam", "Truffle Snout", "Transforms", "Stomach", "Admiral");
    }

    @Override public TransformationSchool transformationSchool() { return TransformationSchool.THIRTY_SIX; }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "mc_transform");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "mc_slam", "Belly Slam", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "mc_snout", "Truffle Snout", AbilityType.TOGGLE),
                AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(tree, unlocked, "mc_transform", "36 Transformations", AbilityType.MENU));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        if (action != AbilityAction.USE) return;
        switch (slot) {
            case 0 -> MarshalAbilityHandler.bellySlam(player);
            case 1 -> MarshalAbilityHandler.sniff(player);
            default -> {}
        }
    }
}
