package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Custom worldgen types: the Heaven and Hell dimensions', and the zones' features. */
public final class ModWorldgen {

    private ModWorldgen() {}

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, JTTWEngine.MOD_ID);

    @SuppressWarnings("unused")
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<? extends DensityFunction>> CELESTIAL_ISLANDS_DF =
            DENSITY_FUNCTION_TYPES.register("celestial_islands",
                    () -> CelestialIslandsDensityFunction.CODEC.codec());

    /** Features of the mod's zones. */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, JTTWEngine.MOD_ID);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> BONE_MASSIF =
            FEATURES.register("bone_massif", () -> new com.jttw.engine.world.feature.BoneMassifFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> BONE_SPIRE =
            FEATURES.register("bone_spire", () -> new com.jttw.engine.world.feature.BoneSpireFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> DEAD_TREE =
            FEATURES.register("dead_tree", () -> new com.jttw.engine.world.feature.DeadTreeFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> BOULDER =
            FEATURES.register("boulder", () -> new com.jttw.engine.world.feature.BoulderFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> RIBCAGE =
            FEATURES.register("ribcage", () -> new com.jttw.engine.world.feature.RibcageFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> LADY_LAIR =
            FEATURES.register("lady_lair", () -> new com.jttw.engine.world.feature.LairFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> HEAVEN_BUILDING =
            FEATURES.register("heaven_building", () -> new com.jttw.engine.world.heaven.HeavenFeatures.IslandBuilding(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, net.minecraft.world.level.levelgen.feature.Feature<?>> JADE_PALACE =
            FEATURES.register("jade_palace", () -> new com.jttw.engine.world.heaven.HeavenFeatures.JadePalace(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        FEATURES.register(modEventBus);
    }
}
