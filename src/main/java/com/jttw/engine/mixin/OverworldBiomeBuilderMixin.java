package com.jttw.engine.mixin;

import com.jttw.engine.world.Zones;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Lists the mod's zone biomes among the overworld's, at a climate no terrain reaches: so they are
 * overworld biomes (their features ordered, their colours sent), but vanilla never chooses them.
 * Where they go is decided by MultiNoiseBiomeSourceMixin.
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void jttw$listZones(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        if (Zones.BUILDING_VANILLA_REGISTRIES.get()) return;
        for (Zones.Zone zone : Zones.MAP.zones()) consumer.accept(Pair.of(Zones.UNREACHABLE, zone.biome()));
    }
}
