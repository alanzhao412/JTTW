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
 * Shares who is flying, and who rides a cloud, with the players who can see them, so each client can draw a flier lying
 * along their flight on a cloud. Sent when it changes, and to anyone who comes in sight of a flier.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FlyingState {

    private FlyingState() {}

    /** How someone is flying: not at all, on their own, or riding Wukong's cloud. */
    public static final int GROUNDED = 0, FLYING = 1, ON_CLOUD = 2;

    private static final Map<UUID, Integer> LAST = new ConcurrentHashMap<>();

    /** Whether a player is flying in a way drawn as flight: not creative's or a spectator's. */
    public static boolean flies(Player p) {
        return p.getAbilities().flying && !p.isCreative() && !p.isSpectator() && !p.isPassenger() && !p.isFallFlying();
    }

    /**
     * Wukong rides his cloud once he has the Somersault Cloud (Flight III), the flight that builds
     * to the fastest anyone flies; before that he flies as everyone else does, on his own.
     */
    public static boolean ridesCloud(Player p) {
        return com.jttw.engine.progression.skilltree.SkillTreeData.getFlightTier(p) >= 3;
    }

    public static int mode(Player p) {
        return !flies(p) ? GROUNDED : ridesCloud(p) ? ON_CLOUD : FLYING;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        int now = mode(p);
        Integer was = LAST.put(p.getUUID(), now);
        if (was == null ? now != GROUNDED : was != now) {
            FlyingStatePacket pkt = new FlyingStatePacket(p.getId(), now);
            NetworkHandler.sendToTrackingPlayers(p, pkt);
            NetworkHandler.sendToPlayer(p, pkt); // their own client draws them too, in third person
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer flier && event.getEntity() instanceof ServerPlayer watcher && flies(flier)) {
            NetworkHandler.sendToPlayer(watcher, new FlyingStatePacket(flier.getId(), mode(flier)));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    /** A new world or a new life is a new entity to the client: tell it afresh. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
