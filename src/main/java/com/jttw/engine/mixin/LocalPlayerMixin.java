package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side: keep flying=true for phasing players so spacebar
 * acts as flight toggle rather than jump.
 *
 * Block collision is now handled server-side by BlockStateBaseMixin +
 * EntityPhaseMixin, so no noPhysics manipulation is needed here.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "aiStep()V", at = @At("TAIL"))
    private void jttw_keepFlying(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer)(Object)this;
        TransformationData data = TransformationManager.getTransformation(self);
        if (data != null && data.canPhase()) {
            self.getAbilities().flying = true;
        }
        // A boat form floats and sails on water (movement is this client's to decide)
        com.jttw.engine.transformation.BoatForms.sail(self);
    }
}