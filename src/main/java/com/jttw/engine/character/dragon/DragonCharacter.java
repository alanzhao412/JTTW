package com.jttw.engine.character.dragon;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/** The Dragon, Water line of the Divine Beasts. */
public final class DragonCharacter implements PlayableCharacter {

    public static final DragonCharacter INSTANCE = new DragonCharacter();

    private DragonCharacter() {}

    @Override public String outcomeId() { return ReincarnationOutcome.DRAGON.id; }
    @Override public SkillTree skillTree() { return DragonSkillTree.TREE; }
    @Override public String treeTitle() { return "DRAGON"; }

    @Override
    public List<String> treeColumns() {
        return List.of("Lightning", "Weather", "Flight", "Pressure", "Regen", "Wave", "Scales", "Force", "Transform", "True Form");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "dr_lightning", "Call Thunder", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "dr_weather", "Weather Control", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "dr_wave", "Tidal Wave", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "dr_transform", "18 Transformations", AbilityType.MENU));
    }

    /**
     * Shift + V: Dimension Travel (Flight III). Holding Shift, the scroll wheel picks where it
     * goes (DimensionScrollHandler); V, still holding it, goes.
     */
    @Override
    public List<AbilitySlot> shiftBar(Set<String> unlocked) {
        int flightTier = skillTree().highestTier(unlocked, "dr_flight");
        return PlayableCharacter.bar(
                flightTier >= 3 ? new AbilitySlot("dr_dimension", "Dimension Travel", AbilityType.TOGGLE, flightTier) : AbilitySlot.EMPTY,
                AbilitySlot.fromBranch(skillTree(), unlocked, "dr_true", "Azure Dragon", AbilityType.TOGGLE),
                AbilitySlot.EMPTY, AbilitySlot.EMPTY);
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        if (action != AbilityAction.USE) return;
        switch (slot) {
            case 0 -> DragonAbilityHandler.castLightning(player);
            case 1 -> DragonAbilityHandler.toggleWeather(player);
            case 2 -> DragonAbilityHandler.castTidalWave(player);
            case 4 -> {
                if (SkillTreeData.getDrFlightTier(player) >= 3) DragonAbilityHandler.travelDimension(player);
            }
            case 5 -> com.jttw.engine.character.divinebeast.TrueForm.toggle(player,
                    com.jttw.engine.registry.ModEntities.AZURE_DRAGON.get(),
                    skillTree().highestTier(SkillTreeData.getUnlockedNodes(player), "dr_true"), "dr_true");
            default -> {}
        }
    }

    @Override public com.jttw.engine.transformation.TransformationSchool transformationSchool() {
        return com.jttw.engine.transformation.TransformationSchool.EIGHTEEN;
    }

    @Override
    public int transformationLevel(Set<String> unlocked) {
        return skillTree().highestTier(unlocked, "dr_transform");
    }
}
