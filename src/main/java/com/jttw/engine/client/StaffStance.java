package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.RuyiJinguBangItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How one who holds the Ruyi Jingu Bang stands with it. At ease (standing or walking, not
 * fighting) it rests across the shoulders behind the head, arms draped over it, as Sun Wukong
 * carries it; striking, it is in hand, and it stays there a moment after the last blow before
 * going back up. Flying, swimming, crouching, riding, or with the staff grown large, it is held.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class StaffStance {

    private StaffStance() {}

    /** Ticks after a blow before the staff goes back across the shoulders. */
    public static final int AT_EASE_AFTER = 20;

    /** The game time each player (by entity id) last swung. */
    private static final Map<Integer, Long> LAST_SWING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LAST_SWING.clear();
            return;
        }
        long now = mc.level.getGameTime();
        for (Player p : mc.level.players()) if (p.swinging) LAST_SWING.put(p.getId(), now);
        LAST_SWING.values().removeIf(t -> now - t > AT_EASE_AFTER * 4L);
    }

    /** Whether {@code p} carries the staff across the shoulders now. */
    public static boolean resting(Player p) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof RuyiJinguBangItem) || RuyiJinguBangItem.isLargeMode(s)) return false;
        if (p.swinging || p.isUsingItem()) return false;
        Minecraft mc = Minecraft.getInstance();
        Long swung = LAST_SWING.get(p.getId());
        if (swung != null && mc.level != null && mc.level.getGameTime() - swung < AT_EASE_AFTER) return false;
        if (com.jttw.engine.client.flight.FlightPose.presence(p, 1) > 0) return false;
        return !p.isSwimming() && !p.isVisuallySwimming() && !p.isFallFlying() && !p.isPassenger()
                && !p.isSleeping() && !p.isCrouching() && !p.isSpectator();
    }
}
