package com.jttw.engine.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gives the GameTest server the dimensions data packs add, Heaven and Hell among them: since 1.21
 * it builds its world from the flat preset alone. Only the GameTest server is affected.
 */
@Mixin(GameTestServer.class)
public class GameTestServerMixin {

    @ModifyArg(method = "/^lambda\\$create\\$\\d+$/",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static Registry<LevelStem> jttw$withDatapackDimensions(Registry<LevelStem> none,
                                                                     @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return context.datapackDimensions().registryOrThrow(net.minecraft.core.registries.Registries.LEVEL_STEM);
    }
}
