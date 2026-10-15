package com.jttw.engine.transformation;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A player in a boat form is a boat: other players can ride in it while its owner steers, and
 * on water it floats at the surface and sails at a boat's speed instead of swimming.
 *
 * <ul>
 *   <li>Seats: two in a boat, one in a chest boat (the chest takes the back seat), placed as a
 *       real boat places its passengers. Riders get off by sneaking.</li>
 *   <li>On water ({@link #sail}): rises to float with a little of its hull under, and glides
 *       toward the steering direction at up to {@value #MAX_SPEED} blocks a tick (a boat's
 *       8 blocks a second), drifting to a stop when not steered.</li>
 * </ul>
 * Movement is the client's for its own player, so {@code LocalPlayerMixin} applies {@link #sail}.
 */
public final class BoatForms {

    private BoatForms() {}

    /** Blocks per tick at full speed on water. */
    public static final double MAX_SPEED = 0.4;
    /** How deep the hull sits in the water when afloat. */
    public static final double FLOAT_DEPTH = 0.15;
    private static final double RISE_PER_TICK = 0.05;
    private static final double MAX_RISE = 0.15;
    private static final double STEER = 0.3;
    private static final double DRIFT = 0.1;

    /** Seats this player has as a boat: 2 for a boat, 1 for a chest boat, 0 if not a boat. */
    public static int seats(Player player) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || data.isBlockForm() || data.isPlayerForm()) return 0;
        EntityType<?> t = data.getEntityType();
        if (t == EntityType.BOAT) return 2;
        if (t == EntityType.CHEST_BOAT) return 1;
        return 0;
    }

    /** Where a rider sits: as a vanilla boat seats them, from the boat-player's position and yaw. */
    public static Vec3 seatPosition(Player boat, Entity passenger) {
        int index = boat.getPassengers().indexOf(passenger);
        float along = boat.getPassengers().size() > 1 ? (index == 0 ? 0.2F : -0.6F) : 0.0F;
        if (seats(boat) == 1) along = 0.15F; // a chest boat's only seat sits forward of the chest
        if (passenger instanceof Animal) along += 0.2F;
        Vec3 offset = new Vec3(along, 0, 0).yRot(-boat.getYRot() * Mth.DEG_TO_RAD - Mth.HALF_PI);
        // a boat's seat a third of its height up, less how far down the rider's own attachment sits
        double y = boat.getY() + EntityType.BOAT.getDimensions().height() / 3.0F - passenger.getVehicleAttachmentPoint(boat).y;
        return new Vec3(boat.getX() + offset.x, y, boat.getZ() + offset.z);
    }

    /**
     * The boat's next velocity.
     *
     * @param v         current velocity
     * @param inWater   the hull is in water
     * @param submerged how deep the water stands on the hull
     * @param forward   steering input forward (-1 to 1)
     * @param strafe    steering input sideways (-1 to 1, positive is left)
     * @param yaw       facing, in degrees
     */
    public static Vec3 sail(Vec3 v, boolean inWater, double submerged, float forward, float strafe, float yaw) {
        if (!inWater) return v;
        double vy;
        if (submerged > FLOAT_DEPTH) vy = Math.min(MAX_RISE, Math.max(v.y, 0) + RISE_PER_TICK);
        else vy = Math.max(0, Math.min(v.y, 0.0)); // afloat: hold at the surface
        double len = Math.sqrt(forward * forward + strafe * strafe);
        double tx = 0, tz = 0;
        if (len > 1e-3) {
            double f = forward / Math.max(1, len), s = strafe / Math.max(1, len);
            double sin = Mth.sin(yaw * Mth.DEG_TO_RAD), cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
            tx = (s * cos - f * sin) * MAX_SPEED;
            tz = (f * cos + s * sin) * MAX_SPEED;
        }
        double k = len > 1e-3 ? STEER : DRIFT;
        return new Vec3(v.x + (tx - v.x) * k, vy, v.z + (tz - v.z) * k);
    }

    /** One tick of sailing for a boat-form player (client side, for the local player). */
    public static void sail(Player player) {
        if (seats(player) == 0) return;
        boolean inWater = player.isInWater();
        player.setDeltaMovement(sail(player.getDeltaMovement(), inWater, player.getFluidHeight(FluidTags.WATER),
                player.zza, player.xxa, player.getYRot()));
        if (inWater) {
            player.setSwimming(false);
            player.setAirSupply(player.getMaxAirSupply());
        }
    }
}
