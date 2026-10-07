package com.jttw.engine.character.marshal;

import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;

import java.util.List;
import java.util.Set;

/**
 * Marshal of the Heavenly Canopy. No skill tree; 36 Transformations is innate.
 * Its slot is a menu, opened client-side, so no ability key reaches the server.
 */
public final class MarshalCharacter implements PlayableCharacter {

    public static final MarshalCharacter INSTANCE = new MarshalCharacter();

    private static final SkillTree NO_TREE = new SkillTree("mc_");

    private MarshalCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY.id; }
    @Override public SkillTree skillTree() { return NO_TREE; }
    @Override public String treeTitle() { return "MARSHAL OF THE HEAVENLY CANOPY"; }
    @Override public List<String> treeColumns() { return List.of(); }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        return PlayableCharacter.bar(new AbilitySlot("mc_transform", "36 Transformations", AbilityType.MENU, 1));
    }
}
