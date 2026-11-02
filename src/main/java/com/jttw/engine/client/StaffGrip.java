package com.jttw.engine.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;

/**
 * Wukong carrying the Ruyi Jingu Bang at ease (StaffStance): across his right shoulder, his right arm
 * bent at the elbow, the elbow back at his side and the fist before his breast, closed round the shaft
 * (the Monkey King's hand turned at the wrist so that the shaft runs through the fist, MonkeyKingModel;
 * the Buddha's, its hand in a wide sleeve, raised and hooked over it). For the Monkey King's body (MonkeyKingModel) and the
 * Victorious Fighting Buddha's (FightingBuddhaLayer): the upper arm is the player model's own right arm
 * (HumanoidModelMixin poses it, so armour sleeves follow it), the forearm folds at the elbow in the
 * drawn model. The angles were solved so the fist's middle lies on the staff's line (the staff as
 * StaffRestLayer and the Buddha's staff_rest lay it). Not while he strikes, uses the staff or holds
 * anything else: then StaffStance says he is not at ease.
 */
public final class StaffGrip {

    private StaffGrip() {}

    /** The Monkey King's upper arm (x, y, z radians) and the bend at his elbow. */
    public static final float MONKEY_X = -3.643f, MONKEY_Y = 0.524f, MONKEY_Z = -2.493f, MONKEY_ELBOW = 2.525f;
    /** The Buddha's upper arm and the bend at his elbow (his forearm shorter: the hand in a wide sleeve). */
    public static final float BUDDHA_X = -3.3f, BUDDHA_Y = -0.5f, BUDDHA_Z = -0.1f, BUDDHA_ELBOW = 2.6f;

    /** Whether {@code p} carries the staff at ease, drawn as the Monkey King or the Buddha. */
    public static boolean gripping(Player p) {
        return StaffStance.resting(p) && (FightingBuddhaClient.drawnAsMonkey(p) || FightingBuddhaClient.drawnAsBuddha(p));
    }

    /** Poses the player model's right arm (the upper arm) to grip the staff at ease, if he carries it so. */
    public static boolean poseUpperArm(Player p, ModelPart rightArm) {
        if (!gripping(p)) return false;
        boolean buddha = FightingBuddhaClient.drawnAsBuddha(p);
        rightArm.xRot = buddha ? BUDDHA_X : MONKEY_X;
        rightArm.yRot = buddha ? BUDDHA_Y : MONKEY_Y;
        rightArm.zRot = buddha ? BUDDHA_Z : MONKEY_Z;
        return true;
    }
}
