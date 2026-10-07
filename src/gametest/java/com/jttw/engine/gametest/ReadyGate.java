package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
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
            runWhenReady(h, body, 0);
        }
    }

    private static void runWhenReady(GameTestHelper h, Consumer<GameTestHelper> body, int waited) {
        if (GT.entityReady(h)) {
            if (waited > 0) JTTWEngine.LOGGER.info("ReadyGate: held a test {} ticks for its platform", waited);
            body.accept(h);
        } else if (waited >= MAX_WAIT_TICKS) {
            GT.requireEntityReady(h); // fails, naming the chunks
        } else {
            h.runAfterDelay(1, () -> runWhenReady(h, body, waited + 1));
        }
    }
}
