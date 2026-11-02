package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.HeavenRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * How Heaven's rank and file look: the heavenly archer, the cloud cavalry, the thunder drummer and the
 * Twenty-Eight Lunar Mansions (the halberdier, {@code heavenly_soldier}, is {@link HeavenRenderers}'s).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavenHostRenderers {

    private HeavenHostRenderers() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeavenRegistry.HEAVENLY_ARCHER.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1f, true));
        event.registerEntityRenderer(HeavenRegistry.CLOUD_CAVALRY.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.3f, true));
        event.registerEntityRenderer(HeavenRegistry.THUNDER_DRUMMER.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.02f, true));
        event.registerEntityRenderer(HeavenRegistry.LUNAR_MANSION.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.08f, true));
    }
}
