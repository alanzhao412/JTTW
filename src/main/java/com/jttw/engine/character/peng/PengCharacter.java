package com.jttw.engine.character.peng;

import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;

import java.util.List;

/** Gold-Winged Peng. No abilities yet: an empty tree and an empty bar. */
public final class PengCharacter implements PlayableCharacter {

    public static final PengCharacter INSTANCE = new PengCharacter();

    private PengCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.GOLD_WINGED_PENG.id; }
    @Override public SkillTree skillTree() { return PengSkillTree.TREE; }
    @Override public String treeTitle() { return "GOLD WINGED PENG"; }
    @Override public List<String> treeColumns() { return List.of(); }
}
