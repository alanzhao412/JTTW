package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.ArmamentsShownPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which of a character's armaments are drawn on their body: all they have about them (in their
 * armory, worn, or in hand) but what is in their hands, which is drawn there. Shared with the
 * players who can see them, as it changes.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ArmamentsShown {

    private ArmamentsShown() {}

    private static final Map<UUID, List<String>> LAST = new ConcurrentHashMap<>();

    /** The armaments drawn on this player's body, by id. */
    public static List<String> shown(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        for (Armaments.Armament a : Armory.slots(p)) {
            ItemStack s = Armory.find(p, a.id());
            if (s.isEmpty() || a.is().test(p.getMainHandItem()) || a.is().test(p.getOffhandItem())) continue;
            out.add(a.id());
        }
        return out;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        List<String> now = shown(p);
        List<String> was = LAST.put(p.getUUID(), now);
        if (!now.equals(was == null ? List.of() : was)) {
            ArmamentsShownPacket pkt = new ArmamentsShownPacket(p.getId(), now);
            NetworkHandler.sendToPlayer(p, pkt);
            NetworkHandler.sendToTrackingPlayers(p, pkt);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer seen && event.getEntity() instanceof ServerPlayer watcher) {
            List<String> now = shown(seen);
            if (!now.isEmpty()) NetworkHandler.sendToPlayer(watcher, new ArmamentsShownPacket(seen.getId(), now));
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
