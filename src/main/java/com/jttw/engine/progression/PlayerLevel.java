package com.jttw.engine.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Reads and writes a player's progression state directly to their persistent NBT data.
 *
 * <p>All methods are static utility calls — no object instance is stored.
 * The player's persistent NBT is the single source of truth for all level data.
 * This is intentionally consistent with how {@link com.jttw.engine.reincarnation.AlignmentData}
 * and other engine systems work.
 *
 * <p><b>For client display</b>, use {@link SyncLevelPacket} after any write call.
 */
public final class PlayerLevel {
    private PlayerLevel() {}

    // -----------------------------------------------------------------------
    // Readers
    // -----------------------------------------------------------------------

    /** Current level (1–100).  Returns 1 if not yet set. */
    public static int getLevel(Player p) {
        return Math.max(1, p.getPersistentData().getInt(LevelConstants.KEY_LEVEL));
    }

    /** Total accumulated XP.  Compare against {@link GrowthRate#totalXpForLevel} to determine level. */
    public static long getXp(Player p) {
        return p.getPersistentData().getLong(LevelConstants.KEY_XP);
    }

    /**
     * Returns the player's assigned class, or null if they haven't reincarnated yet.
     * Always check this before acting on level data.
     */
    @Nullable
    public static ProgressionClass getProgressionClass(Player p) {
        String saved = p.getPersistentData().getString(LevelConstants.KEY_CLASS);
        if (saved.isEmpty()) return null;
        return ProgressionClass.parse(saved);
    }

    /** True if this player has a class assigned (has reincarnated and been initialised). */
    public static boolean hasClass(Player p) {
        return getProgressionClass(p) != null;
    }

    // -----------------------------------------------------------------------
    // Writers
    // -----------------------------------------------------------------------

    public static void setProgressionClass(Player p, ProgressionClass cls) {
        p.getPersistentData().putString(LevelConstants.KEY_CLASS, cls.name());
    }

    public static void setLevel(Player p, int level) {
        p.getPersistentData().putInt(
                LevelConstants.KEY_LEVEL,
                Math.max(1, Math.min(level, LevelConstants.MAX_LEVEL)));
    }

    public static void setXp(Player p, long xp) {
        p.getPersistentData().putLong(LevelConstants.KEY_XP, Math.max(0L, xp));
    }

    // -----------------------------------------------------------------------
    // Ability tracking
    // -----------------------------------------------------------------------

    /**
     * Returns true if the player has unlocked the given ability ID.
     *
     * <p>Systems granting gameplay effects (flight, phase, etc.) should call this
     * to check if the level milestone has been passed before enabling the ability.
     *
     * <p>Example: {@code if (PlayerLevel.hasAbility(player, "heavenly_mobility")) grantFlight();}
     */
    public static boolean hasAbility(Player p, String abilityId) {
        CompoundTag abilities = p.getPersistentData().getCompound(LevelConstants.KEY_ABILITIES);
        return abilities.getBoolean(abilityId);
    }

    /**
     * Marks an ability as unlocked for this player.  Idempotent — safe to call repeatedly.
     * Called automatically by {@link LevelManager} at the appropriate milestone.
     */
    public static void grantAbility(Player p, String abilityId) {
        CompoundTag pd = p.getPersistentData();
        CompoundTag abilities;
        if (pd.contains(LevelConstants.KEY_ABILITIES)) {
            abilities = pd.getCompound(LevelConstants.KEY_ABILITIES);
        } else {
            abilities = new CompoundTag();
            pd.put(LevelConstants.KEY_ABILITIES, abilities);
        }
        abilities.putBoolean(abilityId, true);
    }

    // -----------------------------------------------------------------------
    // Form stage tracking (primarily for Divine Beast evolution)
    // -----------------------------------------------------------------------

    /**
     * Current form stage index.  0 = base form.
     * Incremented by {@link LevelManager} at form-shift milestones.
     * {@link FormStage} maps stage indices to identity descriptions.
     */
    public static int getFormStage(Player p) {
        return p.getPersistentData().getInt(LevelConstants.KEY_FORM);
    }

    public static void setFormStage(Player p, int stage) {
        p.getPersistentData().putInt(LevelConstants.KEY_FORM, Math.max(0, stage));
    }

    // -----------------------------------------------------------------------
    // Divine Beast line tracking
    // -----------------------------------------------------------------------

    /**
     * Returns the player's Divine Beast elemental line, or null if not set.
     * Only meaningful for DIVINE_BEAST class players.
     */
    @Nullable
    public static DivineBeastLine getBeastLine(Player p) {
        String saved = p.getPersistentData().getString(LevelConstants.KEY_BEAST_LINE);
        if (saved.isEmpty()) return null;
        return DivineBeastLine.parse(saved);
    }

    public static void setBeastLine(Player p, DivineBeastLine line) {
        p.getPersistentData().putString(LevelConstants.KEY_BEAST_LINE, line.name());
    }

    // -----------------------------------------------------------------------
    // Clone / respawn transfer
    // -----------------------------------------------------------------------

    /**
     * Copy all level progression data from one player to another.
     * Called from {@link LevelManager#onPlayerClone} so level data survives death.
     */
    public static void copyTo(Player from, Player to) {
        CompoundTag src = from.getPersistentData();
        CompoundTag dst = to.getPersistentData();
        for (String key : new String[]{
                LevelConstants.KEY_LEVEL,
                LevelConstants.KEY_XP,
                LevelConstants.KEY_CLASS,
                LevelConstants.KEY_ABILITIES,
                LevelConstants.KEY_FORM,
                LevelConstants.KEY_BEAST_LINE
        }) {
            if (src.contains(key)) {
                dst.put(key, src.get(key).copy());
            }
        }
    }
}