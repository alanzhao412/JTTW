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

    /** How someone is flying: not at all, on their own, riding Wukong's cloud, or as Erlang, through wind and fog. */
    public static final int GROUNDED = 0, FLYING = 1, ON_CLOUD = 2, IN_SQUALL = 3;

    private static final Map<UUID, Integer> LAST = new ConcurrentHashMap<>();

    /** On the client: whether its own player flies by their own power (from their flight profile). */
    public static volatile boolean localOwnFlight;

    /**
     * Whether a player is flying in a way drawn as flight: by their own power (a character's
     * flight, a form's wings), in survival or creative; not a spectator's, nor creative's alone.
     */
    public static boolean flies(Player p) {
        if (!p.getAbilities().flying || p.isSpectator() || p.isPassenger() || p.isFallFlying()) return false;
        if (!p.isCreative()) return true;
        return p instanceof ServerPlayer sp ? JTTWFlight.profile(sp).own() : localOwnFlight;
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

    /** Erlang Shen flies wrapped in a dark squall (Kuangfeng Shenwu), never on a cloud. */
    public static boolean isErlang(Player p) {
        return com.jttw.engine.reincarnation.ReincarnationOutcome.EYE_OF_HEAVEN.id.equals(
                p.getPersistentData().getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME));
    }

    public static int mode(ServerPlayer p) {
        if (ridesCloud(p)) return ON_CLOUD;
        if (!flies(p)) return GROUNDED;
        return isErlang(p) ? IN_SQUALL : FLYING;
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
