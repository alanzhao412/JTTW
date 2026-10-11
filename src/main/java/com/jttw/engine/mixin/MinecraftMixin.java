package com.jttw.engine.mixin;

import com.jttw.engine.client.PrivateGlowClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** What glows for this player alone (PrivateGlowClient) is outlined as Glowing would be. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void jttw_privateGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (PrivateGlowClient.glows(entity)) cir.setReturnValue(true);
    }
}
