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
import java.util.Set;
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

    /** How far his somersault momentum must build for the cloud to gather under him: all but its last half second. */
    public static final double CLOUD_GATHERS = 0.95;
    /** How far it may fall before the cloud thins away: a moment after he eases off. */
    public static final double CLOUD_THINS = 0.85;

    private static final Set<UUID> ON_CLOUD_NOW = ConcurrentHashMap.newKeySet();

    /**
     * Wukong's cloud marks his top speed: with the Somersault Cloud (Flight III), held at full
     * speed until his momentum is at its height, it gathers under him; slowing, it thins away.
     * Flying slower, and before Flight III, he flies on his own as everyone else does.
     */
    public static boolean ridesCloud(ServerPlayer p) {
        if (!flies(p) || com.jttw.engine.progression.skilltree.SkillTreeData.getFlightTier(p) < 3) {
            ON_CLOUD_NOW.remove(p.getUUID());
            return false;
        }
        double max = com.jttw.engine.character.wukong.WukongAbilityHandler.momentumMax();
        double built = (JTTWFlight.boost(p) - 1) / (max - 1);
        boolean on = built >= (ON_CLOUD_NOW.contains(p.getUUID()) ? CLOUD_THINS : CLOUD_GATHERS);
        if (on) ON_CLOUD_NOW.add(p.getUUID());
        else ON_CLOUD_NOW.remove(p.getUUID());
        return on;
    }

    public static int mode(ServerPlayer p) {
        return ridesCloud(p) ? ON_CLOUD : flies(p) ? FLYING : GROUNDED;
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
        ON_CLOUD_NOW.remove(event.getEntity().getUUID());
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
