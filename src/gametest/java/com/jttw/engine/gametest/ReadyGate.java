package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Holds every test until the chunks under its platform can hold entities
 * ({@link GT#entityReady}), then runs it: most tests run at once, a test placed on freshly
 * generated chunks waits a few ticks.
 *
 * <p>The GameTest runner starts a test in the tick its platform is placed, and there is no
 * per-test hook in between, so this swaps each registered test's body for a gated one when the
 * test server starts, before the first test runs. The runner's batches hold the same
 * {@link TestFunction} objects, so they run the gated bodies. A test whose platform never
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
        Field function;
        try {
            function = TestFunction.class.getDeclaredField("function");
            function.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot gate GameTests: TestFunction.function not found", e);
        }
        for (TestFunction test : GameTestRegistry.getAllTestFunctions()) {
            try {
                @SuppressWarnings("unchecked")
                Consumer<GameTestHelper> body = (Consumer<GameTestHelper>) function.get(test);
                if (body instanceof Gated) continue;
                function.set(test, new Gated(body));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot gate GameTest " + test.getTestName(), e);
            }
        }
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
