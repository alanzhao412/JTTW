package com.jttw.engine.flight;

import com.jttw.engine.transformation.TransformationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class JTTWFlight {

    private JTTWFlight() {}

    private static final String UNIVERSAL_FLIGHT_TAG  = "jttw_universal_flight";
    private static final String FLIGHT_SPEED_TIER_TAG = "jttw_flight_speed_tier";
    private static final String GLIDE_ONLY_TAG        = "jttw_glide_only";

    // ---- Glide tuning (Dragon tier 1) ----
    private static final double GLIDE_SPEED     = 1.20;   // slower than full flight
    private static final double GLIDE_SINK_RATE = -0.15;  // gentle downward drift
    private static final double GLIDE_MAX_Y_UP  = 0.0;    // cannot gain altitude

    // Stored flight vector (Palladium-style turn limiting)
    private static final String FLIGHT_VEC_X = "jttw_flight_vec_x";
    private static final String FLIGHT_VEC_Y = "jttw_flight_vec_y";
    private static final String FLIGHT_VEC_Z = "jttw_flight_vec_z";

    // ---- Flight tuning ----
    // T1 = glide only (all characters). T2 = full flight. T3 = fastest + dimension travel.
    private static final double FLIGHT_SPEED_T2         = 1.55;  // standard full flight
    private static final double FLIGHT_SPEED_T3         = 2.40;  // fastest tier
    private static final double FLIGHT_SPEED_T2_WUKONG  = 1.85;  // Wukong is faster than others at T2
    private static final double FLIGHT_SPEED_T3_WUKONG  = 2.80;  // Wukong is fastest at T3
    private static final double FLIGHT_SPEED_TRANSFORM  = 1.10;  // flying mob transformations — slower than full flight
    private static final double FLIGHT_FLEX_PER_TICK = 0.12;
    private static final double FLIGHT_STRAFE_SCALE = 0.85;

    private static final double MAX_Y = 1.75;
    private static final double MAX_XZ = 2.25;

    // Palladium-style decay
    private static final double NO_INPUT_DECAY = 0.92;
    private static final double NO_INPUT_CUTOFF_SQR = 0.001; // stop when negligible

    // Same decay used after flight ends so the glide feels identical
    private static final double LANDING_DECAY = 0.9;
    private static final String WAS_FLYING_TAG = "jttw_was_flying";

    // ---------------------------
    // Swim (Aquatic flight-style steering)
    // ---------------------------

    private static final double SWIM_MAX_SPEED = 0.65;         // normal swim — cruising
    private static final double SWIM_SPRINT_SPEED = 2.20;      // sprint swim — darting
    private static final double SWIM_SPRINT_FLEX = 0.25;        // faster acceleration when sprinting

    // Stored swim vector
    private static final String SWIM_VEC_X = "jttw_swim_vec_x";
    private static final String SWIM_VEC_Y = "jttw_swim_vec_y";
    private static final String SWIM_VEC_Z = "jttw_swim_vec_z";

    // ---------------------------

    public static void setUniversalFlight(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(UNIVERSAL_FLIGHT_TAG, enabled);
    }

    public static boolean hasUniversalFlight(ServerPlayer player) {
        return player.getPersistentData().getBoolean(UNIVERSAL_FLIGHT_TAG);
    }

    public static void setGlideOnly(ServerPlayer player, boolean glide) {
        player.getPersistentData().putBoolean(GLIDE_ONLY_TAG, glide);
    }

    public static boolean isGlideOnly(ServerPlayer player) {
        return player.getPersistentData().getBoolean(GLIDE_ONLY_TAG);
    }

    /**
     * Sets the flight speed tier (1, 2, or 3) stored in the player's persistent data.
     * Called from {@link com.jttw.engine.network.SpendSkillPointPacket} when a
     * flight node is unlocked.
     */
    public static void setFlightSpeedTier(ServerPlayer player, int tier) {
        player.getPersistentData().putInt(FLIGHT_SPEED_TIER_TAG, Math.max(1, Math.min(3, tier)));
    }

    private static double getFlightSpeed(ServerPlayer player) {
        // If player has no skill-tree flight (tier 0), they're using transform flight — slower
        int tier = player.getPersistentData().getInt(FLIGHT_SPEED_TIER_TAG);
        if (tier == 0) {
            return FLIGHT_SPEED_TRANSFORM;
        }

        boolean wukong = "celestial_primate".equals(
                player.getPersistentData().getString(
                        com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME));

        return switch (tier) {
            case 3  -> wukong ? FLIGHT_SPEED_T3_WUKONG : FLIGHT_SPEED_T3;
            case 2  -> wukong ? FLIGHT_SPEED_T2_WUKONG : FLIGHT_SPEED_T2;
            default -> FLIGHT_SPEED_T2; // fallback
        };
    }

    public static void onServerPlayerTick(ServerPlayer player, @Nullable TransformationData data) {
        if (player.isCreative() || player.isSpectator()) return;

        boolean aquatic = (data != null && isAquatic(data));
        boolean inWater = isInWaterOrBubbleColumn(player);

        // Requirement:
        // - When transformed into a water animal, turn off flight.
        // - In water, water animals move like the flight system, but only inside water.
        if (aquatic) {
            // Force flight OFF for aquatic forms (even if the form would normally grant it)
            if (player.getAbilities().mayfly || player.getAbilities().flying) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }

            // Clear flight momentum tags so they don't interfere
            setStoredFlightVector(player, Vec3.ZERO);
            player.getPersistentData().putBoolean(WAS_FLYING_TAG, false);

            // Apply swim-flight steering only while in water/bubble
            if (inWater) {
                applyAquaticFlight(player);
            } else {
                // Leaving water: clear swim momentum so it doesn't carry onto land
                setStoredSwimVector(player, Vec3.ZERO);
            }
            return;
        } else {
            // Not aquatic: ensure swim momentum is cleared so it never bleeds into non-aquatic play
            setStoredSwimVector(player, Vec3.ZERO);
        }

        // Normal flight logic (unchanged behavior for non-aquatic)
        boolean transformFlight = (data != null && data.canFly());
        boolean universalFlight = hasUniversalFlight(player);
        boolean shouldHaveMayfly = transformFlight || universalFlight;

        if (player.getAbilities().mayfly != shouldHaveMayfly) {
            player.getAbilities().mayfly = shouldHaveMayfly;
            if (!shouldHaveMayfly) player.getAbilities().flying = false;
            player.onUpdateAbilities();

            if (!shouldHaveMayfly) {
                // Carry momentum forward instead of snapping to zero
                Vec3 momentum = getStoredFlightVector(player);
                if (momentum.lengthSqr() > NO_INPUT_CUTOFF_SQR) {
                    player.setDeltaMovement(momentum);
                }
                setStoredFlightVector(player, Vec3.ZERO);
                player.getPersistentData().putBoolean(WAS_FLYING_TAG, false);
            }
        }

        if (shouldHaveMayfly && player.getAbilities().flying) {
            player.getPersistentData().putBoolean(WAS_FLYING_TAG, true);
            applyFlight(player);
            return;
        }

        // Player has mayfly but just toggled flight off — glide with decaying momentum
        if (shouldHaveMayfly && player.getPersistentData().getBoolean(WAS_FLYING_TAG)) {
            Vec3 momentum = getStoredFlightVector(player);
            if (momentum.lengthSqr() > NO_INPUT_CUTOFF_SQR) {
                player.setDeltaMovement(momentum);
                setStoredFlightVector(player, momentum.scale(LANDING_DECAY));
            } else {
                setStoredFlightVector(player, Vec3.ZERO);
                player.getPersistentData().putBoolean(WAS_FLYING_TAG, false);
            }
        }
    }

    private static boolean isAquatic(TransformationData data) {
        net.minecraft.world.entity.EntityType<?> type = data.getEntityType();
        MobCategory cat = type.getCategory();
        if (cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT) return true;
        // Amphibious mobs — gets underwater swimming too
        return type == net.minecraft.world.entity.EntityType.FROG
                || type == net.minecraft.world.entity.EntityType.AXOLOTL
                || type == net.minecraft.world.entity.EntityType.GUARDIAN
                || type == net.minecraft.world.entity.EntityType.ELDER_GUARDIAN;
    }

    private static boolean isInWaterOrBubbleColumn(ServerPlayer player) {
        if (player.isInWater()) return true;
        try {
            return player.level().getBlockState(player.blockPosition()).is(Blocks.BUBBLE_COLUMN);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ---------------------------
    // Flight
    // ---------------------------

    private static Vec3 getStoredFlightVector(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        if (!tag.contains(FLIGHT_VEC_X)) return Vec3.ZERO;
        return new Vec3(
                tag.getDouble(FLIGHT_VEC_X),
                tag.getDouble(FLIGHT_VEC_Y),
                tag.getDouble(FLIGHT_VEC_Z)
        );
    }

    private static void setStoredFlightVector(ServerPlayer p, Vec3 v) {
        CompoundTag tag = p.getPersistentData();
        tag.putDouble(FLIGHT_VEC_X, v.x);
        tag.putDouble(FLIGHT_VEC_Y, v.y);
        tag.putDouble(FLIGHT_VEC_Z, v.z);
    }

    private static Vec3 clampSpeed(Vec3 v) {
        double xz = Math.sqrt(v.x * v.x + v.z * v.z);
        if (xz > MAX_XZ && xz > 1.0e-9) {
            double s = MAX_XZ / xz;
            v = new Vec3(v.x * s, v.y, v.z * s);
        }
        v = new Vec3(v.x, Mth.clamp(v.y, -MAX_Y, MAX_Y), v.z);
        return v;
    }

    private static void applyFlight(ServerPlayer player) {
        if (isGlideOnly(player)) {
            applyGlide(player);
            return;
        }

        Vec3 flightVector = getStoredFlightVector(player);

        double forward = player.zza;
        double strafe  = player.xxa;

        boolean hasMoveInput = (Math.abs(forward) > 0.01) || (Math.abs(strafe) > 0.01);

        if (!hasMoveInput) {
            Vec3 decayed = flightVector.scale(NO_INPUT_DECAY);

            if (decayed.lengthSqr() < NO_INPUT_CUTOFF_SQR) {
                decayed = Vec3.ZERO;
            }

            setStoredFlightVector(player, decayed);
            player.setDeltaMovement(decayed);
            player.fallDistance = 0;
            return;
        }

        double speed = getFlightSpeed(player);

        Vec3 look = player.getLookAngle().normalize();
        Vec3 desired = look.scale(forward * speed);

        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() < 1.0e-9) lookFlat = new Vec3(0, 0, 1);
        lookFlat = lookFlat.normalize();

        Vec3 right = new Vec3(lookFlat.z, 0, -lookFlat.x).normalize();
        desired = desired.add(right.scale(strafe * speed * FLIGHT_STRAFE_SCALE));

        Vec3 delta = desired.subtract(flightVector);
        double deltaLen = delta.length();
        if (deltaLen > FLIGHT_FLEX_PER_TICK && deltaLen > 1.0e-9) {
            delta = delta.scale(FLIGHT_FLEX_PER_TICK / deltaLen);
        }

        flightVector = flightVector.add(delta);
        flightVector = clampSpeed(flightVector);

        setStoredFlightVector(player, flightVector);
        player.setDeltaMovement(flightVector);
        player.fallDistance = 0;
    }

    /**
     * Elytra-style glide for Dragon flight tier 1.
     * Player can steer horizontally and control descent angle, but cannot gain altitude.
     * Looking up slows descent, looking down speeds it up — like real elytra.
     */
    private static void applyGlide(ServerPlayer player) {
        Vec3 glideVector = getStoredFlightVector(player);

        double forward = player.zza;
        double strafe  = player.xxa;

        boolean hasMoveInput = (Math.abs(forward) > 0.01) || (Math.abs(strafe) > 0.01);

        if (!hasMoveInput) {
            // Still glide forward with sink when no input
            Vec3 decayed = new Vec3(
                    glideVector.x * NO_INPUT_DECAY,
                    GLIDE_SINK_RATE,
                    glideVector.z * NO_INPUT_DECAY
            );
            if (decayed.horizontalDistanceSqr() < NO_INPUT_CUTOFF_SQR) {
                decayed = new Vec3(0, GLIDE_SINK_RATE, 0);
            }
            setStoredFlightVector(player, decayed);
            player.setDeltaMovement(decayed);
            player.fallDistance = 0;
            return;
        }

        Vec3 look = player.getLookAngle().normalize();

        // Horizontal steering
        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() < 1.0e-9) lookFlat = new Vec3(0, 0, 1);
        lookFlat = lookFlat.normalize();

        Vec3 desired = lookFlat.scale(forward * GLIDE_SPEED);

        Vec3 right = new Vec3(lookFlat.z, 0, -lookFlat.x).normalize();
        desired = desired.add(right.scale(strafe * GLIDE_SPEED * FLIGHT_STRAFE_SCALE));

        // Y component: looking down = faster descent, looking up = slower descent (but never up)
        double yComponent = Math.min(GLIDE_MAX_Y_UP, look.y * GLIDE_SPEED * 0.5 + GLIDE_SINK_RATE);
        desired = new Vec3(desired.x, yComponent, desired.z);

        Vec3 delta = desired.subtract(glideVector);
        double deltaLen = delta.length();
        if (deltaLen > FLIGHT_FLEX_PER_TICK && deltaLen > 1.0e-9) {
            delta = delta.scale(FLIGHT_FLEX_PER_TICK / deltaLen);
        }

        glideVector = glideVector.add(delta);

        // Clamp horizontal speed, enforce no upward movement
        double xz = Math.sqrt(glideVector.x * glideVector.x + glideVector.z * glideVector.z);
        if (xz > MAX_XZ && xz > 1.0e-9) {
            double s = MAX_XZ / xz;
            glideVector = new Vec3(glideVector.x * s, glideVector.y, glideVector.z * s);
        }
        glideVector = new Vec3(glideVector.x, Math.min(GLIDE_MAX_Y_UP, glideVector.y), glideVector.z);

        setStoredFlightVector(player, glideVector);
        player.setDeltaMovement(glideVector);
        player.fallDistance = 0;
    }

    // ---------------------------
    // Swim (flight-style steering, only in water/bubble)
    // ---------------------------

    private static Vec3 getStoredSwimVector(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        if (!tag.contains(SWIM_VEC_X)) return Vec3.ZERO;
        return new Vec3(
                tag.getDouble(SWIM_VEC_X),
                tag.getDouble(SWIM_VEC_Y),
                tag.getDouble(SWIM_VEC_Z)
        );
    }

    private static void setStoredSwimVector(ServerPlayer p, Vec3 v) {
        CompoundTag tag = p.getPersistentData();
        tag.putDouble(SWIM_VEC_X, v.x);
        tag.putDouble(SWIM_VEC_Y, v.y);
        tag.putDouble(SWIM_VEC_Z, v.z);
    }

    private static Vec3 clampSwimSpeed(Vec3 v, double activeSpeed) {
        double scale = activeSpeed / FLIGHT_SPEED_T2;

        double maxXZ = MAX_XZ * scale;
        double maxY  = MAX_Y  * scale;

        double xz = Math.sqrt(v.x * v.x + v.z * v.z);
        if (xz > maxXZ && xz > 1.0e-9) {
            double s = maxXZ / xz;
            v = new Vec3(v.x * s, v.y, v.z * s);
        }
        v = new Vec3(v.x, Mth.clamp(v.y, -maxY, maxY), v.z);
        return v;
    }

    private static void applyAquaticFlight(ServerPlayer player) {
        Vec3 swimVector = getStoredSwimVector(player);

        double forward = player.zza;
        double strafe  = player.xxa;

        boolean hasMoveInput = (Math.abs(forward) > 0.01) || (Math.abs(strafe) > 0.01);

        if (!hasMoveInput) {
            Vec3 decayed = swimVector.scale(NO_INPUT_DECAY);
            if (decayed.lengthSqr() < NO_INPUT_CUTOFF_SQR) decayed = Vec3.ZERO;
            setStoredSwimVector(player, decayed);
            player.setDeltaMovement(decayed);
            return;
        }

        double speed = player.isSprinting() ? SWIM_SPRINT_SPEED : SWIM_MAX_SPEED;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 desired = look.scale(forward * speed);

        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        if (lookFlat.lengthSqr() < 1.0e-9) lookFlat = new Vec3(0, 0, 1);
        lookFlat = lookFlat.normalize();

        Vec3 right = new Vec3(lookFlat.z, 0, -lookFlat.x).normalize();
        desired = desired.add(right.scale(strafe * speed * FLIGHT_STRAFE_SCALE));

        double flex = player.isSprinting() ? SWIM_SPRINT_FLEX : FLIGHT_FLEX_PER_TICK;
        Vec3 delta = desired.subtract(swimVector);
        double deltaLen = delta.length();
        if (deltaLen > flex && deltaLen > 1.0e-9) {
            delta = delta.scale(flex / deltaLen);
        }

        swimVector = swimVector.add(delta);
        swimVector = clampSwimSpeed(swimVector, speed);

        setStoredSwimVector(player, swimVector);
        player.setDeltaMovement(swimVector);
    }
}