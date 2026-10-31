package com.jttw.engine.mixin;

import com.jttw.engine.client.ZoneClouds;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The clouds over a dark zone take its sky's colour ({@link ZoneClouds}). */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void jttw_zoneClouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(ZoneClouds.cloudColour(cir.getReturnValue()));
    }
}
