package com.jttw.engine.client;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

public final class ClientFlightHandler {

    private ClientFlightHandler() {}

    private static final double FLIGHT_SPEED = 1.55;
    private static final double FLIGHT_FLEX_PER_TICK = 0.12;
    private static final double FLIGHT_STRAFE_SCALE = 0.85;

    private static final double MAX_Y  = 1.75;
    private static final double MAX_XZ = 2.25;

    private static final double SWIM_SPEED = 0.65;          // normal swim — cruising
    private static final double SWIM_SPRINT_SPEED = 2.20;   // sprint swim — darting
    private static final double SWIM_SPRINT_FLEX = 0.25;     // faster acceleration when sprinting

    // Palladium-style decay
    private static final double NO_INPUT_DECAY = 0.92;
    private static final double NO_INPUT_CUTOFF_SQR = 0.001;

    private static final double LANDING_DECAY = 0.9;

    private static Vec3 flightVector = Vec3.ZERO;
    private static Vec3 landingMomentum = Vec3.ZERO;
    private static boolean wasFlying = false;

    private static Vec3 swimVector = Vec3.ZERO;

    /** Flight boost from the server (the Peng's wingbeats): 1 = cruising. */
    private static double boost = 1.0;
    /** While the wings beat, he flies forward with no key held. */
    private static boolean thrust = false;

    public static void setBoost(float value, boolean beating) {
        boost = Math.max(1.0, value);
        thrust = beating;
    }

    public static double boost() {
        return boost;
    }

    private static Vec3 clampSpeed(Vec3 v, double maxXZ, double maxY) {
        double xz = Math.sqrt(v.x * v.x + v.z * v.z);
        if (xz > maxXZ && xz > 1.0e-9) {
            double s = maxXZ / xz;
            v = new Vec3(v.x * s, v.y, v.z * s);
        }
        v = new Vec3(v.x, Mth.clamp(v.y, -maxY, maxY), v.z);
        return v;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (p.isCreative() || p.isSpectator()) return;

        // Check for aquatic swim-flight (takes priority when in water)
        if (isAquaticSwimming(p)) {
            tickAquaticSwim(p);
            // Clear flight state so it doesn't interfere
            flightVector = Vec3.ZERO;
            wasFlying = false;
            landingMomentum = Vec3.ZERO;
            return;
        } else {
            swimVector = Vec3.ZERO;
        }

        if (!p.getAbilities().flying) {
            if (wasFlying) {
                landingMomentum = flightVector;
                flightVector = Vec3.ZERO;
                wasFlying = false;
            }

            if (landingMomentum.lengthSqr() > NO_INPUT_CUTOFF_SQR) {
                p.setDeltaMovement(landingMomentum);
                landingMomentum = landingMomentum.scale(LANDING_DECAY);
            } else {
                landingMomentum = Vec3.ZERO;
            }
            return;
        }

        wasFlying = true;
        tickFlightSteering(p, FLIGHT_SPEED * boost, MAX_XZ * boost, MAX_Y * boost);
    }

    private static void tickFlightSteering(LocalPlayer p, double speed, double maxXZ, double maxY) {
        double forward = p.zza;
        double strafe  = p.xxa;
        if (thrust && forward <= 0.01) forward = 1.0; // beating wings carry him forward

        boolean hasMoveInput = (Math.abs(forward) > 0.01) || (Math.abs(strafe) > 0.01);

        if (!hasMoveInput) {
            flightVector = flightVector.scale(NO_INPUT_DECAY);
            if (flightVector.lengthSqr() < NO_INPUT_CUTOFF_SQR) {
                flightVector = Vec3.ZERO;
            }
            p.setDeltaMovement(flightVector);
            return;
        }

        Vec3 look = p.getLookAngle().normalize();
        Vec3 desired = look.scale(forward * speed);

        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() < 1.0e-9) lookFlat = new Vec3(0, 0, 1);
        lookFlat = lookFlat.normalize();

        Vec3 right = new Vec3(lookFlat.z, 0, -lookFlat.x).normalize();
        desired = desired.add(right.scale(strafe * speed * FLIGHT_STRAFE_SCALE));

        Vec3 delta = desired.subtract(flightVector);
        double deltaLen = delta.length();
        double flex = FLIGHT_FLEX_PER_TICK * boost; // a boosted flier also turns and speeds up harder
        if (deltaLen > flex && deltaLen > 1.0e-9) {
            delta = delta.scale(flex / deltaLen);
        }

        flightVector = flightVector.add(delta);
        flightVector = clampSpeed(flightVector, maxXZ, maxY);

        p.setDeltaMovement(flightVector);
    }

    // -----------------------------------------------------------------------
    // Aquatic swim-flight (same steering as flight, only in water)
    // -----------------------------------------------------------------------

    private static boolean isAquaticSwimming(LocalPlayer p) {
        if (!p.isInWaterOrBubble()) return false;
        TransformationData data = TransformationManager.getTransformation(p);
        if (data == null) return false;
        EntityType<?> type = data.getEntityType();
        if (type == null) return false;
        MobCategory cat = type.getCategory();
        return cat == MobCategory.WATER_CREATURE
                || cat == MobCategory.WATER_AMBIENT
                || type == EntityType.FROG
                || type == EntityType.AXOLOTL
                || type == EntityType.GUARDIAN
                || type == EntityType.ELDER_GUARDIAN;
    }

    private static void tickAquaticSwim(LocalPlayer p) {
        double forward = p.zza;
        double strafe  = p.xxa;

        boolean hasMoveInput = (Math.abs(forward) > 0.01) || (Math.abs(strafe) > 0.01);

        if (!hasMoveInput) {
            swimVector = swimVector.scale(NO_INPUT_DECAY);
            if (swimVector.lengthSqr() < NO_INPUT_CUTOFF_SQR) {
                swimVector = Vec3.ZERO;
            }
            p.setDeltaMovement(swimVector);
            return;
        }

        double speed = p.isSprinting() ? SWIM_SPRINT_SPEED : SWIM_SPEED;

        Vec3 look = p.getLookAngle().normalize();
        Vec3 desired = look.scale(forward * speed);

        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() < 1.0e-9) lookFlat = new Vec3(0, 0, 1);
        lookFlat = lookFlat.normalize();

        Vec3 right = new Vec3(lookFlat.z, 0, -lookFlat.x).normalize();
        desired = desired.add(right.scale(strafe * speed * FLIGHT_STRAFE_SCALE));

        double flex = p.isSprinting() ? SWIM_SPRINT_FLEX : FLIGHT_FLEX_PER_TICK;
        Vec3 delta = desired.subtract(swimVector);
        double deltaLen = delta.length();
        if (deltaLen > flex && deltaLen > 1.0e-9) {
            delta = delta.scale(flex / deltaLen);
        }

        swimVector = swimVector.add(delta);

        // Scale swim caps proportionally to active swim speed
        double capScale = speed / FLIGHT_SPEED;
        swimVector = clampSpeed(swimVector, MAX_XZ * capScale, MAX_Y * capScale);

        p.setDeltaMovement(swimVector);
    }
}
