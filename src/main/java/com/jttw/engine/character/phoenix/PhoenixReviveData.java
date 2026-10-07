package com.jttw.engine.character.phoenix;

import net.minecraft.world.entity.player.Player;

/**
 * Reads and writes Phoenix revive charges from player persistent NBT data.
 * Works on both client and server side (synced via SyncReviveCountPacket).
 */
public final class PhoenixReviveData {
    private PhoenixReviveData() {}

    private static final String KEY_REVIVES = "jttw_phoenix_revives";
    private static final String KEY_LAST_REVIVE_GAIN = "jttw_phoenix_last_gain_day";

    /** Starting revive charges for new Phoenix players. */
    public static final int STARTING_REVIVES = 5;

    /** Minecraft days between each revive charge gain. */
    public static final int DAYS_PER_REVIVE = 7;

    // -----------------------------------------------------------------------
    // Readers
    // -----------------------------------------------------------------------

    public static int getRevives(Player p) {
        return p.getPersistentData().getInt(KEY_REVIVES);
    }

    public static long getLastGainDay(Player p) {
        return p.getPersistentData().getLong(KEY_LAST_REVIVE_GAIN);
    }

    // -----------------------------------------------------------------------
    // Writers
    // -----------------------------------------------------------------------

    public static void setRevives(Player p, int count) {
        p.getPersistentData().putInt(KEY_REVIVES, Math.max(0, count));
    }

    public static void setLastGainDay(Player p, long day) {
        p.getPersistentData().putLong(KEY_LAST_REVIVE_GAIN, day);
    }

    /**
     * Initialize revive data for a new Phoenix player if not already set.
     */
    public static void initIfNeeded(Player p) {
        if (!p.getPersistentData().contains(KEY_REVIVES)) {
            setRevives(p, STARTING_REVIVES);
            setLastGainDay(p, p.level().getDayTime() / 24000L);
        }
    }

    /**
     * Spend one revive charge.
     * @return true if a charge was available and spent.
     */
    public static boolean spendRevive(Player p) {
        int current = getRevives(p);
        if (current <= 0) return false;
        setRevives(p, current - 1);
        return true;
    }
}
