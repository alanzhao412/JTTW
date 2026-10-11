package com.jttw.engine.progression.stats;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.BeastFormPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How far a Divine Beast has grown into its beast, for drawing: 0 (still in human shape) to 5,
 * one step at each form (levels 20, 40, 60, 80) and the last at level 100. Shared with the players
 * who can see them, as it changes.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BeastFormShown {

    private BeastFormShown() {}

    public static final int APEX_LEVEL = 100;

    private static final Map<UUID, Integer> LAST = new ConcurrentHashMap<>();

    /** The look's stage for a beast of this level: 0 to 5. */
    public static int stage(int level) {
        return FormStage.forLevel(level).stageIndex + (level >= APEX_LEVEL ? 1 : 0);
    }

    /** This player's line and stage packed (line ordinal * 8 + stage), or -1 if not a Divine Beast. */
    public static int packed(ServerPlayer p) {
        if (PlayerLevel.getProgressionClass(p) != ProgressionClass.DIVINE_BEAST) return -1;
        DivineBeastLine line = PlayerLevel.getBeastLine(p);
        if (line == null) return -1;
        return line.ordinal() * 8 + stage(PlayerLevel.getLevel(p));
    }

    private static void send(ServerPlayer p, int packed) {
        BeastFormPacket pkt = new BeastFormPacket(p.getId(), packed);
        NetworkHandler.sendToPlayer(p, pkt);
        NetworkHandler.sendToTrackingPlayers(p, pkt);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        int now = packed(p);
        Integer was = LAST.put(p.getUUID(), now);
        if (was == null ? now >= 0 : was != now) send(p, now);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer seen && event.getEntity() instanceof ServerPlayer watcher) {
            int now = packed(seen);
            if (now >= 0) NetworkHandler.sendToPlayer(watcher, new BeastFormPacket(seen.getId(), now));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
