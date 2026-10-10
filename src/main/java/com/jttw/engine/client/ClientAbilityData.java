package com.jttw.engine.client;

import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side cache for all player ability state.
 *
 * <p>Data sources:
 * <ul>
 *   <li>{@link com.jttw.engine.network.s2c.SyncAbilitiesPacket}  — skill-bar slot list.</li>
 *   <li>{@link com.jttw.engine.network.s2c.SyncCooldownPacket}   — per-ability cooldowns.</li>
 *   <li>{@link com.jttw.engine.network.s2c.SyncSkillTreePacket}  — skill point count + unlocked nodes.</li>
 * </ul>
 *
 * <p>All reads are on the client main thread. {@link #tickCooldowns()} is called once per
 * game tick from {@link ClientInit.ClientForgeEvents#onClientTick}.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientAbilityData {

    private ClientAbilityData() {}

    // -----------------------------------------------------------------------
    // Skill bar
    // -----------------------------------------------------------------------

    public static final int PAGE_SIZE = 4;

    private static List<AbilitySlot> abilities = Collections.emptyList();
    private static int pageOffset = 0;

    public static void setAbilities(List<AbilitySlot> incoming) {
        abilities  = List.copyOf(incoming);
        pageOffset = 0;
    }

    public static void cycle() {
        if (abilities.size() <= PAGE_SIZE) return;
        pageOffset = (pageOffset + PAGE_SIZE) % abilities.size();
    }

    /** Shows the second page while {@code shift} is held, if there is one; the first otherwise. */
    public static void showShiftPage(boolean shift) {
        pageOffset = shift && abilities.size() > PAGE_SIZE ? PAGE_SIZE : 0;
    }

    /** The slot index the server knows a slot on this page by: 0-3, or 4-7 on the Shift page. */
    public static int serverIndex(int slotIndex) {
        return pageOffset + slotIndex;
    }

    public static int totalCount() { return abilities.size(); }

    @Nullable
    public static AbilitySlot getSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= PAGE_SIZE) return null;
        int idx = pageOffset + slotIndex;
        if (idx >= abilities.size()) return null;
        return abilities.get(idx);
    }

    /** Returns the {@link AbilityType} for the given slot, or null if the slot is empty. */
    @Nullable
    public static AbilityType getSlotType(int slotIndex) {
        AbilitySlot e = getSlot(slotIndex);
        return (e != null) ? e.type() : null;
    }

    public static int currentPage() {
        if (abilities.isEmpty()) return 1;
        return (pageOffset / PAGE_SIZE) + 1;
    }

    public static int totalPages() {
        if (abilities.isEmpty()) return 1;
        return (int) Math.ceil((double) abilities.size() / PAGE_SIZE);
    }

    public static boolean isMultiPage() { return abilities.size() > PAGE_SIZE; }

    // -----------------------------------------------------------------------
    // Cooldowns
    // -----------------------------------------------------------------------

    private static final Map<String, int[]> cooldowns = new HashMap<>();

    public static void setCooldown(String abilityId, int remaining, int max) {
        cooldowns.put(abilityId, new int[]{ Math.max(0, remaining), Math.max(1, max) });
    }

    public static void tickCooldowns() {
        cooldowns.forEach((id, pair) -> { if (pair[0] > 0) pair[0]--; });
    }

    public static int getRemainingTicks(String abilityId) {
        int[] p = cooldowns.get(abilityId);
        return (p != null) ? p[0] : 0;
    }

    public static int getMaxTicks(String abilityId) {
        int[] p = cooldowns.get(abilityId);
        return (p != null) ? p[1] : 1;
    }

    public static float getCooldownFraction(String abilityId) {
        int remaining = getRemainingTicks(abilityId);
        if (remaining <= 0) return 0f;
        return Math.min(1f, (float) remaining / getMaxTicks(abilityId));
    }

    public static boolean isOnCooldown(String abilityId) {
        return getRemainingTicks(abilityId) > 0;
    }

    // -----------------------------------------------------------------------
    // Skill tree
    // -----------------------------------------------------------------------

    private static int skillPoints = 0;
    private static Set<String> unlockedNodes = new HashSet<>();

    /**
     * Called by {@link com.jttw.engine.network.s2c.SyncSkillTreePacket}.
     * Replaces the full tree state with authoritative server data.
     */
    public static void setSkillTree(int points, Set<String> nodes) {
        skillPoints   = Math.max(0, points);
        unlockedNodes = new HashSet<>(nodes);
    }

    public static int getSkillPoints() { return skillPoints; }

    public static Set<String> getUnlockedNodes() {
        return Collections.unmodifiableSet(unlockedNodes);
    }

    public static boolean hasNode(String nodeId) {
        return unlockedNodes.contains(nodeId);
    }

    /**
     * Applies an optimistic unlock immediately on the client before the server responds.
     * This gives instant feedback when clicking a node. The server's authoritative
     * {@link com.jttw.engine.network.s2c.SyncSkillTreePacket} will correct any mismatch.
     */
    public static void optimisticUnlock(String nodeId, int cost) {
        unlockedNodes.add(nodeId);
        skillPoints = Math.max(0, skillPoints - cost);
    }
}