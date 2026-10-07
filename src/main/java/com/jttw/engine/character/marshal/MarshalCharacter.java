package com.jttw.engine.character.marshal;

import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationSchool;

import java.util.List;
import java.util.Set;

/**
 * Marshal of the Heavenly Canopy, master of the 36 Transformations. Its slot is a menu,
 * opened client-side, so no ability key reaches the server.
 */
public final class MarshalCharacter implements PlayableCharacter {

    public static final MarshalCharacter INSTANCE = new MarshalCharacter();

    private MarshalCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY.id; }
    @Override public SkillTree skillTree() { return MarshalSkillTree.TREE; }
    @Override public String treeTitle() { return "MARSHAL OF THE HEAVENLY CANOPY"; }
    @Override public List<String> treeColumns() { return List.of("Transforms"); }

    @Override public TransformationSchool transformationSchool() { return TransformationSchool.THIRTY_SIX; }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "mc_transform");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(skillTree(), unlocked, "mc_transform", "36 Transformations", AbilityType.MENU));
    }
}
