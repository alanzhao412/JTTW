package com.jttw.engine.mixin;

import com.jttw.engine.dimension.TallOverworld;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gives the GameTest server the dimensions data packs add, Heaven and Hell among them: since 1.21
 * it builds its world from the flat preset alone. And its Overworld the taller sky new worlds have
 * ({@link TallOverworld}), so the tests see the world as a new one is: the flat preset itself keeps
 * vanilla's, as superflat worlds do. Only the GameTest server is affected.
 */
@Mixin(GameTestServer.class)
public class GameTestServerMixin {

    @ModifyArg(method = "/^lambda\\$create\\$\\d+$/",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static Registry<LevelStem> jttw$withDatapackDimensions(Registry<LevelStem> none,
                                                                     @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
    }

    @ModifyExpressionValue(method = "/^lambda\\$create\\$\\d+$/",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/presets/WorldPreset;createWorldDimensions()Lnet/minecraft/world/level/levelgen/WorldDimensions;"))
    private static WorldDimensions jttw$underTheTallerSky(WorldDimensions flat, @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        var tall = context.datapackWorldgen().registryOrThrow(Registries.DIMENSION_TYPE).getHolder(TallOverworld.TYPE);
        LevelStem overworld = flat.dimensions().get(LevelStem.OVERWORLD);
        if (tall.isEmpty() || overworld == null) return flat;
        Map<ResourceKey<LevelStem>, LevelStem> out = new LinkedHashMap<>(flat.dimensions());
        out.put(LevelStem.OVERWORLD, new LevelStem(tall.get(), overworld.generator()));
        return new WorldDimensions(out);
    }
}
