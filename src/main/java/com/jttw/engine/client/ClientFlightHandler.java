package com.jttw.engine.client;

import com.jttw.engine.flight.FlightProfile;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Moves the local player in flight and in water-creature swimming. The client is what moves the
 * player, so it steers here, with the server's {@link FlightSteering} and the
 * {@link FlightProfile} the server sends (speed, glide-only, boost).
 */
public final class ClientFlightHandler {

    private ClientFlightHandler() {}

    private static FlightProfile profile = FlightProfile.DEFAULT;

    private static Vec3 flightVector = Vec3.ZERO;
    private static Vec3 landingMomentum = Vec3.ZERO;
    private static boolean wasFlying = false;
    private static Vec3 swimVector = Vec3.ZERO;
    /** Where the player was last tick: a jump further than any flight is a teleport. */
    private static Vec3 lastPos;
    /** Further than this in a tick, faster than anyone flies, and it was a teleport. */
    private static final double TELEPORT_JUMP = FlightSteering.TOP_SPEED * 1.5 + 2;

    public static void setProfile(FlightProfile newProfile) {
        profile = newProfile;
    }

    public static FlightProfile profile() {
        return profile;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Vec3 pos = p.position();
        if (p.isCreative() || p.isSpectator()) {
            // Vanilla flies these; none of this mod's momentum outlives them
            flightVector = Vec3.ZERO;
            landingMomentum = Vec3.ZERO;
            swimVector = Vec3.ZERO;
            wasFlying = false;
            lastPos = pos;
            return;
        }

        // A teleport ends all momentum: it is not carried from where you were to where you are
        if (lastPos != null && pos.distanceTo(lastPos) > TELEPORT_JUMP) {
            flightVector = Vec3.ZERO;
            landingMomentum = Vec3.ZERO;
            swimVector = Vec3.ZERO;
            wasFlying = false;
        }
        lastPos = pos;

        // A water creature in water swims the way others fly
        TransformationData data = TransformationManager.getTransformation(p);
        if (p.isInWaterOrBubble() && data != null && JTTWFlight.isAquatic(data)) {
            swimVector = FlightSteering.swim(swimVector, p.zza, p.xxa, p.getLookAngle(), p.isSprinting());
            p.setDeltaMovement(swimVector);
            flightVector = Vec3.ZERO;
            wasFlying = false;
            landingMomentum = Vec3.ZERO;
            return;
        }
        swimVector = Vec3.ZERO;

        if (!p.getAbilities().flying) {
            if (wasFlying) {
                landingMomentum = flightVector;
                flightVector = Vec3.ZERO;
                wasFlying = false;
            }
            // Coast out of flight while airborne; on the ground the coasting is over
            if (!p.onGround() && landingMomentum.lengthSqr() > FlightSteering.NO_INPUT_CUTOFF_SQR) {
                p.setDeltaMovement(landingMomentum);
                landingMomentum = landingMomentum.scale(FlightSteering.LANDING_DECAY);
            } else {
                landingMomentum = Vec3.ZERO;
            }
            return;
        }

        wasFlying = true;
        double forward = p.zza;
        if (profile.thrust() && forward <= 0.01) forward = 1.0; // beating wings carry him forward
        flightVector = profile.glide()
                ? FlightSteering.glide(flightVector, forward, p.xxa, p.getLookAngle())
                : FlightSteering.fly(flightVector, forward, p.xxa, p.getLookAngle(), profile.effectiveSpeed());
        p.setDeltaMovement(flightVector);
    }
}
