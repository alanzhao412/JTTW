package com.jttw.engine.character.saurian;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.List;

/**
 * Skill tree for the Saurian Demon King (YAOGUAI): Jiao Mo Wang, the Flood-Dragon Demon King, one
 * of the Monkey King's seven sworn brothers. Water, serpent and venom. Node ids start {@code sdk_}.
 *
 * <pre>
 * Col:   0 (Riptide)  1 (Lash)   2 (Whirlpool)  3 (Wrath)   4 (Riverborn)  5 (Scaled Hide)  6 (Venom)  7 (18 Transformations)
 * Row 0: Surge I      Lash I     Whirlpool I    Wrath I     Riverborn I    Scaled Hide I    Venom I    18 Trans. I
 * Row 1: Surge II     Lash II    Whirlpool II   Wrath II    Riverborn II   Scaled Hide II   Venom II   18 Trans. II
 * Row 2: Surge III    Lash III   Whirlpool III  Wrath III   Riverborn III  Scaled Hide III  Venom III  18 Trans. III
 * </pre>
 */
public final class SaurianSkillTree {

    private SaurianSkillTree() {}

    public static final String SURGE_1 = "sdk_surge_1", SURGE_2 = "sdk_surge_2", SURGE_3 = "sdk_surge_3";
    public static final String LASH_1 = "sdk_lash_1", LASH_2 = "sdk_lash_2", LASH_3 = "sdk_lash_3";
    public static final String WHIRL_1 = "sdk_whirl_1", WHIRL_2 = "sdk_whirl_2", WHIRL_3 = "sdk_whirl_3";
    public static final String WRATH_1 = "sdk_wrath_1", WRATH_2 = "sdk_wrath_2", WRATH_3 = "sdk_wrath_3";
    public static final String RIVER_1 = "sdk_river_1", RIVER_2 = "sdk_river_2", RIVER_3 = "sdk_river_3";
    public static final String HIDE_1 = "sdk_hide_1", HIDE_2 = "sdk_hide_2", HIDE_3 = "sdk_hide_3";
    public static final String VENOM_1 = "sdk_venom_1", VENOM_2 = "sdk_venom_2", VENOM_3 = "sdk_venom_3";
    public static final String TRANSFORM_1 = "sdk_transform_1", TRANSFORM_2 = "sdk_transform_2", TRANSFORM_3 = "sdk_transform_3";

    public static final SkillTree TREE = new SkillTree("sdk_");

    private static void branch(String[] ids, String name, String column, int col, AbilityType type, int[] costs, String[][] text) {
        for (int i = 0; i < 3; i++) {
            TREE.add(new SkillNode(ids[i], name + " " + "I".repeat(i + 1),
                    text[i], costs[i], col, i, i == 0 ? List.of() : List.of(ids[i - 1]), type, i + 1, column));
        }
    }

    static {
        branch(new String[]{SURGE_1, SURGE_2, SURGE_3}, "Riptide Surge", "Riptide Surge", 0, AbilityType.HELD, new int[]{2, 3, 4},
                new String[][]{
                        {"Hold V, let go: launch the", "way you look, striking all", "you pass. Charge: further."},
                        {"Faster, further, harder.", "", ""},
                        {"Faster, further, harder", "still.", ""}});
        branch(new String[]{LASH_1, LASH_2, LASH_3}, "Tidal Lash", "Tidal Lash", 1, AbilityType.TOGGLE, new int[]{2, 3, 4},
                new String[][]{
                        {"B: a rope of water to the", "crosshair yanks a foe to you,", "or you to the block."},
                        {"Longer reach, harder lash.", "", ""},
                        {"Longer still; the lashed", "are slowed.", ""}});
        branch(new String[]{WHIRL_1, WHIRL_2, WHIRL_3}, "Whirlpool", "Whirlpool", 2, AbilityType.TOGGLE, new int[]{3, 4, 5},
                new String[][]{
                        {"N: a whirlpool where you", "look drags foes into its", "heart and drowns them."},
                        {"Wider, longer, deeper.", "", ""},
                        {"Wider, longer, deeper", "still.", ""}});
        branch(new String[]{WRATH_1, WRATH_2, WRATH_3}, "Flood-Dragon's Wrath", "Flood-Dragon's Wrath", 3, AbilityType.TOGGLE, new int[]{4, 5, 6},
                new String[][]{
                        {"M: for 12 s your blows burst", "in floodwater round the one", "struck. Surge free while wet."},
                        {"15 s; the bursts hit harder.", "", ""},
                        {"20 s; harder still.", "", ""}});
        branch(new String[]{RIVER_1, RIVER_2, RIVER_3}, "Riverborn", "Riverborn", 4, AbilityType.PASSIVE, new int[]{2, 3, 4},
                new String[][]{
                        {"Breathe water; swim swiftly.", "", ""},
                        {"Wet (in water or rain):", "+15% damage, and heal.", ""},
                        {"Wet: +30% damage, and", "heal faster.", ""}});
        branch(new String[]{HIDE_1, HIDE_2, HIDE_3}, "Scaled Hide", "Scaled Hide", 5, AbilityType.PASSIVE, new int[]{2, 3, 4},
                new String[][]{
                        {"Tougher against blows.", "", ""},
                        {"Tougher still.", "", ""},
                        {"Tougher still, and", "against magic.", ""}});
        branch(new String[]{VENOM_1, VENOM_2, VENOM_3}, "Venom Fangs", "Venom Fangs", 6, AbilityType.PASSIVE, new int[]{2, 3, 4},
                new String[][]{
                        {"Your blows envenom: poison", "that stacks, up to 3.", ""},
                        {"Up to 5 stacks, stronger.", "", ""},
                        {"Up to 7 stacks, stronger", "still.", ""}});
        branch(new String[]{TRANSFORM_1, TRANSFORM_2, TRANSFORM_3}, "18 Transformations", "18 Transformations", 7, AbilityType.MENU, new int[]{3, 4, 5},
                new String[][]{
                        {"Shift + V: the 18", "Transformations. Neutral", "land forms."},
                        {"Adds neutral", "water forms.", ""},
                        {"Adds neutral", "flying forms.", ""}});
    }
}
