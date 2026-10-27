package com.jttw.engine.mixin;

import com.jttw.engine.village.YaoguaiFear;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's villagers take a Yaoguai player near them for a hostile, as they take a zombie, and so
 * panic and run ({@link YaoguaiFear}). The sensor names the nearest hostile it sees; a demon within
 * {@link YaoguaiFear#RADIUS} counts as one.
 */
@Mixin(VillagerHostilesSensor.class)
public abstract class VillagerHostilesSensorMixin {

    @Inject(method = "isMatchingEntity", at = @At("HEAD"), cancellable = true)
    private void jttw$fearDemons(LivingEntity villager, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        if (YaoguaiFear.frightens(villager, other)) cir.setReturnValue(true);
    }
}
