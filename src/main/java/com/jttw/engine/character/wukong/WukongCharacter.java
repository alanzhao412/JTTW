package com.jttw.engine.character.wukong;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationSchool;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** Sun Wukong, the Celestial Primate. */
public final class WukongCharacter implements PlayableCharacter {

    public static final WukongCharacter INSTANCE = new WukongCharacter();

    private WukongCharacter() {}

    /** True if this player is playing Wukong (the story checkpoints and Hell trip are his alone). */
    public static boolean is(net.minecraft.world.entity.player.Player player) {
        return com.jttw.engine.character.Characters.of(player) == INSTANCE;
    }

    @Override public String outcomeId() { return ReincarnationOutcome.CELESTIAL_PRIMATE.id; }
    @Override public SkillTree skillTree() { return WukongSkillTree.TREE; }
    @Override public String treeTitle() { return "WUKONG"; }

    @Override
    public List<String> treeColumns() {
        return List.of("Transforms", "Clone", "Ring", "Vitality", "Qi", "Yao Wang", "Flight", "Martial");
    }

    @Override public TransformationSchool transformationSchool() { return TransformationSchool.SEVENTY_TWO; }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "wu_transform");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "wu_transform", "72 Transformations", AbilityType.MENU),
                // each press summons one more clone
                AbilitySlot.fromBranch(tree, unlocked, "wu_clone", "Shadow Clone", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "wu_ring", "Ring of Fire", AbilityType.HELD));
    }

    /** Slot 3 (M): Fiery Golden Eyes, once the story has granted them. Not a skill-tree ability. */
    public static final AbilitySlot GOLDEN_EYES =
            new AbilitySlot("wu_golden_eyes", "Fiery Golden Eyes", AbilityType.TOGGLE, 1);

    @Override
    public List<AbilitySlot> skillBar(net.minecraft.world.entity.player.Player player) {
        List<AbilitySlot> bar = skillBar(com.jttw.engine.progression.skilltree.SkillTreeData.getUnlockedNodes(player));
        if (!ImmortalItems.hasFieryGoldenEyes(player)) return bar;
        return PlayableCharacter.bar(bar.get(0), bar.get(1), bar.get(2), GOLDEN_EYES);
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        switch (slot) {
            case 1 -> {
                if (action == AbilityAction.USE) WuCloneManager.summon(player);
            }
            case 2 -> {
                if (action == AbilityAction.START) WukongAbilityHandler.startRing(player);
                else if (action == AbilityAction.STOP) WukongAbilityHandler.stopRing(player);
            }
            case 3 -> {
                // Fiery Golden Eyes: sneaking toggles x-ray, otherwise fires the eye laser
                if (action == AbilityAction.USE) {
                    if (player.isShiftKeyDown()) WukongAbilityHandler.toggleGoldenEyesXray(player);
                    else WukongAbilityHandler.fireEyeLaser(player);
                }
            }
            default -> {}
        }
    }
}
