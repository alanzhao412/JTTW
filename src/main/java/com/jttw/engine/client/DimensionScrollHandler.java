package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.c2s.DimensionSelectPacket;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.character.dragon.DragonSkillTree;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side scroll wheel handler for Dragon dimension selection.
 *
 * <p>When a Dragon player has flight tier 3 unlocked, scrolling while sneaking
 * cycles through available dimensions instead of switching hotbar slots.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DimensionScrollHandler {
    private DimensionScrollHandler() {}

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Only active while sneaking — so normal scroll still works for hotbar
        if (!mc.player.isShiftKeyDown()) return;

        // Check if the player has Dragon flight tier 3 unlocked
        if (!ClientAbilityData.hasNode(DragonSkillTree.FLIGHT_3)) return;

        // Cancel vanilla scroll and send dimension selection to server
        event.setCanceled(true);
        int direction = (event.getScrollDelta() > 0) ? 1 : -1;
        NetworkHandler.sendToServer(new DimensionSelectPacket(direction));
    }
}
