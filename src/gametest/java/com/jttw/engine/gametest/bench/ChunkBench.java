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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

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
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ChunkBench {

    private ChunkBench() {}

    private static final boolean ENABLED = Boolean.getBoolean("jttw.chunkBench");
    /** "terrain" runs {@link TerrainBench} instead of the generation and flight measurements. */
    private static final boolean TERRAIN = "terrain".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "zones" runs {@link ZoneBench}: where the mod's zones were placed, a map of them, and each as built. */
    private static final boolean ZONES = "zones".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "cave" runs {@link CaveBench}: the Bottomless Cave nearest spawn, as built. */
    private static final boolean CAVE = "cave".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "hell" runs {@link HellBench}: Hell as it really is, from the arrival to the Ten Courts. */
    private static final boolean HELL = "hell".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "waters" runs {@link WatersBench}: where the Mystical Waters turn up, its trench and its palace. */
    private static final boolean WATERS = "waters".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "wuzhuang" runs {@link WuzhuangBench}: how often Mount Wuzhuang turns up, and the nearest as built. */
    private static final boolean WUZHUANG = "wuzhuang".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "fire" runs {@link FireBench}: where the Flaming Mountains turn up, the range and the Fire Cloud Cave. */
    private static final boolean FIRE = "fire".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "lion" runs {@link LionBench}: where Lion-Camel Ridge turns up, the ridge and Lion-Camel City. */
    private static final boolean LION = "lion".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "kunlun" runs {@link KunlunBench}: where Mount Kunlun was placed, its peak, its stair walked, its summit. */
    private static final boolean KUNLUN = "kunlun".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "flowerfruit" runs {@link FlowerFruitBench}: where Flower-Fruit Mountain was placed, the island, its falls and cave, pictures. */
    private static final boolean FLOWER_FRUIT = "flowerfruit".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "void" runs the generation and flight measurements in the Great Void instead of the Overworld. */
    private static final boolean VOID = "void".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "village" runs {@link VillageBench}: the nearest Tang village of each kind, and the nearest as built. */
    private static final boolean VILLAGE = "village".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "ridge" runs {@link RidgeBench}: White Bone Ridge, what its heart costs to make, and pictures of it. */
    private static final boolean RIDGE = "ridge".equals(System.getProperty("jttw.chunkBench.mode", ""));
    /** "blackwind" runs {@link BlackWindBench}: Black Wind Mountain, its range, temple, cave and king, and pictures of it. */
    private static final boolean BLACKWIND = "blackwind".equals(System.getProperty("jttw.chunkBench.mode", ""));
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
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested && mc.level == null && mc.screen != null && mc.getOverlay() == null) {
            worldRequested = true;
            mc.options.onboardAccessibility = false;
            String open = System.getProperty("jttw.chunkBench.open", "");
            if (!open.isEmpty()) {
                // a world already saved (an older one, say), opened as a player would
                mc.createWorldOpenFlows().openWorld(open, () -> JTTWEngine.LOGGER.error("ChunkBench: could not open {}", open));
                return;
            }
            LevelSettings settings = new LevelSettings("jttw_bench_" + System.currentTimeMillis(), GameType.CREATIVE,
                    false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(Long.getLong("jttw.chunkBench.seed", 12345L), true, false),
                    access -> withSky(access, access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL)
                            .value().createWorldDimensions()), new net.minecraft.client.gui.screens.TitleScreen());
        }
        // opening a saved world, the game may ask first (its experimental settings, a backup): go on, as a player would
        if (worldRequested && mc.level == null && mc.screen != null && !System.getProperty("jttw.chunkBench.open", "").isEmpty()
                && !(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen)) {
            for (var child : mc.screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button b
                        && b.getMessage().getString().toLowerCase(java.util.Locale.ROOT).matches(".*(proceed|yes|continue|without backup|i know).*")) {
                    JTTWEngine.LOGGER.info("ChunkBench: {} asks; answering \"{}\"", mc.screen.getClass().getSimpleName(), b.getMessage().getString());
                    b.onPress();
                    break;
                }
            }
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

    /**
     * The default world's dimensions, its Overworld under vanilla's sky instead of the taller one if
     * {@code -Pbench.sky=vanilla} (to see what the taller sky costs, the same world otherwise).
     */
    private static net.minecraft.world.level.levelgen.WorldDimensions withSky(net.minecraft.core.RegistryAccess access,
                                                                                  net.minecraft.world.level.levelgen.WorldDimensions dims) {
        if (!"vanilla".equals(System.getProperty("jttw.chunkBench.sky", ""))) return dims;
        var stems = new java.util.LinkedHashMap<>(dims.dimensions());
        var overworld = stems.get(net.minecraft.world.level.dimension.LevelStem.OVERWORLD);
        stems.put(net.minecraft.world.level.dimension.LevelStem.OVERWORLD, new net.minecraft.world.level.dimension.LevelStem(
                access.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD),
                overworld.generator()));
        return new net.minecraft.world.level.levelgen.WorldDimensions(stems);
    }

    /** The world as made: its Overworld's dimension type and build height. */
    static String worldLine(ServerLevel level) {
        return String.format("world %s: its Overworld %s, build height %d to %d", level.getServer().getWorldData().getLevelName(),
                level.dimensionTypeRegistration().unwrapKey().map(k -> k.location().toString()).orElse("?"),
                level.getMinBuildHeight(), level.getMaxBuildHeight());
    }

    @SubscribeEvent
    public static void onItemJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (ENABLED && TERRAIN && !event.getLevel().isClientSide()) TerrainBench.onItemJoin(event);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED || step == null || step == Step.DONE) return;
        MinecraftServer server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = p.serverLevel();
        ticks++;
        if (HELL) {
            if (p.tickCount < 100) return;
            if (HellBench.tick(p)) {
                report.addAll(HellBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (CAVE) {
            if (p.tickCount < 100) return;
            if (CaveBench.tick(p)) {
                report.addAll(CaveBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (VILLAGE) {
            if (p.tickCount < 100) return;
            if (VillageBench.tick(p)) {
                report.addAll(VillageBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (RIDGE) {
            if (p.tickCount < 100) return;
            if (RidgeBench.tick(p)) {
                report.addAll(RidgeBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (FLOWER_FRUIT) {
            if (p.tickCount < 100) return;
            if (FlowerFruitBench.tick(p)) {
                report.addAll(FlowerFruitBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (KUNLUN) {
            if (p.tickCount < 100) return;
            if (KunlunBench.tick(p)) {
                report.addAll(KunlunBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (LION) {
            if (p.tickCount < 100) return;
            if (LionBench.tick(p)) {
                report.addAll(LionBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (BLACKWIND) {
            if (p.tickCount < 100) return;
            if (BlackWindBench.tick(p)) {
                report.addAll(BlackWindBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (FIRE) {
            if (p.tickCount < 100) return;
            if (FireBench.tick(p)) {
                report.addAll(FireBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (WUZHUANG) {
            if (p.tickCount < 100) return;
            if (WuzhuangBench.tick(p)) {
                report.addAll(WuzhuangBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (WATERS) {
            if (p.tickCount < 100) return;
            if (WatersBench.tick(p)) {
                report.addAll(WatersBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (ZONES) {
            if (p.tickCount < 100) return;
            if (ZoneBench.tick(p)) {
                report.addAll(ZoneBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        if (TERRAIN) {
            if (p.tickCount < 100) return;
            if (TerrainBench.tick(p)) {
                report.addAll(TerrainBench.report());
                write();
                next(Step.DONE);
            }
            return;
        }
        switch (step) {
            case WAIT -> {
                if (p.tickCount < 100) return;
                if (VOID && !com.jttw.engine.dimension.GreatVoid.isIn(p)) {
                    com.jttw.engine.dimension.GreatVoid.enter(p);
                    report.add("in the Great Void");
                    return;
                }
                int view = Integer.getInteger("jttw.chunkBench.view", 0); // e.g. -Pbench.view=6
                if (view > 0) server.getPlayerList().setViewDistance(view);
                report.add(worldLine(level));
                report.add("view distance: " + server.getPlayerList().getViewDistance()
                        + " chunks; cores: " + Runtime.getRuntime().availableProcessors());
                int base = 20000; // chunks: far from spawn, never touched
                heapBefore = usedHeap();
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
                report.add(String.format("generation: %d new chunks in %.1f s = %.1f chunks/s (build height %d to %d)", loaded, seconds, loaded / seconds,
                        level.getMinBuildHeight(), level.getMaxBuildHeight()));
                // what they hold: the heap they take while loaded (collected before and after), and their size saved
                long heapAfter = usedHeap();
                level.save(null, true, false);
                java.nio.file.Path region = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("region").resolve("r." + (20000 >> 5) + ".0.mca");
                long disk = 0;
                try {
                    disk = Files.size(region);
                } catch (IOException ignored) {
                    // not written yet: reported as 0
                }
                report.add(String.format("memory: %.0f KB of heap a chunk while loaded (%.1f MB for the %d); saved, %.1f KB a chunk on disk (%s)",
                        (heapAfter - heapBefore) / 1024.0 / loaded, (heapAfter - heapBefore) / 1048576.0, loaded, disk / 1024.0 / loaded, region.getFileName()));
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

    private static long heapBefore;

    /** The heap in use once what can be collected is. */
    private static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) System.gc();
        return rt.totalMemory() - rt.freeMemory();
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
        double next = x + FlightSteering.topSpeed(VOID);
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
                label, x - startX, FlightSteering.topSpeed(VOID) * 20, made, stalls, flownTicks, 100.0 * stalls / flownTicks,
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
