package com.jttw.engine.character.erlang;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationSchool;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** Erlang Shen, whose outcome is the Eye of Heaven. */
public final class ErlangShenCharacter implements PlayableCharacter {

    public static final ErlangShenCharacter INSTANCE = new ErlangShenCharacter();

    private ErlangShenCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.EYE_OF_HEAVEN.id; }
    @Override public SkillTree skillTree() { return ErlangShenSkillTree.TREE; }
    @Override public String treeTitle() { return "ERLANG SHEN"; }

    @Override
    public List<String> treeColumns() {
        return List.of("Transforms", "Xiaotian Quan", "Eye of Heaven", "Flight", "Hunt",
                "Hound Growth", "Zhàn Shén", "Tactics", "Spear");
    }

    @Override public TransformationSchool transformationSchool() { return TransformationSchool.SEVENTY_TWO; }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "es_transform");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "es_xtq", "Xiaotian Quan", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "es_eye", "Eye of Heaven", AbilityType.TOGGLE),
                AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(tree, unlocked, "es_transform", "72 Transformations", AbilityType.MENU));
    }

    @Override
    public void use(ServerPlayer player, String abilityId, AbilityAction action) {
        switch (abilityId) {
            case "es_xtq" -> { if (action == AbilityAction.USE) XiaotianQuanManager.toggle(player); }
            case "es_eye" -> { if (action == AbilityAction.USE) ErlangShenAbilityHandler.toggleEye(player); }
            default -> {}
        }
    }
}
