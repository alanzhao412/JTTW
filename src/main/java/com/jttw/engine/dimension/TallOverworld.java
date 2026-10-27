package com.jttw.engine.dimension;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * The Overworld under a taller sky, for worlds made from now on: its build height raised from y 320
 * to y {@value #TOP} (from y {@value #MIN_Y}, {@value #HEIGHT} blocks), so Mount Kunlun can stand far
 * over every vanilla mountain. Its dimension type ({@code data/jttw_engine/dimension_type/tall_overworld})
 * is vanilla's overworld in all else; the world presets new worlds are made from (default, large
 * biomes, amplified, single biome: {@code data/minecraft/worldgen/world_preset/}) give their Overworld
 * this type instead of vanilla's. Superflat and debug worlds keep vanilla's.
 * <p>
 * Only new worlds: a world keeps the dimension type it was made with, written in its level.dat, so an
 * older world's Overworld stays {@code minecraft:overworld}, 320 high, and loads as it always did
 * (raising a world's sky under its saved chunks would shift every section of them). Its land is
 * vanilla's to the last block: the noise settings are untouched (y -64 to 320), the noise is cut to
 * the lower of the two heights, and over it there is only open sky; features, structures and light
 * reach the new top. Code that asks how high a world is asks the world ({@link #isTall}), never this
 * class's numbers.
 */
public final class TallOverworld {

    private TallOverworld() {}

    /** The taller Overworld's dimension type. */
    public static final ResourceKey<net.minecraft.world.level.dimension.DimensionType> TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, JTTWEngine.id("tall_overworld"));

    /** Its floor, its height and its top (the first y over the world). */
    public static final int MIN_Y = -64, HEIGHT = 704, TOP = MIN_Y + HEIGHT;
    /** Vanilla's top: the Overworld's of an older world, and the land's own ceiling in either. */
    public static final int VANILLA_TOP = 320;

    /** Whether this world's sky is the taller one (higher than vanilla's). */
    public static boolean isTall(LevelHeightAccessor level) {
        return level.getMaxBuildHeight() > VANILLA_TOP;
    }

    /** Whether this dimension type is a taller sky's. */
    public static boolean isTall(DimensionType type) {
        return type.minY() + type.height() > VANILLA_TOP;
    }
}
