package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.renderer.TangFolkRenderer;
import com.jttw.engine.registry.ModVillages;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The Tang villages' folk and the spirit merchants, drawn (see {@link TangFolkRenderer}); kept apart from ClientInit to merge cleanly. */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TangVillageClient {

    private TangVillageClient() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TangFolkRenderer.LAYER, TangFolkRenderer::layer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModVillages.TANG_VILLAGER.get(), TangFolkRenderer::new);
        event.registerEntityRenderer(ModVillages.SPIRIT_MERCHANT.get(), TangFolkRenderer::new);
    }
}
