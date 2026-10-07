package com.jttw.engine.progression.level;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Associates biomes with a level band and base XP for post-20 grinding progression.
 *
 * <p><b>Registration:</b> Call {@link #register(BiomeLevelBand)} during mod setup
 * (e.g. in {@link com.jttw.engine.JTTWEngine} common setup, after Forge registries are ready).
 *
 * <pre>{@code
 * // Phase 1: early biome band (levels 21–40)
 * BiomeLevelBand.register(new BiomeLevelBand(
 *     new ResourceLocation("minecraft", "jungle"),
 *     21, 40,
 *     80   // grunt gives 80 XP, general 400, boss 1600
 * ));
 *
 * // Phase 2: mid biome band (levels 41–65)
 * BiomeLevelBand.register(new BiomeLevelBand(
 *     new ResourceLocation("minecraft", "deep_dark"),
 *     41, 65,
 *     200
 * ));
 * }</pre>
 *
 * <p>Bands may overlap; the first registered match wins (register more specific first).
 *
 * <p><b>TODO:</b> Populate with your actual biome → band mappings.  These are intentionally
 * left empty so you can define the world's biome layout independently.  Consider loading
 * from a JSON datapack so server admins can tune the level landscape without recompiling.
 *
 * <p><b>Suggested band structure (5 phases × 16 levels each):</b>
 * <pre>
 *   Phase 1 (21–36)  — introductory biome,  base grunt XP  ~80
 *   Phase 2 (37–52)  — mid tier biome,      base grunt XP  ~200
 *   Phase 3 (53–68)  — dangerous biome,     base grunt XP  ~400
 *   Phase 4 (69–84)  — extreme biome,       base grunt XP  ~700
 *   Phase 5 (85–100) — endgame biome,       base grunt XP  ~1100
 * </pre>
 */
public record BiomeLevelBand(
        ResourceLocation biome,
        int minLevel,
        int maxLevel,
        int baseGruntXp
) {
    private static final List<BiomeLevelBand> REGISTRY = new ArrayList<>();

    public static void register(BiomeLevelBand band) {
        REGISTRY.add(band);
    }

    public static List<BiomeLevelBand> all() {
        return Collections.unmodifiableList(REGISTRY);
    }

    // -----------------------------------------------------------------------
    // Lookup
    // -----------------------------------------------------------------------

    /**
     * Returns the registered band for the biome the player is currently standing in,
     * or null if the biome has no registered band (vanilla zone / pre-20 area).
     */
    @Nullable
    public static BiomeLevelBand forPlayer(ServerPlayer player) {
        var biomeHolder = player.level().getBiome(player.blockPosition());
        ResourceLocation biomeId = ForgeRegistries.BIOMES.getKey(biomeHolder.value());
        if (biomeId == null) return null;

        for (BiomeLevelBand band : REGISTRY) {
            if (band.biome.equals(biomeId)) return band;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // XP calculation
    // -----------------------------------------------------------------------

    /**
     * XP rewarded for killing an enemy of the given tier in this biome.
     *
     * <p>Higher-level bands automatically scale upward so a single boss kill
     * always represents a significant XP chunk relative to the current grind rate.
     * Scale factor: +10% per 10 levels the band sits above the minimum (21).
     */
    public long xpForTier(EnemyTier tier) {
        float bandScalar = 1.0f + (minLevel - LevelConstants.VANILLA_CAP_LEVEL - 1) / 10.0f;
        return Math.round(baseGruntXp * tier.xpMultiplier * bandScalar);
    }

    /**
     * Whether this band should yield XP for a player at the given level.
     * A 5-level overflow buffer above maxLevel lets players linger and still earn.
     */
    public boolean isRewardingForLevel(int playerLevel) {
        return playerLevel >= minLevel && playerLevel <= maxLevel + 5;
    }

    @Override
    public String toString() {
        return biome + " [Lv " + minLevel + "–" + maxLevel + "] gruntXP=" + baseGruntXp;
    }
}