package com.jttw.engine.reincarnation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Stores and reads alignment points from player persistent NBT.
 *
 * This is intentionally simple so your other systems can increment points
 * from kill/mining/taming/boss events later.
 */
public final class AlignmentData {
    private AlignmentData() {}

    public static final String KEY_ROOT = "jttw_alignment";
    public static final String KEY_HEAVENLY = "heavenly";
    public static final String KEY_SPIRIT_BEAST = "spirit_beast";
    public static final String KEY_PRIMORDIAL = "primordial";
    public static final String KEY_YAOGUAI = "yaoguai";

    public static final String KEY_REINCARNATED = "jttw_reincarnated";
    public static final String KEY_REINCARNATION_OUTCOME = "jttw_reincarnation_outcome";

    private static CompoundTag getRoot(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_ROOT)) pd.put(KEY_ROOT, new CompoundTag());
        return pd.getCompound(KEY_ROOT);
    }

    public static int getHeavenly(Player p) {
        return getRoot(p).getInt(KEY_HEAVENLY);
    }

    public static int getSpiritBeast(Player p) {
        return getRoot(p).getInt(KEY_SPIRIT_BEAST);
    }

    public static int getPrimordial(Player p) {
        return getRoot(p).getInt(KEY_PRIMORDIAL);
    }

    public static int getYaoguai(Player p) {
        return getRoot(p).getInt(KEY_YAOGUAI);
    }

    public static int getTotal(Player p) {
        CompoundTag r = getRoot(p);
        return r.getInt(KEY_HEAVENLY)
                + r.getInt(KEY_SPIRIT_BEAST)
                + r.getInt(KEY_PRIMORDIAL)
                + r.getInt(KEY_YAOGUAI);
    }

    public static void addHeavenly(Player p, int delta) {
        add(p, KEY_HEAVENLY, delta);
    }

    public static void addSpiritBeast(Player p, int delta) {
        add(p, KEY_SPIRIT_BEAST, delta);
    }

    public static void addPrimordial(Player p, int delta) {
        add(p, KEY_PRIMORDIAL, delta);
    }

    public static void addYaoguai(Player p, int delta) {
        add(p, KEY_YAOGUAI, delta);
    }

    public static void setHeavenly(Player p, int v) { set(p, KEY_HEAVENLY, v); }
    public static void setSpiritBeast(Player p, int v) { set(p, KEY_SPIRIT_BEAST, v); }
    public static void setPrimordial(Player p, int v) { set(p, KEY_PRIMORDIAL, v); }
    public static void setYaoguai(Player p, int v) { set(p, KEY_YAOGUAI, v); }

    private static void add(Player p, String key, int delta) {
        CompoundTag r = getRoot(p);
        int v = r.getInt(key);
        r.putInt(key, Math.max(0, v + delta));
    }

    private static void set(Player p, String key, int v) {
        CompoundTag r = getRoot(p);
        r.putInt(key, Math.max(0, v));
    }

    public static boolean hasReincarnated(Player p) {
        return p.getPersistentData().getBoolean(KEY_REINCARNATED);
    }

    public static void markReincarnated(Player p, ReincarnationOutcome outcome) {
        p.getPersistentData().putBoolean(KEY_REINCARNATED, true);
        p.getPersistentData().putString(KEY_REINCARNATION_OUTCOME, outcome.id);
    }

    /** Copies the four alignment counts to the respawned player. */
    public static void copyTo(Player from, Player to) {
        to.getPersistentData().put(KEY_ROOT, getRoot(from).copy());
    }

    public static void resetAlignment(Player p) {
        p.getPersistentData().put(KEY_ROOT, new CompoundTag());
    }
}
