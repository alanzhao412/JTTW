package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.HeavenRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * How Heaven's great names look (its bosses): the Four Heavenly Kings, Li Jing, Nezha and Erlang Shen.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavenGreatRenderers {

    private HeavenGreatRenderers() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var king : java.util.List.of(HeavenRegistry.KING_CHIGUO, HeavenRegistry.KING_ZENGZHANG, HeavenRegistry.KING_GUANGMU, HeavenRegistry.KING_DUOWEN)) {
            event.registerEntityRenderer(king.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.55f, false));
        }
        event.registerEntityRenderer(HeavenRegistry.LI_JING.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.35f, false));
        event.registerEntityRenderer(HeavenRegistry.NEZHA.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 0.92f, false));
        event.registerEntityRenderer(HeavenRegistry.ERLANG_SHEN.get(), ctx -> new HeavenRenderers.Placeholder<>(ctx, 1.12f, false));
    }
}
