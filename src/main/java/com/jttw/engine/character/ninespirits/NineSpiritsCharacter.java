package com.jttw.engine.character.ninespirits;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTree;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

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
    @Override public List<String> treeColumns() { return List.of("Heads", "Roar", "Mane", "True Form", "Nine Lives"); }

    @Override
    public List<AbilitySlot> skillBar(Set<String> unlocked) {
        SkillTree tree = skillTree();
        return PlayableCharacter.bar(
                AbilitySlot.fromBranch(tree, unlocked, "ns_heads", "Heads of Many Elements", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "ns_roar", "Heaven-Shaking Roar", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "ns_flare", "Golden Mane Flare", AbilityType.TOGGLE),
                AbilitySlot.fromBranch(tree, unlocked, "ns_form", "Nine-Headed True Form", AbilityType.TOGGLE));
    }

    @Override
    public void activate(ServerPlayer player, int slot, AbilityAction action) {
        if (action != AbilityAction.USE) return;
        switch (slot) {
            case 0 -> {
                if (player.isShiftKeyDown()) NineSpiritsAbilityHandler.rotateHead(player);
                else NineSpiritsAbilityHandler.fireHead(player);
            }
            case 1 -> NineSpiritsAbilityHandler.roar(player);
            case 2 -> NineSpiritsAbilityHandler.flare(player);
            case 3 -> NineSpiritsAbilityHandler.toggleTrueForm(player);
            default -> {}
        }
    }
}
