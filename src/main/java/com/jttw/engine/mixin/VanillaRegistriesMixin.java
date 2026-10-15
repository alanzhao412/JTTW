package com.jttw.engine.mixin;

import com.jttw.engine.world.Zones;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks while vanilla builds its registries in code, so OverworldBiomeBuilderMixin leaves the
 * zone biomes out: that build knows only vanilla's biomes, and since 1.21 it fails on a biome it
 * was not given (the server runs it at startup).
 */
@Mixin(VanillaRegistries.class)
public class VanillaRegistriesMixin {

    @Inject(method = "createLookup", at = @At("HEAD"))
    private static void jttw$enter(CallbackInfoReturnable<HolderLookup.Provider> cir) {
        Zones.BUILDING_VANILLA_REGISTRIES.set(true);
    }

    @Inject(method = "createLookup", at = @At("RETURN"))
    private static void jttw$exit(CallbackInfoReturnable<HolderLookup.Provider> cir) {
        Zones.BUILDING_VANILLA_REGISTRIES.set(false);
    }
}
