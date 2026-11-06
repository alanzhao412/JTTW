package com.jttw.engine.world.kunlun;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.KunlunFeature;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Mount Kunlun's stair laid out as the server starts, off its thread, before anyone comes near.
 * Its path ({@link KunlunShape#path}) is some seconds' search, made once a region and kept only
 * while the server runs; made by the first of its chunks to be generated, every other of its chunks
 * waited on it, and a player arriving there by a leap (Somersault, Dimension Travel, the Great Void)
 * held the server still for them: six seconds where new vanilla ground took two. It is worked from
 * the world's noise alone (no chunk is touched), as the worldgen threads work it themselves.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class KunlunWarmup {

    private KunlunWarmup() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        warm(event.getServer().overworld());
    }

    /** Lays out the stair of each of the world's Kunluns on the background executor; done when all are. */
    public static CompletableFuture<Void> warm(ServerLevel overworld) {
        long seed = overworld.getSeed();
        int[][] centres = Zones.KUNLUN.centres(seed);
        CompletableFuture<?>[] all = new CompletableFuture<?>[centres.length];
        for (int i = 0; i < centres.length; i++) {
            int[] c = centres[i];
            all[i] = CompletableFuture.runAsync(() -> {
                long t0 = System.nanoTime();
                KunlunFeature.path(overworld, c[0], c[1]);
                JTTWEngine.LOGGER.info("Kunlun at {} {}: its stair laid out in {} ms, before anyone came", c[0], c[1],
                        (System.nanoTime() - t0) / 1_000_000);
            }, Util.backgroundExecutor()).exceptionally(e -> {
                JTTWEngine.LOGGER.warn("Kunlun's stair could not be laid out ahead (it will be when its ground is made)", e);
                return null;
            });
        }
        return CompletableFuture.allOf(all);
    }
}
