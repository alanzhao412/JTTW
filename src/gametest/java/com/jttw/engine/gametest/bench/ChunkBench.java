package com.jttw.engine.gametest.bench;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlightLookAhead;
import com.jttw.engine.flight.FlightSteering;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Measures how fast this machine makes the world, and whether a flier at top speed finds it
 * ready, with and without {@link FlightLookAhead}. Runs in a real client on a normally generated
 * world ({@code ./gradlew runWorldgenBench}), writes {@code run-bench/bench-<time>.txt}, and quits.
 *
 * <ol>
 *   <li>Generation: a 12x12 block of untouched chunks, requested at once; chunks a second.</li>
 *   <li>Flight: the player flies at {@link FlightSteering#TOP_SPEED} in a straight line through
 *       untouched land for 20 seconds, once without the look-ahead and once with it, each over
 *       fresh ground. Like a real client, he moves on only into a loaded chunk and otherwise waits
 *       (a stall). Reported: the speed he actually made, the ticks he stalled, how far ahead the
 *       loaded world reached, and the server's milliseconds per tick (did it keep up).</li>
 *   <li>Explored land: once both paths are unloaded to disk, each is flown again the same way,
 *       so the world is loaded rather than generated.</li>
 * </ol>
 * Carrying him on regardless would make the server generate each chunk he lands in on the spot,
 * freezing its tick; that measures lag, not flight.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ChunkBench {

    private ChunkBench() {}

    private static final boolean ENABLED = Boolean.getBoolean("jttw.chunkBench");
    private static final TicketType<ChunkPos> BENCH = TicketType.create("jttw_bench", Comparator.comparingLong(ChunkPos::toLong));
    private static final int GRID = 12;
    private static final int FLIGHT_TICKS = 400;
    private static final int SETTLE_TICKS = 200;
    private static final double ALTITUDE = 200;

    private enum Step { WAIT, GENERATE, SETTLE_1, FLY_WITHOUT, SETTLE_2, FLY_WITH, UNLOAD, REFLY_WITHOUT, SETTLE_3, REFLY_WITH, DONE }
    /** Long enough, with the player far away, for the flown paths to be unloaded to disk. */
    private static final int UNLOAD_TICKS = 1200;

    private static boolean worldRequested;
    private static Step step = Step.WAIT;
    private static int ticks;
    private static long startNanos;
    private static final List<ChunkPos> grid = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static double x;
    private static double startX;
    /** Where each first flight ended: a re-flight stops there, so it stays over explored land. */
    private static double endWithout, endWith;
    /** 0 while exploring; otherwise where this flight must stop. */
    private static double stopAt;
    private static int flownTicks;
    private static int stalls;
    private static double leadSum;
    private static int leadSamples;
    private static long lastTickNanos;
    private static double tickMsSum;
    private static double tickMsMax;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && mc.level == null && mc.screen != null && mc.getOverlay() == null) {
            worldRequested = true;
            mc.options.onboardAccessibility = false;
            LevelSettings settings = new LevelSettings("jttw_bench_" + System.currentTimeMillis(), GameType.CREATIVE,
                    false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(12345L, true, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL)
                            .value().createWorldDimensions());
        }
        if (mc.level != null) {
            // A paused singleplayer game stops the server, and the bench with it
            mc.options.pauseOnLostFocus = false;
            if (mc.screen != null) mc.setScreen(null);
        }
        if (step == Step.DONE) {
            step = null;
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || step == null || step == Step.DONE) return;
        MinecraftServer server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (step) {
            case WAIT -> {
                if (p.tickCount < 100) return;
                int view = Integer.getInteger("jttw.chunkBench.view", 0); // e.g. -Pbench.view=6
                if (view > 0) server.getPlayerList().setViewDistance(view);
                report.add("view distance: " + server.getPlayerList().getViewDistance()
                        + " chunks; cores: " + Runtime.getRuntime().availableProcessors());
                int base = 20000; // chunks: far from spawn, never touched
                for (int cx = 0; cx < GRID; cx++) for (int cz = 0; cz < GRID; cz++) {
                    ChunkPos c = new ChunkPos(base + cx, cz);
                    grid.add(c);
                    level.getChunkSource().addRegionTicket(BENCH, c, 0, c);
                }
                startNanos = System.nanoTime();
                next(Step.GENERATE);
            }
            case GENERATE -> {
                long loaded = grid.stream().filter(c -> level.getChunkSource().getChunkNow(c.x, c.z) != null).count();
                if (loaded < grid.size() && ticks < 20 * 300) return;
                double seconds = (System.nanoTime() - startNanos) / 1e9;
                report.add(String.format("generation: %d new chunks in %.1f s = %.1f chunks/s", loaded, seconds, loaded / seconds));
                for (ChunkPos c : grid) level.getChunkSource().removeRegionTicket(BENCH, c, 0, c);
                next(Step.SETTLE_1);
            }
            case SETTLE_1 -> {
                if (ticks < SETTLE_TICKS) return;
                FlightLookAhead.enabled = false;
                startFlight(p, level, 600_000);
                next(Step.FLY_WITHOUT);
            }
            case FLY_WITHOUT -> {
                if (!fly(p, level)) return;
                report.add(flightLine("without look-ahead"));
                endWithout = x;
                next(Step.SETTLE_2);
            }
            case SETTLE_2 -> {
                if (ticks < SETTLE_TICKS) return;
                FlightLookAhead.enabled = true;
                startFlight(p, level, 1_200_000);
                next(Step.FLY_WITH);
            }
            case FLY_WITH -> {
                if (!fly(p, level)) return;
                report.add(flightLine("with look-ahead"));
                endWith = x;
                p.teleportTo(level, 0, ALTITUDE, 0, 0f, 0f); // away, so both paths unload
                next(Step.UNLOAD);
            }
            case UNLOAD -> {
                if (ticks < UNLOAD_TICKS) return;
                FlightLookAhead.enabled = false;
                startFlight(p, level, 600_000);
                stopAt = endWithout;
                next(Step.REFLY_WITHOUT);
            }
            case REFLY_WITHOUT -> {
                if (!fly(p, level)) return;
                report.add(flightLine("over explored land, without look-ahead"));
                p.teleportTo(level, 0, ALTITUDE, 0, 0f, 0f);
                next(Step.SETTLE_3);
            }
            case SETTLE_3 -> {
                if (ticks < SETTLE_TICKS) return;
                FlightLookAhead.enabled = true;
                startFlight(p, level, 1_200_000);
                stopAt = endWith;
                next(Step.REFLY_WITH);
            }
            case REFLY_WITH -> {
                if (!fly(p, level)) return;
                report.add(flightLine("over explored land, with look-ahead"));
                write();
                next(Step.DONE);
            }
            default -> {}
        }
    }

    private static void next(Step s) {
        step = s;
        ticks = 0;
    }

    private static void startFlight(ServerPlayer p, ServerLevel level, double from) {
        x = from;
        startX = from;
        flownTicks = 0;
        stalls = 0;
        leadSum = 0;
        leadSamples = 0;
        lastTickNanos = 0;
        tickMsSum = 0;
        tickMsMax = 0;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(level, x, ALTITUDE, 8, -90f, 0f);
    }

    /** One tick of flight east at top speed. Returns true when the flight is over. */
    private static boolean fly(ServerPlayer p, ServerLevel level) {
        long now = System.nanoTime();
        if (lastTickNanos != 0) {
            double ms = (now - lastTickNanos) / 1e6;
            tickMsSum += ms;
            tickMsMax = Math.max(tickMsMax, ms);
        }
        lastTickNanos = now;
        double next = x + FlightSteering.TOP_SPEED;
        if (level.getChunkSource().getChunkNow(((int) Math.floor(next)) >> 4, 0) != null) {
            x = next;
            p.teleportTo(level, x, ALTITUDE, 8, -90f, 0f);
        } else {
            stalls++; // a real client waits at the edge of what it has
        }
        flownTicks++;
        int cx = ((int) Math.floor(x)) >> 4;
        int ahead = 0;
        while (ahead < 64 && level.getChunkSource().getChunkNow(cx + ahead + 1, 0) != null) ahead++;
        leadSum += ahead;
        leadSamples++;
        return stopAt > 0 ? x >= stopAt || ticks >= FLIGHT_TICKS * 3 : ticks >= FLIGHT_TICKS;
    }

    private static String flightLine(String label) {
        double made = (x - startX) / flownTicks * 20;
        return String.format("flight %s (%.0f blocks), aiming for %.0f blocks/s: made %.0f blocks/s, stalled %d of %d ticks (%.0f%%); "
                        + "loaded world %.1f chunks ahead on average; server %.0f ms/tick average, %.0f worst",
                label, x - startX, FlightSteering.TOP_SPEED * 20, made, stalls, flownTicks, 100.0 * stalls / flownTicks,
                leadSum / leadSamples, tickMsSum / Math.max(1, leadSamples - 1), tickMsMax);
    }

    private static void write() {
        File out = new File("bench-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date()) + ".txt");
        try {
            Files.write(out.toPath(), report);
        } catch (IOException e) {
            JTTWEngine.LOGGER.error("ChunkBench: could not write the report", e);
        }
        report.forEach(line -> JTTWEngine.LOGGER.info("ChunkBench: {}", line));
    }
}
