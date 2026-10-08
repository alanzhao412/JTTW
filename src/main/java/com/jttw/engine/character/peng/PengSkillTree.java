package com.jttw.engine.character.peng;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.List;

/**
 * Skill tree for the Gold-Winged Peng (PRIMORDIAL, Spirit Beast secondary): the great roc of
 * Lion-Camel Ridge, whose one beat of the wings crosses ninety thousand li. He flies from the
 * start; his nodes make the flight a weapon. Node ids start {@code gp_}.
 *
 * <pre>
 * Col:   0 (Flight)        1 (Wingstrike)    2 (Feather Storm)   3 (Eclipse)
 * Row 0: 90,000 Li I       Wingstrike I      Feather Storm I     Eclipse Wings I
 * Row 1: 90,000 Li II      Wingstrike II     Feather Storm II    Eclipse Wings II
 * Row 2: 90,000 Li III     Wingstrike III    Feather Storm III
 * </pre>
 * 32 points for the whole tree, against 20 earned by level 100.
 */
public final class PengSkillTree {

    private PengSkillTree() {}

    public static final String SOAR_1 = "gp_soar_1";
    public static final String SOAR_2 = "gp_soar_2";
    public static final String SOAR_3 = "gp_soar_3";
    public static final String STRIKE_1 = "gp_strike_1";
    public static final String STRIKE_2 = "gp_strike_2";
    public static final String STRIKE_3 = "gp_strike_3";
    public static final String FEATHERS_1 = "gp_feathers_1";
    public static final String FEATHERS_2 = "gp_feathers_2";
    public static final String FEATHERS_3 = "gp_feathers_3";
    public static final String ECLIPSE_1 = "gp_eclipse_1";
    public static final String ECLIPSE_2 = "gp_eclipse_2";

    public static final SkillTree TREE = new SkillTree("gp_");

    static {
        // ── NINETY-THOUSAND-LI FLIGHT (col 0, HELD) ─────────────────────
        TREE.add(new SkillNode(SOAR_1, "Ninety-Thousand-Li I",
                new String[]{ "Hold V: beat your wings and", "accelerate, up to twice", "your flying speed." },
                2, 0, 0, List.of(), AbilityType.HELD, 1, "Flight"));
        TREE.add(new SkillNode(SOAR_2, "Ninety-Thousand-Li II",
                new String[]{ "Up to almost three times", "your flying speed.", "" },
                3, 0, 1, List.of(SOAR_1), AbilityType.HELD, 2, "Flight"));
        TREE.add(new SkillNode(SOAR_3, "Ninety-Thousand-Li III",
                new String[]{ "Over three and a half", "times, faster to reach.", "" },
                4, 0, 2, List.of(SOAR_2), AbilityType.HELD, 3, "Flight"));

        // ── WINGSTRIKE (col 1, PASSIVE) ─────────────────────────────────
        TREE.add(new SkillNode(STRIKE_1, "Wingstrike I",
                new String[]{ "In flight, all you touch", "is struck. The faster you", "fly, the harder it hits." },
                2, 1, 0, List.of(), AbilityType.PASSIVE, 1, "Wingstrike"));
        TREE.add(new SkillNode(STRIKE_2, "Wingstrike II",
                new String[]{ "Strikes harder.", "", "" },
                3, 1, 1, List.of(STRIKE_1), AbilityType.PASSIVE, 2, "Wingstrike"));
        TREE.add(new SkillNode(STRIKE_3, "Wingstrike III",
                new String[]{ "Strikes harder still,", "and wider.", "" },
                4, 1, 2, List.of(STRIKE_2), AbilityType.PASSIVE, 3, "Wingstrike"));

        // ── FEATHER STORM (col 2) ───────────────────────────────────────
        TREE.add(new SkillNode(FEATHERS_1, "Feather Storm I",
                new String[]{ "Press B: a fan of 5 golden", "feathers, on the ground", "or in the air." },
                2, 2, 0, List.of(), AbilityType.TOGGLE, 1, "Feather Storm"));
        TREE.add(new SkillNode(FEATHERS_2, "Feather Storm II",
                new String[]{ "7 feathers, harder.", "", "" },
                3, 2, 1, List.of(FEATHERS_1), AbilityType.TOGGLE, 2, "Feather Storm"));
        TREE.add(new SkillNode(FEATHERS_3, "Feather Storm III",
                new String[]{ "9 feathers that pierce", "through all in their way.", "" },
                4, 2, 2, List.of(FEATHERS_2), AbilityType.TOGGLE, 3, "Feather Storm"));

        // ── ECLIPSE WINGS (col 3) ───────────────────────────────────────
        TREE.add(new SkillNode(ECLIPSE_1, "Eclipse Wings I",
                new String[]{ "Press N: your wings blot", "out the sun. All within 10", "blocks are weakened, slowed." },
                3, 3, 0, List.of(), AbilityType.TOGGLE, 1, "Eclipse"));
        TREE.add(new SkillNode(ECLIPSE_2, "Eclipse Wings II",
                new String[]{ "16 blocks, longer; the", "darkened lose their prey", "and glow for you." },
                4, 3, 1, List.of(ECLIPSE_1), AbilityType.TOGGLE, 2, "Eclipse"));
    }
}
