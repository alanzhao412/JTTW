package com.jttw.engine.character.ninespirits;

import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** The Grand Saint of Nine Spirits, the nine-headed lion demon. */
public final class NineSpiritsCharacter implements PlayableCharacter {

    public static final NineSpiritsCharacter INSTANCE = new NineSpiritsCharacter();

    private NineSpiritsCharacter() {}

    /** True if this player is playing the Grand Saint of Nine Spirits. */
    public static boolean is(Player player) {
        return Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS.id; }
    @Override public SkillTree skillTree() { return NineSpiritsSkillTree.TREE; }
    @Override public String treeTitle() { return "GRAND SAINT OF NINE SPIRITS"; }
    @Override public List<String> treeColumns() { return List.of(); }
}
