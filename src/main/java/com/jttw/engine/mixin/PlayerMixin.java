package com.jttw.engine.mixin;

import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(
            method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jttw_overrideDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null) return;

        // Fixed gives the most faithful "match the target entity hitbox" behavior,
        // especially for non-living objects like arrows.
        cir.setReturnValue(EntityDimensions.fixed(data.getWidth(), data.getHeight()));
    }

    @Inject(
            method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jttw_overrideStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null) return;

        cir.setReturnValue(data.getEyeHeight());
    }

    /** The Marshal of the Heavenly Canopy's bottomless stomach: he can always eat. */
    @Inject(method = "canEat(Z)Z", at = @At("HEAD"), cancellable = true)
    private void jttw_bottomlessStomach(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        if (MarshalAbilityHandler.canAlwaysEat((Player) (Object) this)) cir.setReturnValue(true);
    }
}