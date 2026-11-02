package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.renderer.WildRenderers;
import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.registry.ModGrinds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The raided villages' chief, drawn: the ogre's frame in his own skin ({@code ogre_chief.png}), {@value RaidChiefEntity#SIZE} times an ogre's size. */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GrindClient {

    private GrindClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModGrinds.RAID_CHIEF.get(),
                (net.minecraft.client.renderer.entity.EntityRendererProvider<com.jttw.engine.entity.wild.OgreEntity>) ctx -> new WildRenderers.OgreRenderer(ctx, "ogre_chief", WildRenderers.OgreRenderer.SIZE * RaidChiefEntity.SIZE));
    }
}
