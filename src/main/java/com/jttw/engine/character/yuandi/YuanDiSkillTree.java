package com.jttw.engine.character.yuandi;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

/**
 * Skill tree for Yuan Di — the Primordial Emperor, first Demon King.
 *
 * <p><b>Core mechanic — Primordial Transformation:</b>
 * <ul>
 *   <li>T1 (Uncontrollable): auto-triggers at &lt;30% HP. 10s, 30s cooldown.</li>
 *   <li>T2 (Controlled): toggle on X. 60s, 90s recharge.</li>
 *   <li>T3 (Mastered): unlimited, drains Qi/s. All abilities enhanced.</li>
 * </ul>
 *
 * <p><b>Sovereign's Command:</b> innate fear pulse on transformation activation.
 * Enemies are slowed and flee. Higher tiers dominate mobs to fight for you.
 * Scales with Sovereign tree nodes but auto-fires — not a separate button.
 *
 * <p><b>Tree layout — 6 columns, 3 rows max:</b>
 * <pre>
 * Col:  0 (Transform)  1 (Sovereign)  2 (Breath)   3 (Smite)    4 (Night)  5 (Body)  6 (Qi)  7 (Gravity)  8 (Step)  9 (Dominion)
 *
 * Row 0: Awakening     Sovereign I    Breath I     Smite I      Dusk I     Body I    Qi I    Gravity I    Step I    Dominion I
 * Row 1: Controlled    Sovereign II   Breath II    Smite II     Dusk II    Body II   Qi II   Gravity II   Step II   Dominion II
 * Row 2: Mastered      Sovereign III  Breath III   Smite III    Moonlit                      Gravity III
 * </pre>
 */
public final class YuanDiSkillTree {

    private YuanDiSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants
    // -----------------------------------------------------------------------

    // Primordial Transformation (col 0)
    public static final String TRANSFORM_1 = "yd_transform_1";
    public static final String TRANSFORM_2 = "yd_transform_2";
    public static final String TRANSFORM_3 = "yd_transform_3";

    // Sovereign's Command — innate fear on transform activation (col 1)
    public static final String COMMAND_1 = "yd_command_1";
    public static final String COMMAND_2 = "yd_command_2";
    public static final String COMMAND_3 = "yd_command_3";

    // Demon's Breath — smoke beam (col 2)
    public static final String BREATH_1 = "yd_breath_1";
    public static final String BREATH_2 = "yd_breath_2";
    public static final String BREATH_3 = "yd_breath_3";

    // Primordial Smite — ground slam (col 3)
    public static final String SMITE_1 = "yd_smite_1";
    public static final String SMITE_2 = "yd_smite_2";
    public static final String SMITE_3 = "yd_smite_3";

    // Night Power (col 4)
    public static final String NIGHT_1 = "yd_night_1";
    public static final String NIGHT_2 = "yd_night_2";
    public static final String NIGHT_3 = "yd_night_3";

    // Primordial Body + Qi (col 5/6)
    public static final String BODY_1 = "yd_body_1";
    public static final String BODY_2 = "yd_body_2";
    public static final String QI_1   = "yd_qi_1";
    public static final String QI_2   = "yd_qi_2";

    // Abyssal Gravity (col 7)
    public static final String GRAVITY_1 = "yd_gravity_1";
    public static final String GRAVITY_2 = "yd_gravity_2";
    public static final String GRAVITY_3 = "yd_gravity_3";

    // Primordial Step (col 8)
    public static final String STEP_1 = "yd_step_1";
    public static final String STEP_2 = "yd_step_2";

    // Ancient Dominion (col 9)
    public static final String DOMINION_1 = "yd_dominion_1";
    public static final String DOMINION_2 = "yd_dominion_2";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    public static final SkillTree TREE = new SkillTree("yd_");

    static {
        // ── PRIMORDIAL TRANSFORMATION (col 0) ────────────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "Awakening",
                new String[]{ "Primordial form activates", "uncontrollably when HP", "drops below 30%." },
                2, 0, 0, List.of(), AbilityType.PASSIVE, 1, "Transform"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_2, "Controlled Shift",
                new String[]{ "Press M to transform at", "will. 60s duration with a", "recharge timer." },
                3, 0, 1, List.of(TRANSFORM_1), AbilityType.TOGGLE, 2, "Transform"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_3, "Mastered Form",
                new String[]{ "Unlimited transformation.", "Drains Qi per second.", "All abilities enhanced." },
                4, 0, 2, List.of(TRANSFORM_2), AbilityType.TOGGLE, 3, "Transform"
        ));

        // ── SOVEREIGN'S COMMAND (col 1, PASSIVE — innate to Transform) ───
        TREE.add(new SkillNode(
                COMMAND_1, "Sovereign's Command I",
                new String[]{ "Transforming emits a fear", "pulse. 8-block AOE.", "Enemies get Slowness III." },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Sovereign"
        ));
        TREE.add(new SkillNode(
                COMMAND_2, "Sovereign's Command II",
                new String[]{ "12-block fear pulse. Adds", "Weakness II. Hostile mobs", "retarget each other 10s." },
                3, 1, 1, List.of(COMMAND_1), AbilityType.PASSIVE, 2, "Sovereign"
        ));
        TREE.add(new SkillNode(
                COMMAND_3, "Sovereign's Command III",
                new String[]{ "16-block pulse. All non-boss", "mobs fight each other.", "In Form: 24 blocks." },
                4, 1, 2, List.of(COMMAND_2), AbilityType.PASSIVE, 3, "Sovereign"
        ));

        // ── DEMON'S BREATH (col 2, HELD type) ───────────────────────────
        TREE.add(new SkillNode(
                BREATH_1, "Demon's Breath I",
                new String[]{ "Hold V: exhale dark smoke.", "Range scales with level.", "Cannot move while active." },
                2, 2, 0, List.of(), AbilityType.HELD, 1, "Breath"
        ));
        TREE.add(new SkillNode(
                BREATH_2, "Demon's Breath II",
                new String[]{ "Beam applies Wither II.", "Increased damage. Reduced", "Qi drain." },
                3, 2, 1, List.of(BREATH_1), AbilityType.HELD, 2, "Breath"
        ));
        TREE.add(new SkillNode(
                BREATH_3, "Demon's Breath III",
                new String[]{ "Beam widens. Wither III.", "In Primordial Form:", "range +50%, damage x1.5." },
                4, 2, 2, List.of(BREATH_2), AbilityType.HELD, 3, "Breath"
        ));

        // ── PRIMORDIAL SMITE (col 3, USE type) ──────────────────────────
        TREE.add(new SkillNode(
                SMITE_1, "Primordial Smite I",
                new String[]{ "B: Slam the ground. 5-block", "shockwave. Knockback. No", "terrain damage. 20s CD." },
                2, 3, 0, List.of(), AbilityType.TOGGLE, 1, "Smite"
        ));
        TREE.add(new SkillNode(
                SMITE_2, "Primordial Smite II",
                new String[]{ "8-block radius. More damage.", "Lingering slow field for", "5s. 15s cooldown." },
                3, 3, 1, List.of(SMITE_1), AbilityType.TOGGLE, 2, "Smite"
        ));
        TREE.add(new SkillNode(
                SMITE_3, "Primordial Smite III",
                new String[]{ "12-block radius. Enemies", "stunned 2s. In Form:", "18 blocks, x1.5 damage." },
                4, 3, 2, List.of(SMITE_2), AbilityType.TOGGLE, 3, "Smite"
        ));

        // ── NIGHT POWER (col 4) ─────────────────────────────────────────
        TREE.add(new SkillNode(
                NIGHT_1, "Dusk Strength I",
                new String[]{ "At night: +10% damage,", "+10% speed, +8 phys. resist.", "" },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Night"
        ));
        TREE.add(new SkillNode(
                NIGHT_2, "Dusk Strength II",
                new String[]{ "At night: +20% damage,", "+15% speed, +16 phys. resist.", "Night vision." },
                3, 4, 1, List.of(NIGHT_1), AbilityType.PASSIVE, 2, "Night"
        ));
        TREE.add(new SkillNode(
                NIGHT_3, "Moonlit Apex",
                new String[]{ "At night: +30% damage,", "+20% speed, +24 phys. resist.", "HP regen doubles." },
                4, 4, 2, List.of(NIGHT_2), AbilityType.PASSIVE, 3, "Night"
        ));

        // ── PRIMORDIAL BODY + QI (col 5) ─────────────────────────────────
        TREE.add(new SkillNode(
                BODY_1, "Iron Flesh I",
                new String[]{ "+20 max HP. +8 phys. resist.", "10% knockback resist." },
                1, 5, 0, List.of(), AbilityType.PASSIVE, 0, "Body"
        ));
        TREE.add(new SkillNode(
                BODY_2, "Iron Flesh II",
                new String[]{ "+20 max HP. +8 phys. resist.", "25% knockback resist." },
                2, 5, 1, List.of(BODY_1), AbilityType.PASSIVE, 0, "Body"
        ));
        TREE.add(new SkillNode(
                QI_1, "Qi Flow I",
                new String[]{ "+15% Qi regen speed.", "+50 max Qi." },
                1, 6, 0, List.of(), AbilityType.PASSIVE, 0, "Qi"
        ));
        TREE.add(new SkillNode(
                QI_2, "Qi Flow II",
                new String[]{ "+15% Qi regen speed.", "+50 max Qi." },
                2, 6, 1, List.of(QI_1), AbilityType.PASSIVE, 0, "Qi"
        ));

        // ── ABYSSAL GRAVITY (col 7, USE active) ────────────────────────
        TREE.add(new SkillNode(
                GRAVITY_1, "Abyssal Gravity I",
                new String[]{ "Press N: gravity well at", "cursor. Pulls all mobs", "within 8 blocks for 3s." },
                2, 7, 0, List.of(), AbilityType.TOGGLE, 1, "Abyssal Gravity"
        ));
        TREE.add(new SkillNode(
                GRAVITY_2, "Abyssal Gravity II",
                new String[]{ "Mobs in the well take 2", "crushing damage per second.", "Radius 12 blocks." },
                3, 7, 1, List.of(GRAVITY_1), AbilityType.TOGGLE, 2, "Abyssal Gravity"
        ));
        TREE.add(new SkillNode(
                GRAVITY_3, "Abyssal Gravity III",
                new String[]{ "Well collapses into a void", "rift. Everything inside", "takes 30% max HP damage." },
                4, 7, 2, List.of(GRAVITY_2), AbilityType.TOGGLE, 3, "Abyssal Gravity"
        ));

        // ── PRIMORDIAL STEP (col 8, USE mobility) ───────────────────────
        TREE.add(new SkillNode(
                STEP_1, "Primordial Step I",
                new String[]{ "Press N: blink 8 blocks", "in look direction.", "10s cooldown. Costs 5 Qi." },
                2, 8, 0, List.of(), AbilityType.TOGGLE, 1, "Primordial Step"
        ));
        TREE.add(new SkillNode(
                STEP_2, "Primordial Step II",
                new String[]{ "Blink leaves a fear pulse", "at origin. Cooldown", "reduced to 6 seconds." },
                3, 8, 1, List.of(STEP_1), AbilityType.TOGGLE, 2, "Primordial Step"
        ));

        // ── ANCIENT DOMINION (col 9, PASSIVE) ───────────────────────────
        TREE.add(new SkillNode(
                DOMINION_1, "Ancient Dominion I",
                new String[]{ "Mobs you've damaged deal", "15% less damage to you", "for 10 seconds." },
                2, 9, 0, List.of(), AbilityType.PASSIVE, 1, "Ancient Dominion"
        ));
        TREE.add(new SkillNode(
                DOMINION_2, "Ancient Dominion II",
                new String[]{ "Debuffed mobs also deal", "15% less damage to all", "nearby players." },
                3, 9, 1, List.of(DOMINION_1), AbilityType.PASSIVE, 2, "Ancient Dominion"
        ));
    }
}
