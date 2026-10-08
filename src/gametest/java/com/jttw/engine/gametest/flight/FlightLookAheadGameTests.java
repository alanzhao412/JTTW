package com.jttw.engine.gametest.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlightLookAhead;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** A fast flier's path is loaded before they reach it. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlightLookAheadGameTests {

    /**
     * Where the world can be made as fast as he flies, his path is ready before he gets there.
     * (In untouched land at top speed it cannot: ChunkBench measures that ceiling.) Nothing else
     * loads this chunk: the test's fake player is in no one's view.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 300, batch = "flightLookAhead") // alone: other tests' load is not the question
    public static void theWorldAheadOfAFastFlierIsLoadedBeforeHeGetsThere(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        p.getAbilities().flying = true;
        Vec3 start = h.absoluteVec(new Vec3(2.5, 2, 2.5));
        p.setPos(start.x, start.y, start.z);
        FlightLookAhead.tick(p);
        double speed = 1.6; // fast enough to look ahead (32 blocks a second), slow enough for the test server to keep up
        ChunkPos ahead = new ChunkPos(net.minecraft.core.BlockPos.containing(start.add(0, 0, 160)));
        check(h, h.getLevel().getChunkSource().getChunkNow(ahead.x, ahead.z) == null, "not loaded before");
        // He flies south, as his client would carry him, until he is in that chunk
        h.onEachTick(() -> {
            if (new ChunkPos(p.blockPosition()).equals(ahead)) return;
            p.setPos(p.getX(), p.getY(), p.getZ() + speed);
            FlightLookAhead.tick(p);
            if (new ChunkPos(p.blockPosition()).equals(ahead)
                    && h.getLevel().getChunkSource().getChunkNow(ahead.x, ahead.z) == null) {
                h.fail("he reached " + ahead + ", 160 blocks on, before it loaded");
            }
        });
        h.succeedWhen(() -> h.assertTrue(new ChunkPos(p.blockPosition()).equals(ahead), "still flying"));
    }

    @GameTest(template = PLATFORM)
    public static void someoneWalkingRequestsNothing(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        Vec3 start = h.absoluteVec(new Vec3(2.5, 2, 2.5));
        p.setPos(start.x, start.y, start.z);
        FlightLookAhead.tick(p);
        p.setPos(start.x, start.y, start.z + FlightSteering.TOP_SPEED); // fast, but not flying
        eq(h, 0, FlightLookAhead.tick(p), "on foot, nothing");
        h.succeed();
    }
}
