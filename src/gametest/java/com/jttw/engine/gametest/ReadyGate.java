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
 * becomes ready within {@value #MAX_WAIT_TICKS} ticks fails, saying so.
 *
 * <p>A held test's body runs once, however it is reached. The wait is a chain of one-tick
 * callbacks, added while the runner walks the test's table of callbacks; such a walk can come to
 * an entry again, and before this guard a held test's body could run two or three times (its
 * scene set up twice, its end declared twice: "This test already has final clause").
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ReadyGate {

    private ReadyGate() {}

    static final int MAX_WAIT_TICKS = 60;

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
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot gate GameTests in the test server's batches", e);
            }
        }
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
            h.runAfterDelay(1, () -> runWhenReady(h, body, waited + 1, done));
        }
    }
}
