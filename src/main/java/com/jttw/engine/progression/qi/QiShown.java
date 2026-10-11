package com.jttw.engine.progression.qi;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.QiSeenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each player's Qi, shared with the players who can see them, so truth-seeing eyes (the Fiery
 * Golden Eyes, the Eye of Heaven) can read it. Sent as it changes, by whole points.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class QiShown {

    private QiShown() {}

    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    private static long packed(ServerPlayer p) {
        return ((long) Math.round(QiData.getCurrent(p)) << 32) | (Math.round(QiData.getMax(p)) & 0xFFFFFFFFL);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        long now = packed(p);
        Long was = LAST.put(p.getUUID(), now);
        if (was == null || was != now) {
            NetworkHandler.sendToTrackingPlayers(p, new QiSeenPacket(p.getId(), QiData.getCurrent(p), QiData.getMax(p)));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer seen && event.getEntity() instanceof ServerPlayer watcher) {
            NetworkHandler.sendToPlayer(watcher, new QiSeenPacket(seen.getId(), QiData.getCurrent(seen), QiData.getMax(seen)));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
