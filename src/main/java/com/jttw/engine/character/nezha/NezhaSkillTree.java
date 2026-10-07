package com.jttw.engine.character.nezha;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;
import java.util.List;

/**
 * Defines the Third Lotus Prince (Nezha) skill tree.
 *
 * <pre>
 * Col:  0 (Armaments)   1 (Samadhi Fire)  2 (Detonation)  3 (Three Heads)
 *       4 (Flight)      5 (Fortitude)     6 (Qi)          7 (Combat)
 *
 * Row 0: Arm I           Fire I            Det I           Form I
 *        Flight I        Fort I            Qi I            Combat I
 * Row 1: Arm II          Fire II                           Form II
 *        Flight II       Fort II           Qi II           Combat II
 * Row 2: Arm III         Fire III                                          Combat III
 * </pre>
 */
public final class NezhaSkillTree {

    private NezhaSkillTree() {}

    // Node IDs
    public static final String ARM_1    = "nz_arm_1";
    public static final String ARM_2    = "nz_arm_2";
    public static final String ARM_3    = "nz_arm_3";

    public static final String FIRE_1   = "nz_fire_1";
    public static final String FIRE_2   = "nz_fire_2";
    public static final String FIRE_3   = "nz_fire_3";

    public static final String DET_1    = "nz_det_1";

    public static final String FORM_1   = "nz_form_1";
    public static final String FORM_2   = "nz_form_2";

    public static final String FLIGHT_1 = "nz_flight_1";
    public static final String FLIGHT_2 = "nz_flight_2";

    public static final String FORT_1   = "nz_fort_1";
    public static final String FORT_2   = "nz_fort_2";

    public static final String QI_1     = "nz_qi_1";
    public static final String QI_2     = "nz_qi_2";

    public static final String COMBAT_1 = "nz_combat_1";
    public static final String COMBAT_2 = "nz_combat_2";
    public static final String COMBAT_3 = "nz_combat_3";

    public static final SkillTree TREE = new SkillTree("nz_");

    static {
        // ── ARMAMENT MASTERY (col 0) ──────────────────────────────────
        TREE.add(new SkillNode(ARM_1, "Armament Mastery I",
                new String[]{ "Unlock the Fire-Tip Spear", "and Qiankun Ring.", "Weapon cycle enabled." },
                2, 0, 0, List.of(), AbilityType.TOGGLE, 1, "Armaments"));
        TREE.add(new SkillNode(ARM_2, "Armament Mastery II",
                new String[]{ "Unlock Yin-Yang Swords.", "+10% damage with all", "armaments." },
                3, 0, 1, List.of(ARM_1), AbilityType.TOGGLE, 2, "Armaments"));
        TREE.add(new SkillNode(ARM_3, "Armament Mastery III",
                new String[]{ "All armaments +25% damage.", "Qiankun Ring max size 5.", "" },
                4, 0, 2, List.of(ARM_2), AbilityType.TOGGLE, 3, "Armaments"));

        // ── TRUE SAMADHI FIRE (col 1) ─────────────────────────────────
        TREE.add(new SkillNode(FIRE_1, "Samadhi Fire I",
                new String[]{ "Spear shoots fire.", "Costs Qi per cast.", "" },
                3, 1, 0, List.of(), AbilityType.TOGGLE, 1, "Samadhi Fire"));
        TREE.add(new SkillNode(FIRE_2, "Samadhi Fire II",
                new String[]{ "Increased range and burn", "duration. Splash on impact.", "" },
                4, 1, 1, List.of(FIRE_1), AbilityType.TOGGLE, 2, "Samadhi Fire"));
        TREE.add(new SkillNode(FIRE_3, "Samadhi Fire III",
                new String[]{ "Pierces targets. Leaves", "a fire trail behind.", "" },
                5, 1, 2, List.of(FIRE_2), AbilityType.TOGGLE, 3, "Samadhi Fire"));

        // ── LOTUS DETONATION (col 2) ──────────────────────────────────
        TREE.add(new SkillNode(DET_1, "Lotus Detonation",
                new String[]{ "Charge up, then release a", "20-block explosion. Damage", "scales with armament count." },
                5, 2, 0, List.of(ARM_2), AbilityType.HELD, 1, "Detonation"));

        // ── THREE HEADS SIX ARMS (col 3) ──────────────────────────────
        TREE.add(new SkillNode(FORM_1, "Three Heads Six Arms I",
                new String[]{ "Toggle divine form.", "+50% damage while active.", "Drains Qi." },
                4, 3, 0, List.of(), AbilityType.TOGGLE, 1, "Three Heads"));
        TREE.add(new SkillNode(FORM_2, "Three Heads Six Arms II",
                new String[]{ "Detonation becomes 30-block", "radius at 200% damage.", "Fireball becomes barrage of 6." },
                5, 3, 1, List.of(FORM_1), AbilityType.TOGGLE, 2, "Three Heads"));

        // ── FLIGHT (col 4) ────────────────────────────────────────────
        TREE.add(new SkillNode(FLIGHT_1, "Wheel Step",
                new String[]{ "Grants flight while wearing", "Wind and Fire Wheels.", "" },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Flight"));
        TREE.add(new SkillNode(FLIGHT_2, "Blazing Trail",
                new String[]{ "Faster flight speed.", "Fire trail particles.", "" },
                3, 4, 1, List.of(FLIGHT_1), AbilityType.PASSIVE, 2, "Flight"));

        // ── FORTITUDE (col 5) ─────────────────────────────────────────
        TREE.add(new SkillNode(FORT_1, "Lotus Body I",
                new String[]{ "+20 max HP.", "" },
                1, 5, 0, List.of(), AbilityType.PASSIVE, 0, "Fortitude"));
        TREE.add(new SkillNode(FORT_2, "Lotus Body II",
                new String[]{ "+20 max HP.", "" },
                2, 5, 1, List.of(FORT_1), AbilityType.PASSIVE, 0, "Fortitude"));

        // ── QI MASTERY (col 6) ────────────────────────────────────────
        TREE.add(new SkillNode(QI_1, "Heavenly Qi I",
                new String[]{ "+15% Qi regen speed.", "" },
                1, 6, 0, List.of(), AbilityType.PASSIVE, 0, "Qi Mastery"));
        TREE.add(new SkillNode(QI_2, "Heavenly Qi II",
                new String[]{ "+15% Qi regen speed.", "" },
                2, 6, 1, List.of(QI_1), AbilityType.PASSIVE, 0, "Qi Mastery"));

        // ── COMBAT (col 7) ────────────────────────────────────────────
        TREE.add(new SkillNode(COMBAT_1, "Heavenly Warrior I",
                new String[]{ "+15% movement speed.", "" },
                2, 7, 0, List.of(), AbilityType.PASSIVE, 1, "Combat"));
        TREE.add(new SkillNode(COMBAT_2, "Heavenly Warrior II",
                new String[]{ "+20% damage.", "" },
                3, 7, 1, List.of(COMBAT_1), AbilityType.PASSIVE, 2, "Combat"));
        TREE.add(new SkillNode(COMBAT_3, "Heavenly Warrior III",
                new String[]{ "+6 armor. Fire immunity.", "" },
                4, 7, 2, List.of(COMBAT_2), AbilityType.PASSIVE, 3, "Combat"));
    }
}
