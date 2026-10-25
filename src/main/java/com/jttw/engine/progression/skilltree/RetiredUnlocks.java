package com.jttw.engine.progression.skilltree;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The class abilities that are no more, and the points paid for them.
 *
 * <p>The class layer once gave every class a ladder of abilities, all of which could be bought and
 * none of which did anything. The Heavenly, Primordial and Yaoguai now draw their power from their
 * characters' own skill trees, so their ladders were taken down (all but the Heavenly cloud), and
 * the Divine Beast's three forms became free milestones. A save that still holds one of these ids
 * has it cleared when the player logs in, and gets back what it cost, once: the id is gone after,
 * so nothing is paid twice. The cost is what the old scheme charged ({@link AbilityUnlock#costAt}
 * of its level).
 *
 * <p>Saves from before abilities were bought had them given free at the level; nothing in a save
 * tells the two apart, so those are refunded too.
 */
public final class RetiredUnlocks {

    private RetiredUnlocks() {}

    /** An entry taken off the class layer, or made a free milestone. */
    public record Retired(ProgressionClass cls, int level, String abilityId, String displayName) {
        /** The points it cost to buy. */
        public int refund() {
            return AbilityUnlock.costAt(level);
        }
    }

    private static final List<Retired> ALL = List.of(
            // Divine Beast forms: milestones now, reached for nothing
            new Retired(ProgressionClass.DIVINE_BEAST, 1, AbilityUnlock.LESSER_FORM, "Lesser Form"),
            new Retired(ProgressionClass.DIVINE_BEAST, 40, AbilityUnlock.INTERMEDIATE_FORM, "Intermediate Form"),
            new Retired(ProgressionClass.DIVINE_BEAST, 80, AbilityUnlock.SOVEREIGN_FORM, "Sovereign Form"),
            // Heavenly
            new Retired(ProgressionClass.HEAVENLY, 20, "divine_perception", "Divine Perception"),
            new Retired(ProgressionClass.HEAVENLY, 30, "formation_sense", "Formation Sense"),
            new Retired(ProgressionClass.HEAVENLY, 40, "celestial_support", "Celestial Support"),
            new Retired(ProgressionClass.HEAVENLY, 60, "heavenly_authority", "Heavenly Authority"),
            new Retired(ProgressionClass.HEAVENLY, 80, "eye_of_heaven", "Eye of Heaven"),
            new Retired(ProgressionClass.HEAVENLY, 100, "mandate_of_heaven", "Mandate of Heaven"),
            // Primordial
            new Retired(ProgressionClass.PRIMORDIAL, 1, "primordial_seed", "Primordial Seed"),
            new Retired(ProgressionClass.PRIMORDIAL, 30, "awakening", "Awakening"),
            new Retired(ProgressionClass.PRIMORDIAL, 50, "half_awakening", "Half Awakening"),
            new Retired(ProgressionClass.PRIMORDIAL, 70, "primal_form", "Primal Form"),
            new Retired(ProgressionClass.PRIMORDIAL, 90, "chaos_embodiment", "Chaos Embodiment"),
            new Retired(ProgressionClass.PRIMORDIAL, 100, "primordial_origin", "Primordial Origin"),
            // Yaoguai
            new Retired(ProgressionClass.YAOGUAI, 20, "demon_instinct", "Demon Instinct"),
            new Retired(ProgressionClass.YAOGUAI, 30, "first_spike", "Power Spike I"),
            new Retired(ProgressionClass.YAOGUAI, 50, "demon_transformation", "Demon Transformation"),
            new Retired(ProgressionClass.YAOGUAI, 50, "second_spike", "Power Spike II"),
            new Retired(ProgressionClass.YAOGUAI, 70, "great_demon_body", "Great Demon Body"),
            new Retired(ProgressionClass.YAOGUAI, 70, "third_spike", "Power Spike III"),
            new Retired(ProgressionClass.YAOGUAI, 90, "demon_sovereign", "Demon Sovereign"),
            new Retired(ProgressionClass.YAOGUAI, 90, "fourth_spike", "Power Spike IV"));

    public static List<Retired> all() {
        return ALL;
    }

    /** The retired entries among these held ids, in the list's order. */
    public static List<Retired> heldAmong(Collection<String> held) {
        List<Retired> found = new ArrayList<>();
        for (Retired r : ALL) if (held.contains(r.abilityId())) found.add(r);
        return found;
    }

    /** The points owed for these held ids: what each retired one among them cost. */
    public static int refundFor(Collection<String> held) {
        int points = 0;
        for (Retired r : heldAmong(held)) points += r.refund();
        return points;
    }

    /**
     * Clears every retired id from the player's saved abilities and gives back the points they
     * cost, telling them so. Safe to call on every login: with nothing retired held, it does
     * nothing.
     *
     * @return the points returned
     */
    public static int refund(ServerPlayer player) {
        Set<String> held = PlayerLevel.getAbilities(player);
        List<Retired> found = heldAmong(held);
        // an id saved as not held is cleared too, for nothing
        for (Retired r : ALL) PlayerLevel.revokeAbility(player, r.abilityId());
        if (found.isEmpty()) return 0;

        int points = refundFor(held);
        SkillTreeData.addPoints(player, points);
        List<String> names = found.stream().map(Retired::displayName).toList();
        player.sendSystemMessage(Component.literal("§6[JTTW] §rRetired from the class layer: §e"
                + String.join(", ", names) + "§r. §e" + points + " skill point" + (points == 1 ? "" : "s")
                + " §rreturned. §7(Class power now comes from your own skill tree; the Divine Beast's forms are free milestones.)"));
        return points;
    }
}
