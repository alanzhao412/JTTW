package com.jttw.engine.dimension;

import com.jttw.engine.JTTWEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * When a player falls into the void in Heaven, teleport them back to the Overworld
 * instead of killing them.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class HeavenVoidHandler {
    private HeavenVoidHandler() {}

    private static final double VOID_Y = -10.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 80 != 0) return;
        if (!player.level().dimension().equals(JTTWDimensions.HEAVEN)) return;
        if (player.getY() > VOID_Y) return;

        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        // Find a safe spot at the player's original XZ in the overworld
        int x = (int) player.getX();
        int z = (int) player.getZ();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        player.teleportTo(overworld, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        // Cancel fall damage from the teleport
        player.fallDistance = 0.0F;
    }
}
