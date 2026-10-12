package com.jttw.engine.character.phoenix;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

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
 * Col:  0 (Flamethrower)  1 (Life Drain)  2 (Rebirth)   3 (Solar Flare)  4 (Flight)     5 (Molten Talons)   6 (18 Transformations)  7 (Burning Plumage)
 *
 * Row 0: Flamethrower I   Life Drain I    Rebirth I     Solar Flare I    Rising Ember I  Molten Talons I     18 Transformations I    Burning Plumage I
 * Row 1: Flamethrower II  Life Drain II   Rebirth II    Solar Flare II   Rising Ember II Molten Talons II    18 Transformations II   Burning Plumage II
 * Row 2: Flamethrower III                               Solar Flare III                  Molten Talons III   18 Transformations III  Burning Plumage III
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

    public static final String PLUMAGE_1 = "ph_plumage_1", PLUMAGE_2 = "ph_plumage_2", PLUMAGE_3 = "ph_plumage_3";

    public static final String EMBER_1 = "ph_ember_1";
    public static final String EMBER_2 = "ph_ember_2";

    public static final String TALONS_1 = "ph_talons_1";
    public static final String TALONS_2 = "ph_talons_2";
    public static final String TALONS_3 = "ph_talons_3";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    public static final String TRANSFORM_1 = "ph_transform_1", TRANSFORM_2 = "ph_transform_2", TRANSFORM_3 = "ph_transform_3";

    public static final String TRUE_1 = "ph_true_1", TRUE_2 = "ph_true_2", TRUE_3 = "ph_true_3";

    public static final SkillTree TREE = new SkillTree("ph_");

    static {
        // ── FLAMETHROWER (col 0, HELD type) ──────────────────────────────
        TREE.add(new SkillNode(
                FLAME_1, "Flamethrower I",
                new String[]{ "Hold V: breathe fire.", "Range scales with level.", "Cannot move while active." },
                2, 0, 0, List.of(), AbilityType.HELD, 1, "Flamethrower"
        ));
        TREE.add(new SkillNode(
                FLAME_2, "Flamethrower II",
                new String[]{ "Increased damage. Targets", "burn longer. Reduced Qi", "drain." },
                3, 0, 1, List.of(FLAME_1), AbilityType.HELD, 2, "Flamethrower"
        ));
        TREE.add(new SkillNode(
                FLAME_3, "Flamethrower III",
                new String[]{ "Beam widens to 3 blocks", "at max level. Devastating", "inferno." },
                4, 0, 2, List.of(FLAME_2), AbilityType.HELD, 3, "Flamethrower"
        ));

        // ── LIFE DRAIN (col 1, PASSIVE) ──────────────────────────────────
        TREE.add(new SkillNode(
                DRAIN_1, "Life Drain I",
                new String[]{ "Every damaging attack", "heals you for half a", "heart." },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Life Drain"
        ));
        TREE.add(new SkillNode(
                DRAIN_2, "Life Drain II",
                new String[]{ "Every damaging attack", "heals you for one full", "heart." },
                3, 1, 1, List.of(DRAIN_1), AbilityType.PASSIVE, 2, "Life Drain"
        ));

        // ── REBIRTH MASTERY (col 2, PASSIVE) ─────────────────────────────
        TREE.add(new SkillNode(
                REBIRTH_1, "Rebirth Mastery I",
                new String[]{ "Revive explosion radius", "increased to 8 blocks.", "Stronger fire burst." },
                2, 2, 0, List.of(), AbilityType.PASSIVE, 1, "Rebirth Mastery"
        ));
        TREE.add(new SkillNode(
                REBIRTH_2, "Rebirth Mastery II",
                new String[]{ "Revive explosion radius", "increased to 12 blocks.", "Gain 2 revives per week." },
                3, 2, 1, List.of(REBIRTH_1), AbilityType.PASSIVE, 2, "Rebirth Mastery"
        ));

        // ── SOLAR RADIANCE (col 3, TOGGLE aura) ────────────────────────
        TREE.add(new SkillNode(
                RADIANCE_1, "Solar Flare I",
                new String[]{ "Press B: be a sun for 8 s.", "All within 6 blocks burn;", "arrows burn up. 30 s rest." },
                2, 3, 0, List.of(), AbilityType.TOGGLE, 1, "Solar Flare"
        ));
        TREE.add(new SkillNode(
                RADIANCE_2, "Solar Flare II",
                new String[]{ "8 blocks; it burns hotter.", "It ends in a burst that", "blinds all within it." },
                3, 3, 1, List.of(RADIANCE_1), AbilityType.TOGGLE, 2, "Solar Flare"
        ));
        TREE.add(new SkillNode(
                RADIANCE_3, "Solar Flare III",
                new String[]{ "10 blocks, hotter still.", "The undead burn twice", "over." },
                4, 3, 2, List.of(RADIANCE_2), AbilityType.TOGGLE, 3, "Solar Flare"
        ));

        // ── RISING EMBER (col 4, PASSIVE flight) ─────────────────────────
        TREE.add(new SkillNode(
                EMBER_1, "Rising Ember I",
                new String[]{ "Grants permanent flight.", "Hold Space to ascend.", "" },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Rising Ember"
        ));
        TREE.add(new SkillNode(
                EMBER_2, "Rising Ember II",
                new String[]{ "Flight speed greatly", "increased. Immune to all", "fire and lava damage." },
                3, 4, 1, List.of(EMBER_1), AbilityType.PASSIVE, 2, "Rising Ember"
        ));

        // ── MOLTEN TALONS (col 5, PASSIVE combat) ────────────────────────
        TREE.add(new SkillNode(
                TALONS_1, "Molten Talons I",
                new String[]{ "Melee attacks set targets", "on fire for 3 seconds.", "" },
                2, 5, 0, List.of(), AbilityType.PASSIVE, 1, "Molten Talons"
        ));
        TREE.add(new SkillNode(
                TALONS_2, "Molten Talons II",
                new String[]{ "Fire damage you deal heals", "you for 25%.", "" },
                3, 5, 1, List.of(TALONS_1), AbilityType.PASSIVE, 2, "Molten Talons"
        ));
        TREE.add(new SkillNode(
                TALONS_3, "Molten Talons III",
                new String[]{ "Killing a burning enemy", "causes them to explode,", "spreading fire to nearby." },
                4, 5, 2, List.of(TALONS_2), AbilityType.PASSIVE, 3, "Molten Talons"
        ));

        // ── 18 TRANSFORMATIONS (col 6, MENU) ───────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "18 Transformations I",
                new String[]{ "Press M: the 18", "Transformations. Neutral", "land forms." },
                3, 6, 0, List.of(), AbilityType.MENU, 1, "18 Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_2, "18 Transformations II",
                new String[]{ "Adds neutral", "water forms.", "" },
                4, 6, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "18 Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_3, "18 Transformations III",
                new String[]{ "Adds neutral", "flying forms.", "" },
                5, 6, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "18 Transformations"
        ));

        // ── BURNING PLUMAGE (col 7, PASSIVE) ───────────────────────────
        TREE.add(new SkillNode(
                PLUMAGE_1, "Burning Plumage I",
                new String[]{ "Whatever strikes you in", "melee catches fire for", "3 seconds." },
                2, 7, 0, List.of(), AbilityType.PASSIVE, 1, "Burning Plumage"
        ));
        TREE.add(new SkillNode(
                PLUMAGE_2, "Burning Plumage II",
                new String[]{ "It burns for 5 seconds.", "", "" },
                3, 7, 1, List.of(PLUMAGE_1), AbilityType.PASSIVE, 2, "Burning Plumage"
        ));
        TREE.add(new SkillNode(
                PLUMAGE_3, "Burning Plumage III",
                new String[]{ "It takes back twice the", "blow as fire, and is", "weakened for 10 s." },
                4, 7, 2, List.of(PLUMAGE_2), AbilityType.PASSIVE, 3, "Burning Plumage"
        ));

        // ── TRUE FORM (col 8, TOGGLE) ──────────────────────────────────
        TREE.add(new SkillNode(
                TRUE_1, "Vermilion Bird I",
                new String[]{ "Shift + M: become the Vermilion Bird,", "20 s. x3 health, x2.5", "attack. Then exhausted 1 min." },
                4, 8, 0, List.of(), AbilityType.TOGGLE, 1, "Vermilion Bird"
        ));
        TREE.add(new SkillNode(
                TRUE_2, "Vermilion Bird II",
                new String[]{ "30 s. It flies; fire trails", "where it passes, and its", "wingbeats burn all near." },
                5, 8, 1, List.of(TRUE_1), AbilityType.TOGGLE, 2, "Vermilion Bird"
        ));
        TREE.add(new SkillNode(
                TRUE_3, "Vermilion Bird III",
                new String[]{ "45 s as the Vermilion", "Bird. 10 min rest.", "" },
                6, 8, 2, List.of(TRUE_2), AbilityType.TOGGLE, 3, "Vermilion Bird"
        ));
    }
}
