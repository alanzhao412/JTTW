package com.jttw.engine.character.ninespirits;

import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTree;

import java.util.List;

/**
 * Skill tree for the Grand Saint of Nine Spirits (YAOGUAI, Primordial secondary): the
 * nine-headed lion of Bamboo-Knot Mountain, Taiyi Tianzun's mount. Node ids start {@code ns_}.
 *
 * <pre>
 * Col:   0 (Heads)       1 (Roar)        2 (Mane)       3 (True Form)    4 (Nine Lives)
 * Row 0: Fire Head       Roar I          Mane Flare I   True Form I      Nine Lives I
 * Row 1: Thunder Head    Roar II         Mane Flare II  True Form II     Nine Lives II
 * Row 2: Frost Head      Roar III                       True Form III    Nine Lives III
 * </pre>
 * 44 points for the whole tree, against 20 earned by level 100.
 */
public final class NineSpiritsSkillTree {

    private NineSpiritsSkillTree() {}

    public static final String HEADS_1 = "ns_heads_1";
    public static final String HEADS_2 = "ns_heads_2";
    public static final String HEADS_3 = "ns_heads_3";
    public static final String ROAR_1 = "ns_roar_1";
    public static final String ROAR_2 = "ns_roar_2";
    public static final String ROAR_3 = "ns_roar_3";
    public static final String FLARE_1 = "ns_flare_1";
    public static final String FLARE_2 = "ns_flare_2";
    public static final String FORM_1 = "ns_form_1";
    public static final String FORM_2 = "ns_form_2";
    public static final String FORM_3 = "ns_form_3";
    public static final String LIVES_1 = "ns_lives_1";
    public static final String LIVES_2 = "ns_lives_2";
    public static final String LIVES_3 = "ns_lives_3";

    public static final SkillTree TREE = new SkillTree("ns_");

    static {
        // ── HEADS OF MANY ELEMENTS (col 0): each tier wakes another head ──
        TREE.add(new SkillNode(HEADS_1, "Fire Head",
                new String[]{ "Press V: a fireball that", "bursts and sets foes alight.", "Sneak + V: change head." },
                2, 0, 0, List.of(), AbilityType.TOGGLE, 1, "Heads"));
        TREE.add(new SkillNode(HEADS_2, "Thunder Head",
                new String[]{ "A bolt that jumps to", "the nearest foes.", "Fire burns hotter." },
                3, 0, 1, List.of(HEADS_1), AbilityType.TOGGLE, 2, "Heads"));
        TREE.add(new SkillNode(HEADS_3, "Frost Head",
                new String[]{ "A shard that slows, then", "freezes. Thunder jumps", "further." },
                4, 0, 2, List.of(HEADS_2), AbilityType.TOGGLE, 3, "Heads"));

        // ── HEAVEN-SHAKING ROAR (col 1) ─────────────────────────────────
        TREE.add(new SkillNode(ROAR_1, "Heaven-Shaking Roar I",
                new String[]{ "Press B: a roar that", "knocks back and slows", "everything in front." },
                2, 1, 0, List.of(), AbilityType.TOGGLE, 1, "Roar"));
        TREE.add(new SkillNode(ROAR_2, "Heaven-Shaking Roar II",
                new String[]{ "Roared foes reel, dizzy,", "and cannot fight back", "for 2 seconds." },
                3, 1, 1, List.of(ROAR_1), AbilityType.TOGGLE, 2, "Roar"));
        TREE.add(new SkillNode(ROAR_3, "Heaven-Shaking Roar III",
                new String[]{ "Flyers fall from the sky;", "arrows and fireballs", "shatter mid-air." },
                4, 1, 2, List.of(ROAR_2), AbilityType.TOGGLE, 3, "Roar"));

        // ── GOLDEN MANE FLARE (col 2) ───────────────────────────────────
        TREE.add(new SkillNode(FLARE_1, "Golden Mane Flare I",
                new String[]{ "Press N: your mane flares.", "All who look at you within", "12 blocks are blinded." },
                2, 2, 0, List.of(), AbilityType.TOGGLE, 1, "Mane"));
        TREE.add(new SkillNode(FLARE_2, "Golden Mane Flare II",
                new String[]{ "20 blocks; the blinded", "also stumble, slowed.", "" },
                3, 2, 1, List.of(FLARE_1), AbilityType.TOGGLE, 2, "Mane"));

        // ── NINE-HEADED TRUE FORM (col 3) ───────────────────────────────
        TREE.add(new SkillNode(FORM_1, "True Form I",
                new String[]{ "Press M: the great lion.", "Hits strike with 2 heads;", "tougher. Drains Qi." },
                3, 3, 0, List.of(), AbilityType.TOGGLE, 1, "True Form"));
        TREE.add(new SkillNode(FORM_2, "True Form II",
                new String[]{ "Hits strike with 3 heads.", "", "" },
                4, 3, 1, List.of(FORM_1), AbilityType.TOGGLE, 2, "True Form"));
        TREE.add(new SkillNode(FORM_3, "True Form III",
                new String[]{ "Hits strike with 4 heads.", "Tougher still.", "" },
                5, 3, 2, List.of(FORM_2), AbilityType.TOGGLE, 3, "True Form"));

        // ── NINE LIVES (col 4, PASSIVE) ─────────────────────────────────
        TREE.add(new SkillNode(LIVES_1, "Nine Lives I",
                new String[]{ "Survive a killing blow", "once. A spent life comes", "back after 5 minutes." },
                2, 4, 0, List.of(), AbilityType.PASSIVE, 1, "Nine Lives"));
        TREE.add(new SkillNode(LIVES_2, "Nine Lives II",
                new String[]{ "Two lives to spend.", "", "" },
                3, 4, 1, List.of(LIVES_1), AbilityType.PASSIVE, 2, "Nine Lives"));
        TREE.add(new SkillNode(LIVES_3, "Nine Lives III",
                new String[]{ "Three lives to spend.", "", "" },
                4, 4, 2, List.of(LIVES_2), AbilityType.PASSIVE, 3, "Nine Lives"));
    }
}
