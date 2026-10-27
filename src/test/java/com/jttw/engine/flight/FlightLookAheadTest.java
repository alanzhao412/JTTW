package com.jttw.engine.flight;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The corridor of chunks loaded ahead of a fast flier. */
class FlightLookAheadTest {

    private static final Vec3 ORIGIN = new Vec3(8, 100, 8);

    @Test
    void slowFliersAndTeleportsGetNoCorridor() {
        assertTrue(FlightLookAhead.corridor(ORIGIN, new Vec3(0.9, 0, 0)).isEmpty(), "the server keeps up");
        assertTrue(FlightLookAhead.corridor(ORIGIN, new Vec3(FlightLookAhead.MAX_SPEED + 1, 0, 0)).isEmpty(), "a teleport");
        assertFalse(FlightLookAhead.corridor(ORIGIN, new Vec3(FlightSteering.TOP_SPEED, 0, 0)).isEmpty(), "the top speed is flight, not a teleport");
        assertFalse(FlightLookAhead.corridor(ORIGIN, new Vec3(FlightSteering.VOID_TOP_SPEED, 0, 0)).isEmpty(), "nor the Great Void's");
        assertTrue(FlightLookAhead.corridor(ORIGIN, new Vec3(0, -3, 0)).isEmpty(), "falling straight down");
    }

    @Test
    void theCorridorRunsAheadThreeChunksWideNearestFirst() {
        List<ChunkPos> c = FlightLookAhead.corridor(ORIGIN, new Vec3(FlightSteering.TOP_SPEED, 0, 0));
        assertEquals(new ChunkPos(0, 0), c.get(1), "starts where he is");
        double lead = Math.min(FlightLookAhead.MAX_LEAD_BLOCKS, FlightSteering.TOP_SPEED * FlightLookAhead.LEAD_TICKS);
        int farthest = c.stream().mapToInt(p -> p.x).max().orElseThrow();
        assertEquals((int) ((8 + lead) / 16), farthest, "reaches " + FlightLookAhead.LEAD_TICKS + " ticks ahead");
        assertTrue(c.stream().allMatch(p -> Math.abs(p.z) <= 1), "three chunks wide");
        for (int i = 1; i < c.size(); i++) assertTrue(c.get(i).x >= c.get(i - 1).x, "nearest first");
        assertEquals(3 * (farthest + 1), c.size(), "each chunk once");
    }

    @Test
    void theCorridorFollowsTheHeadingAndIsCapped() {
        List<ChunkPos> diagonal = FlightLookAhead.corridor(ORIGIN, new Vec3(-4, 0, -4));
        assertTrue(diagonal.stream().allMatch(p -> p.x <= 1 && p.z <= 1), "north-west only");
        List<ChunkPos> fast = FlightLookAhead.corridor(ORIGIN, new Vec3(14.9, 0, 0));
        int farthest = fast.stream().mapToInt(p -> p.x).max().orElseThrow();
        assertTrue(farthest * 16 <= 8 + FlightLookAhead.MAX_LEAD_BLOCKS, "never past the cap: " + farthest);
    }
}
