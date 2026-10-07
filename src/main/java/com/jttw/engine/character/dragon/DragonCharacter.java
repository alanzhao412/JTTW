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
        return List.of("Lightning", "Weather", "Flight", "Pressure", "Regen");
    }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        // Slot 2 is Tidal Wave once learned; before that, flight tier 3 puts Dimension Travel there
        AbilitySlot slot2 = AbilitySlot.fromBranch(tree, unlocked, "dr_wave", "Tidal Wave", AbilityType.TOGGLE);
        if (slot2.isEmpty()) {
            int flightTier = tree.highestTier(unlocked, "dr_flight");
            if (flightTier >= 3) {
                slot2 = new AbilitySlot("dr_dimension", "Dimension Travel", AbilityType.TOGGLE, flightTier);
            }
        }
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "dr_lightning", "Lightning Strike", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "dr_weather", "Weather Control", AbilityType.TOGGLE),
                slot2);
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        if (action != AbilityAction.USE) return;
        switch (slot) {
            case 0 -> DragonAbilityHandler.castLightning(player);
            case 1 -> DragonAbilityHandler.toggleWeather(player);
            case 2 -> {
                if (SkillTreeData.getDrWaveTier(player) >= 1) DragonAbilityHandler.castTidalWave(player);
                else DragonAbilityHandler.travelDimension(player);
            }
            default -> {}
        }
    }
}
