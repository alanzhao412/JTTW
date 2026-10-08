package com.jttw.engine.character.marshal;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.List;

/**
 * Skill tree for the Marshal of the Heavenly Canopy (HEAVENLY): Zhu Bajie before his fall,
 * admiral of the Milky Way's fleet. Node ids start {@code mc_}.
 *
 * <pre>
 * Col:   0 (Belly Slam)   1 (Truffle Snout)  2 (Transforms)  3 (Stomach)        4 (Admiral)
 * Row 0: Belly Slam I     Snout I            36 Trans. I     Bottomless I      Admiral I
 * Row 1: Belly Slam II    Snout II           36 Trans. II    Bottomless II     Admiral II
 * Row 2: Belly Slam III   Snout III          36 Trans. III   Bottomless III    Admiral III
 * </pre>
 * 48 points for the whole tree, against 20 earned by level 100.
 */
public final class MarshalSkillTree {

    private MarshalSkillTree() {}

    public static final String SLAM_1 = "mc_slam_1";
    public static final String SLAM_2 = "mc_slam_2";
    public static final String SLAM_3 = "mc_slam_3";
    public static final String SNOUT_1 = "mc_snout_1";
    public static final String SNOUT_2 = "mc_snout_2";
    public static final String SNOUT_3 = "mc_snout_3";
    public static final String TRANSFORM_1 = "mc_transform_1";
    public static final String TRANSFORM_2 = "mc_transform_2";
    public static final String TRANSFORM_3 = "mc_transform_3";
    public static final String STOMACH_1 = "mc_stomach_1";
    public static final String STOMACH_2 = "mc_stomach_2";
    public static final String STOMACH_3 = "mc_stomach_3";
    public static final String ADMIRAL_1 = "mc_admiral_1";
    public static final String ADMIRAL_2 = "mc_admiral_2";
    public static final String ADMIRAL_3 = "mc_admiral_3";

    public static final SkillTree TREE = new SkillTree("mc_");

    static {
        // ── BELLY SLAM (col 0) ──────────────────────────────────────────
        TREE.add(new SkillNode(SLAM_1, "Belly Slam I",
                new String[]{ "Press V: leap and crash down.", "The fuller you are, the", "wider and harder the quake." },
                2, 0, 0, List.of(), AbilityType.TOGGLE, 1, "Belly Slam"));
        TREE.add(new SkillNode(SLAM_2, "Belly Slam II",
                new String[]{ "Hits harder; the quaked", "are left staggering.", "" },
                3, 0, 1, List.of(SLAM_1), AbilityType.TOGGLE, 2, "Belly Slam"));
        TREE.add(new SkillNode(SLAM_3, "Belly Slam III",
                new String[]{ "Hits harder still, and", "the quake spreads wider.", "" },
                4, 0, 2, List.of(SLAM_2), AbilityType.TOGGLE, 3, "Belly Slam"));

        // ── TRUFFLE SNOUT (col 1) ───────────────────────────────────────
        TREE.add(new SkillNode(SNOUT_1, "Truffle Snout I",
                new String[]{ "Press B: sniff out every", "creature within 24 blocks,", "seen through walls." },
                2, 1, 0, List.of(), AbilityType.TOGGLE, 1, "Truffle Snout"));
        TREE.add(new SkillNode(SNOUT_2, "Truffle Snout II",
                new String[]{ "Also smells out ores and", "chests within 16 blocks.", "" },
                3, 1, 1, List.of(SNOUT_1), AbilityType.TOGGLE, 2, "Truffle Snout"));
        TREE.add(new SkillNode(SNOUT_3, "Truffle Snout III",
                new String[]{ "32 blocks, and the scent", "lingers twice as long.", "" },
                4, 1, 2, List.of(SNOUT_2), AbilityType.TOGGLE, 3, "Truffle Snout"));

        // ── 36 TRANSFORMATIONS (col 2) ──────────────────────────────────
        TREE.add(new SkillNode(
                TRANSFORM_1, "36 Transformations I",
                new String[]{ "Press M to open the", "36 Transformations.", "Neutral land and water forms." },
                3, 2, 0, List.of(), AbilityType.MENU, 1, "36 Transformations"));
        TREE.add(new SkillNode(
                TRANSFORM_2, "36 Transformations II",
                new String[]{ "Adds neutral", "flying forms." },
                4, 2, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "36 Transformations"));
        TREE.add(new SkillNode(
                TRANSFORM_3, "36 Transformations III",
                new String[]{ "Adds hostile", "land forms." },
                5, 2, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "36 Transformations"));

        // ── BOTTOMLESS STOMACH (col 3, PASSIVE) ─────────────────────────
        TREE.add(new SkillNode(STOMACH_1, "Bottomless Stomach I",
                new String[]{ "A second stomach: twice the", "food bar. Eat past full,", "and the feast heals you." },
                2, 3, 0, List.of(), AbilityType.PASSIVE, 1, "Bottomless Stomach"));
        TREE.add(new SkillNode(STOMACH_2, "Bottomless Stomach II",
                new String[]{ "Overeating heals 1.5x.", "", "" },
                3, 3, 1, List.of(STOMACH_1), AbilityType.PASSIVE, 2, "Bottomless Stomach"));
        TREE.add(new SkillNode(STOMACH_3, "Bottomless Stomach III",
                new String[]{ "Overeating heals 2x; past", "full health it becomes", "golden hearts." },
                4, 3, 2, List.of(STOMACH_2), AbilityType.PASSIVE, 3, "Bottomless Stomach"));

        // ── ADMIRAL OF THE HEAVENLY RIVER (col 4, PASSIVE) ──────────────
        TREE.add(new SkillNode(ADMIRAL_1, "Admiral of the River I",
                new String[]{ "Breathe underwater;", "swim half again as fast.", "" },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Admiral"));
        TREE.add(new SkillNode(ADMIRAL_2, "Admiral of the River II",
                new String[]{ "Swim twice as fast, and", "see clearly underwater.", "" },
                3, 4, 1, List.of(ADMIRAL_1), AbilityType.PASSIVE, 2, "Admiral"));
        TREE.add(new SkillNode(ADMIRAL_3, "Admiral of the River III",
                new String[]{ "Your fleet: players within", "16 blocks swim fast and", "breathe underwater too." },
                4, 4, 2, List.of(ADMIRAL_2), AbilityType.PASSIVE, 3, "Admiral"));
    }
}
