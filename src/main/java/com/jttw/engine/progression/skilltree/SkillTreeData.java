package com.jttw.engine.progression.skilltree;

import com.jttw.engine.character.byakko.ByakkoSkillTree;
import com.jttw.engine.character.dragon.DragonSkillTree;
import com.jttw.engine.character.erlang.ErlangShenSkillTree;
import com.jttw.engine.character.nezha.NezhaSkillTree;
import com.jttw.engine.character.phoenix.PhoenixSkillTree;
import com.jttw.engine.character.turtle.TurtleSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.character.yuandi.YuanDiSkillTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads and writes a player's skill tree state to their persistent NBT data.
 *
 * <p>State stored:
 * <ul>
 *   <li>{@code jttw_skill_points} — unspent points available to spend in the tree.</li>
 *   <li>{@code jttw_skill_nodes}  — NBT list of unlocked node ID strings.</li>
 * </ul>
 *
 * <p><b>Points are awarded in {@link LevelManager} — one point every 5 levels.</b>
 *
 * <p><b>Spending a point:</b>
 * <pre>{@code
 * if (SkillTreeData.tryUnlock(player, "wu_ring_1")) {
 *     // success
 * }
 * }</pre>
 *
 * <p>Gameplay systems check unlock state via {@link #hasNode(Player, String)}.
 * Convenience helpers {@link #getTransformTier}, {@link #getFlightTier}, and
 * {@link #getRingTier} return the highest unlocked tier for Wukong's active abilities.
 */
public final class SkillTreeData {

    private SkillTreeData() {}

    private static final String KEY_POINTS = "jttw_skill_points";
    private static final String KEY_NODES  = "jttw_skill_nodes";

    // -----------------------------------------------------------------------
    // Points
    // -----------------------------------------------------------------------

    public static int getPoints(Player p) {
        return Math.max(0, p.getPersistentData().getInt(KEY_POINTS));
    }

    public static void setPoints(Player p, int points) {
        p.getPersistentData().putInt(KEY_POINTS, Math.max(0, points));
    }

    public static void addPoints(Player p, int amount) {
        setPoints(p, getPoints(p) + amount);
    }

    // -----------------------------------------------------------------------
    // Unlocked nodes
    // -----------------------------------------------------------------------

    /** Returns an immutable snapshot of all unlocked node IDs. */
    public static Set<String> getUnlockedNodes(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_NODES, Tag.TAG_LIST)) return Collections.emptySet();
        ListTag list = pd.getList(KEY_NODES, Tag.TAG_STRING);
        Set<String> result = new HashSet<>();
        for (Tag t : list) result.add(t.getAsString());
        return Collections.unmodifiableSet(result);
    }

    public static boolean hasNode(Player p, String nodeId) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_NODES, Tag.TAG_LIST)) return false;
        ListTag list = pd.getList(KEY_NODES, Tag.TAG_STRING);
        for (Tag t : list) {
            if (t.getAsString().equals(nodeId)) return true;
        }
        return false;
    }

    /** Directly grants a node without costing a skill point. Used by tests / debug. */
    public static void grantNode(Player p, String nodeId) {
        CompoundTag pd = p.getPersistentData();
        ListTag list;
        if (pd.contains(KEY_NODES, Tag.TAG_LIST)) {
            list = pd.getList(KEY_NODES, Tag.TAG_STRING);
        } else {
            list = new ListTag();
        }
        // Avoid duplicates
        for (Tag t : list) {
            if (t.getAsString().equals(nodeId)) return;
        }
        list.add(StringTag.valueOf(nodeId));
        pd.put(KEY_NODES, list);
    }

    // -----------------------------------------------------------------------
    // Unlock (spend a point)
    // -----------------------------------------------------------------------

    /** Resolves a skill node by ID across all registered skill trees. */
    @Nullable
    public static SkillNode resolveNode(String nodeId) {
        return SkillTrees.resolveNode(nodeId);
    }

    /**
     * Attempts to unlock {@code nodeId} for the player, deducting skill points.
     *
     * <p>Validates:
     * <ol>
     *   <li>The node exists in a registered skill tree.</li>
     *   <li>The player has enough skill points.</li>
     *   <li>All prerequisite nodes are already unlocked.</li>
     *   <li>The node is not already unlocked.</li>
     * </ol>
     *
     * @return {@code true} if the unlock succeeded; {@code false} otherwise.
     */
    public static boolean tryUnlock(Player p, String nodeId) {
        SkillNode node = resolveNode(nodeId);
        if (node == null) return false;

        if (hasNode(p, nodeId)) return false;

        int points = getPoints(p);
        if (points < node.cost()) return false;

        Set<String> unlocked = getUnlockedNodes(p);
        for (String prereq : node.prerequisites()) {
            if (!unlocked.contains(prereq)) return false;
        }

        // All checks passed — deduct and grant
        setPoints(p, points - node.cost());
        grantNode(p, nodeId);
        return true;
    }

    // -----------------------------------------------------------------------
    // Copy on death/clone
    // -----------------------------------------------------------------------

    public static void copyTo(Player from, Player to) {
        CompoundTag src = from.getPersistentData();
        CompoundTag dst = to.getPersistentData();
        if (src.contains(KEY_POINTS)) dst.put(KEY_POINTS, src.get(KEY_POINTS).copy());
        if (src.contains(KEY_NODES))  dst.put(KEY_NODES,  src.get(KEY_NODES).copy());
    }

    // -----------------------------------------------------------------------
    // Convenience — Wukong tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked transformation tier (0 = none unlocked). */
    public static int getTransformTier(Player p) {
        return WukongSkillTree.TREE.highestTier(getUnlockedNodes(p), "wu_transform");
    }

    /** Highest unlocked flight tier (0 = flight not unlocked). */
    public static int getFlightTier(Player p) {
        return WukongSkillTree.TREE.highestTier(getUnlockedNodes(p), "wu_flight");
    }

    /** Highest unlocked ring-of-fire tier (0 = not unlocked). */
    public static int getRingTier(Player p) {
        return WukongSkillTree.TREE.highestTier(getUnlockedNodes(p), "wu_ring");
    }

    /** Highest unlocked shadow-clone tier (0 = not unlocked). */
    public static int getCloneTier(Player p) {
        return WukongSkillTree.TREE.highestTier(getUnlockedNodes(p), "wu_clone");
    }

    // -----------------------------------------------------------------------
    // Convenience — Wukong passive helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Yao Wang (Demon King) tier (0 = not unlocked). */
    public static int getYaoWangTier(Player p) {
        return WukongSkillTree.TREE.highestTier(getUnlockedNodes(p), "wu_combat");
    }

    /** Highest unlocked Martial Arts tier (0 = not unlocked). */
    public static int getMartialTier(Player p) {
        return WukongSkillTree.TREE.highestTier(getUnlockedNodes(p), "wu_martial");
    }

    // -----------------------------------------------------------------------
    // Convenience — Erlang Shen tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Erlang Shen transformation tier (0 = none). */
    public static int getEsTransformTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_transform");
    }

    /** Highest unlocked Xiaotian Quan ability tier (0 = not unlocked). */
    public static int getXtqTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_xtq");
    }

    /** Highest unlocked Eye of Heaven tier (0 = not unlocked). */
    public static int getEyeTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_eye");
    }

    /** Highest unlocked Erlang Shen flight tier (0 = not unlocked). */
    public static int getEsFlightTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_flight");
    }

    /** Effective Xiaotian Quan growth tier based on Hound Growth passives. */
    public static int getXtqGrowthTier(Player p) {
        Set<String> nodes = getUnlockedNodes(p);
        if (nodes.contains(ErlangShenSkillTree.XTQ_GROWTH_3)) return 3;
        if (nodes.contains(ErlangShenSkillTree.XTQ_GROWTH_2)) return 2;
        if (nodes.contains(ErlangShenSkillTree.XTQ_GROWTH_1)) return 1;
        return 0;
    }

    /** Highest unlocked Celestial Hunt tier (0 = not unlocked). */
    public static int getEsHuntTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_hunt");
    }

    /** Highest unlocked Zhàn Shén (God of War) tier (0 = not unlocked). */
    public static int getEsWrathTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_wrath");
    }

    /** Highest unlocked Celestial Tactics tier (0 = not unlocked). */
    public static int getEsTacticsTier(Player p) {
        return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_tactics");
    }

    // -----------------------------------------------------------------------
    // Convenience — Yuan Di tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Abyssal Gravity tier (0 = not unlocked). */
    public static int getYdGravityTier(Player p) {
        return YuanDiSkillTree.TREE.highestTier(getUnlockedNodes(p), "yd_gravity");
    }

    /** Highest unlocked Primordial Step tier (0 = not unlocked). */
    public static int getYdStepTier(Player p) {
        return YuanDiSkillTree.TREE.highestTier(getUnlockedNodes(p), "yd_step");
    }

    /** Highest unlocked Ancient Dominion tier (0 = not unlocked). */
    public static int getYdDominionTier(Player p) {
        return YuanDiSkillTree.TREE.highestTier(getUnlockedNodes(p), "yd_dominion");
    }

    // -----------------------------------------------------------------------
    // Convenience — Dragon tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Dragon lightning tier (0 = not unlocked). */
    public static int getDrLightningTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_lightning");
    }

    /** Highest unlocked Dragon weather tier (0 = not unlocked). */
    public static int getDrWeatherTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_weather");
    }

    /** Highest unlocked Dragon flight tier (0 = not unlocked). */
    public static int getDrFlightTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_flight");
    }

    /** Highest unlocked Draconic Pressure tier (0 = not unlocked). */
    public static int getDrPressureTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_pressure");
    }

    /** Highest unlocked Tidal Recovery tier (0 = not unlocked). */
    public static int getDrTidalTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_tidal");
    }

    /** Highest unlocked Tidal Wave tier (0 = not unlocked). */
    public static int getDrWaveTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_wave");
    }

    /** Highest unlocked Dragon Scales tier (0 = not unlocked). */
    public static int getDrScalesTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_scales");
    }

    /** Highest unlocked Tidal Force tier (0 = not unlocked). */
    public static int getDrForceTier(Player p) {
        return DragonSkillTree.TREE.highestTier(getUnlockedNodes(p), "dr_force");
    }

    // -----------------------------------------------------------------------
    // Convenience — World Turtle tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Shell Guard tier (0 = not unlocked). */
    public static int getTuShellTier(Player p) {
        return TurtleSkillTree.TREE.highestTier(getUnlockedNodes(p), "tu_shell");
    }

    /** Highest unlocked Earthen Roots tier (0 = not unlocked). */
    public static int getTuRootsTier(Player p) {
        return TurtleSkillTree.TREE.highestTier(getUnlockedNodes(p), "tu_roots");
    }

    /** Highest unlocked Bedrock Constitution tier (0 = not unlocked). */
    public static int getTuBedrockTier(Player p) {
        return TurtleSkillTree.TREE.highestTier(getUnlockedNodes(p), "tu_bedrock");
    }

    /** Highest unlocked Tectonic Slam tier (0 = not unlocked). */
    public static int getTuTectonicTier(Player p) {
        return TurtleSkillTree.TREE.highestTier(getUnlockedNodes(p), "tu_tectonic");
    }

    /** Highest unlocked Gravity Well tier (0 = not unlocked). */
    public static int getTuGravityTier(Player p) {
        return TurtleSkillTree.TREE.highestTier(getUnlockedNodes(p), "tu_gravity");
    }

    /** Highest unlocked Mountain's Patience tier (0 = not unlocked). */
    public static int getTuPatienceTier(Player p) {
        return TurtleSkillTree.TREE.highestTier(getUnlockedNodes(p), "tu_patience");
    }

    // -----------------------------------------------------------------------
    // Convenience — Byakko tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Byakko leap tier (0 = not unlocked). */
    public static int getBkLeapTier(Player p) {
        return ByakkoSkillTree.TREE.highestTier(getUnlockedNodes(p), "bk_leap");
    }

    /** Highest unlocked Byakko bleed tier (0 = not unlocked). */
    public static int getBkBleedTier(Player p) {
        return ByakkoSkillTree.TREE.highestTier(getUnlockedNodes(p), "bk_bleed");
    }

    /** Highest unlocked Byakko velocity tier (0 = not unlocked). */
    public static int getBkVelocityTier(Player p) {
        return ByakkoSkillTree.TREE.highestTier(getUnlockedNodes(p), "bk_velocity");
    }

    /** Highest unlocked Scent of Blood tier (0 = not unlocked). */
    public static int getBkScentTier(Player p) {
        return ByakkoSkillTree.TREE.highestTier(getUnlockedNodes(p), "bk_scent");
    }

    /** Highest unlocked Predator's Instinct tier (0 = not unlocked). */
    public static int getBkInstinctTier(Player p) {
        return ByakkoSkillTree.TREE.highestTier(getUnlockedNodes(p), "bk_instinct");
    }

    /** Highest unlocked Territorial Roar tier (0 = not unlocked). */
    public static int getBkRoarTier(Player p) {
        return ByakkoSkillTree.TREE.highestTier(getUnlockedNodes(p), "bk_roar");
    }

    // -----------------------------------------------------------------------
    // Convenience — Phoenix tier helpers
    // -----------------------------------------------------------------------

    /** Highest unlocked Solar Radiance tier (0 = not unlocked). */
    public static int getPhRadianceTier(Player p) {
        return PhoenixSkillTree.TREE.highestTier(getUnlockedNodes(p), "ph_radiance");
    }

    /** Highest unlocked Rising Ember tier (0 = not unlocked). */
    public static int getPhEmberTier(Player p) {
        return PhoenixSkillTree.TREE.highestTier(getUnlockedNodes(p), "ph_ember");
    }

    /** Highest unlocked Molten Talons tier (0 = not unlocked). */
    public static int getPhTalonsTier(Player p) {
        return PhoenixSkillTree.TREE.highestTier(getUnlockedNodes(p), "ph_talons");
    }

    public static int getEsSpearTier(Player p) { return ErlangShenSkillTree.TREE.highestTier(getUnlockedNodes(p), "es_spear"); }

    // ── Nezha ────────────────────────────────────────────────────────
    public static int getNzArmTier(Player p)    { return NezhaSkillTree.TREE.highestTier(getUnlockedNodes(p), "nz_arm"); }
    public static int getNzFireTier(Player p)   { return NezhaSkillTree.TREE.highestTier(getUnlockedNodes(p), "nz_fire"); }
    public static int getNzDetTier(Player p)    { return NezhaSkillTree.TREE.highestTier(getUnlockedNodes(p), "nz_det"); }
    public static int getNzFormTier(Player p)   { return NezhaSkillTree.TREE.highestTier(getUnlockedNodes(p), "nz_form"); }
    public static int getNzFlightTier(Player p) { return NezhaSkillTree.TREE.highestTier(getUnlockedNodes(p), "nz_flight"); }
    public static int getNzCombatTier(Player p) { return NezhaSkillTree.TREE.highestTier(getUnlockedNodes(p), "nz_combat"); }
}