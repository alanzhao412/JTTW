package com.jttw.engine.progression.level;

/**
 * The three tiers of enemy in a post-20 biome grinding zone.
 *
 * <p>Every registered biome band contains all three tiers, spawning at different
 * rarities and giving proportionally different XP rewards.
 *
 * <p>Tag biome enemies with {@code "jttw_enemy_tier"} in their persistent data
 * (e.g. {@code entity.getPersistentData().putString("jttw_enemy_tier", "GENERAL")})
 * so {@link LevelManager} can look up the correct tier on kill and award the right XP.
 *
 * <p>XP for a kill = {@link BiomeLevelBand#xpForTier(EnemyTier)} using the player's
 * current biome band.
 */
public enum EnemyTier {

    /**
     * Common zone mob.  Safe introduction, low XP.
     * Should make up ~70% of zone spawns.
     */
    GRUNT(1.0f, "Grunt"),

    /**
     * Mid-tier threat.  Notably more dangerous than grunts, meaningfully more XP.
     * Should make up ~25% of zone spawns.  Rewards targeted hunting.
     */
    GENERAL(5.0f, "General"),

    /**
     * Zone boss.  High XP, significant power, drives major progression spikes.
     * Rare spawn (~5%) or triggered encounter.  Killing one should noticeably move
     * the XP bar regardless of the player's level within the band.
     */
    BOSS(20.0f, "Boss");

    /** XP multiplier relative to the zone's base grunt XP ({@link BiomeLevelBand#baseGruntXp()}). */
    public final float xpMultiplier;

    /** Human-readable label for chat messages and debug output. */
    public final String displayName;

    EnemyTier(float xpMultiplier, String displayName) {
        this.xpMultiplier = xpMultiplier;
        this.displayName = displayName;
    }

    /** Parse from persistent data string.  Returns GRUNT as a safe fallback. */
    public static EnemyTier parse(String s) {
        if (s == null) return GRUNT;
        for (EnemyTier tier : values()) {
            if (tier.name().equalsIgnoreCase(s.trim())) return tier;
        }
        return GRUNT;
    }
}