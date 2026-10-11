package com.jttw.engine.progression.stats;

import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import javax.annotation.Nullable;

/**
 * The four Divine Beast elemental lines.
 *
 * <p>Each line has a domain condition that grants a stat buff when the player
 * is in their element. See {@link DivineBeastHandler} for the buff application.
 *
 * <ul>
 *   <li>WATER (Dragon line): oceans, rivers, and during rain</li>
 *   <li>EARTH (World Turtle line): below sea level</li>
 *   <li>BEAST (Byakko line): forests, jungles, plains, savannahs</li>
 *   <li>FIRE (Solar Sovereign line): deserts (badlands among them) and mountains</li>
 * </ul>
 */
public enum DivineBeastLine {

    WATER("Dragon"),
    EARTH("World Turtle"),
    BEAST("Byakko"),
    FIRE("Solar Sovereign");

    public final String apexName;

    DivineBeastLine(String apexName) {
        this.apexName = apexName;
    }

    /**
     * Check if the player is currently in this line's domain.
     */
    public boolean isInDomain(ServerPlayer player) {
        return switch (this) {
            case WATER -> isWaterDomain(player);
            case EARTH -> player.getY() < player.level().getSeaLevel();
            case BEAST -> isVerdantDomain(player);
            case FIRE -> isAridDomain(player);
        };
    }

    private static boolean isWaterDomain(ServerPlayer player) {
        // Rain counts
        if (player.level().isRainingAt(player.blockPosition().above())) return true;
        // In water
        if (player.isInWater()) return true;
        // Ocean/river biomes
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }

    private static boolean isAridDomain(ServerPlayer player) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        // Deserts (the badlands with them) and mountains
        return biome.is(Biomes.DESERT)
                || biome.is(BiomeTags.IS_BADLANDS)
                || biome.is(BiomeTags.IS_MOUNTAIN);
    }

    private static boolean isVerdantDomain(ServerPlayer player) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        // Forests, jungles, plains and savannahs, and their kinds
        return biome.is(BiomeTags.IS_FOREST)
                || biome.is(BiomeTags.IS_JUNGLE)
                || biome.is(BiomeTags.IS_SAVANNA)
                || biome.is(Biomes.PLAINS)
                || biome.is(Biomes.SUNFLOWER_PLAINS)
                || biome.is(Biomes.SNOWY_PLAINS);
    }

    /**
     * Derive the beast line from a reincarnation outcome.
     * Returns null if the outcome doesn't map to a Divine Beast.
     */
    @Nullable
    public static DivineBeastLine fromOutcome(ReincarnationOutcome outcome) {
        return switch (outcome) {
            case DRAGON      -> WATER;
            case PHOENIX     -> FIRE;
            case WHITE_TIGER -> BEAST;
            case TURTLE      -> EARTH;
            default -> null;
        };
    }

    /** Parse from a saved NBT string. Returns null if unrecognised. */
    @Nullable
    public static DivineBeastLine parse(String name) {
        if (name == null || name.isBlank()) return null;
        for (DivineBeastLine line : values()) {
            if (line.name().equalsIgnoreCase(name.trim())) return line;
        }
        return null;
    }
}
