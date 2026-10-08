package com.jttw.engine.flight;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncFlightPacket;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server side of the mod's flight: who may fly, how fast, and the steering mirrored here.
 *
 * <p>The client moves the player, so what it needs to fly the way the server decides (speed,
 * glide-only, boost) is sent as a {@link FlightProfile} whenever it changes, and both sides
 * steer with the same {@link FlightSteering}.
 *
 * <p>Tiers: 1 glide only, 2 full flight, 3 fastest. A flying form with no tier of its own flies
 * slower than either.
 */
public final class JTTWFlight {

    private JTTWFlight() {}

    private static final String UNIVERSAL_FLIGHT_TAG  = "jttw_universal_flight";
    private static final String FLIGHT_SPEED_TIER_TAG = "jttw_flight_speed_tier";
    private static final String GLIDE_ONLY_TAG        = "jttw_glide_only";
    private static final String BOOST_TAG             = "jttw_flight_boost";
    private static final String THRUST_TAG            = "jttw_flight_thrust";
    private static final String WAS_FLYING_TAG        = "jttw_was_flying";

    // Stored flight vector (Palladium-style turn limiting)
    private static final String FLIGHT_VEC_X = "jttw_flight_vec_x";
    private static final String FLIGHT_VEC_Y = "jttw_flight_vec_y";
    private static final String FLIGHT_VEC_Z = "jttw_flight_vec_z";
    private static final String SWIM_VEC_X = "jttw_swim_vec_x";
    private static final String SWIM_VEC_Y = "jttw_swim_vec_y";
    private static final String SWIM_VEC_Z = "jttw_swim_vec_z";

    /** The profile each player's client last got. A new player object (login, respawn) gets it again. */
    private static final Map<ServerPlayer, FlightProfile> SENT = new WeakHashMap<>();

    // ── who flies, and how ─────────────────────────────────────────

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

    /** Sets the flight speed tier (1, 2, or 3), when a flight node is unlocked or granted. */
    public static void setFlightSpeedTier(ServerPlayer player, int tier) {
        player.getPersistentData().putInt(FLIGHT_SPEED_TIER_TAG, Math.max(1, Math.min(3, tier)));
    }

    /** A boost on top of the flight speed (1 = none); {@code thrust} drives them forward unasked. */
    public static void setBoost(ServerPlayer player, double boost, boolean thrust) {
        CompoundTag tag = player.getPersistentData();
        if (boost <= 1.0 && !thrust) {
            tag.remove(BOOST_TAG);
            tag.remove(THRUST_TAG);
        } else {
            tag.putDouble(BOOST_TAG, boost);
            tag.putBoolean(THRUST_TAG, thrust);
        }
    }

    public static double boost(ServerPlayer player) {
        return Math.max(1.0, player.getPersistentData().getDouble(BOOST_TAG));
    }

    /** How this player flies now. */
    public static FlightProfile profile(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        boolean wukong = "celestial_primate".equals(
                tag.getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME));
        return new FlightProfile(FlightSteering.speedFor(tag.getInt(FLIGHT_SPEED_TIER_TAG), wukong),
                isGlideOnly(player), boost(player), tag.getBoolean(THRUST_TAG));
    }

    /** Sends the client its flight profile if it has changed since it last got one. */
    public static void syncProfile(ServerPlayer player) {
        FlightProfile now = profile(player);
        if (now.equals(SENT.get(player))) return;
        SENT.put(player, now);
        NetworkHandler.sendToPlayer(player, new SyncFlightPacket(now));
    }

    // ── each tick ──────────────────────────────────────────────────

    public static void onServerPlayerTick(ServerPlayer player, @Nullable TransformationData data) {
        syncProfile(player);
        if (player.isCreative() || player.isSpectator()) return;

        boolean aquatic = (data != null && isAquatic(data));
        boolean inWater = isInWaterOrBubbleColumn(player);

        // A water creature does not fly; in water it swims the way others fly
        if (aquatic) {
            if (player.getAbilities().mayfly || player.getAbilities().flying) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            setStoredFlightVector(player, Vec3.ZERO);
            player.getPersistentData().putBoolean(WAS_FLYING_TAG, false);

            if (inWater) {
                Vec3 v = FlightSteering.swim(getStoredSwimVector(player), player.zza, player.xxa,
                        player.getLookAngle(), player.isSprinting());
                setStoredSwimVector(player, v);
                player.setDeltaMovement(v);
            } else {
                setStoredSwimVector(player, Vec3.ZERO); // leaving water: no momentum onto land
            }
            return;
        }
        setStoredSwimVector(player, Vec3.ZERO);

        boolean transformFlight = (data != null && data.canFly());
        boolean shouldHaveMayfly = transformFlight || hasUniversalFlight(player);

        if (player.getAbilities().mayfly != shouldHaveMayfly) {
            player.getAbilities().mayfly = shouldHaveMayfly;
            if (!shouldHaveMayfly) player.getAbilities().flying = false;
            player.onUpdateAbilities();

            if (!shouldHaveMayfly) {
                // Carry momentum forward instead of snapping to zero
                Vec3 momentum = getStoredFlightVector(player);
                if (momentum.lengthSqr() > FlightSteering.NO_INPUT_CUTOFF_SQR) player.setDeltaMovement(momentum);
                setStoredFlightVector(player, Vec3.ZERO);
                player.getPersistentData().putBoolean(WAS_FLYING_TAG, false);
            }
        }

        if (shouldHaveMayfly && player.getAbilities().flying) {
            player.getPersistentData().putBoolean(WAS_FLYING_TAG, true);
            applyFlight(player);
            return;
        }

        // Just stopped flying: coast on decaying momentum
        if (shouldHaveMayfly && player.getPersistentData().getBoolean(WAS_FLYING_TAG)) {
            Vec3 momentum = getStoredFlightVector(player);
            if (momentum.lengthSqr() > FlightSteering.NO_INPUT_CUTOFF_SQR) {
                player.setDeltaMovement(momentum);
                setStoredFlightVector(player, momentum.scale(FlightSteering.LANDING_DECAY));
            } else {
                setStoredFlightVector(player, Vec3.ZERO);
                player.getPersistentData().putBoolean(WAS_FLYING_TAG, false);
            }
        }
    }

    private static void applyFlight(ServerPlayer player) {
        FlightProfile profile = profile(player);
        double forward = player.zza;
        if (profile.thrust() && forward <= 0.01) forward = 1.0;
        Vec3 v = profile.glide()
                ? FlightSteering.glide(getStoredFlightVector(player), forward, player.xxa, player.getLookAngle())
                : FlightSteering.fly(getStoredFlightVector(player), forward, player.xxa, player.getLookAngle(),
                        profile.effectiveSpeed());
        setStoredFlightVector(player, v);
        player.setDeltaMovement(v);
        player.fallDistance = 0;
    }

    /** Water creatures, and the amphibious mobs that swim like them. */
    public static boolean isAquatic(TransformationData data) {
        EntityType<?> type = data.getEntityType();
        if (type == null) return false;
        MobCategory cat = type.getCategory();
        if (cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT) return true;
        return type == EntityType.FROG || type == EntityType.AXOLOTL
                || type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN;
    }

    private static boolean isInWaterOrBubbleColumn(ServerPlayer player) {
        if (player.isInWater()) return true;
        try {
            return player.level().getBlockState(player.blockPosition()).is(Blocks.BUBBLE_COLUMN);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ── stored vectors ─────────────────────────────────────────────

    private static Vec3 getStoredFlightVector(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        if (!tag.contains(FLIGHT_VEC_X)) return Vec3.ZERO;
        return new Vec3(tag.getDouble(FLIGHT_VEC_X), tag.getDouble(FLIGHT_VEC_Y), tag.getDouble(FLIGHT_VEC_Z));
    }

    private static void setStoredFlightVector(ServerPlayer p, Vec3 v) {
        CompoundTag tag = p.getPersistentData();
        tag.putDouble(FLIGHT_VEC_X, v.x);
        tag.putDouble(FLIGHT_VEC_Y, v.y);
        tag.putDouble(FLIGHT_VEC_Z, v.z);
    }

    private static Vec3 getStoredSwimVector(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        if (!tag.contains(SWIM_VEC_X)) return Vec3.ZERO;
        return new Vec3(tag.getDouble(SWIM_VEC_X), tag.getDouble(SWIM_VEC_Y), tag.getDouble(SWIM_VEC_Z));
    }

    private static void setStoredSwimVector(ServerPlayer p, Vec3 v) {
        CompoundTag tag = p.getPersistentData();
        tag.putDouble(SWIM_VEC_X, v.x);
        tag.putDouble(SWIM_VEC_Y, v.y);
        tag.putDouble(SWIM_VEC_Z, v.z);
    }
}
