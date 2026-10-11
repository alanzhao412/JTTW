package com.jttw.engine.flight;

/**
 * How a player flies right now, as the server decides it and the client needs it to move them:
 * their flight speed, whether they can only glide, a boost on top (the Peng's wingbeats), and
 * whether they are being driven forward with no key held.
 */
public record FlightProfile(double speed, boolean glide, double boost, boolean thrust, boolean own) {

    /** A flier with no tier of their own until the server says otherwise. */
    public static final FlightProfile DEFAULT = new FlightProfile(FlightSteering.CRUISE_SPEED, false, 1.0, false, false);

    public FlightProfile {
        boost = Math.max(1.0, boost);
    }

    /** As sent to the client: the boost to the hundredth, so a slow ramp is not re-sent every tick. */
    public FlightProfile forSending() {
        return new FlightProfile(speed, glide, Math.round(boost * 100) / 100.0, thrust, own);
    }

    /** Speed with the boost. */
    public double effectiveSpeed() {
        return speed * boost;
    }
}
