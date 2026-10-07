package com.jttw.engine.character.phoenix;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skill tree for the Solar Sovereign line (FIRE Divine Beast).
 *
 * <p><b>Innate passives (no skill points):</b>
 * <ul>
 *   <li>Higher base HP regen than other classes (+0.5/s at level 1, +2.0/s at level 100)</li>
 *   <li>Phoenix Rebirth: at 10% HP, fully restore HP and trigger fire explosion.
 *       Start with 5 revive charges; gain 1 per Minecraft week.</li>
 * </ul>
 *
 * <p><b>Tree layout — 5 columns, 3 rows max:</b>
 * <pre>
 * Col:  0 (Flamethrower)  1 (Life Drain)  2 (Rebirth)   3 (Solar Radiance)  4 (Flight)     5 (Molten Talons)
 *
 * Row 0: Flamethrower I   Life Drain I    Rebirth I     Solar Radiance I    Rising Ember I  Molten Talons I
 * Row 1: Flamethrower II  Life Drain II   Rebirth II    Solar Radiance II   Rising Ember II Molten Talons II
 * Row 2: Flamethrower III                               Solar Radiance III                  Molten Talons III
 * </pre>
 */
public final class PhoenixSkillTree {

    private PhoenixSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants
    // -----------------------------------------------------------------------

    public static final String FLAME_1 = "ph_flame_1";
    public static final String FLAME_2 = "ph_flame_2";
    public static final String FLAME_3 = "ph_flame_3";

    public static final String DRAIN_1 = "ph_drain_1";
    public static final String DRAIN_2 = "ph_drain_2";

    public static final String REBIRTH_1 = "ph_rebirth_1";
    public static final String REBIRTH_2 = "ph_rebirth_2";

    public static final String RADIANCE_1 = "ph_radiance_1";
    public static final String RADIANCE_2 = "ph_radiance_2";
    public static final String RADIANCE_3 = "ph_radiance_3";

    public static final String EMBER_1 = "ph_ember_1";
    public static final String EMBER_2 = "ph_ember_2";

    public static final String TALONS_1 = "ph_talons_1";
    public static final String TALONS_2 = "ph_talons_2";
    public static final String TALONS_3 = "ph_talons_3";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    static {
        // ── FLAMETHROWER (col 0, HELD type) ──────────────────────────────
        reg(new SkillNode(
                FLAME_1, "Flamethrower I",
                new String[]{ "Hold V: breathe fire.", "Range scales with level.", "Cannot move while active." },
                2, 0, 0, List.of(), AbilityType.HELD, 1, "Flamethrower"
        ));
        reg(new SkillNode(
                FLAME_2, "Flamethrower II",
                new String[]{ "Increased damage. Targets", "burn longer. Reduced Qi", "drain." },
                3, 0, 1, List.of(FLAME_1), AbilityType.HELD, 2, "Flamethrower"
        ));
        reg(new SkillNode(
                FLAME_3, "Flamethrower III",
                new String[]{ "Beam widens to 3 blocks", "at max level. Devastating", "inferno." },
                4, 0, 2, List.of(FLAME_2), AbilityType.HELD, 3, "Flamethrower"
        ));

        // ── LIFE DRAIN (col 1, PASSIVE) ──────────────────────────────────
        reg(new SkillNode(
                DRAIN_1, "Life Drain I",
                new String[]{ "Every damaging attack", "heals you for half a", "heart." },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Life Drain"
        ));
        reg(new SkillNode(
                DRAIN_2, "Life Drain II",
                new String[]{ "Every damaging attack", "heals you for one full", "heart." },
                3, 1, 1, List.of(DRAIN_1), AbilityType.PASSIVE, 2, "Life Drain"
        ));

        // ── REBIRTH MASTERY (col 2, PASSIVE) ─────────────────────────────
        reg(new SkillNode(
                REBIRTH_1, "Rebirth Mastery I",
                new String[]{ "Revive explosion radius", "increased to 8 blocks.", "Stronger fire burst." },
                2, 2, 0, List.of(), AbilityType.PASSIVE, 1, "Rebirth Mastery"
        ));
        reg(new SkillNode(
                REBIRTH_2, "Rebirth Mastery II",
                new String[]{ "Revive explosion radius", "increased to 12 blocks.", "Gain 2 revives per week." },
                3, 2, 1, List.of(REBIRTH_1), AbilityType.PASSIVE, 2, "Rebirth Mastery"
        ));

        // ── SOLAR RADIANCE (col 3, TOGGLE aura) ────────────────────────
        reg(new SkillNode(
                RADIANCE_1, "Solar Radiance I",
                new String[]{ "Emit light. Entities that", "melee you catch fire for", "3 seconds." },
                2, 3, 0, List.of(), AbilityType.TOGGLE, 1, "Solar Radiance"
        ));
        reg(new SkillNode(
                RADIANCE_2, "Solar Radiance II",
                new String[]{ "Light radius doubled.", "Fire thorns duration", "increased to 5 seconds." },
                3, 3, 1, List.of(RADIANCE_1), AbilityType.TOGGLE, 2, "Solar Radiance"
        ));
        reg(new SkillNode(
                RADIANCE_3, "Solar Radiance III",
                new String[]{ "Fire thorns deal double", "damage. Burning attackers", "get Weakness I." },
                4, 3, 2, List.of(RADIANCE_2), AbilityType.TOGGLE, 3, "Solar Radiance"
        ));

        // ── RISING EMBER (col 4, PASSIVE flight) ─────────────────────────
        reg(new SkillNode(
                EMBER_1, "Rising Ember I",
                new String[]{ "Grants permanent flight.", "Hold Space to ascend.", "" },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Rising Ember"
        ));
        reg(new SkillNode(
                EMBER_2, "Rising Ember II",
                new String[]{ "Flight speed greatly", "increased. Immune to all", "fire and lava damage." },
                3, 4, 1, List.of(EMBER_1), AbilityType.PASSIVE, 2, "Rising Ember"
        ));

        // ── MOLTEN TALONS (col 5, PASSIVE combat) ────────────────────────
        reg(new SkillNode(
                TALONS_1, "Molten Talons I",
                new String[]{ "Melee attacks set targets", "on fire for 3 seconds.", "" },
                2, 5, 0, List.of(), AbilityType.PASSIVE, 1, "Molten Talons"
        ));
        reg(new SkillNode(
                TALONS_2, "Molten Talons II",
                new String[]{ "Fire damage you deal heals", "you for 25%.", "" },
                3, 5, 1, List.of(TALONS_1), AbilityType.PASSIVE, 2, "Molten Talons"
        ));
        reg(new SkillNode(
                TALONS_3, "Molten Talons III",
                new String[]{ "Killing a burning enemy", "causes them to explode,", "spreading fire to nearby." },
                4, 5, 2, List.of(TALONS_2), AbilityType.PASSIVE, 3, "Molten Talons"
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

    public static int getHighestTier(Set<String> unlockedNodeIds, String idPrefix) {
        int tier = 0;
        for (SkillNode node : NODES.values()) {
            if (node.id().startsWith(idPrefix) && unlockedNodeIds.contains(node.id())) {
                tier = Math.max(tier, node.tier());
            }
        }
        return tier;
    }
}
