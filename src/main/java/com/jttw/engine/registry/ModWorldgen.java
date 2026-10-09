package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Custom worldgen types: the Heaven and Hell dimensions', and the zones' features. */
public final class ModWorldgen {

    private ModWorldgen() {}

    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, JTTWEngine.MOD_ID);

    @SuppressWarnings("unused")
    public static final RegistryObject<Codec<? extends DensityFunction>> CELESTIAL_ISLANDS_DF =
            DENSITY_FUNCTION_TYPES.register("celestial_islands",
                    () -> CelestialIslandsDensityFunction.CODEC.codec());

    /** Features of the mod's zones. */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, JTTWEngine.MOD_ID);

    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<?>> BONE_MASSIF =
            FEATURES.register("bone_massif", () -> new com.jttw.engine.world.feature.BoneMassifFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<?>> BONE_SPIRE =
            FEATURES.register("bone_spire", () -> new com.jttw.engine.world.feature.BoneSpireFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<?>> DEAD_TREE =
            FEATURES.register("dead_tree", () -> new com.jttw.engine.world.feature.DeadTreeFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<?>> BOULDER =
            FEATURES.register("boulder", () -> new com.jttw.engine.world.feature.BoulderFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<?>> RIBCAGE =
            FEATURES.register("ribcage", () -> new com.jttw.engine.world.feature.RibcageFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<?>> LADY_LAIR =
            FEATURES.register("lady_lair", () -> new com.jttw.engine.world.feature.LairFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        FEATURES.register(modEventBus);
    }
}
