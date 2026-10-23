package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestServer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Holds every test until the chunks under its platform can hold entities
 * ({@link GT#entityReady}), then runs it: most tests run at once, a test placed on freshly
 * generated chunks waits a few ticks.
 *
 * <p>The GameTest runner starts a test in the tick its platform is placed, and there is no
 * per-test hook in between, so this swaps each registered test's body for a gated one when the
 * test server starts, before the first test runs; the server's already-built batches are
 * pointed at the gated copies too, so they run the gated bodies. A test whose platform never
 * becomes ready within {@value #MAX_WAIT_TICKS} ticks fails, saying so; the ticks it waits are
 * added to its timeout.
 *
 * <p>A held test's body runs once, however it is reached. The wait is a chain of one-tick
 * callbacks, added while the runner walks the test's table of callbacks; such a walk can come to
 * an entry again, and before this guard a held test's body could run two or three times (its
 * scene set up twice, its end declared twice: "This test already has final clause").
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ReadyGate {

    private ReadyGate() {}

    /**
     * Long enough for the zones' heavier terrain to generate under a busy test server: full runs
     * have held a test over 160 ticks, and once the 200 this was ran out. The wait is not the
     * test's own time (it is added to its timeout), so a generous bound costs nothing.
     */
    static final int MAX_WAIT_TICKS = 600;

    /**
     * Keeps the chunks under and round a test's platform ticking entities for ten minutes, two
     * chunks out: since 1.21 tests stand far from spawn, where only the chunks of the platform
     * structure's own footprint are forced, the platform reaches a block past it, and a boss that
     * vanishes or flees lands well off it (a creature in a chunk that doesn't tick stands frozen).
     */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> PLATFORM =
            net.minecraft.server.level.TicketType.create("jttw_gametest_platform",
                    java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 20 * 60 * 10);

    private static void holdPlatform(GameTestHelper h) {
        for (int x = 0; x <= 5; x += 5) for (int z = 0; z <= 5; z += 5) {
            var chunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(new net.minecraft.core.BlockPos(x, 1, z)));
            // distance 4: ticket level 29, so entities tick (level 31 and below) two chunks round
            h.getLevel().getChunkSource().addRegionTicket(PLATFORM, chunk, 4, chunk, true);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // TestFunction is a record, so its body cannot be swapped in place: each test is replaced
        // by a gated copy, in the registry (tests started later by command) and in the batches
        // the test server has already built from it (their GameTestInfos hold the old copy).
        Map<TestFunction, TestFunction> gated = new IdentityHashMap<>();
        List<TestFunction> all = new ArrayList<>(GameTestRegistry.getAllTestFunctions());
        GameTestRegistry.getAllTestFunctions().clear();
        for (TestFunction test : all) {
            GameTestRegistry.getAllTestFunctions().add(gated.computeIfAbsent(test, ReadyGate::gate));
        }
        if (event.getServer() instanceof GameTestServer server) {
            try {
                Field batchesField = GameTestServer.class.getDeclaredField("testBatches");
                batchesField.setAccessible(true);
                Field infoFunction = GameTestInfo.class.getDeclaredField("testFunction");
                infoFunction.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<GameTestBatch> batches = (List<GameTestBatch>) batchesField.get(server);
                for (GameTestBatch batch : batches) {
                    for (GameTestInfo info : batch.gameTestInfos()) {
                        TestFunction test = info.getTestFunction();
                        infoFunction.set(info, gated.computeIfAbsent(test, ReadyGate::gate));
                    }
                }
                String only = System.getProperty("jttw.gametest.only", "");
                if (!only.isBlank()) {
                    batchesField.set(server, only(batches, only, Integer.getInteger("jttw.gametest.repeat", 1), server.overworld()));
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot gate GameTests in the test server's batches", e);
            }
        }
    }

    /**
     * Just the batches holding a test whose name holds one of {@code only}'s comma-separated words
     * (any case), each run {@code repeat} times over, its other tests with it as its neighbours in
     * a full run: for chasing a test that fails one run in many ({@code ./gradlew
     * runGameTestServer -Pgametest.only=guardianlaser,yama -Pgametest.repeat=20}).
     */
    private static List<GameTestBatch> only(List<GameTestBatch> batches, String only, int repeat,
                                            net.minecraft.server.level.ServerLevel level) {
        List<String> words = java.util.Arrays.stream(only.toLowerCase(java.util.Locale.ROOT).split(","))
                .map(String::trim).filter(w -> !w.isEmpty()).toList();
        List<GameTestBatch> kept = new ArrayList<>();
        for (GameTestBatch batch : batches) {
            if (batch.gameTestInfos().stream().noneMatch(info -> words.stream()
                    .anyMatch(info.getTestName().toLowerCase(java.util.Locale.ROOT)::contains))) continue;
            for (int i = 0; i < repeat; i++) {
                List<GameTestInfo> copies = batch.gameTestInfos().stream()
                        .map(info -> new GameTestInfo(info.getTestFunction(), info.getRotation(), level,
                                net.minecraft.gametest.framework.RetryOptions.noRetries()))
                        .toList();
                kept.add(new GameTestBatch(batch.name() + "/" + i, copies, batch.beforeBatchFunction(), batch.afterBatchFunction()));
            }
        }
        JTTWEngine.LOGGER.info("ReadyGate: running only {} ({} tests in {} batches)", words,
                kept.stream().mapToInt(b -> b.gameTestInfos().size()).sum(), kept.size());
        return kept;
    }

    /** {@code test} with its body gated (unchanged if it already is). */
    private static TestFunction gate(TestFunction t) {
        if (t.function() instanceof Gated) return t;
        return new TestFunction(t.batchName(), t.testName(), t.structureName(), t.rotation(), t.maxTicks(),
                t.setupTicks(), t.required(), t.manualOnly(), t.maxAttempts(), t.requiredSuccesses(),
                t.skyAccess(), new Gated(t.function()));
    }

    /** A test body that waits for its platform. */
    private record Gated(Consumer<GameTestHelper> body) implements Consumer<GameTestHelper> {
        @Override
        public void accept(GameTestHelper h) {
            holdPlatform(h);
            runWhenReady(h, body, 0, new java.util.concurrent.atomic.AtomicBoolean());
        }
    }

    private static void runWhenReady(GameTestHelper h, Consumer<GameTestHelper> body, int waited,
                                     java.util.concurrent.atomic.AtomicBoolean done) {
        if (done.get()) return; // reached again: it has run, or failed, already
        if (GT.entityReady(h)) {
            done.set(true);
            if (waited > 0) JTTWEngine.LOGGER.info("ReadyGate: held a test {} ticks for its platform", waited);
            body.accept(h);
        } else if (waited >= MAX_WAIT_TICKS) {
            done.set(true);
            GT.requireEntityReady(h); // fails, naming the chunks
        } else {
            extendTimeout(h);
            h.runAfterDelay(1, () -> runWhenReady(h, body, waited + 1, done));
        }
    }

    /**
     * Gives a held test one more tick before it times out: the wait for its platform is not the
     * test's own time (a test given 60 ticks, held 40 for fresh terrain, would otherwise have 20).
     */
    private static void extendTimeout(GameTestHelper h) {
        try {
            Field infoField = GameTestHelper.class.getDeclaredField("testInfo");
            infoField.setAccessible(true);
            Field timeout = GameTestInfo.class.getDeclaredField("timeoutTicks");
            timeout.setAccessible(true);
            Object info = infoField.get(h);
            timeout.setInt(info, timeout.getInt(info) + 1);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot extend a held test's timeout", e);
        }
    }
}
