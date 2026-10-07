package com.jttw.engine.character.byakko;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill tree for the Byakko line (BEAST Divine Beast).
 *
 * <p><b>Innate passives (no skill points):</b>
 * <ul>
 *   <li>Run speed: +0.5% per level (+50% at level 100)</li>
 *   <li>Step assist: +0.01 per level (+1.0 block at level 100)</li>
 *   <li>Raw diet: eating uncooked foods removes negative effects</li>
 * </ul>
 *
 * <p><b>Tree layout — 4 columns:</b>
 * <pre>
 * Col:  0 (Leap)     1 (Scent)    2 (Bleed)     3 (Velocity)   4 (Instinct)     5 (Roar)
 *
 * Row 0: Leap I      Scent I      Bleed I       Velocity I     Instinct I       Roar I
 * Row 1: Leap II     Scent II     Bleed II      Velocity II    Instinct II      Roar II
 * Row 2: Leap III                 Bleed III
 * </pre>
 */
public final class ByakkoSkillTree {

    private ByakkoSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants
    // -----------------------------------------------------------------------

    public static final String LEAP_1 = "bk_leap_1";
    public static final String LEAP_2 = "bk_leap_2";
    public static final String LEAP_3 = "bk_leap_3";

    public static final String SCENT_1 = "bk_scent_1";
    public static final String SCENT_2 = "bk_scent_2";

    public static final String BLEED_1 = "bk_bleed_1";
    public static final String BLEED_2 = "bk_bleed_2";
    public static final String BLEED_3 = "bk_bleed_3";

    public static final String VELOCITY_1 = "bk_velocity_1";
    public static final String VELOCITY_2 = "bk_velocity_2";

    public static final String INSTINCT_1 = "bk_instinct_1";
    public static final String INSTINCT_2 = "bk_instinct_2";

    public static final String ROAR_1 = "bk_roar_1";
    public static final String ROAR_2 = "bk_roar_2";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    static {
        // ── PREDATOR'S LEAP (col 0, USE type) ─────────────────────────────
        reg(new SkillNode(
                LEAP_1, "Predator's Leap I",
                new String[]{ "Press V: launch forward.", "AoE explosion on landing.", "No terrain damage." },
                2, 0, 0, List.of(), AbilityType.TOGGLE, 1, "Predator's Leap"
        ));
        reg(new SkillNode(
                LEAP_2, "Predator's Leap II",
                new String[]{ "Larger explosion radius.", "Increased damage.", "" },
                3, 0, 1, List.of(LEAP_1), AbilityType.TOGGLE, 2, "Predator's Leap"
        ));
        reg(new SkillNode(
                LEAP_3, "Predator's Leap III",
                new String[]{ "Massive AoE on landing.", "Briefly stuns enemies.", "Apex predator." },
                4, 0, 2, List.of(LEAP_2), AbilityType.TOGGLE, 3, "Predator's Leap"
        ));

        // ── SCENT OF BLOOD (col 1, PASSIVE) ──────────────────────────────
        reg(new SkillNode(
                SCENT_1, "Scent of Blood I",
                new String[]{ "+15% damage to targets", "below 50% HP.", "" },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Scent of Blood"
        ));
        reg(new SkillNode(
                SCENT_2, "Scent of Blood II",
                new String[]{ "+30% damage to targets", "below 50% HP.", "" },
                3, 1, 1, List.of(SCENT_1), AbilityType.PASSIVE, 2, "Scent of Blood"
        ));

        // ── SAVAGE BLEED (col 2, passive enhancement) ─────────────────────
        reg(new SkillNode(
                BLEED_1, "Savage Bleed I",
                new String[]{ "Attacks apply bleed.", "Bleed scales with STR.", "" },
                2, 2, 0, List.of(), AbilityType.PASSIVE, 1, "Savage Bleed"
        ));
        reg(new SkillNode(
                BLEED_2, "Savage Bleed II",
                new String[]{ "Stronger bleed damage.", "Longer duration.", "" },
                3, 2, 1, List.of(BLEED_1), AbilityType.PASSIVE, 2, "Savage Bleed"
        ));
        reg(new SkillNode(
                BLEED_3, "Savage Bleed III",
                new String[]{ "Maximum bleed power.", "Spreads to nearby enemies.", "" },
                4, 2, 2, List.of(BLEED_2), AbilityType.PASSIVE, 3, "Savage Bleed"
        ));

        // ── VELOCITY STRIKE (col 3, passive enhancement) ──────────────────
        reg(new SkillNode(
                VELOCITY_1, "Velocity Strike I",
                new String[]{ "Attacks deal bonus damage", "based on movement speed.", "" },
                2, 3, 0, List.of(), AbilityType.PASSIVE, 1, "Velocity Strike"
        ));
        reg(new SkillNode(
                VELOCITY_2, "Velocity Strike II",
                new String[]{ "Doubled speed bonus.", "The faster you move,", "the harder you hit." },
                3, 3, 1, List.of(VELOCITY_1), AbilityType.PASSIVE, 2, "Velocity Strike"
        ));

        // ── PREDATOR'S INSTINCT (col 4, PASSIVE) ────────────────────────
        reg(new SkillNode(
                INSTINCT_1, "Predator's Instinct I",
                new String[]{ "Wounded mobs (<50% HP)", "are highlighted through", "walls within 20 blocks." },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Predator's Instinct"
        ));
        reg(new SkillNode(
                INSTINCT_2, "Predator's Instinct II",
                new String[]{ "First hit on a highlighted", "target deals 2x damage.", "" },
                3, 4, 1, List.of(INSTINCT_1), AbilityType.PASSIVE, 2, "Predator's Instinct"
        ));

        // ── TERRITORIAL ROAR (col 5, USE active) ─────────────────────────
        reg(new SkillNode(
                ROAR_1, "Territorial Roar I",
                new String[]{ "Press B: all mobs within", "8 blocks flee for 3s.", "30s cooldown." },
                2, 5, 0, List.of(), AbilityType.TOGGLE, 1, "Territorial Roar"
        ));
        reg(new SkillNode(
                ROAR_2, "Territorial Roar II",
                new String[]{ "Roar strips buffs from", "enemies. Grants +20%", "speed for 5 seconds." },
                3, 5, 1, List.of(ROAR_1), AbilityType.TOGGLE, 2, "Territorial Roar"
        ));
    }

    private static void reg(SkillNode node) {
        NODES.put(node.id(), node);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public static Map<String, SkillNode> getNodes() {
        return Collections.unmodifiableMap(NODES);
    }

    public static SkillNode get(String id) {
        return NODES.get(id);
    }

    public static List<SkillNode> getAllOrdered() {
        List<SkillNode> list = new ArrayList<>(NODES.values());
        list.sort((a, b) -> a.col() != b.col() ? Integer.compare(a.col(), b.col())
                                                 : Integer.compare(a.row(), b.row()));
        return list;
    }

    public static int getHighestTier(java.util.Set<String> unlockedNodeIds, String idPrefix) {
        int tier = 0;
        for (SkillNode node : NODES.values()) {
            if (node.id().startsWith(idPrefix) && unlockedNodeIds.contains(node.id())) {
                tier = Math.max(tier, node.tier());
            }
        }
        return tier;
    }
}
