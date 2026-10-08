package com.jttw.engine.transformation;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** How a boat-form player moves on water: floating at the surface and sailing like a boat. */
class BoatFormsTest {

    private static final double EPS = 1e-6;

    @Test
    void onLandNothingChanges() {
        Vec3 v = new Vec3(0.1, -0.08, 0.2);
        assertEquals(v, BoatForms.sail(v, false, 0, 1f, 0f, 0f));
    }

    @Test
    void sunkBelowTheSurfaceItRisesAtAnEvenPace() {
        Vec3 next = BoatForms.sail(new Vec3(0, -0.2, 0), true, 0.5, 0f, 0f, 0f);
        assertEquals(0.05, next.y, EPS, "starts rising");
        Vec3 later = BoatForms.sail(new Vec3(0, 0.14, 0), true, 0.5, 0f, 0f, 0f);
        assertEquals(0.15, later.y, EPS, "never faster than 0.15 a tick");
    }

    @Test
    void afloatItHoldsAtTheSurface() {
        assertEquals(0.0, BoatForms.sail(new Vec3(0, -0.1, 0), true, 0.1, 0f, 0f, 0f).y, EPS, "no sinking");
        assertEquals(0.0, BoatForms.sail(new Vec3(0, 0.1, 0), true, 0.1, 0f, 0f, 0f).y, EPS, "no bobbing out");
    }

    /** Facing south (yaw 0) and steering forward, it gathers speed toward +z, up to 0.4 a tick. */
    @Test
    void steeringForwardSailsTheWayItFacesUpToBoatSpeed() {
        Vec3 v = Vec3.ZERO;
        for (int i = 0; i < 60; i++) v = BoatForms.sail(v, true, 0.1, 1f, 0f, 0f);
        assertEquals(0.0, v.x, 1e-4);
        assertEquals(BoatForms.MAX_SPEED, v.z, 1e-3, "8 blocks a second");
        Vec3 first = BoatForms.sail(Vec3.ZERO, true, 0.1, 1f, 0f, 0f);
        assertTrue(first.z > 0 && first.z < BoatForms.MAX_SPEED, "builds up, not instant: " + first.z);
    }

    @Test
    void steeringFollowsTheFacing() {
        // yaw 90 faces west (-x); left (positive strafe) is then south (+z)
        Vec3 forward = BoatForms.sail(Vec3.ZERO, true, 0.1, 1f, 0f, 90f);
        assertTrue(forward.x < 0 && Math.abs(forward.z) < 1e-6, "forward goes west: " + forward);
        Vec3 left = BoatForms.sail(Vec3.ZERO, true, 0.1, 0f, 1f, 90f);
        assertTrue(left.z > 0 && Math.abs(left.x) < 1e-6, "left of west is south: " + left);
        Vec3 diagonal = BoatForms.sail(new Vec3(0, 0, 0), true, 0.1, 1f, 1f, 0f);
        for (int i = 0; i < 60; i++) diagonal = BoatForms.sail(diagonal, true, 0.1, 1f, 1f, 0f);
        assertEquals(BoatForms.MAX_SPEED, diagonal.horizontalDistance(), 1e-3, "diagonal is no faster");
    }

    @Test
    void leftAloneItDriftsToAStop() {
        Vec3 v = new Vec3(0, 0, 0.4);
        Vec3 next = BoatForms.sail(v, true, 0.1, 0f, 0f, 0f);
        assertTrue(next.z < 0.4 && next.z > 0.3, "drifts, not stops dead: " + next.z);
        for (int i = 0; i < 200; i++) v = BoatForms.sail(v, true, 0.1, 0f, 0f, 0f);
        assertEquals(0.0, v.z, 1e-3);
    }
}
