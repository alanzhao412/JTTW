package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.StaffScalePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Intercepts scroll wheel when looking at a placed staff entity.
 * Sends scale adjustment packets to the server.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StaffScrollHandler {
    private StaffScrollHandler() {}

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult entityHit)) return;

        Entity target = entityHit.getEntity();
        if (!(target instanceof PlacedStaffEntity staff)) return;

        if (!staff.isOwner(mc.player)) return;

        // Cancel vanilla scroll (hotbar switching) and send scale packet
        event.setCanceled(true);
        float delta = (float) event.getScrollDelta(); // +1 up, -1 down
        NetworkHandler.sendToServer(new StaffScalePacket(staff.getId(), delta));
    }
}
