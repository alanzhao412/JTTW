package com.jttw.engine.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.FlyingStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shares who is flying with the players who can see them, so each client can draw a flier lying
 * along their flight on a cloud. Sent when it changes, and to anyone who comes in sight of a flier.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FlyingState {

    private FlyingState() {}

    private static final Map<UUID, Boolean> LAST = new ConcurrentHashMap<>();

    /** Whether a player is flying in a way that is drawn on a cloud: not creative's or a spectator's. */
    public static boolean flies(Player p) {
        return p.getAbilities().flying && !p.isCreative() && !p.isSpectator() && !p.isPassenger() && !p.isFallFlying();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        boolean now = flies(p);
        Boolean was = LAST.put(p.getUUID(), now);
        if (was == null ? now : was != now) {
            NetworkHandler.sendToTrackingPlayers(p, new FlyingStatePacket(p.getId(), now));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer flier && event.getEntity() instanceof ServerPlayer watcher && flies(flier)) {
            NetworkHandler.sendToPlayer(watcher, new FlyingStatePacket(flier.getId(), true));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
