package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.wukong.EyeLaser;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.EyeLaserOutputPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * The Fiery Golden Eyes on N: a tap fires the laser (as N comes up), and the mouse wheel turned
 * while N is held sets its output instead, up for more and down for less, without firing. Sneaking
 * with N still turns the x-ray on and off at once, as before.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class EyeLaserInput {

    private EyeLaserInput() {}

    /** N is down on the laser (not sneaking), and whether the wheel has turned since. */
    private static boolean held, scrolled;

    /** Whether N, pressed now on this slot, is the laser's: held for the wheel, firing as it comes up. */
    static boolean isLaser(Minecraft mc, AbilitySlot slot) {
        return slot != null && EyeLaser.ABILITY_ID.equals(slot.id()) && mc.player != null
                && !mc.player.isShiftKeyDown() && ClientAbilityData.serverIndex(2) < ClientAbilityData.PAGE_SIZE;
    }

    /** N went down on the laser. */
    static void press() {
        held = true;
        scrolled = false;
    }

    /** N came up: whether to fire (it was held for the laser, the wheel did not turn, and he is not sneaking now). */
    static boolean release(Minecraft mc) {
        boolean fire = held && !scrolled && mc.player != null && !mc.player.isShiftKeyDown();
        held = false;
        scrolled = false;
        return fire;
    }

    /** A screen opened over a held N: forget it, nothing fires. */
    static void cancel() {
        held = false;
        scrolled = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!held || mc.player == null || mc.screen != null) return;
        if (ClientInit.ABILITY_SLOT_2 == null || !ClientInit.ABILITY_SLOT_2.isDown()) return;
        double dy = event.getScrollDeltaY();
        if (dy == 0) return;
        event.setCanceled(true); // not the hotbar
        scrolled = true;
        NetworkHandler.sendToServer(new EyeLaserOutputPacket(dy > 0 ? 1 : -1));
    }
}
