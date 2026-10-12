package com.jttw.engine.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets HumanoidModel to override arm poses for clone husks.
 *
 * The zombie arm-raise (from AbstractZombieModel → AnimationUtils.animateZombieArms)
 * is prevented server-side by forcing setAggressive(false) every tick in WuCloneManager.
 * When isAggressive is false, animateZombieArms does nothing, so this TAIL inject on
 * HumanoidModel.setupAnim gets the final say on arm rotation.
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {

    private static final String PREFIX = "jttw_clone:";

    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    private static boolean jttw$isClone(LivingEntity entity) {
        if (!(entity instanceof Husk)) return false;
        Component name = entity.getCustomName();
        return name != null && name.getString().startsWith(PREFIX);
    }

    // Prevent attack animation from forcing weird arm poses on clones
    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void jttw$disableAttackPose(LivingEntity entity, float ageInTicks, CallbackInfo ci) {
        if (jttw$isClone(entity)) ci.cancel();
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jttw$forceArmsDown(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (entity instanceof net.minecraft.world.entity.player.Player p) {
            jttw$flightLimbs(p, ageInTicks);
            jttw$staffAtEase(p);
            return;
        }
        if (!jttw$isClone(entity)) return;

        // Natural player arm swing — arms at sides, swinging with walk cycle
        float swing = limbSwing * 0.6662F;
        float amt   = limbSwingAmount;

        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 2.0F * amt * 0.5F;
        this.leftArm.xRot  = Mth.cos(swing) * 2.0F * amt * 0.5F;

        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.yRot  = 0.0F;
        this.leftArm.zRot  = 0.0F;
    }

    /**
     * A flier's limbs (see FlightPose). Stretched out along the flight: arms at the sides, legs
     * together. On Wukong's cloud: knees bent (legs forward, one more than the other, as the
     * single-piece legs can show it), arms trailing behind, further the faster he goes.
     */
    private void jttw$flightLimbs(net.minecraft.world.entity.player.Player p, float ageInTicks) {
        float[] w = com.jttw.engine.client.flight.FlightPose.limbs(p, ageInTicks - (int) ageInTicks);
        if (w == null) return;
        float stretched = w[0], cloud = w[1], move = w[2];
        if (stretched > 0) {
            jttw$toward(rightArm, stretched, 0.05f, 0, 0.12f);
            jttw$toward(leftArm, stretched, 0.05f, 0, -0.12f);
            jttw$toward(rightLeg, stretched, 0.04f, 0, 0.03f);
            jttw$toward(leftLeg, stretched, -0.04f, 0, -0.03f);
        }
        if (cloud > 0) {
            float trail = 0.55f + 0.55f * move, flutter = 0.06f * Mth.sin(ageInTicks * 0.35f) * (0.4f + move);
            jttw$toward(rightArm, cloud, trail + flutter, 0.1f, 0.22f);
            jttw$toward(leftArm, cloud, trail - flutter, -0.1f, -0.22f);
            jttw$toward(rightLeg, cloud, -0.42f, 0.06f, 0.07f);
            jttw$toward(leftLeg, cloud, -0.18f, -0.06f, -0.07f);
        }
    }

    /** The staff across the shoulders (StaffStance): the arms out along it, draped over it. */
    private void jttw$staffAtEase(net.minecraft.world.entity.player.Player p) {
        if (!com.jttw.engine.client.StaffStance.resting(p)) return;
        rightArm.xRot = ARM_BACK;
        rightArm.yRot = 0;
        rightArm.zRot = ARM_OUT;
        leftArm.xRot = ARM_BACK;
        leftArm.yRot = 0;
        leftArm.zRot = -ARM_OUT;
    }

    /** At ease with the staff: how far back, and out (radians), the arms lie along it. */
    private static final float ARM_BACK = -0.3f, ARM_OUT = 2.0f;

    private static void jttw$toward(ModelPart part, float weight, float x, float y, float z) {
        part.xRot = Mth.lerp(weight, part.xRot, x);
        part.yRot = Mth.lerp(weight, part.yRot, y);
        part.zRot = Mth.lerp(weight, part.zRot, z);
    }
}
