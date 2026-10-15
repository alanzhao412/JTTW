package com.jttw.engine.world;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.PrivateGlowPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A glow only one player sees: the outline vanilla's Glowing draws, through walls, but on that
 * player's screen alone (Glowing itself shows to everyone). For the senses that pick creatures
 * out for their owner: Byakko's nose for the wounded, Erlang Shen's marks and his hound's
 * tracking, the Peng's eclipse, Yuan Di's dominated.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.jttw.engine.JTTWEngine.MOD_ID)
public final class PrivateGlow {

    private PrivateGlow() {}

    /** Who glows for whom, until when (game time): viewer, then entity id. */
    private static final Map<UUID, Map<Integer, Long>> SHOWN = new HashMap<>();

    /** {@code target} glows for {@code viewer} for {@code ticks}. */
    public static void show(ServerPlayer viewer, Entity target, int ticks) {
        show(viewer, java.util.List.of(target), ticks);
    }

    /** These glow for {@code viewer} for {@code ticks}. */
    public static void show(ServerPlayer viewer, Collection<? extends Entity> targets, int ticks) {
        if (targets.isEmpty()) return;
        long now = viewer.level().getGameTime(), until = now + ticks;
        Map<Integer, Long> mine = SHOWN.computeIfAbsent(viewer.getUUID(), k -> new HashMap<>());
        mine.values().removeIf(t -> t <= now);
        int[] ids = targets.stream().limit(PrivateGlowPacket.CAP).mapToInt(Entity::getId).toArray();
        for (int id : ids) mine.put(id, until);
        NetworkHandler.sendToPlayer(viewer, new PrivateGlowPacket(ticks, ids));
    }

    /** Whether {@code target} glows for {@code viewer} now. */
    public static boolean glowsFor(ServerPlayer viewer, Entity target) {
        Map<Integer, Long> mine = SHOWN.get(viewer.getUUID());
        Long until = mine == null ? null : mine.get(target.getId());
        return until != null && viewer.level().getGameTime() < until;
    }

    /** {@code target} stops glowing for {@code viewer}. */
    public static void hide(ServerPlayer viewer, Entity target) {
        show(viewer, target, 0);
    }

    /** A viewer gone: what glowed for them is forgotten. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SHOWN.remove(event.getEntity().getUUID());
    }
}
