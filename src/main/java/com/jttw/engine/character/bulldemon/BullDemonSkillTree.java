package com.jttw.engine.character.bulldemon;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.List;

/**
 * Skill tree for the Bull Demon King (YAOGUAI): the Great Sage Who Pacifies Heaven, Wukong's
 * sworn brother, lord of Jade Cloud Mountain. Node ids start {@code bdk_}.
 *
 * <pre>
 * Col:   0 (Bull Rush)    1 (Transforms)   2 (Great White Bull)   3 (Thick Hide)
 * Row 0: Bull Rush I      72 Trans. I      Great White Bull I     Thick Hide I
 * Row 1: Bull Rush II     72 Trans. II     Great White Bull II    Thick Hide II
 * Row 2: Bull Rush III    72 Trans. III    Great White Bull III   Thick Hide III
 * </pre>
 * 42 points for the whole tree, against 20 earned by level 100.
 */
public final class BullDemonSkillTree {

    private BullDemonSkillTree() {}

    public static final String RUSH_1 = "bdk_rush_1";
    public static final String RUSH_2 = "bdk_rush_2";
    public static final String RUSH_3 = "bdk_rush_3";
    public static final String TRANSFORM_1 = "bdk_transform_1";
    public static final String TRANSFORM_2 = "bdk_transform_2";
    public static final String TRANSFORM_3 = "bdk_transform_3";
    public static final String BULL_1 = "bdk_bull_1";
    public static final String BULL_2 = "bdk_bull_2";
    public static final String BULL_3 = "bdk_bull_3";
    public static final String HIDE_1 = "bdk_hide_1";
    public static final String HIDE_2 = "bdk_hide_2";
    public static final String HIDE_3 = "bdk_hide_3";

    public static final SkillTree TREE = new SkillTree("bdk_");

    static {
        // ── BULL RUSH (col 0, HELD) ─────────────────────────────────────
        TREE.add(new SkillNode(RUSH_1, "Bull Rush I",
                new String[]{ "Hold V: charge where you", "look, goring all in the way.", "Ram a wall to stun those near." },
                2, 0, 0, List.of(), AbilityType.HELD, 1, "Bull Rush"));
        TREE.add(new SkillNode(RUSH_2, "Bull Rush II",
                new String[]{ "Faster, longer, harder.", "", "" },
                3, 0, 1, List.of(RUSH_1), AbilityType.HELD, 2, "Bull Rush"));
        TREE.add(new SkillNode(RUSH_3, "Bull Rush III",
                new String[]{ "Faster, longer, harder", "still; stuns last longer.", "" },
                4, 0, 2, List.of(RUSH_2), AbilityType.HELD, 3, "Bull Rush"));

        // ── 72 TRANSFORMATIONS (col 1) ──────────────────────────────────
        TREE.add(new SkillNode(TRANSFORM_1, "72 Transformations I",
                new String[]{ "Press M to open the", "72 Transformations.", "Neutral land and water forms." },
                3, 1, 0, List.of(), AbilityType.MENU, 1, "72 Transformations"));
        TREE.add(new SkillNode(TRANSFORM_2, "72 Transformations II",
                new String[]{ "Adds neutral", "flying forms." },
                4, 1, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "72 Transformations"));
        TREE.add(new SkillNode(TRANSFORM_3, "72 Transformations III",
                new String[]{ "Adds hostile", "forms." },
                5, 1, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "72 Transformations"));

        // ── GREAT WHITE BULL (col 2) ────────────────────────────────────
        TREE.add(new SkillNode(BULL_1, "Great White Bull I",
                new String[]{ "Shift + M: become the giant", "white bull. More health,", "harder hits. Drains Qi." },
                3, 2, 0, List.of(), AbilityType.TOGGLE, 1, "Great White Bull"));
        TREE.add(new SkillNode(BULL_2, "Great White Bull II",
                new String[]{ "More health, harder hits.", "", "" },
                4, 2, 1, List.of(BULL_1), AbilityType.TOGGLE, 2, "Great White Bull"));
        TREE.add(new SkillNode(BULL_3, "Great White Bull III",
                new String[]{ "More still.", "", "" },
                5, 2, 2, List.of(BULL_2), AbilityType.TOGGLE, 3, "Great White Bull"));

        // ── THICK HIDE (col 3, PASSIVE) ─────────────────────────────────
        TREE.add(new SkillNode(HIDE_1, "Thick Hide I",
                new String[]{ "Fire cannot burn you.", "Tougher against blows.", "" },
                2, 3, 0, List.of(), AbilityType.PASSIVE, 1, "Thick Hide"));
        TREE.add(new SkillNode(HIDE_2, "Thick Hide II",
                new String[]{ "Tougher against blows", "and magic.", "" },
                3, 3, 1, List.of(HIDE_1), AbilityType.PASSIVE, 2, "Thick Hide"));
        TREE.add(new SkillNode(HIDE_3, "Thick Hide III",
                new String[]{ "Tougher still; hard to", "knock back.", "" },
                4, 3, 2, List.of(HIDE_2), AbilityType.PASSIVE, 3, "Thick Hide"));
    }
}
