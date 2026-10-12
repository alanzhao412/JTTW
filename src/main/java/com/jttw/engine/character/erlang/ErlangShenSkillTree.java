package com.jttw.engine.character.erlang;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

/**
 * Defines Erlang Shen's (EYE_OF_HEAVEN) complete skill tree.
 *
 * <p><b>Tree layout — 6 columns, 3 rows max:</b>
 * <pre>
 * Col:    0 (Transforms)  1 (Xiaotian Quan)  2 (Eye of Heaven)  3 (Flight)  4 (Celestial Hunt)  5 (Hound Growth)  6 (Zhàn Shén)
 *
 * Row 0:  Transform I     XTQ I              Eye I              Wind & Fog  Hunt I              Hound Training I  Zhàn Shén I
 * Row 1:  Transform II    XTQ II             Eye II             Wind        Hunt II             Hound Training II Zhàn Shén II
 * Row 2:  Transform III   XTQ III            Eye III            Gale                                              Zhàn Shén III
 * </pre>
 *
 * <p>Players earn 1 skill point every 5 levels (max 20 at level 100).
 */
public final class ErlangShenSkillTree {

    private ErlangShenSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants — use these in gameplay checks
    // -----------------------------------------------------------------------

    public static final String TRANSFORM_1  = "es_transform_1";
    public static final String TRANSFORM_2  = "es_transform_2";
    public static final String TRANSFORM_3  = "es_transform_3";

    public static final String XTQ_1        = "es_xtq_1";
    public static final String XTQ_2        = "es_xtq_2";
    public static final String XTQ_3        = "es_xtq_3";

    public static final String EYE_1        = "es_eye_1";
    public static final String EYE_2        = "es_eye_2";
    public static final String EYE_3        = "es_eye_3";

    public static final String FLIGHT_1     = "es_flight_1";
    public static final String FLIGHT_2     = "es_flight_2";
    public static final String FLIGHT_3     = "es_flight_3";

    public static final String HUNT_1       = "es_hunt_1";
    public static final String HUNT_2       = "es_hunt_2";

    public static final String WRATH_1     = "es_wrath_1";
    public static final String WRATH_2     = "es_wrath_2";
    public static final String WRATH_3     = "es_wrath_3";

    public static final String XTQ_GROWTH_1 = "es_xtq_growth_1";
    public static final String XTQ_GROWTH_2 = "es_xtq_growth_2";
    public static final String XTQ_GROWTH_3 = "es_xtq_growth_3";

    public static final String TACTICS_1 = "es_tactics_1";
    public static final String TACTICS_2 = "es_tactics_2";

    public static final String SPEAR_1  = "es_spear_1";
    public static final String SPEAR_2  = "es_spear_2";
    public static final String SPEAR_3  = "es_spear_3";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    public static final SkillTree TREE = new SkillTree("es_");

    static {
        // ── 72 TRANSFORMATIONS (col 0, MENU type) ─────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "72 Transformations I",
                new String[]{ "Press M to open the", "72 Transformations.",
                              "Neutral land, water and sky forms." },
                3, 0, 0, List.of(), AbilityType.MENU, 1, "Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_2, "72 Transformations II",
                new String[]{ "Adds hostile land forms", "(no bosses)." },
                4, 0, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_3, "72 Transformations III",
                new String[]{ "Adds bosses, hostile sea and", "sky forms, objects and players.",
                              "Transmute equipped items." },
                5, 0, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "Transformations"
        ));

        // ── XIAOTIAN QUAN (col 1, TOGGLE type) ────────────────────────
        TREE.add(new SkillNode(
                XTQ_1, "Xiaotian Quan I",
                new String[]{ "Press V: summon your", "celestial hound companion.",
                              "Costs Qi to maintain." },
                3, 1, 0, List.of(), AbilityType.TOGGLE, 1, "Xiaotian Quan"
        ));
        TREE.add(new SkillNode(
                XTQ_2, "Xiaotian Quan II",
                new String[]{ "Improved companion stats.", "Xiaotian Quan gains",
                              "tracking sense." },
                4, 1, 1, List.of(XTQ_1), AbilityType.TOGGLE, 2, "Xiaotian Quan"
        ));
        TREE.add(new SkillNode(
                XTQ_3, "Xiaotian Quan III",
                new String[]{ "Peak hound power.", "Attacks apply Slowness.",
                              "Enhanced combat partner." },
                5, 1, 2, List.of(XTQ_2), AbilityType.TOGGLE, 3, "Xiaotian Quan"
        ));

        // ── EYE OF HEAVEN (col 2, TOGGLE type) ───────────────────────
        TREE.add(new SkillNode(
                EYE_1, "Eye of Heaven I",
                new String[]{ "Press B: activate the", "Third Eye. See through",
                              "72 Transformations." },
                3, 2, 0, List.of(), AbilityType.TOGGLE, 1, "Eye of Heaven"
        ));
        TREE.add(new SkillNode(
                EYE_2, "Eye of Heaven II",
                new String[]{ "Reveals ores through", "blocks within 16 blocks.",
                              "Color-coded by type." },
                4, 2, 1, List.of(EYE_1), AbilityType.TOGGLE, 2, "Eye of Heaven"
        ));
        TREE.add(new SkillNode(
                EYE_3, "Eye of Heaven III",
                new String[]{ "Extended ore range (32).", "Reduced Qi drain.",
                              "True divine perception." },
                5, 2, 2, List.of(EYE_2), AbilityType.TOGGLE, 3, "Eye of Heaven"
        ));

        // ── FLIGHT (col 3, PASSIVE) ──────────────────────────────────
        // His own: the clouds (the Nimbus, the Somersault Cloud) are Wukong's alone. He leaps
        // through wind and fog (Kuangfeng Shenwu), drawn as a dark squall about him in flight
        TREE.add(new SkillNode(
                FLIGHT_1, "Wind and Fog",
                new String[]{ "Grants permanent flight,", "wrapped in a dark squall.", "Hold Space to ascend." },
                2, 3, 0, List.of(), AbilityType.PASSIVE, 1, "Flight"
        ));
        TREE.add(new SkillNode(
                FLIGHT_2, "Divine Wind",
                new String[]{ "Flight speed greatly", "increased.", "" },
                3, 3, 1, List.of(FLIGHT_1), AbilityType.PASSIVE, 2, "Flight"
        ));
        TREE.add(new SkillNode(
                FLIGHT_3, "Celestial Gale",
                new String[]{ "Flight speed increased", "again: your swiftest.", "" },
                4, 3, 2, List.of(FLIGHT_2), AbilityType.PASSIVE, 3, "Flight"
        ));

        // ── CELESTIAL HUNT (col 4, PASSIVE) ─────────────────────────
        TREE.add(new SkillNode(
                HUNT_1, "Celestial Hunt I",
                new String[]{ "Mark targets on hit.", "You and XTQ deal +15%", "to marked targets." },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Celestial Hunt"
        ));
        TREE.add(new SkillNode(
                HUNT_2, "Celestial Hunt II",
                new String[]{ "+25% to marked targets.", "Marked enemies glow", "through walls." },
                3, 4, 1, List.of(HUNT_1), AbilityType.PASSIVE, 2, "Celestial Hunt"
        ));

        // ── ZHÀN SHÉN / GOD OF WAR (col 6, PASSIVE) ────────────────
        TREE.add(new SkillNode(
                WRATH_1, "Zhàn Shén I",
                new String[]{ "Below 40% HP: enter Wrath.", "+15% damage and speed.", "" },
                2, 6, 0, List.of(), AbilityType.PASSIVE, 1, "Zhàn Shén"
        ));
        TREE.add(new SkillNode(
                WRATH_2, "Zhàn Shén II",
                new String[]{ "Wrath: +25% damage/speed.", "Knockback immunity.", "XTQ enters berserk." },
                3, 6, 1, List.of(WRATH_1), AbilityType.PASSIVE, 2, "Zhàn Shén"
        ));
        TREE.add(new SkillNode(
                WRATH_3, "Zhàn Shén III",
                new String[]{ "Wrath: +35% damage/speed.", "10% lifesteal. XTQ gains", "Slowness II on attacks." },
                4, 6, 2, List.of(WRATH_2), AbilityType.PASSIVE, 3, "Zhàn Shén"
        ));

        // ── HOUND GROWTH PASSIVES (col 5) ────────────────────────────
        TREE.add(new SkillNode(
                XTQ_GROWTH_1, "Hound Training I",
                new String[]{ "Xiaotian Quan gains", "+20 HP, +3 ATK.", "" },
                2, 5, 0, List.of(), AbilityType.PASSIVE, 0, "Hound Growth"
        ));
        TREE.add(new SkillNode(
                XTQ_GROWTH_2, "Hound Training II",
                new String[]{ "Xiaotian Quan gains", "tracking ability. +20 HP.", "" },
                3, 5, 1, List.of(XTQ_GROWTH_1), AbilityType.PASSIVE, 0, "Hound Growth"
        ));
        TREE.add(new SkillNode(
                XTQ_GROWTH_3, "Hound Training III",
                new String[]{ "Peak stats. Attacks", "apply Slowness II.", "" },
                4, 5, 2, List.of(XTQ_GROWTH_2), AbilityType.PASSIVE, 0, "Hound Growth"
        ));

        // ── CELESTIAL TACTICS (col 7, PASSIVE) ──────────────────────────
        TREE.add(new SkillNode(
                TACTICS_1, "Celestial Tactics I",
                new String[]{ "When you and XTQ hit the", "same target within 1s,", "deal +30% bonus damage." },
                2, 7, 0, List.of(), AbilityType.PASSIVE, 1, "Celestial Tactics"
        ));
        TREE.add(new SkillNode(
                TACTICS_2, "Celestial Tactics II",
                new String[]{ "Coordinated strikes apply", "Weakness II and Slowness", "for 2 seconds." },
                3, 7, 1, List.of(TACTICS_1), AbilityType.PASSIVE, 2, "Celestial Tactics"
        ));

        // ── THREE-POINTED SPEAR (col 8, PASSIVE — weapon mastery) ────────
        TREE.add(new SkillNode(
                SPEAR_1, "Spear Mastery I",
                new String[]{ "Sneak, hold, release: throw.", "Tears through terrain up to", "100 blocks. Back in 20s." },
                3, 8, 0, List.of(), AbilityType.PASSIVE, 1, "Spear Mastery"
        ));
        TREE.add(new SkillNode(
                SPEAR_2, "Spear Mastery II",
                new String[]{ "Spear thrusts deal", "25% more damage." },
                4, 8, 1, List.of(SPEAR_1), AbilityType.PASSIVE, 2, "Spear Mastery"
        ));
        TREE.add(new SkillNode(
                SPEAR_3, "Spear Mastery III",
                new String[]{ "A fully charged thrust ends", "in a shockwave that pulls", "enemies inward." },
                5, 8, 2, List.of(SPEAR_2), AbilityType.PASSIVE, 3, "Spear Mastery"
        ));
    }
}
