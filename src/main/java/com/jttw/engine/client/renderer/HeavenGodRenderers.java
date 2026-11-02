package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.HeavenRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * How Heaven's gods and star lords look (its minibosses): the Nine Luminaries, the Great Spirit Giant, the
 * Thunder Lord and the Lightning Mother, Thousand-Mile Eye and Wind-Following Ear.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavenGodRenderers {

    private HeavenGodRenderers() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeavenRegistry.STAR_LORD.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.12f, true));
        event.registerEntityRenderer(HeavenRegistry.GREAT_SPIRIT_GIANT.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 2.15f, true));
        event.registerEntityRenderer(HeavenRegistry.THUNDER_LORD.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.18f, true));
        event.registerEntityRenderer(HeavenRegistry.LIGHTNING_MOTHER.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.08f, true));
        event.registerEntityRenderer(HeavenRegistry.THOUSAND_MILE_EYE.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.08f, true));
        event.registerEntityRenderer(HeavenRegistry.WIND_FOLLOWING_EAR.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.08f, true));
    }
}
