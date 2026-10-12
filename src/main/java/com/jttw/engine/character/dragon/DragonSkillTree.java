package com.jttw.engine.character.dragon;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

/**
 * Skill tree for the Dragon line (WATER Divine Beast).
 *
 * <p><b>Tree layout — 5 columns, 3 rows max:</b>
 * <pre>
 * Col:  0 (Lightning)  1 (Weather)  2 (Flight)      3 (Pressure)  4 (Recovery)  5 (Tidal Wave)  6 (Scales)  7 (Tidal Force)
 *
 * Row 0: Call Thunder  Rain Call    Elytra Glide    Pressure I    Recovery I    Tidal Wave I    Scales I    Tidal Force I
 * Row 1: Call Thunder  Tempest      Dragon Flight   Pressure II   Recovery II   Tidal Wave II   Scales II   Tidal Force II
 * Row 2: Wrath         —            Dim Travel      —             —             Tidal Wave III  Scales III
 * </pre>
 *
 * <p>Players earn 1 skill point every 5 levels (max 20 at level 100).
 * Dragon gets flight earlier than other classes — Elytra Glide is cheap (cost 1).
 */
public final class DragonSkillTree {

    private DragonSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants
    // -----------------------------------------------------------------------

    public static final String LIGHTNING_1 = "dr_lightning_1";
    public static final String LIGHTNING_2 = "dr_lightning_2";
    public static final String LIGHTNING_3 = "dr_lightning_3";

    public static final String WEATHER_1 = "dr_weather_1";
    public static final String WEATHER_2 = "dr_weather_2";

    public static final String FLIGHT_1 = "dr_flight_1";
    public static final String FLIGHT_2 = "dr_flight_2";
    public static final String FLIGHT_3 = "dr_flight_3";

    public static final String PRESSURE_1 = "dr_pressure_1";
    public static final String PRESSURE_2 = "dr_pressure_2";

    public static final String TIDAL_1 = "dr_tidal_1";
    public static final String TIDAL_2 = "dr_tidal_2";

    public static final String WAVE_1 = "dr_wave_1";
    public static final String WAVE_2 = "dr_wave_2";
    public static final String WAVE_3 = "dr_wave_3";

    public static final String SCALES_1 = "dr_scales_1";
    public static final String SCALES_2 = "dr_scales_2";
    public static final String SCALES_3 = "dr_scales_3";

    public static final String FORCE_1 = "dr_force_1";
    public static final String FORCE_2 = "dr_force_2";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    public static final String TRANSFORM_1 = "dr_transform_1", TRANSFORM_2 = "dr_transform_2", TRANSFORM_3 = "dr_transform_3";

    public static final String TRUE_1 = "dr_true_1", TRUE_2 = "dr_true_2", TRUE_3 = "dr_true_3";

    public static final SkillTree TREE = new SkillTree("dr_");

    static {
        // ── LIGHTNING STRIKE (col 0, USE type) ──────────────────────────
        TREE.add(new SkillNode(
                LIGHTNING_1, "Call Thunder I",
                new String[]{ "Press V: summon lightning", "where you look (5 block range).", "Damage scales with STR." },
                2, 0, 0, List.of(), AbilityType.TOGGLE, 1, "Lightning"
        ));
        TREE.add(new SkillNode(
                LIGHTNING_2, "Call Thunder II",
                new String[]{ "Lightning damage increased.", "Applies Slowness to targets.", "" },
                3, 0, 1, List.of(LIGHTNING_1), AbilityType.TOGGLE, 2, "Lightning"
        ));
        TREE.add(new SkillNode(
                LIGHTNING_3, "Call Thunder III",
                new String[]{ "Summons 3 bolts in a cone.", "Massive STR scaling.", "Sets targets on fire." },
                4, 0, 2, List.of(LIGHTNING_2), AbilityType.TOGGLE, 3, "Lightning"
        ));

        // ── WEATHER CONTROL (col 1, TOGGLE type) ────────────────────────
        TREE.add(new SkillNode(
                WEATHER_1, "Rain Call",
                new String[]{ "Press B: toggle rain.", "Activates your water domain", "buff anywhere." },
                2, 1, 0, List.of(), AbilityType.TOGGLE, 1, "Weather"
        ));
        TREE.add(new SkillNode(
                WEATHER_2, "Tempest",
                new String[]{ "Upgrades rain to thunderstorm.", "Lightning strikes deal +50%", "during your storms." },
                3, 1, 1, List.of(WEATHER_1), AbilityType.TOGGLE, 2, "Weather"
        ));

        // ── FLIGHT (col 2, PASSIVE — auto-grants) ──────────────────────
        TREE.add(new SkillNode(
                FLIGHT_1, "Elytra Glide",
                new String[]{ "Grants elytra-style gliding.", "Glide through the air but", "cannot gain altitude." },
                1, 2, 0, List.of(), AbilityType.PASSIVE, 1, "Flight"
        ));
        TREE.add(new SkillNode(
                FLIGHT_2, "Dragon Flight",
                new String[]{ "Full powered flight.", "Hold Space to ascend.", "Momentum-based physics." },
                3, 2, 1, List.of(FLIGHT_1), AbilityType.PASSIVE, 2, "Flight"
        ));
        TREE.add(new SkillNode(
                FLIGHT_3, "Dimension Travel",
                new String[]{ "Shift + scroll: choose a", "dimension. Shift + V: go", "there. 20 Qi a jump." },
                4, 2, 2, List.of(FLIGHT_2), AbilityType.PASSIVE, 3, "Flight"
        ));

        // ── DRACONIC PRESSURE (col 3, PASSIVE — aura) ──────────────────
        TREE.add(new SkillNode(
                PRESSURE_1, "Draconic Pressure I",
                new String[]{ "Nearby enemies get", "Slowness I (6 block range).", "" },
                2, 3, 0, List.of(), AbilityType.PASSIVE, 1, "Draconic Pressure"
        ));
        TREE.add(new SkillNode(
                PRESSURE_2, "Draconic Pressure II",
                new String[]{ "Aura also applies", "Weakness I. Range +2.", "" },
                3, 3, 1, List.of(PRESSURE_1), AbilityType.PASSIVE, 2, "Draconic Pressure"
        ));

        // ── TIDAL RECOVERY (col 4, PASSIVE) ─────────────────────────────
        TREE.add(new SkillNode(
                TIDAL_1, "Tidal Recovery I",
                new String[]{ "Regen HP in water/rain.", "+15% Qi regen in your", "water domain." },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Tidal Recovery"
        ));
        TREE.add(new SkillNode(
                TIDAL_2, "Tidal Recovery II",
                new String[]{ "Stronger HP regen.", "+30% Qi regen in water", "domain. Always regenerate." },
                3, 4, 1, List.of(TIDAL_1), AbilityType.PASSIVE, 2, "Tidal Recovery"
        ));

        // ── TIDAL WAVE (col 5, USE active) ─────────────────────────────
        TREE.add(new SkillNode(
                WAVE_1, "Tidal Wave I",
                new String[]{ "Press N: cone water blast.", "Pushes mobs 10 blocks.", "Applies Slowness. 15s CD." },
                2, 5, 0, List.of(), AbilityType.TOGGLE, 1, "Tidal Wave"
        ));
        TREE.add(new SkillNode(
                WAVE_2, "Tidal Wave II",
                new String[]{ "Larger cone. Puts out fires.", "Tears away leaves, plants,", "glass and snow in its way." },
                3, 5, 1, List.of(WAVE_1), AbilityType.TOGGLE, 2, "Tidal Wave"
        ));
        TREE.add(new SkillNode(
                WAVE_3, "Tidal Wave III",
                new String[]{ "Hit enemies are trapped", "in a water bubble for 3s.", "They drown and can't move." },
                4, 5, 2, List.of(WAVE_2), AbilityType.TOGGLE, 3, "Tidal Wave"
        ));

        // ── DRAGON SCALES (col 6, PASSIVE defense) ──────────────────────
        TREE.add(new SkillNode(
                SCALES_1, "Dragon Scales I",
                new String[]{ "+10% damage resistance.", "Projectiles deal 25% less", "damage to you." },
                2, 6, 0, List.of(), AbilityType.PASSIVE, 1, "Dragon Scales"
        ));
        TREE.add(new SkillNode(
                SCALES_2, "Dragon Scales II",
                new String[]{ "+20% damage resistance.", "Reflect 15% of melee", "damage back to attacker." },
                3, 6, 1, List.of(SCALES_1), AbilityType.PASSIVE, 2, "Dragon Scales"
        ));
        TREE.add(new SkillNode(
                SCALES_3, "Dragon Scales III",
                new String[]{ "Below 25% HP: scales", "harden. 50% resistance", "for 5s. 60s cooldown." },
                4, 6, 2, List.of(SCALES_2), AbilityType.PASSIVE, 3, "Dragon Scales"
        ));

        // ── TIDAL FORCE (col 7, PASSIVE knockback) ──────────────────────
        TREE.add(new SkillNode(
                FORCE_1, "Tidal Force I",
                new String[]{ "Your attacks deal 3x", "knockback.", "" },
                2, 7, 0, List.of(), AbilityType.PASSIVE, 1, "Tidal Force"
        ));
        TREE.add(new SkillNode(
                FORCE_2, "Tidal Force II",
                new String[]{ "5x knockback. Mobs that", "hit a wall take bonus", "impact damage." },
                3, 7, 1, List.of(FORCE_1), AbilityType.PASSIVE, 2, "Tidal Force"
        ));

        // ── 18 TRANSFORMATIONS (col 8, MENU) ───────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "18 Transformations I",
                new String[]{ "Press M: the 18", "Transformations. Neutral", "land forms." },
                3, 8, 0, List.of(), AbilityType.MENU, 1, "18 Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_2, "18 Transformations II",
                new String[]{ "Adds neutral", "water forms.", "" },
                4, 8, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "18 Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_3, "18 Transformations III",
                new String[]{ "Adds neutral", "flying forms.", "" },
                5, 8, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "18 Transformations"
        ));

        // ── TRUE FORM (col 9, TOGGLE) ──────────────────────────────────
        TREE.add(new SkillNode(
                TRUE_1, "Azure Dragon I",
                new String[]{ "Shift + M: become the Azure Dragon,", "20 s. x3 health, x2.5", "attack. Then exhausted 1 min." },
                4, 9, 0, List.of(), AbilityType.TOGGLE, 1, "Azure Dragon"
        ));
        TREE.add(new SkillNode(
                TRUE_2, "Azure Dragon II",
                new String[]{ "30 s as the Azure Dragon.", "It swims the air; its", "thunder needs no rest." },
                5, 9, 1, List.of(TRUE_1), AbilityType.TOGGLE, 2, "Azure Dragon"
        ));
        TREE.add(new SkillNode(
                TRUE_3, "Azure Dragon III",
                new String[]{ "45 s as the Azure Dragon.", "10 min rest.", "" },
                6, 9, 2, List.of(TRUE_2), AbilityType.TOGGLE, 3, "Azure Dragon"
        ));
    }
}
