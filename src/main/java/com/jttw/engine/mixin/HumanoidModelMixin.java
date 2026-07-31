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
}