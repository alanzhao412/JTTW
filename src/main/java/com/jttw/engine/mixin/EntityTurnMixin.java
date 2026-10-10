package com.jttw.engine.mixin;

import com.jttw.engine.client.flight.FlightOrientation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client only. While the local player flies, the mouse turns their free orientation
 * ({@link FlightOrientation}) instead of vanilla's yaw and pitch, which stop at the vertical.
 */
@Mixin(Entity.class)
public class EntityTurnMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void jttw$freeFlightTurn(double dx, double dy, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer p && FlightOrientation.turn(p, dx, dy)) ci.cancel();
    }
}
