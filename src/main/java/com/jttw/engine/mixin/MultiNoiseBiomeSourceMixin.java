package com.jttw.engine.mixin;

import com.jttw.engine.world.Zones;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Puts the mod's zone biomes where their zones lie ({@link Zones}): a zone's region (placed on its
 * host ground) takes its whole footprint, water and all, so it stands whole, not in scraps; but
 * only at the heights it takes (an underground zone leaves the surface over it vanilla's, an
 * aerial one the ground under it). Only in a biome source that lists the zone biome, which the
 * overworld's does (OverworldBiomeBuilderMixin); the Nether's and others' are left alone. Where
 * the zones lie is found with this source the first time it is needed, if the overworld's loading
 * has not found it already.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {

    @Shadow
    private Climate.ParameterList<Holder<Biome>> parameters() {
        throw new AssertionError("shadowed");
    }

    /** This source's zone biomes, found once: empty when it lists none. */
    @Unique
    private volatile Map<Zones.Zone, Holder<Biome>> jttw$zones;

    @Unique
    private Map<Zones.Zone, Holder<Biome>> jttw$zones() {
        Map<Zones.Zone, Holder<Biome>> found = jttw$zones;
        if (found == null) {
            found = new LinkedHashMap<>();
            for (Pair<Climate.ParameterPoint, Holder<Biome>> entry : parameters().values()) {
                for (Zones.Zone zone : Zones.MAP.zones()) {
                    if (entry.getSecond().is(zone.biome())) found.put(zone, entry.getSecond());
                }
            }
            jttw$zones = found;
        }
        return found;
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private void jttw$placeZones(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        Map<Zones.Zone, Holder<Biome>> zones = jttw$zones();
        if (zones.isEmpty()) return;
        long seed = Zones.seed();
        if (!Zones.MAP.settled(seed)) Zones.settle(seed, (MultiNoiseBiomeSource) (Object) this, sampler, false);
        int bx = QuartPos.toBlock(x), by = QuartPos.toBlock(y), bz = QuartPos.toBlock(z);
        for (Map.Entry<Zones.Zone, Holder<Biome>> e : zones.entrySet()) {
            if (e.getKey().covers(seed, bx, by, bz)) {
                cir.setReturnValue(e.getValue());
                return;
            }
        }
    }
}
