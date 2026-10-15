package com.jttw.engine.client.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlightAttitude;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * Free flight: the local player's whole orientation while they fly, so the mouse is not stopped at
 * straight up or straight down. Pulling it on up past the vertical carries the view over and down
 * the far side, upside down, into a loop like a rollercoaster's.
 *
 * <p>The orientation is kept as a quaternion. Moving the mouse up or down turns it about the
 * flier's own right hand (so pitch has no end); moving it left or right turns it about the world's
 * up (so the horizon stays where it is). From it come the player's yaw and pitch (which steer the
 * flight, as before) and a roll for the camera, which turns the view over at the top of a loop.
 * Landing, the roll eases back to level.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FlightOrientation {

    private FlightOrientation() {}

    /** Degrees per unit of mouse movement: vanilla's. */
    private static final double SENSITIVITY = 0.15;
    /** Ticks for the camera's roll to ease back to level after landing. */
    private static final int LEVEL_OUT_TICKS = 8;

    private static final Quaternionf ORIENTATION = new Quaternionf();
    private static boolean active;
    private static float roll, rollOld, settle;
    /** The yaw and pitch last given the player from the orientation, to notice anything else setting them. */
    private static float givenYaw, givenPitch;

    /** Whether a player is flying in a way the mod gives them (not creative's or a spectator's). */
    public static boolean flies(Player p) {
        return com.jttw.engine.flight.FlyingState.flies(p);
    }

    public static boolean active() {
        return active;
    }

    /** The orientation a yaw and pitch (degrees) make, with no roll. */
    public static Quaternionf of(float yaw, float pitch) {
        return FlightAttitude.of(yaw, pitch);
    }

    /** The local flier's orientation, as it stands (a copy). */
    public static Quaternionf orientation() {
        return new Quaternionf(ORIENTATION);
    }

    /** The camera's roll, in degrees, between ticks. */
    public static float roll(float partialTick) {
        // flying, the mouse sets it every frame: as it stands; levelling out, between ticks
        return active ? roll : Mth.rotLerp(partialTick, rollOld, roll);
    }

    /**
     * Turns the flier by a mouse movement (vanilla's units), instead of vanilla's turning, which
     * stops at the vertical. Returns false (vanilla turns) when the local player is not flying.
     */
    public static boolean turn(LocalPlayer p, double dx, double dy) {
        if (!active) return false;
        FlightAttitude.turn(ORIENTATION, (float) (dx * SENSITIVITY), (float) (dy * SENSITIVITY));
        apply(p);
        return true;
    }

    /** Sets the player's yaw and pitch from the orientation (they steer the flight), and the camera's roll. */
    private static void apply(LocalPlayer p) {
        float[] a = FlightAttitude.angles(ORIENTATION);
        float yaw = a[0], pitch = a[1], r = a[2];
        // keep the turn continuous: the yaw follows on from where it was, never wrapping a full turn
        float was = p.getYRot();
        yaw = was + Mth.wrapDegrees(yaw - was);
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        p.setYHeadRot(yaw);
        givenYaw = yaw;
        givenPitch = pitch;
        roll = r;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer p = Minecraft.getInstance().player;
        rollOld = roll;
        boolean flying = p != null && flies(p);
        if (flying && !active) {
            seed(p);
            active = true;
            settle = 0;
        } else if (active && (p.getYRot() != givenYaw || p.getXRot() != givenPitch)) {
            // something else turned them (a teleport, the server): fly on from there, level
            seed(p);
        } else if (!flying && active) {
            active = false;
            settle = LEVEL_OUT_TICKS;
            if (p != null) {
                // back to vanilla's limits: looking the way they were, the right way up
                p.setXRot(Mth.clamp(p.getXRot(), -90, 90));
            }
        }
        if (!active && settle > 0) {
            settle--;
            roll = Mth.wrapDegrees(roll) * (settle / (float) LEVEL_OUT_TICKS);
        }
    }

    private static void seed(LocalPlayer p) {
        ORIENTATION.set(of(p.getYRot(), p.getXRot()));
        givenYaw = p.getYRot();
        givenPitch = p.getXRot();
        roll = rollOld = 0;
    }

    /** The camera's roll, while flying (and while it levels out after). */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active && settle == 0 && roll == 0) return;
        event.setRoll(event.getRoll() + roll((float) event.getPartialTick()));
    }
}
