package com.jttw.engine.flight;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A flier's attitude as a quaternion, so turning has no ends: pitching on past straight up carries
 * the flier over onto their back and round, a loop. Turning left and right is about the world's
 * up, so the horizon stays put; up and down is about the flier's own right hand.
 *
 * <p>Minecraft's yaw and pitch (degrees; pitch positive looking down) are read back out of it, with
 * the roll left over, which tilts the camera.
 */
public final class FlightAttitude {

    private FlightAttitude() {}

    /** The attitude a yaw and pitch make, the right way up. */
    public static Quaternionf of(float yaw, float pitch) {
        return new Quaternionf().rotateY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(pitch));
    }

    /** Turns {@code q} in place by so many degrees of yaw (about the world's up) and pitch (about its own right). */
    public static Quaternionf turn(Quaternionf q, float yawDegrees, float pitchDegrees) {
        new Quaternionf().rotateY((float) Math.toRadians(-yawDegrees)).mul(q, q);
        return q.rotateX((float) Math.toRadians(pitchDegrees)).normalize();
    }

    /** The yaw, pitch and roll (degrees) of an attitude: yaw and pitch as Minecraft has them. */
    public static float[] angles(Quaternionf q) {
        Vector3f forward = q.transform(new Vector3f(0, 0, 1));
        Vector3f up = q.transform(new Vector3f(0, 1, 0));
        float pitch = (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -forward.y))));
        // straight up or down there is no heading ahead: take it from where the head points
        float yaw = Math.abs(forward.y) > 0.999f
                ? (float) Math.toDegrees(Math.atan2(up.x * Math.signum(forward.y), -up.z * Math.signum(forward.y)))
                : (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        Vector3f level = of(yaw, pitch).transform(new Vector3f(0, 1, 0));
        float roll = (float) Math.toDegrees(Math.atan2(new Vector3f(level).cross(up).dot(forward), level.dot(up)));
        return new float[]{yaw, pitch, roll};
    }
}
