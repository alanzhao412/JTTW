package com.jttw.engine.character.byakko;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

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

    public static final String TRANSFORM_1 = "bk_transform_1", TRANSFORM_2 = "bk_transform_2", TRANSFORM_3 = "bk_transform_3";

    public static final String TRUE_1 = "bk_true_1", TRUE_2 = "bk_true_2", TRUE_3 = "bk_true_3";

    public static final SkillTree TREE = new SkillTree("bk_");

    static {
        // ── PREDATOR'S LEAP (col 0, USE type) ─────────────────────────────
        TREE.add(new SkillNode(
                LEAP_1, "Predator's Leap I",
                new String[]{ "Press V: pounce ~15 blocks,", "raking all you pass, and", "crash down where you land." },
                2, 0, 0, List.of(), AbilityType.TOGGLE, 1, "Predator's Leap"
        ));
        TREE.add(new SkillNode(
                LEAP_2, "Predator's Leap II",
                new String[]{ "Larger explosion radius.", "Increased damage.", "" },
                3, 0, 1, List.of(LEAP_1), AbilityType.TOGGLE, 2, "Predator's Leap"
        ));
        TREE.add(new SkillNode(
                LEAP_3, "Predator's Leap III",
                new String[]{ "Massive AoE on landing.", "Briefly stuns enemies.", "Apex predator." },
                4, 0, 2, List.of(LEAP_2), AbilityType.TOGGLE, 3, "Predator's Leap"
        ));

        // ── SCENT OF BLOOD (col 1, PASSIVE) ──────────────────────────────
        TREE.add(new SkillNode(
                SCENT_1, "Scent of Blood I",
                new String[]{ "+15% damage to targets", "below 50% HP.", "" },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Scent of Blood"
        ));
        TREE.add(new SkillNode(
                SCENT_2, "Scent of Blood II",
                new String[]{ "+30% damage to targets", "below 50% HP.", "" },
                3, 1, 1, List.of(SCENT_1), AbilityType.PASSIVE, 2, "Scent of Blood"
        ));

        // ── SAVAGE BLEED (col 2, passive enhancement) ─────────────────────
        TREE.add(new SkillNode(
                BLEED_1, "Savage Bleed I",
                new String[]{ "Attacks apply bleed.", "Bleed scales with STR.", "" },
                2, 2, 0, List.of(), AbilityType.PASSIVE, 1, "Savage Bleed"
        ));
        TREE.add(new SkillNode(
                BLEED_2, "Savage Bleed II",
                new String[]{ "Stronger bleed damage.", "Longer duration.", "" },
                3, 2, 1, List.of(BLEED_1), AbilityType.PASSIVE, 2, "Savage Bleed"
        ));
        TREE.add(new SkillNode(
                BLEED_3, "Savage Bleed III",
                new String[]{ "Maximum bleed power.", "Spreads to nearby enemies.", "" },
                4, 2, 2, List.of(BLEED_2), AbilityType.PASSIVE, 3, "Savage Bleed"
        ));

        // ── VELOCITY STRIKE (col 3, passive enhancement) ──────────────────
        TREE.add(new SkillNode(
                VELOCITY_1, "Velocity Strike I",
                new String[]{ "Attacks deal bonus damage", "based on movement speed.", "" },
                2, 3, 0, List.of(), AbilityType.PASSIVE, 1, "Velocity Strike"
        ));
        TREE.add(new SkillNode(
                VELOCITY_2, "Velocity Strike II",
                new String[]{ "Doubled speed bonus.", "The faster you move,", "the harder you hit." },
                3, 3, 1, List.of(VELOCITY_1), AbilityType.PASSIVE, 2, "Velocity Strike"
        ));

        // ── PREDATOR'S INSTINCT (col 4, PASSIVE) ────────────────────────
        TREE.add(new SkillNode(
                INSTINCT_1, "Predator's Instinct I",
                new String[]{ "Wounded mobs (<50% HP)", "are highlighted through", "walls within 20 blocks." },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Predator's Instinct"
        ));
        TREE.add(new SkillNode(
                INSTINCT_2, "Predator's Instinct II",
                new String[]{ "First hit on a highlighted", "target deals 2x damage.", "" },
                3, 4, 1, List.of(INSTINCT_1), AbilityType.PASSIVE, 2, "Predator's Instinct"
        ));

        // ── TERRITORIAL ROAR (col 5, USE active) ─────────────────────────
        TREE.add(new SkillNode(
                ROAR_1, "Territorial Roar I",
                new String[]{ "Press B: all within 8", "blocks are pinned 3 s and", "thrown back. 30 s rest." },
                2, 5, 0, List.of(), AbilityType.TOGGLE, 1, "Territorial Roar"
        ));
        TREE.add(new SkillNode(
                ROAR_2, "Territorial Roar II",
                new String[]{ "Roar strips buffs from", "enemies. Grants +20%", "speed for 5 seconds." },
                3, 5, 1, List.of(ROAR_1), AbilityType.TOGGLE, 2, "Territorial Roar"
        ));

        // ── 18 TRANSFORMATIONS (col 6, MENU) ───────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "18 Transformations I",
                new String[]{ "Press N: the 18", "Transformations. Neutral", "land forms." },
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

        // ── TRUE FORM (col 7, TOGGLE) ──────────────────────────────────
        TREE.add(new SkillNode(
                TRUE_1, "White Tiger I",
                new String[]{ "Press M: become the White", "Tiger, 20 s. x3 health, x2.5", "attack. Then exhausted 1 min." },
                4, 7, 0, List.of(), AbilityType.TOGGLE, 1, "White Tiger"
        ));
        TREE.add(new SkillNode(
                TRUE_2, "White Tiger II",
                new String[]{ "30 s as the White Tiger.", "Its pounce goes further", "and strikes twice as hard." },
                5, 7, 1, List.of(TRUE_1), AbilityType.TOGGLE, 2, "White Tiger"
        ));
        TREE.add(new SkillNode(
                TRUE_3, "White Tiger III",
                new String[]{ "45 s as the White Tiger.", "Its roar carries twice", "as far. 10 min rest." },
                6, 7, 2, List.of(TRUE_2), AbilityType.TOGGLE, 3, "White Tiger"
        ));
    }
}
