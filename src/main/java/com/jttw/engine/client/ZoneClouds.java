package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.Zones;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;

/**
 * The clouds over the dark zones take their sky's colour: vanilla's bright white clouds would sit
 * oddly over Black Wind Mountain's ash-grey sky, the Flaming Mountains' orange one or Silk-Web
 * Ridge's violet murk. Each such zone's biome has a cloud tint, multiplied into vanilla's cloud
 * colour (so night, rain and thunder still darken it), {@code ClientLevelMixin}.
 *
 * <p>The tint is sampled round the camera as vanilla samples the sky's colour (a gaussian over the
 * biomes some twenty blocks about), so it fades in across a zone's border, and eased from tick to
 * tick besides, so a quick flight over the border does not snap it. Client-side only.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ZoneClouds {

    private ZoneClouds() {}

    /** The cloud tint of each zone whose sky is dark or coloured, as a multiplier on vanilla's cloud colour. */
    public static final Map<ResourceKey<Biome>, Vec3> TINTS = Map.of(
            Zones.BLACK_WIND_BIOME, new Vec3(0.30, 0.30, 0.33),    // dark, ashy: under the black winds
            Zones.FIRE_MOUNTAIN_BIOME, new Vec3(0.90, 0.52, 0.34), // smoky orange, lit from the fires below
            Zones.SPIDER_CAVE_BIOME, new Vec3(0.62, 0.56, 0.72),   // violet-grey, a murk of silk and poison
            Zones.FENGDU_BIOME, new Vec3(0.46, 0.48, 0.58),        // the Ghost City's leaden grey-blue
            Zones.LION_CAMEL_RIDGE_BIOME, new Vec3(0.66, 0.67, 0.73)); // the demons' ridge: a dim, dusty grey

    /** Of the gap to the sampled tint, closed each tick (some two seconds to settle). */
    public static final double EASE = 0.08;

    /** The tint now (eased), and how much of it is laid on (0: vanilla's clouds; 1: the zone's wholly). */
    private static Vec3 tint = new Vec3(1, 1, 1);
    private static double weight = 0;

    /** Vanilla's cloud colour with a zone's tint laid on it at {@code w} (0 to 1). */
    public static Vec3 blend(Vec3 vanilla, Vec3 tint, double w) {
        w = Mth.clamp(w, 0, 1);
        return new Vec3(vanilla.x * Mth.lerp(w, 1, tint.x), vanilla.y * Mth.lerp(w, 1, tint.y), vanilla.z * Mth.lerp(w, 1, tint.z));
    }

    /** The cloud colour to draw, vanilla's tinted by the zone round the camera. */
    public static Vec3 cloudColour(Vec3 vanilla) {
        return weight < 1e-3 ? vanilla : blend(vanilla, tint, weight);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            if (level == null) weight = 0;
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 at = camera.isInitialized() ? camera.getPosition() : mc.player != null ? mc.player.position() : null;
        if (at == null) return;
        Vec3 q = at.subtract(2, 2, 2).scale(0.25); // as vanilla's sky colour samples: quarts, centred
        var biomes = level.getBiomeManager();
        // the tints' sum, each weighted by its share, and their shares
        Vec3 sum = CubicSampler.gaussianSampleVec3(q, (x, y, z) -> {
            Vec3 t = biomes.getNoiseBiomeAtQuart(x, y, z).unwrapKey().map(TINTS::get).orElse(null);
            return t == null ? Vec3.ZERO : t;
        });
        double share = CubicSampler.gaussianSampleVec3(q, (x, y, z) ->
                biomes.getNoiseBiomeAtQuart(x, y, z).unwrapKey().map(TINTS::containsKey).orElse(false) ? new Vec3(1, 1, 1) : Vec3.ZERO).x;
        Vec3 want = share > 1e-4 ? sum.scale(1 / share) : tint;
        weight += (share - weight) * EASE;
        tint = tint.lerp(want, weight < 1e-3 ? 1 : EASE * 2);
    }
}
