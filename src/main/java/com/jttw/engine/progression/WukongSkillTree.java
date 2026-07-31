package com.jttw.engine.progression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines Sun Wukong's (CELESTIAL_PRIMATE) complete skill tree.
 *
 * <p><b>Tree layout — 6 columns, 3 rows max:</b>
 * <pre>
 * Col:    0 (Transforms)    1 (Shadow Clone)  2 (Ring of Fire)   3 (Vitality)  4 (Qi)  5 (Yao Wang)  6 (Flight)  7 (Martial Arts)
 *
 * Row 0:  Transform I       Clone I            Ring I             Vitality I    Qi I    Yao Wang I    Cloud Step  Martial Arts I
 * Row 1:  Transform II      Clone II           Ring II            Vitality II   Qi II   Yao Wang II   Nimbus      Martial Arts II
 * Row 2:  Transform III     Clone III          Ring III                                 Yao Wang III  Somersault  Martial Arts III
 * </pre>
 *
 * <p>Players earn 1 skill point every 5 levels (max 20 at level 100). With 39 total cost
 * to max all nodes, choices are forced — invest deep in abilities you want.
 *
 * <p>To hook a node into gameplay, check {@link SkillTreeData#hasNode(net.minecraft.world.entity.player.Player, String)}
 * using the node ID strings defined as constants below.
 */
public final class WukongSkillTree {

    private WukongSkillTree() {}

    // -----------------------------------------------------------------------
    // Node ID constants — use these in gameplay checks
    // -----------------------------------------------------------------------

    public static final String TRANSFORM_1 = "wu_transform_1";
    public static final String TRANSFORM_2 = "wu_transform_2";
    public static final String TRANSFORM_3 = "wu_transform_3";

    public static final String FLIGHT_1 = "wu_flight_1";
    public static final String FLIGHT_2 = "wu_flight_2";
    public static final String FLIGHT_3 = "wu_flight_3";

    public static final String RING_1 = "wu_ring_1";
    public static final String RING_2 = "wu_ring_2";
    public static final String RING_3 = "wu_ring_3";

    public static final String VITALITY_1 = "wu_vitality_1";
    public static final String VITALITY_2 = "wu_vitality_2";
    public static final String QI_1       = "wu_qi_1";
    public static final String QI_2       = "wu_qi_2";
    public static final String COMBAT_1   = "wu_combat_1";
    public static final String COMBAT_2   = "wu_combat_2";
    public static final String COMBAT_3   = "wu_combat_3";

    public static final String CLONE_1    = "wu_clone_1";
    public static final String CLONE_2    = "wu_clone_2";
    public static final String CLONE_3    = "wu_clone_3";

    public static final String MARTIAL_1  = "wu_martial_1";
    public static final String MARTIAL_2  = "wu_martial_2";
    public static final String MARTIAL_3  = "wu_martial_3";

    // -----------------------------------------------------------------------
    // Registry
    // -----------------------------------------------------------------------

    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    static {
        // ── 72 TRANSFORMATIONS (col 0, MENU type) ───────────────────────
        reg(new SkillNode(
                TRANSFORM_1, "72 Transformations I",
                new String[]{ "Press V to open the", "Transformation Menu.",
                              "Ground & water mobs only." },
                3, 0, 0, List.of(), AbilityType.MENU, 1, "Transformations"
        ));
        reg(new SkillNode(
                TRANSFORM_2, "72 Transformations II",
                new String[]{ "Unlocks flying mob forms.", "Transform your equipped",
                              "items into same-tier variants." },
                4, 0, 1, List.of(TRANSFORM_1), AbilityType.MENU, 2, "Transformations"
        ));
        reg(new SkillNode(
                TRANSFORM_3, "72 Transformations III",
                new String[]{ "Unlocks boss entity forms.", "Qi cost scales with",
                              "the size of the form." },
                5, 0, 2, List.of(TRANSFORM_2), AbilityType.MENU, 3, "Transformations"
        ));

        // ── FLIGHT (col 6, PASSIVE — auto-grants flight when unlocked) ──────
        reg(new SkillNode(
                FLIGHT_1, "Cloud Step",
                new String[]{ "Grants permanent flight.", "Hold Space to ascend.", "" },
                2, 6, 0, List.of(), AbilityType.PASSIVE, 1, "Flight"
        ));
        reg(new SkillNode(
                FLIGHT_2, "Nimbus Cloud",
                new String[]{ "Flight speed greatly", "increased. Somersault",
                              "cloud momentum physics." },
                3, 6, 1, List.of(FLIGHT_1), AbilityType.PASSIVE, 2, "Flight"
        ));
        reg(new SkillNode(
                FLIGHT_3, "Somersault Cloud",
                new String[]{ "Cycles Overworld/Nether/End", "while airborne. Press B",
                              "to dimension-travel." },
                4, 6, 2, List.of(FLIGHT_2), AbilityType.PASSIVE, 3, "Flight"
        ));

        // ── RING OF FIRE (col 2, HELD type) ─────────────────────────────
        reg(new SkillNode(
                RING_1, "Ring of Fire I",
                new String[]{ "Hold N: creates a ring of", "fire. Pushes & ignites",
                              "all nearby entities." },
                2, 2, 0, List.of(), AbilityType.HELD, 1, "Ring of Fire"
        ));
        reg(new SkillNode(
                RING_2, "Ring of Fire II",
                new String[]{ "While ring is active, your", "Qi regenerates faster",
                              "inside the ring." },
                3, 2, 1, List.of(RING_1), AbilityType.HELD, 2, "Ring of Fire"
        ));
        reg(new SkillNode(
                RING_3, "Ring of Fire III",
                new String[]{ "Entities cannot enter the", "ring at all. Impenetrable",
                              "barrier of flames." },
                4, 2, 2, List.of(RING_2), AbilityType.HELD, 3, "Ring of Fire"
        ));

        // ── VITALITY PASSIVES (col 3) ─────────────────────────────────────
        reg(new SkillNode(
                VITALITY_1, "Iron Body I",
                new String[]{ "+20 max HP.", "" },
                1, 3, 0, List.of(), AbilityType.PASSIVE, 0, "Vitality"
        ));
        reg(new SkillNode(
                VITALITY_2, "Iron Body II",
                new String[]{ "+20 max HP.", "" },
                2, 3, 1, List.of(VITALITY_1), AbilityType.PASSIVE, 0, "Vitality"
        ));

        // ── QI PASSIVES (col 4) ───────────────────────────────────────────
        reg(new SkillNode(
                QI_1, "Qi Mastery I",
                new String[]{ "+15% Qi regen speed.", "" },
                1, 4, 0, List.of(), AbilityType.PASSIVE, 0, "Qi Mastery"
        ));
        reg(new SkillNode(
                QI_2, "Qi Mastery II",
                new String[]{ "+15% Qi regen speed.", "" },
                2, 4, 1, List.of(QI_1), AbilityType.PASSIVE, 0, "Qi Mastery"
        ));

        // ── YAO WANG / DEMON KING (col 5, PASSIVE — Demonic Fury stacking) ──
        reg(new SkillNode(
                COMBAT_1, "Yao Wang I",
                new String[]{ "+10% all damage. Combat", "builds Demonic Fury stacks", "(+3% dmg/spd each, max 5)." },
                2, 5, 0, List.of(), AbilityType.PASSIVE, 1, "Yao Wang"
        ));
        reg(new SkillNode(
                COMBAT_2, "Yao Wang II",
                new String[]{ "+20% all damage. Max 10", "fury stacks. 5% lifesteal.", "12s decay timer." },
                3, 5, 1, List.of(COMBAT_1), AbilityType.PASSIVE, 2, "Yao Wang"
        ));
        reg(new SkillNode(
                COMBAT_3, "Yao Wang III",
                new String[]{ "+30% all damage. Max 15", "stacks. 10% lifesteal. KB", "immune at 10+. Weakness aura." },
                4, 5, 2, List.of(COMBAT_2), AbilityType.PASSIVE, 3, "Yao Wang"
        ));

        // ── SHADOW CLONE (col 1, TOGGLE / HELD type) ───────────────────────
        reg(new SkillNode(
                CLONE_1, "Shadow Clone I",
                new String[]{ "Press B: summon 1 clone", "that fights alongside you.", "Costs Qi to maintain." },
                3, 1, 0, List.of(), AbilityType.TOGGLE, 1, "Shadow Clone"
        ));
        reg(new SkillNode(
                CLONE_2, "Shadow Clone II",
                new String[]{ "Summon 2 clones.", "Mirrors your stats and", "equipped weapon." },
                4, 1, 1, List.of(CLONE_1), AbilityType.TOGGLE, 2, "Shadow Clone"
        ));
        reg(new SkillNode(
                CLONE_3, "Shadow Clone III",
                new String[]{ "Press B to summon clones.", "Max 9 clones active.", "Hit a clone to dismiss it." },
                5, 1, 2, List.of(CLONE_2), AbilityType.TOGGLE, 3, "Shadow Clone"
        ));

        // ── MARTIAL ARTS (col 7, PASSIVE — permanent stat boosts) ────────
        reg(new SkillNode(
                MARTIAL_1, "Wind Step",
                new String[]{ "+15% movement speed.", "Move like the wind between", "strikes." },
                2, 7, 0, List.of(), AbilityType.PASSIVE, 1, "Martial Arts"
        ));
        reg(new SkillNode(
                MARTIAL_2, "Staff Mastery",
                new String[]{ "+3 extra reach with the", "Ruyi Jingu Bang in all", "modes." },
                3, 7, 1, List.of(MARTIAL_1), AbilityType.PASSIVE, 2, "Martial Arts"
        ));
        reg(new SkillNode(
                MARTIAL_3, "Iron Body",
                new String[]{ "+30% attack damage.", "+6 armor. Wukong's body", "is as hard as stone." },
                4, 7, 2, List.of(MARTIAL_2), AbilityType.PASSIVE, 3, "Martial Arts"
        ));
    }

    private static void reg(SkillNode node) {
        NODES.put(node.id(), node);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public static Map<String, SkillNode> getNodes() {
        return Collections.unmodifiableMap(NODES);
    }

    public static SkillNode get(String id) {
        return NODES.get(id);
    }

    /** Returns all nodes in the tree, ordered by col then row. */
    public static List<SkillNode> getAllOrdered() {
        List<SkillNode> list = new ArrayList<>(NODES.values());
        list.sort((a, b) -> a.col() != b.col() ? Integer.compare(a.col(), b.col())
                                                 : Integer.compare(a.row(), b.row()));
        return list;
    }

    /**
     * Returns the highest unlocked tier for the ability branch with the given root ID prefix.
     * E.g. prefix "wu_transform" checks wu_transform_1, _2, _3.
     *
     * @param unlockedNodeIds the set of node IDs the player has unlocked.
     */
    public static int getHighestTier(java.util.Set<String> unlockedNodeIds, String idPrefix) {
        int tier = 0;
        for (SkillNode node : NODES.values()) {
            if (node.id().startsWith(idPrefix) && unlockedNodeIds.contains(node.id())) {
                tier = Math.max(tier, node.tier());
            }
        }
        return tier;
    }
}