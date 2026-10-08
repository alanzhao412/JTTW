package com.jttw.engine.flight;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The one flight steering both the server and the client fly by. */
class FlightSteeringTest {

    private static final Vec3 SOUTH = new Vec3(0, 0, 1);

    /** Holds forward for {@code ticks}, looking south, and returns the speed reached. */
    private static double cruise(double speed, int ticks) {
        Vec3 v = Vec3.ZERO;
        for (int i = 0; i < ticks; i++) v = FlightSteering.fly(v, 1, 0, SOUTH, speed);
        return v.length();
    }

    @Test
    void eachTierFliesAtItsOwnSpeed() {
        assertEquals(FlightSteering.FORM_SPEED, FlightSteering.speedFor(0, false));
        assertEquals(FlightSteering.CRUISE_SPEED, FlightSteering.speedFor(1, false));
        assertEquals(FlightSteering.CRUISE_SPEED, FlightSteering.speedFor(2, false));
        assertEquals(FlightSteering.FAST_SPEED, FlightSteering.speedFor(3, false));
        assertTrue(FlightSteering.speedFor(2, true) > FlightSteering.speedFor(2, false), "Wukong outflies at II");
        assertTrue(FlightSteering.speedFor(3, true) > FlightSteering.speedFor(3, false), "and at III");
    }

    @Test
    void theTopTiersAreNotClippedToTheCruisingCap() {
        // the old cap (2.25 blocks/tick) held tier III and Wukong below their speeds
        assertEquals(FlightSteering.FAST_SPEED, cruise(FlightSteering.FAST_SPEED, 200), 1e-6);
        assertEquals(FlightSteering.WUKONG_FAST_SPEED, cruise(FlightSteering.WUKONG_FAST_SPEED, 200), 1e-6);
        assertEquals(FlightSteering.CRUISE_SPEED, cruise(FlightSteering.CRUISE_SPEED, 200), 1e-6);
    }

    @Test
    void aFasterFlierReachesTopSpeedJustAsSoon() {
        double slow = cruise(FlightSteering.CRUISE_SPEED, 6) / FlightSteering.CRUISE_SPEED;
        double fast = cruise(FlightSteering.WUKONG_FAST_SPEED, 6) / FlightSteering.WUKONG_FAST_SPEED;
        assertEquals(slow, fast, 1e-9);
    }

    @Test
    void withNoInputAFlierCoastsToAStop() {
        Vec3 v = new Vec3(0, 0, 2);
        for (int i = 0; i < 200; i++) v = FlightSteering.fly(v, 0, 0, SOUTH, FlightSteering.CRUISE_SPEED);
        assertEquals(Vec3.ZERO, v);
    }

    @Test
    void aGliderNeverClimbs() {
        Vec3 up = new Vec3(0, 1, 1).normalize();
        Vec3 v = new Vec3(0, 0.5, 1); // even thrown upward
        for (int i = 0; i < 100; i++) {
            v = FlightSteering.glide(v, 1, 0, up);
            assertTrue(v.y <= 0, "climbing at tick " + i + ": " + v);
        }
        Vec3 idle = FlightSteering.glide(new Vec3(0, 0, 1), 0, 0, SOUTH);
        assertEquals(FlightSteering.GLIDE_SINK_RATE, idle.y, 1e-9, "with no input it still sinks");
    }

    @Test
    void aGliderDivesFasterLookingDown() {
        Vec3 down = new Vec3(0, -1, 1).normalize();
        Vec3 level = Vec3.ZERO, diving = Vec3.ZERO;
        for (int i = 0; i < 40; i++) {
            level = FlightSteering.glide(level, 1, 0, SOUTH);
            diving = FlightSteering.glide(diving, 1, 0, down);
        }
        assertTrue(diving.y < level.y, diving + " vs " + level);
    }

    @Test
    void sprintingSwimmersDart() {
        Vec3 swim = Vec3.ZERO, dart = Vec3.ZERO;
        for (int i = 0; i < 60; i++) {
            swim = FlightSteering.swim(swim, 1, 0, SOUTH, false);
            dart = FlightSteering.swim(dart, 1, 0, SOUTH, true);
        }
        assertEquals(FlightSteering.SWIM_SPEED, swim.length(), 1e-6);
        assertEquals(FlightSteering.SWIM_SPRINT_SPEED, dart.length(), 1e-6);
    }

    @Test
    void theBoostMultipliesTheSpeedAndRoundsToTheHundredth() {
        FlightProfile p = new FlightProfile(FlightSteering.CRUISE_SPEED, false, 2.004, true);
        assertEquals(2.0, p.boost());
        assertEquals(FlightSteering.CRUISE_SPEED * 2, p.effectiveSpeed(), 1e-9);
        assertEquals(1.0, new FlightProfile(1.55, false, 0.4, false).boost(), "never below 1");
    }
}
