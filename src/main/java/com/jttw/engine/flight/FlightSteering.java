package com.jttw.engine.flight;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The flight, glide and swim steering, shared by the server ({@link JTTWFlight}) and the client
 * that actually moves the player, so both fly the same way at the same speed. Pure: each call
 * takes this tick's flight vector and input and returns the next one.
 *
 * <p>The flier eases toward where they look (Palladium-style turn limiting), coasts to a stop
 * with no input, and is capped in proportion to their speed, so a faster tier is never clipped
 * to a slower one's limit.
 */
public final class FlightSteering {

    private FlightSteering() {}

    // ── speeds (blocks per tick) ───────────────────────────────────
    /** Tier 2, full flight. */
    public static final double CRUISE_SPEED = 1.55;
    /** Tier 3, the fastest. */
    public static final double FAST_SPEED = 2.40;
    /** Wukong outflies everyone at each tier. */
    public static final double WUKONG_CRUISE_SPEED = 1.85;
    public static final double WUKONG_FAST_SPEED = 2.80;
    /**
     * The fastest anyone flies: the Peng at the height of his wingbeats, Wukong at full
     * somersault momentum. 112 blocks a second.
     */
    public static final double TOP_SPEED = 5.58;
    /**
     * A Heavenly's cloud (Heavenly Mobility): a third of Wukong's cloud at its height, 37 blocks
     * a second, and never building up.
     */
    public static final double HEAVENLY_CLOUD_SPEED = TOP_SPEED / 3;
    /** Ticks a flier holding top speed takes to build all the way to {@link #TOP_SPEED}: 10 seconds. */
    public static final int BUILD_UP_TICKS = 200;
    /** A flying form's own wings, without a flight tier. */
    public static final double FORM_SPEED = 1.10;
    public static final double GLIDE_SPEED = 1.20;
    public static final double GLIDE_SINK_RATE = -0.15;
    public static final double SWIM_SPEED = 0.65;
    public static final double SWIM_SPRINT_SPEED = 2.20;

    // ── handling ───────────────────────────────────────────────────
    /** How far the flight vector may change in a tick at cruising speed; scales with speed. */
    public static final double FLEX_PER_TICK = 0.12;
    public static final double SWIM_SPRINT_FLEX = 0.25;
    public static final double STRAFE_SCALE = 0.85;
    /** Caps as a multiple of speed: 2.25 and 1.75 blocks/tick at cruising speed. */
    public static final double XZ_CAP_PER_SPEED = 2.25 / CRUISE_SPEED;
    public static final double Y_CAP_PER_SPEED = 1.75 / CRUISE_SPEED;
    public static final double NO_INPUT_DECAY = 0.92;
    public static final double NO_INPUT_CUTOFF_SQR = 0.001;
    /** The momentum carried after flight ends decays like this each tick. */
    public static final double LANDING_DECAY = 0.9;

    /** Flight speed for a flight tier (0 = a flying form with no tier of its own). */
    public static double speedFor(int tier, boolean wukong) {
        return switch (tier) {
            case 0 -> FORM_SPEED;
            case 3 -> wukong ? WUKONG_FAST_SPEED : FAST_SPEED;
            default -> wukong ? WUKONG_CRUISE_SPEED : CRUISE_SPEED;
        };
    }

    private static boolean hasInput(double forward, double strafe) {
        return Math.abs(forward) > 0.01 || Math.abs(strafe) > 0.01;
    }

    private static Vec3 decay(Vec3 v) {
        Vec3 d = v.scale(NO_INPUT_DECAY);
        return d.lengthSqr() < NO_INPUT_CUTOFF_SQR ? Vec3.ZERO : d;
    }

    private static Vec3 flatRight(Vec3 look) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-9) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        return new Vec3(flat.z, 0, -flat.x);
    }

    private static Vec3 stepToward(Vec3 current, Vec3 desired, double flex) {
        Vec3 delta = desired.subtract(current);
        double len = delta.length();
        if (len > flex && len > 1.0e-9) delta = delta.scale(flex / len);
        return current.add(delta);
    }

    private static Vec3 cap(Vec3 v, double maxXZ, double maxY) {
        double xz = Math.sqrt(v.x * v.x + v.z * v.z);
        if (xz > maxXZ && xz > 1.0e-9) {
            double s = maxXZ / xz;
            v = new Vec3(v.x * s, v.y, v.z * s);
        }
        return new Vec3(v.x, Mth.clamp(v.y, -maxY, maxY), v.z);
    }

    /** One tick of full flight toward where the flier looks, at {@code speed}. */
    public static Vec3 fly(Vec3 current, double forward, double strafe, Vec3 look, double speed) {
        if (!hasInput(forward, strafe)) return decay(current);
        look = look.normalize();
        Vec3 desired = look.scale(forward * speed).add(flatRight(look).scale(strafe * speed * STRAFE_SCALE));
        Vec3 next = stepToward(current, desired, FLEX_PER_TICK * speed / CRUISE_SPEED);
        return cap(next, speed * XZ_CAP_PER_SPEED, speed * Y_CAP_PER_SPEED);
    }

    /**
     * One tick of elytra-style gliding: steer across, sink always; looking down dives faster,
     * looking up sinks slower, never climbs.
     */
    public static Vec3 glide(Vec3 current, double forward, double strafe, Vec3 look) {
        if (!hasInput(forward, strafe)) {
            Vec3 d = new Vec3(current.x * NO_INPUT_DECAY, GLIDE_SINK_RATE, current.z * NO_INPUT_DECAY);
            return d.horizontalDistanceSqr() < NO_INPUT_CUTOFF_SQR ? new Vec3(0, GLIDE_SINK_RATE, 0) : d;
        }
        look = look.normalize();
        Vec3 right = flatRight(look);
        Vec3 flatLook = new Vec3(-right.z, 0, right.x);
        Vec3 desired = flatLook.scale(forward * GLIDE_SPEED).add(right.scale(strafe * GLIDE_SPEED * STRAFE_SCALE));
        double y = Math.min(0, look.y * GLIDE_SPEED * 0.5 + GLIDE_SINK_RATE);
        Vec3 next = stepToward(current, new Vec3(desired.x, y, desired.z), FLEX_PER_TICK);
        next = cap(next, GLIDE_SPEED * XZ_CAP_PER_SPEED, Double.MAX_VALUE);
        return new Vec3(next.x, Math.min(0, next.y), next.z);
    }

    /** One tick of a water creature's swimming: like flight, only in water; sprinting darts. */
    public static Vec3 swim(Vec3 current, double forward, double strafe, Vec3 look, boolean sprinting) {
        if (!hasInput(forward, strafe)) return decay(current);
        double speed = sprinting ? SWIM_SPRINT_SPEED : SWIM_SPEED;
        look = look.normalize();
        Vec3 desired = look.scale(forward * speed).add(flatRight(look).scale(strafe * speed * STRAFE_SCALE));
        Vec3 next = stepToward(current, desired, sprinting ? SWIM_SPRINT_FLEX : FLEX_PER_TICK);
        return cap(next, speed * XZ_CAP_PER_SPEED, speed * Y_CAP_PER_SPEED);
    }
}
