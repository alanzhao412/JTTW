package com.jttw.engine.character.turtle;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

/**
 * Skill tree for the World Turtle line (EARTH Divine Beast).
 *
 * <p><b>Innate passive (no skill points):</b> +100 physical resistance at level 100,
 * scaling linearly with level (+1 per level).
 *
 * <p><b>Tree layout — 4 columns, 3 rows max:</b>
 * <pre>
 * Col:  0 (Shell)    1 (Roots)    2 (Bedrock)    3 (Force)    4 (Tectonic)    5 (Gravity)    6 (Patience)
 *
 * Row 0: Shell I     Roots I      Bedrock I      Force I      Tectonic I     Gravity I      Patience I
 * Row 1: Shell II    Roots II     Bedrock II     Force II     Tectonic II    Gravity II     Patience II
 * Row 2: Shell III                               Force III    Tectonic III
 * </pre>
 */
public final class TurtleSkillTree {

    private TurtleSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants
    // -----------------------------------------------------------------------

    public static final String SHELL_1 = "tu_shell_1";
    public static final String SHELL_2 = "tu_shell_2";
    public static final String SHELL_3 = "tu_shell_3";

    public static final String ROOTS_1 = "tu_roots_1";
    public static final String ROOTS_2 = "tu_roots_2";

    public static final String BEDROCK_1 = "tu_bedrock_1";
    public static final String BEDROCK_2 = "tu_bedrock_2";

    public static final String FORCE_1 = "tu_force_1";
    public static final String FORCE_2 = "tu_force_2";
    public static final String FORCE_3 = "tu_force_3";

    public static final String TECTONIC_1 = "tu_tectonic_1";
    public static final String TECTONIC_2 = "tu_tectonic_2";
    public static final String TECTONIC_3 = "tu_tectonic_3";

    public static final String GRAVITY_1 = "tu_gravity_1";
    public static final String GRAVITY_2 = "tu_gravity_2";

    public static final String PATIENCE_1 = "tu_patience_1";
    public static final String PATIENCE_2 = "tu_patience_2";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    public static final String TRANSFORM_1 = "tu_transform_1", TRANSFORM_2 = "tu_transform_2", TRANSFORM_3 = "tu_transform_3";

    public static final SkillTree TREE = new SkillTree("tu_");

    static {
        // ── SHELL GUARD (col 0, HELD type) ──────────────────────────────
        TREE.add(new SkillNode(
                SHELL_1, "Shell Guard I",
                new String[]{ "Hold V: hunker down.", "Greatly reduces damage taken.", "Slows you. Cannot attack." },
                2, 0, 0, List.of(), AbilityType.HELD, 1, "Shell Guard"
        ));
        TREE.add(new SkillNode(
                SHELL_2, "Shell Guard II",
                new String[]{ "Projectiles are completely", "negated while guarding.", "" },
                3, 0, 1, List.of(SHELL_1), AbilityType.HELD, 2, "Shell Guard"
        ));
        TREE.add(new SkillNode(
                SHELL_3, "Shell Guard III",
                new String[]{ "Explosions are completely", "negated while guarding.", "Immovable mountain." },
                4, 0, 2, List.of(SHELL_2), AbilityType.HELD, 3, "Shell Guard"
        ));

        // ── EARTHEN ROOTS (col 1, PASSIVE) ─────────────────────────────
        TREE.add(new SkillNode(
                ROOTS_1, "Earthen Roots I",
                new String[]{ "Rooted: -10% speed, for good.", "-15% damage taken; 15% of", "each hit back to its striker." },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Earthen Roots"
        ));
        TREE.add(new SkillNode(
                ROOTS_2, "Earthen Roots II",
                new String[]{ "-15% speed. -30% damage", "taken; a quarter of each", "hit back to its striker." },
                3, 1, 1, List.of(ROOTS_1), AbilityType.PASSIVE, 2, "Earthen Roots"
        ));

        // ── BEDROCK CONSTITUTION (col 2, PASSIVE) ──────────────────────
        TREE.add(new SkillNode(
                BEDROCK_1, "Bedrock Constitution I",
                new String[]{ "Passive HP regen (+0.5/s).", "+16 phys. resist.", "" },
                2, 2, 0, List.of(), AbilityType.PASSIVE, 1, "Bedrock Constitution"
        ));
        TREE.add(new SkillNode(
                BEDROCK_2, "Bedrock Constitution II",
                new String[]{ "Stronger regen (+1.0/s).", "+32 phys. resist.", "Immune to knockback." },
                3, 2, 1, List.of(BEDROCK_1), AbilityType.PASSIVE, 2, "Bedrock Constitution"
        ));

        // ── UNSTOPPABLE FORCE (col 3, TOGGLE/USE type) ───────────────────
        TREE.add(new SkillNode(
                FORCE_1, "Unstoppable Force I",
                new String[]{ "Press B: dash 20 blocks", "forward. Explodes on hit,", "damage scales with phys. resist." },
                2, 3, 0, List.of(), AbilityType.TOGGLE, 1, "Unstoppable Force"
        ));
        TREE.add(new SkillNode(
                FORCE_2, "Unstoppable Force II",
                new String[]{ "Increased knockback range.", "Enemies within 35 blocks", "are blasted away." },
                3, 3, 1, List.of(FORCE_1), AbilityType.TOGGLE, 2, "Unstoppable Force"
        ));
        TREE.add(new SkillNode(
                FORCE_3, "Unstoppable Force III",
                new String[]{ "Max knockback (50 blocks).", "Enemies are stunned on hit.", "Immovable meets unstoppable." },
                4, 3, 2, List.of(FORCE_2), AbilityType.TOGGLE, 3, "Unstoppable Force"
        ));

        // ── TECTONIC SLAM (col 4, USE active) ───────────────────────────
        TREE.add(new SkillNode(
                TECTONIC_1, "Tectonic Slam I",
                new String[]{ "Press N: stomp the ground.", "6-block AoE. Damage scales", "with phys. resist. 10s CD." },
                2, 4, 0, List.of(), AbilityType.TOGGLE, 1, "Tectonic Slam"
        ));
        TREE.add(new SkillNode(
                TECTONIC_2, "Tectonic Slam II",
                new String[]{ "Creates earth spikes that", "linger 5s. Mobs walking", "over them take damage." },
                3, 4, 1, List.of(TECTONIC_1), AbilityType.TOGGLE, 2, "Tectonic Slam"
        ));
        TREE.add(new SkillNode(
                TECTONIC_3, "Tectonic Slam III",
                new String[]{ "Stomp launches mobs into", "the air. Fall damage", "applies on landing." },
                4, 4, 2, List.of(TECTONIC_2), AbilityType.TOGGLE, 3, "Tectonic Slam"
        ));

        // ── GRAVITY WELL (col 5, TOGGLE) ────────────────────────────────
        TREE.add(new SkillNode(
                GRAVITY_1, "Gravity Well I",
                new String[]{ "Pull all mobs within 8", "blocks toward you slowly.", "Reverse knockback." },
                2, 5, 0, List.of(), AbilityType.TOGGLE, 1, "Gravity Well"
        ));
        TREE.add(new SkillNode(
                GRAVITY_2, "Gravity Well II",
                new String[]{ "Pulled mobs take 15%", "more damage from all", "sources." },
                3, 5, 1, List.of(GRAVITY_1), AbilityType.TOGGLE, 2, "Gravity Well"
        ));

        // ── MOUNTAIN'S PATIENCE (col 6, PASSIVE) ────────────────────────
        TREE.add(new SkillNode(
                PATIENCE_1, "Mountain's Patience I",
                new String[]{ "Standing still: +5% damage", "per second (max 25%).", "Resets on movement." },
                2, 6, 0, List.of(), AbilityType.PASSIVE, 1, "Mountain's Patience"
        ));
        TREE.add(new SkillNode(
                PATIENCE_2, "Mountain's Patience II",
                new String[]{ "At max stacks, next hit is", "a guaranteed crit and", "stuns for 2 seconds." },
                3, 6, 1, List.of(PATIENCE_1), AbilityType.PASSIVE, 2, "Mountain's Patience"
        ));

        // ── 18 TRANSFORMATIONS (col 7, MENU) ───────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "18 Transformations I",
                new String[]{ "Shift + V: the 18", "Transformations. Neutral", "land forms." },
                3, 7, 0, List.of(), AbilityType.MENU, 1, "18 Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_2, "18 Transformations II",
                new String[]{ "Adds neutral", "water forms.", "" },
                4, 7, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "18 Transformations"
        ));
        TREE.add(new SkillNode(
                TRANSFORM_3, "18 Transformations III",
                new String[]{ "Adds neutral", "flying forms.", "" },
                5, 7, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "18 Transformations"
        ));
    }
}
