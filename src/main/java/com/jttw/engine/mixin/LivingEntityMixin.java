package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A transformed player's hitbox and eyes are their form's.
 *
 * <p>(Player.getDimensions is LivingEntity's, final, now, and the eye height is part of the
 * dimensions rather than a method of its own, so both are set here, for every pose, as the
 * PlayerMixin overrides of getDimensions and getStandingEyeHeight did.)
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
            method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jttw_overrideDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof Player player)) return;

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null) return;

        // Fixed gives the most faithful "match the target entity hitbox" behavior,
        // especially for non-living objects like arrows.
        cir.setReturnValue(EntityDimensions.fixed(data.getWidth(), data.getHeight()).withEyeHeight(data.getEyeHeight()));
    }
}
