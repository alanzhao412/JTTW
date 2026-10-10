package com.jttw.engine.flight;

import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Free flight: pitch has no end, so pulling up carries a flier over the top and round a loop. */
class FlightAttitudeTest {

    private static final float EPS = 0.05f;

    private static float wrap(float d) {
        d %= 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    private static void assertAngles(float yaw, float pitch, float roll, Quaternionf q, String what) {
        float[] a = FlightAttitude.angles(q);
        assertEquals(0, wrap(a[0] - yaw), EPS, what + ": yaw " + a[0]);
        assertEquals(pitch, a[1], EPS, what + ": pitch " + a[1]);
        assertEquals(0, wrap(a[2] - roll), EPS, what + ": roll " + a[2]);
    }

    private static Quaternionf pullUp(Quaternionf q, float degrees) {
        // in small steps, as a mouse moves
        for (int i = 0; i < degrees; i++) FlightAttitude.turn(q, 0, -1);
        return q;
    }

    @Test
    void anAttitudeReadsBackAsTheYawAndPitchItWasMadeFrom() {
        for (float yaw : new float[]{0, 37, 90, -120, 179}) {
            for (float pitch : new float[]{-80, -30, 0, 45, 85}) {
                assertAngles(yaw, pitch, 0, FlightAttitude.of(yaw, pitch), yaw + "/" + pitch);
            }
        }
    }

    @Test
    void pullingUpPastTheVerticalGoesOverOntoTheBack() {
        Quaternionf q = pullUp(FlightAttitude.of(30, 0), 120);
        // 120 up from level: heading back the way they came, 60 above the horizon, upside down
        assertAngles(210, -60, 180, q, "over the top");
    }

    @Test
    void aFullPullIsALoopBackToLevelFlight() {
        Quaternionf q = pullUp(FlightAttitude.of(30, 0), 360);
        assertAngles(30, 0, 0, q, "round the loop");
    }

    @Test
    void halfwayRoundTheLoopTheyFlyBackUpsideDown() {
        assertAngles(180, 0, 180, pullUp(FlightAttitude.of(0, 0), 180), "the top of the loop");
    }

    @Test
    void turningAsideKeepsTheHorizonLevel() {
        Quaternionf q = FlightAttitude.of(0, 20);
        FlightAttitude.turn(q, 90, 0);
        assertAngles(90, 20, 0, q, "turned right");
    }
}
