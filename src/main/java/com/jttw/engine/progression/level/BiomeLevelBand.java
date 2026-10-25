package com.jttw.engine.progression.level;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Associates biomes with a level band and base XP for post-20 grinding progression.
 *
 * <p><b>Registration:</b> each zone's band is registered from its level range in
 * {@link com.jttw.engine.world.Zones} ({@link com.jttw.engine.world.Zones#registerLevelBands}, from
 * {@link com.jttw.engine.JTTWEngine}'s common setup); a place without a band (Fengdu) is left out.
 * Another can be added the same way:
 *
 * <pre>{@code
 * BiomeLevelBand.register(new BiomeLevelBand(
 *     ResourceLocation.fromNamespaceAndPath("minecraft", "jungle"),
 *     21, 40,
 *     80   // grunt gives 80 XP, general 400, boss 1600
 * ));
 * }</pre>
 *
 * <p>Bands may overlap; the first registered match wins (register more specific first).
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
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
        if (biomeId == null) return null;

        for (BiomeLevelBand band : REGISTRY) {
            if (band.biome.equals(biomeId)) return band;
        }
        return null;
    }

    /** The registered band for a biome, or null. */
    @Nullable
    public static BiomeLevelBand forBiome(ResourceLocation biomeId) {
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