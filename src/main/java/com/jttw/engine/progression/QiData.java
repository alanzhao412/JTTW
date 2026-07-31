package com.jttw.engine.progression;

import net.minecraft.world.entity.player.Player;

/**
 * Reads and writes a player's Qi (current + max) from their persistent NBT data.
 *
 * <p>Qi is a floating-point resource that:
 * <ul>
 *   <li>Is capped at {@code max} (derived from {@link StatBlock#qi()}).</li>
 *   <li>Regenerates over time at a rate driven by {@link StatBlock#will()}.</li>
 *   <li>Is spent when abilities or techniques are activated.</li>
 * </ul>
 *
 * <p>Client-side values are kept in sync via {@link SyncQiPacket}, written here
 * to the client player's persistent data so all read calls are uniform.
 */
public final class QiData {
    private QiData() {}

    static final String KEY_CURRENT = "jttw_qi_current";
    static final String KEY_MAX     = "jttw_qi_max";

    // -----------------------------------------------------------------------
    // Readers
    // -----------------------------------------------------------------------

    public static float getCurrent(Player p) {
        return p.getPersistentData().getFloat(KEY_CURRENT);
    }

    public static float getMax(Player p) {
        return p.getPersistentData().getFloat(KEY_MAX);
    }

    // -----------------------------------------------------------------------
    // Writers
    // -----------------------------------------------------------------------

    public static void setCurrent(Player p, float value) {
        float max = getMax(p);
        p.getPersistentData().putFloat(KEY_CURRENT, Math.max(0f, max > 0 ? Math.min(value, max) : value));
    }

    public static void setMax(Player p, float value) {
        p.getPersistentData().putFloat(KEY_MAX, Math.max(0f, value));
    }

    /**
     * Consume a given amount of Qi.
     *
     * @return {@code true} if the player had enough Qi and it was spent.
     *         {@code false} if insufficient — nothing is deducted.
     */
    public static boolean spend(Player p, float amount) {
        float current = getCurrent(p);
        if (current < amount) return false;
        setCurrent(p, current - amount);
        return true;
    }

    /** Restore Qi by {@code amount}, clamped to max. */
    public static void restore(Player p, float amount) {
        setCurrent(p, getCurrent(p) + amount);
    }
}