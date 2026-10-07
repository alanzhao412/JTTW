package com.jttw.engine.character.marshal;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.List;

/** The Marshal of the Heavenly Canopy's skill tree: the 36 Transformations. */
public final class MarshalSkillTree {

    private MarshalSkillTree() {}

    public static final String TRANSFORM_1 = "mc_transform_1";
    public static final String TRANSFORM_2 = "mc_transform_2";
    public static final String TRANSFORM_3 = "mc_transform_3";

    public static final SkillTree TREE = new SkillTree("mc_");

    static {
        TREE.add(new SkillNode(
                TRANSFORM_1, "36 Transformations I",
                new String[]{ "Press V to open the", "36 Transformations.", "Neutral land and water forms." },
                3, 0, 0, List.of(), AbilityType.MENU, 1, "36 Transformations"));
        TREE.add(new SkillNode(
                TRANSFORM_2, "36 Transformations II",
                new String[]{ "Adds neutral", "flying forms." },
                4, 0, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "36 Transformations"));
        TREE.add(new SkillNode(
                TRANSFORM_3, "36 Transformations III",
                new String[]{ "Adds hostile", "land forms." },
                5, 0, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "36 Transformations"));
    }
}
