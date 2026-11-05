package com.jttw.engine.dimension;

import com.jttw.engine.JTTWEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * When a player falls into the void in Heaven, teleport them back to the Overworld
 * instead of killing them: where they were, on its surface ({@link RealmArrival}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class HeavenVoidHandler {
    private HeavenVoidHandler() {}

    private static final double VOID_Y = -10.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // each tick: looked at once in four seconds, a faller dropped some 300 blocks into the realm's void first
        if (!player.level().dimension().equals(JTTWDimensions.HEAVEN)) return;
        if (player.getY() > VOID_Y) return;

        // down to where they were in the Overworld, on its surface; over water or nothing, their respawn
        // point (RealmArrival, as the realm leaps: the ground's chunk made before its height is read)
        if (!RealmArrival.travel(player, Level.OVERWORLD)) return;
        // Cancel fall damage from the teleport
        player.fallDistance = 0.0F;
    }
}
